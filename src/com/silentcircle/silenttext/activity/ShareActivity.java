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
package com.silentcircle.silenttext.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.dialog.ShareDialogFragment;
import com.silentcircle.silenttext.fragment.ContactListFragment;
import com.silentcircle.silenttext.location.LocationObserver;
import com.silentcircle.silenttext.location.LocationUtils;
import com.silentcircle.silenttext.location.OnLocationReceivedListener;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.json.util.JSONSirenDecorator;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.util.AttachmentUtils;

public class ShareActivity extends SilentActivity implements ContactListFragment.Callback {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

	private static String getPrintableHistory( List<Event> events ) {
		StringBuilder history = new StringBuilder();
		for( int i = 0; i < events.size(); i++ ) {
			Event event = events.get( i );
			if( event instanceof Message && ( ( (Message) event ).expires() || ( (Message) event ).getSiren() == null ) ) {
				continue;
			}
			history.append( "[" ).append( DATE_FORMAT.format( new Date( event.getTime() ) ) ).append( "] " );
			if( event instanceof Message ) {
				Message message = (Message) event;
				history.append( withoutDomain( message.getSender() ) ).append( ": " );
				try {
					JSONObject json = new JSONObject( message.getText() );
					if( json.has( "message" ) ) {
						history.append( json.getString( "message" ) );
					} else {
						history.append( "(attachment)" );
					}
				} catch( JSONException exception ) {
					history.append( message.getText() );
				}
			} else {
				history.append( event.getText() );
			}
			history.append( "\n" );
		}
		return history.toString();
	}

	private static String withoutDomain( String fullAddress ) {
		return fullAddress == null ? null : fullAddress.replaceAll( "^(.+)@(.+)$", "$1" );
	}

	private String savedId;

	private OutgoingMessage createOutgoingMessage( Conversation conversation, JSONObject siren, Location location ) {

		JSONSirenDecorator.decorateSirenForConversation( siren, conversation, location, true );

		OutgoingMessage message = new OutgoingMessage( getUsername(), siren.toString() );
		message.setId( UUID.randomUUID().toString() );

		return message;

	}

	private OutgoingMessage createOutgoingMessage( Conversation conversation, Location location ) {
		return createOutgoingMessage( conversation, new JSONObject(), location );
	}

	private OutgoingMessage createOutgoingMessage( Conversation conversation, String text, Uri uri, String mimeType, Location location ) {

		JSONObject siren = new JSONObject();

		JSONSirenDecorator.decorateSirenForAttachment( this, siren, uri, mimeType );
		JSONSirenDecorator.decorateSirenForConversation( siren, conversation, location, true );

		if( text != null ) {
			try {
				siren.put( "message", text );
			} catch( JSONException impossible ) {
				// Ignore.
			}
		}

		OutgoingMessage message = new OutgoingMessage( getUsername(), siren.toString() );
		message.setConversationID( conversation.getPartner().getUsername() );
		message.setId( UUID.randomUUID().toString() );

		if( conversation.hasBurnNotice() ) {
			message.setBurnNotice( conversation.getBurnDelay() );
		}

		return message;

	}

	private String getMIMEType( Intent intent ) {
		String mimeType = null;
		mimeType = intent.getType();
		if( mimeType != null ) {
			return mimeType;
		}
		mimeType = getContentResolver().getType( intent.getData() );
		if( mimeType != null ) {
			return mimeType;
		}
		String fileName = AttachmentUtils.getFileNameFromURI( this, intent.getData() );
		if( fileName == null ) {
			return null;
		}
		String extension = AttachmentUtils.getExtensionFromFileName( fileName );
		if( extension == null ) {
			return null;
		}
		mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension( extension );
		return mimeType;
	}

	@Override
	public void onBeginLoading() {
		beginLoading( R.id.content );
	}

	@Override
	public void onContactSelected( Contact contact ) {
		sendFile( contact.getUsername() );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.fragment );
		setTitle( R.string.send_to );
		findViewById( R.id.error ).setVisibility( View.GONE );

		Bundle b = getIntent().getExtras();
		if( b != null ) {
			savedId = b.getString( ShareDialogFragment.SAVED_ID );
		}

		FragmentManager manager = getFragmentManager();
		Fragment fragment = manager.findFragmentById( R.id.content );

		if( fragment == null ) {
			manager.beginTransaction().add( R.id.content, new ContactListFragment() ).commit();
		}

