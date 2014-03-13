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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ping.packet.Ping;

import android.os.Build;

import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.listener.OnObjectReceiveListener;
import com.silentcircle.silenttext.listener.TransportListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.transport.QueueingTransport;
import com.silentcircle.silenttext.transport.Transport;
import com.silentcircle.silenttext.transport.Transport.OnReceiveListener;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.transport.xmpp.SecureXMPPTransport;
import com.silentcircle.silenttext.transport.xmpp.XMPPConnectionBuilder;
import com.silentcircle.silenttext.transport.xmpp.exception.NotAuthenticatedException;
import com.silentcircle.silenttext.transport.xmpp.exception.NotConnectedException;
import com.silentcircle.silenttext.transport.xmpp.packet.filter.PacketIDFilter;
import com.silentcircle.silenttext.transport.xmpp.packet.iq.GCMRegistration;
import com.silentcircle.silenttext.transport.xmpp.packet.iq.PrivacyPrivileges;
import com.silentcircle.silenttext.transport.xmpp.packet.iq.PrivacyPrivilegesQuery;
import com.silentcircle.silenttext.transport.xmpp.packet.iq.RemoveOfflineMessage;

public class JabberClient implements ConnectionListener {

	public static interface OnDisconnectedListener {

		public void onDisconnected( Throwable throwable );

	}

	protected class PingTask extends TimerTask {

		protected class DisconnectTask extends TimerTask {

			@Override
			public void run() {
				getLog().warn( "#ping timeout" );
				logout();
				adviseReconnect();
			}

		}

		protected DisconnectTask disconnect;

		@Override
		public void run() {

			final Ping packet = new Ping();

			disconnect = new DisconnectTask();
			timer.schedule( disconnect, 5000 );

			getConnection().addPacketListener( new PacketListener() {

				@Override
				public void processPacket( Packet p ) {
					getLog().warn( "#ping connected" );
					disconnect.cancel();
					timer.purge();
					disconnect = null;
				}

			}, new PacketFilter() {

				@Override
				public boolean accept( Packet p ) {
					return packet.getPacketID().equals( p.getPacketID() );
				}

			} );

			getConnection().sendPacket( packet );

		}

	}

	private static Connection createConnection( String host, int port, String keystorePath ) {
		XMPPConnectionBuilder builder = new XMPPConnectionBuilder();
		ServiceConfiguration config = ServiceConfiguration.getInstance();
		if( config.shouldValidateCertificates ) {
			builder.trust( "BKS", "silence", keystorePath );
		}
		if( config.loggingEnabled ) {
			builder.debug();
		}
		Connection connection = builder.presence().compress().sasl().service( config.xmpp.serviceName ).build( host, port );
		return connection;
	}

	public static String getDefaultResourceName() {
		return Build.MODEL;
	}

	private static String report( XMPPError error ) {
		if( error == null ) {
			return "";
		}
		Integer code = Integer.valueOf( error.getCode() );
		String condition = error.getCondition();
		String message = error.getMessage();
		XMPPError.Type type = error.getType();
		return String.format( "{type:%s,code:%d,condition:%s,message:%s}", type, code, condition, message );
	}

	private static String sanitizeResourceName( String in, String defaultValue ) {
		if( in == null ) {
			return defaultValue;
		}
		int size = in.length();
		if( size <= 0 ) {
			return defaultValue;
		}
		if( size > 1023 ) {
			return in.substring( 0, 1023 );
		}
		return in;
	}

	private static String withoutDomain( String username ) {
		return username == null ? null : username.replaceAll( "@.+$", "" );
	}

	private static String withoutResource( String username ) {
		return username == null ? null : username.replaceAll( "/.+$", "" );
	}

	private OnDisconnectedListener onDisconnectedListener;
	protected final List<TransportListener> listeners = new ArrayList<TransportListener>();
	private final QueueingTransport<SecureXMPPTransport> transport;
	private final Server server;

	protected final Timer timer = new Timer();
	private final long pingInterval;
	private String resourceName;

	private Log log;

	private TimerTask ping;

