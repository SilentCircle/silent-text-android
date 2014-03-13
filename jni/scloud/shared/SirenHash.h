//
//  SirenHash.h
//  
//
//  Created by Vinnie Moscaritolo on 5/6/13.
//
//

#ifndef _SirenHash_h
#define _SirenHash_h

#include "SCpubTypes.h"
#include "cryptowrappers.h"

SCLError  Siren_ComputeHash(    HASH_Algorithm  hash,
                            const char*      sirenData,
                            uint8_t*            hashOut );



#endif
