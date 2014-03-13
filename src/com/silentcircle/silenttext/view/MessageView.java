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

import java.util.Date;

import org.jivesoftware.smack.util.Base64;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.method.MovementMethod;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.SCloudActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.listener.LaunchActivityOnClick;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.SCIMPError;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.HandshakeEvent;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.WarningEvent;
import com.silentcircle.silenttext.model.siren.SirenLocation;
import com.silentcircle.silenttext.model.siren.SirenObject;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;

public class MessageView extends RelativeLayout implements Checkable, HasChoiceMode {

	public static class DisplayTimeRemaining implements OnClickListener {

		private final long expireTime;

		public DisplayTimeRemaining( long expireTime ) {
			this.expireTime = expireTime;
		}

		@Override
		public void onClick( View v ) {
			Toast.makeText( v.getContext(), v.getContext().getString( R.string.expires_n, DateUtils.getRelativeTimeSpanString( expireTime, System.currentTimeMillis(), 0 ) ), Toast.LENGTH_SHORT ).show();
		}

	}

	public static class ViewAttachment implements OnClickListener {

		private final String partner;
		private final String eventID;
		private final String locator;
		private final String key;

		public ViewAttachment( String partner, String eventID, String locator, String key ) {
			this.partner = partner;
			this.eventID = eventID;
			this.locator = locator;
			this.key = key;
		}

		@Override
		public void onClick( View view ) {

			Intent intent = new Intent( view.getContext(), SCloudActivity.class );

			Extra.ID.to( intent, eventID );
			Extra.PARTNER.to( intent, partner );
			Extra.LOCATOR.to( intent, locator );
			Extra.KEY.to( intent, key );

			view.getContext().startActivity( intent );

		}

	}

	private static final StyleSpan AUTHOR = new StyleSpan( Typeface.BOLD );

	public static String getTimeString( Context context, long raw ) {
		java.text.DateFormat dateFormat = DateFormat.getLongDateFormat( context );
		java.text.DateFormat timeFormat = DateFormat.getTimeFormat( context );
		String now = dateFormat.format( new Date() );
		String date = dateFormat.format( new Date( raw ) );
		String time = timeFormat.format( new Date( raw ) );
		return now.equals( date ) ? time : date;
	}

	protected boolean inChoiceMode;
	protected boolean checked;
	protected long burnTime;
	private boolean burnNotice;
	private boolean location;
	protected String eventID;
	private OnClickListener locationActionListener;
	private OnClickListener burnActionListener;
	private OnClickListener thumbnailActionListener;

	private OnClickListener resendActionListener;

	private MovementMethod textMovementMethod;

	public MessageView( Context context ) {
		super( context );
	}

	public MessageView( Context context, AttributeSet attributes ) {
		super( context, attributes );
	}

	public MessageView( Context context, AttributeSet attributes, int defaultStyle ) {
		super( context, attributes, defaultStyle );
	}

	private void attachChildListeners() {
		setOnClickListener( R.id.message_location, locationActionListener );
		setOnClickListener( R.id.message_thumbnail, thumbnailActionListener );
		setOnClickListener( R.id.message_burn, burnActionListener );
		setOnClickListener( R.id.message_resend, resendActionListener );
	}

	private void detachChildListeners() {
		removeOnClickListeners( R.id.message_location, R.id.message_thumbnail, R.id.message_burn, R.id.message_resend );
	}

	protected ImageView findImageViewById( int viewResourceId ) {
		return (ImageView) findViewById( viewResourceId );
	}

	protected TextView findTextViewById( int viewResourceId ) {
		return (TextView) findViewById( viewResourceId );
	}

	private String formatString( int stringResourceID, String message ) {
		return getContext().getResources().getString( stringResourceID, message );
	}

	private String getDisplayNameForSender( Message message ) {
		if( message instanceof OutgoingMessage ) {
			return getResources().getString( R.string.me );
		}
		SilentTextApplication application = SilentTextApplication.from( getContext() );
		String sender = application.getContacts().getDisplayName( message.getSender() );
		return sender == null ? message.getSender().replaceAll( "@.+$", "" ) : sender;
	}

