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
package com.silentcircle.silenttext.client;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.Authenticator;
import com.silentcircle.api.model.ActivationCodeCredential;
import com.silentcircle.api.model.Credential;
import com.silentcircle.api.model.UsernamePasswordCredential;
import com.silentcircle.silenttext.client.listener.JSONAPIKeyListener;
import com.silentcircle.silenttext.listener.JSONResponseListener;

public class ActivationClient implements Authenticator {

	private static final String V1 = "v1";
	private static final String ME = "me";
	private static final String DEVICE = "device";
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String KEY_ACTIVATION_CODE = "provisioning_code";
	private static final String KEY_DEVICE_NAME = "device_name";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";

	private static void applyCredential( ActivationCodeCredential credential, JSONObject request ) throws JSONException {
		request.put( KEY_ACTIVATION_CODE, credential.getActivationCode() );
	}

	private static void applyCredential( Credential credential, JSONObject request ) throws JSONException {
		if( credential instanceof UsernamePasswordCredential ) {
			applyCredential( (UsernamePasswordCredential) credential, request );
		}
		if( credential instanceof ActivationCodeCredential ) {
			applyCredential( (ActivationCodeCredential) credential, request );
		}
	}

	private static void applyCredential( UsernamePasswordCredential credential, JSONObject request ) throws JSONException {
		request.put( KEY_USERNAME, credential.getUsername() );
		request.put( KEY_PASSWORD, credential.getPassword() );
	}

	private static HTTPContent toHTTPContent( JSONObject request ) {
		byte [] buffer = request.toString().getBytes();
		HTTPContent content = new HTTPContent( new ByteArrayInputStream( buffer ), CONTENT_TYPE_JSON, buffer.length );
		return content;
	}

	protected final SimpleHTTPClient http;

	protected CharSequence baseURL;

	/**
	 * @deprecated This constructor will not validate connections against the embedded trust store.
	 *             Consider using {@link ActivationClient#ActivationClient(KeyStore)} instead.
	 */
	@Deprecated
	public ActivationClient() {
		this( new SimpleHTTPClient() );
	}

	public ActivationClient( CharSequence baseURL ) {
		this();
		this.baseURL = baseURL;
	}

	public ActivationClient( KeyStore trustStore ) {
		this( new SimpleHTTPClient( trustStore ) );
	}

	public ActivationClient( KeyStore trustStore, CharSequence baseURL ) {
		this( trustStore );
		this.baseURL = baseURL;
	}

	public ActivationClient( SimpleHTTPClient http ) {
		this.http = http;
	}

	public ActivationClient( SimpleHTTPClient http, CharSequence baseURL ) {
		this.http = http;
		this.baseURL = baseURL;
	}

	private AuthenticatedSession activate( final CharSequence deviceID, JSONObject request ) {
		JSONAPIKeyListener response = new JSONAPIKeyListener();
		http.put( url( V1, ME, DEVICE, deviceID, "" ), toHTTPContent( request ), new JSONResponseListener( response ) );
		return new AuthenticatedClientSession( http, baseURL, response.apiKey, deviceID );
	}

	@Override
	public AuthenticatedSession authenticate( Credential credential, CharSequence deviceID, CharSequence deviceName ) {
		JSONObject request = new JSONObject();
		try {
			applyCredential( credential, request );
			request.put( KEY_DEVICE_NAME, deviceName );
		} catch( JSONException exception ) {
			throw new RuntimeException( exception );
		}
		return activate( deviceID, request );
	}

	private String url( CharSequence... components ) {
		URLBuilder url = new URLBuilder( baseURL.toString() );
		for( int i = 0; i < components.length; i++ ) {
			url.component( components[i].toString() );
		}
		return url.toString();
	}

}
