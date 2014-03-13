#include <jni.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <tomcrypt.h>
#include <SCloud.h>
#include "base64.h"
#include "uint8_t_array.h"
#include "scloud_encrypt_parameters.h"
#include "scloud_encrypt_packet.h"
#include "scloud_decrypt_parameters.h"
#include "scloud_decrypt_packet.h"

#include <android/log.h>
#define LOGE(...) __android_log_print( ANDROID_LOG_DEBUG, "scloud-jni", __VA_ARGS__ );

static jboolean jignore;
static jmethodID onDecrypted;
static jmethodID onEncrypted;
static int enabled = 0;

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

JNIEXPORT void JNICALL Java_com_silentcircle_scloud_NativePacket_onCreate( JNIEnv *jni, jobject this ) {

  jclass jSCloudPacket = (*jni)->GetObjectClass( jni, this );

  onDecrypted = (*jni)->GetMethodID( jni, jSCloudPacket, "onBlockDecrypted", "([B[B)V" );
  onEncrypted = (*jni)->GetMethodID( jni, jSCloudPacket, "onBlockEncrypted", "(Ljava/lang/String;Ljava/lang/String;[B)V" );

  SCLError error = kSCLError_NoErr;
  uint8_t_array *expected = uint8_t_array_parse( "Hello, world!" );
  SCloudEncryptParameters *encryptParameters = SCloudEncryptParameters_init();
  encryptParameters->context = uint8_t_array_parse( "example@silentcircle.com" );
  SCloudEncryptPacket *encryptPacket = SCloudEncryptPacket_init( encryptParameters );
  error = SCloudEncryptPacket_encrypt( encryptPacket, expected );
  if( error == kSCLError_NoErr ) {
    SCloudDecryptParameters *decryptParameters = SCloudDecryptParameters_init();
    decryptParameters->key = uint8_t_array_copy( encryptPacket->key->items, encryptPacket->key->size );
    SCloudDecryptPacket *decryptPacket = SCloudDecryptPacket_init( decryptParameters );
    error = SCloudDecryptPacket_decrypt( decryptPacket, encryptPacket->data );
    if( error == kSCLError_NoErr ) {
      uint8_t_array *actual = decryptPacket->data;
      if( expected->size == actual->size && memcmp( expected->items, actual->items, expected->size ) == 0 ) {
        enabled = 1;
      }
    }
    SCloudDecryptPacket_free( decryptPacket );
  }
  SCloudEncryptPacket_free( encryptPacket );
  uint8_t_array_free( expected );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scloud_NativePacket_onDestroy( JNIEnv *jni, jobject this ) {/*
  (*jni)->DeleteLocalRef( jni, onDecrypted );
  (*jni)->DeleteLocalRef( jni, onEncrypted );
*/}

JNIEXPORT void JNICALL Java_com_silentcircle_scloud_NativePacket_encrypt( JNIEnv *jni, jobject this, jstring jcontext, jstring jmetaData, jbyteArray jdata ) {

  if( !enabled ) { return; }

  const char *context = (*jni)->GetStringUTFChars( jni, jcontext, &jignore );
  const char *metaData = (*jni)->GetStringUTFChars( jni, jmetaData, &jignore );
  jbyte *data = (*jni)->GetByteArrayElements( jni, jdata, 0 );
  size_t dataSize = (size_t) (*jni)->GetArrayLength( jni, jdata );

  SCloudEncryptParameters *parameters = SCloudEncryptParameters_init();
  parameters->context = uint8_t_array_parse( context );
  parameters->metaData = uint8_t_array_parse( metaData );

  SCloudEncryptPacket *packet = SCloudEncryptPacket_init( parameters );
  uint8_t_array *inData = uint8_t_array_copy( data, dataSize );
  SCLError error = SCloudEncryptPacket_encrypt( packet, inData );
  uint8_t_array_free( inData );

  if( error == kSCLError_NoErr ) {

    packet->key->items[packet->key->size] = (char) 0;
    jstring jkey = (*jni)->NewStringUTF( jni, packet->key->items );

    char base64locator[64];
    sc_base64_encode( packet->locator->items, packet->locator->size, base64locator, 64 );
    jstring jlocator = (*jni)->NewStringUTF( jni, base64locator );
    jbyteArray joutData = (*jni)->NewByteArray( jni, sizeof(uint8_t) * packet->data->size );
    (*jni)->SetByteArrayRegion( jni, joutData, 0, sizeof(uint8_t) * packet->data->size, (jbyte*) packet->data->items );

    (*jni)->CallVoidMethod( jni, this, onEncrypted, jkey, jlocator, joutData );

    (*jni)->DeleteLocalRef( jni, joutData );
    (*jni)->DeleteLocalRef( jni, jlocator );
    (*jni)->DeleteLocalRef( jni, jkey );

  } else {
    LOGE("NativePacket#encrypt: Error Code: %d", error );
  }

  SCloudEncryptPacket_free( packet );

  (*jni)->ReleaseByteArrayElements( jni, jdata, data, JNI_ABORT );
  (*jni)->ReleaseStringUTFChars( jni, jmetaData, metaData );
  (*jni)->ReleaseStringUTFChars( jni, jcontext, context );

}

JNIEXPORT void JNICALL Java_com_silentcircle_scloud_NativePacket_decrypt( JNIEnv *jni, jobject this, jbyteArray jdata, jstring jkey ) {

  if( !enabled ) { return; }

  jbyte *data = (*jni)->GetByteArrayElements( jni, jdata, 0 );
  const char *key = (*jni)->GetStringUTFChars( jni, jkey, &jignore );

  SCloudDecryptParameters *parameters = SCloudDecryptParameters_init();
  parameters->key = uint8_t_array_parse( key );
  SCloudDecryptPacket *packet = SCloudDecryptPacket_init( parameters );
  uint8_t_array *inData = uint8_t_array_copy( data, (*jni)->GetArrayLength( jni, jdata ) );
  SCLError error = SCloudDecryptPacket_decrypt( packet, inData );
  uint8_t_array_free( inData );

  if( error == kSCLError_NoErr ) {

    jbyteArray joutData = (*jni)->NewByteArray( jni, sizeof(uint8_t) * packet->data->size );
    (*jni)->SetByteArrayRegion( jni, joutData, 0, sizeof(uint8_t) * packet->data->size, (jbyte*) packet->data->items );

    jbyteArray joutMetaData = (*jni)->NewByteArray( jni, sizeof(uint8_t) * packet->metaData->size );
    (*jni)->SetByteArrayRegion( jni, joutMetaData, 0, sizeof(uint8_t) * packet->metaData->size, (jbyte*) packet->metaData->items );

    (*jni)->CallVoidMethod( jni, this, onDecrypted, joutData, joutMetaData );

    (*jni)->DeleteLocalRef( jni, joutData );

  } else {
    LOGE( "NativePacket#decrypt: Error Code: %d", error );
  }

  SCloudDecryptPacket_free( packet );

  (*jni)->ReleaseStringUTFChars( jni, jkey, key );
  (*jni)->ReleaseByteArrayElements( jni, jdata, data, JNI_ABORT );

}
