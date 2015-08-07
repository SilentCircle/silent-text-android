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
package com.silentcircle.scimp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;

import com.silentcircle.api.model.Key;
import com.silentcircle.api.model.Signature;
import com.silentcircle.api.model.User;
import com.silentcircle.api.web.model.BasicSignature;
import com.silentcircle.api.web.model.json.JSONObjectWriter;
import com.silentcircle.http.client.CachingHTTPClient;
import com.silentcircle.http.client.URLBuilder;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.scloud.NativePacket;
import com.silentcircle.scloud.listener.OnBlockDecryptedListener;
import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silentstorage.repository.file.RepositoryLockedException;
import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.SCimpBridge;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Attachment;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.SCIMPError;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.WarningEvent;
import com.silentcircle.silenttext.model.util.SirenUtils;
import com.silentcircle.silenttext.receiver.Receiver;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;
import com.silentcircle.silenttext.task.GetUserFromServerTask;
import com.silentcircle.silenttext.task.RequestResendTask;
import com.silentcircle.silenttext.transport.Envelope;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.KeyUtils;
import com.silentcircle.silenttext.util.StringUtils;

public class DefaultPacketOutput implements PacketOutput {

	static class OnMessageDecryptedReceiver implements Receiver<Message> {

		private final SoftReference<Context> contextReference;
		private final String data;

		OnMessageDecryptedReceiver( Context context, String data ) {
			contextReference = new SoftReference<Context>( context );
			this.data = data;
		}

		private Context getContext() {
			return contextReference.get();
		}

		@Override
		public void onReceive( Message message ) {

			message.setText( data );
			message.setState( MessageState.DECRYPTED );

			Siren siren = message.getSiren();

			if( siren != null ) {

				prepareAttachments( siren );

				int secondsToLive = siren.getShredAfter();

				if( secondsToLive > 0 ) {
					message.setBurnNotice( secondsToLive );
				}

			}

		}

		private void prepareAttachments( Siren siren ) {

			Context context = getContext();
			byte [] locator = siren.getCloudLocatorAsByteArray();
			byte [] key = siren.getCloudKeyAsByteArray();

			if( context == null || locator == null || key == null ) {
				return;
			}

			String url = new URLBuilder( ServiceConfiguration.getInstance().scloud.url ).component( new String( locator ) ).build().toString();
			SilentTextApplication application = SilentTextApplication.from( context );

			InputStream response = new CachingHTTPClient( new ApacheHTTPClient( new HttpClient() ), application.getHTTPResponseCache() ).get( url );

			NativePacket decryptor = new NativePacket();

			final ByteArrayOutputStream out = new ByteArrayOutputStream();

			decryptor.setOnBlockDecryptedListener( new OnBlockDecryptedListener() {

				@Override
				public void onBlockDecrypted( byte [] data, byte [] metaData ) {
					try {
						out.write( metaData );
					} catch( IOException exception ) {
						// Ignore.
					}
				}

			} );

			decryptor.decrypt( IOUtils.readFully( response ), new String( key ) );

			Attachment attachment = new Attachment();

			attachment.setLocator( locator );
			attachment.setKey( key );
			attachment.setType( siren.getMIMETypeAsByteArray() );

			try {

				JSONObject metaData = new JSONObject( out.toString() );

				if( metaData.has( "FileName" ) ) {
					attachment.setName( StringUtils.toByteArray( metaData.getString( "FileName" ) ) );
				}

				if( attachment.getType() == null && metaData.has( "MediaType" ) ) {
					attachment.setType( StringUtils.toByteArray( metaData.getString( "MediaType" ) ) );
				}

				if( metaData.has( "FileSize" ) ) {
					attachment.setSize( metaData.getLong( "FileSize" ) );
				}

			} catch( JSONException exception ) {
				// Ignore.
			}

			application.getAttachments().save( attachment );

		}

	}

	static class OnMessageEncryptedReceiver implements Receiver<Message> {

		private final String data;

		OnMessageEncryptedReceiver( String data ) {
			this.data = data;
		}

