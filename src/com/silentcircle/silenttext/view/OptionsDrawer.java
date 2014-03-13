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

import java.lang.ref.SoftReference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.AdvancedActivity;
import com.silentcircle.silenttext.activity.LicenseActivity;
import com.silentcircle.silenttext.activity.LockActivity;
import com.silentcircle.silenttext.activity.PrivacyPolicyActivity;
import com.silentcircle.silenttext.activity.UnlockActivity;
import com.silentcircle.silenttext.listener.DeactivateApplicationOnConfirm;
import com.silentcircle.silenttext.listener.LaunchActivityOnClick;
import com.silentcircle.silenttext.listener.LaunchConfirmDialogOnClick;
import com.silentcircle.silenttext.listener.LockApplicationOnClick;
import com.silentcircle.silenttext.listener.SetPreferenceAndSiblingVisibilityOnChecked;
import com.silentcircle.silenttext.listener.SetPreferenceOnChecked;
import com.silentcircle.silenttext.receiver.NotificationBroadcaster;
import com.silentcircle.silenttext.util.InactivityTimeout;

public class OptionsDrawer extends ScrollView {

	private static final String KEY_ENABLE_SCREENSHOTS = "enable_screenshots";
	private static final String KEY_INACTIVITY_TIMEOUT = "inactivity_timeout";
	private static final String KEY_PASSCODE_SET = "passcode_set";
	private static final String KEY_SEND_RECEIPTS = "send_receipts";

	private static final String PREFERENCES = "passcode_options";

	public static int getInactivityTimeout( Context context ) {
		SharedPreferences preferences = getPreferences( context );
		if( !preferences.getBoolean( KEY_PASSCODE_SET, false ) ) {
			return -1;
		}
		return preferences.getInt( KEY_INACTIVITY_TIMEOUT, -1 );
	}

	private static boolean getPreference( Context context, String key, boolean defaultValue ) {
		return getPreferences( context ).getBoolean( key, defaultValue );
	}

	private static SharedPreferences getPreferences( Context context ) {
		return context.getSharedPreferences( PREFERENCES, Context.MODE_PRIVATE );
	}

	public static boolean isEmptyPasscode( Context context ) {
		return !getPreference( context, KEY_PASSCODE_SET, false );
	}

	public static boolean isSecureOutputRequired( Context context ) {
		return !getPreference( context, KEY_ENABLE_SCREENSHOTS, false );
	}

	public static boolean isSendReceiptsEnabled( Context context ) {
		return getPreference( context, KEY_SEND_RECEIPTS, false );
	}

	public static void setEmptyPasscode( Context context, boolean emptyPasscode ) {
		setPreference( context, KEY_PASSCODE_SET, !emptyPasscode );
	}

	private static void setPreference( Context context, String key, boolean value ) {
		getPreferences( context ).edit().putBoolean( key, value ).commit();
	}

	private static void setPreference( Context context, String key, int value ) {
		getPreferences( context ).edit().putInt( key, value ).commit();
	}

	public static void setSendReceipts( Context context, boolean sendReceipts ) {
		setPreference( context, KEY_SEND_RECEIPTS, sendReceipts );
	}

	private SoftReference<Activity> activityReference;

	public OptionsDrawer( Context context ) {
		super( context );
	}

