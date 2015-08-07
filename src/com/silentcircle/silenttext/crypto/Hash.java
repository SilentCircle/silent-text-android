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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.util.StringUtils;

public class Hash {

	public static final String SHA1 = "SHA-1";

	private static MessageDigest getDigest( String algorithm ) {
		try {
			return MessageDigest.getInstance( algorithm );
		} catch( NoSuchAlgorithmException exception ) {
			throw new RuntimeException( exception );
		}
	}

	public static byte [] hash( MessageDigest digest, byte []... ins ) {
		digest.reset();
		for( int i = 0; i < ins.length; i++ ) {
			if( ins[i] != null ) {
				digest.update( ins[i] );
			}
		}
		return digest.digest();
	}

	public static char [] hash( MessageDigest digest, char []... ins ) {
		digest.reset();
		for( int i = 0; i < ins.length; i++ ) {
			char [] in = ins[i];
			byte [] inBytes = CryptoUtils.toByteArraySafe( in );
			digest.update( inBytes );
			CryptoUtils.randomize( inBytes );
		}
		byte [] outBytes = digest.digest();
		byte [] out = com.silentcircle.silentstorage.util.Base64.encodeBase64( outBytes );
		char [] hash = CryptoUtils.toCharArraySafe( out );
		CryptoUtils.randomize( out );
		CryptoUtils.randomize( outBytes );
		return hash;
	}

	public static String hash( MessageDigest digest, String... ins ) {
		digest.reset();
		for( int i = 0; i < ins.length; i++ ) {
			String in = ins[i];
			if( in == null ) {
				continue;
			}
			digest.update( in.getBytes() );
		}
		return StringUtils.pad( Base64.encodeBase64URLSafeString( digest.digest() ), 4, '=' );
	}

	public static byte [] hash( String algorithm, byte []... ins ) {
		return hash( getDigest( algorithm ), ins );
	}

	public static char [] hash( String algorithm, char []... ins ) {
		return hash( getDigest( algorithm ), ins );
	}

	public static String hash( String algorithm, String... ins ) {
		return hash( getDigest( algorithm ), ins );
	}

	public static byte [] sha1( byte []... ins ) {
		return hash( SHA1, ins );
	}

	public static char [] sha1( char []... ins ) {
		return hash( SHA1, ins );
	}

	public static String sha1( String... ins ) {
		return hash( SHA1, ins );
	}

	@Deprecated
	public static char [] sha1Legacy( char []... ins ) {
		MessageDigest digest = getDigest( SHA1 );
		digest.reset();
		for( int i = 0; i < ins.length; i++ ) {
			char [] in = ins[i];
			byte [] inBytes = CryptoUtils.toByteArray( in );
			digest.update( inBytes );
			CryptoUtils.randomize( inBytes );
		}
		byte [] outBytes = digest.digest();
		char [] out = CryptoUtils.toCharArray( outBytes );
		CryptoUtils.randomize( outBytes );
		return out;
	}

	public static byte [] toBytes( int n ) {
		return new byte [] {
			(byte) ( n >> 0x18 & 0xFF ),
			(byte) ( n >> 0x10 & 0xFF ),
			(byte) ( n >> 0x08 & 0xFF ),
			(byte) ( n >> 0x00 & 0xFF )
		};
	}

	public static byte [] toBytes( long n ) {
		return new byte [] {
			(byte) ( n >> 0x38 & 0xFF ),
			(byte) ( n >> 0x30 & 0xFF ),
			(byte) ( n >> 0x28 & 0xFF ),
			(byte) ( n >> 0x20 & 0xFF ),
			(byte) ( n >> 0x18 & 0xFF ),
			(byte) ( n >> 0x10 & 0xFF ),
			(byte) ( n >> 0x08 & 0xFF ),
			(byte) ( n >> 0x00 & 0xFF )
		};
	}

}
