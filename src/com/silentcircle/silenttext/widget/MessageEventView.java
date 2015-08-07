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
package com.silentcircle.silenttext.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationActivity;
import com.silentcircle.silenttext.activity.SCloudActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.location.LocationUtils;
import com.silentcircle.silenttext.model.Attachment;
import com.silentcircle.silenttext.model.Location;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.thread.Updater;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.DateUtils;
import com.silentcircle.silenttext.util.Updatable;
import com.silentcircle.silenttext.util.ViewUtils;
import com.silentcircle.silenttext.view.AvatarView;
import com.silentcircle.silenttext.view.HasChoiceMode;

public class MessageEventView extends RelativeLayout implements Updatable, Checkable, HasChoiceMode, OnClickListener {

	static class Views {

		public final AvatarView avatar;
		public final View card;
		public final ImageView preview;
		public final TextView text;
		public final TextView time;
		public final TextView burn_notice;
		public final View delivered;
		public final View action_location;
		public final View action_send;

		public Views( MessageEventView parent ) {

			avatar = (AvatarView) parent.findViewById( R.id.message_avatar );
			card = parent.findViewById( R.id.message_card );
			preview = (ImageView) parent.findViewById( R.id.message_preview );
			text = (TextView) parent.findViewById( R.id.message_body );
			time = (TextView) parent.findViewById( R.id.message_time );
			burn_notice = (TextView) parent.findViewById( R.id.message_burn_notice );
			delivered = parent.findViewById( R.id.message_delivered );

			action_location = parent.findViewById( R.id.message_action_location );
			action_location.setOnClickListener( parent );

			action_send = parent.findViewById( R.id.message_action_send );

			if( action_send != null ) {
				action_send.setOnClickListener( parent );
			}

		}

	}

	private static void setClickable( boolean clickable, View... views ) {
		for( View view : views ) {
			if( view != null ) {
				view.setClickable( clickable );
			}
		}
	}

	private static void setVoicemailLabelAndPreview( Siren siren, TextView labelView, ImageView imageView ) {
		imageView.setVisibility( GONE );

		String voicemailDurationLabel = AttachmentUtils.getLabelForDuration( siren.getMediaDuration() ) != null ? AttachmentUtils.getLabelForDuration( siren.getMediaDuration() ) : "";
		String voicemailLabel = siren.getVoicemailName() != null ? siren.getVoicemailName() : siren.getVoicemailNumber();

		labelView.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_voicemail, 0, 0, 0 );
		labelView.setCompoundDrawablePadding( 20 );

