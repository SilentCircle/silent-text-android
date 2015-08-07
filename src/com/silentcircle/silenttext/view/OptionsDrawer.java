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
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.UserPreferences;
import com.silentcircle.silenttext.preference.RingtonePreference;
import com.silentcircle.silenttext.preference.RingtonePreference.OnRingtoneChangeListener;
import com.silentcircle.silenttext.util.InactivityTimeout;

public class OptionsDrawer extends ScrollView implements OnRingtoneChangeListener {

	public static abstract class SetApplicationPreferenceAndToggleSiblingVisibilityOnCheckedChange extends SetApplicationPreferenceOnCheckedChange {

		protected static void setSiblingVisibility( View view, int siblingViewResourceID, boolean visible ) {
			if( view != null ) {
				View parent = (View) view.getParent();
				if( parent != null ) {
					View sibling = parent.findViewById( siblingViewResourceID );
					if( sibling != null ) {
						sibling.setVisibility( visible ? View.VISIBLE : View.GONE );
					}
				}
			}
		}

		private final int siblingViewResourceID;

		public SetApplicationPreferenceAndToggleSiblingVisibilityOnCheckedChange( int siblingViewResourceID ) {
			this.siblingViewResourceID = siblingViewResourceID;
		}

		@Override
		public void onCheckedChanged( CompoundButton view, boolean enabled ) {
			super.onCheckedChanged( view, enabled );
			setSiblingVisibility( view, siblingViewResourceID, enabled );
		}

	}

	public static abstract class SetApplicationPreferenceOnCheckedChange extends SetPreferenceOnCheckedChange {

		@Override
		protected UserPreferences getPreferences( Context context ) {
			return SilentTextApplication.from( context ).getGlobalPreferences();
		}

		@Override
		protected void savePreferences( Context context, UserPreferences preferences ) {
			SilentTextApplication.from( context ).saveApplicationPreferences( preferences );
		}

	}

	public static abstract class SetPreferenceOnCheckedChange implements OnCheckedChangeListener {

		protected abstract UserPreferences getPreferences( Context context );

		@Override
		public void onCheckedChanged( CompoundButton view, boolean enabled ) {
			setPreference( view, enabled );
		}

		protected abstract void savePreferences( Context context, UserPreferences preferences );

		private void setPreference( Context context, boolean enabled ) {
			UserPreferences preferences = getPreferences( context );
			if( preferences != null ) {
				if( setPreference( preferences, enabled ) ) {
					savePreferences( context, preferences );
				}
			}
		}

		protected abstract boolean setPreference( UserPreferences preferences, boolean enabled );

		private void setPreference( View view, boolean enabled ) {
			setPreference( view == null ? null : view.getContext(), enabled );
		}

	}

	public static abstract class SetUserPreferenceOnCheckedChange extends SetPreferenceOnCheckedChange {

		@Override
		protected UserPreferences getPreferences( Context context ) {
			return SilentTextApplication.from( context ).getUserPreferences();
		}

		@Override
		protected void savePreferences( Context context, UserPreferences preferences ) {
			SilentTextApplication.from( context ).saveUserPreferences( preferences );
		}

	}

	private static final String KEY_INACTIVITY_TIMEOUT = "inactivity_timeout";
	private static final String KEY_PASSCODE_SET = "passcode_set";
	private static final String KEY_SEND_RECEIPTS = "send_receipts";
	private static final String PREFERENCES = "passcode_options";

	public static UserPreferences getApplicationPreferences( Context context ) {
		SilentTextApplication application = SilentTextApplication.from( context );
		UserPreferences preferences = application.getGlobalPreferences();
		return preferences == null ? application.createDefaultApplicationPreferences() : preferences;
	}

	public static int getInactivityTimeout( Context context ) {
		UserPreferences preferences = getApplicationPreferences( context );
		return preferences.isPasscodeSet ? preferences.passcodeUnlockValidityPeriod : -1;
	}

	public static int getInactivityTimeoutLegacy( Context context ) {
		SharedPreferences preferences = getPreferencesLegacy( context );
		if( !preferences.getBoolean( KEY_PASSCODE_SET, false ) ) {
			return -1;
		}
		return preferences.getInt( KEY_INACTIVITY_TIMEOUT, -1 );
	}

	@Deprecated
	private static boolean getPreferenceLegacy( Context context, String key, boolean defaultValue ) {
		return getPreferencesLegacy( context ).getBoolean( key, defaultValue );
	}

	@Deprecated
	private static SharedPreferences getPreferencesLegacy( Context context ) {
		return context.getSharedPreferences( PREFERENCES, Context.MODE_PRIVATE );
	}

	public static UserPreferences getUserPreferences( Context context ) {
		SilentTextApplication application = SilentTextApplication.from( context );
		UserPreferences preferences = application.getUserPreferences();
		return preferences == null ? application.createDefaultUserPreferences() : preferences;
	}

