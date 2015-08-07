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

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;
import org.robolectric.tester.org.apache.http.RequestMatcher;

import com.silentcircle.silenttext.fixture.TestFixture;

public abstract class ClientTestFixture extends TestFixture {

	protected static void doItLive() {
		FakeHttpLayer http = Robolectric.getFakeHttpLayer();
		http.interceptHttpRequests( false );
		http.logHttpRequests();
	}

	protected static void inject( RequestMatcher request, HttpResponse response ) {
		FakeHttpLayer http = Robolectric.getFakeHttpLayer();
		if( !http.isInterceptingHttpRequests() ) {
			http.interceptHttpRequests( true );
		}
		http.addHttpResponseRule( request, response );
	}

	protected static void respondTo( String method, String uriPattern, int statusCode, JSONObject responseBody ) {
		inject( whenRequested( method, uriPattern ), respondWith( statusCode, responseBody ) );
	}

	protected static HttpResponse respondWith( int statusCode, JSONObject responseBody ) {
		return respondWith( statusCode, "Mocked", "application/json", responseBody.toString() );
	}

	protected static HttpResponse respondWith( int statusCode, String reason, String contentType, String responseBody ) {
		BasicHttpResponse response = new BasicHttpResponse( new BasicStatusLine( new ProtocolVersion( "HTTP", 1, 1 ), statusCode, reason ) );
		response.addHeader( "Content-Type", contentType );
		try {
			response.setEntity( new StringEntity( responseBody ) );
		} catch( UnsupportedEncodingException exception ) {
			// Ignore.
		}
		return response;
	}

	protected static HttpResponse respondWithFileNotFound() {
		return respondWith( 404, "File Not Found", "text/html", "<html><body><h1>File Not Found</h1><p>The requested resource was not found on this server.</p></body></html>" );
	}

	protected static HttpResponse respondWithInternalServerError() {
		return respondWith( 500, "Internal Server Error", "text/html", "<html><body><h1>Internal Server Error</h1><p>An error occurred. Please try again.</p></body></html>" );
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

	protected static RequestMatcher whenRequested( String method, String uriPattern ) {
		return new FakeHttpLayer.UriRegexMatcher( method, uriPattern );
	}

}
