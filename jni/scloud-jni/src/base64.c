#include "base64.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static char Base64Padding = '=';
static char *Base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

int sc_base64_decode( char *input, uint8_t *output, size_t outputSize ) {

  size_t inputSize = strlen(input);
  int inputOffset = 0;
  int outputOffset = 0;
  uint8_t block[4];
  int i;
  char currentValue;

  if( outputSize < inputSize * 4 / 3 ) {
    return -1;
  }

  while( inputOffset < inputSize ) {

    for( i = 0; i < 4; i++ ) {
      currentValue = input[ inputOffset + i ];
      if( currentValue == Base64Padding ) {
        block[i] = 0;
      } else {
        block[i] = strchr( Base64, currentValue ) - Base64;
      }
    }

    inputOffset += 4;

    output[outputOffset++] = block[0] << 2 | block[1] >> 4;
    output[outputOffset++] = block[1] << 4 | block[2] >> 2;
    output[outputOffset++] = block[2] << 6 | block[3] >> 0;

  }

  output[outputOffset] = '\0';

  return 0;

}

int sc_base64_encode( uint8_t *input, size_t inputSize, char *output, size_t outputSize ) {

  int inputOffset = 0;
  int outputOffset = 0;
  uint8_t block[3];
  int blockSize;
  int i;

  if( outputSize < inputSize * 4 / 3 ) {
    return -1;
  }

  while( inputOffset < inputSize ) {

    for( i = 0; i < 3; i++ ) {
      if( inputOffset + i >= inputSize ) {
        block[i] = 0;
      } else {
        block[i] = input[ inputOffset + i ];
        blockSize = i + 1;
      }
    }

    inputOffset += 3;

    output[outputOffset++] = Base64[ block[0] >> 2 ];
    output[outputOffset++] = Base64[ ( ( block[0] & 0x03 ) << 4 ) | ( ( block[1] & 0xf0 ) >> 4 ) ];
    output[outputOffset++] = blockSize > 1 ? Base64[ ( ( block[1] & 0x0f ) << 2 ) | ( ( block[2] & 0xc0 ) >> 6 ) ] : Base64Padding;
    output[outputOffset++] = blockSize > 2 ? Base64[ block[2] & 0x3f ] : Base64Padding;

  }

  output[outputOffset] = '\0';

  return 0;

}
