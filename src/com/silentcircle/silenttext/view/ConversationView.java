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
package com.silentcircle.silenttext.view;

import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.HandshakeEvent;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.WarningEvent;
import com.silentcircle.silenttext.model.siren.SirenObject;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;

public class ConversationView extends RelativeLayout implements OnClickListener, Checkable, HasChoiceMode {

	protected boolean inChoiceMode;
	protected boolean checked;

	public ConversationView( Context context ) {
		super( context );
	}

	public ConversationView( Context context, AttributeSet attributes ) {
		super( context, attributes );
	}

	public ConversationView( Context context, AttributeSet attributes, int defaultStyle ) {
		super( context, attributes, defaultStyle );
	}

	private String formatDate( long time ) {
		return MessageView.getTimeString( getContext(), time );
	}

	private SilentTextApplication getApplication() {
		return (SilentTextApplication) getContext().getApplicationContext();
	}

	private ContactRepository getContacts() {
		return getApplication().getContacts();
	}

	private ConversationRepository getConversations() {
		return getApplication().getConversations();
	}

	private EventRepository getEvents( Conversation conversation ) {
		return getConversations().historyOf( conversation );
	}

	protected String getText( int id ) {
		return ( (TextView) findViewById( id ) ).getText().toString();
	}

	@Override
	public boolean isChecked() {
		return checked;
	}

	@Override
	public boolean isInChoiceMode() {
		return inChoiceMode;
	}

	private boolean isVerified( Conversation conversation ) {
		if( getApplication().isSelf( conversation.getPartner().getUsername() ) ) {
			return true;
		}
		ResourceStateRepository states = getConversations().contextOf( conversation );
		ResourceState state = states.findById( conversation.getPartner().getDevice() );
		return state != null && states.isVerified( state );
	}

	@Override
	public void onClick( View _this ) {
		if( isInChoiceMode() ) {
			return;
		}
		Object tag = getTag();
		if( tag != null && tag instanceof Conversation ) {
			Intent intent = new Intent( getContext(), ConversationActivity.class );
			intent.putExtra( Extra.PARTNER.getName(), ( (Conversation) tag ).getPartner().getUsername() );
			getContext().startActivity( intent );
		}
	}

	@Override
	public void setChecked( boolean checked ) {
		this.checked = checked;
		if( getBackground().isStateful() ) {
			getBackground().setState( new int [] {
				( isChecked() ? 1 : -1 ) * android.R.attr.state_checked
			} );
		}
	}

	@Override
	public void setInChoiceMode( boolean inChoiceMode ) {
		this.inChoiceMode = inChoiceMode;
	}

	protected void setSpannableText( int id, CharSequence text ) {
		( (TextView) findViewById( id ) ).setText( text, BufferType.SPANNABLE );
	}

	@Override
	public void setTag( Object tag ) {

		super.setTag( tag );

		if( tag != null && tag instanceof Conversation ) {

			Conversation conversation = (Conversation) tag;
			String remoteUserID = conversation.getPartner().getUsername();
			conversation.getPartner().setAlias( getContacts().getDisplayName( remoteUserID ) );
			AvatarView badge = (AvatarView) findViewById( R.id.avatar );
			badge.setContact( conversation.getPartner() );
			badge.setSecondaryOnClickListener( this );

			setText( R.id.alias, conversation.getPartner().getAlias() );
			setText( R.id.device, conversation.getPartner().getDevice() );

			findViewById( R.id.secure_icon ).setVisibility( isVerified( conversation ) ? VISIBLE : GONE );

			EventRepository events = getEvents( conversation );
			Event preview = events.findById( conversation.getPreviewEventID() );

			if( conversation.getPreviewEventID() != null && preview == null ) {
				transition( conversation.getPartner().getUsername(), conversation.getPreviewEventID() );
				List<Event> history = events.list();
				if( history.size() > 0 ) {
					Event e = history.get( history.size() - 1 );
					conversation.setPreviewEventID( e.getId() );
					conversation.setLastModified( e.getTime() );
				} else {
					conversation.setPreviewEventID( null );
				}
				conversation.setUnreadMessageCount( 0 );
				for( int i = 0; i < history.size(); i++ ) {
					Event e = history.get( i );
					if( e instanceof IncomingMessage ) {
						IncomingMessage m = (IncomingMessage) e;
						if( MessageState.DECRYPTED.equals( m.getState() ) ) {
							conversation.offsetUnreadMessageCount( 1 );
						}
					}
				}
				getConversations().save( conversation );
			}

			setText( R.id.count, Integer.toString( conversation.getUnreadMessageCount() ) );

			int color = R.color.conversation_summary_snippet_message;

			if( preview instanceof HandshakeEvent ) {
				color = R.color.conversation_summary_snippet_security_event;
			} else if( preview instanceof WarningEvent ) {
				color = R.color.conversation_summary_snippet_warning;
			} else if( preview instanceof ErrorEvent ) {
				color = R.color.conversation_summary_snippet_error;
			}

			TextView snippetView = (TextView) findViewById( R.id.snippet );
			TextView timeView = (TextView) findViewById( R.id.timestamp );
			SpannableStringBuilder snippet = new SpannableStringBuilder();

			timeView.setText( formatDate( preview == null ? System.currentTimeMillis() : preview.getTime() ) );
			snippetView.setTextColor( getResources().getColor( color ) );

			if( preview instanceof IncomingMessage ) {
				snippet.insert( 0, "← " );
			} else if( preview instanceof OutgoingMessage ) {
				snippet.insert( 0, "→ " );
			}

			if( preview == null ) {
				snippet.append( getResources().getString( R.string.silence ) );
				snippet.setSpan( new StyleSpan( Typeface.ITALIC ), 0, snippet.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE );
			} else {
				try {
					SirenObject siren = new SirenObject( preview.getText() );
					if( siren.getString( "thumbnail" ) != null ) {
						snippet.setSpan( new ImageSpan( getContext(), R.drawable.ic_attached_photo, DynamicDrawableSpan.ALIGN_BOTTOM ), snippet.length() - 1, snippet.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE );
					}
					if( siren.getString( "message" ) != null ) {
						snippet.append( siren.getString( "message" ) );
					} else {
						snippet.setSpan( new StyleSpan( Typeface.ITALIC ), snippet.length() - 1, snippet.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE );
						if( siren.getString( "thumbnail" ) != null ) {
							snippet.append( getResources().getString( R.string.attachment ) );
						} else {
							snippet.append( getResources().getString( R.string.silence ) );
						}
					}
				} catch( JSONException exception ) {
					snippet.append( preview.getText() );
				}
			}

			if( conversation.containsUnreadMessages() ) {
				snippetView.setTypeface( null, Typeface.BOLD );
				snippetView.setTextColor( getResources().getColor( R.color.silent_white ) );
			} else {
				snippetView.setTypeface( null, Typeface.NORMAL );
			}

			snippetView.setText( snippet, BufferType.SPANNABLE );

		}

	}

	protected void setText( int id, String text ) {
		( (TextView) findViewById( id ) ).setText( text );
	}

	@Override
	public void toggle() {
		setChecked( !checked );
	}

	private void transition( String remoteUserID, String packetID ) {
		Intent intent = Action.TRANSITION.intent();
		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );
		getContext().sendBroadcast( intent, Manifest.permission.READ );
	}

}
