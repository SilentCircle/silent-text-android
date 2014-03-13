#ifndef __SCIMP_KEYS_H__
#define __SCIMP_KEYS_H__ 1

#include <SCkeys.h>
#include "uint8_t_array.h"

SCLError SCimp_generatePrivateKey( SCKeyContextRef *key, const char *owner );

SCLError SCimp_exportPrivateKey( SCKeyContextRef in, uint8_t_array *storageKey, uint8_t_array *out );

SCLError SCimp_importPrivateKey( uint8_t_array *in, uint8_t_array *storageKey, SCKeyContextRef *out );

SCLError SCimp_exportPublicKey( SCKeyContextRef in, uint8_t_array *out );

SCLError SCimp_importPublicKey( uint8_t_array *in, SCKeyContextRef *out );

#endif/*__SCIMP_KEYS_H__*/
