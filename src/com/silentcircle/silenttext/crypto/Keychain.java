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

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

import com.silentcircle.silenttext.util.IOUtils;

/**
 * The Keychain is an API for managing cryptographic keys. It's actually just a thin abstraction
 * layer on top of Java's {@link KeyStore} object.
 */
public class Keychain {

	/**
	 * The default type of the underlying keystore.
	 */
	public static final String DEFAULT_TYPE = "pkcs12";

	private static final Certificate [] EMPTY_CERTIFICATE_CHAIN = new Certificate [0];

	/**
	 * @deprecated FIXME: Migrate this to a calibration function instead of hard-coding a value
	 *             here.
	 */
	@Deprecated
	public static final int KEY_ITERATIONS = 1024;

	public static final int KEY_LENGTH = 256;
	public static final String KEY_WRAPPER_ALGORITHM = "AES";
	public static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final String TAG = "Keychain";

	/**
	 * Creates an encryption key with the given password.
	 * 
	 * @param password
	 *            the password to use to generate the key.
	 * @return a key that can be used to encrypt/decrypt data.
	 * @throws NoSuchAlgorithmException
	 *             if {@link Keychain#KEY_ALGORITHM} is not a supported {@link SecretKeyFactory}
	 *             algorithm.
	 * @throws InvalidKeySpecException
	 *             if the {@link PBEKeySpec} created in this method is not valid.
	 */
	public static Key createKey( char [] password ) throws NoSuchAlgorithmException, InvalidKeySpecException {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance( KEY_ALGORITHM );
			Key key = factory.generateSecret( new PBEKeySpec( password, salt( 8 ), KEY_ITERATIONS, KEY_LENGTH ) );
			return new SecretKeySpec( key.getEncoded(), KEY_WRAPPER_ALGORITHM );
		} catch( Exception e ) {
			Log.e( TAG, e.getMessage() );
			return null;
		}
	}

	/**
	 * Generates random bytes.
	 * 
	 * @param size
	 *            the size of the generated byte array
	 * @return a byte array filled with random bytes equal to the given {@code size}.
	 */
	public static byte [] salt( int size ) {
		byte [] salt = new byte [size];
		new SecureRandom().nextBytes( salt );
		return salt;
	}

	private final KeyStore keystore;

	/**
	 * Creates a new Keychain backed by a keystore of {@link Keychain#DEFAULT_TYPE}.
	 * 
	 * @throws KeyStoreException
	 *             if {@link Keychain#DEFAULT_TYPE} is not a supported keystore type.
	 */
	public Keychain() throws KeyStoreException {
		this( DEFAULT_TYPE );
	}

	/**
	 * Creates a new Keychain backed by the given KeyStore.
	 * 
	 * @param keystore
	 *            the underlying keystore to use for this Keychain.
	 */
	public Keychain( KeyStore keystore ) {
		this.keystore = keystore;
	}

	/**
	 * Creates a new Keychain backed by a keystore of type {@code keyStoreType}.
	 * 
	 * @param keyStoreType
	 *            The keystore type, as defined by the <a href=
	 *            "http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#KeyStore"
	 *            >Standard Algorithm Names</a> document and/or any keystore types provided by the
	 *            runtime environment.
	 * @throws KeyStoreException
	 *             if {@code keyStoreType} is not a supported keystore type.
	 */
	public Keychain( String keyStoreType ) throws KeyStoreException {
		this( KeyStore.getInstance( keyStoreType ) );
	}

	public boolean contains( String alias ) {
		try {
			return keystore.containsAlias( alias );
		} catch( Exception exception ) {
			return false;
		}
	}

	public Key get( String alias, char [] password ) {
		try {
			return keystore.getKey( alias, password );
		} catch( Exception exception ) {
			return null;
		}
	}

	public void load( InputStream in, char [] password ) {
		try {
			keystore.load( in, password );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		} finally {
			IOUtils.close( in );
		}
	}

	public void put( String alias, Key key, char [] password ) {
		try {
			keystore.setKeyEntry( alias, key, password, EMPTY_CERTIFICATE_CHAIN );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	public void save( OutputStream out, char [] password ) {
		try {
			keystore.store( out, password );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		} finally {
			IOUtils.close( out );
		}
	}

}
