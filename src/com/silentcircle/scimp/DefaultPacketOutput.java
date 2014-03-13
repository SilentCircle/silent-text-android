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
package com.silentcircle.scimp;

import java.lang.ref.SoftReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.Message.Type;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;

import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.NativeBridge;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.JabberClient;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.SCIMPError;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.WarningEvent;
import com.silentcircle.silenttext.model.siren.SirenMessage;
import com.silentcircle.silenttext.receiver.Receiver;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;
import com.silentcircle.silenttext.transport.TransportQueue;

public class DefaultPacketOutput implements PacketOutput {

	private static final int RETRY_ATTEMPTS = 2;

	private static void flushErrors( EventRepository events ) {
		if( events == null || !events.exists() ) {
			return;
		}
		List<Event> history = events.list();
		for( int i = 0; i < history.size(); i++ ) {
			Event event = history.get( i );
			if( event instanceof ErrorEvent || event instanceof WarningEvent ) {
				events.remove( event );
			}
		}
	}

	private static String nextPacketID( String remoteUserID ) {
		return new org.jivesoftware.smack.packet.Message( remoteUserID, Type.chat ).getPacketID();
	}

	private static String requestResend( String packetID ) {
		try {
			JSONObject json = new JSONObject();
			json.put( "request_resend", packetID );
			return json.toString();
		} catch( JSONException impossible ) {
			return null;
		}
	}

	private final SoftReference<Context> contextReference;

	protected static final Log LOG = new Log( "SCimp" );

