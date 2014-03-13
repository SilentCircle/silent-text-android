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
//  SCAccountsWebManager.m
//  ST2
//
//  Created by Vinnie Moscaritolo on 7/8/13.
//

#import "SCAccountsWebAPIManager.h"
#import "AppDelegate.h"
#import "AppConstants.h"
#import "STPublicKey.h"
#import "STUser.h"
#import "YapCollectionsDatabase.h"
#import "YapCollectionsDatabaseView.h"
#import "DDLog.h"
#import "SCSRVResolver.h"
#import "SCWebRequest.h"
#import <SystemConfiguration/SystemConfiguration.h>
#import "NSDate+SCDate.h"
#import "STUser.h"
#import "NSString+SCUtilities.h"

// Log levels: off, error, warn, info, verbose

#if DEBUG
static const int ddLogLevel = LOG_LEVEL_VERBOSE;
#else
static const int ddLogLevel = LOG_LEVEL_WARN;
#endif

static  NSString*   debugbrokerURLString =   @"https://accounts-testing.silentcircle.com";

@implementation SCAccountsWebAPIManager 
{
    NSOperationQueue    *opQueue;
    SCSRVResolver       *srvResolver;
    NSString            *brokerURLString;
    dispatch_queue_t    resolverQueue;

}

static NSString * OperationsChangedContext = @"OperationsChangedContext";

static SCAccountsWebAPIManager *sharedInstance;

+ (void)initialize
{
	static BOOL initialized = NO;
	if (!initialized)
	{
		initialized = YES;
		sharedInstance = [[SCAccountsWebAPIManager alloc] init];
        [sharedInstance commonInit];
  	}
}

+ (SCAccountsWebAPIManager *)sharedInstance
{
	return sharedInstance;
}

- (void)dealloc
{
    [opQueue removeObserver:self forKeyPath:@"operations"];
    opQueue = NULL;
    resolverQueue = NULL;
    
    //    dispatch_release(_resolverQueue);
    
}


- (void)commonInit
{
    
    opQueue = [[NSOperationQueue alloc] init];
    
    // Set to 1 to serialize operations. Comment out for parallel operations.
    [opQueue setMaxConcurrentOperationCount:4];
    [opQueue setSuspended:YES];
    
    [opQueue addObserver:self
              forKeyPath:@"operations"
                 options:0
                 context:&OperationsChangedContext];
    
    
    resolverQueue = dispatch_queue_create("broker.silentcircle.com", NULL);
    
    [self updateSrvCache];
}
 
#pragma mark - SCSRVResolver methods

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context
{
    if (context == &OperationsChangedContext)
    {
        
        DDLogVerbose(@"Queue size: %d", (int)[[opQueue operations] count]);
    }
    else
    {
        [super observeValueForKeyPath:keyPath
                             ofObject:object
                               change:change
                              context:context];
    }
}


#pragma mark - SCSRVResolver methods

-(void) updateSrvCache
{
    srvResolver = [[SCSRVResolver alloc] initWithdDelegate:self
                                              delegateQueue:resolverQueue
                                              resolverQueue:NULL];
    
    [srvResolver startWithSRVName:kSCBrokerSRVname timeout:30.0];
    
    
}

- (void)srvResolver:(SCSRVResolver *)sender didResolveRecords:(NSArray *)srvResults
{
    BOOL found = NO;
    
    if (sender != srvResolver) return;
 	
    for(SCSRVRecord* srvRecord in srvResults)
    {
        
        NSString *srvHost = srvRecord.target;
		UInt16      srvPort = srvRecord.port;
        
        SCNetworkReachabilityRef reachabilityRef = SCNetworkReachabilityCreateWithName(NULL, [srvHost UTF8String]);
        if(reachabilityRef!= NULL)
        {
            
            SCNetworkReachabilityFlags flags;
            if (SCNetworkReachabilityGetFlags(reachabilityRef, &flags)
                && (flags & kSCNetworkReachabilityFlagsReachable))
            {
                brokerURLString = (srvPort == 443)
                ?  [NSString stringWithFormat:@"https://%@", srvHost] 
                :  [NSString stringWithFormat:@"http://%@:%d", srvHost, srvPort] ;
                found = TRUE;
            }
            
            
            CFRelease(reachabilityRef);
        }
        if(found) break;
    }
    
    
    if(!found)
        brokerURLString =  @"https://accounts.silentcircle.com" ;
   
#warning USE DEBUG ACCOUNTS SERVER
    //// for testing
//    brokerURLString =   @"https://accounts-testing.silentcircle.com"  ;
 /////
    
    
    [opQueue setSuspended:NO];
    
 }


