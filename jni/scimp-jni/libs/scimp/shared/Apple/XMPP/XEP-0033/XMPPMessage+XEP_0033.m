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
//  XMPPMessage+XEP_0083.h
//  silenttext
//
 
#import "XMPPMessage+XEP_0033.h"
#import "NSXMLElement+XMPP.h"
#if TARGET_OS_IPHONE
#import "DDXML.h"
#endif

#import "XMPPJID.h"


static NSString *const xmlns_multicast = @"http://jabber.org/protocol/address";


@implementation XMPPMessage (XEP_0033)


- (id)initWithType:(NSString *)type jids:(NSArray *)jids module:(NSString*)module
{
	if ((self = [super initWithName:@"message"]))
	{
		if (type)
			[self addAttributeWithName:@"type" stringValue:type];
        
        [self addAttributeWithName:@"to" stringValue:module];

        
        NSXMLElement *multicast = [NSXMLElement elementWithName:@"addresses" xmlns:xmlns_multicast];
        [self addChild:multicast];
        
        for(NSString* recipient in jids)
        {
            NSXMLElement *address =  [NSXMLElement elementWithName:@"address" ];
            [address addAttributeWithName:@"type" stringValue:@"to"];
            [address addAttributeWithName:@"jid" stringValue: recipient];
            [multicast addChild:address];
         }
        
 	}
	return self;
}


+ (XMPPMessage *)multicastMessageWithType:(NSString *)type jids:(NSArray *)jids module:(NSString*)module
{
	return [[XMPPMessage alloc] initWithType:type jids:jids module:module];
}

- (BOOL)isMulticast
{
	return ([[self elementsForXmlns:xmlns_multicast] count] > 0);
}


- (NSArray*) jids
{
    NSMutableArray *jids =  [[NSMutableArray alloc] init];
    NSXMLElement *multicast =[self elementForName:@"addresses"];
    
    if(multicast)
   {
       NSArray * addresses = [multicast elementsForName:@"address"  ];
      
       for(NSXMLElement* address in  addresses)
       {
           NSString* jid = [[address attributeForName: @"jid"] stringValue];
           [jids addObject:[XMPPJID jidWithString:jid]];
       }
 }
     
    return jids;
}

- (NSArray*) jidStrings
{
    NSMutableArray *jids =  [[NSMutableArray alloc] init];
    NSXMLElement *multicast =[self elementForName:@"addresses"];
    
    if(multicast)
    {
        NSArray * addresses = [multicast elementsForName:@"address"  ];
        
        for(NSXMLElement* address in  addresses)
        {
            NSString* jid = [[address attributeForName: @"jid"] stringValue];
            [jids addObject: jid];
        }
    }
    
    return jids;
}



@end
