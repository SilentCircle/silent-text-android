/*
Copyright (C) 2013-2015, Silent Circle, LLC. All rights reserved.

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

import org.twuni.twoson.JSONGenerator;

import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.AbstractHTTPClient;
import com.silentcircle.http.client.HTTPContent;
import com.silentcircle.http.client.URLBuilder;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.http.client.listener.JSONResponseListener;
import com.silentcircle.http.client.listener.JSONValueListener;
import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.client.dns.CachingSRVResolver;
import com.silentcircle.silenttext.model.Credential;

public class SCloudBroker {

	private static final String BROKER_URI = "/broker/";

	private final CachingSRVResolver networks;
	private final AbstractHTTPClient http;
	private final CharSequence accessToken;

	/**
	 * @deprecated This constructor will not validate connections against the embedded trust store.
	 *             Consider using {@link SCloudBroker#SCloudBroker(Credential,KeyStore)} instead.
	 */
	@Deprecated
	public SCloudBroker( CharSequence accessToken ) {
		this( accessToken, new ApacheHTTPClient( new HttpClient() ) );
	}

	public SCloudBroker( CharSequence accessToken, AbstractHTTPClient http ) {
		this( accessToken, http, null );
	}

	public SCloudBroker( CharSequence accessToken, AbstractHTTPClient http, CachingSRVResolver networks ) {

		if( accessToken == null ) {
			throw new IllegalArgumentException( "Missing required parameter: accessToken" );
		}

		if( http == null ) {
			throw new IllegalArgumentException( "Missing required parameter: http" );
		}

		this.accessToken = StringUtils.clone( accessToken );
		this.http = http;
		this.networks = networks;

	}

	public SCloudBroker( CharSequence accessToken, KeyStore trustStore ) {
		this( accessToken, trustStore, null );
	}

	public SCloudBroker( CharSequence accessToken, KeyStore trustStore, CachingSRVResolver networks ) {
		this( accessToken, new ApacheHTTPClient( new HttpClient( trustStore ) ), networks );
	}

	private String getBaseURL() {
		return ServiceConfiguration.getInstance().getAPIURL( networks );
	}

	public void prepareSCloudUpload( List<SCloudObject> objects, long expirationTime, JSONValueListener onObjectReceivedListener ) throws IOException {

		if( StringUtils.isEmpty( accessToken ) ) {
			throw new IllegalStateException( "Missing field: accessToken" );
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JSONGenerator request = new JSONGenerator( out );

		request.openObject();

		request.writeKey( "operation" );
		request.writeString( "upload" );

		request.next();

		request.writeKey( "api_key" );
		request.writeString( StringUtils.toByteArray( accessToken ) );

		request.next();

		request.writeKey( "files" );
		request.openObject();

		int objectsCount = objects.size();

		for( int i = 0; i < objectsCount; i++ ) {

			SCloudObject object = objects.get( i );

			if( i > 0 ) {
				request.next();
			}

			request.writeKey( String.valueOf( object.getLocator() ) );
			request.openObject();

			request.writeKey( "shred_date" );
			request.writeString( StringUtils.getDate( expirationTime ) );

			request.next();

			request.writeKey( "size" );
			request.write( object.getSize() );

			request.closeObject();// file

		}

		request.closeObject();// files
		request.closeObject();// ROOT

		byte [] requestData = out.toByteArray();
		URLBuilder url = new URLBuilder( getBaseURL(), BROKER_URI );
		InputStream body = new ByteArrayInputStream( requestData );
		HTTPContent content = new HTTPContent( body, "application/json", requestData.length );

		http.post( url.toString(), content, new JSONResponseListener( onObjectReceivedListener ) );

	}

}
