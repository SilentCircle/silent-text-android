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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.net.SocketFactory;

import org.twuni.xmppt.xml.XMLBuilder;
import org.twuni.xmppt.xml.XMLElement;
import org.twuni.xmppt.xmpp.XMPPClientConnection;
import org.twuni.xmppt.xmpp.XMPPClientConnection.AcknowledgmentListener;
import org.twuni.xmppt.xmpp.XMPPClientConnection.PacketListener;
import org.twuni.xmppt.xmpp.XMPPClientConnectionManager;
import org.twuni.xmppt.xmpp.core.IQ;
import org.twuni.xmppt.xmpp.core.Message;
import org.twuni.xmppt.xmpp.stream.AcknowledgmentRequest;
import org.twuni.xmppt.xmpp.stream.StreamError;
import org.twuni.xmppt.xmpp.stream.StreamManagement;

import android.content.Context;

import com.silentcircle.http.client.exception.NetworkException;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.listener.TransportListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.task.GetDeviceChangedTask;
import com.silentcircle.silenttext.transport.Envelope;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.transport.TransportQueue.Processor;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.StringUtils;

public class XMPPTransport implements Processor, AcknowledgmentListener, PacketListener, AcknowledgmentRequestor {

	private static class Endpoint {

		public final CharSequence host;
		public final int port;
		public final String serviceName;

		public Endpoint( CharSequence host, int port, String serviceName ) {
			this.host = host;
			this.port = port;
			this.serviceName = serviceName;
		}

	}

	public static interface OnDisconnectedListener {

		public void onDisconnected( XMPPTransport client );

	}

	private static class PingTask extends TimerTask {

		private final AcknowledgmentRequestor requestor;

		public PingTask( AcknowledgmentRequestor requestor ) {
			this.requestor = requestor;
		}

		@Override
		public void run() {
			requestor.requestAcknowledgment();
		}

	}

	private static class ProcessQueueTask extends TimerTask {

		private final TransportQueue queue;
		private final Processor processor;

		public ProcessQueueTask( TransportQueue queue, Processor processor ) {
			this.queue = queue;
			this.processor = processor;
		}

		/**
		 * @param exception
		 */
		protected void onException( Throwable exception ) {
			// By default, do nothing.
		}

		@Override
		public void run() {
			try {
				queue.process( processor );
			} catch( Throwable exception ) {
				onException( exception );
			}
		}

	}

	public static final int DEFAULT_PING_INTERVAL = 5 * 60 * 1000;
	public static final int DEFAULT_QUEUE_PROCESSOR_INTERVAL = 2500;
	public static final int MAXIMUM_CONSECUTIVE_FAILED_ACKNOWLEDGMENTS = 2;

	final Context context;
	protected final List<TransportListener> listeners = new ArrayList<TransportListener>();
	private Log log;
	private final String connectionID;
	private final XMPPClientConnectionManager connectionManager = new XMPPClientConnectionManager();
	private final Endpoint endpoint;
	private final TransportQueue queue;
	private final Timer timer;
	private TimerTask pingTask;
	private TimerTask processQueueTask;
	private final List<Object> unacknowledgedPackets = new ArrayList<Object>();
	private int consecutiveFailedAcknowledgments = 0;

	private static final IllegalStateException NOT_CONNECTED = new IllegalStateException( new NetworkException() );

	public XMPPTransport( Context context, Credential credential, String resourceName, CharSequence host, int port, TransportQueue queue, SocketFactory socketFactory ) {
		this.context = context;
		endpoint = new Endpoint( host, port, credential.getDomain() );
		connectionID = credential.getUsername();
		this.queue = queue;
		timer = new Timer( true );
		XMPPClientConnection.Builder connection = new XMPPClientConnection.Builder();
		connection.logger( new AndroidLogger( "JabberClient" ) );
		connection.host( String.valueOf( host ) ).port( port ).secure( true );
		connection.serviceName( credential.getDomain() );
		connection.userName( credential.getUsername().replaceAll( "^(.+)@(.+)$", "$1" ) ).password( credential.getPassword() );
		connection.resourceName( resourceName );
		if( socketFactory instanceof XMPPSocketFactory ) {
			connection.socketFactory( ( (XMPPSocketFactory) socketFactory ).proxy() );
		}
		connection.acknowledgmentListener( this );
		connection.packetListener( this );
		connectionManager.startManaging( connectionID, connection );
	}

