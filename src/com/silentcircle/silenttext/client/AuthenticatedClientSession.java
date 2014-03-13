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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.aspect.util.Sensitivity;
import com.silentcircle.api.aspect.util.Statefulness;
import com.silentcircle.api.model.Credential;
import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.User;
import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.client.listener.JSONCredentialListener;
import com.silentcircle.silenttext.client.listener.JSONUserListener;
import com.silentcircle.silenttext.client.model.SensitiveKey;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.client.model.json.JSONSensitiveKeySerializer;
import com.silentcircle.silenttext.listener.HTTPResponseListener;
import com.silentcircle.silenttext.listener.JSONObjectListener;
import com.silentcircle.silenttext.listener.JSONResponseListener;
import com.silentcircle.silenttext.log.Log;

public class AuthenticatedClientSession extends AuthenticatedSession {

	static class Future<T> {

		public T result;

		public Future( T defaultValue ) {
			result = defaultValue;
		}

	}

	private static final String KEY_PUSH_TOKEN = "device_token";
	private static final String KEY_PUSH_SERVICE = "service";
	private static final String KEY_API_KEY = "api_key";

	public static final int VERSION = 1;
	private static final String V1 = "v1";
	private static final String ME = "me";
	private static final String PUBLIC_KEY = "pubkey";
	private static final String DEVICE = "device";
	private static final String USER = "user";
	private static final String PUSH_TOKEN = "pushtoken";

	private static final String CONTENT_TYPE_JSON = "application/json";
	private final Log log = new Log( getClass().getSimpleName() );

	private final SimpleHTTPClient http;

	protected CharSequence baseURL;

	private static final JSONSensitiveKeySerializer JSON_KEY_SERIALIZER = new JSONSensitiveKeySerializer();

	public AuthenticatedClientSession( SimpleHTTPClient http ) {
		this.http = http;
	}

	public AuthenticatedClientSession( SimpleHTTPClient http, CharSequence baseURL, CharSequence accessToken, CharSequence deviceID ) {
		this.http = http;
		this.baseURL = baseURL;
		this.accessToken = accessToken;
		this.deviceID = deviceID;
	}

	@Override
	public void burn() {
		super.burn();
		Sensitivity.burn( baseURL );
		baseURL = null;
	}

	@Override
	public Credential getCredential( CharSequence applicationID ) {
		JSONCredentialListener response = new JSONCredentialListener( applicationID );
		http.get( url( V1, ME, DEVICE, deviceID, "" ), new JSONResponseListener( response ) );
		return response.credential;
	}

	protected Log getLog() {
		return log;
	}

	public User getSelf() {
		JSONUserListener response = new JSONUserListener();
		try {
			http.get( url( V1, ME, "" ), new JSONResponseListener( response ) );
		} catch( Throwable exception ) {
			log.error( exception, "#getSelf" );
		}
		return response.user;
	}

	@Override
	public User getUser( CharSequence userID ) {
		JSONUserListener response = new JSONUserListener();
		try {
			http.get( url( V1, USER, userID, "" ), new JSONResponseListener( response ) );
		} catch( Throwable exception ) {
			log.error( exception, "#getUser" );
		}
		return response.user;
	}

	public boolean isValid( final Repository<ServiceEndpoint> networks ) {

		final Future<Boolean> valid = new Future<Boolean>( Boolean.FALSE );

		http.get( url( V1, ME, "" ), new HTTPResponseListener() {

			@Override
			public void onResponse( HttpResponse response ) {
				int status = response.getStatusLine().getStatusCode();
				boolean ok = 200 <= status && status < 400;
				if( !ok ) {
					ServiceConfiguration.getInstance().api.removeFromCache( networks );
				}
				valid.result = Boolean.valueOf( ok );
			}

		} );

		return valid.result.booleanValue();

	}

	@Override
	public void load( DataInputStream in ) throws IOException {
		super.load( in );
		int version = in.readInt();
		switch( version ) {
			case 1:
				baseURL = Statefulness.readCharSequence( in );
				break;
		}
	}

	@Override
	public void logout() {
		http.delete( url( V1, ME, DEVICE, deviceID, "" ) );
	}

	@Override
	public void registerPushNotifications( CharSequence applicationID, CharSequence service, CharSequence token ) {
		JSONObject request = new JSONObject();
		try {
			request.put( KEY_PUSH_SERVICE, service );
			request.put( KEY_PUSH_TOKEN, token );
		} catch( JSONException exception ) {
			throw new RuntimeException( exception );
		}
		http.put( url( V1, ME, DEVICE, deviceID, PUSH_TOKEN, applicationID, "" ), CONTENT_TYPE_JSON, request.toString().getBytes() );
	}

	@Override
	public void removeKey( CharSequence locator ) {
		http.delete( url( V1, PUBLIC_KEY, locator, "" ) );
	}

	@Override
	public void save( DataOutputStream out ) throws IOException {
		super.save( out );
		out.writeInt( VERSION );
		Statefulness.writeCharSequence( baseURL, out );
	}

	@Override
	public void unregisterPushNotifications( CharSequence applicationID ) {
		http.delete( url( V1, ME, DEVICE, deviceID, PUSH_TOKEN, applicationID, "" ) );
	}

	@Override
	public void uploadKey( Key key ) {
		ByteArrayOutputStream json = new ByteArrayOutputStream();
		try {
			JSON_KEY_SERIALIZER.write( (SensitiveKey) key, new DataOutputStream( json ) );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}
		byte [] payload = json.toByteArray();
		getLog().debug( "#uploadKey payload:%s", new String( payload ) );
		http.put( url( V1, PUBLIC_KEY, key.getLocator(), "" ), new HTTPContent( new ByteArrayInputStream( payload ), CONTENT_TYPE_JSON, payload.length ), new JSONResponseListener( new JSONObjectListener() {

			@Override
			public void onObjectReceived( JSONObject json ) {
				getLog().debug( "#uploadKey -> #onObjectReceived json:%s", json.toString() );
			}

		} ) );
	}

	private String url( CharSequence... components ) {
		URLBuilder url = new URLBuilder( baseURL.toString() );
		for( int i = 0; i < components.length; i++ ) {
			CharSequence component = components[i];
			url.component( component.toString() );
		}
		url.query( KEY_API_KEY, accessToken );
		return url.toString();
	}

}
