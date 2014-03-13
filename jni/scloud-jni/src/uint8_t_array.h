#ifndef __UINT8_T_ARRAY_H__
#define __UINT8_T_ARRAY_H__ 1

#include <stdint.h>
#include <stdlib.h>

typedef struct {

  uint8_t version;
  size_t size;
  uint8_t *items;

} uint8_t_array;

uint8_t_array *uint8_t_array_init();

void uint8_t_array_free( uint8_t_array *this );

uint8_t_array *uint8_t_array_parse( const char *in );

uint8_t_array *uint8_t_array_copy( void *from, size_t size );

#endif/*__UINT8_T_ARRAY_H__*/
