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

import java.security.KeyStore;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import com.silentcircle.silenttext.log.Log;

public class ValidatingHttpClient extends DefaultHttpClient {

	private static final Log LOG = new Log( "ValidatingHttpClient" );
	private static final String HTTPS = "https";

	private static SSLSocketFactory createSSLSocketFactory( KeyStore trustStore ) {
		try {
			return new SSLSocketFactory( trustStore );
		} catch( Exception exception ) {
			LOG.error( exception, "#createSSLSocketFactory" );
			return SSLSocketFactory.getSocketFactory();
		}
	}

	public ValidatingHttpClient( KeyStore trustStore ) {
		initialize( trustStore );
	}

	public ValidatingHttpClient( KeyStore trustStore, ClientConnectionManager connectionManager, HttpParams parameters ) {
		super( connectionManager, parameters );
		initialize( trustStore );
	}

	public ValidatingHttpClient( KeyStore trustStore, HttpParams parameters ) {
		super( parameters );
		initialize( trustStore );
	}

	protected void initialize( KeyStore trustStore ) {
		X509HostnameVerifier verifier = new SilentHostnameVerifier( trustStore );
		SchemeRegistry registry = getConnectionManager().getSchemeRegistry();
		SSLSocketFactory factory = createSSLSocketFactory( trustStore );
		factory.setHostnameVerifier( verifier );
		registry.register( new Scheme( HTTPS, factory, 443 ) );
	}

}
