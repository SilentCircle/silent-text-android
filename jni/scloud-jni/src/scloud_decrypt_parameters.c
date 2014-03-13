#include <stdlib.h>
#include "uint8_t_array.h"
#include "scloud_decrypt_parameters.h"

SCloudDecryptParameters *SCloudDecryptParameters_init() {
  SCloudDecryptParameters *this = malloc( sizeof( SCloudDecryptParameters ) );
  this->version = 1;
  this->key = uint8_t_array_init();
  return this;
}

void SCloudDecryptParameters_free( SCloudDecryptParameters *this ) {
  if( this == NULL ) { return; }
  if( this->key != NULL ) { uint8_t_array_free( this->key ); this->key = NULL; }
  free( this );
}
