#ifndef __SCLOUD_ENCRYPT_PACKET_H__
#define __SCLOUD_ENCRYPT_PACKET_H__ 1

#include <stdint.h>
#include "uint8_t_array.h"
#include "scloud_encrypt_parameters.h"

typedef struct {

  uint8_t version;
  SCloudEncryptParameters *parameters;
  uint8_t_array *key;
  uint8_t_array *locator;
  uint8_t_array *data;

} SCloudEncryptPacket;

SCloudEncryptPacket *SCloudEncryptPacket_init( SCloudEncryptParameters *parameters );

void SCloudEncryptPacket_free( SCloudEncryptPacket *this );

SCLError SCloudEncryptPacket_encrypt( SCloudEncryptPacket *this, uint8_t_array *data );

SCLError SCloudEncryptPacketEventHandler( SCloudContextRef ctx, SCloudEvent* event, void *uservalue );

#endif/*__SCLOUD_ENCRYPT_PACKET_H__*/
