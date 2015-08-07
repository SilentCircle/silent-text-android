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

import java.lang.ref.SoftReference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.dialog.InformationalDialog;
import com.silentcircle.silenttext.listener.ClearHistoryOnConfirm;
import com.silentcircle.silenttext.listener.LaunchConfirmDialogOnClick;
import com.silentcircle.silenttext.listener.ResetKeysOnConfirm;
import com.silentcircle.silenttext.listener.SaveConversationHistoryOnConfirm;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;
import com.silentcircle.silenttext.util.BurnDelay;

public class ConversationOptionsDrawer extends ScrollView {

	private SoftReference<Activity> activityReference;
	protected String partner;

	public ConversationOptionsDrawer( Context context ) {
		super( context );
	}

	public ConversationOptionsDrawer( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public ConversationOptionsDrawer( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public void attach( Activity activity, String partner ) {
		activityReference = new SoftReference<Activity>( activity );
		setPartner( partner );
	}

	public void detach() {
		activityReference = null;
		partner = null;
	}

	protected Activity getActivity() {
		if( activityReference == null ) {
			return null;
		}
		Activity activity = activityReference.get();
		if( activity == null ) {
			activityReference = null;
		}
		return activity;
	}

	private boolean isTalkingToSelf() {
		return SilentTextApplication.from( getContext() ).isSelf( partner );
	}

	public void onMessageOptionsChanged() {
		if( partner == null ) {
			setClearButtonEnabled( false );
			return;
		}
		SilentTextApplication application = SilentTextApplication.from( getContext() );
		ConversationRepository conversations = application.getConversations();
		if( conversations == null || !conversations.exists() ) {
			setClearButtonEnabled( false );
			return;
		}
		Conversation conversation = conversations.findByPartner( partner );
		if( conversation == null ) {
			setClearButtonEnabled( false );
			return;
		}
		boolean empty = conversations.historyOf( conversation ).list().isEmpty();
		setClearButtonEnabled( !empty );
		setVisibleIf( !empty, R.id.save );
	}

	public void onVerificationOptionsChanged() {
		Activity activity = getActivity();
		if( activity == null ) {
			return;
		}
		SilentTextApplication application = SilentTextApplication.from( activity );
		if( application == null ) {
			return;
		}
		ConversationRepository conversations = application.getConversations();
		if( conversations == null || !conversations.exists() ) {
			return;
		}
		Conversation conversation = conversations.findByPartner( partner );
		if( conversation == null ) {
			return;
		}
		ResourceStateRepository states = conversations.contextOf( conversation );
		ResourceState state = states == null ? null : states.findById( conversation.getPartner().getDevice() );
		boolean secured = state != null && state.isSecure();
		boolean verified = secured && states != null && state != null && states.isVerified( state );
		setSecured( secured );
		setVerified( verified, state == null ? null : state.getVerifyCode() );
	}

	public void prepareMessageOptions() {

		SeekBar seeker = (SeekBar) findViewById( R.id.burn_delay_value );
		CheckBox burnNotice = (CheckBox) findViewById( R.id.burn_notice );
		CheckBox locationSharing = (CheckBox) findViewById( R.id.location_sharing );
		CheckBox sendReceipts = (CheckBox) findViewById( R.id.send_receipts );

		SilentTextApplication application = SilentTextApplication.from( getContext() );

		ConversationRepository conversations = application.getConversations();
		if( conversations != null && conversations.exists() ) {

			Conversation conversation = conversations.findByPartner( partner );
			if( conversation != null ) {

				if( !seeker.isPressed() ) {
					seeker.setMax( BurnDelay.Defaults.numLevels() - 1 );
					if( conversation.hasBurnNotice() ) {
						seeker.setProgress( BurnDelay.Defaults.getLevel( conversation.getBurnDelay() ) );
					} else {
						seeker.setProgress( 0 );
					}
				}

				burnNotice.setChecked( conversation.hasBurnNotice() );
				locationSharing.setChecked( conversation.isLocationEnabled() );
				sendReceipts.setChecked( conversation.shouldSendReadReceipts() );

				TextView label = (TextView) findViewById( R.id.burn_delay_label );
				label.setText( BurnDelay.Defaults.getLabel( getContext(), seeker.getProgress() ) );

			}

		}

		seeker.setOnTouchListener( new OnTouchListener() {

			@Override
			public boolean onTouch( View v, MotionEvent event ) {

				int action = event.getAction();

				switch( action ) {
					case MotionEvent.ACTION_DOWN:
						v.getParent().requestDisallowInterceptTouchEvent( true );
						break;

					case MotionEvent.ACTION_UP:
						v.getParent().requestDisallowInterceptTouchEvent( false );
						break;
				}

				v.onTouchEvent( event );

				return true;

			}

		} );

		seeker.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
				Context context = seekBar.getContext();
				SilentTextApplication application = SilentTextApplication.from( context );
				ConversationRepository conversations = application.getConversations();
				Conversation conversation = conversations.findByPartner( partner );

				if( conversation.hasBurnNotice() ) {
					TextView label = (TextView) findViewById( R.id.burn_delay_label );
					label.setText( BurnDelay.Defaults.getLabel( getContext(), progress ) );
					CheckBox burnNotice = (CheckBox) findViewById( R.id.burn_notice );
					burnNotice.setChecked( progress > 0 );
				}
			}

			@Override
			public void onStartTrackingTouch( SeekBar seekBar ) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch( SeekBar seekBar ) {

				if( partner == null ) {
					return;
				}

				Context context = seekBar.getContext();
				SilentTextApplication application = SilentTextApplication.from( context );
				ConversationRepository conversations = application.getConversations();
				Conversation conversation = conversations.findByPartner( partner );

				conversation.setBurnDelay( BurnDelay.Defaults.getDelay( seekBar.getProgress() ) );
				conversation.setBurnNotice( conversation.getBurnDelay() > 0 );
				conversations.save( conversation );

				CheckBox burnNotice = (CheckBox) findViewById( R.id.burn_notice );
				burnNotice.setChecked( conversation.hasBurnNotice() );

				Intent intent = Action.UPDATE_CONVERSATION.intent();
				Extra.PARTNER.to( intent, partner );
				context.sendBroadcast( intent, Manifest.permission.READ );

			}

		} );

