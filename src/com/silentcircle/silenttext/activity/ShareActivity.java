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
package com.silentcircle.silenttext.activity;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.view.ContactView;
import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.adapter.ListAdapter;

public class ShareActivity extends SilentActivity {

	private static void applyConversationSettings( Conversation conversation, JSONObject json ) throws JSONException {

		if( conversation.hasBurnNotice() ) {
			json.put( "shred_after", conversation.getBurnDelay() );
		}

		if( conversation.isLocationEnabled() ) {
			// TODO: Should we put the location in here?
		}

	}

	private List<Contact> createContactsList() {
		List<Contact> contacts = new ArrayList<Contact>();
		List<Conversation> conversations = getConversations().list();
		for( int i = 0; i < conversations.size(); i++ ) {
			contacts.add( conversations.get( i ).getPartner() );
		}
		return contacts;
	}

	private OutgoingMessage createOutgoingMessage( Conversation conversation, String text ) {

		JSONObject json = new JSONObject();

		try {
			applyConversationSettings( conversation, json );
			if( text != null ) {
				json.put( "message", text );
			}
		} catch( JSONException exception ) {
			log.error( exception, "While trying to create outgoing message" );
		}

		OutgoingMessage message = new OutgoingMessage( getUsername(), json.toString() );
		message.setConversationID( conversation.getPartner().getUsername() );

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
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_share );
		setTitle( R.string.send_to );

		if( getIntent() != null ) {
			setIntent( getIntent() );
		}

		ListView contacts = findListViewById( R.id.contacts );

		contacts.setEmptyView( findViewById( R.id.empty ) );

		contacts.setOnItemClickListener( new OnItemClickListener() {

			@Override
			public void onItemClick( AdapterView<?> parentView, View view, int position, long itemID ) {
				if( view instanceof ContactView ) {
					String remoteUserID = ( (ContactView) view ).getContact().getUsername();
					sendFile( remoteUserID );
				}
			}

		} );

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

		ListView contacts = findListViewById( R.id.contacts );

		contacts.setAdapter( new ListAdapter<Contact>( R.layout.contact, createContactsList() ) );

	}

	private void sendFile( Conversation conversation, Intent intent ) {

		final String remoteUserID = conversation.getPartner().getUsername();
		Uri uri = (Uri) intent.getParcelableExtra( Intent.EXTRA_STREAM );
		String text = intent.getStringExtra( Intent.EXTRA_TEXT );

		if( uri == null && text == null ) {
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

		OutgoingMessage message = createOutgoingMessage( conversation, text );

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
		sendFile( getConversations().findByPartner( remoteUserID ), getIntent() );
	}

	protected void viewConversation( String remoteUserID ) {
		Intent intent = new Intent( this, ConversationActivity.class );
		Extra.PARTNER.to( intent, remoteUserID );
		intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
		startActivity( intent );
	}

}
