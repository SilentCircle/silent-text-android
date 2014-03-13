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
//
//  XMPPSilentCircleStorage.h
  
#import <Foundation/Foundation.h>

#import "XMPPJID.h"
#import <SCimp.h>

@interface SCimpWrapper : NSObject {
@public
    NSString* conversationID;
    NSString* scimpID;
    
	XMPPJID *localJid;
	XMPPJID *remoteJid;
    
    NSString* threadElement;
    NSArray* multicastJids;
    
	NSMutableArray *pendingOutgoingMessages;
    
	void*       scimpCtx;    /* scimp context */
}

- (BOOL)isReady;

- (id)initWithLocalJid:(XMPPJID *)myJid remoteJID:(XMPPJID *)theirJid scimpCtx:(SCimpContextRef)ctx;
- (id)initWithThread:(NSString *)threadID localJID:(XMPPJID *)myJid scimpCtx:(SCimpContextRef)ctx;

- (NSString*) updateScimpID;

+ (NSString*) searchKeyWithLocalJid:(XMPPJID *)myJid remoteJID:(XMPPJID *)theirJid options:(XMPPJIDCompareOptions)options;
+ (NSString*) threadHash :(NSString *)thread localJID:(XMPPJID *)localJID;

@end;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@protocol XMPPSilentCircleStorageDelegate
@required

/**
 * In order to save and restore state, a key is used to encrypt the saved state,
 * and later used to decrypt saved state in order to restore it.
 *
 * If a key exists for this combination of JIDs, it should be returned.
 * Otherwise, a new key is to be generated, stored for this combination of JIDs, and then returned.
 **/
- (SCKeyContextRef)storageKey;

/**
 * Instructs the storage protocol to save session state data.
 * This is data coming from SCimpSaveState.
 **/

- (void)saveState:(NSData *)state withSCimpWrapper:(SCimpWrapper*)scimp;

/**
 * Instructs the storage protocol to retrieve previously saved state data.
 * This is data going to SCimpRestoreState.
 **/
 
- (BOOL )restoreStateAndPendingWithSCimpID:(NSString *)scimpID state:(NSData **)state pending:(NSArray**)pending;

- (BOOL )restoreSCimpWrapperWithSCimpID:(NSString *)scimpID scimp:(SCimpWrapper **)scimpout  state:(NSData **)inState;

- (void)removeStateWithSCimpID:(NSString *)scimpID;

- (XMPPJID*) remoteJIDforConversationID:(NSString*)conversationID;

- (NSString*) multicastKeyforConversationID:(NSString*)conversationID;

- (NSString*) multicastKeyforThreadID:(NSString*)threadID;

- (NSString*) conversationIDForRemoteJid:(XMPPJID*)remoteJid localJid:(XMPPJID*)myJid;

- (NSString*) conversationIDForForMultiCast:(NSString *)threadID localJid:(XMPPJID*)myJid;

- (void)saveMulticastKey:(NSString*)keyString forJid:(NSString*)localJID;
 
- (NSString*) publicKeyForRemoteJid:(NSString*)remoteJID;

- (NSString*) privateKeyForJid:(NSString*)localJID;

- (NSString*) privateKeyForLocator:(NSString*)locator;


@end