		locationSharing.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
				toggleLocationSharing( isChecked );
			}

		} );

		burnNotice.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
				toggleBurnNotice( isChecked );
			}

		} );

		sendReceipts.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
				toggleSendReceipts( isChecked );
			}

		} );

		if( partner != null ) {
			findViewById( R.id.clear ).setOnClickListener( new LaunchConfirmDialogOnClick( R.string.are_you_sure, R.string.cannot_be_undone, getActivity(), new ClearHistoryOnConfirm( partner ) ) );
			findViewById( R.id.save ).setOnClickListener( new LaunchConfirmDialogOnClick( R.string.are_you_sure, R.string.save_will_expose_data, R.string.cancel, R.string.save, getActivity(), new SaveConversationHistoryOnConfirm( getActivity(), partner ) ) );
		}

		onMessageOptionsChanged();

	}

	public void prepareVerificationOptions() {

		View resetKeysView = findViewById( R.id.reset_keys );

		resetKeysView.setVisibility( isTalkingToSelf() ? GONE : VISIBLE );
		resetKeysView.setOnClickListener( new LaunchConfirmDialogOnClick( R.string.are_you_sure, R.string.reset_keys_warning, getActivity(), new ResetKeysOnConfirm( resetKeysView, partner ) ) );

		findViewById( R.id.verify_rating ).setOnClickListener( new InformationalDialog( R.string.security_rating, R.layout.security_rating ) );

		CheckBox verify = (CheckBox) findViewById( R.id.verified );

		verify.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
				setVerified( isChecked );
			}

		} );

		onVerificationOptionsChanged();

	}

	protected void setCheckedIf( boolean condition, int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			View view = findViewById( id );
			if( view instanceof Checkable ) {
				( (Checkable) view ).setChecked( condition );
			}
		}
	}

	private void setClearButtonEnabled( boolean enabled ) {
		findViewById( R.id.clear ).setEnabled( enabled );
	}

	protected void setEnabledIf( boolean condition, int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int viewResourceID = viewResourceIDs[i];
			View view = findViewById( viewResourceID );
			if( view != null ) {
				view.setEnabled( condition );
			}
		}
	}

	public void setPartner( String partner ) {
		this.partner = partner;
		prepareMessageOptions();
		prepareVerificationOptions();
	}

	private void setSecured( boolean secured ) {
		setVisibleIf( secured, R.id.verify );
		View resetButton = findViewById( R.id.reset_keys );
		Object pending = resetButton.getTag( R.id.pending );
		if( secured && pending == null ) {
			setEnabledIf( true, R.id.reset_keys );
		}
		pending = null;
		resetButton.setTag( R.id.pending, pending );
	}

	protected void setText( int viewResourceID, CharSequence text ) {
		( (TextView) findViewById( viewResourceID ) ).setText( text );
	}

	protected void setText( int viewResourceID, int stringResourceID ) {
		( (TextView) findViewById( viewResourceID ) ).setText( stringResourceID );
	}

	protected void setVerified( boolean verified ) {
		Activity activity = getActivity();
		if( activity == null ) {
			return;
		}
		SilentTextApplication application = SilentTextApplication.from( getActivity() );
		ConversationRepository conversations = application.getConversations();
		if( conversations == null || !conversations.exists() ) {
			return;
		}
		Conversation conversation = conversations.findByPartner( partner );
		if( conversation == null ) {
			return;
		}
		ResourceStateRepository states = conversations.contextOf( conversation );
		if( states == null ) {
			return;
		}
		ResourceState state = states.findById( conversation.getPartner().getDevice() );
		if( state == null ) {
			return;
		}
		states.setVerified( state, verified );
		setVerified( verified, state.getVerifyCode() );

		Intent intent = Action.UPDATE_CONVERSATION.intent();
		Extra.PARTNER.to( intent, partner );
		getActivity().sendBroadcast( intent, Manifest.permission.READ );

	}

	private void setVerified( boolean verified, String sasPhrase ) {

		TextView label = (TextView) findViewById( R.id.verify_label );
		label.setText( verified ? R.string.verify_description_verified : R.string.verify_description_unverified );

		CheckBox verify = (CheckBox) findViewById( R.id.verified );

		verify.setChecked( verified );
		verify.setText( sasPhrase );

	}

	protected void setVisibleIf( boolean condition, int... viewResourceIDs ) {
		int visibility = condition ? View.VISIBLE : View.GONE;
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int viewResourceID = viewResourceIDs[i];
			View view = findViewById( viewResourceID );
			if( view != null ) {
				view.setVisibility( visibility );
			}
		}
	}

	protected void toggleBurnNotice( boolean isChecked ) {

		SilentTextApplication application = SilentTextApplication.from( getActivity() );

		ConversationRepository conversations = application.getConversations();
		if( conversations != null && conversations.exists() ) {

			Conversation conversation = conversations.findByPartner( partner );
			if( conversation != null ) {

				conversation.setBurnNotice( isChecked );
				if( isChecked && conversation.getBurnDelay() <= 0 ) {
					conversation.setBurnDelay( BurnDelay.getDefaultDelay() );
				}
				conversations.save( conversation );

				Intent intent = Action.UPDATE_CONVERSATION.intent();
				Extra.PARTNER.to( intent, partner );
				getActivity().sendBroadcast( intent, Manifest.permission.READ );

			}

		}

	}

	protected void toggleLocationSharing( boolean isChecked ) {

		SilentTextApplication application = SilentTextApplication.from( getContext() );

		ConversationRepository conversations = application.getConversations();
		if( conversations != null && conversations.exists() ) {

			Conversation conversation = conversations.findByPartner( partner );
			if( conversation != null ) {

				conversation.setLocationEnabled( isChecked );
				conversations.save( conversation );

				Intent intent = Action.UPDATE_CONVERSATION.intent();
				Extra.PARTNER.to( intent, partner );
				getActivity().sendBroadcast( intent, Manifest.permission.READ );

			}

		}

	}

	protected void toggleSendReceipts( boolean isChecked ) {

		SilentTextApplication application = SilentTextApplication.from( getActivity() );

		ConversationRepository conversations = application.getConversations();
		if( conversations != null && conversations.exists() ) {

			Conversation conversation = conversations.findByPartner( partner );
			if( conversation != null ) {

				conversation.setSendReadReceipts( isChecked );
				conversations.save( conversation );

				Intent intent = Action.UPDATE_CONVERSATION.intent();
				Extra.PARTNER.to( intent, partner );
				getActivity().sendBroadcast( intent, Manifest.permission.READ );

			}

		}

	}

}
