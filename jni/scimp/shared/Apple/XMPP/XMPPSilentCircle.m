/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#import "XMPPSilentCircle.h"
#import "XMPPLogging.h"
#import "XMPPInternal.h"
#import "XMPPFramework.h"
#import "NSData+XMPP.h"
#import "XMPPMessage+SilentCircle.h"
#import "XMPPMessage+XEP_0033.h"
#import "XMPPLogging.h"
#import "NSString+SCUtilities.h"
#import "XMPPSilentCircleStorage.h"
 
#import <SCimp.h>
#import <Siren.h>

#include <tomcrypt.h>

#include <cryptowrappers.h>
 #import <SCpubTypes.h>
#import "AppConstants.h"


#if ! __has_feature(objc_arc)
#warning This file must be compiled with ARC. Use -fobjc-arc flag (or convert project to ARC).
#endif

// Log levels: off, error, warn, info, verbose
// Log flags: trace
#if DEBUG
//  static const int xmppLogLevel = XMPP_LOG_LEVEL_VERBOSE | XMPP_LOG_FLAG_TRACE;
static const int xmppLogLevel = XMPP_LOG_LEVEL_WARN;


#else
  static const int xmppLogLevel = XMPP_LOG_LEVEL_WARN;
#endif



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - XMPPSilentCircle
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@implementation XMPPSilentCircle
{
	id <XMPPSilentCircleStorageDelegate> storage;
		
	XMPPJID         *myJID;
    SCimpContextRef pubScimpCtx;
}

NSMutableArray *scimpCache;  //Value=SCimpWrapper

@synthesize storage = storage;
@synthesize scimpCipherSuite = _scimpCipherSuite;
@synthesize scimpSASMethod  = _scimpSASMethod;


- (id)init
{
	// This will cause a crash - it's designed to.
	// Only the init methods listed in XMPPSilentCircle.h are supported.
	
    pubScimpCtx = kInvalidSCimpContextRef;
    
	return [self initWithStorage:nil dispatchQueue:NULL];
}

- (id)initWithDispatchQueue:(dispatch_queue_t)queue
{
	// This will cause a crash - it's designed to.
	// Only the init methods listed in XMPPSilentCircle.h are supported.
	
	return [self initWithStorage:nil dispatchQueue:NULL];
}

- (id)initWithStorage:(id <XMPPSilentCircleStorageDelegate>)inStorage
{
	return [self initWithStorage:inStorage dispatchQueue:NULL];
}

- (id)initWithStorage:(id <XMPPSilentCircleStorageDelegate>)inStorage dispatchQueue:(dispatch_queue_t)queue
{
	NSParameterAssert(inStorage != nil);
	
	if ((self = [super initWithDispatchQueue:queue]))
	{
		storage = inStorage;
		
		scimpCache = [[NSMutableArray alloc] init];
		
	}
	return self;
}

- (BOOL)activate:(XMPPStream *)aXmppStream
{
    SCLError        err = kSCLError_NoErr;;
    
	if ([super activate:aXmppStream])
	{
#ifdef _XMPP_CAPABILITIES_H
		[xmppStream autoAddDelegate:self delegateQueue:moduleQueue toModulesOfClass:[XMPPCapabilities class]];
#endif
		myJID = [xmppStream myJID];
		[[NSNotificationCenter defaultCenter] addObserver:self
		                                         selector:@selector(myJidDidChange:)
		                                             name:XMPPStreamDidChangeMyJIDNotification
		                                           object:xmppStream];
		
        err = SCimpNew([[myJID bare] UTF8String], NULL,  &pubScimpCtx);
        if (err != kSCLError_NoErr)
        {
            XMPPLogError(@"%@: SCimpNew error = %d", THIS_FILE, err);
            return NO;
        }

        err = SCimpSetEventHandler(pubScimpCtx, XMPPSCimpEventHandler, (__bridge void *)self);
		if (err != kSCLError_NoErr)
		{
			XMPPLogError(@"%@: SCimpSetEventHandler error = %d", THIS_FILE, err);
		}
        
return YES;
	}
	
	return NO;
}

- (void)deactivate
{
    if(pubScimpCtx)
        SCimpFree(pubScimpCtx);
    pubScimpCtx = kInvalidSCimpContextRef;
    
    
#ifdef _XMPP_CAPABILITIES_H
	[xmppStream removeAutoDelegate:self delegateQueue:moduleQueue fromModulesOfClass:[XMPPCapabilities class]];
#endif
	
	[[NSNotificationCenter defaultCenter] removeObserver:self
	                                                name:XMPPStreamDidChangeMyJIDNotification
	                                              object:xmppStream];
	
	[super deactivate];
}


- (void)myJidDidChange:(NSNotification *)notification
{
	// My JID changed.
	// So either our resource changed, or a different user logged in.
	// Either way, since the encryption is tied to our JID, we have to flush all state.
	
	XMPPJID *newMyJid = xmppStream.myJID;
	
	dispatch_block_t block = ^{ @autoreleasepool {
  
       [self removeAllSecureContexts];
  		myJID = newMyJid;
        
        if(pubScimpCtx)
            SCimpFree(pubScimpCtx);
        pubScimpCtx = kInvalidSCimpContextRef;
        
        SCLError  err = SCimpNew([[myJID bare] UTF8String], NULL,  &pubScimpCtx);
        if (err != kSCLError_NoErr)
        {
            XMPPLogError(@"%@: SCimpNew error = %d", THIS_FILE, err);
        }

        err = SCimpSetEventHandler(pubScimpCtx, XMPPSCimpEventHandler, (__bridge void *)self);
		if (err != kSCLError_NoErr)
		{
			XMPPLogError(@"%@: SCimpSetEventHandler error = %d", THIS_FILE, err);
		}

        
 	}};
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_async(moduleQueue, block);
}



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark Secure Context Cache
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


-(SCimpWrapper*) scimpForScimpID: (NSString *)scimpID
{
    SCimpWrapper* scimp = NULL;
    
    NSPredicate * predicate = [NSPredicate predicateWithFormat:@"(scimpID!=nil) && scimpID==%@",scimpID];
    
    NSArray *filteredArray = [scimpCache filteredArrayUsingPredicate:predicate ];
    
    if(filteredArray && [filteredArray count])
        scimp = [filteredArray objectAtIndex:0];
    
    return scimp;
}

-(SCimpWrapper*) scimpForJid:(XMPPJID *)remoteJID
{
    SCimpWrapper* scimp = NULL;
    
    NSPredicate *findMatchingJid = [NSPredicate predicateWithBlock: ^BOOL(id obj, NSDictionary *bind)
                                    {
                                        SCimpWrapper *item = (SCimpWrapper*) obj;
                                        
                                        BOOL found = [remoteJID isEqualToJID: item->remoteJid  options:
                                                      (remoteJID.resource  && item->remoteJid.resource)?XMPPJIDCompareFull:XMPPJIDCompareBare]
                                                    && [myJID isEqualToJID:item->localJid options:XMPPJIDCompareBare];
                                        
                                        return found ;
                                    }];
    
    NSArray*  scimpItems = [scimpCache filteredArrayUsingPredicate:findMatchingJid];
    
    if([scimpItems count])
    {
        scimp = [scimpItems objectAtIndex:0];
    }
    
    return scimp;
}


