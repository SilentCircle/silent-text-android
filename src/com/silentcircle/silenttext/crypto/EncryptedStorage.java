/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.util.Log;

import com.silentcircle.silentstorage.io.AESWithCBCAndPKCS7PaddingCipherFactory;
import com.silentcircle.silentstorage.io.HMacSHA256Factory;
import com.silentcircle.silentstorage.repository.file.SecureFileRepository;
import com.silentcircle.silentstorage.repository.helper.RepositoryHelper;
import com.silentcircle.silenttext.model.SymmetricKey;
import com.silentcircle.silenttext.util.IOUtils;

public class EncryptedStorage {

	private static final String PBKDF2_HMAC_SHA1 = "PBKDF2WithHmacSHA1";
	private static final String MASTER_KEY = "___DEFAULT___";
	private static final RepositoryHelper<SymmetricKey> HELPER = new SymmetricKey.Helper();
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String KEY_ALGORITHM = "AES";
	private static final String TAG = "EncryptedStorage";

	private static byte [] createMasterKey( File directory, char [] passcode ) {
		File file = getMasterKeyFile( directory );
		StorageKeySpec spec = createMasterKeySpec( passcode );
		writeStorageKeySpecToFile( spec, file );
		if( spec.key == null ) {
			Log.d( TAG, "create: spec.key is null" );
		}
		return spec.key.getEncoded();
	}

	private static StorageKeySpec createMasterKeySpec( char [] passcode ) {
		try {
			EphemeralKeySpec ephemeralKey = new EphemeralKeySpec( KEY_ALGORITHM, PBKDF2_HMAC_SHA1, CIPHER_ALGORITHM, new PBKDF2Calibrator( CIPHER_ALGORITHM, KEY_ALGORITHM ).calibrate( 1, 1024, 20480 ), 256, 16, 16 );
			StorageKeySpec storageKey = new StorageKeySpec( passcode, KEY_ALGORITHM, ephemeralKey );
			return storageKey;
		} catch( NoSuchAlgorithmException exception ) {
			throw new RuntimeException( exception );
		}
	}

	public static byte [] createUserKey( Context context, byte [] masterKey, String username ) {
		return username != null ? createUserKey( context.getFilesDir(), masterKey, username ) : null;
	}

	private static byte [] createUserKey( File directory, byte [] masterKey, String username ) {
		SymmetricKey userKey = createUserKey( username );
		saveUserKey( directory, masterKey, userKey );
		return userKey.key;
	}

	private static SymmetricKey createUserKey( String username ) {
		return new SymmetricKey( username, CryptoUtils.randomBytes( 32 ) );
	}

	private static File getMasterKeyFile( File directory ) {
		return new File( directory, Hash.sha1( MASTER_KEY ) + ".key" );
	}

	public static byte [] loadMasterKey( Context context, char [] passcode ) {
		return loadMasterKey( context.getFilesDir(), passcode );
	}

	private static byte [] loadMasterKey( File directory, char [] passcode ) {

		StorageKeySpec spec = loadMasterKeySpec( directory );

		if( spec != null ) {
			spec.unlock( passcode );
			if( spec.key != null ) {
				return spec.key.getEncoded();
			}
			Log.d( TAG, "load: spec.key is null" );
			return null;
		}

		return createMasterKey( directory, passcode );

	}

	private static StorageKeySpec loadMasterKeySpec( File directory ) {

		File file = getMasterKeyFile( directory );

		if( file.exists() && file.length() > 0 ) {
			InputStream in = null;
			try {
				in = new FileInputStream( file );
				return new StorageKeySpec( in );
			} catch( IOException exception ) {
				throw new RuntimeException( exception );
			} finally {
				IOUtils.close( in );
			}
		}

		return null;

	}

	public static byte [] loadUserKey( Context context, byte [] masterKey, String username ) {
		return username != null ? loadUserKey( context.getFilesDir(), masterKey, username ) : null;
	}

	private static byte [] loadUserKey( File directory, byte [] masterKey, String username ) {
		try {
			SecureFileRepository<SymmetricKey> repository = repository( directory, masterKey );
			SymmetricKey key = repository.findByID( username.toCharArray() );
			if( key == null || key.key == null ) {
				throw new NullPointerException();
			}
			return key.key;
		} catch( NullPointerException exception ) {
			return createUserKey( directory, masterKey, username );
		}
	}

	private static SecureFileRepository<SymmetricKey> repository( File directory ) {
		return new SecureFileRepository<SymmetricKey>( new File( directory, Hash.sha1( "storage_keys" ) ), HELPER, new AESWithCBCAndPKCS7PaddingCipherFactory(), new HMacSHA256Factory() );
	}

	private static SecureFileRepository<SymmetricKey> repository( File directory, byte [] masterKey ) {
		SecureFileRepository<SymmetricKey> repository = repository( directory );
		repository.unlock( masterKey );
		return repository;
	}

	public static void saveMasterKey( Context context, char [] passcode, byte [] masterKey ) {
		saveMasterKey( context.getFilesDir(), passcode, masterKey );
	}

	private static void saveMasterKey( File directory, char [] passcode, byte [] masterKey ) {

		StorageKeySpec spec = loadMasterKeySpec( directory );

		if( spec == null ) {
			spec = createMasterKeySpec( passcode );
		}

		spec.key = new SecretKeySpec( masterKey, spec.keyAlgorithm );
		spec.cycle( passcode );

		writeStorageKeySpecToFile( spec, getMasterKeyFile( directory ) );

	}

	public static void saveUserKey( Context context, byte [] masterKey, String username, byte [] userKey ) {
		saveUserKey( context.getFilesDir(), masterKey, username, userKey );
	}

	private static void saveUserKey( File directory, byte [] masterKey, String username, byte [] userKey ) {
		saveUserKey( directory, masterKey, new SymmetricKey( username, userKey ) );
	}

	private static void saveUserKey( File directory, byte [] masterKey, SymmetricKey key ) {
		repository( directory, masterKey ).save( key );
	}

	private static void writeStorageKeySpecToFile( StorageKeySpec storageKey, File file ) {
		OutputStream out = null;
		try {
			out = new FileOutputStream( file, false );
			storageKey.write( out );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		} finally {
			IOUtils.close( out );
		}
	}

}
