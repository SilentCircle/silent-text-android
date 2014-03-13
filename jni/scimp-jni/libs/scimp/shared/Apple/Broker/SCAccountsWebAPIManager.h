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
//  SCAccountsWebManager.h
//  ST2
//
//  Created by Vinnie Moscaritolo on 7/8/13.
//  Copyright (c) 2013 Silent Circle LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

@class STUser;
@class STPublicKey;

typedef void (^SCAccountsWebAPICompletionBlock)(NSError *error, NSDictionary* infoDict);

@interface SCAccountsWebAPIManager : NSObject

+ (SCAccountsWebAPIManager *)sharedInstance;

-(void) updateSrvCache;

-(void) testBrokerWithUser:(STUser*)user;

-(void) uploadPublicKeyKeyWithLocator:(NSString*)locator
                            keyString:(NSString*)keyString
                              forUser:(STUser*)user
                      completionBlock:(SCAccountsWebAPICompletionBlock)completion;


-(void) removeKeyWithLocator:(NSString*)locator forUser:(STUser*)user
             completionBlock:(SCAccountsWebAPICompletionBlock)completion;

-(void) getKeyWithLocator:(NSString*)locator
                  forUser:(STUser*)user
          completionBlock:(SCAccountsWebAPICompletionBlock)completion;

-(void) provisionUser:(NSString*)userName
         withPassword:(NSString*)password
           deviceName:(NSString*)deviceName
             deviceID:(NSString*)deviceID
         debugNetwork:(BOOL)debugNetwork
        completionBlock:(SCAccountsWebAPICompletionBlock)completion;


-(void) provisionWithCode:(NSString*)activationCode
           deviceName:(NSString*)deviceName
             deviceID:(NSString*)deviceID
             debugNetwork:(BOOL)debugNetwork
      completionBlock:(SCAccountsWebAPICompletionBlock)completion;


 -(void) getConfigForDeviceID:(NSString*)deviceID
                  apiKey:(NSString*)apiKey
                 debugNetwork:(BOOL)debugNetwork
   completionBlock:(SCAccountsWebAPICompletionBlock)completion;

-(void) getUserInfo:(NSString*)userName
            forUser:(STUser*)user
    completionBlock:(SCAccountsWebAPICompletionBlock)completion;

-(void) setApplicationPushToken:(NSString*)deviceToken
                        forUser:(STUser*)user
                completionBlock:(SCAccountsWebAPICompletionBlock)completion;

-(void) removeApplicationPushTokenforUser:(STUser*)user
                          completionBlock:(SCAccountsWebAPICompletionBlock)completion;

@end
@protocol SCAccountsWebAPIManagerDelegate <NSObject>

@optional


- (void)SCAccountsWebDidCompleteWithError:(NSError *)error dict:(NSDictionary*) dict ;

- (void)SCAccountsWebDidStart;
 
@end
