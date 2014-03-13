/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

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
package com.silentcircle.silenttext.client.model.json;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.twuni.twoson.JSONGenerator;
import org.twuni.twoson.JSONParser;

import com.silentcircle.api.model.Signature;
import com.silentcircle.silentstorage.io.Serializer;
import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.client.model.SensitiveKey;
import com.silentcircle.silenttext.client.model.SensitiveSignature;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONSensitiveKeySerializer extends Serializer<SensitiveKey> {

	private static final JSONSensitiveSignatureSerializer SIGNATURE = new JSONSensitiveSignatureSerializer();

	private static void next( JSONGenerator generator, String key, CharSequence value ) throws IOException {
		if( value != null ) {
			generator.next();
			write( generator, key, value );
		}
	}

	private static void write( JSONGenerator generator, String key, CharSequence value ) throws IOException {
		if( value != null ) {
			generator.writeKey( key );
			generator.writeCharArray( CryptoUtils.copyAsCharArray( value ) );
		}
	}

	private static void write( JSONGenerator generator, String key, int value ) throws IOException {
		generator.writeKey( key );
		generator.write( value );
	}

	@Override
	public SensitiveKey read( DataInputStream in ) throws IOException {
		return read( in, new SensitiveKey() );
	}

	@Override
	public SensitiveKey read( DataInputStream in, SensitiveKey out ) throws IOException {
		if( out == null ) {
			return out;
		}
		JSONParser parser = new JSONParser( in, new JSONSensitiveKeyListener( out ) );
		parser.read();
		return out;
	}

	@Override
	public SensitiveKey write( SensitiveKey in, DataOutputStream out ) throws IOException {

		if( in == null ) {
			return in;
		}

		JSONGenerator generator = new JSONGenerator( out );

		generator.openObject();

		write( generator, "version", in.getVersion() );

		next( generator, "locator", in.getLocator() );
		next( generator, "owner", in.getOwner() );
		next( generator, "keySuite", in.getSuite() );
		next( generator, "comment", in.getComment() );
		next( generator, "start_date", JSONUtils.getDate( in.getCreationDate() ) );
		next( generator, "expire_date", JSONUtils.getDate( in.getExpirationDate() ) );

		if( in.getPublicKey() != null ) {
			next( generator, "pubKey", Base64.encodeBase64String( in.getPublicKey() ) );
		}

		if( in.getPrivateKey() != null ) {
			next( generator, "privKey", Base64.encodeBase64String( in.getPrivateKey() ) );
		}

		if( in.getSignatures() != null && !in.getSignatures().isEmpty() ) {

			generator.next();
			generator.writeKey( "signatures" );
			generator.openArray();

			for( int i = 0; i < in.getSignatures().size(); i++ ) {

				Signature signature = in.getSignatures().get( i );

				if( i > 0 ) {
					generator.next();
				}

				SIGNATURE.write( (SensitiveSignature) signature, out );

			}

			generator.closeArray();

		}

		generator.closeObject();

		return in;

	}

}