	public JabberClient( Server server, String resourceName, String host, int port, String keystorePath, TransportQueue queue, long pingInterval ) {

		this.server = server;
		this.pingInterval = pingInterval;
		this.resourceName = sanitizeResourceName( resourceName, getDefaultResourceName() );

		Connection connection = createConnection( host, port, keystorePath );
		SecureXMPPTransport wrapped = new SecureXMPPTransport( connection );

		transport = new QueueingTransport<SecureXMPPTransport>( wrapped, queue ) {

			@Override
			protected TransportQueue.Processor getProcessor() {

				final TransportQueue.Processor wrapped = super.getProcessor();

				return new TransportQueue.Processor() {

					@Override
					public void process( String id, String to, String text, boolean notifiable, boolean badgeworthy ) {
						wrapped.process( id, to, text, notifiable, badgeworthy );
						for( TransportListener listener : listeners ) {
							listener.onMessageSent( id, to, text );
						}
					}

				};

			}

		};

		transport.setOnReceiveListener( new OnReceiveListener() {

			@Override
			public void onMessageReturned( String id, String intendedRecipient, String reason ) {
				for( TransportListener listener : listeners ) {
					listener.onMessageReturned( id, intendedRecipient, reason );
				}
			}

			@Override
			public void onReceive( String id, String from, String text, boolean notifiable, boolean badgeworthy ) {
				for( TransportListener listener : listeners ) {
					listener.onSecureMessageReceived( id, from, text, notifiable, badgeworthy );
				}
			}

		} );

		transport.getWrappedTransport().startListening();

	}

	public void addListener( TransportListener listener ) {
		listeners.add( listener );
	}

	public void adviseReconnect() {
		adviseReconnect( transport.getWrappedTransport().getConnection() );
	}

	private void adviseReconnect( Connection connection ) {
		try {
			connectAndLogin( connection );
		} catch( XMPPException exception ) {
			connectionClosedOnError( new Exception( report( exception.getXMPPError() ), exception ) );
		}
	}

	private void cancelPing() {
		if( pingInterval <= 0 ) {
			return;
		}
		if( ping != null ) {
			ping.cancel();
			timer.purge();
			ping = null;
		}
	}

	public void connectAndLogin() throws XMPPException {
		connectAndLogin( transport.getWrappedTransport().getConnection() );
	}

	private void connectAndLogin( Connection connection ) throws XMPPException {
		if( !connection.isConnected() ) {
			connection.connect();
		}
		if( !connection.isAuthenticated() ) {
			connection.login( server.getCredential().getShortUsername(), server.getCredential().getPassword(), StringUtils.escapeForXML( getResourceName() ) );
			schedulePing();
		}
		listenForConnectionEvents( connection );
		transport.processQueue();
	}

	@Override
	public void connectionClosed() {
		if( onDisconnectedListener != null ) {
			cancelPing();
			onDisconnectedListener.onDisconnected( new Exception() );
		}
	}

	@Override
	public void connectionClosedOnError( Exception exception ) {
		if( onDisconnectedListener != null ) {
			cancelPing();
			onDisconnectedListener.onDisconnected( exception );
		}
	}

	protected Connection getConnection() {
		return transport.getWrappedTransport().getConnection();
	}

	public String getFullUsername() {
		String user = transport.getWrappedTransport().getConnection().getUser();
		return user == null ? server.getCredential().getUsername() : user;
	}

	public Log getLog() {
		if( log == null ) {
			log = new Log( getClass().getSimpleName() );
		}
		return log;
	}

	public String getResourceName() {
		return resourceName;
	}

	public String getServerHost() {
		try {
			Connection connection = transport.getWrappedTransport().getConnection();
			String host = connection.getHost();
			return host == null ? "(none)" : host;
		} catch( Exception exception ) {
			return "(none)";
		}
	}

	public int getServerPort() {
		try {
			Connection connection = transport.getWrappedTransport().getConnection();
			return connection.getPort();
		} catch( Exception exception ) {
			return 0;
		}
	}

	public String getShortUsername() {
		return withoutDomain( getUsername() );
	}

	public Transport getTransport() {
		return transport;
	}

	public String getUsername() {
		return withoutResource( getFullUsername() );
	}

	public void isAccessible( final String username, final OnObjectReceiveListener<Boolean> listener ) {

		if( listener == null ) {
			return;
		}

		if( username == null ) {
			listener.onObjectReceived( Boolean.valueOf( false ) );
			return;
		}

		final Connection connection = transport.getWrappedTransport().getConnection();

		new Thread() {

			@Override
			public void run() {

				final IQ query = new PrivacyPrivilegesQuery( username.toLowerCase( Locale.ENGLISH ) );
				query.setTo( connection.getServiceName() );
				PacketCollector result = connection.createPacketCollector( new PacketIDFilter( query.getPacketID() ) );

				if( !( connection.isConnected() && connection.isAuthenticated() ) ) {
					listener.onObjectReceived( Boolean.valueOf( false ) );
					return;
				}

				connection.sendPacket( query );
				Packet packet = result.nextResult(/* timeout? */);

				if( packet instanceof PrivacyPrivileges ) {

					PrivacyPrivileges privileges = (PrivacyPrivileges) packet;

					if( privileges.contains( "presence" ) ) {
						listener.onObjectReceived( Boolean.valueOf( true ) );
						return;
					}

					listener.onObjectReceived( Boolean.valueOf( false ) );
					return;

				}

				listener.onObjectReceived( Boolean.valueOf( false ) );
				return;

			}

		}.start();

	}