	public OptionsDrawer( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public OptionsDrawer( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	private void assign( int viewResourceID, OnCheckedChangeListener onCheckedChangeListener ) {
		( (CheckBox) findViewById( viewResourceID ) ).setOnCheckedChangeListener( onCheckedChangeListener );
	}

	private void assign( int viewResourceID, String preferences, String preferencesKey ) {
		assign( viewResourceID, new SetPreferenceOnChecked( preferences, preferencesKey ) );
	}

	public void attach( Activity activity ) {
		activityReference = new SoftReference<Activity>( activity );
		prepareNotificationOptions();
		preparePrivacyOptions();
		prepareAboutInformation();
		prepareDeactivationOption();
	}

	private Intent createRemovePasscodeIntent() {
		Intent intent = new Intent( getActivity(), UnlockActivity.class );
		Extra.FORCE.flag( intent );
		Extra.NEXT.to( intent, Extra.SILENT.flag( new Intent( getActivity(), LockActivity.class ) ) );
		return intent;
	}

	private Intent createSetPasscodeIntent() {
		Intent intent = new Intent( getActivity(), UnlockActivity.class );
		Extra.FORCE.flag( intent );
		Extra.NEXT.to( intent, new Intent( getActivity(), LockActivity.class ) );
		return intent;
	}

	public void detach() {
		activityReference = null;
	}

	private Activity getActivity() {
		if( activityReference == null ) {
			return null;
		}
		Activity activity = activityReference.get();
		if( activity == null ) {
			activityReference = null;
		}
		return activity;
	}

	protected int getInactivityTimeout() {
		return getInactivityTimeout( getContext() );
	}

	protected int getInactivityTimeoutLevel() {
		return InactivityTimeout.Defaults.getLevel( getInactivityTimeout() );
	}

	public void onPasscodeUpdate() {

		Context context = getContext();

		if( isEmptyPasscode( context ) ) {
			setText( R.id.set_passcode, R.string.set_passcode );
			setVisibleIf( false, R.id.lock, R.id.inactivity_timeout, R.id.remove_passcode );
			setVisibleIf( true, R.id.enable_screenshots );
		} else {
			setText( R.id.set_passcode, R.string.change_passcode );
			setVisibleIf( true, R.id.lock, R.id.inactivity_timeout, R.id.remove_passcode );
			setVisibleIf( false, R.id.enable_screenshots );
		}

	}

	@Override
	protected void onWindowVisibilityChanged( int visibility ) {
		super.onWindowVisibilityChanged( visibility );
		setVisibleIf( false, R.id.enable_screenshots_pending );
	}

	private void prepareAboutInformation() {
		findViewById( R.id.privacy_policy ).setOnClickListener( new LaunchActivityOnClick( getActivity(), PrivacyPolicyActivity.class ) );
		findViewById( R.id.license ).setOnClickListener( new LaunchActivityOnClick( getActivity(), LicenseActivity.class ) );
		findViewById( R.id.advanced ).setOnClickListener( new LaunchActivityOnClick( getActivity(), AdvancedActivity.class ) );
	}

	private void prepareDeactivationOption() {
		findViewById( R.id.deactivate ).setOnClickListener( new LaunchConfirmDialogOnClick( R.string.are_you_sure, R.string.cannot_be_undone, getActivity(), new DeactivateApplicationOnConfirm() ) );
	}

	private void prepareNotificationOptions() {

		assign( R.id.enable_notifications, new SetPreferenceAndSiblingVisibilityOnChecked( NotificationBroadcaster.PREFERENCES, NotificationBroadcaster.KEY_ENABLE_NOTIFICATIONS, R.id.if_notifications_enabled ) );
		assign( R.id.enable_notification_sound, NotificationBroadcaster.PREFERENCES, NotificationBroadcaster.KEY_ENABLE_SOUND );
		assign( R.id.enable_notification_vibrate, NotificationBroadcaster.PREFERENCES, NotificationBroadcaster.KEY_ENABLE_VIBRATE );

		SharedPreferences preferences = NotificationBroadcaster.getPreferences( getContext() );

		setCheckedIf( NotificationBroadcaster.isEnabled( preferences ), R.id.enable_notifications );
		setCheckedIf( NotificationBroadcaster.isSoundEnabled( preferences ), R.id.enable_notification_sound );
		setCheckedIf( NotificationBroadcaster.isVibrateEnabled( preferences ), R.id.enable_notification_vibrate );

		setVisibleIf( NotificationBroadcaster.isEnabled( preferences ), R.id.if_notifications_enabled );

	}

	private void preparePrivacyOptions() {

		assign( R.id.send_receipts, PREFERENCES, KEY_SEND_RECEIPTS );

		assign( R.id.enable_screenshots, new SetPreferenceOnChecked( PREFERENCES, KEY_ENABLE_SCREENSHOTS ) {

			@Override
			public void onCheckedChanged( CompoundButton view, boolean isChecked ) {
				super.onCheckedChanged( view, isChecked );
				setVisibleIf( true, R.id.enable_screenshots_pending );
			}

		} );
		setCheckedIf( isSendReceiptsEnabled( getContext() ), R.id.send_receipts );
		setCheckedIf( !isSecureOutputRequired( getContext() ), R.id.enable_screenshots );
		setVisibleIf( false, R.id.enable_screenshots_pending );

		findViewById( R.id.set_passcode ).setOnClickListener( new LaunchActivityOnClick( getActivity(), createSetPasscodeIntent(), R.string.none ) );
		findViewById( R.id.remove_passcode ).setOnClickListener( new LaunchActivityOnClick( getActivity(), createRemovePasscodeIntent(), R.string.none ) );
		findViewById( R.id.lock ).setOnClickListener( new LockApplicationOnClick( getActivity() ) );

		SeekBar seeker = (SeekBar) findViewById( R.id.inactivity_timeout_seeker );
		seeker.setProgress( getInactivityTimeoutLevel() );

		setText( R.id.inactivity_timeout_label, InactivityTimeout.Defaults.getLabel( getContext(), seeker.getProgress() ) );

		seeker.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
				setText( R.id.inactivity_timeout_label, InactivityTimeout.Defaults.getLabel( getContext(), progress ) );
			}

			@Override
			public void onStartTrackingTouch( SeekBar seekBar ) {
				// Ignore this.
			}

			@Override
			public void onStopTrackingTouch( SeekBar seekBar ) {
				setInactivityTimeoutLevel( seekBar.getProgress() );
			}

		} );

	}

	private void setCheckedIf( boolean condition, int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			View view = findViewById( id );
			if( view instanceof Checkable ) {
				( (Checkable) view ).setChecked( condition );
			}
		}
	}

	protected void setInactivityTimeout( int timeout ) {
		setPreference( KEY_INACTIVITY_TIMEOUT, timeout );
	}

	protected void setInactivityTimeoutLevel( int level ) {
		setInactivityTimeout( InactivityTimeout.Defaults.getDelay( level ) );
	}

	private void setPreference( String key, int value ) {
		setPreference( getContext(), key, value );
	}

	protected void setText( int viewResourceID, CharSequence text ) {
		( (TextView) findViewById( viewResourceID ) ).setText( text );
	}

	protected void setText( int viewResourceID, int stringResourceID ) {
		( (TextView) findViewById( viewResourceID ) ).setText( stringResourceID );
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

}