-(SCimpWrapper*) scimpforThread: (NSString *)threadElement forJID:(XMPPJID*)jid
{
    SCimpWrapper* scimp = NULL;
    
    NSPredicate * predicate = [NSPredicate predicateWithFormat:@"(threadElement!=nil) && threadElement==%@",threadElement];

    NSArray *filteredArray = [scimpCache filteredArrayUsingPredicate:predicate ];
    
    if(filteredArray.count)
    {
        for(SCimpWrapper *item in filteredArray)
        {
           if([item->localJid isEqualToJID:jid options:XMPPJIDCompareBare])
           {
               scimp = item; break;
           }
        }
    }
   
    return scimp;
}


#pragma mark secure context control


- (SCimpWrapper *)scimpForThread:(NSString *)threadElement forJID:(XMPPJID*)jid toJids:(NSArray*)multicastJids isNew:(BOOL *)isNewPtr
{
 	BOOL isNew = NO;
    
    SCimpWrapper *scimp = [self scimpforThread: threadElement forJID:jid ];
    
    if (scimp == nil)
	{
        SCLError        err = kSCLError_NoErr;;
        
        scimp = [self restoreStateForThread: threadElement forJID:jid];
        
		if (!scimp)
		{
            SCimpContextRef ctx = kInvalidSCimpContextRef;
            
            NSString* conversationID = [storage conversationIDForForMultiCast:threadElement localJid:myJID];
            NSString* multicastKey = [storage multicastKeyforConversationID:conversationID ];
            
            if(!multicastKey)   // it might be a new conversation
            {
                multicastKey = [storage multicastKeyforThreadID: threadElement];
            }
            
            if(multicastKey)
            {
                SCKeyContextRef    key = kInvalidSCKeyContextRef;
                
                err = SCKeyDeserialize((uint8_t*)multicastKey.UTF8String, multicastKey.length, &key);
                if (err != kSCLError_NoErr)
                {
                    XMPPLogError(@"%@: SCKeyDeserialize error = %d", THIS_FILE, err);
                    return nil;
                }
                
                err = SCimpNewSymmetric(key, threadElement.UTF8String, &ctx);
                
                if (err != kSCLError_NoErr)
                {
                    XMPPLogError(@"%@: SCimpNewSymmetric error = %d", THIS_FILE, err);
                    return nil;
                }
                
                SCKeyFree(key);
                isNew = YES;
                
                // use PGP SAS for scimp Symmetric
                SCimpSetNumericProperty(ctx,kSCimpProperty_SASMethod, kSCimpSAS_PGP);
                
                scimp = [[SCimpWrapper alloc] initWithThread:threadElement localJID:myJID scimpCtx:ctx ];
                scimp->conversationID = conversationID;
                scimp->multicastJids = multicastJids;
                
                NSDictionary* info = [self secureContextInfoForScimp:scimp];
                
                [self->multicastDelegate xmppSilentCircle:self didEstablishSecureContextForSCimpWrapper:scimp  withInfo:info];

            }
            else
            {
                XMPPLogError(@" multicastKey %@ not found", multicastKey);
                
            }
        }
        if(scimp)
        {
            [scimpCache addObject:scimp];
            
            err = SCimpSetEventHandler(scimp->scimpCtx, XMPPSCimpEventHandler, (__bridge void *)self);
            if (err != kSCLError_NoErr)
            {
                XMPPLogError(@"%@: SCimpSetEventHandler error = %d", THIS_FILE, err);
            }
            
            SCimpEnableTransitionEvents(scimp->scimpCtx, true);
            
        }
   
    }
        
 	
	if (isNewPtr) *isNewPtr = isNew;
	return scimp;
}

-(SCLError) restoreScimpWrapperState:(SCimpWrapper *)scimp
{
    SCLError        err = kSCLError_NoErr; 
 
    [scimpCache addObject:scimp];
    
    err = SCimpSetEventHandler(scimp->scimpCtx, XMPPSCimpEventHandler, (__bridge void *)self);
    if (err != kSCLError_NoErr)
    {
        XMPPLogError(@"%@: SCimpSetEventHandler error = %d", THIS_FILE, err);
    }
    
    SCimpEnableTransitionEvents(scimp->scimpCtx, true);
    
    err = SCimpSetNumericProperty(scimp->scimpCtx, kSCimpProperty_CipherSuite, _scimpCipherSuite);
    if (err != kSCLError_NoErr)
    {
        XMPPLogError(@"%@: SCimpSetNumericProperty(kSCimpProperty_CipherSuite) error = %d", THIS_FILE, err);
    }
    
    err = SCimpSetNumericProperty(scimp->scimpCtx, kSCimpProperty_SASMethod, _scimpSASMethod);
    if (err != kSCLError_NoErr)
    {
        XMPPLogError(@"%@: SCimpSetNumericProperty(kSCimpProperty_SASMethod) error = %d", THIS_FILE, err);
    }
    
    

done:
    return err;
    
}

- (SCimpWrapper *)scimpForJid:(XMPPJID *)remoteJid isNew:(BOOL *)isNewPtr
{
	BOOL isNew = NO;
    
    SCimpWrapper *scimp = [self scimpForJid: remoteJid ];
    
    if(scimp)
    {
        // was bare JID, update it now.
        if(remoteJid.resource && !scimp->remoteJid.resource)
        {
            //  we have upgraded our JID to a full JID now.
            [storage removeStateWithSCimpID:scimp->scimpID];
            
            scimp->remoteJid = remoteJid;
            [scimp updateScimpID];
            [self saveState:scimp];
        }
    }
    
	if (scimp == nil)
	{
			SCLError        err = kSCLError_NoErr;;
        
        scimp = [self restoreState:myJID remoteJid:remoteJid];
        
		if (!scimp)
		{
            SCimpContextRef ctx = kInvalidSCimpContextRef;
            
			err = SCimpNew([[myJID bare] UTF8String], [[remoteJid bare] UTF8String],  &ctx);
			if (err != kSCLError_NoErr)
			{
				XMPPLogError(@"%@: SCimpNew error = %d", THIS_FILE, err);
				return nil;
			}
			
			isNew = YES;
            
            scimp = [[SCimpWrapper alloc] initWithLocalJid:myJID remoteJID:remoteJid scimpCtx:ctx ];
            
            scimp->conversationID = [storage conversationIDForRemoteJid:remoteJid localJid:myJID];
 		}
        
        err = [self restoreScimpWrapperState: scimp]; CKERR;
    }
done:
    
	if (isNewPtr) *isNewPtr = isNew;
	return scimp;
}