	public void addListener( TransportListener listener ) {
		listeners.add( listener );
	}

	public void adviseReconnect() {
		try {
			connectionManager.connectAll();
			startQueueProcessor();
			startPing();
		} catch( IllegalStateException exception ) {
			throw new NetworkException( exception );
		} catch( IOException exception ) {
			throw new NetworkException( exception );
		}
	}

	public void disconnect() {
		softDisconnect();
		connectionManager.stopManaging( connectionID );
	}

	private XMPPClientConnection getConnection() {
		return connectionManager.getConnection( connectionID );
	}

	public Log getLog() {
		if( log == null ) {
			log = new Log( "JabberClient" );
		}
		return log;
	}

	public CharSequence getServerHost() {
		return endpoint.host;
	}

	public int getServerPort() {
		return endpoint.port;
	}

	public boolean isConnected() {
		return connectionManager.isConnected( connectionID );
	}

	public void onDestroy() {
		getLog().onDestroy();
	}

	@Override
	public void onException( XMPPClientConnection connection, Throwable exception ) {
		getLog().error( exception, "#onException" );
	}

	@Override
	public void onFailedAcknowledgment( XMPPClientConnection connection, int expected, int actual ) {
		getLog().info( "#onFailedAcknowledgment expected:%d actual:%d", Integer.valueOf( expected ), Integer.valueOf( actual ) );
		consecutiveFailedAcknowledgments++;
		if( consecutiveFailedAcknowledgments >= MAXIMUM_CONSECUTIVE_FAILED_ACKNOWLEDGMENTS ) {
			softDisconnect();
			try {
				adviseReconnect();
			} catch( NetworkException exception ) {
				getLog().info( exception, "#onFailedAcknowledgment" );
			} catch( RuntimeException exception ) {
				getLog().info( exception, "#onFailedAcknowledgment" );
			}
			return;
		}
		for( Object packet : unacknowledgedPackets ) {
			send( packet );
		}
		if( queue instanceof AcknowledgmentListener ) {
			( (AcknowledgmentListener) queue ).onFailedAcknowledgment( connection, expected, actual );
		}
	}

	@Override
	public void onPacketReceived( XMPPClientConnection connection, Object packet ) {

		if( packet instanceof Message ) {

			Message message = (Message) packet;

			Envelope envelope = new Envelope();

			envelope.time = System.currentTimeMillis();
			envelope.id = message.id();
			envelope.to = message.to();
			envelope.from = message.from();

			String content = String.valueOf( message.getContent() );
			envelope.content = StringUtils.find( content, "^.*<x.+>(\\?SCIMP:[^\\.]+.)</x>.*$", 1 );

			// TODO: look at namespace for special indicator and add it to a new flag on the
			// Envelope:
			// <body/><x xmlns="http://silentcircle.com/protocol/scimp#public-key">

			if( Message.TYPE_ERROR.equals( message.type() ) ) {

				String from = envelope.to;

				envelope.to = envelope.from;
				envelope.from = from;

				for( TransportListener listener : listeners ) {
					listener.onMessageReturned( envelope, "returned" );
				}

			} else {

				boolean secure = envelope.content != null;

				if( !secure ) {
					envelope.content = StringUtils.find( content, "^.*<body>(.+)</body>.*$", 1 );
				}

				for( TransportListener listener : listeners ) {
					if( secure ) {
						listener.onSecureMessageReceived( envelope );
					} else if( envelope.content != null ) {
						listener.onInsecureMessageReceived( envelope );
					}
				}

			}

		} else if( packet instanceof StreamError ) {
			String foundReplacedConnection = StringUtils.find( String.valueOf( packet ).toString(), "^.*(Replaced by new connection).*$", 1 );

			if( foundReplacedConnection != null ) {
				getLog().info( "#StreamError %s", packet );

				AsyncUtils.execute( new GetDeviceChangedTask( context ) {

					@Override
					protected void onPostExecute( Void result ) {
						if( deviceChanged ) {
							( (SilentTextApplication) context ).deactivate();
						}
					}

				}, SilentTextApplication.from( context ).getUsername() );
			}
		} else {
			getLog().info( "#onPacketReceived %s", packet );
		}

	}

