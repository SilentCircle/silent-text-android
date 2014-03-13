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

import java.nio.CharBuffer;

import org.twuni.twoson.JSONEventListener;

import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.client.model.SensitiveSignature;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONSensitiveSignatureListener implements JSONEventListener {

	private static byte [] decodeBytes( char [] value ) {
		return Base64.decodeBase64( CryptoUtils.toByteArray( value ) );
	}

	private static CharBuffer export( char [] value ) {
		return CharBuffer.wrap( CryptoUtils.copyOf( value ) );
	}

	private final Log log = new Log( getClass().getSimpleName() );
	private final SensitiveSignature out;
	private JSONSensitiveSignatureKey key;

	public JSONSensitiveSignatureListener( SensitiveSignature out ) {
		this.out = out;
	}

	@Override
	public void onBeginArray() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onBeginObject() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onBoolean( boolean value ) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onEndArray() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndObject() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFloat( float value ) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInteger( int value ) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNull() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onObjectKey( char [] value ) {
		key = JSONSensitiveSignatureKey.valueOf( value );
	}

	@Override
	public void onString( char [] value ) {
		if( key == null ) {
			log.warn( "#onString ignoring value:%s", new String( value ) );
			return;
		}
		switch( key ) {
			case SIGNATURE:
				out.setData( decodeBytes( value ) );
				break;
			case SIGNED_BY:
				out.setSignerLocator( export( value ) );
				break;
			case START_DATE:
				out.setDate( JSONUtils.getDate( value ) );
				break;
			case HASH_LIST:
				out.setHashList( export( value ) );
				break;
			default:
				break;
		}
	}

}