-(void) removeAllSecureContexts
{
 	dispatch_block_t block = ^{
		
        
  		for (SCimpWrapper *scimp in scimpCache)
		{
            SCimpFree(scimp->scimpCtx);
            scimp->scimpCtx = kInvalidSCimpContextRef;
   		}
        [scimpCache removeAllObjects];
        
	};
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_sync(moduleQueue, block);
    
}

-(void) saveAllSecureContexts
{
 	dispatch_block_t block = ^{
		
  		for (SCimpWrapper *scimp in scimpCache)
		{
			[self saveState:scimp];
		}
        
	};
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_sync(moduleQueue, block);
    
}

- (void)removeSecureContextForSCimpID:(NSString *)scimpID
{
 	
	dispatch_block_t block = ^{
		
		SCimpWrapper *scimp = [self scimpForScimpID: scimpID];
    
        if(scimp)
        {
            SCimpFree(scimp->scimpCtx);
            scimp->scimpCtx = kInvalidSCimpContextRef;
            [scimpCache removeObject: scimp];
         }
        
        scimp = NULL;
        
	};
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_sync(moduleQueue, block);
	
 
}

- (BOOL)hasSecureContextForSCimpID:(NSString *)scimpID
{
	__block BOOL result = NO;
	
	dispatch_block_t block = ^{
		
		SCimpWrapper *scimp = [self scimpForScimpID: scimpID];
        
        if(!scimp)
        {
            scimp = [self restoreStateWithSCimpID:scimpID];
        }

        if(scimp)
        {
             result = [scimp isReady];
        }
	};
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_sync(moduleQueue, block);
	
	return result;
}



- (void)rekeySecureContextForSCimpID:(NSString *)scimpID withReset:(BOOL) withReset
{
   	dispatch_block_t block = ^{
		
        SCimpWrapper *scimp = [self scimpForScimpID: scimpID];
          
        if(!scimp)
        {
            scimp = [self restoreStateWithSCimpID:scimpID];
        }
  
         if(scimp)
        {
            SCLError  err = kSCLError_NoErr;
            SCimpState current_state = kSCimpState_Init;
            
            if(withReset)
            {
                SCimpResetKeys(scimp->scimpCtx);
            }
            
            err = SCimpGetNumericProperty( scimp->scimpCtx, kSCimpProperty_SCIMPstate, &current_state);
            
            /* dont key when we are keying */
            if(current_state != kSCimpState_Commit
               || current_state !=  kSCimpState_DH1
               || current_state !=  kSCimpState_DH2
               || current_state !=  kSCimpState_Confirm)
                SCimpStartDH(scimp->scimpCtx);
            
        }
    };
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_sync(moduleQueue, block);

}

