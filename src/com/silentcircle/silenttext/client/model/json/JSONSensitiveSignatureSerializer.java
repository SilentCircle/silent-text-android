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

import com.silentcircle.silentstorage.io.Serializer;
import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.client.model.SensitiveSignature;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONSensitiveSignatureSerializer extends Serializer<SensitiveSignature> {

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

	@Override
	public SensitiveSignature read( DataInputStream in ) throws IOException {
		return read( in, new SensitiveSignature() );
	}

	@Override
	public SensitiveSignature read( DataInputStream in, SensitiveSignature out ) throws IOException {
		if( out == null ) {
			return out;
		}
		JSONParser parser = new JSONParser( in, new JSONSensitiveSignatureListener( out ) );
		parser.read();
		return out;
	}

	@Override
	public SensitiveSignature write( SensitiveSignature in, DataOutputStream out ) throws IOException {

		if( in == null ) {
			return in;
		}

		JSONGenerator generator = new JSONGenerator( out );

		generator.openObject();

		write( generator, "signature", Base64.encodeBase64String( in.getData() ) );
		next( generator, "start_date", JSONUtils.getDate( in.getDate() ) );
		next( generator, "signed_by", in.getSignerLocator() );
		next( generator, "hashList", in.getHashList() );

		generator.closeObject();

		return in;

	}

}
