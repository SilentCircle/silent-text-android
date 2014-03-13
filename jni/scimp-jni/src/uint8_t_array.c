#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "uint8_t_array.h"

uint8_t_array *uint8_t_array_init() {

  uint8_t_array *this = malloc( sizeof( uint8_t_array ) );

  this->version = 1;
  this->size = 0;
  this->items = NULL;

  return this;

}

uint8_t_array *uint8_t_array_allocate( size_t size ) {
  uint8_t_array *this = uint8_t_array_init();
  this->size = size;
  this->items = calloc( this->size, sizeof(uint8_t) );
  return this;
}

void uint8_t_array_free( uint8_t_array *this ) {
  if( this == NULL ) { return; }
  if( this->items != NULL ) { free( this->items ); this->items = NULL; }
  this->size = 0;
  free( this );
}

uint8_t_array *uint8_t_array_parse( const char *in ) {
  return uint8_t_array_copy( (void*) in, strlen( in ) );
}

uint8_t_array *uint8_t_array_copy( void *from, size_t size ) {

  uint8_t_array *this = uint8_t_array_init();

  this->size = size;
  this->items = malloc( sizeof(uint8_t) * size );

  memcpy( this->items, from, sizeof(uint8_t) * this->size );

  return this;

}
