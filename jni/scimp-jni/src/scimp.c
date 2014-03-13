#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <tomcrypt.h>
#include <SCkeys.h>
#include "scimp_packet.h"
#include "uint8_t_array.h"
#include "scimp_keys.h"

extern ltc_math_descriptor ltc_mp;

static void printPacketInfo( const char *tag, SCimpPacket *packet ) {
  fprintf( stderr, "[%s] {\n  \"error\": %d,\n  \"warning\": %d,\n  \"state\": %d,\n  \"secret\": \"%s\",\n  \"context\": \"%s\",\n  \"outgoing_data\": \"%s\",\n  \"decrypted_data\": \"%s\"\n}\n\n", tag, packet->error, packet->warning, packet->state, packet->secret, packet->context, packet->outgoingData, packet->decryptedData );
}

static void exchangeKeys( SCimpPacket *initiator, SCimpPacket *responder ) {

  printPacketInfo( "initiator", initiator );
  printPacketInfo( "responder", responder );

  SCimpPacket_connect( initiator );

  printPacketInfo( "initiator", initiator );

  while( initiator->outgoingData != NULL || responder->outgoingData != NULL ) {
    if( initiator->outgoingData != NULL ) {
      SCimpPacket_receivePacket( responder, initiator->outgoingData ); free( initiator->outgoingData ); initiator->outgoingData = NULL;
      printPacketInfo( "responder", responder );
    }
    if( responder->outgoingData != NULL ) {
      SCimpPacket_receivePacket( initiator, responder->outgoingData ); free( responder->outgoingData ); responder->outgoingData = NULL;
      printPacketInfo( "initiator", initiator );
    }
  }

}

static void sendPacketData( SCimpPacket *initiator, SCimpPacket *responder, char *data ) {
  SCimpPacket_sendPacket( initiator, data );
  printPacketInfo( "initiator", initiator );
  SCimpPacket_receivePacket( responder, initiator->outgoingData );
  printPacketInfo( "responder", responder );
}

static void registerCryptoAlgorithms() {

  ltc_mp = ltm_desc;

  register_prng(&sprng_desc);

  register_hash(&sha256_desc);
  register_hash(&sha512_desc);
  register_hash(&sha512_256_desc);
  register_hash(&skein512_desc);
  register_cipher(&aes_desc);
  register_hash(&skein256_desc);
  register_hash(&skein512_desc);


}

void testPublicKeyImport( uint8_t_array *publicKey ) {
  printf( "\ntestPublicKeyImport public_key:%s\n", publicKey->items );
  SCKeyContextRef key = kInvalidSCKeyContextRef;
  SCLError err = SCKeyDeserialize( publicKey->items, publicKey->size, &key );
  printf( "\ntestPublicKeyImport error:%d\n", err );
  SCKeyFree( key );
}

int main( int argc, const char *argv[] ) {

  registerCryptoAlgorithms();

  uint8_t_array *storageKeyAlice = uint8_t_array_allocate(64);
  uint8_t_array *storageKeyBob = uint8_t_array_allocate(64);
#define INIT( value, with ) char value[1024]; strncpy( value, with, strlen(with) + 1 );
  INIT( localUserID, "alice@silentcircle.com" );
  INIT( remoteUserID, "bob@silentcircle.com" );
#undef INIT

  int i;
  for( i = 1; i < argc; i++ ) {
    if( strcmp( argv[i], "-l" ) == 0 ) {
      i++;
      strncpy( localUserID, argv[i], strlen(argv[i]) );
    }
    if( strcmp( argv[i], "-r" ) == 0 ) {
      i++;
      strncpy( remoteUserID, argv[i], strlen(argv[i]) );
    }
  }

  char buffer[ 4 * 1024 ];
  size_t dataSize = 0;
  dataSize = fread( buffer, sizeof(char), sizeof(buffer), stdin );
  char data[dataSize];
  strncpy( data, buffer, dataSize );

  SCimpPacket *alice = SCimpPacket_create( storageKeyAlice, localUserID, remoteUserID );
  SCimpPacket *bob = SCimpPacket_create( storageKeyBob, remoteUserID, localUserID );

  SCKeyContextRef alicePrivateKey = NULL;
  SCKeyContextRef bobPrivateKey = NULL;
  SCKeyContextRef bobPublicKey = NULL;
  SCKeyContextRef alicePublicKey = NULL;
  uint8_t_array *alicePrivateKeySerialized = uint8_t_array_init();
  uint8_t_array *alicePublicKeySerialized = uint8_t_array_init();
  uint8_t_array *bobPublicKeySerialized = uint8_t_array_init();
  uint8_t_array *bobPrivateKeySerialized = uint8_t_array_init();

  SCimp_generatePrivateKey( &alicePrivateKey, localUserID );
  SCimp_generatePrivateKey( &bobPrivateKey, remoteUserID );

  SCimp_exportPrivateKey( alicePrivateKey, storageKeyAlice, alicePrivateKeySerialized );
  SCimp_exportPrivateKey( bobPrivateKey, storageKeyBob, bobPrivateKeySerialized );

  SCimpPacket_setPrivateKey( alice, alicePrivateKeySerialized, storageKeyAlice );
  SCimpPacket_setPrivateKey( bob, bobPrivateKeySerialized, storageKeyBob );

  SCimp_exportPublicKey( bobPrivateKey, bobPublicKeySerialized );
  SCimp_exportPublicKey( alicePrivateKey, alicePublicKeySerialized );

  testPublicKeyImport( alicePublicKeySerialized );
  testPublicKeyImport( bobPublicKeySerialized );

  SCimpPacket_setPublicKey( alice, bobPublicKeySerialized );

  SCimp_importPublicKey( bobPublicKeySerialized, &bobPublicKey );
  SCimp_importPublicKey( alicePublicKeySerialized, &alicePublicKey );

  sendPacketData( alice, bob, data );

  exchangeKeys( alice, bob );
  sendPacketData( alice, bob, data );

  if( strcmp( data, bob->decryptedData ) == 0 ) {
    printf( "Success! (%s)\n", alice->secret );
  } else {
    printf( "Failed.\n" );
  }

  SCimpPacket_free( bob );
  SCimpPacket_free( alice );

  return 0;

}