		@Override
		public void onReceive( Message message ) {
			message.setCiphertext( data );
			message.setState( MessageState.ENCRYPTED );
		}

	}

	private static final int RETRY_ATTEMPTS = 5;

	// from SCimp.h
	private static int kSCimpState_Init = 0;

	private static int kSCimpState_Ready = 1;

	private static int kSCimpState_Error = 2;

	private static String createID( String key ) {
		return String.format( "%s:%d", key, Long.valueOf( System.currentTimeMillis() ) );
	}

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

	private static Collection<OutgoingMessage> getPendingOutgoingMessages( EventRepository events, String exceptPacketID ) {
		Collection<OutgoingMessage> pending = new ArrayList<OutgoingMessage>();
		for( Event event : events.list() ) {
			if( event.getId().equals( exceptPacketID ) ) {
				continue;
			}
			if( event instanceof OutgoingMessage ) {
				OutgoingMessage message = (OutgoingMessage) event;
				if( MessageState.COMPOSED.equals( message.getState() ) && message.hasText() ) {
					pending.add( message );
				}
			}
		}
		return pending;
	}

	static boolean isKeyingInProgress( int state ) {
		if( state == kSCimpState_Init || state == kSCimpState_Ready || state == kSCimpState_Error ) {
			return false;
		}
		return true;
	}

	private static void markPacketAsDelivered( EventRepository events, String deliveredPacketID, long deliveryTime ) {
		Event event = events.findById( deliveredPacketID );
		if( event instanceof OutgoingMessage ) {
			OutgoingMessage message = (OutgoingMessage) event;
			message.setState( MessageState.DELIVERED );
			message.setDeliveryTime( deliveryTime );
			events.save( message );
		}
	}

	public static String requestResend( Context context, String packetID ) {
		if( packetID == null ) {
			return null;
		}

		JSONObject resendJSON = new JSONObject();

		SilentTextApplication application = SilentTextApplication.from( context );

		Repository<NamedKeyPair> namedKeyPairRepository = application.getNamedKeyPairs();

		if( namedKeyPairRepository != null ) {
			NamedKeyPair userKeyPair = namedKeyPairRepository.list().fetchAll().get( 0 );

			if( userKeyPair != null ) {
				Key userPublicKey = KeyUtils.extractPublicKey( userKeyPair );

				if( userPublicKey != null ) {
					List<Signature> userKeySignatures = userPublicKey.getSignatures();

					if( userKeySignatures != null && userKeySignatures.size() > 0 ) {
						Signature userKeySignature = userKeySignatures.get( 0 );

						ByteArrayOutputStream out = new ByteArrayOutputStream();

						try {
							JSONObjectWriter.writeSignature( (BasicSignature) userKeySignature, new DataOutputStream( out ) );

							if( out.size() != 0 ) {
								resendJSON.put( "siren_sig_v2", out.toString() );
							}
						} catch( IOException e ) {
							// Fall through.
						} catch( JSONException e ) {
							// Fall through.
						}
					}
				}
			}
		}

		try {
			resendJSON.put( "request_resend", packetID );

			return resendJSON.toString();
		} catch( JSONException impossible ) {
			return null;
		}
	}

