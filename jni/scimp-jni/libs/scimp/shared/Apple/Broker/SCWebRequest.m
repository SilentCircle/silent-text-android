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
//  SCWebRequest
//  SilentText
//

#include "SCpubTypes.h"

#import "AppConstants.h"
#import "AppDelegate.h"
#import "StorageCipher.h"
#import <CommonCrypto/CommonDigest.h>

#import "RequestUtils.h"
#import "SCWebRequest.h"
#import "NSDate+SCDate.h"


#define NSLS_COMMON_CERT_FAILED  NSLocalizedString(@"Certificate match error -- please update to the latest version of Silent Text", @"Certificate match error -- please update to the latest version of Silent Text")


#define NSLS_COMMON_PROVISION_ERROR_DETAIL NSLocalizedString(@"Please contact Silent Circle customer support with error code: \"%@\".",\
@"Please contact Silent Circle customer support with error code: \"%@\".")


static const NSInteger kMaxRetries  = 3;

NSString *const kSCBroker_SignedURL      = @"signedURL";
 
static const NSTimeInterval kBrokerTimeoutValue  = 30.0;

 
 
@implementation SCWebRequest
{
    
    BOOL           isExecuting;
    BOOL           isFinished;
    
    NSString*           httpMethod;
    NSURL*              webURL;
    NSDictionary*       webRequest;
    
    NSInteger           statusCode;
    NSURLConnection*    connection;
    NSData              *requestData;
    NSMutableData*      responseData;
    size_t              totalSize;
    
    NSURLRequest        *request;
    NSUInteger          attempts;
    BOOL                shouldCheckCert;
    
    
}
@synthesize delegate;
@synthesize userObject;

 
#pragma mark - Class Lifecycle

-(id)initWithDelegate: (id)aDelegate
           httpMethod: (NSString *)methodIn
                  url: (NSURL *)urlIn
      shouldCheckCert: (BOOL)shouldCheckCertIn
         requestDict:  (NSDictionary*)requestDictIn
               object: (id)anObject 
{
    self = [super init];
    if (self)
    {
        delegate       = aDelegate;
        httpMethod     = methodIn;
        webURL         = urlIn;
        webRequest     = requestDictIn;
        userObject     = anObject;
        attempts       = 0;
        shouldCheckCert = shouldCheckCertIn;
           
        isExecuting = NO;
        isFinished  = NO;
        totalSize = 0;
    }
    
    return self;
}

-(void)dealloc
{
    webURL  = NULL;
    delegate   = NULL;
    webRequest  = NULL;
    request    = NULL;
    requestData = NULL;

 }


-(NSData*) createWebRequestJSON
{

    NSData *data = ([NSJSONSerialization  isValidJSONObject: webRequest] ?
                           [NSJSONSerialization dataWithJSONObject: webRequest options: 0 error: nil] :
                           nil);
    
    return data;
}

-(NSURLRequest *)createWebRequestUrl
{
    NSMutableURLRequest* req    = NULL;
     
    NSBundle *main = NSBundle.mainBundle;
    NSString *version = [main objectForInfoDictionaryKey: @"CFBundleShortVersionString"];
    NSString *build   = [main objectForInfoDictionaryKey: (NSString *)kCFBundleVersionKey];
    build = build?build:@"XXX";
    NSString *userAgent = [NSString stringWithFormat: @"SilentText %@ (%@)", version, build];
    
    req = [ NSMutableURLRequest requestWithURL:webURL];
    [req setHTTPMethod:httpMethod];
    
    req.cachePolicy = NSURLRequestReloadIgnoringLocalAndRemoteCacheData;
    req.timeoutInterval = kBrokerTimeoutValue;
      
    if(webRequest)
        requestData = [self createWebRequestJSON  ];
    
    [req setValue:userAgent forHTTPHeaderField:@"User-Agent"];
    [req setHTTPBody: requestData];

    [req setValue:@"application/json" forHTTPHeaderField:@"Accept"];
    [req setValue:@"application/json; charset=utf-8" forHTTPHeaderField:@"Content-Type"];
    [req setValue:[NSString stringWithFormat:@"%ld", (unsigned long)[requestData length]] forHTTPHeaderField:@"Content-Length"];
  
    
 /// I am not sure if this is already done for us?
    NSString *preferredLanguageCodes = [[NSLocale preferredLanguages] componentsJoinedByString:@", "];
	[req setValue:[NSString stringWithFormat:@"%@, en-us;q=0.8", preferredLanguageCodes] forHTTPHeaderField:@"Accept-Language" ];
                                 
    return req;
}


