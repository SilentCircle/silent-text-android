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
//  SCimpWrapper.m
//  silenttext
//

#import "XMPPSilentCircleStorage.h"

 
@implementation SCimpWrapper

- (id)init
{
	if ((self = [super init]))
	{
 		threadElement = NULL;
		scimpCtx = NULL;
		
		pendingOutgoingMessages = [[NSMutableArray alloc] init];
	}
	return self;
}

- (id)initWithLocalJid:(XMPPJID *)myJid remoteJID:(XMPPJID *)theirJid scimpCtx:(SCimpContextRef)ctx
{
	if ((self = [self init]))
	{
        remoteJid = theirJid;
        localJid = myJid;
        scimpCtx = ctx;
        [self updateScimpID];
	}
	return self;
}

- (id)initWithThread:(NSString *)threadID localJID:(XMPPJID *)localJID scimpCtx:(SCimpContextRef)ctx
{
	if ((self = [self init]))
	{
        threadElement = threadID;
        scimpCtx = ctx;
        localJid = localJID;
        scimpID = [SCimpWrapper threadHash:threadID localJID:localJID];
	}
	return self;
}

- (void) dealloc
{
    
    pendingOutgoingMessages = NULL;
    
    if (scimpCtx) {
        SCimpFree(scimpCtx);
        scimpCtx = NULL;
    }
    
    remoteJid = NULL;
    localJid = NULL;
    threadElement = NULL;
}


+ (NSString*) threadHash :(NSString *)thread localJID:(XMPPJID *)localJID 
{
    NSString*       encodedString = NULL;
    
    
    NSInteger hash1 = [thread hash ];
    NSInteger hash2 = [localJID.bare hash ];
     
    encodedString = [NSString stringWithFormat:@"%lX%lX", (long)hash1, (long)hash2];
    
    return encodedString;
};

+ (NSString*) searchKeyWithLocalJid:(XMPPJID *)myJid remoteJID:(XMPPJID *)theirJid options:(XMPPJIDCompareOptions)options
{
    NSInteger hash = 0;
    
    if(options == XMPPJIDCompareBare)
    {
        hash = [[myJid.bare stringByAppendingString:theirJid.bare] hash ];
    }
    else  if(options == XMPPJIDCompareFull)
    {
        hash = [[myJid.bare stringByAppendingString:theirJid.full] hash ];
        
    }
    else
        hash = [[myJid.bare stringByAppendingString:theirJid.resource?theirJid.full:theirJid.bare] hash ];
    
    return   [NSString stringWithFormat:@"%lX", (long)hash];
}


-(NSString*) updateScimpID
{
    scimpID = [SCimpWrapper searchKeyWithLocalJid:localJid remoteJID:remoteJid options:0];
     return scimpID;
}


- (BOOL)isReady
{
	if (scimpCtx == NULL) return NO;
	
	SCimpInfo info;
	SCLError err = SCimpGetInfo(scimpCtx, &info);
	
	return (err == kSCLError_NoErr && info.isReady);
}

- (NSString *)description
{
	return [NSString stringWithFormat:@"<SCimpWrapper %p: ctx=%p tuple=[%@, %@]>", self, scimpCtx, localJid.full, remoteJid.full];
}




@end