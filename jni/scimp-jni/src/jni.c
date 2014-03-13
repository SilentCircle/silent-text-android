#include <jni.h>
#include <string.h>
#include <tomcrypt.h>
#include <SCimp.h>
#include "scimp_keys.h"
#include "scimp_packet.h"
#include "uint8_t_array.h"

static jmethodID onConnect;
static jmethodID onSendPacket;
static jmethodID onReceivePacket;
static jmethodID onError;
static jmethodID onWarning;
static jmethodID onStateTransition;
static jmethodID getPrivateKey;
static jmethodID getPrivateKeyStorageKey;

extern ltc_math_descriptor ltc_mp;

jint JNI_OnLoad( JavaVM *vm, void *reserved ) {

  register_prng( &sprng_desc );

  register_hash( &sha256_desc );
  register_hash( &sha512_desc );
  register_hash( &sha512_256_desc );
  register_hash( &skein512_desc );
  register_hash( &skein256_desc );
  register_hash( &skein512_desc );

  register_cipher( &aes_desc );

  ltc_mp = ltm_desc;

  return JNI_VERSION_1_6;

}

void JNI_OnUnLoad( JavaVM *vm, void *reserved ) {
  // ...perform any necessary cleanup here...
}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_onCreate( JNIEnv *jni, jobject this ) {

  jclass type = (*jni)->GetObjectClass( jni, this );

  onConnect = (*jni)->GetMethodID( jni, type, "onConnect", "([BLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V" );
  onSendPacket = (*jni)->GetMethodID( jni, type, "onSendPacket", "([BLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V" );
  onReceivePacket = (*jni)->GetMethodID( jni, type, "onReceivePacket", "([BLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V" );
  onError = (*jni)->GetMethodID( jni, type, "onError", "([BLjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V" );
  onWarning = (*jni)->GetMethodID( jni, type, "onWarning", "([BLjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V" );
  onStateTransition = (*jni)->GetMethodID( jni, type, "onStateTransition", "([BLjava/lang/String;Ljava/lang/String;I)V" );
  getPrivateKey = (*jni)->GetMethodID( jni, type, "getPrivateKey", "(Ljava/lang/String;)[B" );
  getPrivateKeyStorageKey = (*jni)->GetMethodID( jni, type, "getPrivateKeyStorageKey", "(Ljava/lang/String;)[B" );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_onDestroy( JNIEnv *jni, jobject this ) {/*
  (*jni)->DeleteLocalRef( jni, onStateTransition );
  (*jni)->DeleteLocalRef( jni, onWarning );
  (*jni)->DeleteLocalRef( jni, onError );
  (*jni)->DeleteLocalRef( jni, onReceivePacket );
  (*jni)->DeleteLocalRef( jni, onSendPacket );
  (*jni)->DeleteLocalRef( jni, getPrivateKey );
  (*jni)->DeleteLocalRef( jni, getPrivateKeyStorageKey );
*/}

#define NEW_BYTES( jname, name, nameSize ) jbyte *name = (*jni)->GetByteArrayElements( jni, jname, 0 ); size_t nameSize = (size_t) (*jni)->GetArrayLength( jni, jname );
#define NEW_STRING( jname, name ) const char *name = jname == NULL ? NULL : (*jni)->GetStringUTFChars( jni, jname, NULL );
#define FREE_STRING( jname, name ) if( jname != NULL ) { (*jni)->ReleaseStringUTFChars( jni, jname, name ); }
#define NEW_OUT_STRING( jname, name ) jstring jname = name == NULL ? NULL : (*jni)->NewStringUTF( jni, name );
#define FREE_OUT_STRING( jname ) if( jname != NULL ) { (*jni)->DeleteLocalRef( jni, jname ); }
#define FREE_BYTES( jname, name )  (*jni)->ReleaseByteArrayElements( jni, jname, name, JNI_ABORT );

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_resetStorageKey( JNIEnv *jni, jobject this, jbyteArray joldStorageKey, jstring jcontext, jbyteArray jnewStorageKey ) {

  NEW_STRING( jcontext, context );
  NEW_BYTES( joldStorageKey, oldStorageKey, oldStorageKeySize );
  NEW_BYTES( jnewStorageKey, newStorageKey, newStorageKeySize );

  uint8_t_array *inOldStorageKey = uint8_t_array_copy( oldStorageKey, oldStorageKeySize );
  SCimpPacket *packet = SCimpPacket_restore( inOldStorageKey, context );
  uint8_t_array_free( inOldStorageKey );

  packet->storageKey = uint8_t_array_copy( newStorageKey, newStorageKeySize );
  SCimpPacket_save( packet );

  SCimpPacket_free( packet );
  FREE_STRING( jcontext, context );
  FREE_BYTES( joldStorageKey, oldStorageKey );
  FREE_BYTES( jnewStorageKey, newStorageKey );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_connect( JNIEnv *jni, jobject this, jbyteArray jstorageKey, jstring jpacketID, jstring jlocalUserID, jstring jremoteUserID, jstring jcontext ) {

  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );
  NEW_STRING( jpacketID, packetID );
  NEW_STRING( jlocalUserID, localUserID );
  NEW_STRING( jremoteUserID, remoteUserID );
  NEW_STRING( jcontext, context );

  SCimpPacket *packet = NULL;

  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );

  if( context != NULL && strlen(context) > 0 ) {
    packet = SCimpPacket_restore( storageKey, context );
  } else {
    packet = SCimpPacket_create( storageKey, localUserID, remoteUserID );
  }

  if( packet->error == kSCLError_NoErr ) {
    SCimpPacket_connect( packet );
  }

  if( packet->error != kSCLError_NoErr ) {
    jint jerror = (jint) packet->error;
    (*jni)->CallVoidMethod( jni, this, onError, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jerror );
  }

  if( packet->warning != kSCLError_NoErr ) {
    jint jwarning = (jint) packet->warning;
    (*jni)->CallVoidMethod( jni, this, onWarning, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jwarning );
  }

  switch( packet->action ) {

    case kSCimpPacket_Action_CONNECT: {

      NEW_OUT_STRING( joutData, packet->outgoingData );
      NEW_OUT_STRING( joutContext, packet->context );
      NEW_OUT_STRING( joutSecret, packet->secret );

      (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret );

      FREE_OUT_STRING( joutSecret );
      FREE_OUT_STRING( joutContext );
      FREE_OUT_STRING( joutData );

    } break;

  }

  //uint8_t_array_free( storageKey );

  SCimpPacket_free( packet );

  FREE_STRING( jcontext, context );
  FREE_STRING( jremoteUserID, remoteUserID );
  FREE_STRING( jlocalUserID, localUserID );
  FREE_STRING( jpacketID, packetID );
  FREE_BYTES( jstorageKey, storageKeyItems );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_receivePacketPKI( JNIEnv *jni, jobject this, jbyteArray jstorageKey, jbyteArray jprivateKeyStorageKey, jbyteArray jprivateKey, jbyteArray jpublicKey, jstring jpacketID, jstring jlocalUserID, jstring jremoteUserID, jstring jdata, jstring jcontext, jboolean jnotifiable, jboolean jbadgeworthy ) {

  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );
  NEW_BYTES( jprivateKeyStorageKey, privateKeyStorageKeyItems, privateKeyStorageKeySize );
  NEW_BYTES( jprivateKey, privateKeyItems, privateKeySize );
  NEW_BYTES( jpublicKey, publicKeyItems, publicKeySize );
  NEW_STRING( jpacketID, packetID );
  NEW_STRING( jlocalUserID, localUserID );
  NEW_STRING( jremoteUserID, remoteUserID );
  NEW_STRING( jdata, data );
  NEW_STRING( jcontext, context );

  SCimpPacket *packet = NULL;

  uint8_t_array *privateKeyStorageKey = uint8_t_array_copy( privateKeyStorageKeyItems, privateKeyStorageKeySize );
  uint8_t_array *privateKey = uint8_t_array_copy( privateKeyItems, privateKeySize );
  uint8_t_array *publicKey = uint8_t_array_copy( publicKeyItems, publicKeySize );
  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );

  if( context != NULL && strlen(context) > 0 ) {
    packet = SCimpPacket_restore( storageKey, context );
  } else {
    packet = SCimpPacket_create( storageKey, localUserID, remoteUserID );
  }

  if( !SCimpPacket_isSecure( packet ) ) {

    if( packet->error == kSCLError_NoErr ) {
      SCimpPacket_setPrivateKey( packet, privateKey, privateKeyStorageKey );
    }

    if( packet->error == kSCLError_NoErr ) {
      // SCimpPacket_setPublicKey( packet, publicKey );// Don't PKSTART on an incoming packet.
    }

  }

  if( packet->error == kSCLError_NoErr ) {
    SCimpPacket_receivePacket( packet, data );
  }

  if( packet->error != kSCLError_NoErr ) {
    jint jerror = (jint) packet->error;
    (*jni)->CallVoidMethod( jni, this, onError, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jerror );
  }

  if( packet->warning != kSCLError_NoErr ) {
    jint jwarning = (jint) packet->warning;
    (*jni)->CallVoidMethod( jni, this, onWarning, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jwarning );
  }

  if( packet->decryptedData != NULL ) {

    NEW_OUT_STRING( joutData, packet->decryptedData );
    NEW_OUT_STRING( joutContext, packet->context );
    NEW_OUT_STRING( joutSecret, packet->secret );

    (*jni)->CallVoidMethod( jni, this, onReceivePacket, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, jnotifiable, jbadgeworthy );

    FREE_OUT_STRING( joutSecret );
    FREE_OUT_STRING( joutContext );
    FREE_OUT_STRING( joutData );

  }

  switch( packet->action ) {

    case kSCimpPacket_Action_CONNECT: {

      NEW_OUT_STRING( joutContext, packet->context );
      NEW_OUT_STRING( joutSecret, packet->secret );

      (*jni)->CallVoidMethod( jni, this, onConnect, jstorageKey, NULL, jlocalUserID, jremoteUserID, joutContext, joutSecret );

      FREE_OUT_STRING( joutSecret );
      FREE_OUT_STRING( joutContext );

    } break;

    case kSCimpPacket_Action_SEND: {

      if( packet->outgoingData != NULL ) {

        NEW_OUT_STRING( joutData, packet->outgoingData );
        NEW_OUT_STRING( joutContext, packet->context );
        NEW_OUT_STRING( joutSecret, packet->secret );

        (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, NULL, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, jnotifiable, jbadgeworthy );

        FREE_OUT_STRING( joutSecret );
        FREE_OUT_STRING( joutContext );
        FREE_OUT_STRING( joutData );

      }

    } break;

  }

  //uint8_t_array_free( storageKey );
  //uint8_t_array_free( publicKey );
  //uint8_t_array_free( privateKey );
  //uint8_t_array_free( privateKeyStorageKey );

  SCimpPacket_free( packet );

  FREE_STRING( jcontext, context );
  FREE_STRING( jdata, data );
  FREE_STRING( jremoteUserID, remoteUserID );
  FREE_STRING( jlocalUserID, localUserID );
  FREE_STRING( jpacketID, packetID );
  FREE_BYTES( jpublicKey, publicKeyItems );
  FREE_BYTES( jprivateKey, privateKeyItems );
  FREE_BYTES( jprivateKeyStorageKey, privateKeyStorageKeyItems );
  FREE_BYTES( jstorageKey, storageKeyItems );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_receivePacket( JNIEnv *jni, jobject this, jbyteArray jstorageKey, jstring jpacketID, jstring jlocalUserID, jstring jremoteUserID, jstring jdata, jstring jcontext, jboolean jnotifiable, jboolean jbadgeworthy ) {

  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );
  NEW_STRING( jpacketID, packetID );
  NEW_STRING( jlocalUserID, localUserID );
  NEW_STRING( jremoteUserID, remoteUserID );
  NEW_STRING( jdata, data );
  NEW_STRING( jcontext, context );

  SCimpPacket *packet = NULL;

  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );

  if( context != NULL && strlen(context) > 0 ) {
    packet = SCimpPacket_restore( storageKey, context );
  } else {
    packet = SCimpPacket_create( storageKey, localUserID, remoteUserID );
  }

  if( packet->error == kSCLError_NoErr ) {
    SCimpPacket_receivePacket( packet, data );
  }

  if( packet->error != kSCLError_NoErr ) {
    jint jerror = (jint) packet->error;
    (*jni)->CallVoidMethod( jni, this, onError, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jerror );
  }

  if( packet->warning != kSCLError_NoErr ) {
    jint jwarning = (jint) packet->warning;
    (*jni)->CallVoidMethod( jni, this, onWarning, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jwarning );
  }

  if( packet->decryptedData != NULL ) {

    NEW_OUT_STRING( joutData, packet->decryptedData );
    NEW_OUT_STRING( joutContext, packet->context );
    NEW_OUT_STRING( joutSecret, packet->secret );

    (*jni)->CallVoidMethod( jni, this, onReceivePacket, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, jnotifiable, jbadgeworthy );

    FREE_OUT_STRING( joutSecret );
    FREE_OUT_STRING( joutContext );
    FREE_OUT_STRING( joutData );

  }

  switch( packet->action ) {

    case kSCimpPacket_Action_CONNECT: {

        NEW_OUT_STRING( joutContext, packet->context );
        NEW_OUT_STRING( joutSecret, packet->secret );

        (*jni)->CallVoidMethod( jni, this, onConnect, jstorageKey, NULL, jlocalUserID, jremoteUserID, joutContext, joutSecret );

        FREE_OUT_STRING( joutSecret );
        FREE_OUT_STRING( joutContext );

    } break;

    case kSCimpPacket_Action_SEND: {

      if( packet->outgoingData != NULL ) {

        NEW_OUT_STRING( joutData, packet->outgoingData );
        NEW_OUT_STRING( joutContext, packet->context );
        NEW_OUT_STRING( joutSecret, packet->secret );

        (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, NULL, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, jnotifiable, jbadgeworthy );

        FREE_OUT_STRING( joutSecret );
        FREE_OUT_STRING( joutContext );
        FREE_OUT_STRING( joutData );

      }

    } break;

  }

  //uint8_t_array_free( storageKey );

  SCimpPacket_free( packet );

  FREE_STRING( jcontext, context );
  FREE_STRING( jdata, data );
  FREE_STRING( jremoteUserID, remoteUserID );
  FREE_STRING( jlocalUserID, localUserID );
  FREE_STRING( jpacketID, packetID );
  FREE_BYTES( jstorageKey, storageKeyItems );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_sendPacket( JNIEnv *jni, jobject this, jbyteArray jstorageKey, jstring jpacketID, jstring jlocalUserID, jstring jremoteUserID, jstring jdata, jstring jcontext, jboolean jnotifiable, jboolean jbadgeworthy ) {

  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );
  NEW_STRING( jpacketID, packetID );
  NEW_STRING( jlocalUserID, localUserID );
  NEW_STRING( jremoteUserID, remoteUserID );
  NEW_STRING( jdata, data );
  NEW_STRING( jcontext, context );

  SCimpPacket *packet = NULL;

  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );

  if( context != NULL && strlen(context) > 0 ) {
    packet = SCimpPacket_restore( storageKey, context );
    if( packet->error == kSCLError_NoErr ) {
      SCimpPacket_sendPacket( packet, data );
    }
  } else {
    packet = SCimpPacket_create( storageKey, localUserID, remoteUserID );
    if( packet->error == kSCLError_NoErr ) {
      SCimpPacket_connect( packet );
    }
  }

  packet->notifiable = jnotifiable;

  if( packet->error != kSCLError_NoErr ) {
    jint jerror = (jint) packet->error;
    (*jni)->CallVoidMethod( jni, this, onError, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jerror );
  }

  if( packet->warning != kSCLError_NoErr ) {
    jint jwarning = (jint) packet->warning;
    (*jni)->CallVoidMethod( jni, this, onWarning, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jwarning );
  }

    switch( packet->action ) {

      case kSCimpPacket_Action_CONNECT: {

        if( packet->outgoingData != NULL ) {

          NEW_OUT_STRING( joutData, packet->outgoingData );
          NEW_OUT_STRING( joutContext, packet->context );
          NEW_OUT_STRING( joutSecret, packet->secret );

          (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, NULL, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, (jboolean) packet->notifiable, jbadgeworthy );

          FREE_OUT_STRING( joutSecret );
          FREE_OUT_STRING( joutContext );
          FREE_OUT_STRING( joutData );

        }

      } break;

      case kSCimpPacket_Action_SEND: {

        if( packet->outgoingData != NULL ) {

          NEW_OUT_STRING( joutData, packet->outgoingData );
          NEW_OUT_STRING( joutContext, packet->context );
          NEW_OUT_STRING( joutSecret, packet->secret );

          (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, (jboolean) packet->notifiable, jbadgeworthy );

          FREE_OUT_STRING( joutSecret );
          FREE_OUT_STRING( joutContext );
          FREE_OUT_STRING( joutData );

        }

      } break;

    }

  //uint8_t_array_free( storageKey );

  SCimpPacket_free( packet );

  FREE_STRING( jcontext, context );
  FREE_STRING( jdata, data );
  FREE_STRING( jremoteUserID, remoteUserID );
  FREE_STRING( jlocalUserID, localUserID );
  FREE_STRING( jpacketID, packetID );
  FREE_BYTES( jstorageKey, storageKeyItems );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scimp_NativePacket_sendPacketPKI( JNIEnv *jni, jobject this, jbyteArray jstorageKey, jbyteArray jprivateKeyStorageKey, jbyteArray jprivateKey, jbyteArray jpublicKey, jstring jpacketID, jstring jlocalUserID, jstring jremoteUserID, jstring jdata, jstring jcontext, jboolean jnotifiable, jboolean jbadgeworthy ) {

  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );
  NEW_BYTES( jprivateKeyStorageKey, privateKeyStorageKeyItems, privateKeyStorageKeySize );
  NEW_BYTES( jprivateKey, privateKeyItems, privateKeySize );
  NEW_BYTES( jpublicKey, publicKeyItems, publicKeySize );
  NEW_STRING( jpacketID, packetID );
  NEW_STRING( jlocalUserID, localUserID );
  NEW_STRING( jremoteUserID, remoteUserID );
  NEW_STRING( jdata, data );
  NEW_STRING( jcontext, context );

  SCimpPacket *packet = NULL;

  uint8_t_array *privateKeyStorageKey = uint8_t_array_copy( privateKeyStorageKeyItems, privateKeyStorageKeySize );
  uint8_t_array *privateKey = uint8_t_array_copy( privateKeyItems, privateKeySize );
  uint8_t_array *publicKey = uint8_t_array_copy( publicKeyItems, publicKeySize );
  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );

  if( context != NULL && strlen(context) > 0 ) {

    packet = SCimpPacket_restore( storageKey, context );

  } else {

    packet = SCimpPacket_create( storageKey, localUserID, remoteUserID );

    if( packet->error == kSCLError_NoErr ) {
      SCimpPacket_setPublicKey( packet, publicKey );
    }

  }

  packet->notifiable = jnotifiable;

  if( !SCimpPacket_isSecure( packet ) ) {

    if( packet->error == kSCLError_NoErr ) {
      SCimpPacket_setPrivateKey( packet, privateKey, privateKeyStorageKey );
    }

  }

  if( packet->error == kSCLError_NoErr ) {
    SCimpPacket_sendPacket( packet, data );
  }

  if( packet->error != kSCLError_NoErr ) {
    jint jerror = (jint) packet->error;
    (*jni)->CallVoidMethod( jni, this, onError, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jerror );
  }

  if( packet->warning != kSCLError_NoErr ) {
    jint jwarning = (jint) packet->warning;
    (*jni)->CallVoidMethod( jni, this, onWarning, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, jwarning );
  }

    switch( packet->action ) {

      case kSCimpPacket_Action_CONNECT: {

        if( packet->outgoingData != NULL ) {

          NEW_OUT_STRING( joutData, packet->outgoingData );
          NEW_OUT_STRING( joutContext, packet->context );
          NEW_OUT_STRING( joutSecret, packet->secret );

          (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, NULL, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, (jboolean) packet->notifiable, jbadgeworthy );

          FREE_OUT_STRING( joutSecret );
          FREE_OUT_STRING( joutContext );
          FREE_OUT_STRING( joutData );

        }

      } break;

      case kSCimpPacket_Action_SEND: {

        if( packet->outgoingData != NULL ) {

          NEW_OUT_STRING( joutData, packet->outgoingData );
          NEW_OUT_STRING( joutContext, packet->context );
          NEW_OUT_STRING( joutSecret, packet->secret );

          (*jni)->CallVoidMethod( jni, this, onSendPacket, jstorageKey, jpacketID, jlocalUserID, jremoteUserID, joutData, joutContext, joutSecret, (jboolean) packet->notifiable, jbadgeworthy );

          FREE_OUT_STRING( joutSecret );
          FREE_OUT_STRING( joutContext );
          FREE_OUT_STRING( joutData );

        }

      } break;

    }

  //uint8_t_array_free( storageKey );
  //uint8_t_array_free( publicKey );
  //uint8_t_array_free( privateKey );
  //uint8_t_array_free( privateKeyStorageKey );

  SCimpPacket_free( packet );

  FREE_STRING( jcontext, context );
  FREE_STRING( jdata, data );
  FREE_STRING( jremoteUserID, remoteUserID );
  FREE_STRING( jlocalUserID, localUserID );
  FREE_STRING( jpacketID, packetID );
  FREE_BYTES( jpublicKey, publicKeyItems );
  FREE_BYTES( jprivateKey, privateKeyItems );
  FREE_BYTES( jprivateKeyStorageKey, privateKeyStorageKeyItems );
  FREE_BYTES( jstorageKey, storageKeyItems );

}

