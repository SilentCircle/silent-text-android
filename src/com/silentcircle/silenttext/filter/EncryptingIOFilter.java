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
package com.silentcircle.silenttext.filter;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.crypto.CryptoUtils;

public class EncryptingIOFilter implements IOFilter<String> {

	public static final String KEY_ALGORITHM = "AES";
	private static final String DELIMITER = ";";

	private final Key key;

	public EncryptingIOFilter( Key key ) {
		this.key = key;
	}

	private byte [] decrypt( byte [] ciphertext, byte [] IV ) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
		cipher.init( Cipher.DECRYPT_MODE, key, new IvParameterSpec( IV ) );
		return cipher.doFinal( ciphertext );
	}

	private byte [] encrypt( byte [] plaintext, byte [] IV ) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
		cipher.init( Cipher.ENCRYPT_MODE, key, new IvParameterSpec( IV ) );
		return cipher.doFinal( plaintext );
	}

	@Override
	public String filterInput( String value ) {
		if( value == null ) {
			return value;
		}
		int index = value.indexOf( DELIMITER );
		if( index < 0 ) {
			throw new IllegalArgumentException();
		}
		byte [] IV = Base64.decodeBase64( value.substring( 0, index ) );
		byte [] ciphertext = Base64.decodeBase64( value.substring( index + DELIMITER.length() ) );
		try {
			return new String( decrypt( ciphertext, IV ) );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	@Override
	public String filterOutput( String value ) {
		if( value == null ) {
			return value;
		}
		byte [] IV = CryptoUtils.randomBytes( 16 );
		try {
			byte [] encrypted = encrypt( value.getBytes(), IV );
			return String.format( "%s%s%s", Base64.encodeBase64String( IV ), DELIMITER, Base64.encodeBase64String( encrypted ) );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

}