	public static boolean isEmptyPasscode( Context context ) {
		UserPreferences preferences = getApplicationPreferences( context );
		return !preferences.isPasscodeSet;
	}

	public static boolean isEmptyPasscodeLegacy( Context context ) {
		return !getPreferenceLegacy( context, KEY_PASSCODE_SET, false );
	}

	public static boolean isSendReceiptsEnabled( Context context ) {
		UserPreferences preferences = getUserPreferences( context );
		return preferences.sendDeliveryAcknowledgments;
	}

	public static boolean isSendReceiptsEnabledLegacy( Context context ) {
		return getPreferenceLegacy( context, KEY_SEND_RECEIPTS, false );
	}

	protected static void saveApplicationPreferences( Context context, UserPreferences preferences ) {
		SilentTextApplication.from( context ).saveApplicationPreferences( preferences );
	}

	protected static void saveUserPreferences( Context context, UserPreferences preferences ) {
		SilentTextApplication.from( context ).saveUserPreferences( preferences );
	}

	public static void setEmptyPasscode( Context context, boolean emptyPasscode ) {
		UserPreferences preferences = getApplicationPreferences( context );
		preferences.isPasscodeSet = !emptyPasscode;
		saveApplicationPreferences( context, preferences );
	}

	public static void setInactivityTimeout( Context context, int inactivityTimeout ) {
		UserPreferences preferences = getApplicationPreferences( context );
		preferences.passcodeUnlockValidityPeriod = inactivityTimeout;
		saveApplicationPreferences( context, preferences );
	}

	public static void setSendReceipts( Context context, boolean sendReceipts ) {
		UserPreferences preferences = getUserPreferences( context );
		preferences.sendDeliveryAcknowledgments = sendReceipts;
		saveUserPreferences( context, preferences );
	}

	protected static void setText( View view, CharSequence text, CharSequence hint ) {
		if( view instanceof TextView ) {
			( (TextView) view ).setText( text );
		}
		if( view instanceof EditText ) {
			( (EditText) view ).setHint( hint );
		}
	}

	private SoftReference<Activity> activityReference;
	private final RingtonePreference ringtonePreference = new RingtonePreference();

	public OptionsDrawer( Context context ) {
		super( context );
	}

	public OptionsDrawer( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public OptionsDrawer( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public void attach( Activity activity ) {
		activityReference = new SoftReference<Activity>( activity );
		update();
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

	protected long getInactivityTimeout() {
		return getInactivityTimeout( getContext() );
	}

	protected int getInactivityTimeoutLevel() {
		return InactivityTimeout.Defaults.getLevel( (int) getInactivityTimeout() );
	}

	public char [] getRingtoneName() {
		UserPreferences preferences = getApplicationPreferences( getContext() );
		if( preferences.notificationSound ) {
			if( preferences.ringtoneName == null ) {
				try {
					return RingtonePreference.getDefaultRingtone().toString().toCharArray();
				} catch( NullPointerException exception ) {
					return preferences.ringtoneName;
				}
			}
			return preferences.ringtoneName;
		}
		return preferences.ringtoneName;
	}

	protected SilentTextApplication getSilentTextApplication() {
		return SilentTextApplication.from( getContext() );
	}

	public void onActivityResult( int requestCode, int resultCode, Intent extras ) {
		ringtonePreference.onActivityResult( requestCode, resultCode, extras );
	}

	@Override
	public void onRingtoneChange( Uri ringtone ) {
		UserPreferences preferences = getApplicationPreferences( getContext() );
		preferences.ringtoneName = ringtone != null ? ringtone.toString().toCharArray() : null;
		preferences.notificationSound = ringtone != null;
		getSilentTextApplication().saveApplicationPreferences( preferences );
		( (TextView) findViewById( R.id.notification_ringtone_value ) ).setText( ringtonePreference.getRingtoneTitle( getContext() ) );
	}

	protected void pickRingtone() {
		ringtonePreference.pickRingtone( getActivity() );
	}

	private void prepareNotificationOptions() {

		ringtonePreference.setOnRingtoneChangeListener( null );
		ringtonePreference.setRingtone( getRingtoneName() );
		ringtonePreference.setOnRingtoneChangeListener( this );

		findViewById( R.id.notification_ringtone ).setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				pickRingtone();
			}

		} );

		( (TextView) findViewById( R.id.notification_ringtone_value ) ).setText( ringtonePreference.getRingtoneTitle( getContext() ) );

	}

	protected void setInactivityTimeout( int timeout ) {
		setInactivityTimeout( getContext(), timeout );
	}

	protected void setInactivityTimeoutLevel( int level ) {
		setInactivityTimeout( InactivityTimeout.Defaults.getDelay( level ) );
	}

	protected void setText( int viewResourceID, CharSequence text ) {
		setText( viewResourceID, text, null );
	}

	protected void setText( int viewResourceID, CharSequence text, CharSequence hint ) {
		setText( findViewById( viewResourceID ), text, hint );
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

	public void update() {
		prepareNotificationOptions();
	}

}
