#ifndef __SCLOUD_DECRYPT_PARAMETERS_H__
#define __SCLOUD_DECRYPT_PARAMETERS_H__ 1

#include <stdint.h>
#include "uint8_t_array.h"

typedef struct {
  uint8_t version;
  uint8_t_array *key;
} SCloudDecryptParameters;

SCloudDecryptParameters *SCloudDecryptParameters_init();
void SCloudDecryptParameters_free( SCloudDecryptParameters *this );

#endif/*__SCLOUD_DECRYPT_PARAMETERS_H__*/
