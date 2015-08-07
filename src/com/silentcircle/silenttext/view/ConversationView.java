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
package com.silentcircle.silenttext.view;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.SCIMPError;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.HandshakeEvent;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.WarningEvent;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.DateUtils;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.util.ViewUtils;

public class ConversationView extends LinearLayout implements OnClickListener, Checkable, HasChoiceMode {

	class UpdateDisplayNameTask extends AsyncTask<String, Void, String> {

		private final Object tag;

		UpdateDisplayNameTask( Object tag ) {
			this.tag = tag;
		}

		@Override
		protected String doInBackground( String... args ) {
			return SilentTextApplication.from( getContext() ).getDisplayName( args[0] );
		}

		@Override
		protected void onPostExecute( String displayName ) {

			if( tag != getTag() ) {
				return;
			}

			Conversation conversation = (Conversation) getTag();

			if( StringUtils.isMinimumLength( displayName, 1 ) ) {
				conversation.getPartner().setAlias( displayName );
				setText( R.id.alias, conversation.getPartner().getAlias() );
			}

		}
	}

	private static void setDrawableLeft( TextView view, int drawableResourceID ) {
		view.setCompoundDrawablesWithIntrinsicBounds( drawableResourceID, 0, 0, 0 );
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
	private static void setDrawableStart( TextView view, int drawableResourceID ) {
		view.setCompoundDrawablesRelativeWithIntrinsicBounds( drawableResourceID, 0, 0, 0 );
	}

	private static void setDrawableStartSupport( TextView tv, int drawableResourceID ) {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
			setDrawableLeft( tv, drawableResourceID );
		} else {
			setDrawableStart( tv, drawableResourceID );
		}
	}

	protected boolean inChoiceMode;
	protected boolean checked;

	public ConversationView( Context context ) {
		super( context );
	}

	public ConversationView( Context context, AttributeSet attributes ) {
		super( context, attributes );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	public ConversationView( Context context, AttributeSet attributes, int defaultStyle ) {
		super( context, attributes, defaultStyle );
	}

	private void enableMarquee( int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			View view = findViewById( id );
			if( view != null ) {
				view.setSelected( true );
			}
		}
	}

	private String formatDate( long time ) {
		return DateUtils.getTimeString( getContext(), time );
	}

	private String formatString( int stringResourceID, String message ) {
		return getContext().getResources().getString( stringResourceID, message );
	}

	private SilentTextApplication getApplication() {
		return (SilentTextApplication) getContext().getApplicationContext();
	}

	private ConversationRepository getConversations() {
		return getApplication().getConversations();
	}

	private EventRepository getEvents( Conversation conversation ) {
		ConversationRepository conversations = getConversations();
		return conversations != null ? conversations.historyOf( conversation ) : null;
	}

	private String getLocalizedErrorString( int formatStringResourceID, SCIMPError error ) {
		return formatString( formatStringResourceID, getLocalizedErrorString( error ) );
	}

