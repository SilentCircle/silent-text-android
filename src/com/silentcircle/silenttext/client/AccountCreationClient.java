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
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.security.KeyStore;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.listener.HTTPResponseListener;
import com.silentcircle.silenttext.listener.JSONObjectListener;
import com.silentcircle.silenttext.listener.JSONResponseListener;
import com.silentcircle.silenttext.log.Log;

public class AccountCreationClient {

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
	private static final byte [] SALT_FOR_DEVICE_ID = "SilentCircle".getBytes();

	protected static final Log LOG = new Log( AccountCreationClient.class.getSimpleName() );
	protected static final String KEY_PROVISIONING_CODE = "provisioning_code";
	protected static final String KEY_ERROR_MESSAGE = "error_msg";

	private static JSONObject getAccountCreationRequestJSON( CharSequence username, CharSequence password, CharSequence emailAddress, CharSequence firstName, CharSequence lastName ) {

		JSONObject json = new JSONObject();

		try {

			json.put( KEY_SERIAL_NUMBER, getDeviceSerialNumber() );
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

	@TargetApi( Build.VERSION_CODES.GINGERBREAD )
	private static String getDeviceSerialNumber() {
		return android.os.Build.SERIAL;
	}

	private static HTTPContent toHTTPContent( JSONObject request ) {
		byte [] buffer = request.toString().getBytes();
		HTTPContent content = new HTTPContent( new ByteArrayInputStream( buffer ), CONTENT_TYPE_JSON, buffer.length );
		return content;
	}

	private final SimpleHTTPClient http;
	private final SoftReference<Context> contextReference;
	private CharSequence baseURL;

	/**
	 * @deprecated This constructor will not validate connections against the embedded trust store.
	 *             Consider using
	 *             {@link AccountCreationClient#AccountCreationClient(Context,KeyStore)} instead.
	 */
	@Deprecated
	public AccountCreationClient( Context context ) {
		this( context, new SimpleHTTPClient() );
	}

	public AccountCreationClient( Context context, CharSequence baseURL ) {
		this( context );
		this.baseURL = baseURL;
	}

	public AccountCreationClient( Context context, KeyStore trustStore ) {
		this( context, new SimpleHTTPClient( trustStore ) );
	}

	public AccountCreationClient( Context context, KeyStore trustStore, CharSequence baseURL ) {
		this( context, trustStore );
		this.baseURL = baseURL;
	}

	public AccountCreationClient( Context context, SimpleHTTPClient http ) {
		contextReference = new SoftReference<Context>( context );
		this.http = http;
	}

	public AccountCreationClient( Context context, SimpleHTTPClient http, CharSequence baseURL ) {
		contextReference = new SoftReference<Context>( context );
		this.http = http;
		this.baseURL = baseURL;
	}

	public CharSequence createAccount( CharSequence username, CharSequence password, CharSequence emailAddress, CharSequence firstName, CharSequence lastName ) {

		JSONObject parameters = getAccountCreationRequestJSON( username, password, emailAddress, firstName, lastName );
		final Result result = new Result();

		http.post( getAccountCreationURL(), toHTTPContent( parameters ), new JSONResponseListener( new JSONObjectListener() {

			@Override
			public void onObjectReceived( JSONObject json ) {
				if( json.has( KEY_PROVISIONING_CODE ) ) {
					try {
						result.activationCode = json.getString( KEY_PROVISIONING_CODE );
					} catch( JSONException exception ) {
						throw new RuntimeException( exception );
					}
					return;
				}
				if( json.has( KEY_ERROR_MESSAGE ) ) {
					try {
						throw new RuntimeException( json.getString( KEY_ERROR_MESSAGE ) );
					} catch( JSONException exception ) {
						throw new RuntimeException( getString( R.string.error_unknown ) );
					}
				}
				throw new RuntimeException( getString( R.string.error_unknown ) );
			}

		} ) );

		return result.activationCode;

	}

	private String getAccountCreationURL() {
		return url( PROVISIONING, IMEI, getHashedDeviceID() );
	}

	protected Context getContext() {
		return contextReference.get();
	}

	private String getDeviceID() {
		TelephonyManager telephony = getSystemService( Context.TELEPHONY_SERVICE );
		return telephony == null ? UUID.randomUUID().toString() : telephony.getDeviceId();
	}

	protected String getHashedDeviceID() {
		return new BigInteger( 1, Hash.sha1( SALT_FOR_DEVICE_ID, getDeviceID().getBytes() ) ).toString( 16 );
	}

	protected String getString( int stringResourceID ) {
		Context context = getContext();
		return context == null ? null : context.getString( stringResourceID );
	}

	protected <T> T getSystemService( String serviceName ) {
		Context context = getContext();
		return context == null ? null : (T) context.getSystemService( serviceName );
	}

	public boolean isEntitledToAccountCreation() {
	    return false;
	}

	private String url( CharSequence... components ) {
		URLBuilder url = new URLBuilder( baseURL.toString() );
		for( int i = 0; i < components.length; i++ ) {
			url.component( components[i].toString() );
		}
		return url.toString();
	}

}