#pragma mark - Overwriding NSOperation Methods
-(void)start
{
    // Makes sure that start method always runs on the main thread.
    if (![NSThread isMainThread])
    {
        [self performSelectorOnMainThread:@selector(start) withObject:nil waitUntilDone:NO];
        return;
    }
    
    [self willChangeValueForKey:@"isExecuting"];
    isExecuting = YES;
    [self didChangeValueForKey:@"isExecuting"];
    
    
    if(!webURL)
    {
        [self finish];
        return;
    }
     
    request = [self createWebRequestUrl];
    
    connection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
    
    if (connection == nil)
    {
        [self finish];
        return;
    }
    
    [self performSelectorOnMainThread:@selector(didStart) withObject:nil waitUntilDone:NO];
}

-(BOOL)isConcurrent
{
    return YES;
}

-(BOOL)isExecuting
{
    return isExecuting;
}

-(BOOL)isFinished
{
    return isFinished;
}


-(void)finish
{
    connection = nil;
    responseData = NULL;
    requestData = NULL;
  
    
  
    [self willChangeValueForKey:@"isExecuting"];
    [self willChangeValueForKey:@"isFinished"];
    
    isExecuting = NO;
    isFinished  = YES;
    
    [self didChangeValueForKey:@"isExecuting"];
    [self didChangeValueForKey:@"isFinished"];
}



#pragma mark - NSURLConnectionDelegate methods


- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *) response
{
    
    if ([response isKindOfClass: [NSHTTPURLResponse class]]) {
        statusCode = [(NSHTTPURLResponse*) response statusCode];
        /* HTTP Status Codes
         200 OK
         400 Bad Request
         401 Unauthorized (bad username or password)
         403 Forbidden
         404 Not Found
         502 Bad Gateway
         503 Service Unavailable
         */
    }
       
	responseData = [NSMutableData data];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    [responseData appendData:data];
}


- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    
    if(statusCode == 200)
    {
//        NSString *requestStr = connection.originalRequest.URL.absoluteString;
//        NSDictionary *dict = [requestStr URLQueryParameters];
 //       NSString    *requestID = [dict valueForKey:@"request_id"];
        
        [self completeBrokerRequestWithData:responseData ];
        
    }
    else
    {
        NSMutableDictionary* details = [NSMutableDictionary dictionary];
        NSString* error_message = [NSHTTPURLResponse localizedStringForStatusCode:(NSInteger)statusCode];
        
        if(responseData && (responseData.length > 0))
        {
            NSError *jsonError;
            
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:responseData options:0 error:&jsonError];
            if(!jsonError)
            {
                error_message = [dict objectForKey:@"error_msg"];
            }
            else
                error_message = [NSString.alloc  initWithBytes:responseData.bytes
                                                        length:responseData.length
                                                      encoding:NSUTF8StringEncoding];
                
        }
        else
            error_message = [NSString stringWithFormat:@"%@ - code:(%d)", error_message, statusCode];
        
        [details setValue: error_message forKey:NSLocalizedDescriptionKey];
        
        NSError * error =
            [NSError errorWithDomain:kSCErrorDomain code:statusCode userInfo:details];
        
        [self performSelectorOnMainThread:@selector(didCompleteWithError:) withObject:error waitUntilDone:NO];

    }
    [self finish];
}


