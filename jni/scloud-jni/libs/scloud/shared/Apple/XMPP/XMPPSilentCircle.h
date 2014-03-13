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

#import <Foundation/Foundation.h>
#import "XMPP.h"
#import <SCimp.h>
#import "SilentTextLib.h"



#define _XMPP_SILENT_CIRCLE_H

/**
 * This module implements the Silent Circle Instant Messaging Protocol (SCimp).
 * It can automatically encrypt / decrypt messages between peers.
**/
@protocol XMPPSilentCircleStorageDelegate;

@interface XMPPSilentCircle : XMPPModule

- (id)initWithStorage:(id <XMPPSilentCircleStorageDelegate>)storage;
- (id)initWithStorage:(id <XMPPSilentCircleStorageDelegate>)storage dispatchQueue:(dispatch_queue_t)queue;

@property (nonatomic, strong, readonly) id <XMPPSilentCircleStorageDelegate> storage;

@property (nonatomic) SCimpCipherSuite scimpCipherSuite;
@property (nonatomic) SCimpSAS scimpSASMethod;


/**
 * Whether or not the module has an active secure context for communication with the remote jid.
 * If this method returns no, then a secure context will either need to be restored or
 * a new secure context will need to be negotiated.
 * 
 * The given remoteJid must be a full jid (i.e. jid must contain a resource).
**/
- (BOOL)hasSecureContextForSCimpID:(NSString *)scimpID;

/* Restart the keying process for the remote JID */
- (void)rekeySecureContextForSCimpID:(NSString *)scimpID withReset:(BOOL) withReset;

 - (void)acceptSharedSecretForSCimpID:(NSString *)scimpID;

- (void)removeSecureContextForSCimpID:(NSString *)scimpID;

-(void) removeAllSecureContexts;
-(void) saveAllSecureContexts;
@end


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@protocol XMPPSilentCircleDelegate
@optional

/**
 * 
**/
- (void)xmppSilentCircle:(XMPPSilentCircle *)sender willEstablishSecureContextForSCimpWrapper:(SCimpWrapper*)scimp;

/**
 * This method is invoked after a secure context has been negotiated, or during rekeying.
 * It is also invoked after a stored secured context has been restored.
**/

extern NSString  *const kSCIMPInfoCipherSuite;
extern NSString  *const kSCIMPInfoVersion;
extern NSString  *const kSCIMPInfoSASMethod;
extern NSString  *const kSCIMPInfoSAS;
extern NSString  *const kSCIMPInfoCSMatch;
extern NSString  *const kSCIMPInfoHasCS;

- (void)xmppSilentCircle:(XMPPSilentCircle *)sender didEstablishSecureContextForSCimpWrapper:(SCimpWrapper*)scimp withInfo: (NSDictionary*)info;

- (void)xmppSilentCircle:(XMPPSilentCircle *)sender didStartPublicKeyForSCimpWrapper:(SCimpWrapper*)scimp ;
 

/**
 * protocolWarning  indicates that the other side did not respond to estalishing secure context or the
 other side responded out of order, depending on the error, the appropriate action is probably to rekey.
 **/

- (void)xmppSilentCircle:(XMPPSilentCircle *)sender protocolWarningForSCimpWrapper:(SCimpWrapper*)scimp withMessage:(XMPPMessage *)message error:(SCLError)error;

/**
 * protocolError  indicates that the SCIMP was unable to make a secure context
 **/

- (void)xmppSilentCircle:(XMPPSilentCircle *)sender protocolErrorForSCimpWrapper:(SCimpWrapper*)scimp withMessage:(XMPPMessage *)message error:(SCLError)error;

/**
 * protocolStateChange  used to track the state change in a secure context
 **/

- (void)xmppSilentCircle:(XMPPSilentCircle *)sender protocolDidChangeStateForSCimpWrapper:(SCimpWrapper*)scimp state:(SCimpState)state;


 

@end
