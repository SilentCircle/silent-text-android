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
package com.silentcircle.silenttext.fragment;

import java.lang.ref.SoftReference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.activity.LicenseActivity;
import com.silentcircle.silenttext.activity.LockActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.dialog.EditTextDialog;
import com.silentcircle.silenttext.dialog.EditTextDialog.Callback;
import com.silentcircle.silenttext.listener.ConfirmOnClick;
import com.silentcircle.silenttext.listener.DeactivateApplicationOnConfirm;
import com.silentcircle.silenttext.listener.DismissDialogOnClick;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.UserPreferences;
import com.silentcircle.silenttext.preference.SilentRingtonePreference;
import com.silentcircle.silenttext.receiver.LockApplicationOnReceive;
import com.silentcircle.silenttext.receiver.NotificationBroadcaster;
import com.silentcircle.silenttext.task.ResetPassphraseTask;
import com.silentcircle.silenttext.transport.TransportQueue;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.DeviceUtils;
import com.silentcircle.silenttext.util.InactivityTimeout;
import com.silentcircle.silenttext.view.OptionsDrawer;

public abstract class SettingsFragmentHelper implements OnSharedPreferenceChangeListener, OnPreferenceClickListener, OnCheckedChangeListener, OnCancelListener, DialogInterface.OnClickListener, Callback, OnPreferenceChangeListener {

	public static class DeactivateApplicationAndRemoveAccountPreferenceOnConfirm extends DeactivateApplicationOnConfirm {

		public static interface Callback {

			public PreferenceScreen getPreferenceScreen();

			public String getUsername();

			public boolean shouldRemoveUserData();

			public void shouldRemoveUserData( boolean shouldRemoveUserData );

		}

		private final Callback callback;

		public DeactivateApplicationAndRemoveAccountPreferenceOnConfirm( Callback callback ) {
			this.callback = callback;
		}

		@Override
		public void onConfirm( Context context ) {

			setRemoveUserData( callback.shouldRemoveUserData() );
			super.onConfirm( context );
			callback.shouldRemoveUserData( false );

			toast( context, R.string.notify_account_removed );

			PreferenceScreen screen = callback.getPreferenceScreen();

			if( screen != null ) {

				PreferenceCategory accounts = (PreferenceCategory) screen.findPreference( "accounts" );

				if( accounts != null ) {

					remove( accounts, callback.getUsername() );

					if( accounts.getPreferenceCount() < 1 ) {
						screen.removePreference( accounts );
						PreferenceGroup notificationPreferences = (PreferenceGroup) screen.findPreference( "notifications" );
						remove( notificationPreferences, "should_send_read_receipts" );
					}

				}

			}

		}

	}

	public static class DeactivateApplicationAndRemoveAccountPreferenceOnConfirmCallback implements DeactivateApplicationAndRemoveAccountPreferenceOnConfirm.Callback {

		private final SoftReference<SettingsFragmentHelper> settingsReference;
		private final String username;

		public DeactivateApplicationAndRemoveAccountPreferenceOnConfirmCallback( SettingsFragmentHelper settings, String username ) {
			settingsReference = new SoftReference<SettingsFragmentHelper>( settings );
			this.username = username;
		}

		@Override
		public PreferenceScreen getPreferenceScreen() {
			SettingsFragmentHelper settings = settingsReference.get();
			return settings != null ? settings.getPreferenceScreen() : null;
		}

		@Override
		public String getUsername() {
			return username;
		}

		@Override
		public boolean shouldRemoveUserData() {
			SettingsFragmentHelper settings = settingsReference.get();
			return settings != null && settings.shouldRemoveUserData();
		}

		@Override
		public void shouldRemoveUserData( boolean shouldRemoveUserData ) {
			SettingsFragmentHelper settings = settingsReference.get();
			if( settings != null ) {
				settings.shouldRemoveUserData( shouldRemoveUserData );
			}
		}

	}

	public static class OnPassphraseResetCallback implements ResetPassphraseAndToastTask.Callback {