- (void)srvResolver:(SCSRVResolver *)sender didNotResolveDueToError:(NSError *)error
{
    
    if (sender != srvResolver)
        return;
    
    [opQueue setSuspended:YES];
    
    brokerURLString = NULL;
}



#pragma mark - SCWebRequest methods

-(void) provisionWithCode:(NSString*)activationCode
               deviceName:(NSString*)deviceName
                 deviceID:(NSString*)deviceID
             debugNetwork:(BOOL)debugNetwork
          completionBlock:(SCAccountsWebAPICompletionBlock)completion
{
   
    DDLogVerbose(@"SCWebRequest provisionWithCode" );
    
    NSDictionary*  dict  = @{
                             @"provisioning_code": activationCode,
                             @"device_name": deviceName
                             };
    
    
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/me/device/%@/",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                           [deviceID urlEncodeString:NSASCIIStringEncoding] ];
    
    
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"PUT"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: debugNetwork?NO:YES 
                                                        requestDict: dict
                                                             object: completion];
    
    [opQueue addOperation:operation];
    

}
-(void) provisionUser:(NSString*)userName
         withPassword:(NSString*)password
           deviceName:(NSString*)deviceName
             deviceID:(NSString*)deviceID
         debugNetwork:(BOOL)debugNetwork
     completionBlock:(SCAccountsWebAPICompletionBlock)completion

{
    DDLogVerbose(@"SCWebRequest provisionUser" );
  
    NSDictionary*  dict  = @{
                             @"username": userName,
                             @"password": password,
                             @"device_name": deviceName
                             };
     
      
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/me/device/%@/",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                           [deviceID urlEncodeString:NSASCIIStringEncoding] ];

    
     SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"PUT"
                                                                url: [NSURL URLWithString:urlstring]
                                                     shouldCheckCert: debugNetwork?NO:YES
                                                         requestDict: dict
                                                             object: completion];
    
    [opQueue addOperation:operation];

}

-(void) getConfigForDeviceID:(NSString*)deviceID
                      apiKey:(NSString*)apiKey
                debugNetwork:(BOOL)debugNetwork
              completionBlock:(SCAccountsWebAPICompletionBlock)completion;
 {
     
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/me/device/%@/?api_key=%@",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                           [deviceID urlEncodeString:NSASCIIStringEncoding] ,
                           [apiKey urlEncodeString:NSASCIIStringEncoding]];
    
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"GET"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: debugNetwork?NO:YES
                                                          requestDict: NULL
                                                             object: completion];
    
    [opQueue addOperation:operation];
   
}


-(void) uploadPublicKeyKeyWithLocator:(NSString*)locator
                            keyString:(NSString*)keyString
                              forUser:(STUser*)user
                      completionBlock:(SCAccountsWebAPICompletionBlock)completion

{

    DDLogVerbose(@"SCWebRequest uploadPublicKey" );
       
    NSError *error;
    NSDictionary *jsonDict = [NSJSONSerialization JSONObjectWithData:[keyString dataUsingEncoding:NSUTF8StringEncoding] options:0 error:&error];

    BOOL debugNetwork = user.onDebugNetwork;

    
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/pubkey/%@/?api_key=%@",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                           [locator urlEncodeString:NSASCIIStringEncoding] ,
                           [user.apiKey urlEncodeString:NSASCIIStringEncoding]];
     
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"PUT"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: debugNetwork?NO:YES
                                                        requestDict: jsonDict
                                                             object: completion];
    [opQueue addOperation:operation];

}



