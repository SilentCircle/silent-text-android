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
package com.silentcircle.silenttext.client.fixture;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.tester.org.apache.http.RequestMatcher;

import com.silentcircle.api.Authenticator;
import com.silentcircle.api.model.Application;
import com.silentcircle.api.model.Credential;
import com.silentcircle.api.model.Device;
import com.silentcircle.api.model.UsernamePasswordCredential;
import com.silentcircle.api.web.AuthenticatedSessionClient;
import com.silentcircle.api.web.AuthenticatorClient;
import com.silentcircle.api.web.model.BasicApplication;
import com.silentcircle.api.web.model.BasicDevice;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.silenttext.MockUtils;

public abstract class AccountsClientTestFixture extends ClientTestFixture {

	private static final CharSequence APPLICATION_ID_SILENT_TEXT = "silent_text";

	protected static JSONObject errorResponse( String errorMessage ) {
		JSONObject response = new JSONObject();
		try {
			response.put( "result", "error" );
			response.put( "message", errorMessage );
		} catch( JSONException exception ) {
			// Ignore.
		}
		return response;
	}

	protected static void injectSuccessfulLogin() {
		inject( whenLoginRequested(), respondWithValidLoginResponse() );
		inject( whenPushRegistrationRequested(), respondWithValidPushRegistrationResponse() );
	}

	protected static void injectSuccessfulLogout() {
		inject( whenPushUnregistrationRequested(), respondWithValidPushUnregistrationResponse() );
		inject( whenLogoutRequested(), respondWithValidLogoutResponse() );
	}

	protected static HttpResponse respondWithError( int status, String message ) {
		return respondWith( status, errorResponse( message ) );
	}

	protected static HttpResponse respondWithValidLoginResponse() {
		return respondWith( 200, validLoginResponse() );
	}

	protected static HttpResponse respondWithValidLogoutResponse() {
		return respondWith( 200, validLogoutResponse() );
	}

	protected static HttpResponse respondWithValidPushRegistrationResponse() {
		return respondWith( 200, validPushRegistrationResponse() );
	}

	protected static HttpResponse respondWithValidPushUnregistrationResponse() {
		return respondWith( 200, validPushUnregistrationResponse() );
	}

	protected static HttpResponse respondWithValidResponse() {
		return respondWith( 200, validResponse() );
	}

	protected static JSONObject validLoginResponse() {
		JSONObject response = validResponse();
		try {
			response.put( "api_key", MockUtils.mockAPIKey() );
		} catch( JSONException exception ) {
			// Ignore.
		}
		return response;
	}

	protected static JSONObject validLogoutResponse() {
		return validResponse();
	}

	protected static JSONObject validPushRegistrationResponse() {
		return validResponse();
	}

	protected static JSONObject validPushUnregistrationResponse() {
		return validResponse();
	}

	protected static JSONObject validResponse() {
		JSONObject response = new JSONObject();
		try {
			response.put( "result", "success" );
		} catch( JSONException exception ) {
			// Ignore.
		}
		return response;
	}

	protected static RequestMatcher whenLoginRequested() {
		return whenRequested( "PUT", "^https?://[^/]+/v1/me/device/([^/]+)/$" );
	}

	protected static RequestMatcher whenLogoutRequested() {
		return whenRequested( "DELETE", "^https?://[^/]+/v1/me/device/([^/]+)/(\\?.+)?$" );
	}

	protected static RequestMatcher whenPublicKeyRemoved() {
		return whenRequested( "DELETE", "^https?://[^/]+/v1/me/pubkey/([^/]+)/(\\?.+)?$" );
	}

	protected static RequestMatcher whenPublicKeyRevoked() {
		return whenRequested( "DELETE", "^https?://[^/]+/v1/me/pubkey/([^/]+)/(\\?.+)?$" );
	}

	protected static RequestMatcher whenPublicKeyUploaded() {
		return whenRequested( "PUT", "^https?://[^/]+/v1/me/pubkey/([^/]+)/(\\?.+)?$" );
	}

	protected static RequestMatcher whenPushRegistrationRequested() {
		return whenRequested( "PUT", "^https?://[^/]+/v1/me/device/([^/]+)/application/([^/]+)/(\\?.+)?$" );
	}

	protected static RequestMatcher whenPushUnregistrationRequested() {
		return whenRequested( "DELETE", "^https?://[^/]+/v1/me/device/([^/]+)/application/([^/]+)/(\\?.+)?$" );
	}

	protected AuthenticatedSessionClient session;

	private static final CharSequence PUSH_SERVICE_GCM = "gcm";

	private static final CharSequence PUSH_TARGET_DEVELOPMENT = "dev";

	private static final CharSequence DEVICE_CATEGORY_ANDROID = "android";

	protected void login() {
		login( "https://localhost:7357", "alice", "example" );
	}

	protected void login( CharSequence baseURL, CharSequence username, CharSequence password ) {

		Authenticator activation = new AuthenticatorClient( new ApacheHTTPClient( new HttpClient() ), baseURL );
		Credential credential = new UsernamePasswordCredential( username, password );

		CharSequence deviceID = MockUtils.mockDeviceID();
		CharSequence deviceName = MockUtils.mockDeviceName();
		CharSequence pushRegistrationToken = MockUtils.mockPushRegistrationToken();
		Device device = new BasicDevice( deviceID, DEVICE_CATEGORY_ANDROID, deviceName );
		Application app = new BasicApplication( APPLICATION_ID_SILENT_TEXT );

		session = (AuthenticatedSessionClient) activation.authenticate( credential, device, app );
		session.startReceivingPushNotifications( APPLICATION_ID_SILENT_TEXT, PUSH_SERVICE_GCM, PUSH_TARGET_DEVELOPMENT, pushRegistrationToken );

	}

	protected void loginSuccessfully() {
		injectSuccessfulLogin();
		login();
	}

	protected void loginThenLogout() {
		login();
		logout();
	}

	protected void logout() {
		session.stopReceivingPushNotifications( APPLICATION_ID_SILENT_TEXT );
		session.logout();
	}

	protected void logoutSuccessfully() {
		injectSuccessfulLogout();
		logout();
	}

}