	private String getLocalizedErrorString( int formatStringResourceID, SCIMPError error ) {
		return formatString( formatStringResourceID, getLocalizedErrorString( error ) );
	}

	private String getLocalizedErrorString( SCIMPError error ) {
		Resources resources = getResources();
		String key = String.format( "%s_%s", error.getClass().getSimpleName(), error.name() );
		int identifier = resources.getIdentifier( key, "string", getContext().getPackageName() );
		return resources.getString( identifier );
	}

	private String getTimeString( Event event ) {
		int labelID = R.string.received;
		boolean delivered = false;
		if( event instanceof OutgoingMessage ) {
			labelID = R.string.sent;
			MessageState state = ( (OutgoingMessage) event ).getState();
			if( MessageState.DELIVERED.equals( state ) ) {
				delivered = true;
			}
		}
		return getResources().getString( R.string.message_timestamp, getResources().getString( labelID ), getTimeString( getContext(), event.getTime() ), delivered ? getResources().getString( R.string.checkmark ) : "" );
	}

	@Override
	public boolean isChecked() {
		return isInChoiceMode() && checked;
	}

	@Override
	public boolean isInChoiceMode() {
		return inChoiceMode;
	}

	private void removeOnClickListeners( int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			setOnClickListener( viewResourceIDs[i], null );
		}
	}

	private void reset() {

		burnNotice = false;
		location = false;

		setVisibleIf( false, R.id.message_text, R.id.message_time, R.id.message_burn, R.id.message_location, R.id.message_thumbnail, R.id.message_resend );
		TextView time = (TextView) findViewById( R.id.message_time );
		time.setTextColor( getResources().getColor( R.color.silent_glass ) );
		time.setBackgroundColor( 0 );
		time.setCompoundDrawablesWithIntrinsicBounds( 0, 0, 0, 0 );

	}

	protected void save( Event event ) {
		SilentTextApplication application = SilentTextApplication.from( getContext() );
		ConversationRepository conversations = application.getConversations();
		if( conversations == null || !conversations.exists() ) {
			return;
		}
		Conversation conversation = conversations.findByPartner( event.getConversationID() );
		if( conversation == null ) {
			return;
		}
		EventRepository events = conversations.historyOf( conversation );
		if( events == null || !events.exists() ) {
			return;
		}
		events.save( event );
	}

	private void setBurnAction() {
		if( burnNotice && burnTime > 0 ) {
			burnActionListener = new DisplayTimeRemaining( burnTime );
			findViewById( R.id.message_burn ).setOnClickListener( burnActionListener );
		}
	}

	@Override
	public void setChecked( boolean checked ) {
		if( this.checked == checked ) {
			return;
		}
		if( checked && !isInChoiceMode() ) {
			return;
		}
		this.checked = checked;
		if( getBackground().isStateful() ) {
			getBackground().setState( new int [] {
				( isChecked() ? 1 : -1 ) * android.R.attr.state_checked
			} );
		}
	}

	private void setEvent( ErrorEvent event ) {

		TextView time = findTextViewById( R.id.message_time );
		TextView text = findTextViewById( R.id.message_text );

		setTextSize( text, R.dimen.text_normal );
		text.setTextColor( getResources().getColor( R.color.silent_red ) );

		if( SCIMPError.NONE.equals( event.getError() ) ) {
			text.setText( event.getText() );
		} else {
			text.setText( getLocalizedErrorString( R.string.error_format, event.getError() ) );
		}

		text.setVisibility( VISIBLE );
		time.setVisibility( GONE );

	}

	private void setEvent( Event event ) {

		TextView time = findTextViewById( R.id.message_time );
		TextView text = findTextViewById( R.id.message_text );

		setTextSize( text, R.dimen.text_normal );
		text.setTextColor( getResources().getColor( R.color.silent_glass ) );
		text.setText( event.getText() );

		text.setVisibility( VISIBLE );
		time.setVisibility( GONE );

	}

	/**
	 * @param event
	 */
	private void setEvent( HandshakeEvent event ) {

		TextView time = findTextViewById( R.id.message_time );
		TextView text = findTextViewById( R.id.message_text );

		setTextSize( text, R.dimen.text_small );
		text.setTextColor( getResources().getColor( R.color.silent_orange ) );
		text.setText( event.getText() );

		text.setVisibility( VISIBLE );
		time.setVisibility( GONE );

	}

	private void setEvent( final Message message ) {

		burnNotice = message.expires();
		burnTime = message.getExpirationTime();

		TextView time = findTextViewById( R.id.message_time );
		TextView text = findTextViewById( R.id.message_text );

		setTextSize( text, R.dimen.text_xlarge );
		setSiren( message );

		if( message instanceof OutgoingMessage && MessageState.RESEND_REQUESTED.equals( message.getState() ) ) {
			findViewById( R.id.message_resend ).setVisibility( View.VISIBLE );
			resendActionListener = new OnClickListener() {

				@Override
				public void onClick( View v ) {
					message.setState( MessageState.COMPOSED );
					save( message );
					Intent intent = Action.TRANSITION.intent();
					Extra.PARTNER.to( intent, message.getConversationID() );
					Extra.ID.to( intent, message.getId() );
					v.getContext().sendBroadcast( intent, Manifest.permission.WRITE );
				}
			};
			setOnClickListener( R.id.message_resend, resendActionListener );
		}

		if( message instanceof OutgoingMessage && MessageState.SENT.compareTo( message.getState() ) > 0 ) {

			text.setTextColor( getResources().getColor( R.color.silent_glass ) );

			String label = getResources().getString( R.string.sending );

			if( !SilentTextApplication.from( getContext() ).isOnline() ) {
				label = getResources().getString( R.string.waiting_for_connection );
			}

			if( MessageState.RESEND_REQUESTED.equals( message.getState() ) ) {
				label = getResources().getString( R.string.resend_requested ) + " ";
				time.setTextColor( getResources().getColor( R.color.silent_orange ) );
				time.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_warning, 0, 0, 0 );
			}

			time.setText( label );

		} else {
			text.setTextColor( getResources().getColor( R.color.silent_white ) );
			time.setText( getTimeString( message ) );
		}

		text.setVisibility( VISIBLE );
		time.setVisibility( VISIBLE );

	}

	private void setEvent( WarningEvent event ) {

		TextView time = findTextViewById( R.id.message_time );
		TextView text = findTextViewById( R.id.message_text );

		setTextSize( text, R.dimen.text_normal );

		text.setTextColor( getResources().getColor( R.color.silent_yellow ) );
		text.setText( getLocalizedErrorString( R.string.warning_format, event.getWarning() ) );

		text.setVisibility( VISIBLE );
		time.setVisibility( GONE );

	}

	@Override
	public void setInChoiceMode( boolean inChoiceMode ) {
		if( inChoiceMode == this.inChoiceMode ) {
			return;
		}
		this.inChoiceMode = inChoiceMode;
		if( inChoiceMode ) {
			detachChildListeners();
			textMovementMethod = findTextViewById( R.id.message_text ).getMovementMethod();
			findTextViewById( R.id.message_text ).setMovementMethod( null );
		} else {
			attachChildListeners();
			findTextViewById( R.id.message_text ).setMovementMethod( textMovementMethod );
			textMovementMethod = null;
		}
	}

	private void setLocation( String sender, SirenLocation location ) {
		this.location = true;
		locationActionListener = new LaunchActivityOnClick( new Intent( Intent.ACTION_VIEW, Uri.parse( location.getURI( sender ) ) ), R.string.error_activity_not_found_for_location );
		findViewById( R.id.message_location ).setOnClickListener( locationActionListener );
	}

	private void setLocation( String sender, String locationString ) {
		if( locationString == null ) {
			return;
		}
		try {
			setLocation( sender, new SirenLocation( locationString ) );
		} catch( JSONException exception ) {
			// Ignore.
		}
	}

	private void setOnClickListener( int viewResourceID, OnClickListener onClickListener ) {
		View view = findViewById( viewResourceID );
		if( view == null ) {
			return;
		}
		view.setOnClickListener( onClickListener );
	}

	private void setSiren( Message message ) {
		setSiren( getDisplayNameForSender( message ), message.getText() );
	}

	private void setSiren( String sender, SirenObject siren ) {

		setText( sender, siren.getString( "message" ) );

		setLocation( sender, siren.getString( "location" ) );
		setThumbnail( siren.getString( "thumbnail" ) );
		setThumbnailAction( siren );
		setBurnAction();

	}

	private void setSiren( String sender, String sirenText ) {
		try {
			setSiren( sender, new SirenObject( sirenText ) );
		} catch( JSONException exception ) {
			setText( sender, sirenText );
		}
	}

	@Override
	public void setTag( Object tag ) {

		super.setTag( tag );

		reset();

		if( tag == null ) {
			return;
		}

		if( tag instanceof Event ) {
			eventID = ( (Event) tag ).getId();
		}

		if( tag instanceof HandshakeEvent ) {
			setEvent( (HandshakeEvent) tag );
		} else if( tag instanceof ErrorEvent ) {
			setEvent( (ErrorEvent) tag );
		} else if( tag instanceof WarningEvent ) {
			setEvent( (WarningEvent) tag );
		} else if( tag instanceof Message ) {
			setEvent( (Message) tag );
		} else if( tag instanceof Event ) {
			setEvent( (Event) tag );
		}

		setVisibleIf( burnNotice, R.id.message_burn );
		setVisibleIf( location, R.id.message_location );

	}

	private void setText( String sender, String text ) {
		SpannableStringBuilder contents = new SpannableStringBuilder();
		contents.append( sender );
		contents.setSpan( AUTHOR, 0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		if( text != null ) {
			contents.append( ": " );
			contents.append( text );
		}
		TextView view = findTextViewById( R.id.message_text );
		view.setText( contents, BufferType.SPANNABLE );
		Linkify.addLinks( view, Linkify.ALL );
		if( isInChoiceMode() ) {
			textMovementMethod = view.getMovementMethod();
			view.setMovementMethod( null );
		}
	}

	private void setTextSize( TextView view, int dimenResourceId ) {
		view.setTextSize( TypedValue.COMPLEX_UNIT_PX, getResources().getDimension( dimenResourceId ) );
	}

	private void setThumbnail( Bitmap bitmap ) {

		ImageView view = findImageViewById( R.id.message_thumbnail );

		view.setImageBitmap( bitmap );
		view.setVisibility( bitmap == null ? GONE : VISIBLE );

	}

	private void setThumbnail( byte [] bitmapData ) {
		setThumbnail( BitmapFactory.decodeByteArray( bitmapData, 0, bitmapData.length ) );
	}

	private void setThumbnail( String base64bitmapData ) {
		if( base64bitmapData == null ) {
			return;
		}
		setThumbnail( Base64.decode( base64bitmapData ) );
	}

	private void setThumbnailAction( SirenObject siren ) {

		final String locator = siren.getString( "cloud_url" );
		final String key = siren.getString( "cloud_key" );

		if( locator != null && key != null ) {
			thumbnailActionListener = new ViewAttachment( "{placeholder}", eventID, locator, key );
			findViewById( R.id.message_thumbnail ).setOnClickListener( thumbnailActionListener );
		}

	}

	private void setVisibleIf( boolean truth, int... viewResourceIds ) {
		for( int viewResourceId : viewResourceIds ) {
			findViewById( viewResourceId ).setVisibility( truth ? VISIBLE : GONE );
		}
	}

	@Override
	public void toggle() {
		setChecked( !isChecked() );
	}

}
