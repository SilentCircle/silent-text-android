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
//  NSString+SCUtilities.m
//  SilentText
//

#import "NSString+SCUtilities.h"
 

#include <tomcrypt.h>


@implementation NSString (SCUtilities)
 
- (NSData *)base64Decoded
{
     NSData* decodedData = NULL;
    
    size_t  len     = self.length;
    uint8_t *oBuf   = NULL;
    
    oBuf = XMALLOC(len);    // overallocate   -- so what! this isn't a pdp11
    if(oBuf)
    {
        if( base64_decode( (const unsigned char*)[self UTF8String], self.length, oBuf, &len) == CRYPT_OK)
        {
            decodedData = [NSData dataWithBytesNoCopy: oBuf length:len freeWhenDone: YES];

        }
    }
    
    return decodedData;
}

- (NSString *)urlEncodeString:(NSStringEncoding)encoding
{
    return (NSString *) (__bridge_transfer NSString *) CFURLCreateStringByAddingPercentEscapes(NULL, (CFStringRef)self,
                                                                                               NULL, (CFStringRef)@"!*'();:@&=+$,/?%#[]",
                                                                                               CFStringConvertNSStringEncodingToEncoding(encoding));
}  


 +(NSString*) base64EncodeBytes: (const uint8_t  *)bytes length:(NSUInteger)length
{
    NSString*       encodedString = NULL;
    
    uint8_t         *oBuf   = NULL;
    unsigned long   len =  ((((length) + 2) / 3) * 4)+1 ;
    
    oBuf = XMALLOC(len);
    if(oBuf)
    {
        *oBuf= 0;
        
        if( base64_encode(bytes, length, oBuf, &len) == CRYPT_OK)
        {
            encodedString = [NSString.alloc initWithBytesNoCopy: oBuf
                                                         length: len
                                                       encoding: NSUTF8StringEncoding
                                                   freeWhenDone: YES];
        }
    }
    
    return encodedString;
    
}

+(NSString*) hexEncodeBytes: (const uint8_t  *)bytes length:(NSUInteger)length
{
    NSString*       encodedString = NULL;
    
    char hexDigit[] = "0123456789abcdef";
    uint8_t         *oBuf   = NULL;
    unsigned long   len =  (length * 2) ;
    
    oBuf = XMALLOC(len);
    if(oBuf)
    {
        *oBuf= 0;
          
        register int    i;
        uint8_t *p = oBuf;
        
        for (i = 0; i < length; i++)
         {
            *p++ =  hexDigit[ bytes[i] >>4];
            *p++ =  hexDigit[ bytes[i] &0xF];
        }
        
        encodedString = [NSString.alloc initWithBytesNoCopy: oBuf
                                                         length: len
                                                       encoding: NSUTF8StringEncoding
                                                   freeWhenDone: YES];
        
    }
    
    return encodedString;
}


@end
