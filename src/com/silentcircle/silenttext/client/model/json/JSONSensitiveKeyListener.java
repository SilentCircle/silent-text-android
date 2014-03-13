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
import java.util.ArrayList;

import org.twuni.twoson.JSONEventListener;

import com.silentcircle.api.model.Signature;
import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.client.model.SensitiveKey;
import com.silentcircle.silenttext.client.model.SensitiveSignature;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.JSONUtils;

public class JSONSensitiveKeyListener implements JSONEventListener {

	private static byte [] decodeBytes( char [] value ) {
		return Base64.decodeBase64( CryptoUtils.toByteArray( value ) );
	}

	private static CharBuffer export( char [] value ) {
		return CharBuffer.wrap( CryptoUtils.copyOf( value ) );
	}

	private final Log log = new Log( getClass().getSimpleName() );
	private final SensitiveKey out;
	private JSONSensitiveKeyKey key;
	private JSONEventListener delegate;
	private SensitiveSignature signature;

	public JSONSensitiveKeyListener( SensitiveKey out ) {
		this.out = out;
	}

	@Override
	public void onBeginArray() {

		if( delegate != null ) {
			delegate.onBeginArray();
			return;
		}

	}

	@Override
	public void onBeginObject() {

		if( delegate != null ) {
			delegate.onBeginObject();
			return;
		}

		if( JSONSensitiveKeyKey.SIGNATURES.equals( key ) ) {
			signature = new SensitiveSignature();
			delegate = new JSONSensitiveSignatureListener( signature );
		}

	}

	@Override
	public void onBoolean( boolean value ) {
		if( delegate != null ) {
			delegate.onBoolean( value );
			return;
		}
	}

	@Override
	public void onEndArray() {
		if( delegate != null ) {
			delegate.onEndArray();
			return;
		}
		if( JSONSensitiveKeyKey.SIGNATURES.equals( key ) ) {
			key = null;
		}
	}

	@Override
	public void onEndObject() {
		if( JSONSensitiveKeyKey.SIGNATURES.equals( key ) ) {
			if( signature != null ) {
				if( out.getSignatures() == null ) {
					out.setSignatures( new ArrayList<Signature>() );
				}
				out.getSignatures().add( signature );
				signature = null;
				delegate = null;
			}
		}
	}

	@Override
	public void onFloat( float value ) {
		if( delegate != null ) {
			delegate.onFloat( value );
			return;
		}
	}

	@Override
	public void onInteger( int value ) {
		if( delegate != null ) {
			delegate.onInteger( value );
			return;
		}
		switch( key ) {
			case VERSION:
				out.setVersion( value );
				break;
			default:
				break;
		}
	}

	@Override
	public void onNull() {
		if( delegate != null ) {
			delegate.onNull();
			return;
		}
	}

	@Override
	public void onObjectKey( char [] value ) {

		if( delegate != null ) {
			delegate.onObjectKey( value );
			return;
		}

		key = JSONSensitiveKeyKey.valueOf( value );

	}

	@Override
	public void onString( char [] value ) {

		if( delegate != null ) {
			delegate.onString( value );
			return;
		}

		if( key == null ) {
			log.warn( "#onString ignoring value:%s", new String( value ) );
			return;
		}

		switch( key ) {
			case COMMENT:
				out.setComment( export( value ) );
				break;
			case KEY_SUITE:
				out.setSuite( export( value ) );
				break;
			case LOCATOR:
				out.setLocator( export( value ) );
				break;
			case OWNER:
				out.setOwner( export( value ) );
				break;
			case START_DATE:
				out.setCreationDate( JSONUtils.getDate( value ) );
				break;
			case EXPIRE_DATE:
				out.setExpirationDate( JSONUtils.getDate( value ) );
				break;
			case PUBLIC_KEY:
				out.setPublicKey( decodeBytes( value ) );
				break;
			case PRIVATE_KEY:
				out.setPrivateKey( decodeBytes( value ) );
				break;
			default:
				break;
		}

	}

}
