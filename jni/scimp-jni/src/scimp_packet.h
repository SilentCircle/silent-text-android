#ifndef __SCIMP_PACKET_H__
#define __SCIMP_PACKET_H__ 1

#include <stdint.h>
#include <SCkeys.h>
#include <SCimp.h>
#include "uint8_t_array.h"

#define kSCimpPacket_Action_CONNECT 0
#define kSCimpPacket_Action_SEND    1
#define kSCimpPacket_Action_RECEIVE 2

typedef SCLError (*SCimpPacket_getPrivateKey)( char *locator, SCKeyContextRef outPrivateKey );

typedef struct {
  uint8_t version;
  SCimpContextRef scimp;
  SCLError warning;
  SCLError error;
  int action;
  SCimpState state;
  char *decryptedData;
  char *outgoingData;
  char *context;
  uint8_t_array *storageKey;
  char *secret;
  char *localUserID;
  char *remoteUserID;
  SCimpPacket_getPrivateKey getPrivateKey;
  int notifiable;
  int isPublicKeyData;
} SCimpPacket;

SCimpPacket *SCimpPacket_init( uint8_t_array *storageKey );

void SCimpPacket_free( SCimpPacket *this );

SCimpPacket *SCimpPacket_create( uint8_t_array *storageKey, const char *localUserID, const char *remoteUserID );

SCimpPacket *SCimpPacket_restore( uint8_t_array *storageKey, const char *context );

void SCimpPacket_save( SCimpPacket *this );

void SCimpPacket_receivePacket( SCimpPacket *this, const char *data );

void SCimpPacket_sendPacket( SCimpPacket *this, const char *data );

void SCimpPacket_connect( SCimpPacket *this );

void SCimpPacket_setPrivateKey( SCimpPacket *this, uint8_t_array *privateKey, uint8_t_array *storageKey );

void SCimpPacket_setPublicKey( SCimpPacket *this, uint8_t_array *publicKey );

int SCimpPacket_isSecure( SCimpPacket *this );

#endif/*__SCIMP_PACKET_H__*/
