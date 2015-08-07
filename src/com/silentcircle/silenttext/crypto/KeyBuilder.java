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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.Key;

/**
 * Builds a cryptographic key, optionally backed by a keystore file.
 */
public class KeyBuilder {

	public static final String DEFAULT_ALIAS = "default";

	private String type;
	private File file;
	private String alias;

	public KeyBuilder() {
		clear();
	}

	/**
	 * The alias for the key within the backing keystore.
	 */
	public KeyBuilder alias( String alias ) {
		this.alias = alias;
		return this;
	}

	/**
	 * Loads the keystore (from a file, if {@link #file(File)} was called) and retrieves the key
	 * from it with the alias specified in {@link #alias(String)}. If no key was found, this will
	 * create a new one via {@link Keychain#createKey(char[])}, add this key to the keystore under
	 * the specified alias, and save the keystore to the file.
	 * 
	 * @param password
	 *            The password to use for 1) accessing the keystore, 2) accessing the key alias, and
	 *            3) creating a new key.
	 * @return A valid {@link Key} object.
	 * @throws RuntimeException
	 *             if any unrecoverable errors occurred during this process.
	 */
	public Key build( char [] password ) {
		try {
			return buildSanely( password );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	private Key buildSanely( char [] password ) throws Exception {

		Keychain keychain = new Keychain( type );

		if( file == null ) {
			keychain.load( null, password );
		} else {
			try {
				keychain.load( new FileInputStream( file ), password );
			} catch( FileNotFoundException exception ) {
				keychain.load( null, password );
			}
		}

		if( keychain.contains( alias ) ) {
			return keychain.get( alias, password );
		}

		Key key = Keychain.createKey( password );

		keychain.put( alias, key, password );

		if( file != null ) {
			keychain.save( new FileOutputStream( file, false ), password );
		}

		return key;

	}

	/**
	 * Initializes this builder to its default values. Its type is set to
	 * {@link KeyBuilder#DEFAULT_TYPE}, alias set to {@link KeyBuilder#DEFAULT_ALIAS}, and file set
	 * to {@code null}.
	 * 
	 * @return
	 */
	public KeyBuilder clear() {
		return type( Keychain.DEFAULT_TYPE ).file( null ).alias( DEFAULT_ALIAS );
	}

	public KeyBuilder file( File file ) {
		this.file = file;
		return this;
	}

	public KeyBuilder type( String type ) {
		this.type = type;
		return this;
	}

}
