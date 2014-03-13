//
//  main.c
//  yajl-test
//
//  Created by Vinnie Moscaritolo on 5/30/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#include <yajl_parse.h>
#include <yajl_gen.h>

 
#define CKERR  if((err != yajl_gen_status_ok)) {\
printf("ERROR %d  %s:%d \n",  err, __FILE__, __LINE__); \
goto done; }

static void yajlTestFree(void * ctx, void * ptr)
{
     free(ptr);
}

static void * yajlTestMalloc(void * ctx, size_t sz)
{
      return malloc(sz);
}

static void * yajlTestRealloc(void * ctx, void * ptr, size_t sz)
{
      
    return realloc(ptr, sz);
}

static const char* kCommitStr = "commit";
static const char* kVersionStr = "version";
static const char* kCipherSuiteStr = "cipherSuite";
static const char* kSASmethodStr = "sasMethod";
static const char* kHpkiStr = "Hpki";
static const char* kHcsStr = "Hcs";
static const char* kDataStr = "data";
static const char* kMacStr = "mac";
static const char* kSeqStr = "seq";





static int sParse_null(void * ctx)
{
    printf("null\n");
     return 1;
 }

static int sParse_boolean(void * ctx, int boolean)
{
    printf("boolean: %d\n", boolean );
     return 1;
}

static int sParse_number(void * ctx, const char * s, size_t l)
{
    printf("number:  %.*s\n", (int)l, s);
     return 1;
}

static int sParse_string(void * ctx, const unsigned char * stringVal,
                           size_t stringLen)
{
    printf("string:  %.*s\n", (int)stringLen, stringVal);
     return 1;
}

static int sParse_map_key(void * ctx, const unsigned char * stringVal,
                            size_t stringLen)
{
    printf("map key: %.*s\n", (int)stringLen, stringVal);
     return 1;
}

static int sParse_start_map(void * ctx)
{
    printf("map start\n");
     return 1;
}


static int sParse_end_map(void * ctx)
{
    printf("map end\n");
     return 1;
}

static int sParse_start_array(void * ctx)
{
    printf("array start\n");
     return 1;
}

static int sParse_end_array(void * ctx)
{
    printf("array end\n");
     return 1;
}

static yajl_callbacks callbacks = {
    sParse_null,
    sParse_boolean,
    NULL,
    NULL,
    sParse_number,
    sParse_string,
    sParse_start_map,
    sParse_map_key,
    sParse_end_map,
    sParse_start_array,
    sParse_end_array
};


int main(int argc, const char * argv[])
{
    yajl_gen g;
    yajl_gen g1;
    yajl_handle hand;
    const unsigned char * buf;
    size_t len;
   
    //    yajl_handle hand;

    yajl_alloc_funcs allocFuncs = {
        yajlTestMalloc,
        yajlTestRealloc,
        yajlTestFree,
        (void *) NULL
    };
    
    yajl_status err = yajl_gen_status_ok;
    
    // insert code here...
    printf("Yajl test\n");
    
        
    g = yajl_gen_alloc(&allocFuncs);
    yajl_gen_config(g, yajl_gen_beautify, 1);
    yajl_gen_config(g, yajl_gen_validate_utf8, 1);
    
#if 1
    err = yajl_gen_map_open(g); CKERR;
    err = yajl_gen_string(g, kCommitStr, strlen(kCommitStr)) ; CKERR;
      err = yajl_gen_map_open(g); CKERR;
    
    err = yajl_gen_string(g, kVersionStr, strlen(kVersionStr)) ; CKERR;
    err = yajl_gen_number(g, "1.0", 3) ; CKERR;
 
    err = yajl_gen_string(g, kCipherSuiteStr, strlen(kCipherSuiteStr)) ; CKERR;
    err = yajl_gen_number(g, "2", 1) ; CKERR;
  
    err = yajl_gen_string(g, kSASmethodStr, strlen(kSASmethodStr)) ; CKERR;
    err = yajl_gen_number(g, "1", 1) ; CKERR;
    
    err = yajl_gen_string(g, kHpkiStr, strlen(kHpkiStr)) ; CKERR;
    err = yajl_gen_string(g, "3LTtqt9DUpbhs2pC5m7reBab+Dc9qntZx4nMitPTNMk=", 44) ; CKERR;
    
    err = yajl_gen_string(g, kHcsStr, strlen(kHcsStr)) ; CKERR;
    err = yajl_gen_string(g, "ghIvlgjqUhI=", 12) ; CKERR;
   
     err = yajl_gen_map_close(g); CKERR;
    err = yajl_gen_map_close(g); CKERR;
 
#else 
     err = yajl_gen_map_open(g); CKERR;
    err = yajl_gen_string(g, kDataStr, strlen(kDataStr)) ; CKERR;
     err = yajl_gen_map_open(g); CKERR;
    
   
     err = yajl_gen_string(g, kSeqStr, strlen(kSeqStr)) ; CKERR;
     err = yajl_gen_number(g, "12345", 5) ; CKERR;  // no leading zeros
  
       err = yajl_gen_string(g, kHcsStr, strlen(kMacStr)) ; CKERR;
      err = yajl_gen_string(g, "Eq52ryWVGlRFJM2idS0K5A==", 24) ; CKERR;
    
     err = yajl_gen_string(g, "Msg", 3) ; CKERR;
     err = yajl_gen_string(g, "3IKX/wRQ+TRYSNKj0l7WYgdTIGMmhH4L0n4l5he0tucJ5ohieorCe/iSJ/Mcl3qVkqLaBXIsJqVXtrJrfa+v6nLaZJPq4+pFgtedPafQCYqz+12V8+IkL8zqpDnbQLDF", 129) ; CKERR;
    
     err = yajl_gen_map_close(g); CKERR;
    err = yajl_gen_map_close(g); CKERR;
#endif
    
   err =  yajl_gen_get_buf(g, &buf, &len);CKERR;
    
    if(buf && len > 0)
        printf("\n%s\n", buf);
    
    g1 = yajl_gen_alloc(NULL);
    hand = yajl_alloc(&callbacks, &allocFuncs, (void *) g1);
    
    /* and let's allow comments by default */
    yajl_config(hand, yajl_allow_comments, 1);
    err = yajl_parse(hand, buf,  len); CKERR;
    err = yajl_complete_parse(hand); CKERR;

      
 done:
    
    if (err != yajl_status_ok) {
        
        printf("\nError %d:  \n", err);
    }
    
    
    yajl_gen_clear(g);
    
    yajl_gen_free(g1);
    yajl_free(hand);

  if(g)   yajl_gen_free(g);

    return 0;
} 