#include <stdio.h>

void hprint( void *buffer, int offset, int length ) {
  char hexDigit[] = "0123456789ABCDEF";
  register int i;
  int lineStart;
  int lineLength;
  short c;
  const unsigned char *bufferPtr = buffer;
#define kLineSize 16
  for( lineStart = 0; lineStart < length; lineStart += lineLength ) {
    lineLength = kLineSize;
    if( lineStart + lineLength > length ) {
      lineLength = length - lineStart;
    }
    printf( "%6d: ", lineStart + offset );
    for( i = 0; i < lineLength; i++ ) {
      printf( "%c", hexDigit[ bufferPtr[lineStart+i] >> 4 ] );
      printf( "%c", hexDigit[ bufferPtr[lineStart+i] & 0xF ] );      if( ( lineStart + i ) & 0x01 ) {
        printf( "%c", ' ' );
      }
    }
    for( ; i < kLineSize; i++ ) {
      printf("   ");
    }
    printf("  ");
    for( i = 0; i < lineLength; i++ ) {
      c = bufferPtr[lineStart + i] & 0xFF;
      if( c > ' ' && c < '~' ) {
        printf("%c", c);      } else {
        printf(".");
      }
    }
    printf("\n");
  }
#undef kLineSize
}
