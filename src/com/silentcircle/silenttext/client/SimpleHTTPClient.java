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
import java.io.IOException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.json.JSONObject;

import com.silentcircle.silenttext.listener.HTTPResponseListener;
import com.silentcircle.silenttext.log.Log;

public class SimpleHTTPClient {

	private static class HTTPResponseConsumer implements HTTPResponseListener {

		public HTTPResponseConsumer() {
		}

		@Override
		public void onResponse( HttpResponse response ) {
			try {
				response.getEntity().consumeContent();
			} catch( IOException exception ) {
				// Doesn't matter. We're just cleaning up.
			}
		}
	}

	protected static class ListeningResponseHandler implements ResponseHandler<Void> {

		protected final HTTPResponseListener listener;

		public ListeningResponseHandler( HTTPResponseListener listener ) {
			this.listener = listener;
		}

		@Override
		public Void handleResponse( HttpResponse response ) throws ClientProtocolException, IOException {
			listener.onResponse( response );
			return null;
		}

	}

	protected final Log log = new Log( getClass().getSimpleName() );
	private static final HTTPResponseConsumer HTTP_RESPONSE_CONSUMER = new HTTPResponseConsumer();
	private static final HashMap<String, String> NO_HEADERS = new HashMap<String, String>( 0 );
	private static final String CONTENT_TYPE_JSON = "application/json";

	private static void applyHeaders( AbstractHttpMessage http, HTTPContent content, Map<String, String> headers ) {

		for( String key : headers.keySet() ) {
			http.addHeader( key, headers.get( key ) );
		}

		if( headers.get( "Content-Length" ) == null ) {
			// http.addHeader( "Content-Length", Long.toString( content.getLength() ) );
		}

		if( headers.get( "Content-Type" ) == null ) {
			http.addHeader( "Content-Type", content.getType() );
		}

	}

	private static HttpUriRequest createPOST( String url, HTTPContent content, Map<String, String> headers ) {
		return prepareRequest( new HttpPost( url ), content, headers );
	}

	private static HttpUriRequest createPUT( String url, HTTPContent content, Map<String, String> headers ) {
		return prepareRequest( new HttpPut( url ), content, headers );
	}

	private static <T extends HttpEntityEnclosingRequestBase> T prepareRequest( T request, HTTPContent content, Map<String, String> headers ) {
		applyHeaders( request, content, headers );
		try {
			request.setEntity( new BufferedHttpEntity( new InputStreamEntity( content.getBody(), content.getLength() ) ) );
		} catch( IOException exception ) {
			request.setEntity( new InputStreamEntity( content.getBody(), content.getLength() ) );
		}
		return request;
	}

	public static Map<String, String> toStringMap( Header... headers ) {
		Map<String, String> map = new HashMap<String, String>( headers.length );
		for( Header header : headers ) {
			map.put( header.getName(), header.getValue() );
		}
		return map;
	}

	protected final HttpClient http;

	/**
	 * @deprecated This constructor will not validate connections against the embedded trust store.
	 *             Consider using {@link SimpleHTTPClient#SimpleHTTPClient(KeyStore)} instead.
	 */
	@Deprecated
	public SimpleHTTPClient() {
		this( new DefaultHttpClient() );
	}

	public SimpleHTTPClient( HttpClient http ) {
		this.http = http;
	}

	public SimpleHTTPClient( KeyStore trustStore ) {
		this( new ValidatingHttpClient( trustStore ) );
	}

	public void delete( String url ) {
		delete( url, null );
	}

	public void delete( String url, HTTPResponseListener responseListener ) {
		execute( new HttpDelete( url ), responseListener );
	}

	private void execute( HttpUriRequest request ) {
		execute( request, null );
	}

	private void execute( HttpUriRequest request, HTTPResponseListener responseListener ) {
		log.debug( "#execute method:%s uri:%s", request.getMethod(), request.getURI() );
		try {
			http.execute( request, new ListeningResponseHandler( responseListener == null ? HTTP_RESPONSE_CONSUMER : responseListener ) );
		} catch( Exception exception ) {
			throw new RuntimeException( exception.getMessage(), exception );
		}
	}

	public void get( String url, HTTPResponseListener responseListener ) {
		execute( new HttpGet( url ), responseListener );
	}

	public void post( String url, HTTPContent content ) {
		post( url, content, NO_HEADERS );
	}

	public void post( String url, HTTPContent content, HTTPResponseListener responseListener ) {
		post( url, content, NO_HEADERS, responseListener );
	}

	public void post( String url, HTTPContent content, Map<String, String> headers ) {
		execute( createPOST( url, content, headers ) );
	}

	public void post( String url, HTTPContent content, Map<String, String> headers, HTTPResponseListener responseListener ) {
		execute( createPOST( url, content, headers ), responseListener );
	}

	public void post( String url, String contentType, String contentBody ) {
		byte [] buffer = contentBody.getBytes();
		post( url, new HTTPContent( new ByteArrayInputStream( buffer ), contentType, buffer.length ) );
	}

	public void postJSON( String url, String json ) {
		post( url, CONTENT_TYPE_JSON, json );
	}

	public void put( String url, HTTPContent content ) {
		put( url, content, NO_HEADERS );
	}

	public void put( String url, HTTPContent content, HTTPResponseListener responseListener ) {
		put( url, content, NO_HEADERS, responseListener );
	}

	public void put( String url, HTTPContent content, Map<String, String> headers ) {
		execute( createPUT( url, content, headers ) );
	}

	public void put( String url, HTTPContent content, Map<String, String> headers, HTTPResponseListener responseListener ) {
		execute( createPUT( url, content, headers ), responseListener );
	}

	public void put( String url, JSONObject json ) {
		put( url, "application/json", json.toString().getBytes() );
	}

	public void put( String url, String type, byte [] data ) {
		put( url, new HTTPContent( new ByteArrayInputStream( data ), type, data.length ) );
	}

}
