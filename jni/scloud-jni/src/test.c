#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <tomcrypt.h>
#include <cryptowrappers.h>
#include <SCloud.h>
#include "scloud_packet.h"
#include "hprint.h"

static char *banter[] = {
  "Hello. My name is Inigo Montoya. You killed my father. Prepare to die.12345",
  " Finish him. Finish him, your way.",
  "Oh good, my way. Thank you Vizzini... what's my way?",
  " Pick up one of those rocks, get behind a boulder, in a few minutes the man in black will come running around the bend, the minute his head is in view, hit it with the rock.",
  "My way's not very sportsman-like. ",
  "Why do you wear a mask? Were you burned by acid, or something like that?",
  " Oh no, it's just that they're terribly comfortable. I think everyone will be wearing them in the future.",
  " I do not envy you the headache you will have when you awake. But for now, rest well and dream of large women.",
  " I just want you to feel you're doing well.",
  "That Vizzini, he can *fuss*" ,
  "Fuss, fuss... I think he like to scream at *us*.",
  "Probably he means no *harm*. ",
  "He's really very short on *charm*." ,
  "You have a great gift for rhyme." ,
  "Yes, yes, some of the time.",
  "Enough of that.",
  "Fezzik, are there rocks ahead? ",
  "If there are, we all be dead. ",
  "No more rhymes now, I mean it. ",
  "Anybody want a peanut?",
  "short",
  "no",
  "",
  NULL
};

SCLError sEventHandler( SCloudContextRef ctx, SCloudEvent* event, void *uservalue ) {

  SCloudPacket *packet = (SCloudPacket*) uservalue;

  SCLError err= kSCLError_NoErr;

  switch( event->type ) {

    case kSCloudEvent_DecryptedData: {
      SCloudEventDecryptData *d = &event->data.decryptData;
      printf( " Plain: %.*s\n\n", (int) d->length, d->data );
      if( !strcmp( (char*) packet->extra, (char*) d->data ) ) {
        printf( "ERROR: [%s] != [%.*s]\n", (char*) packet->extra, (int) d->length, (char*) d->data );
      }
    } break;

    default: {
      // Ignore.
    } break;

  }

  return err;

}

static SCLError TestSCloud_encrypt( uint8_t *plaintext, size_t plaintextSize, SCloudPacket *packet ) {

  SCLError err = kSCLError_NoErr;

  SCloudContextRef scloud = NULL;

  uint8_t context[8];// This cannot be of size > 8.
  size_t contextLen = 0;

  uint8_t metaData[17];// This cannot be of size <= 16.
  size_t metaDataLen = 0;

  err = SCloudEncryptNew( context, contextLen, plaintext, plaintextSize, metaData, metaDataLen, sEventHandler, (void*) packet, &scloud ); CKERR;
  err = SCloudCalculateKey( scloud, 1024 ); CKERR;
  err = SCloudEncryptGetKeyBLOB( scloud, &packet->key, &packet->keySize ); CKERR;
  packet->locatorSize = 4 * 1024;
  packet->locator = malloc( packet->locatorSize );
  err = SCloudEncryptGetLocator( scloud, packet->locator, &packet->locatorSize ); CKERR;

  packet->data = malloc( 4 * 1024 * 1024 );
  size_t dataSize;
  while( err == kSCLError_NoErr ) {
    err = SCloudEncryptNext( scloud, packet->data + packet->dataSize, &dataSize );
    packet->dataSize += dataSize;
  }
  if( err == kSCLError_EndOfIteration ) { err = kSCLError_NoErr; }

done:

  if( IsntNull(scloud) ) SCloudFree(scloud);

  return err;

}


static SCLError TestSCloud_decrypt( SCloudPacket *packet ) {

  SCLError err = kSCLError_NoErr;
  SCloudContextRef scloud = NULL;

  err = SCloudDecryptNew( packet->key, packet->keySize, sEventHandler, (void*) packet, &scloud ); CKERR;
  err = SCloudDecryptNext( scloud, packet->data, packet->dataSize ); CKERR;

done:

  if( IsntNull(scloud) ) SCloudFree(scloud);

  return err;

}

static SCLError TestSCloud() {

  SCLError err = kSCLError_NoErr;

  int i;
  int count = sizeof(banter) / sizeof(char*);
  SCloudPacket *packet;
  char json[4096];

  for( i = 0; i < count; i++ ) {

    if( banter[i] == NULL ) {
      continue;
    }

    packet = SCloudPacket_init();
    packet->extra = banter[i];

    err = TestSCloud_encrypt( (uint8_t*) banter[i], strlen(banter[i]), packet ); CKERR;
    printf( "Encrypted Data:\n" ); hprint( packet->data, 0, packet->dataSize );
    SCloudPacket_json( packet, json );
    printf( "\t%s\n", json );

    err = TestSCloud_decrypt( packet ); CKERR;

    SCloudPacket_free( packet );

  }

done:

  return err;

}

extern ltc_math_descriptor ltc_mp;

int main() {

  register_prng( &sprng_desc );

  register_hash( &sha256_desc );
  register_hash( &sha512_desc );
  register_hash( &sha512_256_desc );
  register_hash( &skein512_desc );
  register_hash( &skein256_desc );
  register_hash( &skein512_desc );

  register_cipher( &aes_desc );

  ltc_mp = ltm_desc;

  SCLError err = CRYPT_OK;
  char version_string[32];

  err = SCloudGetVersionString( sizeof(version_string), version_string ); CKERR;

  printf( "SCloud %s\n", version_string );

  err = TestSCloud(); CKERR;

done:

  if( IsSCLError( err ) ) {
    char errorBuf[256];
    if( IsntSCLError( SCLGetErrorString( err, sizeof( errorBuf ), errorBuf ) ) ) {
      printf("\nError %d:%s\n", err, errorBuf);
    } else {
      printf("\nError %d\n", err);
    }
  }

  return 0;

}