- (void)acceptSharedSecretForSCimpID:(NSString *)scimpID
{
  
  	dispatch_block_t block = ^{
		
        SCimpWrapper *scimp = [self scimpForScimpID: scimpID];
        if(scimp)
        {
            SCimpAcceptSecret(scimp->scimpCtx);
        }
    };
	
	if (dispatch_get_specific(moduleQueueTag))
		block();
	else
		dispatch_sync(moduleQueue, block);
    
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark Utilities
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 
- (BOOL)encryptPubKeyMessage:(XMPPMessage *)message pkString:(NSString*)pkString
{
	XMPPLogTrace();
	
    BOOL success = NO;
    NSString *scKeyData  = [[message elementForName:kSCPPPubSiren] stringValue];
    
     SCKeyContextRef pubKey = kInvalidSCKeyContextRef;
     
    if(pkString
       && IsntSCLError(SCKeyDeserialize((uint8_t*)pkString.UTF8String,  pkString.length, &pubKey))
       && SCKeyContextRefIsValid(pubKey)
       && ([scKeyData length] > 0))
        {
            NSData *data = [scKeyData dataUsingEncoding:NSUTF8StringEncoding];
            NSString* notifyValue =   [[message elementForName:kSCPPSiren] attributeStringValueForName: kXMPPNotifiable];
            NSString*  badgeValue =   [[message elementForName:kSCPPSiren] attributeStringValueForName: kXMPPBadge];
            
            // strip out the siren before encrypting and sending.
            [message stripSilentCircleData];
            
            
            if(notifyValue || badgeValue)
            {
                //
                // mark any packets that dont really need push notifcations here as false.
                //
                
                NSXMLElement *x = [NSXMLElement elementWithName:@"x" xmlns:kSCPPNameSpace];
                if(notifyValue)  [x addAttributeWithName: kXMPPNotifiable stringValue: notifyValue];
                if(badgeValue)  [x addAttributeWithName: kXMPPBadge stringValue: badgeValue];
                
                [message addChild:x];
                
            }
           
            SCLError err = SCimpSendPublic(pubScimpCtx, pubKey,  (void *) data.bytes , (size_t) data.length , (__bridge void*) message );
            if (err != kSCLError_NoErr)
            {
                XMPPLogError(@"%@: %@ - SCimpSendPublic err = %d", THIS_FILE, THIS_METHOD, err);
            }
            else success = YES;

        }
    
    if(SCKeyContextRefIsValid (pubKey))
        SCKeyFree(pubKey);
    

    return success;
}


- (void)encryptOutgoingMessage:(XMPPMessage *)message withScimp:(SCimpWrapper *)scimp
{
	XMPPLogTrace();
	
	NSString *sirenData = [[message elementForName:kSCPPSiren] stringValue];
    
     
	if ([sirenData length] > 0)
	{
		NSData *data = [sirenData dataUsingEncoding:NSUTF8StringEncoding];
        NSString* notifyValue =   [[message elementForName:kSCPPSiren] attributeStringValueForName: kXMPPNotifiable];
        NSString*  badgeValue =   [[message elementForName:kSCPPSiren] attributeStringValueForName: kXMPPBadge];
    
        // strip out the siren before encrypting and sending.
        [message stripSilentCircleData];
        
        NSXMLElement *x = [NSXMLElement elementWithName:@"x" xmlns:kSCPPNameSpace];
        
        //
        // mark any packets that dont really need push notifcations here as false.
        //
        if(notifyValue)  [x addAttributeWithName: kXMPPNotifiable stringValue: notifyValue];
        if(badgeValue)  [x addAttributeWithName: kXMPPBadge stringValue: badgeValue];
          
        [message addChild:x];
        
        // How does this work?
		// We invoke SCimpSendMsg which process the message and synchrously
        // calls XMPPSCimpEventHandler and then assembleSCimpDataMessage
		
		SCLError err = SCimpSendMsg(scimp->scimpCtx, (void *)[data bytes], (size_t)[data length], (__bridge void*) message );
		if (err != kSCLError_NoErr)
		{
			XMPPLogError(@"%@: %@ - SCimpSendMsg err = %d", THIS_FILE, THIS_METHOD, err);
		}
    }
}


- (SCimpWrapper*)restoreStateForThread:(NSString *)threadElement forJID:(XMPPJID*)jid 
{
      SCimpWrapper* scimp = NULL;
    NSData *state = NULL;
    NSArray *pending = NULL;
  
    NSString* scimpID;
    
    scimpID = [SCimpWrapper threadHash:threadElement localJID:jid ];
    [storage  restoreStateAndPendingWithSCimpID:scimpID state:&state pending:&pending];
     
    SCKeyContextRef storageKey = storage.storageKey;
      
    if (state && storageKey)
    {
        SCLError err = kSCLError_NoErr;
        SCimpContextRef ctx = kInvalidSCimpContextRef;
        
        err = SCimpDecryptState(storageKey,                 // key 
                                (void *)[state bytes],       // blob
                                (size_t)[state length],      // blob length
                                &ctx);                       // out context
        
        if (err == kSCLError_NoErr)
        {
            scimp = [[SCimpWrapper alloc] initWithThread:threadElement  localJID:myJID  scimpCtx:ctx ];
            scimp->conversationID = [storage conversationIDForForMultiCast: threadElement localJid:jid];
            [scimp->pendingOutgoingMessages removeAllObjects];
            if(pending) scimp->pendingOutgoingMessages =  [pending mutableCopy];
            
        }
        else
            XMPPLogWarn(@"%@: Error restoring state for [%@] : %d", THIS_FILE,threadElement , err);
    }
    
    return scimp;
}

- (SCimpWrapper*)restoreState:(XMPPJID *)myJid remoteJid:(XMPPJID *)remoteJid
{
    SCimpWrapper* scimp = NULL;
    NSData *state = NULL;
    NSArray *pending = NULL;
    
    NSString* scimpID;
    
    if(remoteJid.resource)
    {
        scimpID = [SCimpWrapper searchKeyWithLocalJid:myJid remoteJID:remoteJid options:XMPPJIDCompareFull];
        [storage  restoreStateAndPendingWithSCimpID:scimpID state:&state pending:&pending];
    }
    if(!state)
    {
        scimpID = [SCimpWrapper searchKeyWithLocalJid:myJid remoteJID:remoteJid options:XMPPJIDCompareBare];
        [storage  restoreStateAndPendingWithSCimpID:scimpID state:&state pending:&pending];
    }
    
    SCKeyContextRef storageKey = storage.storageKey;
      
    if (state && storageKey)
    {
        SCLError err = kSCLError_NoErr;
        SCimpContextRef ctx = kInvalidSCimpContextRef;
     
        err = SCimpDecryptState(storageKey,                 // key 
                                (void *)[state bytes],       // blob
                                (size_t)[state length],      // blob length
                                &ctx);                       // out context
        
        if (err == kSCLError_NoErr)
        {
            scimp = [[SCimpWrapper alloc] initWithLocalJid:myJID remoteJID:remoteJid scimpCtx:ctx ];
            scimp->conversationID = [storage conversationIDForRemoteJid:remoteJid localJid:myJID];
            [scimp->pendingOutgoingMessages removeAllObjects];
            if(pending) scimp->pendingOutgoingMessages =  [pending mutableCopy];
        }
        else
            XMPPLogWarn(@"%@: Error restoring state for [%@, %@] : %d", THIS_FILE, myJID, remoteJid, err);
    }

    return scimp;
}



- (SCimpWrapper*)restoreStateWithSCimpID:(NSString *)scimpID
{
    SCimpWrapper* scimp = NULL;
    NSData *state = NULL;
     
    
 
    SCKeyContextRef storageKey = storage.storageKey;
    
    if (storageKey &&
        [storage  restoreSCimpWrapperWithSCimpID:scimpID
                                           scimp:&scimp
                                           state:&state] )
    {
        SCLError err = kSCLError_NoErr;
        SCimpContextRef ctx = kInvalidSCimpContextRef;
        
        err = SCimpDecryptState(storageKey,                 // key
                                (void *)[state bytes],       // blob
                                (size_t)[state length],      // blob length
                                &ctx);                       // out context
          
        if (err == kSCLError_NoErr)
        {
            scimp->scimpCtx = ctx;
            
            err = [self restoreScimpWrapperState: scimp];

        }
        else
        {
            scimp = NULL;
            XMPPLogWarn(@"%@: Error restoring state for [%@] : %d", THIS_FILE,scimpID , err);
            
        }
    }
    
    
    return scimp;
}



- (void)saveState:(SCimpWrapper *)scimp
{
  //	XMPPLogWarn(@"%@: %@ [%@, %@] -> %@", THIS_FILE, THIS_METHOD, scimp->localJid, scimp->remoteJid, searchKey);
    
     SCKeyContextRef storageKey = storage.storageKey;

	if(storageKey && scimp)
    {
        void *blob = NULL;
        size_t blobLen = 0;
        
        SCLError err = SCimpEncryptState(scimp->scimpCtx, storageKey, &blob, &blobLen);
        
        if (err == kSCLError_NoErr)
        {
            NSData *state = [NSData dataWithBytesNoCopy:blob length:blobLen freeWhenDone:YES];
            
            [storage saveState:state withSCimpWrapper:scimp];
      
        }
        else
        {
            XMPPLogError(@"%@: SCimpSaveState error = %d", THIS_FILE, err);
        }
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark SCimp
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)sendSCimpHandshakeMessage:(NSString *)scimpOut shouldPush:(BOOL)shouldPush withScimp:(SCimpWrapper *)scimp
{
    // This is a SCimp handshake message.
    //
    // <message to=remoteJid">
    // <body>This message is protected by Silent Circle. http://silentcircle.com/  </body>
    //   <x xmlns="http://silentcircle.com">scimp-encoded-data</x>
    // </message>
    
	XMPPLogCTrace();

    // Note from awd: This message does not have the id attribute with the UUID as provided by Siren.
    // This breaks roundtrip message and response processing. If this is no longer deemed to be important,
    // then this now vestigal feature should be removed from Siren.
    XMPPMessage *message = [XMPPMessage message];
    [message addAttributeWithName:@"to" stringValue: scimp->remoteJid.bare];
    [message addAttributeWithName:@"type" stringValue:@"chat"];
    
    NSXMLElement *x = [NSXMLElement elementWithName:@"x" xmlns:kSCPPNameSpace];

    [x setStringValue:scimpOut];
    
    NSXMLElement *bodyElement = [NSXMLElement elementWithName:@"body"];
    [message addChild:bodyElement];
    [bodyElement setStringValue:  [NSString stringWithFormat:kSCPPBodyTextFormat, [self->myJID bare]]];
    
    if(!shouldPush)
    {
        [x addAttributeWithName: kXMPPNotifiable stringValue:@"false"];
        [x addAttributeWithName: kXMPPBadge stringValue:@"false"];
    }

    [message addChild:x];
    [self->xmppStream sendElement:message];
}

- (void)assembleSCimpDataMessage:(NSString *)scimpOut shouldPush:(BOOL)shouldPush withScimp:(SCimpWrapper *)scimp withMessage:(XMPPMessage *)message
{
     
    // Add the encrypted body.
    // Result should look like this:
    //
    // <message to=remoteJid">
    //   <body></body>
    //   <x xmlns="http://silentcircle.com">scimp-encrypted-data</x>
    // </message>
     
    NSXMLElement *x = [message elementForName:@"x" xmlns:kSCPPNameSpace];
    
    if(!x)
    {
         x = [NSXMLElement elementWithName:@"x" xmlns:kSCPPNameSpace];
        [message addChild:x];
    }

    if(!shouldPush)
    {
        [x addAttributeWithName: kXMPPNotifiable stringValue:@"false"];
        [x addAttributeWithName: kXMPPBadge stringValue:@"false"];
    }
    

    [x setStringValue:scimpOut];

#if 0
    
#warning REmove before ship
    NSXMLElement *bodyElement = [NSXMLElement elementWithName:@"body"];
    [message addChild:bodyElement];
    [bodyElement setStringValue:  [NSString stringWithFormat:@"test Message from %@ %ld bytes ", [self->myJID bare],  (unsigned long)scimpOut.length ]];
    
#endif
}

- (void)assembleSCimpPKDataMessage:(NSString *)scimpOut shouldPush:(BOOL)shouldPush withScimp:(SCimpWrapper *)scimp withMessage:(XMPPMessage *)message
{
    
    // Add the encrypted body.
    // Result should look like this:
    //
    // <message to=remoteJid">
    //   <body></body>
    //   <x xmlns="http://silentcircle.com/protocol/scimp#public-key>scimp-encrypted-data</x>
    // </message>
    
    NSXMLElement *x = [message elementForName:@"x" xmlns:kSCPPpublicKeyNameSpace];
    
    if(!x)
    {
        x = [NSXMLElement elementWithName:@"x" xmlns:kSCPPpublicKeyNameSpace];
        [message addChild:x];
    }
    
    if(!shouldPush)
    {
        [x addAttributeWithName: kXMPPNotifiable stringValue:@"false"];
        [x addAttributeWithName: kXMPPBadge stringValue:@"false"];
    }
    
    
    [x setStringValue:scimpOut];
    
}


- (NSDictionary*)secureContextInfoForScimp:(SCimpWrapper*) scimp
{
    NSMutableDictionary* infoDict  = NULL;
    
    if(scimp)
    {
        SCLError     err  = kSCLError_NoErr;
        SCimpInfo    info;
        char *SASstr = NULL;
        size_t length = 0;
        
        err = SCimpGetInfo(scimp->scimpCtx, &info); CKERR;
        
        if(info.isReady)
        {
            err = SCimpGetAllocatedDataProperty(scimp->scimpCtx, kSCimpProperty_SASstring, (void*) &SASstr, &length); CKERR;
            
            NSString *SAS = [NSString.alloc initWithBytesNoCopy: SASstr
                                                         length: length
                                                       encoding: NSUTF8StringEncoding
                                                   freeWhenDone: YES];
            
            infoDict = [NSMutableDictionary dictionaryWithCapacity: 10];
            
            [infoDict setValue:[NSNumber numberWithInteger:info.version] forKey:kSCIMPInfoVersion];
            [infoDict setValue:[NSNumber numberWithInteger:info.cipherSuite] forKey:kSCIMPInfoCipherSuite];
            [infoDict setValue:[NSNumber numberWithInteger:info.sasMethod] forKey:kSCIMPInfoSASMethod];
            [infoDict setValue:[NSNumber numberWithBool: (info.csMatches?YES:NO)] forKey:kSCIMPInfoCSMatch];
            [infoDict setValue:[NSNumber numberWithBool: (info.hasCs?YES:NO)] forKey:kSCIMPInfoHasCS];
            [infoDict setValue:[NSNumber numberWithInteger:info.scimpMethod] forKey:kSCIMPMethod];
                        
            [infoDict setValue:SAS forKey:kSCIMPInfoSAS];
        }
    }
    
    
done:
    
    return infoDict;
}



#pragma mark
#pragma mark SCimp Event Handler

SCLError XMPPSCimpEventHandler(SCimpContextRef ctx, SCimpEvent *event, void *userInfo)
{
	XMPPLogCTrace();
    
    SCLError  err = kSCLError_NoErr;
	char        errorBuf[256];
    
	XMPPSilentCircle *self = (__bridge XMPPSilentCircle *)userInfo;
	
	SCimpWrapper *scimp = nil;
	for (SCimpWrapper *w in scimpCache)
	{
		if (w->scimpCtx == ctx)
		{
			scimp = w;
			break;
		}
	}
	
    
    if(ctx == self->pubScimpCtx)
    {
        
    }
    else  if (scimp == nil)
    {
        XMPPLogCError(@"%@: %s - Unknown scimp context", THIS_FILE, __FUNCTION__);
        
        // We don't know of an existing context for this callback.
        
        return kSCLError_UnknownRequest;
}
    
    switch(event->type)
	{
            
#pragma mark kSCimpEvent_Warning
        case kSCimpEvent_Warning:
        {
            SCLError warning = event->data.warningData.warning;
            
            SCLGetErrorString(event->data.warningData.warning, sizeof(errorBuf), errorBuf);
            
            XMPPLogCVerbose(@"%@: %s - kSCimpEvent_Warning - %d:  %s", THIS_FILE, __FUNCTION__,
                            event->data.warningData.warning, errorBuf);
            
            if(warning == kSCLError_SecretsMismatch)
            {
                 // we ignore the SAS warning here, we'll pick it up once the connection is established.
            }
            else if(warning == kSCLError_ProtocolContention )
            {
            // the other side responded to our Commit with an other commit.
                [self->multicastDelegate xmppSilentCircle:self protocolWarningForSCimpWrapper:scimp
                                                    withMessage:NULL 
                                                    error:event->data.warningData.warning ];
                
            }
            else if(warning == kSCLError_ProtocolError )
            {
                // the other side responded out of order? probably we should rekey.
               [self->multicastDelegate xmppSilentCircle:self protocolWarningForSCimpWrapper:scimp 
                                                    withMessage:NULL
                                                   error:event->data.warningData.warning ];
                
            }
            else
            {
                NSCAssert2(NO, @"Unhandled SCimpEvent_Warning: %d: %s",
                                warning, errorBuf );
            }
            break;
        }
  
#pragma mark kSCimpEvent_Error
        case kSCimpEvent_Error:
        {
            SCLGetErrorString(event->data.errorData.error, sizeof(errorBuf), errorBuf);
            
            XMPPLogCError(@"%@: %s - kSCimpEvent_Error - %d:  %s", THIS_FILE, __FUNCTION__,
                            event->data.errorData.error, errorBuf);
      
            
            [self->multicastDelegate xmppSilentCircle:self protocolErrorForSCimpWrapper:scimp
                                                withMessage:(__bridge XMPPMessage*)event->userRef
                                                error:event->data.errorData.error ];
        
            break;
        }

#pragma mark kSCimpEvent_Keyed
        case kSCimpEvent_Keyed:
        {
            XMPPLogCVerbose(@"%@: %s - kSCimpEvent_Keyed", THIS_FILE, __FUNCTION__);
            
            // SCimp is telling us it has completed the key exchange,
            // and is ready to encrypt and decrypt messages.
   
            NSDictionary* info = [self secureContextInfoForScimp:scimp];
            
            // Only notify the delegates after the scimp state is saved. (Precludes a state race condition.)
            [self->multicastDelegate xmppSilentCircle:self didEstablishSecureContextForSCimpWrapper:scimp  withInfo:info
             ];
            

            // Dequeue anything we had pending.
            
            for (XMPPMessage *message in scimp->pendingOutgoingMessages)
            {
                // We don't encrypt the message here.
                // SCimp isn't ready yet as its in the middle of completing and invoking our event handler.
                // So we simply handle the encryption via the normal channels, which is actually easier to maintain.
                
                [self->xmppStream sendElement:message];
            }
              
            [scimp->pendingOutgoingMessages removeAllObjects];
            [self saveState:scimp];

             break;
        }
            
#pragma mark kSCimpEvent_ReKeying
        case kSCimpEvent_ReKeying:
        {
            XMPPLogCVerbose(@"%@: %s - kSCimpEvent_ReKeying", THIS_FILE, __FUNCTION__);
  
            //*VINNIE*     not sure if we need to do the same as keying event  we might need more code here?
 
 /*           // the otherside did a rekey.
            [self saveState:scimp];
            
            [self->multicastDelegate xmppSilentCircle:self protocolWarning:scimp->remoteJid
                                          withMessage:NULL 
                                                error:kSCLError_ProtocolContention ];
 */
            break;
        }
            
        
#pragma mark kSCimpEvent_SendPacket
        case kSCimpEvent_SendPacket:
        {
            XMPPLogCVerbose(@"%@: %s - kSCimpEvent_SendPacket", THIS_FILE, __FUNCTION__);
            
            // SCimp is handing us encrypted data to send.
            // This may be:
            // - data we asked it to encrypt (for an outgoing message)
            // - handshake related information
            
            
            NSString* scimpOut = [NSString.alloc initWithBytes: event->data.sendData.data
                                                        length: event->data.sendData.length 
                                                      encoding: NSUTF8StringEncoding  ];
            
//            XMPPLogCVerbose(@"Outgoing encrypted data: %@", scimpOut);
		 	
            // if the kSCimpEvent_SendPacket event has a userRef, it means that this was the result 
            //  of calling  SCimpSendMsg and not a scimp doing protocol keying stuff.
            
            if (event->userRef)
            {
                
                // Add the encrypted body.
                // Result should look like this:
                //
                // <message to=remoteJid">
                //   <body></body>
                //   <x xmlns="http://silentcircle.com">scimp-encrypted-data</x>
                // </message>

                // add the scimp-encrypted-data to the message here.
                
                if(event->data.sendData.isPKdata )
                {
                    [self assembleSCimpPKDataMessage:scimpOut
                                        shouldPush:event->data.sendData.shouldPush
                                         withScimp:scimp
                                       withMessage:(__bridge XMPPMessage*)event->userRef];
  
                }
                else
                {
                    [self assembleSCimpDataMessage:scimpOut
                                        shouldPush:event->data.sendData.shouldPush
                                         withScimp:scimp
                                       withMessage:(__bridge XMPPMessage*)event->userRef];
 
                }
               }
            else
            {
                // This is a SCimp handshake message.
                [self sendSCimpHandshakeMessage:scimpOut
                                     shouldPush:event->data.sendData.shouldPush
                                      withScimp:scimp];
            }
            break;
        }
           
#pragma mark kSCimpEvent_Decrypted
        case kSCimpEvent_Decrypted:
        {
            XMPPLogCVerbose(@"%@: %s - kSCimpEvent_Decrypted", THIS_FILE, __FUNCTION__);
            
            XMPPMessage* message  = (__bridge XMPPMessage*)event->userRef;
            
            NSString *decryptedBody = [[NSString alloc] initWithBytes:event->data.decryptData.data
                                                               length:event->data.decryptData.length
                                                                encoding:NSUTF8StringEncoding];
   
            NSXMLElement *sirenElement = [NSXMLElement elementWithName:kSCPPSiren xmlns:kSCPPNameSpace];
   
             
            // strip out any existing internal sielnt circle notations to prevent counterfeiting 
            [message stripSilentCircleData];
            
            [message addChild:sirenElement];
            [message setConversationIdElement: scimp->conversationID];
            [sirenElement setStringValue:decryptedBody];
             break;
        }
            
#pragma mark kSCimpEvent_ClearText
        case kSCimpEvent_ClearText:
        {
            XMPPLogCError(@"%@: %s - kSCimpEvent_ClearText - this should never happen with us", THIS_FILE, __FUNCTION__);
 // *VINNIE* this should never happen with us.
            break;
        }

#pragma mark kSCimpEvent_Transition
         case kSCimpEvent_Transition:
        {
            SCimpEventTransitionData  *d =    &event->data.transData;
            
            [self->multicastDelegate xmppSilentCircle:self protocolDidChangeStateForSCimpWrapper:scimp state:d->state];
            
        }
            break;
            
#pragma mark kSCimpEvent_AdviseSaveState
        case kSCimpEvent_AdviseSaveState:
        {
            [self saveState:scimp];

        }
        break;
       
            
#pragma mark kSCimpEvent_PubData
        case kSCimpEvent_PubData:
        {
            XMPPMessage* message  = (__bridge XMPPMessage*)event->userRef;
            
            NSString *scKeyBody = [[NSString alloc] initWithBytes:event->data.pubData.data
                                                               length:event->data.pubData.length
                                                             encoding:NSUTF8StringEncoding];
            
            
            
            Siren* siren = [[Siren alloc]initWithJSON:scKeyBody];
            if(siren.isValid && siren.multicastKey)
            {
                [self.storage saveMulticastKey:siren.multicastKey forJid:self->myJID.bare];
                break;
            }
             // strip out any existing internal sielnt circle notations to prevent counterfeiting
            [message stripSilentCircleData];
      
            NSXMLElement *scElement = [NSXMLElement elementWithName:kSCPPPubSiren xmlns:kSCPPNameSpace];
            [message addChild:scElement];
            [scElement setStringValue:scKeyBody];
            
        }
            break;
            
#pragma mark kSCimpEvent_NeedsPrivKey
            
            /*
             it's better for us to not specify the public key ahdead of time and allow the remote
             to ask us for the public key they want to start a conversation with
             */

    case kSCimpEvent_NeedsPrivKey:
        {
            SCimpEventNeedsPrivKeyData  *d =  &event->data.needsKeyData;
            
             NSString* pkString = [self.storage privateKeyForLocator: [NSString stringWithUTF8String:d->locator]];
            if(pkString)
            {
                SCKeyContextRef privKey = kInvalidSCKeyContextRef;
                SCKeyContextRef storageKey = self.storage.storageKey;
                
                if(storageKey
                   && IsntSCLError(SCKeyDeserialize((uint8_t*)pkString.UTF8String,  pkString.length, &privKey))
                   && SCKeyContextRefIsValid(privKey))
                {
                    bool isLocked = true;
                    
#warning CLEANUP THIS CODE VINNIE
                    // private key should be locked by storage Key
                    err = SCKeyIsLocked(privKey,&isLocked);
                    if(isLocked)
                    {
                        uint8_t symKey[64];
                        size_t  symKeyLen = 0;
                        size_t  ivLen = 0;
                        
                        err = SCKeyGetProperty(storageKey, kSCKeyProp_SymmetricKey, NULL, symKey , sizeof(symKey), &symKeyLen);
                        err = SCKeyGetProperty(storageKey, kSCKeyProp_IV, NULL,  symKey+symKeyLen , symKeyLen, &ivLen);
                        err = SCKeyUnlock(privKey, symKey, symKeyLen + ivLen);
                        
                        if (err != kSCLError_NoErr)
                        {
                            XMPPLogCError(@"%@: SCKeyUnlock error = %d", THIS_FILE, err);
                        }
                        
                        ZERO(symKey, sizeof(symKey));
                    }
                    
                    if(IsntSCLError(err))
                        *(d->privKey) = privKey;
                   }
            }

             err = SCKeyContextRefIsValid( *(d->privKey))? kSCLError_NoErr: kSCLError_KeyNotFound;
            
            if(IsSCLError(err))
            {
                XMPPLogCError(@"%@: %s - kSCimpEvent_NeedsPrivKey : %s\n", THIS_FILE, __FUNCTION__ , d->locator);
                
                
              [self->multicastDelegate xmppSilentCircle:self protocolErrorForSCimpWrapper:scimp
                                          withMessage:(__bridge XMPPMessage*)event->userRef
                                                error:kSCLError_KeyNotFound ];
            }

        }
            break;

        case kSCimpEvent_Shutdown:
        {
            [self saveState:scimp];
            break;
         }

            
  
        default:
        {
            NSCAssert1(NO, @"Unhandled SCimpEvent: %d:", event->type );
            
            err =  kSCLError_LazyProgrammer;
            break;
        
        }
    }
  	
	return err;
}

#pragma mark

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark XMPPStream Delegate
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (XMPPMessage *)xmppStream:(XMPPStream *)sender willSendMessage:(XMPPMessage *)message
{
	XMPPLogTrace2(@"%@: %@ - %@", THIS_FILE, THIS_METHOD, [message compactXMLString]);
	
	// If we need to encrypt the message, then convert from this:
	//
	// <message to="remoteJid">
	//   <body>unencrypted-message</body>
	// </message>
	//
	// To this:
	//
	// <message to="remoteJid">
	//   <body></body>
	//   <x xmlns="http://silentcircle.com">base64-encoded-encrypted-message</x>
	// </message>
	
    if(!([message.name isEqualToString:@"message"] &&  message.isChatMessage))
    {
        return message;

    }
        
        
	NSXMLElement *x = [message elementForName:@"x" xmlns:kSCPPNameSpace];
	if (x)
	{
		XMPPLogVerbose(@"%@: %@ - Ignoring. Already has encryption stuff", THIS_FILE, THIS_METHOD);
		
		return message;
	}

    if([message isMulticast] )
    {
   
        NSString* messageID = [[message  attributeForName: kXMPPID]stringValue];
        NSString* threadID = message.threadElement;
        
        XMPPLogError(@"multicast %@ for %@: ",messageID, threadID );
        
        BOOL isNewSCimpContext = NO;
        SCimpWrapper *scimp = [self scimpForThread:threadID
                                            forJID:[sender myJID]
                                            toJids:message.jidStrings
                                             isNew:&isNewSCimpContext];
        
        // handle multicast encrypt
        [self encryptOutgoingMessage:message withScimp:scimp];
        return message;
    }
    else if([message elementForName:kSCPPPubSiren])
    {
        // just pass these through
        XMPPJID *messageJid = [message to];

        NSString* pkString = [storage publicKeyForRemoteJid:messageJid.bare];
      
        if(![self encryptPubKeyMessage:message pkString:pkString])
            message = NULL;
        
         return message;
    }
    else
    {
        
        /////  NOT SURE WHY THIS CODE IS HERE?
        XMPPJID *messageJid = [message to];
        if (messageJid == nil)
        {
            messageJid = [[sender myJID] domainJID];
        }
        
        // find prefered resource for this conversation in order to lookup scimp context        
        if(!messageJid.resource)
        {
            NSString* conversationID = [storage conversationIDForRemoteJid:messageJid localJid:myJID];
            if(conversationID)
            {
                XMPPJID * lookupJid =  [storage remoteJIDforConversationID:conversationID];
                
                if(lookupJid) messageJid = lookupJid;
                
            }
        }
        
        // Encrypt message
        
        BOOL isNewSCimpContext = NO;
        SCimpWrapper *scimp = [self scimpForJid:messageJid isNew:&isNewSCimpContext];
        
        if (scimp == nil)
        {
            XMPPLogVerbose(@"%@: %@ - scimp == nil", THIS_FILE, THIS_METHOD);
            
            // Some error occurred while creating a scimp context.
            // The error was logged in scimpForJid.
            // But since this message is expected to be encrypted,
            // it's better to drop it than send it in plain text.
            
            return nil;
        }
        else if (isNewSCimpContext)
        {
            SCimpInfo info;
            
            XMPPLogVerbose(@"%@: %@ - isNewSCimpContext", THIS_FILE, THIS_METHOD);
            
            // if thread element add it to the scimp record
            if(message.threadElement)
                scimp->threadElement =  message.threadElement;
            
            // inform delegate that we are about to establish secure context
            [multicastDelegate xmppSilentCircle:self willEstablishSecureContextForSCimpWrapper: scimp];
      
            ZERO(&info, sizeof(info));
            
            SCimpGetInfo(scimp->scimpCtx, &info);
            NSString* pkString = [storage publicKeyForRemoteJid:messageJid.bare];
          
            if(info.canPKstart && pkString)
            {
                SCKeyContextRef remoteKey = kInvalidSCKeyContextRef;
                time_t          expireDate  = time(NULL) + (3600 * 24);
                  
                SCKeyDeserialize((uint8_t*) pkString.UTF8String,  pkString.length, &remoteKey);
                
                // use PGP SAS for PKstarts
                SCimpSetNumericProperty(scimp->scimpCtx,kSCimpProperty_SASMethod, kSCimpSAS_PGP);  
                
                XMPPLogVerbose(@"%@: %@ - PK Start...", THIS_FILE, THIS_METHOD);
               if(IsntSCLError( SCimpStartPublicKey(scimp->scimpCtx, remoteKey, expireDate))) 
               {
                   [self->multicastDelegate xmppSilentCircle:self didStartPublicKeyForSCimpWrapper:scimp  ];

                   [self encryptOutgoingMessage:message withScimp:scimp];
                   return message;
                }
  
            }
            else
                
            {
                // Stash message, and handle it after the scimp stack is ready.
                [scimp->pendingOutgoingMessages addObject:message];
                [self saveState:scimp];
 
                  // SCimp context was just created.
                // We need to start the key exchange.
                SCimpStartDH(scimp->scimpCtx);
                
                return nil;

            }
             
        }
        else if ([scimp isReady] == NO)
        {
            XMPPLogVerbose(@"%@: %@ - [scimp isReady] == NO", THIS_FILE, THIS_METHOD);
            
            // SCimp stack isn't ready yet.
            // Key exchange is in progress.
            
            // Stash message, and handle it after the scimp stack is ready.
            [scimp->pendingOutgoingMessages addObject:message];
            [self saveState:scimp];

            return nil;
        }
        else
        {
            XMPPLogVerbose(@"%@: %@ - encrypting message...", THIS_FILE, THIS_METHOD);
            
            // SCimp stack is ready.
            // Perform encryption.
            
            [self encryptOutgoingMessage:message withScimp:scimp];
            return message;
        }
    }
    
    return message;

}

- (XMPPMessage *)xmppStream:(XMPPStream *)sender willReceiveMessage:(XMPPMessage *)message
{
	XMPPLogTrace2(@"%@: %@ - %@", THIS_FILE, THIS_METHOD, [message compactXMLString]);
	
	// We're looking for messages like this:
	//
	// <message to="remoteJid">
	//   <body></body>
	//   <x xmlns="http://silentcircle.com">base64-encoded-encrypted-message</x>
	// </message>
	

	BOOL hasContent = NO;
    
    
	NSXMLElement *r_error = [message elementForName:@"error"];
    
    if(r_error)
    {
     	XMPPLogVerbose(@"%@: %@ - Error message recieved from XMPP", THIS_FILE, THIS_METHOD);
        
        return message;
  
    }
    
    NSString *mcKeySpace = [[message elementForName:@"x" xmlns:kSCPPpublicKeyNameSpace] stringValue];
 	if (mcKeySpace )
	{
        SCLError err = SCimpProcessPacket(pubScimpCtx, (uint8_t *)mcKeySpace.UTF8String, mcKeySpace.length, (__bridge void*) message);
        if (err != kSCLError_NoErr)
        {
            XMPPLogError(@"%@: %@ - SCimpProcessPacket err = %d", THIS_FILE, THIS_METHOD, err);
        }
        else
        {
            
            hasContent = (([message elementForName: kSCPPSiren xmlns: kSCPPNameSpace] != NULL)
                          || ([message elementForName: kSCPPPubSiren xmlns: kSCPPNameSpace] != NULL) );
            
        }
    
        return hasContent ? message : nil;
	}

    NSString *x = [[message elementForName:@"x" xmlns:kSCPPNameSpace] stringValue];
 	if (x == nil)
	{
		XMPPLogVerbose(@"%@: %@ - Nothing to decrypt", THIS_FILE, THIS_METHOD);
		return message;
	}
	
	XMPPJID *messageJid = [message from];
	if (messageJid == nil)
	{
		messageJid = [[sender myJID] domainJID];
	}

 	BOOL isNewSCimpContext = NO;
    
    SCimpWrapper *scimp = NULL;
    
    if(message.threadElement)  // is it multicast
    {
        NSMutableArray* messageJids =  [NSMutableArray arrayWithArray:message.jidStrings];
          
        [messageJids addObject:messageJid.bare];               // add the sender
        [messageJids removeObject:sender.myJID.bare];           // remove myself
   
        scimp = [self scimpForThread: message.threadElement forJID:[sender myJID] toJids:messageJids isNew:&isNewSCimpContext];
     }
  	else
    {
        scimp = [self scimpForJid:messageJid isNew:&isNewSCimpContext];
	}
    
	if (scimp == nil)
	{
		XMPPLogVerbose(@"%@: %@ - scimp == nil", THIS_FILE, THIS_METHOD);
		
		// Some error occurred while creating a scimp context.
		// The error was logged in scimpForJid.
		
		hasContent = YES;
	}
	else
    {
          XMPPLogVerbose(@"Incoming encrypted data: %@", x);

        if ([x length] > 0)
        {
            // How does this work?
            // We invoke the SCimpProcessPacket, which immediately turns around and invokes our XMPPSCimpEventHandler.
            // The event handler is given the decrypted data, which it will inject into the message.
             
            SCLError err = SCimpProcessPacket(scimp->scimpCtx, (uint8_t *)x.UTF8String, x.length, (__bridge void*) message);
            if (err != kSCLError_NoErr)
            {
                XMPPLogError(@"%@: %@ - SCimpProcessPacket err = %d", THIS_FILE, THIS_METHOD, err);
            }
            else
            {
                hasContent = (([message elementForName: kSCPPSiren xmlns: kSCPPNameSpace] != NULL)
                              || ([message elementForName: kSCPPPubSiren xmlns: kSCPPNameSpace] != NULL) );
                
            }
            
        }
         
    }
      
    return hasContent ? message : nil;
}
 
#ifdef _XMPP_CAPABILITIES_H
/**
 * If an XMPPCapabilites instance is used we want to advertise our support for SCimp.
**/
- (void)xmppCapabilities:(XMPPCapabilities *)sender collectingMyCapabilities:(NSXMLElement *)query
{
	// This method is invoked on the moduleQueue.
	
	// Add the SCimp feature to the list.
	//   
	// <query xmlns="http://jabber.org/protocol/disco#info">
	//   ...
	//   <feature var="http://silentcircle.com"/>
	//   ...
	// </query>
	//
	// From XEP=0115:
	//   It is RECOMMENDED for the value of the 'node' attribute to be an HTTP URL at which a user could find
	//   further information about the software product, such as "http://psi-im.org" for the Psi client;
	
	NSXMLElement *feature = [NSXMLElement elementWithName:@"feature"];
	[feature addAttributeWithName:@"var" stringValue:kSCPPNameSpace];
	
	[query addChild:feature];
}
#endif

@end