		private final SoftReference<SettingsFragmentHelper> settingsReference;

		public OnPassphraseResetCallback( SettingsFragmentHelper settings ) {
			settingsReference = new SoftReference<SettingsFragmentHelper>( settings );
		}

		@Override
		public void onAfterPassphraseReset() {
			SettingsFragmentHelper settings = settingsReference.get();
			if( settings != null ) {
				settings.update();
			}
		}

		@Override
		public void onPostExecute() {
			SettingsFragmentHelper settings = settingsReference.get();
			if( settings != null ) {
				settings.setPendingPassphraseReset( false );
			}
		}

	}

	public static class ResetPassphraseAndToastTask extends ResetPassphraseTask {

		public static interface Callback {

			public void onAfterPassphraseReset();

			public void onPostExecute();

		}

		private final Callback callback;

		public ResetPassphraseAndToastTask( Context context, Callback callback ) {
			super( context );
			this.callback = callback;
		}

		@Override
		protected void onPassphraseReset() {
			Context context = getContext();

			// Passcode is removed.
			SilentTextApplication app = SilentTextApplication.from( context );
			SharedPreferences prefs = app.getSharedPreferences( LockActivity.PASS_CODE_SET, Context.MODE_PRIVATE );
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean( LockActivity.PASS_CODE_SET, false ).apply();

			OptionsDrawer.setEmptyPasscode( context, true );
			LockApplicationOnReceive.cancel( context );
			toast( context, R.string.notify_passcode_removed );
			callback.onAfterPassphraseReset();
		}

		@Override
		protected void onPassphraseResetFailed() {
			toast( getContext(), R.string.error_incorrect_passcode );
			callback.onAfterPassphraseReset();
		}

		@Override
		protected void onPostExecute( Boolean success ) {
			callback.onPostExecute();
			super.onPostExecute( success );
		}

	}

	public static class SetInactivityTimeoutOnSeekBarChange implements OnSeekBarChangeListener {

		private final SoftReference<View> viewReference;

		public SetInactivityTimeoutOnSeekBarChange( View view ) {
			viewReference = new SoftReference<View>( view );
		}

		@Override
		public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
			View view = viewReference.get();
			if( view != null ) {
				( (TextView) view.findViewById( R.id.inactivity_timeout_label ) ).setText( InactivityTimeout.Defaults.getLabel( seekBar.getContext(), progress ) );
			}
		}

		@Override
		public void onStartTrackingTouch( SeekBar seekBar ) {
			// Ignore this.
		}

