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
package com.silentcircle.silenttext.thread;

import android.net.Uri;

import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.client.JabberClient;
import com.silentcircle.silenttext.client.JabberClient.OnDisconnectedListener;
import com.silentcircle.silenttext.client.model.ServiceEndpoint;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.transport.TransportQueue;

public class NewJabberClient implements Runnable {

	private final TransportQueue queue;
	private final String keystorePath;
	private final Server server;
	private final JabberClient previousClient;
	private final OnDisconnectedListener onDisconnected;
	private final String resourceName;
	private final long pingInterval;
	private final Repository<ServiceEndpoint> networks;

	public NewJabberClient( JabberClient previousClient, Server server, String resourceName, String keystorePath, TransportQueue queue, OnDisconnectedListener onDisconnected, long pingInterval, Repository<ServiceEndpoint> networks ) {
		this.previousClient = previousClient;
		this.server = server;
		this.resourceName = resourceName;
		this.keystorePath = keystorePath;
		this.queue = queue;
		this.onDisconnected = onDisconnected;
		this.pingInterval = pingInterval;
		this.networks = networks;
	}

	/**
	 * @param client
	 */
	protected void onClientConnected( JabberClient client ) {
		// Do nothing.
	}

	/**
	 * @param client
	 */
	protected void onClientInitialized( JabberClient client ) {
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
	protected void onServiceRecordReceived( String host, int port ) {
		// Do nothing.
	}

	@Override
	public void run() {
		try {
			if( previousClient != null ) {
				previousClient.logout();
			}
			ServiceConfiguration config = ServiceConfiguration.getInstance();
			if( config.xmpp.performSRVLookup ) {
				onLookupServiceRecord( config.xmpp.host );
			}
			server.setURL( config.xmpp.getURL( networks ) );
			onServiceRecordReceived( config.xmpp.host, config.xmpp.port );
			Uri url = Uri.parse( server.getURL() );
			JabberClient client = new JabberClient( server, resourceName, url.getHost(), url.getPort(), keystorePath, queue, pingInterval );
			client.setOnDisconnectedListener( onDisconnected );
			onClientInitialized( client );
			client.adviseReconnect();
			onClientConnected( client );
		} catch( Exception exception ) {
			onError( exception );
		}
	}

}
