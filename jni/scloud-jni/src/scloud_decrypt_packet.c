#include <stdlib.h>
#include <string.h>
#include <SCloud.h>
#include "uint8_t_array.h"
#include "scloud_decrypt_parameters.h"
#include "scloud_decrypt_packet.h"

SCloudDecryptPacket *SCloudDecryptPacket_init( SCloudDecryptParameters *parameters ) {

  SCloudDecryptPacket *this = malloc( sizeof( SCloudDecryptPacket ) );

  this->version = 1;
  this->parameters = parameters;
  this->data = uint8_t_array_init();
  this->metaData = uint8_t_array_init();

  return this;

}

void SCloudDecryptPacket_free( SCloudDecryptPacket *this ) {

  if( this == NULL ) { return; }

  if( this->parameters != NULL ) { SCloudDecryptParameters_free( this->parameters ); this->parameters = NULL; }
  if( this->data != NULL ) { uint8_t_array_free( this->data ); this->data = NULL; }
  if( this->metaData != NULL ) { uint8_t_array_free( this->metaData ); this->metaData = NULL; }

  free( this );

}

SCLError SCloudDecryptPacket_decrypt( SCloudDecryptPacket *this, uint8_t_array *data ) {

  SCLError err = kSCLError_NoErr;
  SCloudContextRef scloud = NULL;
  uint8_t_array *key = this->parameters->key;

  err = SCloudDecryptNew( key->items, key->size, SCloudDecryptPacketEventHandler, (void*) this, &scloud ); CKERR;
  err = SCloudDecryptNext( scloud, data->items, data->size ); CKERR;

done:

  if( IsntNull(scloud) ) { SCloudFree( scloud ); }

  return err;

}

SCLError SCloudDecryptPacketEventHandler( SCloudContextRef ctx, SCloudEvent* event, void *uservalue ) {

  SCloudDecryptPacket *packet = uservalue;

  switch( event->type ) {

    case kSCloudEvent_DecryptedData: {

      SCloudEventDecryptData *d = &event->data.decryptData;
      uint8_t_array *out = packet->data;

      out->size += d->length;
      out->items = realloc( out->items, out->size );

      memcpy( out->items + ( out->size - d->length ), d->data, d->length );

    } break;

    case kSCloudEvent_DecryptedMetaData: {

      SCloudEventDecryptMetaData *d = &event->data.metaData;
      uint8_t_array *out = packet->metaData;

      out->size += d->length;
      out->items = realloc( out->items, out->size );

      memcpy( out->items + ( out->size - d->length ), d->data, d->length );

    } break;


    default: {
      // Do nothing.
    } break;

  }

  return kSCLError_NoErr;

}
