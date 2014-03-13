#include <SCimp.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <tomcrypt.h>
#include "scimp_keys.h"
#include "scimp_packet.h"
#include "uint8_t_array.h"

#define __COPY( to, from, length ) if( length < (size_t)-1 ) { to = realloc( to, length + 1 ); memcpy( to, from, length ); to[length] = (char) 0; }

static SCLError SCimpPacketEventHandler( SCimpContextRef context, SCimpEvent *event, void *misc ) {

  SCimpPacket *packet = misc;

  switch( event->type ) {

    case kSCimpEvent_Transition: {
      SCimpEventTransitionData data = event->data.transData;
      packet->state = data.state;
      switch( data.state ) {
        case kSCimpState_Commit: {
          // ...do something?
        } break;
        default: {
          // ...don't worry about it.
        } break;
      }
    } break;

    case kSCimpEvent_SendPacket: {
      packet->action = kSCimpPacket_Action_SEND;
      SCimpEventSendData data = event->data.sendData;
      packet->notifiable = data.shouldPush;
      packet->isPublicKeyData = data.isPKdata;
      __COPY( packet->outgoingData, data.data, data.length );
      printf( "SEND PACKET: %s\n", packet->outgoingData );
    } break;

    case kSCimpEvent_Decrypted: {
      packet->action = kSCimpPacket_Action_RECEIVE;
      SCimpEventDecryptData data = event->data.decryptData;
      __COPY( packet->decryptedData, data.data, data.length );
      printf( "DECRYPTED: %s\n", packet->decryptedData );
    } break;

    case kSCimpEvent_PubData: {
      // SCimpEventPubData data = event->data.pubData;
      // err = SCKeyDeserialize( d.data, d.length, &multicastKey ); CKERR;
    } break;

    case kSCimpEvent_Keyed: {

      SCimpInfo info = event->data.keyedData.info;
      size_t size = 0;
      SCimpGetAllocatedDataProperty( packet->scimp, kSCimpProperty_SASstring, (void*) &packet->secret, &size );

      if( packet->outgoingData == NULL ) {
        packet->action = kSCimpPacket_Action_CONNECT;
      }

      if( info.scimpMethod == kSCimpMethod_DH ) {
        SCimpAcceptSecret( packet->scimp );
      }

      // TODO: Remember the SCimp method. We need to distinguish between PKI keying and DH keying.

    } break;

    case kSCimpEvent_ReKeying: {
      // SCimpInfo info = event->data.keyedData.info;
      // TODO: What to do with this information?
    } break;

    case kSCimpEvent_NeedsPrivKey: {

      SCimpEventNeedsPrivKeyData data = event->data.needsKeyData;

      if( packet->getPrivateKey != NULL ) {
        packet->error = packet->getPrivateKey( data.locator, data.privKey );
      } else {
        packet->error = kSCLError_KeyNotFound;
      }

    } break;

    case kSCimpEvent_AdviseSaveState: {
      SCimpPacket_save( packet );
    } break;

    case kSCimpEvent_Error: {
      packet->error = event->data.errorData.error;
    } break;

    case kSCimpEvent_Warning: {
      packet->warning = event->data.warningData.warning;
    } break;

    default: {
      // ...don't worry about it.
    } break;

  }

  return packet->error;

}

SCimpPacket *SCimpPacket_init( uint8_t_array *storageKey ) {

  SCimpPacket *this = malloc( sizeof( SCimpPacket ) );

  this->version = 1;
  this->error = kSCLError_NoErr;
  this->warning = kSCLError_NoErr;
  this->storageKey = storageKey;
  this->outgoingData = NULL;
  this->decryptedData = NULL;
  this->context = NULL;
  this->scimp = NULL;
  this->secret = NULL;
  this->localUserID = NULL;
  this->remoteUserID = NULL;
  this->getPrivateKey = NULL;
  this->notifiable = 0;
  this->isPublicKeyData = 0;

  return this;

}

void SCimpPacket_free( SCimpPacket *this ) {
  if( this == NULL ) { return; }
  if( this->outgoingData != NULL ) { free( this->outgoingData ); this->outgoingData = NULL; }
  if( this->decryptedData != NULL ) { free( this->decryptedData ); this->decryptedData = NULL; }
  if( this->scimp != NULL ) { SCimpFree( this->scimp ); this->scimp = NULL; }
  if( this->context != NULL ) { free( this->context ); this->context = NULL; }
  if( this->storageKey != NULL ) { uint8_t_array_free( this->storageKey ); this->storageKey = NULL; }
  if( this->secret != NULL ) { free( this->secret ); this->secret = NULL; }
  if( this->localUserID != NULL ) { free( this->localUserID ); this->localUserID = NULL; }
  if( this->remoteUserID != NULL ) { free( this->remoteUserID ); this->remoteUserID = NULL; }
  this->getPrivateKey = NULL;
  this->notifiable = 0;
  this->isPublicKeyData = 0;
  free( this ); this = NULL;
}