	public boolean isOnline() {
		Connection connection = transport.getWrappedTransport().getConnection();
		return connection.getHost() != null && connection.isConnected();
	}

	public boolean isSelf( String username ) {
		return getShortUsername().equals( username ) || getUsername().equals( username );
	}

	private void listenForConnectionEvents( Connection connection ) {

		if( !connection.isConnected() ) {
			getLog().info( "Can't listen for connection events while disconnected." );
			return;
		}

		connection.removeConnectionListener( this );
		connection.addConnectionListener( this );

	}

	public void logout() {
		transport.getWrappedTransport().stopListening();
		transport.getWrappedTransport().getConnection().disconnect();
		cancelPing();
	}

	public void onDestroy() {
		getLog().onDestroy();
		cancelPing();
	}

	public void ping() {
		schedulePing();
		ping.run();
	}

	@Override
	public void reconnectingIn( int n ) {
		// TODO Auto-generated method stub
	}

	@Override
	public void reconnectionFailed( Exception exception ) {
		// TODO Auto-generated method stub
	}

	@Override
	public void reconnectionSuccessful() {
		schedulePing();
	}

	public void registerPushNotificationToken( String packageName, String token ) {
		registerPushNotificationToken( packageName, token, GCMRegistration.ACTION_REGISTER );
	}

	private void registerPushNotificationToken( final String packageName, final String token, final String action ) {

		final Connection connection = transport.getWrappedTransport().getConnection();

		new Thread() {

			@Override
			public void run() {
				IQ iq = new GCMRegistration( packageName, token, action );
				iq.setTo( connection.getServiceName() );
				iq.setType( Type.SET );
				connection.sendPacket( iq );
			}

		}.start();

	}

	public void removeListener( TransportListener listener ) {
		listeners.remove( listener );
	}

	public void removeOfflineMessage( String remoteUserID, String messageID ) {
		Connection connection = transport.getWrappedTransport().getConnection();
		IQ iq = new RemoveOfflineMessage( remoteUserID, messageID );
		iq.setTo( connection.getServiceName() );
		iq.setType( Type.SET );
		sendPacket( iq );
	}

	private void schedulePing() {
		if( pingInterval <= 0 ) {
			return;
		}
		cancelPing();
		ping = new PingTask();
		timer.schedule( ping, pingInterval, pingInterval );
	}

	public void sendEncryptedMessage( String id, String to, String ciphertext, boolean notifiable, boolean badgeworthy ) {
		try {
			transport.send( id, to, ciphertext, notifiable, badgeworthy );
		} catch( NotConnectedException exception ) {
			adviseReconnect( exception.getConnection() );
		} catch( NotAuthenticatedException exception ) {
			adviseReconnect( exception.getConnection() );
		}
	}

	public void sendPacket( final IQ iq ) {
		if( iq == null ) {
			return;
		}
		new Thread() {

			@Override
			public void run() {
				try {
					getConnection().sendPacket( iq );
				} catch( Throwable exception ) {
					getLog().warn( exception, "#sendPacket type:%s id:%s", iq.getClass().getSimpleName(), iq.getPacketID() );
				}
			}

		}.start();
	}

	public void setLog( Log log ) {
		this.log = log;
	}

	public void setOnDisconnectedListener( OnDisconnectedListener onDisconnectedListener ) {
		this.onDisconnectedListener = onDisconnectedListener;
	}

	public void setResourceName( String resourceName ) {
		String previousResourceName = this.resourceName;
		this.resourceName = sanitizeResourceName( resourceName, getDefaultResourceName() );
		if( !previousResourceName.equals( resourceName ) ) {
			logout();
			adviseReconnect();
		}
	}

	public void unregisterPushNotificationToken( String packageName, String token ) {
		registerPushNotificationToken( packageName, token, GCMRegistration.ACTION_DEREGISTER );
	}

}