-(void) removeKeyWithLocator:(NSString*)locator
                     forUser:(STUser*)user
             completionBlock:(SCAccountsWebAPICompletionBlock)completion
{
    
    DDLogVerbose(@"SCWebRequest removePublicKey %@" , locator);
  
    BOOL debugNetwork = user.onDebugNetwork;

    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/pubkey/%@/?api_key=%@",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                          [locator urlEncodeString:NSASCIIStringEncoding] ,
                           [user.apiKey urlEncodeString:NSASCIIStringEncoding]];

      SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"DELETE"
                                                                url: [NSURL URLWithString:urlstring]
                                                      shouldCheckCert: debugNetwork?NO:YES
                                                          requestDict: NULL
                                                             object: completion];

    [opQueue addOperation:operation];
  
}

-(void) getKeyWithLocator:(NSString*)locator
                     forUser:(STUser*)user
             completionBlock:(SCAccountsWebAPICompletionBlock)completion
{
    
    DDLogVerbose(@"SCWebRequest getKeyWithLocator %@" , locator);
 
    BOOL debugNetwork = user.onDebugNetwork;

    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/pubkey/%@/?api_key=%@",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                           [locator urlEncodeString:NSASCIIStringEncoding] ,
                           [user.apiKey urlEncodeString:NSASCIIStringEncoding]];
    
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"GET"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: debugNetwork?NO:YES
                                                        requestDict: NULL
                                                             object: completion];
    
    [opQueue addOperation:operation];
    
}



-(void) getUserInfo:(NSString*)userName
            forUser:(STUser*)user
    completionBlock:(SCAccountsWebAPICompletionBlock)completion
{
    
    DDLogVerbose(@"SCWebRequest getUserInfo %@" , userName);
    
    BOOL debugNetwork = user.onDebugNetwork;
    
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/user/%@/?api_key=%@",
                           debugNetwork? debugbrokerURLString: brokerURLString,
                           [userName urlEncodeString:NSASCIIStringEncoding] ,
                           [user.apiKey urlEncodeString:NSASCIIStringEncoding]];

    
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"GET"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: debugNetwork?NO:YES
                                                        requestDict: NULL
                                                             object: completion];

    [opQueue addOperation:operation];
}


-(void) setApplicationPushToken:(NSString*)pushToken
                        forUser:(STUser*)user
                completionBlock:(SCAccountsWebAPICompletionBlock)completion{
    
  
    DDLogVerbose(@"SCWebRequest setApplicationPushToken %@ for %@" ,pushToken,  user.jid);
    
    BOOL debugNetwork = user.onDebugNetwork;
    
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/me/device/%@/pushtoken/silenttext/?api_key=%@",
                            debugNetwork? debugbrokerURLString: brokerURLString,
                           [user.deviceID urlEncodeString:NSASCIIStringEncoding],
                           [user.apiKey urlEncodeString:NSASCIIStringEncoding]];
  
    NSDictionary*  dict  = @{
                             @"service":    @"apns",
                             @"token":      pushToken
                             };
    
    
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"PUT"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: debugNetwork?NO:YES
                                                        requestDict: dict
                                                             object: completion];
      
    [opQueue addOperation:operation];
}


-(void) removeApplicationPushTokenforUser:(STUser*)user
                completionBlock:(SCAccountsWebAPICompletionBlock)completion{
    
    DDLogVerbose(@"SCWebRequest setApplicationPushToken %@",  user.jid);
    
    NSString* urlstring = [NSString stringWithFormat:@"%@/v1/me/device/%@/pushtoken/silenttext/?api_key=%@",
                           brokerURLString,
                           [user.deviceID urlEncodeString:NSASCIIStringEncoding],
                           [user.apiKey urlEncodeString:NSASCIIStringEncoding]];
      
    
    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
                                                         httpMethod: @"DELETE"
                                                                url: [NSURL URLWithString:urlstring]
                                                    shouldCheckCert: YES
                                                        requestDict: NULL
                                                             object: completion];
    
    [opQueue addOperation:operation];
}

