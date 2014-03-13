#ifndef __BASE64_H__
#define __BASE64_H__

#include <stdint.h>
#include <stdlib.h>

int sc_base64_encode( uint8_t *src, size_t srclength, char *target, size_t targsize );
int sc_base64_decode( char *src, uint8_t *target, size_t targsize );

#endif/*__BASE64_H__*/