	private static final SimpleDateFormat ISO8601 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );

	static {
		ISO8601.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	public DefaultPacketOutput( Context context ) {
		contextReference = new SoftReference<Context>( context );
	}

	private boolean consumePacket( String packetID, String remoteUserID, String data ) {

		try {

			JSONObject siren = new JSONObject( data );

			Conversation conversation = getConversations().findByPartner( remoteUserID );

			if( siren.has( "request_burn" ) ) {
				EventRepository events = getConversations().historyOf( conversation );
				Event event = events.findById( siren.getString( "request_burn" ) );
				if( event == null ) {
					events.remove( events.findById( packetID ) );
					return true;
				}
				events.remove( event );
				events.remove( events.findById( packetID ) );
				if( event.getId().equals( conversation.getPreviewEventID() ) ) {
					if( event instanceof IncomingMessage ) {
						if( MessageState.DECRYPTED.equals( ( (IncomingMessage) event ).getState() ) ) {
							conversation.offsetUnreadMessageCount( -1 );
						}
					}
					List<Event> history = events.list();
					if( history.size() > 0 ) {
						conversation.setPreviewEventID( history.get( history.size() - 1 ).getId() );
					} else {
						conversation.setPreviewEventID( null );
					}
					getConversations().save( conversation );
				}
				return true;
			}

			if( siren.has( "request_resend" ) ) {
				EventRepository events = getConversations().historyOf( conversation );
				Event event = events.findById( siren.getString( "request_resend" ) );
				if( event != null ) {
					if( event instanceof OutgoingMessage ) {
						OutgoingMessage message = (OutgoingMessage) event;
						message.setState( MessageState.RESEND_REQUESTED );
						events.save( message );
					}

				}
				events.remove( events.findById( packetID ) );
				return true;
			}

			if( siren.has( "received_id" ) ) {
				EventRepository events = getConversations().historyOf( conversation );
				Event event = events.findById( siren.getString( "received_id" ) );
				if( event instanceof OutgoingMessage ) {
					OutgoingMessage message = (OutgoingMessage) event;
					message.setState( MessageState.DELIVERED );
					if( siren.has( "received_time" ) ) {
						try {
							String receivedTime = siren.getString( "received_time" );
							long time = ISO8601.parse( receivedTime ).getTime();
							if( time > message.getTime() ) {
								message.setTime( time );
							}
						} catch( ParseException exception ) {
							message.setTime( System.currentTimeMillis() );
						}
					} else {
						message.setTime( System.currentTimeMillis() );
					}
					events.save( message );
				}
				return true;
			}

		} catch( JSONException exception ) {
			// Fine.
		}

		return false;

	}

	private void flush( String remoteUserID, EventRepository events, String exceptPacketID ) {
		for( Event event : events.list() ) {
			if( event.getId().equals( exceptPacketID ) ) {
				continue;
			}
			if( event instanceof OutgoingMessage ) {
				OutgoingMessage message = (OutgoingMessage) event;
				if( MessageState.COMPOSED.equals( message.getState() ) && message.hasText() ) {
					transition( remoteUserID, message.getId() );
				}
			}
		}
	}

	private SilentTextApplication getApplication() {
		return (SilentTextApplication) getContext().getApplicationContext();
	}

	private Context getContext() {
		return contextReference.get();
	}

	private ConversationRepository getConversations() {
		return getApplication().getConversations();
	}

	private JabberClient getJabber() {
		return getApplication().getJabber();
	}

	private NativeBridge getNative() {
		return getApplication().getNative();
	}

	private TransportQueue getOutgoingMessageQueue() {
		return getApplication().getOutgoingMessageQueue();
	}

	private void handleError( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int errorCode ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( remoteUserID );
		if( conversation != null ) {
			conversation.offsetFailures( 1 );
			conversations.save( conversation );
			ResourceStateRepository states = conversations.contextOf( conversation );
			ResourceState state = states.findById( conversation.getPartner().getDevice() );
			switch( SCIMPError.forValue( errorCode ) ) {
				case CORRUPT_DATA:
					// Our storage key could not decrypt the context. Let's just use the new storage
					// key create a new context and begin the keying process.
					states.remove( state );
					if( conversation.getFailures() <= RETRY_ATTEMPTS ) {
						getNative().getInput().connect( storageKey, null, localUserID, remoteUserID, null );
					}
					break;
				case KEY_NOT_FOUND:
					// We got a message we couldn't decrypt. Let's hold onto the message ID and
					// queue up a resend request for after we have established keys.
					if( packetID != null ) {
						EventRepository events = conversations.historyOf( conversation );
						Event event = events.findById( packetID );
						if( event == null ) {
							event = new OutgoingMessage( localUserID, requestResend( packetID ) );
							event.setConversationID( remoteUserID );
							events.save( event );
						}
					}
					if( conversation.getFailures() <= RETRY_ATTEMPTS ) {
						getNative().getInput().connect( storageKey, null, localUserID, remoteUserID, state == null ? null : state.getState() );
					}
					break;
				default:
					// Something else happened that we didn't expect. Since this is an error, let's
					// drop the secure context.
					states.remove( state );
					break;
			}
		}
		invalidate( remoteUserID );
	}

	/**
	 * @param storageKey
	 * @param packetID
	 * @param localUserID
	 * @param remoteUserID
	 * @param warningCode
	 */
	private void handleWarning( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int warningCode ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( remoteUserID );
		if( conversation != null ) {
			conversation.offsetFailures( 1 );
			conversations.save( conversation );
			ResourceStateRepository states = conversations.contextOf( conversation );
			ResourceState state = states.findById( conversation.getPartner().getDevice() );
			switch( SCIMPError.forValue( warningCode ) ) {
				case SECRETS_MISMATCH:
					// We had already established a secure conversation, and we're still secure, but
					// our SAS phrase has changed.
					// TODO: Exactly how does this happen, and is it normal?
					states.setVerified( state, false );
					break;
				default:
					// Something else happened that we didn't expect. Just drop the packet without
					// affecting the existing context.
					break;
			}
		}
		invalidate( remoteUserID );
	}

	private void invalidate( String remoteUserID ) {
		Intent intent = Action.UPDATE_CONVERSATION.intent();
		Extra.PARTNER.to( intent, remoteUserID );
		getContext().sendBroadcast( intent, Manifest.permission.READ );
	}

	@Override
	public void onConnect( byte [] storageKey, String packetID, String localUserID, String remoteUserID, String context, String secret ) {
		LOG.debug( "CONNECT id:%s to:%s context:%s %s", packetID, remoteUserID, Hash.sha1( context ), secret );
		processPacket( remoteUserID, context, null, secret, null );
		invalidate( remoteUserID );
	}

	@Override
	public void onError( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int errorCode ) {
		LOG.error( "ERROR id:%s user:%s %s", packetID, remoteUserID, SCIMPError.forValue( errorCode ) );
		Event event = new ErrorEvent( errorCode );
		event.setConversationID( remoteUserID );
		save( remoteUserID, event );
		handleError( storageKey, packetID, localUserID, remoteUserID, errorCode );
	}

	@Override
	public void onReceivePacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, final String data, String context, String secret, boolean notifiable, boolean badgeworthy ) {
		LOG.debug( "RECV id:%s from:%s notifiable:%b badgeworthy:%b context:%s\n%s", packetID, remoteUserID, Boolean.valueOf( notifiable ), Boolean.valueOf( badgeworthy ), Hash.sha1( context ), data );

		if( !consumePacket( packetID, remoteUserID, data ) ) {

			processPacket( remoteUserID, context, packetID, secret, new Receiver<Message>() {

				@Override
				public void onReceive( Message message ) {
					message.setText( data );
					message.setState( MessageState.DECRYPTED );
					try {
						SirenMessage siren = new SirenMessage( data );
						int secondsToLive = siren.getShredAfter();
						if( secondsToLive > 0 ) {
							message.setBurnNotice( secondsToLive );
						}
					} catch( JSONException ignore ) {
						// Don't worry about it.
					}
				}

			} );

		}

		invalidate( remoteUserID );

	}

	@Override
	public void onSendPacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, final String data, String context, String secret, boolean notifiable, boolean badgeworthy ) {
		LOG.debug( "SEND id:%s to:%s notifiable:%b badgeworthy:%b context:%s\n%s", packetID, remoteUserID, Boolean.valueOf( notifiable ), Boolean.valueOf( badgeworthy ), Hash.sha1( context ), data );

		processPacket( remoteUserID, context, packetID, secret, new Receiver<Message>() {

			@Override
			public void onReceive( Message message ) {
				message.setCiphertext( data );
				message.setState( MessageState.ENCRYPTED );
			}

		} );

		JabberClient xmpp = getJabber();
		if( xmpp != null ) {
			xmpp.sendEncryptedMessage( packetID == null ? nextPacketID( remoteUserID ) : packetID, remoteUserID, data, notifiable, badgeworthy );
		} else {
			getOutgoingMessageQueue().add( packetID == null ? nextPacketID( remoteUserID ) : packetID, remoteUserID, data, notifiable, badgeworthy );
		}

		invalidate( remoteUserID );

	}

	@Override
	public void onStateTransition( byte [] storageKey, String localUserID, String remoteUserID, int toState ) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onWarning( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int warningCode ) {
		LOG.warn( "WARN id:%s user:%s %s", packetID, remoteUserID, SCIMPError.forValue( warningCode ) );
		if( !SCIMPError.SECRETS_MISMATCH.equals( SCIMPError.forValue( warningCode ) ) ) {
			Event event = new WarningEvent( warningCode );
			event.setConversationID( remoteUserID );
			save( remoteUserID, event );
		}
		handleWarning( storageKey, packetID, localUserID, remoteUserID, warningCode );
	}

	private void processPacket( String remoteUserID, String context, String packetID, String secret, Receiver<Message> receiver ) {

		ConversationRepository conversations = getConversations();
		if( conversations == null || !conversations.exists() ) {
			return;
		}
		Conversation conversation = conversations.findByPartner( remoteUserID );

		if( conversation == null ) {
			return;
		}

		ResourceStateRepository states = conversations.contextOf( conversation );
		String resource = conversation.getPartner().getDevice();
		if( resource == null ) {
			resource = "";
		}
		ResourceState state = states.findById( resource );
		if( state == null ) {
			state = new ResourceState();
			state.setResource( resource );
		}
		String previousVerifyCode = state.getVerifyCode();
		state.setState( context );
		state.setVerifyCode( secret );
		states.save( state );

		if( packetID != null ) {
			conversation.setPreviewEventID( packetID );
		}

		EventRepository events = conversations.historyOf( conversation );

		if( state.isSecure() ) {
			conversation.setFailures( 0 );
			if( secret != null && !secret.equals( previousVerifyCode ) ) {
				// Record this: we have just secured the conversation.
				flushErrors( events );
			}
			flush( remoteUserID, events, packetID );
		}

		if( packetID == null ) {
			conversation.setLastModified( System.currentTimeMillis() );
			conversations.save( conversation );
			return;
		}

		if( receiver != null ) {

			Event event = events.findById( packetID );

			if( event == null ) {
				event = new IncomingMessage( remoteUserID, packetID, null );
				event.setConversationID( remoteUserID );
			}

			if( event instanceof Message ) {
				Message message = (Message) event;
				message.setTime( System.currentTimeMillis() );
				receiver.onReceive( message );
				if( MessageState.DECRYPTED.equals( message.getState() ) ) {
					conversation.offsetUnreadMessageCount( 1 );
				}
				events.save( message );
			}

		}

		conversation.setLastModified( System.currentTimeMillis() );
		conversations.save( conversation );
		transition( remoteUserID, packetID );

	}

	private void save( String remoteUserID, Event event ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( remoteUserID );
		if( conversation != null ) {
			conversations.historyOf( conversation ).save( event );
		}
	}

	private void transition( String remoteUserID, String packetID ) {
		Intent intent = Action.TRANSITION.intent();
		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );
		getContext().sendBroadcast( intent, Manifest.permission.READ );
	}

}
