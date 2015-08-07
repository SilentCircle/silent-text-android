/*
Copyright (C) 2013-2015, Silent Circle, LLC. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.silentcircle.silenttext.crypto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.silentcircle.silentstorage.util.IOUtils;
import com.silentcircle.silenttext.log.Log;

/**
 * Specifies a password-protected, cycling ephemeral key. These are the parameters for encrypting
 * and decrypting a {@link StorageKeySpec}.
 */
public class EphemeralKeySpec {

	public static final int VERSION = 1;
	public static final Random RANDOM = new SecureRandom();

	public String keyAlgorithm;
	public String intermediateKeyAlgorithm;
	public String cipherAlgorithm;
	public int iterationCount;
	public int keyLength;
	public byte [] salt;
	public byte [] iv;

	private static final Log LOG = new Log( "EphemeralKeySpec" );

	private static final String TAG = "EphemeralKeySpec";

	/**
	 * Creates an empty ephemeral key with no parameters set.
	 */
	public EphemeralKeySpec() {
	}

	/**
	 * Deserializes an ephemeral key from the given input stream.
	 * 
	 * @param in
	 *            a stream positioned at the beginning of data written with
	 *            {@link #write(OutputStream)}
	 * @throws IOException
	 *             if something goes wrong while deserializing the key
	 */
	public EphemeralKeySpec( InputStream in ) throws IOException {
		read( in );
	}

	/**
	 * Creates a new ephemeral key with the given parameters.
	 * 
	 * @param keyAlgorithm
	 *            The algorithm to use for this key.
	 * @param intermediateKeyAlgorithm
	 *            The algorithm to use for the intermediate key derived from a given passcode.
	 * @param cipherAlgorithm
	 *            The encryption algorithm to use for this key.
	 * @param iterationCount
	 *            The number of PBKDF iterations to run.
	 * @param keyLength
	 *            The size of this key.
	 * @param salt
	 *            The salt to use for the PBKDF.
	 * @param iv
	 *            The initialization vector to use for encryption and decryption.
	 */
	public EphemeralKeySpec( String keyAlgorithm, String intermediateKeyAlgorithm, String cipherAlgorithm, int iterationCount, int keyLength, byte [] salt, byte [] iv ) {
		this.keyAlgorithm = keyAlgorithm;
		this.intermediateKeyAlgorithm = intermediateKeyAlgorithm;
		this.cipherAlgorithm = cipherAlgorithm;
		this.iterationCount = iterationCount;
		this.keyLength = keyLength;
		this.salt = salt;
		this.iv = iv;
	}

	/**
	 * Creates a new ephemeral key with the given parameters.
	 * 
	 * @param keyAlgorithm
	 *            The algorithm to use for this key.
	 * @param intermediateKeyAlgorithm
	 *            The algorithm to use for the intermediate key derived from a given passcode.
	 * @param cipherAlgorithm
	 *            The encryption algorithm to use for this key.
	 * @param iterationCount
	 *            The number of PBKDF iterations to run.
	 * @param keyLength
	 *            The size of this key, in bits.
	 * @param saltLength
	 *            The length of the salt, in bytes, to generate and use for the PBKDF.
	 * @param ivLength
	 *            The length of the initialization vector, in bytes, to generate and use for
	 *            encryption and decryption.
	 */
	public EphemeralKeySpec( String keyAlgorithm, String intermediateKeyAlgorithm, String cipherAlgorithm, int iterationCount, int keyLength, int saltLength, int ivLength ) {
		this( keyAlgorithm, intermediateKeyAlgorithm, cipherAlgorithm, iterationCount, keyLength, new byte [saltLength], new byte [ivLength] );
		consume();
	}

	/**
	 * Fills the initialization vector (IV) and salt with random bytes. This is equivalent to
	 * calling {@code randomize( new SecureRandom() )}.
	 */
	public void consume() {
		randomize( RANDOM );
	}

	/**
	 * Decrypts the data with the given passcode. If successful, {@link #consume()} is called
	 * afterwards.
	 * 
	 * @param data
	 *            The encrypted input data.
	 * @param passcode
	 *            The passcode to use for decrypting the data.
	 * @return The decrypted output data, or {@code null} if something went wrong.
	 */
	public byte [] decrypt( byte [] data, char [] passcode ) {
		byte [] result = transform( Cipher.DECRYPT_MODE, data, passcode );
		if( result != null ) {
			consume();
		}
		return result;
	}

	/**
	 * Encrypts the {@code data} with the given {@code passcode}.
	 * 
	 * @param data
	 *            The data to encrypt.
	 * @param passcode
	 *            The passcode to use for encryption.
	 * @return The encrypted data.
	 */
	public byte [] encrypt( byte [] data, char [] passcode ) {
		return transform( Cipher.ENCRYPT_MODE, data, passcode );
	}

	public Key getKey( char [] passcode ) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance( intermediateKeyAlgorithm );
			Key key = factory.generateSecret( new PBEKeySpec( passcode, salt, iterationCount, keyLength ) );
			if( key == null ) {
				Log.d( TAG, "key is null" );
				return null;
			}
			return new SecretKeySpec( key.getEncoded(), keyAlgorithm );
		} catch( Exception exception ) {
			Log.e( TAG, exception.getMessage() );
			LOG.error( exception, "#getKey" );
			return null;
		}
	}

	/**
	 * Fills the initialization vector (IV) and salt with random bytes using the given pseudo-random
	 * number generator.
	 * 
	 * @param random
	 *            the pseudo-random number generator to use.
	 */
	public void randomize( Random random ) {
		random.nextBytes( iv );
		random.nextBytes( salt );
	}

	public void read( InputStream in ) throws IOException {

		if( in == null ) {
			return;
		}

		DataInputStream meta = new DataInputStream( in );

		int version = meta.readInt();

		switch( version ) {

			case 1:

				keyAlgorithm = meta.readUTF();
				intermediateKeyAlgorithm = meta.readUTF();
				cipherAlgorithm = meta.readUTF();
				iterationCount = meta.readInt();
				keyLength = meta.readInt();

				salt = new byte [meta.readInt()];
				IOUtils.fill( in, salt );

				iv = new byte [meta.readInt()];
				IOUtils.fill( in, iv );

				break;

		}

	}

	private byte [] transform( int cipherMode, byte [] data, char [] passcode ) {
		try {
			Cipher cipher = Cipher.getInstance( cipherAlgorithm );
			cipher.init( cipherMode, getKey( passcode ), new IvParameterSpec( iv ) );
			return cipher.doFinal( data );
		} catch( Exception exception ) {
			LOG.error( exception, "#transform mode:%d", Integer.valueOf( cipherMode ) );
			return null;
		}
	}

	public void write( OutputStream out ) throws IOException {

		if( out == null ) {
			return;
		}

		DataOutputStream meta = new DataOutputStream( out );

		meta.writeInt( VERSION );

		meta.writeUTF( keyAlgorithm );
		meta.writeUTF( intermediateKeyAlgorithm );
		meta.writeUTF( cipherAlgorithm );
		meta.writeInt( iterationCount );
		meta.writeInt( keyLength );

		meta.writeInt( salt.length );
		out.write( salt );

		meta.writeInt( iv.length );
		out.write( iv );

	}

}