		@Override
		public void onStopTrackingTouch( SeekBar seekBar ) {
			OptionsDrawer.setInactivityTimeout( seekBar.getContext(), InactivityTimeout.Defaults.getDelay( seekBar.getProgress() ) );
		}

	}

	private static Preference addPreferenceIfNecessary( PreferenceGroup parent, String key, int titleResourceID, int summaryResourceID, OnPreferenceClickListener onPreferenceClickListener ) {
		Preference preference = addPreferenceIfNecessary( parent, key, titleResourceID, onPreferenceClickListener );
		if( preference != null ) {
			preference.setSummary( summaryResourceID );
		}
		return preference;
	}

	private static Preference addPreferenceIfNecessary( PreferenceGroup parent, String key, int titleResourceID, OnPreferenceClickListener onPreferenceClickListener ) {
		Preference preference = parent.findPreference( key );
		if( preference == null ) {
			preference = createPreference( parent.getContext(), key, titleResourceID, onPreferenceClickListener );
			parent.addPreference( preference );
		}
		return preference;
	}

	private static Preference createPreference( Context context, String key, int titleResourceID, OnPreferenceClickListener onPreferenceClickListener ) {
		Preference preference = new Preference( context );
		preference.setKey( key );
		preference.setTitle( titleResourceID );
		preference.setOnPreferenceClickListener( onPreferenceClickListener );
		return preference;
	}

	protected static void remove( PreferenceGroup parent, String preferenceKey ) {
		if( parent != null && preferenceKey != null ) {
			Preference preference = parent.findPreference( preferenceKey );
			if( preference != null ) {
				parent.removePreference( preference );
			}
		}
	}

	private static boolean runJNITest( SilentTextApplication application, Context context, String key ) {
		if( key == null || !key.startsWith( "test_" ) ) {
			return false;
		}

		AlertDialog.Builder alert = new AlertDialog.Builder( context );
		alert.setTitle( "SCimp JNI Tests" );
		String messageS = "";

		if( "test_pki_keygen".equals( key ) ) {
			final String username = application.getUsername();
			byte [] result = application.getKeyGenerator().generateKey( username != null ? username : "alice@silentcircle.com", application.getLocalStorageKey() );
			if( result != null ) {
				alert.setTitle( "Generated Key Pair" );
				messageS = new String( result );
			}
		} else {
			int err = 0;

			if( "test_scimp_DHCommunication".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpDHCommunication();
			} else if( "test_scimp_DHSimultaneousCommunication".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpDHSimultaneousCommunication();
			} else if( "test_scimp_KeySerializer".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpKeySerializer();
			} else if( "test_scimp_OfflinePKCommunication".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpOfflinePKCommunication();
			} else if( "test_scimp_PKCommunication".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpPKCommunication();
			} else if( "test_scimp_PKContention".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpPKContention();
			} else if( "test_scimp_PKExpiration".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpPKExpiration();
			} else if( "test_scimp_PKSaveRestore".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpPKSaveRestore();
			} else if( "test_scimp_SimultaneousPKCommunication".equals( key ) ) {
				err = application.getSCimpBridge().testSCimpSimultaneousPKCommunication();
			}

			messageS = err == 0 ? "Tests successful." : "Tests failed with error: " + err;
		}

		if( !messageS.isEmpty() ) {
			alert.setMessage( messageS );
			alert.show();
		}
		return true;
	}

	@TargetApi( Build.VERSION_CODES.ICE_CREAM_SANDWICH )
	private static void setChecked( PreferenceGroup parent, String preferenceKey, boolean checked ) {
		Preference p = parent.findPreference( preferenceKey );
		if( p instanceof CheckBoxPreference ) {
			( (CheckBoxPreference) p ).setChecked( checked );
		} else if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ) {
			if( p instanceof TwoStatePreference ) {
				( (TwoStatePreference) p ).setChecked( checked );
			}
		}
		if( p != null ) {
			p.setDefaultValue( Boolean.valueOf( checked ) );
		}
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	private static void setIconCompat( Preference preference, int iconResourceID ) {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
			preference.setIcon( iconResourceID );
		}
	}

	private static void setInactivityTimeout( Preference preference, int delay ) {
		Context context = preference.getContext();
		preference.setDefaultValue( Integer.toString( delay ) );
		preference.setSummary( InactivityTimeout.Defaults.getLabel( context, InactivityTimeout.Defaults.getLevel( delay ) ) );
	}

	protected static void toast( Context context, int messageResourceID ) {
		Toast.makeText( context, messageResourceID, Toast.LENGTH_SHORT ).show();
	}

	protected static void toast( Context context, String message ) {
		Toast.makeText( context, message, Toast.LENGTH_SHORT ).show();
	}

	private boolean removeUserData;
	private boolean pendingPassphraseReset;

	protected abstract void addPreferencesFromResource( int xmlResourceID );

	protected Context getContext() {
		PreferenceScreen screen = getPreferenceScreen();
		return screen != null ? screen.getContext() : null;
	}

	protected abstract PreferenceScreen getPreferenceScreen();

	public void onActivityResult( int requestCode, int resultCode, Intent data ) {
		if( data != null ) {
			Log.i( "SettingsFragmentHelper", "onActivityResult: " + requestCode + " : " + resultCode + " : " + data.toString() );
		}
	}

	@Override
	public void onCancel( DialogInterface dialog ) {
		update( getPreferenceScreen() );
	}

	@Override
	public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
		shouldRemoveUserData( !isChecked );
	}

	@Override
	public void onClick( DialogInterface dialog, int which ) {
		update( getPreferenceScreen() );
	}

	/**
	 * @param savedInstanceState
	 */
	public void onCreate( Bundle savedInstanceState ) {

		restoreState( savedInstanceState );

		addPreferencesFromResource( R.xml.preferences );
		PreferenceScreen screen = getPreferenceScreen();

		registerClick( screen, "clear_user_cache" );
		registerClick( screen, "clear_object_cache" );
		registerClick( screen, "clear_network_cache" );
		registerClick( screen, "clear_packet_queue" );
		registerClick( screen, "clear_key_pairs" );
		registerClick( screen, "license_information" );
		registerClick( screen, "privacy_statement" );
		registerClick( screen, "terms_and_conditions" );
		registerClick( screen, "send_feedback" );
		registerClick( screen, "set_passcode" );
		registerClick( screen, "change_passcode" );
		registerClick( screen, "remove_passcode" );
		registerClick( screen, "autolock" );

		registerChange( screen, "notification_vibrate" );
		registerChange( screen, "should_send_read_receipts" );

		// tests
		registerClick( screen, "test_pki_keygen" );
		registerClick( screen, "test_scimp_DHCommunication" );
		registerClick( screen, "test_scimp_DHSimultaneousCommunication" );
		registerClick( screen, "test_scimp_KeySerializer" );
		registerClick( screen, "test_scimp_OfflinePKCommunication" );
		registerClick( screen, "test_scimp_PKCommunication" );
		registerClick( screen, "test_scimp_PKContention" );
		registerClick( screen, "test_scimp_PKExpiration" );
		registerClick( screen, "test_scimp_PKSaveRestore" );
		registerClick( screen, "test_scimp_SimultaneousPKCommunication" );

	}

	public void onDestroy() {
		// By default, do nothing.
	}

	public void onPause() {
		PreferenceScreen screen = getPreferenceScreen();
		screen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener( this );
		SilentTextApplication.from( screen.getContext() ).sendToBackground();
	}

	@Override
	public boolean onPreferenceChange( Preference preference, Object newValue ) {

		String key = preference.getKey();
		Context context = preference.getContext();
		SilentTextApplication application = SilentTextApplication.from( context );

		if( "notification_vibrate".equals( key ) ) {
			UserPreferences preferences = application.getGlobalPreferences();
			preferences.notificationVibrate = newValue != null && ( (Boolean) newValue ).booleanValue();
			application.saveApplicationPreferences( preferences );
			return true;
		}

		if( "should_send_read_receipts".equals( key ) ) {
			OptionsDrawer.setSendReceipts( context, newValue != null && ( (Boolean) newValue ).booleanValue() );
			return true;
		}

		return true;

	}

	@Override
	public boolean onPreferenceClick( Preference preference ) {

		String key = preference.getKey();
		Context context = preference.getContext();
		SilentTextApplication application = SilentTextApplication.from( context );

		if( "clear_user_cache".equals( key ) ) {
			application.getUsers().clear();
			toast( context, "User cache cleared." );
			return true;
		}

		if( "clear_packet_queue".equals( key ) ) {
			TransportQueue queue = application.getOutgoingMessageQueue();
			if( queue instanceof Repository ) {
				( (Repository<?>) queue ).clear();
				toast( context, "Packet queue cleared." );
				return true;
			}
		}

		if( "clear_object_cache".equals( key ) ) {
			application.getAttachments().clear();
			toast( context, "Object cache cleared." );
			return true;
		}

		if( "clear_network_cache".equals( key ) ) {
			application.getServiceEndpointRepository().clear();
			toast( context, "Network cache cleared." );
			return true;
		}

		if( "clear_key_pairs".equals( key ) ) {
			application.revokeAllKeyPairs();
			toast( context, "Revoking all published public keys and clearing their corresponding private keys from local storage." );
			return true;

		}

		if( "license_information".equals( key ) ) {
			context.startActivity( new Intent( context, LicenseActivity.class ) );
			return true;
		}

		if( "privacy_statement".equals( key ) ) {
			context.startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( context.getResources().getString( R.string.sc_privacy_link ) ) ) );
			return true;
		}

		if( "terms_and_conditions".equals( key ) ) {
			context.startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( context.getResources().getString( R.string.sc_tos_link ) ) ) );
			return true;
		}

		if( "autolock".equals( key ) ) {

			AlertDialog.Builder alert = new AlertDialog.Builder( context );

			final View view = LayoutInflater.from( context ).inflate( R.layout.inactivity_timeout, null );

			SeekBar seeker = (SeekBar) view.findViewById( R.id.inactivity_timeout_seeker );

			seeker.setProgress( InactivityTimeout.Defaults.getLevel( OptionsDrawer.getInactivityTimeout( context ) ) );

			( (TextView) view.findViewById( R.id.inactivity_timeout_label ) ).setText( InactivityTimeout.Defaults.getLabel( context, seeker.getProgress() ) );

			seeker.setOnSeekBarChangeListener( new SetInactivityTimeoutOnSeekBarChange( view ) );

			alert.setTitle( preference.getTitle() );
			alert.setView( view );
			alert.setOnCancelListener( this );
			alert.setPositiveButton( android.R.string.ok, this );
			alert.show();

			return true;

		}

		if( "set_passcode".equals( key ) ) {
			context.startActivity( new Intent( context, LockActivity.class ) );
			return true;
		}

		if( "change_passcode".equals( key ) ) {
			context.startActivity( new Intent( context, LockActivity.class ) );
			return true;
		}

		if( "remove_passcode".equals( key ) ) {
			EditTextDialog.show( context, R.id.remove_passcode, R.string.current_passcode, "", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, this );
			return true;
		}

		final String username = application.getUsername();

		if( username != null && username.equals( key ) ) {

			AlertDialog.Builder alert = new AlertDialog.Builder( context );
			LinearLayout layout = (LinearLayout) LayoutInflater.from( context ).inflate( R.layout.dialog_with_checkbox, null );

			( (TextView) layout.findViewById( R.id.message ) ).setText( context.getString( R.string.confirm_account_removal, username ) );
			( (CheckBox) layout.findViewById( R.id.checkbox ) ).setOnCheckedChangeListener( this );
			( (CheckBox) layout.findViewById( R.id.checkbox ) ).setChecked( !shouldRemoveUserData() );

			alert.setTitle( R.string.are_you_sure );
			alert.setView( layout );
			alert.setNegativeButton( R.string.cancel, new DismissDialogOnClick() );
			alert.setPositiveButton( R.string.remove, new ConfirmOnClick( new DeactivateApplicationAndRemoveAccountPreferenceOnConfirm( new DeactivateApplicationAndRemoveAccountPreferenceOnConfirmCallback( this, username ) ) ) );

			alert.show();

			return true;

		}

		if( "send_feedback".equals( key ) ) {
			DeviceUtils.shareDebugInformation( context );
			return true;
		}

		if( key != null && key.startsWith( "test_" ) ) {
			runJNITest( application, context, key );
			return true;
		}

		return false;

	}

	public void onResume() {
		PreferenceScreen screen = getPreferenceScreen();
		update( screen );
		screen.getSharedPreferences().registerOnSharedPreferenceChangeListener( this );
		SilentTextApplication.from( screen.getContext() ).sendToForeground();
	}

	@Override
	public void onSharedPreferenceChanged( SharedPreferences preferences, String key ) {
		update( getPreferenceScreen() );
	}

	@Override
	public void onValue( int dialogID, CharSequence value ) {

		if( R.id.remove_passcode == dialogID ) {

			pendingPassphraseReset = true;
			update();
			AsyncUtils.execute( new ResetPassphraseAndToastTask( getContext(), new OnPassphraseResetCallback( this ) ), value, "" );

			return;

		}

	}

	private void registerChange( PreferenceScreen screen, String preferenceKey ) {
		Preference preference = screen.findPreference( preferenceKey );
		if( preference != null ) {
			preference.setOnPreferenceChangeListener( this );
		}
	}

	private void registerClick( PreferenceScreen screen, String preferenceKey ) {
		screen.findPreference( preferenceKey ).setOnPreferenceClickListener( this );
	}

	public void restoreState( Bundle savedInstanceState ) {
		if( savedInstanceState != null ) {
			removeUserData = savedInstanceState.getBoolean( "removeUserData" );
			pendingPassphraseReset = savedInstanceState.getBoolean( "pendingPassphraseReset" );
		}
	}

	public void saveState( Bundle outState ) {
		outState.putBoolean( "removeUserData", removeUserData );
		outState.putBoolean( "pendingPassphraseReset", pendingPassphraseReset );
	}

	public void setPendingPassphraseReset( boolean pendingPassphraseReset ) {
		this.pendingPassphraseReset = pendingPassphraseReset;
	}

	protected boolean shouldRemoveUserData() {
		return removeUserData;
	}

	protected void shouldRemoveUserData( boolean removeUserData ) {
		this.removeUserData = removeUserData;
	}

	protected void update() {
		update( getPreferenceScreen() );
	}

	private void update( PreferenceScreen screen ) {

		ServiceConfiguration.getInstance().update( screen );

		Context context = screen.getContext();
		SilentTextApplication application = SilentTextApplication.from( context );

		if( application.isUserKeyUnlocked() ) {
			PreferenceCategory accounts = (PreferenceCategory) screen.findPreference( "accounts" );
			if( accounts != null ) {
				accounts.removeAll();
				Preference account = new Preference( context );
				String username = application.getUsername();
				account.setKey( username );
				account.setDefaultValue( username );
				account.setTitle( username );
				account.setSummary( R.string.tap_to_remove );
				setIconCompat( account, R.drawable.ic_action_delete );
				account.setPersistent( false );
				account.setOnPreferenceClickListener( this );
				accounts.addPreference( account );
			}
		} else {
			remove( screen, "accounts" );
		}

		PreferenceGroup passcodePreferences = (PreferenceGroup) screen.findPreference( "passcode" );

		if( !OptionsDrawer.isEmptyPasscode( screen.getContext() ) ) {
			remove( passcodePreferences, "set_passcode" );
			addPreferenceIfNecessary( passcodePreferences, "autolock", R.string.autolock, this ).setEnabled( !pendingPassphraseReset );
			addPreferenceIfNecessary( passcodePreferences, "change_passcode", R.string.change_passcode, this ).setEnabled( !pendingPassphraseReset );
			addPreferenceIfNecessary( passcodePreferences, "remove_passcode", R.string.remove_passcode, R.string.remove_passcode_description, this ).setEnabled( !pendingPassphraseReset );
			setInactivityTimeout( passcodePreferences.findPreference( "autolock" ), OptionsDrawer.getInactivityTimeout( context ) );
		} else {
			addPreferenceIfNecessary( passcodePreferences, "set_passcode", R.string.set_passcode, R.string.set_passcode_description, this );
			remove( passcodePreferences, "autolock" );
			remove( passcodePreferences, "change_passcode" );
			remove( passcodePreferences, "remove_passcode" );
		}

		if( !ServiceConfiguration.getInstance().debug ) {
			remove( screen, "developer_options" );
		}

		PreferenceGroup notificationPreferences = (PreferenceGroup) screen.findPreference( "notifications" );

		if( application.getUserPreferences() == null ) {
			remove( notificationPreferences, "should_send_read_receipts" );
		}

		setChecked( screen, "should_send_read_receipts", OptionsDrawer.isSendReceiptsEnabled( context ) );
		setChecked( screen, "notification_vibrate", NotificationBroadcaster.isVibrateEnabled( context ) );

		( (SilentRingtonePreference) screen.findPreference( "notification_ringtone" ) ).updateSummary();

		PreferenceGroup debugInformation = (PreferenceGroup) screen.findPreference( "debug_information" );
		DeviceUtils.putDebugInformation( debugInformation );

	}
}
