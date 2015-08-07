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
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import com.silentcircle.silentstorage.util.IOUtils;

/**
 * Specifies a password-protected cryptographic storage key.
 */
public class StorageKeySpec {

	public static final int VERSION = 2;

	public String keyAlgorithm;
	public EphemeralKeySpec ephemeralKey;
	public byte [] encryptedKey;
	public Key key;

	/**
	 * Unlocks the encrypted key from the given parameters, then cycles its ephemeral key.
	 * 
	 * @param passcode
	 *            The passcode which unlocks the given encrypted key.
	 * @param keyAlgorithm
	 *            The storage key's algorithm.
	 * @param encryptedKey
	 *            The raw bytes of a storage key previously encrypted with the given parameters.
	 * @param ephemeralKey
	 *            The ephemeral key to use for decrypting and encrypting the storage key.
	 */
	public StorageKeySpec( char [] passcode, String keyAlgorithm, byte [] encryptedKey, EphemeralKeySpec ephemeralKey ) {
		this.keyAlgorithm = keyAlgorithm;
		this.ephemeralKey = ephemeralKey;
		this.encryptedKey = encryptedKey;
		unlock( passcode );
		cycle( passcode );
	}

	/**
	 * Generates a new storage key from the given parameters, then cycles its ephemeral key.
	 * 
	 * @param passcode
	 *            The passcode that will be used to unlock this storage key.
	 * @param keyAlgorithm
	 *            The storage key's algorithm.
	 * @param ephemeralKey
	 *            The ephemeral key to use for encrypting and decrypting the storage key.
	 * @throws NoSuchAlgorithmException
	 *             if the key algorithm is unknown.
	 */
	public StorageKeySpec( char [] passcode, String keyAlgorithm, EphemeralKeySpec ephemeralKey ) throws NoSuchAlgorithmException {
		this.ephemeralKey = ephemeralKey;
		this.keyAlgorithm = keyAlgorithm;
		key = KeyGenerator.getInstance( keyAlgorithm ).generateKey();
		cycle( passcode );
	}

	/**
	 * Deserializes a storage key from the given input stream.
	 * 
	 * @param in
	 *            a stream positioned at the beginning of data written with
	 *            {@link #write(OutputStream)}
	 * @throws IOException
	 *             if something goes wrong while deserializing the key
	 */
	public StorageKeySpec( InputStream in ) throws IOException {
		read( in );
	}

	/**
	 * Deserializes a storage key from the given input stream, then unlocks it.
	 * 
	 * @param in
	 *            a stream positioned at the beginning of data written with
	 *            {@link #write(OutputStream)}
	 * @param passcode
	 *            The passcode which unlocks the given encrypted key.
	 * @throws IOException
	 *             if something goes wrong while deserializing the key
	 */
	public StorageKeySpec( InputStream in, char [] passcode ) throws IOException {
		read( in );
		unlock( passcode );
	}

	/**
	 * Randomizes the ephemeral key parameters, then encrypts the storage key with the ephemeral key
	 * and the given passcode.
	 * 
	 * @param passcode
	 */
	public void cycle( char [] passcode ) {
		if( key == null ) {
			unlock( passcode );
			return;
		}
		ephemeralKey.consume();
		encryptedKey = ephemeralKey.encrypt( key.getEncoded(), passcode );
	}

	/**
	 * Deserializes a storage key from the given input stream.
	 * 
	 * @param in
	 *            a stream positioned at the beginning of data written with
	 *            {@link #write(OutputStream)}
	 * @throws IOException
	 *             if something goes wrong while deserializing the key
	 */
	public void read( InputStream in ) throws IOException {

		if( in == null ) {
			return;
		}

		DataInputStream meta = new DataInputStream( in );

		int version = meta.readInt();

		switch( version ) {

			case 2:

				keyAlgorithm = meta.readUTF();

				ephemeralKey = new EphemeralKeySpec( in );

				encryptedKey = new byte [meta.readInt()];
				IOUtils.fill( in, encryptedKey );

				break;

			case 1:

				keyAlgorithm = meta.readUTF();
				meta.readUTF();

				ephemeralKey = new EphemeralKeySpec( in );

				encryptedKey = new byte [meta.readInt()];
				IOUtils.fill( in, encryptedKey );

				break;

		}

	}

	/**
	 * Unlocks the encrypted key using the given passcode, then cycles the encryption parameters.
	 * 
	 * @param passcode
	 *            The code used to unlock the key.
	 * @throws IllegalArgumentException
	 *             if the given passcode is invalid for this storage key.
	 */
	public void unlock( char [] passcode ) {
		if( encryptedKey == null || passcode == null ) {
			return;
		}
		byte [] keyBytes = ephemeralKey.decrypt( encryptedKey, passcode );
		if( keyBytes == null ) {
			return;
		}
		key = new SecretKeySpec( keyBytes, keyAlgorithm );
		cycle( passcode );
	}

	/**
	 * Serializes this storage key to the given output stream.
	 * 
	 * @param out
	 *            the stream to which this key will be serialized
	 * @throws IOException
	 *             if something goes wrong while serializing this key
	 */
	public void write( OutputStream out ) throws IOException {

		if( out == null ) {
			return;
		}

		DataOutputStream meta = new DataOutputStream( out );

		meta.writeInt( VERSION );

		meta.writeUTF( keyAlgorithm );

		ephemeralKey.write( out );

		meta.writeInt( encryptedKey.length );
		out.write( encryptedKey );

	}

}