-(void) testBrokerWithUser:(STUser *)user
{
    NSString* dateSting = [[NSDate dateWithTimeIntervalSinceNow:3600*10] rfc3339String];
                           
    DDLogVerbose(@"SCWebRequest upload" );
    
    NSDictionary* dict  = [NSDictionary dictionaryWithObjectsAndKeys:
                                  user.apiKey , @"api_key",
                                  @"upload", @"operation",
                                  [NSDictionary dictionaryWithObjectsAndKeys:
                                   [NSDictionary dictionaryWithObjectsAndKeys:dateSting, @"shred_date", @65536, @"size", nil ], @"Dv0CobNmBNO6ZfF7JYpZLuauKTU",
                                   [NSDictionary dictionaryWithObjectsAndKeys:dateSting, @"shred_date", @65536, @"size", nil ], @"GDJVkOI0irPZtmI0YVKPcfnOoL4",
                                   [NSDictionary dictionaryWithObjectsAndKeys:dateSting, @"shred_date", @1728, @"size", nil ], @"HSqAokWuFFBSdByzelS3AHfAPjI",
                                   [NSDictionary dictionaryWithObjectsAndKeys:dateSting, @"shred_date", @65536, @"size", nil ], @"TXoanbFReYth2Th4yesDp--G7_Q",
                                   [NSDictionary dictionaryWithObjectsAndKeys:dateSting, @"shred_date", @65536, @"size", nil ], @"WHUnEScbw4dnE6b4QxKvW75h3uY",
                                   nil],
                                  @"files",
                                  nil];

//    SCWebRequest * operation = [SCWebRequest.alloc initWithDelegate: self
//                                                         httpMethod: @"POST"
//                                                          urlString: @"broker/"
//                                                        requestDict: dict
//                                                             object: NULL];
    
  
 //   [opQueue addOperation:operation];
    
    DDLogVerbose(@"SCWebRequest delete" );
    
    dict  = [NSDictionary dictionaryWithObjectsAndKeys:
                    user.apiKey , @"api_key",
                    @"delete", @"operation",
                    [NSDictionary dictionaryWithObjectsAndKeys:
                     [NSDictionary dictionaryWithObjectsAndKeys:nil ], @"Dv0CobNmBNO6ZfF7JYpZLuauKTU",
                     [NSDictionary dictionaryWithObjectsAndKeys: nil ], @"GDJVkOI0irPZtmI0YVKPcfnOoL4",
                     [NSDictionary dictionaryWithObjectsAndKeys:nil ], @"HSqAokWuFFBSdByzelS3AHfAPjI",
                     [NSDictionary dictionaryWithObjectsAndKeys: nil ], @"TXoanbFReYth2Th4yesDp--G7_Q",
                     [NSDictionary dictionaryWithObjectsAndKeys: nil ], @"WHUnEScbw4dnE6b4QxKvW75h3uY",
                     nil],
                    @"files",
                    nil];
    
//    operation = [SCWebRequest.alloc initWithDelegate: self
//                                                         httpMethod: @"POST"
//                                                          urlString: @"broker/"
//                                                        requestDict: dict
//                                                             object: NULL];
// 
 //   [opQueue addOperation:operation];
    
}
 
-(void)SCWebRequest:(SCWebRequest *)sender requestDidStart:(NSDictionary*)brokerRequest
{
    DDLogVerbose(@"SCWebRequest requestDidStart" );
  
}

- (void)SCWebRequest:(SCWebRequest *)sender requestCompletedWithWithError:(NSError *)error
                request:(NSDictionary*)brokerRequest
             totalBytes:(size_t)totalBytes
                   info:(NSDictionary*)info
{
     if(sender.userObject)
    {
        SCAccountsWebAPICompletionBlock completionBlock = sender.userObject ;
        
        (completionBlock)(error, info);
      
    }
      
}



@end
