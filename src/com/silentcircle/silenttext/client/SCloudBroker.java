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
import java.io.InputStream;
import java.security.KeyStore;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.listener.JSONObjectListener;
import com.silentcircle.silenttext.listener.JSONResponseListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Credential;

public class SCloudBroker {

	private static final String BROKER_URI = "/broker/";

	protected final Log log = new Log( getClass().getSimpleName() );
	private final Credential serverCredential;
	private final Repository<ServiceEndpoint> networks;
	protected final SimpleHTTPClient http;
	protected String deviceId;

	/**
	 * @deprecated This constructor will not validate connections against the embedded trust store.
	 *             Consider using {@link SCloudBroker#SCloudBroker(Credential,KeyStore)} instead.
	 */
	@Deprecated
	public SCloudBroker( Credential serverCredential ) {
		this( serverCredential, new SimpleHTTPClient() );
	}

	public SCloudBroker( Credential serverCredential, KeyStore trustStore ) {
		this( serverCredential, trustStore, null );
	}

	public SCloudBroker( Credential serverCredential, KeyStore trustStore, Repository<ServiceEndpoint> networks ) {
		this( serverCredential, new SimpleHTTPClient( trustStore ), networks );
	}

	public SCloudBroker( Credential serverCredential, SimpleHTTPClient http ) {
		this( serverCredential, http, null );
	}

	public SCloudBroker( Credential serverCredential, SimpleHTTPClient http, Repository<ServiceEndpoint> networks ) {
		this.serverCredential = serverCredential;
		this.http = http;
		this.networks = networks;
	}

	private String getBaseURL() {
		return ServiceConfiguration.getInstance().api.getURL( networks );
	}

	public void prepareSCloudUpload( JSONObject files, JSONObjectListener onObjectReceivedListener ) {

		JSONObject request = new JSONObject();

		try {
			request.put( "operation", "upload" );
			request.put( "api_key", serverCredential.getPassword() );
			request.put( "files", files );
		} catch( JSONException impossible ) {
			// Don't worry about it.
		}

		byte [] requestData = request.toString().getBytes();
		URLBuilder url = new URLBuilder( getBaseURL(), BROKER_URI );
		InputStream body = new ByteArrayInputStream( requestData );
		HTTPContent content = new HTTPContent( body, "application/json", requestData.length );

		try {
			http.post( url.toString(), content, new JSONResponseListener( onObjectReceivedListener ) );
		} catch( RuntimeException exception ) {
			ServiceConfiguration.getInstance().api.removeFromCache( networks );
			throw exception;
		}

	}

	public void setAPIKey( String apiKey ) {
		serverCredential.setPassword( apiKey );
	}

}