		labelView.setText( voicemailDurationLabel + "\n" + voicemailLabel );
	}

	private static void toggleDeliveredState( Message message, View view ) {
		if( view != null ) {
			if( MessageState.DELIVERED.equals( message.getState() ) ) {
				view.setVisibility( VISIBLE );
				ViewUtils.setAlpha( view, 0.5f );
			} else {
				view.setVisibility( GONE );
			}
		}
	}

	private static void toggleSendActionVisibility( Message message, View actionView ) {

		if( actionView == null ) {
			return;
		}

		actionView.setVisibility( GONE );

		if( message instanceof OutgoingMessage ) {
			if( MessageState.RESEND_REQUESTED.equals( message.getState() ) ) {
				actionView.setVisibility( VISIBLE );
			}
		}

	}

	private Views views;
	private boolean checked;

	private boolean inChoiceMode;

	private final Updater updater;

	Context context;

	public MessageEventView( Context context ) {
		super( context );
		MessageEventView.this.context = context;
		updater = new Updater( this );
	}

	public MessageEventView( Context context, AttributeSet attrs ) {
		super( context, attrs );
		MessageEventView.this.context = context;
		updater = new Updater( this );
	}

	public MessageEventView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		MessageEventView.this.context = context;
		updater = new Updater( this );
	}

	public Message getMessage() {
		Object tag = getTag();
		return tag instanceof Message ? (Message) tag : null;
	}

	private Views getViews() {
		if( views == null ) {
			views = new Views( this );
		}
		return views;
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
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		scheduleNextUpdate();
	}

	@Override
	public void onClick( View view ) {

		if( isInChoiceMode() ) {
			return;
		}

		Views v = getViews();
		Message message = getMessage();
		Context context = view.getContext();

		if( message == null ) {
			return;
		}

		if( v.action_location == view ) {

			try {

				Siren siren = message.getSiren();
				Location location = siren.getLocation();

				LocationUtils.viewLocation( context, location.getLatitude(), location.getLongitude() );

			} catch( NullPointerException exception ) {
				// This might happen if the message does not have a location; catching and burying
				// this exception is cleaner than a series of null checks.
			}

			return;

		}

		if( v.action_send == view ) {

			if( message.getState() == MessageState.RESEND_REQUESTED ) {
				( (ConversationActivity) context ).performAction( R.id.action_resend, message );

				return;
			}

			Intent intent = Action.TRANSITION.intent();

			Extra.PARTNER.to( intent, message.getConversationID() );
			Extra.ID.to( intent, message.getId() );
			Extra.STATE.to( intent, MessageState.COMPOSED.value() );

			context.sendBroadcast( intent, Manifest.permission.WRITE );

			return;

		}

		if( v.card == view || this == view ) {

			Siren siren = message.getSiren();

			if( siren.hasAttachments() ) {

				Intent intent = new Intent( context, SCloudActivity.class );

				Extra.ID.to( intent, message.getId() );
				Extra.PARTNER.to( intent, message.getConversationID() );
				Extra.LOCATOR.to( intent, siren.getCloudLocator() );
				Extra.KEY.to( intent, siren.getCloudKey() );

				context.startActivity( intent );

				return;

			}

			Intent links = ViewUtils.createIntentForLinks( v.text );

			if( links != null ) {
				context.startActivity( links );
			}

			return;

		}

	}

	@Override
	protected int [] onCreateDrawableState( int extraSpace ) {
		final int [] state = super.onCreateDrawableState( extraSpace + 1 );
		if( inChoiceMode && checked ) {
			mergeDrawableStates( state, ViewUtils.STATE_CHECKED );
		}
		return state;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		getViews();
	}

	private void scheduleNextUpdate() {
		Handler handler = getHandler();
		if( handler != null ) {
			handler.postDelayed( updater, 1000 );
		}
	}

	@Override
	public void setChecked( boolean checked ) {
		if( checked != this.checked ) {
			this.checked = checked;
			refreshDrawableState();
		}
	}

	@Override
	public void setInChoiceMode( boolean inChoiceMode ) {
		if( inChoiceMode != this.inChoiceMode ) {
			this.inChoiceMode = inChoiceMode;
			Views v = getViews();
			setClickable( !inChoiceMode, v.action_location, v.action_send );
			refreshDrawableState();
		}
	}

	private void setLabelAndPreview( Siren siren, TextView labelView, ImageView imageView ) {

		Context context = getContext();

		labelView.setVisibility( GONE );

		Attachment attachment = AttachmentUtils.getAttachment( context, siren );
		Bitmap bitmap = AttachmentUtils.getPreviewImage( context, siren, attachment );
		String contentType = AttachmentUtils.getContentType( siren, attachment );

		imageView.setImageBitmap( bitmap );
		imageView.setVisibility( bitmap != null ? VISIBLE : GONE );

		if( bitmap != null ) {
			String duration = AttachmentUtils.getLabelForDuration( siren.getMediaDuration() );
			if( duration != null ) {
				labelView.setText( duration );
				labelView.setVisibility( VISIBLE );
				ViewUtils.setDrawableStart( labelView, AttachmentUtils.getAttachmentLabelIcon( contentType ) );
			}
			return;
		}

		labelView.setVisibility( VISIBLE );
		ViewUtils.setDrawableStart( labelView, AttachmentUtils.getAttachmentLabelIcon( contentType ) );

		String label = AttachmentUtils.getLabelForDuration( siren.getMediaDuration() );

		if( label == null && attachment != null && attachment.getName() != null ) {
			label = new String( attachment.getName() );
		}

		if( label == null && contentType != null ) {
			label = contentType;
		}

		if( label == null ) {
			labelView.setText( R.string.attachment );
		} else {
			labelView.setText( new String( label ) );
		}

	}

	public void setMessage( Message message ) {

		setVisibility( VISIBLE );

		Views v = getViews();

		v.avatar.setContact( message.getSender() );

		setTime( message, v.time );
		updateBurnNotice();
		toggleEnabledState( message );
		toggleDeliveredState( message, v.delivered );
		toggleSendActionVisibility( message, v.action_send );

		setSiren( message.getSiren() );

	}

	public void setSiren( Siren siren ) {

		if( siren == null ) {
			return;
		}

		Views v = getViews();

		v.action_location.setVisibility( siren.getLocation() != null ? VISIBLE : GONE );

		if( siren.hasAttachments() ) {
			if( siren.isVoicemail() ) {
				setVoicemailLabelAndPreview( siren, v.text, v.preview );
			} else {
				setLabelAndPreview( siren, v.text, v.preview );
			}

			if( Constants.isRTL() ) {
				showRTLLanguage( v );
			}
			return;
		}

		String chatMessage = siren.getChatMessage();

		if( chatMessage != null ) {
			v.preview.setVisibility( GONE );
			v.text.setText( chatMessage );
			Linkify.addLinks( v.text, Linkify.ALL );
			v.text.setMovementMethod( null );
			v.text.setVisibility( VISIBLE );
			ViewUtils.setDrawableStart( v.text, 0 );

			if( Constants.isRTL() ) {
				showRTLLanguage( v );
			}
			return;
		}

		setVisibility( GONE );

	}

	@Override
	public void setTag( Object tag ) {
		super.setTag( tag );
		if( tag instanceof Message ) {
			setMessage( (Message) tag );
		}
	}

	private void setTime( Message message, TextView time ) {

		if( message instanceof OutgoingMessage ) {

			switch( message.getState() ) {

				case COMPOSED:

					time.setText( R.string.securing );
					return;

				case ENCRYPTED:

					if( SilentTextApplication.from( getContext() ).isXMPPTransportConnected() ) {
						time.setText( R.string.sending );
					} else {
						time.setText( R.string.waiting_for_connection );
					}

					return;

				default:
					break;

			}

		}

		time.setText( DateUtils.getRelativeTimeSpanString( getContext(), message.getTime() ) );

	}

	@SuppressLint( "NewApi" )
	private void showRTLLanguage( Views v ) {
		Message msg = getMessage();
		SilentTextApplication app = SilentTextApplication.from( context );
		if( Build.VERSION.SDK_INT >= 16 ) {
			if( msg.getSender().contains( app.getUsername() ) ) {
				v.card.setBackground( context.getResources().getDrawable( R.drawable.bg_my_card_dark_default_flip ) );
			} else {
				v.card.setBackground( context.getResources().getDrawable( R.drawable.bg_card_dark_default_flip ) );
			}
		} else {
			if( msg.getSender().contains( app.getUsername() ) ) {
				v.card.setBackgroundDrawable( context.getResources().getDrawable( R.drawable.bg_card_dark_default ) );
			} else {
				v.card.setBackgroundDrawable( context.getResources().getDrawable( R.drawable.bg_my_card_dark_default ) );
			}
		}
	}

	@Override
	public void toggle() {
		setChecked( !isChecked() );
	}

	private void toggleEnabledState( Message message ) {

		boolean enabled = !( message instanceof OutgoingMessage ) || MessageState.SENT.compareTo( message.getState() ) <= 0 && !MessageState.RESEND_REQUESTED.equals( message.getState() );
		ViewUtils.setAlpha( getViews().card, enabled ? 1 : 0.5f );

	}

	@Override
	public void update() {
		updateBurnNotice();
		scheduleNextUpdate();
	}

	private void updateBurnNotice() {

		Message message = getMessage();
		Views v = getViews();
		Context context = getContext();

		if( message == null || v == null || context == null ) {
			return;
		}

		if( message.expires() ) {
			v.burn_notice.setVisibility( VISIBLE );

			if( message.getState() == MessageState.READ || message.getState() == MessageState.SENT ) {
				v.burn_notice.setText( DateUtils.getShortTimeString( context, message.getExpirationTime() - System.currentTimeMillis() ) );
			} else {
				v.burn_notice.setText( DateUtils.getShortTimeString( context, message.getBurnNotice() * 1000 ) );
			}
		} else {
			v.burn_notice.setVisibility( GONE );
		}

	}
}
