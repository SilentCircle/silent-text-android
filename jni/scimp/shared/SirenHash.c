

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
//  SirenHash.c
//  crypto-optest
 
#include "SirenHash.h"

 
#include <tomcrypt.h>
#include "yajl_parse.h"
#include <yajl_gen.h>
 
#include "SCpubTypes.h"
#include "SCutilities.h"

#include "cryptowrappers.h"


#define CKSTAT  if((stat != yajl_gen_status_ok)) {\
printf("ERROR %d  %s:%d \n",  err, __FILE__, __LINE__); \
err = kSCLError_CorruptData; \
goto done; }


char*  sHashable_items_list[] = {
        "message",
        "cloud_key",
        "cloud_url",
        "fyeo",
        "message",
        "request_receipt",
        "received_id",
        "request_resend",
        "request_burn",
        "shred_after",
        "location",
        "vcard",
        "thumbnail",
        "media_type",
        NULL 
};

 

#pragma mark - memory management

static void yajlFree(void * ctx, void * ptr)
{
    XFREE(ptr);
}

static void * yajlMalloc(void * ctx, size_t sz)
{
    return XMALLOC(sz);
}

static void * yajlRealloc(void * ctx, void * ptr, size_t sz)
{
    
    return XREALLOC(ptr, sz);
}
#pragma mark - utility
 
#pragma mark - key import

 
struct SirenJSONcontext
{
    int                 level;
    
    int             validItem;
    HASH_ContextRef hash;
    
    void*           jItem;
    size_t*         jItemSize;
    
};

typedef struct SirenJSONcontext SirenJSONcontext;

static int sParse_start_map(void * ctx)
{
    SirenJSONcontext *jctx = (SirenJSONcontext*) ctx;
    int retval = 1;
    
    if(IsntNull(jctx))
    {
        jctx->level++;
    }
    
done:
    
    return retval;
}

static int sParse_end_map(void * ctx)
{
    SirenJSONcontext *jctx = (SirenJSONcontext*) ctx;
    
    if(IsntNull(jctx)  )
    {
        
        jctx->level--;
        
    }
    
    
    return 1;
}

static int sParse_bool(void * ctx, int boolVal)
{
    SirenJSONcontext *jctx = (SirenJSONcontext*) ctx;
    
    uint16_t    len = 1;
    uint8_t     val = boolVal?1:0;
    
    if(jctx->validItem)
    {
        
        HASH_Update(jctx->hash, &len, sizeof(len));
        HASH_Update(jctx->hash, &val, 1);
    }
  return 1;
}


static int sParse_number(void * ctx, const char * stringVal, size_t stringLen)
{
    SirenJSONcontext *jctx = (SirenJSONcontext*) ctx;
  
    if(jctx->validItem)
    {
        uint16_t    len = (uint16_t)stringLen;
   
        HASH_Update(jctx->hash, &len, sizeof(len));
        HASH_Update(jctx->hash, stringVal, stringLen);
    }

 
    return 1;
}

 
static int sParse_string(void * ctx, const unsigned char * stringVal,
                         size_t stringLen)
{
      SirenJSONcontext *jctx = (SirenJSONcontext*) ctx;

    if(jctx->validItem)
    {
        uint16_t    len = (uint16_t)stringLen;
        
        HASH_Update(jctx->hash, &len, sizeof(len));
        HASH_Update(jctx->hash, stringVal, stringLen);
    }
    
    return 1;
}

static int sParse_map_key(void * ctx, const unsigned char * stringVal,
                          size_t stringLen)
{
    SirenJSONcontext *jctx = (SirenJSONcontext*) ctx;
      
    jctx->validItem =  0;
    char** item;
    for(item = sHashable_items_list; *item; item++ )
    {
    
        if((strlen(*item) == stringLen)
           &&  CMP(stringVal, *item, stringLen))
        {
            uint16_t    len = (uint16_t)stringLen;
            
            HASH_Update(jctx->hash, &len, sizeof(len));
            HASH_Update(jctx->hash, stringVal, stringLen);
            jctx->validItem = 1;
            break;
        }
      }
     
    return 1;
}

 

SCLError  Siren_ComputeHash(    HASH_Algorithm  hash,
                            const char*         sirenData,
                            uint8_t*            hashOut )
{
    SCLError        err = kSCLError_NoErr;
    
    yajl_gen_status         stat = yajl_gen_status_ok;
    yajl_handle             pHand = NULL;
    SirenJSONcontext       *jctx = NULL;
    
    static yajl_callbacks callbacks = {
        NULL,
        sParse_bool,
        NULL,
        NULL,
        sParse_number,
        sParse_string,
        sParse_start_map,
        sParse_map_key,
        sParse_end_map,
        NULL,
        NULL
    };
    
    yajl_alloc_funcs allocFuncs = {
        yajlMalloc,
        yajlRealloc,
        yajlFree,
        (void *) NULL
    };
    
     jctx = XMALLOC(sizeof (SirenJSONcontext)); CKNULL(jctx);
    ZERO(jctx, sizeof(SirenJSONcontext));
    err  = HASH_Init(hash, &jctx->hash); CKERR;

    pHand = yajl_alloc(&callbacks, &allocFuncs, (void *) jctx);
    
    yajl_config(pHand, yajl_allow_comments, 1);
    stat = (yajl_gen_status) yajl_parse(pHand, (uint8_t*)sirenData,  strlen(sirenData)); CKSTAT;
    stat = (yajl_gen_status) yajl_complete_parse(pHand); CKSTAT;
    
    err = HASH_Final(jctx->hash, hashOut); CKERR;
       
done:
    
    if(IsntNull(jctx))
    {
        ZERO(jctx, sizeof(SirenJSONcontext));
        XFREE(jctx);
    }
    
    if(IsntNull(pHand))
        yajl_free(pHand);
    
    return err;
    
}