- (void)connection:(NSURLConnection *)connectionIn didFailWithError:(NSError *)error
{
//    NSString *requestStr = connection.originalRequest.URL.absoluteString;
//    NSDictionary *dict = [requestStr URLQueryParameters];
    
     
    if(error.domain == NSURLErrorDomain && error.code == kCFURLErrorUserCancelledAuthentication)
    {
        NSMutableDictionary* details = [NSMutableDictionary dictionary];
        [details setValue:NSLS_COMMON_CERT_FAILED forKey:NSLocalizedDescriptionKey];
        
        error = [NSError errorWithDomain:NSURLErrorDomain code:kCFURLErrorClientCertificateRejected userInfo:details];
    }
    else  if(error && attempts++ < kMaxRetries)
    {
        connection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
        
        if(connection) return;
    }
      
    [self performSelectorOnMainThread:@selector(didCompleteWithError:) withObject:error waitUntilDone:NO];

    [self finish];
}


#pragma mark - Helpers methods

- (void)completeBrokerRequestWithError:( NSError*)error
{
    
};

- (void)completeBrokerRequestWithData:(NSData *)data 
{
    NSError *jsonError;
    
    NSDictionary *info = [NSJSONSerialization JSONObjectWithData:data options:0 error:&jsonError];
    
     if (jsonError==nil){
        
        NSString *string = nil;
        
        if ((string = [info valueForKey:@"error_msg"])) {
            
            NSMutableDictionary* details = [NSMutableDictionary dictionary];
            [details setValue:string forKey:NSLocalizedDescriptionKey];
            
            NSError * error =
            [NSError errorWithDomain:kSCErrorDomain code:NSURLErrorCannotConnectToHost userInfo:details];
            [self performSelectorOnMainThread:@selector(didCompleteWithError:) withObject:error waitUntilDone:NO];
        }
        else
        {
            [self performSelectorOnMainThread:@selector(didCompleteWithInfo:) withObject:info waitUntilDone:NO];
        }
    }else
    {
        [self performSelectorOnMainThread:@selector(didCompleteWithError:) withObject:jsonError waitUntilDone:NO];

    }
    
}

#pragma mark - AsyncBrokerRequestDelegate callbacks

-(void)didCompleteWithInfo:(NSDictionary *)info
{
    
    if(self.delegate)
    {
        [self.delegate SCWebRequest:self requestCompletedWithWithError:nil
                                request:webRequest
                               totalBytes:totalSize
                                     info:info];
    }
}


-(void)didCompleteWithError:(NSError *)error
{
    
    if(self.delegate)
    {
        [self.delegate SCWebRequest:self requestCompletedWithWithError:error
                                request:webRequest
                               totalBytes:0
                                   info:nil];
        
    }
}

-(void)didStart
{
    if(self.delegate)
    {
        [self.delegate SCWebRequest:self requestDidStart:webRequest   ];
    }
}


#pragma mark - NSURLConnectionDelegate credential code

- (BOOL)connection:(NSURLConnection *)connection canAuthenticateAgainstProtectionSpace:(NSURLProtectionSpace *)protectionSpace {
    return [protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust] || [protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodDefault];
}


-(void)connection:(NSURLConnection *)connection
didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge
{
    if(!shouldCheckCert)
    {
        [challenge.sender useCredential:[NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust] forAuthenticationChallenge:challenge];
    }
    else
    {
        if ([challenge.protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust])
        {
            
            NSData* provisonCert = STAppDelegate.accountsCert ;
            
            uint8_t hash[CC_SHA1_DIGEST_LENGTH];
            uint8_t  provisonCertHash [CC_SHA1_DIGEST_LENGTH];
            
            SecTrustRef trust = [challenge.protectionSpace serverTrust];
            
            SecCertificateRef certificate = SecTrustGetCertificateAtIndex(trust, 0);
            
            NSData* serverCertificateData = (__bridge_transfer NSData*)SecCertificateCopyData(certificate);
            
            CC_SHA1([provisonCert bytes], [provisonCert length], provisonCertHash);
            CC_SHA1([serverCertificateData bytes], [serverCertificateData length], hash);
            
            if(CMP(hash,provisonCertHash, CC_SHA1_DIGEST_LENGTH))
            {
                [challenge.sender useCredential:[NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust] forAuthenticationChallenge:challenge];
                
            }
            else
            {
                [[challenge sender] cancelAuthenticationChallenge:challenge];
            }
            
        }
    }
    
    
}


@end