JNIEXPORT jbyteArray JNICALL Java_com_silentcircle_scimp_NativeKeyGenerator_generateKey( JNIEnv *jni, jobject this, jstring jowner, jbyteArray jstorageKey ) {

  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );
  NEW_STRING( jowner, owner );

  SCKeyContextRef key = kInvalidSCKeyContextRef;
  uint8_t_array *outKey = uint8_t_array_init();
  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );

  SCimp_generatePrivateKey( &key, owner );
  SCimp_exportPrivateKey( key, storageKey, outKey );

  jbyteArray joutKey = (*jni)->NewByteArray( jni, outKey->size );

  (*jni)->SetByteArrayRegion( jni, joutKey, 0, outKey->size, outKey->items );

  //uint8_t_array_free( outKey );
  uint8_t_array_free( storageKey );
  SCKeyFree( key );
  FREE_STRING( jowner, owner );
  FREE_BYTES( jstorageKey, storageKeyItems );

  return joutKey;

}

JNIEXPORT jbyteArray JNICALL Java_com_silentcircle_scimp_NativeKeyGenerator_getPublicKey( JNIEnv *jni, jobject this, jbyteArray jprivateKey, jbyteArray jstorageKey ) {

  NEW_BYTES( jprivateKey, privateKeyItems, privateKeySize );
  NEW_BYTES( jstorageKey, storageKeyItems, storageKeySize );

  SCKeyContextRef key = kInvalidSCKeyContextRef;
  uint8_t_array *privateKey = uint8_t_array_copy( privateKeyItems, privateKeySize );
  uint8_t_array *storageKey = uint8_t_array_copy( storageKeyItems, storageKeySize );
  uint8_t_array *publicKey = uint8_t_array_init();

  SCimp_importPrivateKey( privateKey, storageKey, &key );
  SCimp_exportPublicKey( key, publicKey );

  jbyteArray jpublicKey = (*jni)->NewByteArray( jni, publicKey->size );

  (*jni)->SetByteArrayRegion( jni, jpublicKey, 0, publicKey->size, publicKey->items );

  //uint8_t_array_free( publicKey );
  //uint8_t_array_free( storageKey );
  //uint8_t_array_free( privateKey );
  SCKeyFree( key );
  FREE_BYTES( jprivateKey, privateKeyItems );
  FREE_BYTES( jstorageKey, storageKeyItems );

  return jpublicKey;

}

JNIEXPORT jint JNICALL Java_com_silentcircle_scimp_NativePacket_testSCKeyDeserialize( JNIEnv *jni, jobject this, jstring jserializedKey ) {

  NEW_STRING( jserializedKey, serializedKey );
  SCKeyContextRef key = NULL;
  uint8_t *in = (uint8_t*) serializedKey;
  size_t inSize = strlen(serializedKey);
  SCLError err = SCKeyDeserialize( in, inSize, &key );

  if( key != NULL ) {
    SCKeyFree( key );
  }

  FREE_STRING( jserializedKey, serializedKey );

  return (jint) err;

}

#undef FREE_BYTES
#undef FREE_OUT_STRING
#undef NEW_OUT_STRING
#undef FREE_STRING
#undef NEW_STRING
#undef NEW_BYTES
