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
//  XMPPMessage+SilentCircle.m
//  SilentText
//

#import "AppConstants.h"
#import "XMPPMessage+SilentCircle.h"

#import "NSXMLElement+XMPP.h"
#import "XMPPElement+Delay.h"
#import "NSDate+XMPPDateTimeProfiles.h"
#import "XMPPDateTimeProfiles.h"

@implementation XMPPMessage (SilentCircle)

// this picks up the silent circle timestamp ST-209
- (NSDate*) timestamp 
{
    NSDate *date = nil;
    NSXMLElement *timestampElement = [self elementForName: @"time" xmlns: kSCPPTimestampNameSpace];
    
    if(timestampElement)
    {
        NSString *dateString = [timestampElement attributeStringValueForName:@"stamp"];
        
        if(dateString)
            date = [XMPPDateTimeProfiles parseDateTime: dateString];
    }

    return date;
}

- (BOOL) isChatMessageWithPubSiren {
    
	if ([self isChatMessage]) {
        
        return([self elementForName: kSCPPPubSiren xmlns: kSCPPNameSpace]?YES:NO);

  }
	return NO;
    
} // -isChatMessageWithSiren


- (BOOL) isChatMessageWithSiren {
    
	if ([self isChatMessage]) {
        
		return([self elementForName: kSCPPSiren xmlns: kSCPPNameSpace]?YES:NO);
	}
	return NO;

} // -isChatMessageWithSiren


-(void) setConversationIdElement: (NSString*) conversationID
{
    NSXMLElement *conversationIdElement = [NSXMLElement elementWithName:kSCPPConversationID xmlns:kSCPPNameSpace];
    [self addChild:conversationIdElement];
    [conversationIdElement setStringValue:conversationID];
}


- (NSString*) conversationIdElement {
    
    NSString *conversationID = nil;
    NSXMLElement  *element =  [self elementForName: kSCPPConversationID xmlns: kSCPPNameSpace];
    
    if(element)
    {
        conversationID =  [element stringValue];
    }
    
    return conversationID;
}

- (NSString*) threadElement
{
 
    NSArray *elements = [self elementsForName:@"thread"];
    if([elements count])
    {
        return [[elements objectAtIndex: 0] stringValue];
    }
    return NULL;
}

- (void) setThreadElement:(NSString*) threadID
{
    NSXMLElement * threadElement = [NSXMLElement.alloc initWithName: kXMPPThread stringValue:threadID];
    
    [self addChild: threadElement];
    
}

- (void)stripSilentCircleData
{
    NSUInteger index, count = [self childCount];
    
    for (index=count; index > 0; index--)
    {
        NSXMLElement *element = (NSXMLElement *)[self childAtIndex:(index-1)];
        
        if (   [[element name] isEqualToString:kSCPPSiren]
            || [[element name] isEqualToString:kSCPPTimestamp]
            || [[element name] isEqualToString:kSCPPPubSiren]
            || [[element name] isEqualToString:kSCPPConversationID] )
        {
            [self removeChildAtIndex:(index-1)];
        }
    }
}

- (NSString*)scPublicElement
{
    NSString *scKeyString = nil;
    NSXMLElement  *element =  [self elementForName: @"x" xmlns: kSCPPpublicKeyNameSpace];
  
    if(element)
    {
        scKeyString =  [element stringValue];
    }
    
    return scKeyString;
}

- (void) setScPublicElement:(NSString*) keyMessage
{
    
    NSXMLElement *element = [NSXMLElement elementWithName:@"x" xmlns:kSCPPpublicKeyNameSpace];
    element.stringValue = keyMessage;
    [self addChild: element];


}

@end
