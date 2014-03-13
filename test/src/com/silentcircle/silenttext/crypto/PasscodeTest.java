/*
Copyright © 2013, Silent Circle, LLC.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the names of its contributors may 
      be used to endorse or promote products derived from this software 
      without specific prior written permission.

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Test;

import com.silentcircle.silenttext.Assert;

public class PasscodeTest extends Assert {

	private static byte [] randomBytes( int n ) {
		byte [] buffer = new byte [n];
		SecureRandom random = new SecureRandom();
		random.nextBytes( buffer );
		return buffer;
	}

	@Test
	public void something_shouldBeAwesome() throws NoSuchAlgorithmException, IOException {

		byte [] iv = randomBytes( 16 );
		char [] passcode = "loki".toCharArray();
		byte [] plaintext = "[This is the best test in the known universe.]".getBytes();

		EphemeralKeySpec ephemeralKey = new EphemeralKeySpec( "AES", "PBKDF2WithHmacSHA1", "AES/CBC/PKCS5Padding", 50000, 256, 16, 16 );
		StorageKeySpec storageKey = new StorageKeySpec( passcode, "AES", "AES/CBC/PKCS5Padding", ephemeralKey );

		byte [] ciphertext = storageKey.encrypt( iv, plaintext );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		storageKey.write( out );
		ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

		storageKey = new StorageKeySpec( in, passcode );

		String result = new String( storageKey.decrypt( iv, ciphertext ) );
		System.out.println( result );

	}

}
