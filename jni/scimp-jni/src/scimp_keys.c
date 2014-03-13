#include <SCimp.h>
#include <time.h>
#include <tomcrypt.h>
#include "scimp_keys.h"

SCLError SCimp_generatePrivateKey( SCKeyContextRef *key, const char *owner ) {

  SCLError err = kSCLError_NoErr;
  ECC_ContextRef ecc = kInvalidECC_ContextRef;
  uint8_t_array *nonce = uint8_t_array_allocate(32);
  sprng_read( nonce->items, nonce->size, NULL );

  err = ECC_Init( &ecc ); CKERR;
  err = ECC_Generate( ecc, 384 ); CKERR;
  err = SCKeyImport_ECC( ecc, nonce->items, nonce->size, key ); CKERR;

  ECC_Free( ecc );
  ecc = kInvalidECC_ContextRef;
  uint8_t_array_free( nonce );

  time_t now = time( NULL );
  time_t later = now + ( 3600 * 24 );

  err = SCKeySetProperty( *key, kSCKeyProp_StartDate,  SCKeyPropertyType_Time,       &now,     sizeof( time_t )   ); CKERR;
  err = SCKeySetProperty( *key, kSCKeyProp_ExpireDate, SCKeyPropertyType_Time,       &later,   sizeof( time_t )   ); CKERR;
  err = SCKeySetProperty( *key, kSCKeyProp_Owner,      SCKeyPropertyType_UTF8String, (void*) owner,    strlen( owner )    ); CKERR;

done:

  return err;

}

SCLError SCimp_exportPrivateKey( SCKeyContextRef in, uint8_t_array *storageKey, uint8_t_array *out ) {

  SCLError err = kSCLError_NoErr;

  err = SCKeySerializePrivate( in, storageKey->items, storageKey->size, &out->items, &out->size ); CKERR;

done:

  return err;

}

SCLError SCimp_exportPublicKey( SCKeyContextRef in, uint8_t_array *out ) {

  SCLError err = kSCLError_NoErr;

  err = SCKeySerialize( in, &out->items, &out->size ); CKERR;

done:

  return err;

}

SCLError SCimp_importPrivateKey( uint8_t_array *in, uint8_t_array *storageKey, SCKeyContextRef *out ) {

  SCLError err = kSCLError_NoErr;
  bool isLocked = true;

  err = SCKeyDeserialize( in->items, in->size, out ); CKERR;
  err = SCKeyIsLocked( *out, &isLocked ); CKERR;

  if( isLocked ) {
    err = SCKeyUnlock( *out, storageKey->items, storageKey->size ); CKERR;
  }

done:

  return err;

}

SCLError SCimp_importPublicKey( uint8_t_array *in, SCKeyContextRef *out ) {

  SCLError err = kSCLError_NoErr;

  err = SCKeyDeserialize( in->items, in->size, out ); CKERR;

done:

  return err;

}
