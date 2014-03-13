#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <tomcrypt.h>
#include <cryptowrappers.h>
#include <SCloud.h>
#include "uint8_t_array.h"
#include "scloud_encrypt_packet.h"
#include "scloud_encrypt_parameters.h"
#include "scloud_decrypt_packet.h"
#include "scloud_decrypt_parameters.h"

extern ltc_math_descriptor ltc_mp;

int main( int argc, const char *argv[] ) {

  register_prng( &sprng_desc );

  register_hash( &sha256_desc );
  register_hash( &sha512_desc );
  register_hash( &sha512_256_desc );
  register_hash( &skein512_desc );
  register_hash( &skein256_desc );
  register_hash( &skein512_desc );

  register_cipher( &aes_desc );

  ltc_mp = ltm_desc;

  SCLError error = kSCLError_NoErr;
  uint8_t_array *key = uint8_t_array_init();

  int mode = 0;
  int i;
  for( i = 1; i < argc; i++ ) {

    if( strcmp( argv[i], "-d" ) == 0 ) {
      mode = 1;
    }

    if( strcmp( argv[i], "-e" ) == 0 ) {
      mode = 2;
    }

    if( strcmp( argv[i], "-k" ) == 0 ) {

      i++;

      key->size = strlen( argv[i] );
      key->items = malloc( key->size );
      memcpy( key->items, argv[i], key->size );

    }

  }

  switch( mode ) {
    case 1: {

      if( key->size > 0 ) {

        SCloudDecryptParameters *parameters = SCloudDecryptParameters_init();

        parameters->key = key;

        uint8_t_array *data = uint8_t_array_init();

        data->size = 4 * 1024 * 1024;
        data->items = malloc( data->size );

        data->size = fread( data->items, sizeof(uint8_t), data->size, stdin );
        data->items = realloc( data->items, data->size );

        SCloudDecryptPacket *packet = SCloudDecryptPacket_init( parameters );
        error = SCloudDecryptPacket_decrypt( packet, data );

        if( error != kSCLError_NoErr ) {
          fprintf( stderr, "Error Code: %d", error );
        } else {
          fwrite( packet->data->items, sizeof(uint8_t), packet->data->size, stdout );
        }

        SCloudDecryptPacket_free( packet );
        uint8_t_array_free( data );

      } else {
        fprintf( stderr, "You must specify a key.\n" );
      }

    } break;

    case 2: {

      SCloudEncryptParameters *parameters = SCloudEncryptParameters_init();

      uint8_t_array *data = uint8_t_array_init();

      data->size = 4 * 1024 * 1024;
      data->items = malloc( data->size );

      data->size = fread( data->items, sizeof(uint8_t), data->size, stdin );
      data->items = realloc( data->items, data->size );

      SCloudEncryptPacket *packet = SCloudEncryptPacket_init( parameters );
      error = SCloudEncryptPacket_encrypt( packet, data );

      if( error != kSCLError_NoErr ) {
        fprintf( stderr, "Error Code: %d", error );
      } else {
        fwrite( packet->key->items, sizeof(uint8_t), packet->key->size, stderr );
        fwrite( packet->data->items, sizeof(uint8_t), packet->data->size, stdout );
      }

      SCloudEncryptPacket_free( packet );
      uint8_t_array_free( data );

    } break;

    default: {
      fprintf( stderr, "USAGE:\n\t%s -e\n\tEncrypt standard input to standard output, print key to standard error.\n\n\t%s -d -k <key>\n\tDecrypt standard input to standard output using the given encryption key.\n\n", argv[0], argv[0] );
    } break;

  }

  return 0;

}