SCimpPacket *SCimpPacket_create( uint8_t_array *storageKey, const char *localUserID, const char *remoteUserID ) {

  SCimpPacket *this = SCimpPacket_init( storageKey );

  __COPY( this->localUserID, localUserID, strlen( localUserID ) );
  __COPY( this->remoteUserID, remoteUserID, strlen( remoteUserID ) );

#define __IMPORTANT(statement) this->error = statement; if( this->error != kSCLError_NoErr ) { return this; }

  __IMPORTANT( SCimpNew( localUserID, remoteUserID, &this->scimp ) );
  __IMPORTANT( SCimpSetNumericProperty( this->scimp, kSCimpProperty_CipherSuite, kSCimpCipherSuite_SHA256_HMAC_AES128_ECC384 ) );
  __IMPORTANT( SCimpSetNumericProperty( this->scimp, kSCimpProperty_SASMethod, kSCimpSAS_NATO ) );

  __IMPORTANT( SCimpSetEventHandler( this->scimp, SCimpPacketEventHandler, (void*) this ) );
  __IMPORTANT( SCimpEnableTransitionEvents( this->scimp, true ) );

#undef __IMPORTANT

  return this;

}

SCimpPacket *SCimpPacket_restore( uint8_t_array *storageKey, const char *context ) {

  SCimpPacket *this = SCimpPacket_init( storageKey );

#define __IMPORTANT(statement) this->error = statement; if( this->error != kSCLError_NoErr ) { return this; }

  __IMPORTANT( SCimpRestoreState( this->storageKey->items, this->storageKey->size, (void*) context, strlen( context ), &this->scimp ) );
  __IMPORTANT( SCimpSetEventHandler( this->scimp, SCimpPacketEventHandler, (void*) this ) );
  __IMPORTANT( SCimpEnableTransitionEvents( this->scimp, true ) );

#undef __IMPORTANT

  if( SCimpPacket_isSecure( this ) ) {
    size_t size = 0;
    SCimpGetAllocatedDataProperty( this->scimp, kSCimpProperty_SASstring, (void*) &this->secret, &size );
  }

  return this;

}

int SCimpPacket_isSecure( SCimpPacket *this ) {
  SCimpInfo info;
  SCimpGetInfo( this->scimp, &info );
  return info.isReady ? 1 : 0;
}

static void captureError( SCimpPacket *this, SCLError error ) { if( this->error == kSCLError_NoErr ) { this->error = error; } }
#define CAPTURE_ERROR( from ) captureError( this, from );

void SCimpPacket_receivePacket( SCimpPacket *this, const char *data ) {
  CAPTURE_ERROR( SCimpProcessPacket( this->scimp, (void*) data, strlen(data), (void*) this ) );
}

void SCimpPacket_sendPacket( SCimpPacket *this, const char *data ) {
  CAPTURE_ERROR( SCimpSendMsg( this->scimp, (void*) data, strlen(data), (void*) this ) );
}

void SCimpPacket_connect( SCimpPacket *this ) {
  CAPTURE_ERROR( SCimpStartDH( this->scimp ) );
  this->action = kSCimpPacket_Action_CONNECT;
}

void SCimpPacket_setPublicKey( SCimpPacket *this, uint8_t_array *publicKey ) {

  SCKeyContextRef remotePublicKey = kInvalidSCKeyContextRef;

  SCimpInfo info;
  ZERO( &info, sizeof(info) );
  SCimpGetInfo( this->scimp, &info );

  if( info.canPKstart ) {
    CAPTURE_ERROR( SCimp_importPublicKey( publicKey, &remotePublicKey ) );
    CAPTURE_ERROR( SCimpStartPublicKey( this->scimp, remotePublicKey, time(NULL) + ( 3600 * 24 ) ) );
  }

  ZERO( &info, sizeof(info) );

}

void SCimpPacket_setPrivateKey( SCimpPacket *this, uint8_t_array *privateKey, uint8_t_array *storageKey ) {

  SCKeyContextRef localPrivateKey = kInvalidSCKeyContextRef;

  CAPTURE_ERROR( SCimp_importPrivateKey( privateKey, storageKey, &localPrivateKey ) );
  CAPTURE_ERROR( SCimpSetPrivateKey( this->scimp, localPrivateKey ) ); localPrivateKey = NULL;

}

void SCimpPacket_save( SCimpPacket *this ) {

  if( this->error != kSCLError_NoErr ) { return; }

  void *stateBuffer = NULL;
  size_t stateBufferSize = 0;

  CAPTURE_ERROR( SCimpSaveState( this->scimp, this->storageKey->items, this->storageKey->size, &stateBuffer, &stateBufferSize ) );

  if( this->error != kSCLError_NoErr ) { return; }

  __COPY( this->context, stateBuffer, stateBufferSize );

}

#undef CAPTURE_ERROR
#undef __COPY
