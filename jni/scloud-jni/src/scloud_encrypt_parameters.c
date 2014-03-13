#include <stdlib.h>
#include "uint8_t_array.h"
#include "scloud_encrypt_parameters.h"

SCloudEncryptParameters *SCloudEncryptParameters_init() {

  SCloudEncryptParameters *this = malloc( sizeof( SCloudEncryptParameters ) );

  this->version = 1;
  this->context = uint8_t_array_init();
  this->metaData = uint8_t_array_init();

  return this;

}

void SCloudEncryptParameters_free( SCloudEncryptParameters *this ) {
  if( this == NULL ) { return; }
  if( this->context != NULL ) { uint8_t_array_free( this->context ); this->context = NULL; }
  if( this->metaData != NULL ) { uint8_t_array_free( this->metaData ); this->metaData = NULL; }
  free( this );
}