	private static String unwrapSCimpPacket( String encoded ) {
		return StringUtils.fromByteArray( Base64.decodeBase64( encoded.replaceAll( "^\\?SCIMP:(.+)\\.$", "$1" ) ) );
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

	private void burnPacket( Conversation conversation, String remoteUserID, String burnedPacketID ) {

		ConversationRepository conversations = getConversations();
		EventRepository events = conversations.historyOf( conversation );
		Event event = events.findById( burnedPacketID );

		if( event == null ) {
			return;
		}

		if( !( event instanceof IncomingMessage ) ) {
			return;
		}

		IncomingMessage message = (IncomingMessage) event;

		if( !remoteUserID.equals( message.getSender() ) ) {
			return;
		}

		events.remove( event );
		getApplication().removeAttachments( event );

		if( event.getId().equals( conversation.getPreviewEventID() ) ) {

			if( event instanceof IncomingMessage && MessageState.DECRYPTED.equals( ( (IncomingMessage) event ).getState() ) ) {
				conversation.offsetUnreadMessageCount( -1 );
			}

			List<Event> history = events.list();
			int count = history.size();

			if( count > 0 ) {
				conversation.setPreviewEventID( history.get( count - 1 ).getId() );
			} else {
				conversation.setPreviewEventID( (byte []) null );
			}

			conversations.save( conversation );

		}

	}

	private boolean consumePacket( String remoteUserID, String data ) {

		Siren siren = SirenUtils.parse( data );

		if( siren.isPing() ) {
			// Ignore this for now.
			return true;
		}

		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( remoteUserID );

		if( siren.isBurnRequest() ) {
			burnPacket( conversation, remoteUserID, siren.getRequestBurn() );
			return true;
		}

		EventRepository events = conversations.historyOf( conversation );

		if( siren.isResendRequest() ) {
			resendPacket( events, siren.getRequestResend() );
			return true;
		}

		if( siren.isAcknowledgment() ) {
			markPacketAsDelivered( events, siren.getAcknowledgment(), siren.getDeliveryTimestamp() );
			return true;
		}

		return false;

	}

	private Conversation findConversation( String remoteUserID ) {

		ConversationRepository conversations = getConversations();

		if( conversations == null || !conversations.exists() ) {
			return null;
		}

		return conversations.findByPartner( remoteUserID );

	}

	private EventRepository findConversationHistory( String remoteUserID ) {
		ConversationRepository conversations = getConversations();
		if( conversations != null ) {
			Conversation conversation = conversations.findByPartner( remoteUserID );
			if( conversation != null ) {
				return conversations.historyOf( conversation );
			}
		}
		return null;
	}

	private ResourceState findResourceState( Conversation conversation ) {

		ConversationRepository conversations = getConversations();

		if( conversations == null || !conversations.exists() ) {
			return null;
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

		return state;

	}

	private void flush( String remoteUserID, Collection<OutgoingMessage> messages ) {
		if( messages != null ) {
			for( OutgoingMessage message : messages ) {
				getNative().encrypt( remoteUserID, message.getId(), message.getText(), true, true );
			}
		}
	}

	SilentTextApplication getApplication() {
		return (SilentTextApplication) getContext().getApplicationContext();
	}

	private Context getContext() {
		return contextReference.get();
	}

	private ConversationRepository getConversations() {
		return getApplication().getConversations();
	}

	private SCimpBridge getNative() {
		return getApplication().getSCimpBridge();
	}

	PacketInput getNativeInput() {
		return getNative().getInput();
	}

	private TransportQueue getOutgoingMessageQueue() {
		return getApplication().getOutgoingMessageQueue();
	}

	private void handleError( final byte [] storageKey, final String packetID, final String localUserID, final String remoteUserID, int errorCode, final int state ) {

		final ConversationRepository conversations = getConversations();
		final Conversation conversation = conversations.findByPartner( remoteUserID );

		if( conversation != null ) {

			conversation.offsetFailures( 1 );
			SCIMPError error = SCIMPError.forValue( errorCode );

			if( conversation.getFailures() > RETRY_ATTEMPTS ) {
				LOG.error( "#onError error:%s failures:%d max_retries:%d (retry limit exceeded)", error, Integer.valueOf( conversation.getFailures() ), Integer.valueOf( RETRY_ATTEMPTS ) );
			}

			conversations.save( conversation );
			final ResourceStateRepository states = conversations.contextOf( conversation );
			final ResourceState resourceState = states.findById( conversation.getPartner().getDevice() );

			switch( error ) {

				case CORRUPT_DATA:

					// Refresh user info.

					AsyncUtils.execute( new GetUserFromServerTask( getApplication(), false ) {

						@Override
						protected void onPostExecute( User user ) {
							// We got a message we couldn't decrypt. Let's hold onto the message ID
							// and
							// queue up a resend request for after we have established keys.

							AsyncUtils.execute( new RequestResendTask( getApplication(), packetID, localUserID, remoteUserID ) {

								@Override
								protected void onPostExecute( Void result ) {

									// Our storage key could not decrypt the
									// context. Let's just use
									// the new storage
									// key create a new context and begin the keying
									// process.

									states.remove( resourceState );

									if( conversation.getFailures() <= RETRY_ATTEMPTS && !isKeyingInProgress( state ) ) {
										// attempt to reconnect if not already
										// reconnecting
										getNativeInput().connect( storageKey, null, localUserID, remoteUserID, null );
									}
								}

							} );
						}

					}, remoteUserID );

					break;

				case KEY_NOT_FOUND:

					// Refresh user info.

					AsyncUtils.execute( new GetUserFromServerTask( getApplication(), false ) {

						@Override
						protected void onPostExecute( User user ) {
							AsyncUtils.execute( new RequestResendTask( getApplication(), packetID, localUserID, remoteUserID ) {

								@Override
								protected void onPostExecute( Void result ) {
									if( conversation.getFailures() <= RETRY_ATTEMPTS && !isKeyingInProgress( state ) ) {
										// attempt to reconnect if not already reconnecting
										getNativeInput().connect( storageKey, null, localUserID, remoteUserID, resourceState == null ? null : resourceState.getState() );
									}
								}

							} );
						}

					}, remoteUserID );

					break;

				case PROTOCOL_ERROR:
				case BAD_INTEGRITY:
					// Refresh user info.

					AsyncUtils.execute( new GetUserFromServerTask( getApplication(), false ) {

						@Override
						protected void onPostExecute( User user ) {
							// We got a message we couldn't decrypt. Let's hold onto the message ID
							// and
							// queue up a resend request for after we have established keys.

							AsyncUtils.execute( new RequestResendTask( getApplication(), packetID, localUserID, remoteUserID ) {

								@Override
								protected void onPostExecute( Void result ) {

									// Our storage key could not decrypt the
									// context. Let's just use
									// the new storage
									// key create a new context and begin the keying
									// process.

									states.remove( resourceState );

									if( conversation.getFailures() <= RETRY_ATTEMPTS && !isKeyingInProgress( state ) ) {
										// attempt to reconnect if not already
										// reconnecting
										getNativeInput().connect( storageKey, null, localUserID, remoteUserID, null );
									}
								}

							} );
						}

					}, remoteUserID );
					break;

				case NOT_CONNECTED:
					AsyncUtils.execute( new GetUserFromServerTask( getApplication(), false ) {

						@Override
						protected void onPostExecute( User user ) {
							if( conversation.getFailures() <= RETRY_ATTEMPTS && !isKeyingInProgress( state ) ) {
								// attempt to reconnect if not already reconnecting, with packet
								// encryption
								// following through automatically
								getNativeInput().connect( storageKey, null, localUserID, remoteUserID, resourceState == null ? null : resourceState.getState() );
							} else if( conversation.getFailures() > RETRY_ATTEMPTS ) {
								states.remove( resourceState );

								getNativeInput().connect( storageKey, null, localUserID, remoteUserID, null );
							}
						}

					}, remoteUserID );
					break;

				default:
					// Something else happened that we didn't expect. Since this is an error, let's
					// drop the secure context.
					states.remove( resourceState );
					break;
			}

		}

		if( ServiceConfiguration.getInstance().debug ) {
			saveError( errorCode, remoteUserID );
		}

		invalidate( remoteUserID );

	}

	/**
	 * @param storageKey
	 * @param packetID
	 * @param localUserID
	 * @param remoteUserID
	 * @param warningCode
	 * @param state
	 */
	private void handleWarning( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int warningCode, int state ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( remoteUserID );
		if( conversation != null ) {
			conversation.offsetFailures( 1 );
			conversations.save( conversation );
			ResourceStateRepository states = conversations.contextOf( conversation );
			ResourceState resourceState = states.findById( conversation.getPartner().getDevice() );
			switch( SCIMPError.forValue( warningCode ) ) {
				case SECRETS_MISMATCH:
					// We had already established a secure conversation, and we're still secure, but
					// our SAS phrase has changed.
					// TODO: Exactly how does this happen, and is it normal?
					states.setVerified( resourceState, false );
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
	public void onError( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int errorCode, int state ) {
		LOG.error( "ERROR id:%s user:%s %s", packetID, remoteUserID, SCIMPError.forValue( errorCode ) );
		handleError( storageKey, packetID, localUserID, remoteUserID, errorCode, state );
	}

	@Override
	public void onReceivePacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, byte [] dataBytes, String context, String secret, boolean notifiable, boolean badgeworthy ) {

		final String data = StringUtils.fromByteArray( dataBytes );
		LOG.debug( "RECV id:%s from:%s notifiable:%b badgeworthy:%b context:%s\n%s", packetID, remoteUserID, Boolean.valueOf( notifiable ), Boolean.valueOf( badgeworthy ), Hash.sha1( context ), data );

		if( consumePacket( remoteUserID, data ) ) {
			saveState( remoteUserID, context, secret );
			savePacket( remoteUserID, packetID, data, MessageState.DECRYPTED );
		} else {
			processPacket( remoteUserID, context, packetID, secret, new OnMessageDecryptedReceiver( getContext(), data ) );
		}

		invalidate( remoteUserID );

	}

	@Override
	public void onSendPacket( byte [] storageKey, String packetID, String localUserID, String remoteUserID, byte [] dataBytes, String context, String secret, boolean notifiable, boolean badgeworthy ) {

		final String data = StringUtils.fromByteArray( dataBytes );

		LOG.debug( "SEND id:%s to:%s notifiable:%b badgeworthy:%b context:%s\n%s\n%s", packetID, remoteUserID, Boolean.valueOf( notifiable ), Boolean.valueOf( badgeworthy ), Hash.sha1( context ), data, unwrapSCimpPacket( data ) );

		processPacket( remoteUserID, context, packetID, secret, new OnMessageEncryptedReceiver( data ) );

		Envelope envelope = new Envelope();

		envelope.time = System.currentTimeMillis();
		envelope.id = packetID == null ? UUID.randomUUID().toString() : packetID;
		envelope.from = localUserID;
		envelope.to = remoteUserID;
		envelope.content = data;
		envelope.notifiable = notifiable;
		envelope.badgeworthy = badgeworthy;
		envelope.state = Envelope.State.PENDING;

		try {
			getOutgoingMessageQueue().add( envelope );
		} catch( RepositoryLockedException exception ) {
			LOG.warn( exception, "#onSendPacket id:%s to:%s", envelope.id, envelope.to );
		}

		invalidate( remoteUserID );

	}

	@Override
	public void onStateTransition( byte [] storageKey, String localUserID, String remoteUserID, int toState ) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onWarning( byte [] storageKey, String packetID, String localUserID, String remoteUserID, int warningCode, int state ) {
		LOG.warn( "WARN id:%s user:%s %s", packetID, remoteUserID, SCIMPError.forValue( warningCode ) );
		if( !SCIMPError.SECRETS_MISMATCH.equals( SCIMPError.forValue( warningCode ) ) && ServiceConfiguration.getInstance().debug ) {
			saveWarning( warningCode, remoteUserID );
		}
		handleWarning( storageKey, packetID, localUserID, remoteUserID, warningCode, state );
	}

	private void processPacket( String remoteUserID, String context, String packetID, String secret, Receiver<Message> receiver ) {

		Conversation conversation = findConversation( remoteUserID );

		if( conversation == null ) {
			return;
		}

		ResourceState state = findResourceState( conversation );
		String previousVerifyCode = state.getVerifyCode();

		saveState( conversation, context, secret );

		if( packetID != null ) {
			conversation.setPreviewEventID( packetID );
		}

		ConversationRepository conversations = getConversations();

		EventRepository events = conversations.historyOf( conversation );
		Collection<OutgoingMessage> pending = null;

		if( state.isSecure() ) {
			conversation.setFailures( 0 );
			if( secret != null && !secret.equals( previousVerifyCode ) ) {
				// Record this: we have just secured the conversation.
				flushErrors( events );
				pending = getPendingOutgoingMessages( events, packetID );
			}
		}

		if( packetID == null ) {
			conversation.setLastModified( System.currentTimeMillis() );
			conversations.save( conversation );
			flush( remoteUserID, pending );
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
				if( !SilentTextApplication.isOutgoingResendRequest( message ) ) {
					// EA: don't save resend requests because they stack up as blank views in the
					// conversation
					// FIXME: pull this condition out if we fix that, 'cuz they should get saved in
					// case we receive repeats later
					events.save( message );
				}
			}

		}

		conversation.setLastModified( System.currentTimeMillis() );
		conversations.save( conversation );
		transition( remoteUserID, packetID );
		flush( remoteUserID, pending );

	}

	private void resendPacket( EventRepository events, String requestedPacketID ) {

		Event event = events.findById( requestedPacketID );

		if( event instanceof OutgoingMessage ) {
			// STA-1023 - Bypass handling of this from the Activity to this class
			// OutgoingMessage message = (OutgoingMessage) event;
			// message.setState( MessageState.RESEND_REQUESTED );
			// events.save( message );

			OutgoingMessage message = (OutgoingMessage) event;

			String self = getApplication().getUsername();
			Conversation conversation = getApplication().getConversations().findById( message.getConversationID() );

			if( self != null && self.equals( conversation.getPartner().getUsername() ) ) {
				// Talking to self
				return;
			}

			// Purge current message event, set a new ID for the message, and push it out as a
			// composed message
			events.remove( message );
			message.setId( UUID.randomUUID().toString() );
			message.setState( MessageState.COMPOSED );
			events.save( message );
			transition( conversation.getPartner().getUsername(), event.getId() );
		}
	}

	private void save( String remoteUserID, Event event ) {
		ConversationRepository conversations = getConversations();
		if( conversations != null ) {
			Conversation conversation = conversations.findByPartner( remoteUserID );
			if( conversation != null ) {
				EventRepository events = conversations.historyOf( conversation );
				if( events != null ) {
					events.save( event );
				}
			}
		}
	}

	private void saveError( int code, String remoteUserID ) {
		saveErrorOrWarning( new ErrorEvent( code ), remoteUserID, "ERROR" );
	}

	private void saveErrorOrWarning( Event event, String remoteUserID, String identifierPrefix ) {

		event.setId( createID( identifierPrefix ) );
		event.setConversationID( remoteUserID );

		save( remoteUserID, event );

	}

	private void savePacket( String remoteUserID, String packetID, String data, MessageState decrypted ) {

		EventRepository events = findConversationHistory( remoteUserID );

		if( events != null ) {

			Event event = events.findById( packetID );

			if( event instanceof Message ) {

				Message message = (Message) event;

				message.setText( data );
				message.setState( decrypted );

				events.save( message );

			}

		}

	}

	private void saveState( Conversation conversation, String context, String secret ) {

		if( conversation == null ) {
			return;
		}

		ConversationRepository conversations = getConversations();

		if( conversations != null ) {

			if( conversations.exists() ) {

				ResourceStateRepository states = conversations.contextOf( conversation );

				if( states != null ) {

					ResourceState state = findResourceState( conversation );

					if( context != null ) {
						state.setState( context );
					} else {
						LOG.warn( "#saveState conversation_id:%s context:(null)", conversation.getId() );
					}

					state.setVerifyCode( secret );

					states.save( state );

				}

			}

		}

	}

	private void saveState( String remoteUserID, String context, String secret ) {
		Conversation conversation = findConversation( remoteUserID );
		if( conversation != null ) {
			saveState( conversation, context, secret );
		}
	}

	private void saveWarning( int code, String remoteUserID ) {
		saveErrorOrWarning( new WarningEvent( code ), remoteUserID, "WARN" );
	}

	private void transition( String remoteUserID, String packetID ) {

		Intent intent = Action.TRANSITION.intent();

		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );

		getContext().sendBroadcast( intent, Manifest.permission.READ );

	}

}