		if( getIntent() != null ) {
			setIntent( getIntent() );
		}

	}

	@Override
	public void onFinishLoading() {
		finishLoading( R.id.content );
	}

	@Override
	protected void onNewIntent( Intent intent ) {
		super.onNewIntent( intent );
		setIntent( intent );
	}

	@Override
	protected void onResume() {

		super.onResume();

		if( !isUnlocked() ) {
			requestUnlock();
			return;
		}

		if( !isActivated() ) {
			requestActivation();
			return;
		}

	}

	protected void save( Conversation conversation, OutgoingMessage message ) {
		if( message != null ) {
			ConversationRepository conversations = getConversations();
			if( conversations != null && conversation != null ) {
				EventRepository events = conversations.historyOf( conversation );
				if( events != null ) {
					events.save( message );
				}
			}
		}
	}

	protected void sendChatMessageAsAttachment( final Conversation conversation, final CharSequence text ) {

		if( conversation != null && LocationUtils.isLocationSharingAvailable( this ) && conversation.isLocationEnabled() ) {

			LocationObserver.observe( this, new OnLocationReceivedListener() {

				@Override
				public void onLocationReceived( Location location ) {
					sendChatMessageAsAttachment( conversation, text, location );
				}

				@Override
				public void onLocationUnavailable() {
					onLocationReceived( null );
				}

			} );

			return;

		}

		sendChatMessageAsAttachment( conversation, text, null );

	}

	public void sendChatMessageAsAttachment( Conversation conversation, CharSequence text, Location location ) {

		OutgoingMessage message = createOutgoingMessage( conversation, location );
		message.setState( MessageState.UNKNOWN );
		save( conversation, message );

		Intent encrypt = Action.ENCRYPT.intent();

		encrypt.setDataAndType( null, "text/plain" );

		Extra.PARTNER.to( encrypt, conversation.getPartner().getUsername() );
		Extra.ID.to( encrypt, message.getId() );
		Extra.TEXT.to( encrypt, text );

		sendBroadcast( encrypt, Manifest.permission.WRITE );

	}

	protected void sendFile( Conversation conversation, Intent intent ) {
		sendFile( conversation, intent, null );
	}

	protected void sendFile( Conversation conversation, Intent intent, Location location ) {

		final String remoteUserID = conversation.getPartner().getUsername();
		Uri uri = (Uri) intent.getParcelableExtra( Intent.EXTRA_STREAM );
		String text = intent.getStringExtra( Intent.EXTRA_TEXT );

		if( uri == null && text == null ) {
			if( !TextUtils.isEmpty( savedId ) ) {
				ConversationRepository conversations = SilentTextApplication.from( this ).getConversations();
				Conversation c = conversations.findByPartner( savedId );
				EventRepository history = conversations.historyOf( c );
				List<Event> messages = history.list();
				sendChatMessageAsAttachment( conversation, getPrintableHistory( messages ), null );
				finish();
			}
			return;
		}

		long size = AttachmentUtils.getFileSize( this, uri );

		if( uri != null ) {

			if( size >= AttachmentUtils.FILE_SIZE_LIMIT ) {

				AttachmentUtils.showFileSizeErrorDialog( this, new OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						setResult( RESULT_CANCELED );
						finish();
					}

				} );

				return;

			}

		}

		OutgoingMessage message = createOutgoingMessage( conversation, text, uri, intent.getType(), location );

		if( uri == null ) {
			message.setState( MessageState.COMPOSED );
		} else {
			message.setState( MessageState.UNKNOWN );
		}

		boolean isTalkingToSelf = isSelf( remoteUserID );

		if( isTalkingToSelf ) {
			message.setState( MessageState.SENT );
		}

		getConversations().historyOf( conversation ).save( message );

		if( uri == null ) {
			if( !isTalkingToSelf ) {
				transition( remoteUserID, message.getId() );
			}
			viewConversation( remoteUserID );
			finish();
			return;
		}

		Intent encrypt = Action.ENCRYPT.intent();
		encrypt.setDataAndType( uri, getMIMEType( intent ) );
		Extra.PARTNER.to( encrypt, remoteUserID );
		Extra.ID.to( encrypt, message.getId() );
		sendBroadcast( encrypt, Manifest.permission.WRITE );

		if( size > AttachmentUtils.FILE_SIZE_WARNING_THRESHOLD ) {

			AttachmentUtils.showFileSizeWarningDialog( this, remoteUserID, message.getId(), new OnClickListener() {

				@Override
				public void onClick( DialogInterface dialog, int which ) {
					viewConversation( remoteUserID );
					finish();
				}

			} );

			return;

		}

		viewConversation( remoteUserID );
		finish();

	}

	protected void sendFile( String remoteUserID ) {

		boolean existed = getConversations().exists( remoteUserID );
		final Conversation conversation = getOrCreateConversation( remoteUserID );

		if( !existed ) {
			getNative().connect( remoteUserID );
		}

		if( LocationUtils.isLocationSharingAvailable( this ) && conversation.isLocationEnabled() ) {

			LocationObserver.observe( this, new OnLocationReceivedListener() {

				@Override
				public void onLocationReceived( Location location ) {
					sendFile( conversation, getIntent(), location );
				}

				@Override
				public void onLocationUnavailable() {
					sendFile( conversation, getIntent() );
				}

			} );

		} else {
			sendFile( conversation, getIntent() );
		}

	}

	protected void viewConversation( String remoteUserID ) {
		Intent intent = new Intent( this, ConversationActivity.class );
		Extra.PARTNER.to( intent, remoteUserID );
		intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
		startActivity( intent );
	}

}
