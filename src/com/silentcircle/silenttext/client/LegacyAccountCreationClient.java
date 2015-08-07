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
package com.silentcircle.silenttext.client;

import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.security.KeyStore;

import org.json.JSONException;
import org.json.JSONObject;
import org.twuni.twoson.JSONValue;

import android.content.Context;

import com.silentcircle.api.Authenticator;
import com.silentcircle.api.UserManager;
import com.silentcircle.core.util.JSONUtils;
import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.AbstractHTTPClient;
import com.silentcircle.http.client.HTTPContent;
import com.silentcircle.http.client.HTTPResponse;
import com.silentcircle.http.client.URLBuilder;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.http.client.exception.EmptyResponseException;
import com.silentcircle.http.client.listener.HTTPResponseListener;
import com.silentcircle.http.client.listener.JSONResponseListener;
import com.silentcircle.http.client.listener.JSONValueListener;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.DeviceUtils;

/**
 * @deprecated The server API supporting this client has been sunsetted and will no longer receive
 *             support. Please instead use {@link UserManager} to create an account and
 *             {@link Authenticator} to authorize a device.
 * @see UserManager
 * @see Authenticator
 */
@Deprecated
public class LegacyAccountCreationClient {

	protected static class Result {

		public boolean eligible;
		public CharSequence activationCode;

	}

	private static final String KEY_EMAIL = "email";
	private static final String KEY_LAST_NAME = "last_name";
	private static final String KEY_FIRST_NAME = "first_name";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_SERIAL_NUMBER = "psn";

	private static final String PROVISIONING = "provisioning";
	private static final String IMEI = "imei";
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String SALT_FOR_DEVICE_ID = "SilentCircle";

	protected static final Log LOG = new Log( LegacyAccountCreationClient.class.getSimpleName() );
	protected static final String KEY_PROVISIONING_CODE = "provisioning_code";
	protected static final String KEY_ERROR_MESSAGE = "error_msg";

	private static HTTPContent toHTTPContent( JSONObject request ) {
		byte [] buffer = request.toString().getBytes();
		HTTPContent content = new HTTPContent( new ByteArrayInputStream( buffer ), CONTENT_TYPE_JSON, buffer.length );
		return content;
	}

	private final AbstractHTTPClient http;
	private final SoftReference<Context> contextReference;
	private CharSequence baseURL;
	private final CharSequence deviceID;
	private final CharSequence deviceSerial;

	/**
	 * @deprecated This constructor will not validate connections against the embedded trust store.
	 *             Consider using
	 *             {@link LegacyAccountCreationClient#AccountCreationClient(Context,KeyStore)}
	 *             instead.
	 */
	@Deprecated
	public LegacyAccountCreationClient( Context context ) {
		this( context, new ApacheHTTPClient( new HttpClient() ) );
	}

	public LegacyAccountCreationClient( Context context, AbstractHTTPClient http ) {
		this( context, http, null );
	}

	public LegacyAccountCreationClient( Context context, AbstractHTTPClient http, CharSequence baseURL ) {
		this( context, http, baseURL, DeviceUtils.getHashedDeviceID( context, SALT_FOR_DEVICE_ID ), DeviceUtils.getSerialNumber() );
	}

	public LegacyAccountCreationClient( Context context, AbstractHTTPClient http, CharSequence baseURL, CharSequence deviceID, CharSequence deviceSerial ) {
		contextReference = new SoftReference<Context>( context );
		this.http = http;
		this.baseURL = baseURL;
		this.deviceID = deviceID;
		this.deviceSerial = deviceSerial;
	}

	public LegacyAccountCreationClient( Context context, CharSequence baseURL ) {
		this( context );
		this.baseURL = baseURL;
	}

	public LegacyAccountCreationClient( Context context, KeyStore trustStore ) {
		this( context, new ApacheHTTPClient( new HttpClient( trustStore ) ) );
	}

	public LegacyAccountCreationClient( Context context, KeyStore trustStore, CharSequence baseURL ) {
		this( context, trustStore );
		this.baseURL = baseURL;
	}

	public CharSequence createAccount( CharSequence username, CharSequence password, CharSequence emailAddress, CharSequence firstName, CharSequence lastName ) {

		JSONObject parameters = getAccountCreationRequestJSON( username, password, emailAddress, firstName, lastName );
		final Result result = new Result();

		http.post( getAccountCreationURL(), toHTTPContent( parameters ), new JSONResponseListener( new JSONValueListener() {

			@Override
			public void onObjectReceived( JSONValue json ) {
				if( json == null ) {
					throw new EmptyResponseException();
				}
				result.activationCode = JSONUtils.getString( json, KEY_PROVISIONING_CODE );
				if( StringUtils.isEmpty( result.activationCode ) ) {
					CharSequence errorMessage = JSONUtils.getString( json, KEY_ERROR_MESSAGE );
					throw new RuntimeException( StringUtils.isEmpty( errorMessage ) ? getString( R.string.error_unknown ) : String.valueOf( errorMessage ) );
				}
			}

		} ) );

		return result.activationCode;

	}

	private JSONObject getAccountCreationRequestJSON( CharSequence username, CharSequence password, CharSequence emailAddress, CharSequence firstName, CharSequence lastName ) {

		JSONObject json = new JSONObject();

		try {

			json.put( KEY_SERIAL_NUMBER, deviceSerial );
			json.put( KEY_USERNAME, username );
			json.put( KEY_PASSWORD, password );
			json.put( KEY_FIRST_NAME, firstName );
			json.put( KEY_LAST_NAME, lastName );
			json.put( KEY_EMAIL, emailAddress );

		} catch( JSONException exception ) {
			throw new RuntimeException( exception );
		}

		return json;

	}

	private String getAccountCreationURL() {
		return url( PROVISIONING, IMEI, deviceID, "" );
	}

	protected Context getContext() {
		return contextReference.get();
	}

	protected String getString( int stringResourceID ) {
		Context context = getContext();
		return context == null ? null : context.getString( stringResourceID );
	}

	public boolean isEntitledToAccountCreation() {

		final Result result = new Result();

		http.get( getAccountCreationURL(), new HTTPResponseListener() {

			@Override
			public void onResponse( HTTPResponse response ) {
				LOG.debug( "#onResponse status:%d", Integer.valueOf( response.getStatusCode() ) );
				result.eligible = response.getStatusCode() == 200;
			}

		} );

		return result.eligible;

	}

	private String url( CharSequence... components ) {
		URLBuilder url = new URLBuilder( baseURL.toString() );
		for( int i = 0; i < components.length; i++ ) {
			url.component( components[i].toString() );
		}
		return url.toString();
	}

}
