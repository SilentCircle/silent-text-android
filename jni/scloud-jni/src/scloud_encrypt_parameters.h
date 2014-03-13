#ifndef __SCLOUD_ENCRYPT_PARAMETERS_H__
#define __SCLOUD_ENCRYPT_PARAMETERS_H__ 1

#include <stdint.h>
#include "uint8_t_array.h"

typedef struct {

  uint8_t version;
  uint8_t_array *context;
  uint8_t_array *metaData;

} SCloudEncryptParameters;

SCloudEncryptParameters *SCloudEncryptParameters_init();
void SCloudEncryptParameters_free( SCloudEncryptParameters *this );

#endif/*__SCLOUD_ENCRYPT_PARAMETERS_H__*/