	@Override
	public void onSuccessfulAcknowledgment( XMPPClientConnection connection ) {
		getLog().info( "#onSuccessfulAcknowledgment" );
		consecutiveFailedAcknowledgments = 0;
		unacknowledgedPackets.clear();
		if( queue instanceof AcknowledgmentListener ) {
			( (AcknowledgmentListener) queue ).onSuccessfulAcknowledgment( connection );
		}
	}

	@Override
	public void process( Envelope envelope ) {

		if( !connectionManager.isConnected( connectionID ) ) {
			throw NOT_CONNECTED;
		}

		// add an empty <body/> tag for XMPP
		XMLBuilder body = new XMLBuilder( "body" );
		body.content( "" );

		XMLBuilder x = new XMLBuilder( "x" );
		x.attribute( XMLElement.ATTRIBUTE_NAMESPACE, "http://silentcircle.com" );
		x.attribute( "badge", Boolean.toString( envelope.badgeworthy ) );
		x.attribute( "notifiable", Boolean.toString( envelope.notifiable ) );
		x.content( envelope.content );

		Message message = new Message( envelope.id, Message.TYPE_CHAT, null, envelope.to, body.toString() + x.toString() );

		send( message );

		for( TransportListener listener : listeners ) {
			listener.onMessageSent( envelope );
		}

	}

	public void removeOfflineMessage( String remoteUserID, String messageID ) {

		XMLBuilder offline = new XMLBuilder( "offline" );

		offline.attribute( XMLElement.ATTRIBUTE_NAMESPACE, "http://silentcircle.com/protocol/offline" );

		XMLBuilder item = new XMLBuilder( "item" );

		item.attribute( "action", "remove" );
		item.attribute( "to", remoteUserID );
		item.attribute( "id", messageID );

		offline.content( item.close() );

		Object iq = new IQ( UUID.randomUUID().toString(), IQ.TYPE_SET, null, endpoint.serviceName, offline );
		unacknowledgedPackets.add( iq );
		send( iq );

	}

	@Override
	public void requestAcknowledgment() {
		if( isConnected() ) {
			if( getConnection().isFeatureAvailable( StreamManagement.class ) ) {
				send( new AcknowledgmentRequest() );
			} else {
				onSuccessfulAcknowledgment( getConnection() );
			}
		}
	}

	private void send( Object... packets ) {
		try {
			XMPPClientConnection conn = getConnection();
			if( conn != null ) {
				conn.send( packets );
			}
		} catch( IOException exception ) {
			getLog().error( exception, "#send" );
		}
	}

	public void sendEncryptedMessage( Envelope envelope ) {
		queue.add( envelope );
	}

	public void setLog( Log log ) {
		this.log = log;
	}

	public void softDisconnect() {

		XMPPClientConnection connection = connectionManager.getConnection( connectionID );

		if( connection != null ) {
			try {
				connection.logout();
				connection.disconnect();
			} catch( IOException exception ) {
				getLog().info( exception, "#softDisconnect" );
			}
		}

		stopQueueProcessor();
		stopPing();

	}

	private void startPing() {
		startPing( DEFAULT_PING_INTERVAL );
	}

	private void startPing( int interval ) {
		stopPing();
		if( isConnected() ) {
			pingTask = new PingTask( this );
			timer.schedule( pingTask, interval, interval );
		}
	}

	private void startQueueProcessor() {
		startQueueProcessor( DEFAULT_QUEUE_PROCESSOR_INTERVAL );
	}

	private void startQueueProcessor( int interval ) {
		stopQueueProcessor();
		if( isConnected() ) {
			if( queue instanceof AcknowledgmentListener ) {
				( (AcknowledgmentListener) queue ).onFailedAcknowledgment( getConnection(), 0, 0 );
			}
			processQueueTask = new ProcessQueueTask( queue, this ) {

				@Override
				protected void onException( Throwable exception ) {
					getLog().warn( exception, "ProcessQueueTask#onException" );
					softDisconnect();
				}

			};
			timer.schedule( processQueueTask, interval, interval );
		}
	}

	private void stopPing() {
		if( pingTask != null ) {
			pingTask.cancel();
			pingTask = null;
		}
		timer.purge();
	}

	private void stopQueueProcessor() {
		if( processQueueTask != null ) {
			processQueueTask.cancel();
			processQueueTask = null;
		}
		timer.purge();
	}

}
