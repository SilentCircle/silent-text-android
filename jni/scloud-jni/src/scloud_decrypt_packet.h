#ifndef __SCLOUD_DECRYPT_PACKET_H__
#define __SCLOUD_DECRYPT_PACKET_H__ 1

#include <stdint.h>
#include "uint8_t_array.h"
#include "scloud_decrypt_parameters.h"

typedef struct {

  uint8_t version;
  SCloudDecryptParameters *parameters;
  uint8_t_array *data;
  uint8_t_array *metaData;

} SCloudDecryptPacket;

SCloudDecryptPacket *SCloudDecryptPacket_init( SCloudDecryptParameters *parameters );

void SCloudDecryptPacket_free( SCloudDecryptPacket *this );

SCLError SCloudDecryptPacket_decrypt( SCloudDecryptPacket *this, uint8_t_array *data );

SCLError SCloudDecryptPacketEventHandler( SCloudContextRef ctx, SCloudEvent* event, void *uservalue );

#endif/*__SCLOUD_DECRYPT_PACKET_H__*/