	private String getLocalizedErrorString( SCIMPError error ) {
		Resources resources = getResources();
		String key = String.format( "SCIMPError_%s", error.getName() );
		int identifier = resources.getIdentifier( key, "string", getContext().getPackageName() );
		return resources.getString( identifier );
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

	@Override
	public void onClick( View _this ) {
		if( isInChoiceMode() ) {
			return;
		}
		Object tag = getTag();
		if( tag != null && tag instanceof Conversation ) {
			Constants.mConversationListItemClicked = true;
			Intent intent = new Intent( getContext(), ConversationActivity.class );
			intent.putExtra( Extra.PARTNER.getName(), ( (Conversation) tag ).getPartner().getUsername() );
			ViewUtils.startActivity( getContext(), intent, R.anim.slide_in_from_right, R.anim.slide_out_to_left );
		}
	}

	@Override
	protected int [] onCreateDrawableState( int extraSpace ) {
		final int [] state = super.onCreateDrawableState( extraSpace + 1 );
		if( checked ) {
			mergeDrawableStates( state, ViewUtils.STATE_CHECKED );
		}
		return state;
	}

	private void save( Conversation conversation ) {
		ConversationRepository conversations = getConversations();
		if( conversations != null ) {
			conversations.save( conversation );
		}
	}

	@Override
	public void setChecked( boolean checked ) {
		if( checked != this.checked ) {
			this.checked = checked;
			refreshDrawableState();
		}
	}

	protected void setDrawableStart( int id, int drawableResourceID ) {
		View view = findViewById( id );
		if( view instanceof TextView ) {
			TextView tv = (TextView) view;
			setDrawableStartSupport( tv, drawableResourceID );
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
	public void setTag( final Object tag ) {

		super.setTag( tag );

		enableMarquee( R.id.alias, R.id.snippet );

		if( tag != null && tag instanceof Conversation ) {

			Conversation conversation = (Conversation) tag;
			String remoteUserID = conversation.getPartner().getUsername();

			setTag( R.id.username, remoteUserID );
			setText( R.id.alias, conversation.getPartner().getAlias() );

			AsyncUtils.execute( new UpdateDisplayNameTask( tag ), remoteUserID );

			AvatarView badge = (AvatarView) findViewById( R.id.avatar );
			badge.setContact( conversation.getPartner() );
			badge.setSecondaryOnClickListener( this );

			EventRepository events = getEvents( conversation );
			Event preview = events != null ? events.findById( conversation.getPreviewEventID() ) : null;

			if( events != null ) {
				for( int i = events.list().size() - 1; i > 0; i-- ) {
					if( events.list().get( i ) != null && events.list().get( i ) instanceof Message && !SilentTextApplication.isResendRequest( (Message) events.list().get( i ) ) && ( (Message) events.list().get( i ) ).getSiren() != null ) {
						preview = events.list().get( i );

						break;
					}
				}
			}

			if( conversation.getPreviewEventID() != null && preview == null ) {
				transition( conversation.getPartner().getUsername(), conversation.getPreviewEventID() );
				List<Event> history = events != null ? events.list() : null;
				if( history != null && history.size() > 0 ) {
					Event e = history.get( history.size() - 1 );
					conversation.setPreviewEventID( e.getId() );
					conversation.setLastModified( e.getTime() );

					preview = e;
				} else {
					conversation.setPreviewEventID( (byte []) null );
				}
				conversation.setUnreadMessageCount( 0 );
				if( history != null ) {
					for( int i = 0; i < history.size(); i++ ) {
						Event e = history.get( i );
						if( e instanceof IncomingMessage ) {
							IncomingMessage m = (IncomingMessage) e;
							if( MessageState.DECRYPTED.equals( m.getState() ) ) {
								conversation.offsetUnreadMessageCount( 1 );
							}
						}
					}
				}
				save( conversation );
			}

			if( conversation.getUnreadMessageCount() > 0 ) {
				setText( R.id.count, Integer.toString( conversation.getUnreadMessageCount() ) );
				findViewById( R.id.count ).setVisibility( VISIBLE );
			} else {
				findViewById( R.id.count ).setVisibility( GONE );
			}

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
				Siren siren = preview instanceof Message ? ( (Message) preview ).getSiren() : null;
				if( siren == null ) {
					if( preview instanceof WarningEvent ) {
						SCIMPError warning = ( (WarningEvent) preview ).getWarning();
						if( SCIMPError.NONE.equals( warning ) ) {
							snippet.append( preview.getText() );
						} else {
							snippet.append( getLocalizedErrorString( R.string.error_format, warning ) );
						}
					} else if( preview instanceof ErrorEvent ) {
						SCIMPError error = ( (ErrorEvent) preview ).getError();
						if( SCIMPError.NONE.equals( error ) ) {
							snippet.append( preview.getText() );
						} else {
							snippet.append( getLocalizedErrorString( R.string.error_format, error ) );
						}
					} else {
						snippet.clear();
					}
				} else {
					if( siren.isVoicemail() ) {
						snippet.clear();
						snippet.append( siren.getVoicemailName() );
					} else {
						if( siren.getThumbnail() != null ) {
							snippet.setSpan( new ImageSpan( getContext(), R.drawable.ic_attached_photo, DynamicDrawableSpan.ALIGN_BOTTOM ), snippet.length() - 1, snippet.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE );
						}
						if( siren.getChatMessage() != null ) {
							snippet.append( siren.getChatMessage() );
						} else {
							snippet.setSpan( new StyleSpan( Typeface.ITALIC ), snippet.length() - 1, snippet.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE );
							if( siren.getThumbnail() != null ) {
								snippet.append( getResources().getString( R.string.attachment ) );
							} else {
								snippet.append( getResources().getString( R.string.silence ) );
							}
						}
					}
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
