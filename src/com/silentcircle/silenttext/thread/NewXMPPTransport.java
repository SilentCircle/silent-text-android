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
package com.silentcircle.silenttext.thread;

import java.util.List;

import javax.net.SocketFactory;

import android.content.Context;

import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.client.XMPPTransport;
import com.silentcircle.silenttext.client.dns.CachingSRVResolver;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.thread.NamedThread.HasThreadName;
import com.silentcircle.silenttext.transport.TransportQueue;

public class NewXMPPTransport implements Runnable, HasThreadName {

	private final Context context;
	private final TransportQueue queue;
	private final Credential credential;
	private final XMPPTransport previousClient;
	private final String resourceName;
	private final CachingSRVResolver networks;
	private final SocketFactory socketFactory;

	private final Log log = new Log( "NewJabberClient" );

	public NewXMPPTransport( Context context, XMPPTransport previousClient, Credential credential, String resourceName, TransportQueue queue, CachingSRVResolver networks, SocketFactory socketFactory ) {
		this.context = context;
		this.previousClient = previousClient;
		this.credential = credential;
		this.resourceName = resourceName;
		this.queue = queue;
		this.networks = networks;
		this.socketFactory = socketFactory;
	}

	@Override
	public String getThreadName() {
		return "NewJabberClient";
	}

	/**
	 * @param client
	 */
	protected void onClientConnected( XMPPTransport client ) {
		// Do nothing.
	}

	/**
	 * @param client
	 */
	protected void onClientInitialized( XMPPTransport client ) {
		// Do nothing.
	}

	/**
	 * @param throwable
	 */
	protected void onError( Throwable throwable ) {
		// Do nothing.
	}

	/**
	 * @param domain
	 */
	protected void onLookupServiceRecord( String domain ) {
		// Do nothing.
	}

	/**
	 * @param host
	 * @param port
	 */
	protected void onServiceRecordReceived( CharSequence host, int port ) {
		// Do nothing.
	}

	@Override
	public void run() {

		try {

			if( previousClient != null ) {
				previousClient.disconnect();
			}

			ServiceConfiguration config = ServiceConfiguration.getInstance();

			if( config.xmpp.performSRVLookup ) {
				onLookupServiceRecord( config.xmpp.override ? config.xmpp.host : config.environment );
			}

			List<ServiceEndpoint> endpoints = config.getXMPPServiceEndpoints( networks );

			for( ServiceEndpoint endpoint : endpoints ) {

				try {
					onServiceRecordReceived( endpoint.host, endpoint.port );
					XMPPTransport client = new XMPPTransport( context, credential, resourceName, endpoint.host, endpoint.port, queue, socketFactory );
					onClientInitialized( client );
					client.adviseReconnect();
					onClientConnected( client );
					return;
				} catch( NetworkException exception ) {
					if( StringUtils.equals( exception.getMessage(), "<not-authorized/>" ) ) {
						log.error( exception, "XMPP connection not authorized with host %s:%d", endpoint.host, Integer.valueOf( endpoint.port ) );

						throw new IllegalStateException( exception.getMessage() );
					}
				} catch( Throwable exception ) {
					// This endpoint didn't work. Let's try a different one.
					log.error( exception, "Unable to establish XMPP connection with host %s:%d", endpoint.host, Integer.valueOf( endpoint.port ) );
				}

			}
			throw new NetworkException( "Unable to establish a XMPP client connection." );
		} catch( Throwable exception ) {
			onError( exception );
		}

	}

}
