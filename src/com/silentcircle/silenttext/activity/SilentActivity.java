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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.silentcircle.api.model.Entitlement;
import com.silentcircle.api.model.User;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.NativeBridge;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.JabberClient;
import com.silentcircle.silenttext.client.SCloudBroker;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.listener.DismissOnClick;
import com.silentcircle.silenttext.listener.OnObjectReceiveListener;
import com.silentcircle.silenttext.listener.SOSListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.receiver.FinishActivityOnReceive;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.ServerRepository;
import com.silentcircle.silenttext.thread.ViewAnimator;
import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.adapter.ListAdapter;

public abstract class SilentActivity extends SherlockFragmentActivity {

	protected static final String ACTION_SILENT_CALL = "com.silentcircle.silentphone.action.NEW_OUTGOING_CALL";

	private static final int R_id_activate = 0xFFFF & R.id.activate;
	private static final int R_id_unlock = 0xFFFF & R.id.unlock;

	protected static Intent createIntentToCallUser( String remoteUserID ) {
		Intent intent = new Intent( ACTION_SILENT_CALL, Uri.fromParts( PROTOCOL_SILENT_PHONE, remoteUserID, null ) );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		return intent;
	}

	protected static boolean isDebuggable() {
		return ServiceConfiguration.getInstance().debug;
	}

	protected BroadcastReceiver lockReceiver;

	protected Runnable hideError = new ViewAnimator( this, R.id.error, R.anim.slide_up );
	protected Handler handler;
	protected Log log;
	protected final List<AsyncTask<?, ?, ?>> tasks = new ArrayList<AsyncTask<?, ?, ?>>();
	protected static final String PROTOCOL_SILENT_PHONE = "silenttel";
	protected AsyncTask<Void, Void, Void> isAccessibleTask;

	private static final String CACHE_STAGING_DIR_NAME = ".temp";

	protected void _invalidateOptionsMenu() {
		invalidateOptionsMenu( true );
	}

	protected void adviseReconnect() {
		getSilentTextApplication().adviseReconnect();
	}

	protected void autoUnlock() {
		getSilentTextApplication().unlock( new char [0] );
	}

	protected void beginLoading( int contentViewId ) {
		// getSupportActionBar().hide();
		findViewById( contentViewId ).setVisibility( View.GONE );
		findViewById( R.id.progress ).setVisibility( View.VISIBLE );
	}

	protected void clearTasks() {
		while( !tasks.isEmpty() ) {
			tasks.get( 0 ).cancel( true );
			tasks.remove( 0 );
		}
	}

	protected void closeDrawer() {
		DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer );
		drawer.closeDrawer( Gravity.END );
	}

	protected EditText findEditTextById( int viewResourceId ) {
		return (EditText) findViewById( viewResourceId );
	}

	protected ListView findListViewById( int viewResourceId ) {
		return (ListView) findViewById( viewResourceId );
	}

	protected TextView findTextViewById( int viewResourceId ) {
		return (TextView) findViewById( viewResourceId );
	}

	protected void finishLoading( int contentViewId ) {
		// getSupportActionBar().show();
		findViewById( contentViewId ).setVisibility( View.VISIBLE );
		findViewById( R.id.progress ).setVisibility( View.GONE );
	}

	protected CharSequence generateDeviceID() {
		return UUID.randomUUID().toString();
	}

	protected SilentActivity getActivity() {
		return this;
	}

	protected <T extends ListAdapter<?>> T getAdapter( int viewResourceId ) {
		return (T) findListViewById( viewResourceId ).getAdapter();
	}

	protected SCloudBroker getBroker() {
		return getSilentTextApplication().getBroker();
	}

	protected File getCacheStagingDir() {
		return new File( getCacheDir(), CACHE_STAGING_DIR_NAME );
	}

	protected ContactRepository getContacts() {
		return getSilentTextApplication().getContacts();
	}

	protected Conversation getConversation( String partner ) {
		if( partner == null ) {
			return null;
		}
		ConversationRepository conversations = getConversations();
		if( conversations == null ) {
			return null;
		}
		return conversations.findByPartner( partner );
	}

	protected ConversationRepository getConversations() {
		return getSilentTextApplication().getConversations();
	}

	protected Intent getDebugSettingsIntent() {
		return isDebuggable() ? new Intent( this, Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? LegacySettingsActivity.class : HoneycombSettingsActivity.class ) : null;
	}

	protected JabberClient getJabber() {
		return getSilentTextApplication().getJabber();
	}

	protected String getLocalResourceName() {
		return getSilentTextApplication().getLocalResourceName();
	}

	protected Log getLog() {
		if( log == null ) {
			log = new Log( getClass().getSimpleName() );
		}
		return log;
	}

	protected NativeBridge getNative() {
		return getSilentTextApplication().getNative();
	}

	protected String getOnlineStatus() {
		return getSilentTextApplication().getOnlineStatus();
	}

	protected Conversation getOrCreateConversation( String partner ) {

		Conversation conversation = getConversation( partner );

		if( conversation == null ) {

			conversation = new Conversation();
			conversation.setStorageKey( CryptoUtils.randomBytes( 64 ) );
			conversation.setPartner( new Contact( partner ) );
			conversation.getPartner().setAlias( getContacts().getDisplayName( partner ) );

			if( isSelf( partner ) ) {
				conversation.getPartner().setDevice( getLocalResourceName() );
				getConversations().save( conversation );
			} else {
				getConversations().save( conversation );
				getNative().connect( partner );
			}

		}

		return conversation;

	}

	protected String getRegisteredDeviceID() {
		Server server = getServer( "broker" );
		if( server == null ) {
			return null;
		}
		Credential credential = server.getCredential();
		if( credential == null ) {
			return null;
		}
		return credential.getUsername();
	}

	protected Server getServer( String name ) {
		return getSilentTextApplication().getServer( name );
	}

	protected ServerRepository getServers() {
		return getSilentTextApplication().getServers();
	}

	protected String getShortUsername() {
		Server server = getServer( "xmpp" );
		return server == null ? null : server.getCredential().getShortUsername();
	}

	protected SilentTextApplication getSilentTextApplication() {
		return (SilentTextApplication) getApplication();
	}

	protected File getTempDir() {
		String state = Environment.getExternalStorageState();
		if( !Environment.MEDIA_MOUNTED.equals( state ) ) {
			return null;
		}
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO ) {
			return getTempDir_PreFroyo();
		}
		return getTempDir_Froyo();
	}

	@TargetApi( Build.VERSION_CODES.FROYO )
	private File getTempDir_Froyo() {
		return getExternalFilesDir( Environment.DIRECTORY_PICTURES );
	}

	private File getTempDir_PreFroyo() {
		return new File( Environment.getExternalStorageDirectory(), getString( R.string.silent_text ) );
	}

	protected String getTextFromView( int viewResourceID ) {
		View view = findViewById( viewResourceID );
		if( view instanceof TextView ) {
			TextView t = (TextView) view;
			CharSequence text = t.getText();
			return text == null ? null : text.toString();
		}
		return null;
	}

	protected long getTimeUntilInactive() {
		return getSilentTextApplication().getTimeUntilInactive( OptionsDrawer.getInactivityTimeout( this ) );
	}

	protected User getUser( CharSequence username ) {
		return getSilentTextApplication().getUser( username );
	}

	protected String getUsername() {
		Server server = getServer( "xmpp" );
		return server == null ? null : server.getCredential().getUsername();
	}

	protected void hideSoftKeyboard( int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			View view = findViewById( id );
			if( view != null ) {
				hideSoftKeyboard( view );
			}
		}
	}

	protected void hideSoftKeyboard( View v ) {
		if( v == null ) {
			return;
		}
		InputMethodManager imm = (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE );
		if( imm == null ) {
			return;
		}
		imm.hideSoftInputFromWindow( v.getWindowToken(), 0 );
		v.clearFocus();
	}

	protected void hideSoftKeyboardOnDrawerToggle() {

		View view = findViewById( R.id.drawer );

		if( view instanceof DrawerLayout ) {

			( (DrawerLayout) view ).setDrawerListener( new DrawerListener() {

				@Override
				public void onDrawerClosed( View view ) {
					hideSoftKeyboard( R.id.compose_text, R.id.search, R.id.username, R.id.password, R.id.passcode, R.id.passcode_verify );
				}

				@Override
				public void onDrawerOpened( View view ) {
					hideSoftKeyboard( R.id.compose_text, R.id.search, R.id.username, R.id.password, R.id.passcode, R.id.passcode_verify );
				}

				@Override
				public void onDrawerSlide( View view, float percent ) {
					// Do nothing.
				}

				@Override
				public void onDrawerStateChanged( int state ) {
					// Do nothing.
				}

			} );

		}

	}

	protected void initializeErrorView() {
		View errorView = findViewById( R.id.error );

		if( errorView != null ) {
			errorView.setOnClickListener( new DismissOnClick() );
			errorView.setVisibility( View.GONE );
		}
	}

	protected void invalidateOptionsMenu( boolean safe ) {
		if( safe ) {
			runOnUiThread( new Runnable() {

				@Override
				public void run() {
					invalidateOptionsMenu( false );
				}

			} );
		} else {
			super.invalidateOptionsMenu();
		}
	}

	protected void isAccessible( final String username, final OnObjectReceiveListener<Boolean> listener ) {
		if( isSelf( username ) ) {
			listener.onObjectReceived( Boolean.valueOf( true ) );
			return;
		}
		String deviceID = getRegisteredDeviceID();
		if( deviceID == null ) {
			JabberClient jabber = getJabber();
			if( jabber != null ) {
				jabber.isAccessible( username, listener );
				return;
			}
			listener.onObjectReceived( Boolean.valueOf( false ) );
			return;
		}
		if( isAccessibleTask != null ) {
			return;
		}
		isAccessibleTask = new AsyncTask<Void, Void, Void>() {

			@Override
			public Void doInBackground( Void... ignore ) {
				User user = getUser( username );
				listener.onObjectReceived( Boolean.valueOf( user != null && user.getEntitlements().contains( Entitlement.SILENT_CIRCLE_MOBILE ) ) );
				isAccessibleTask = null;
				return null;
			}

		}.execute();

	}

	protected boolean isActivated() {
		return getSilentTextApplication().isActivated();
	}

	protected boolean isInactive() {
		int timeout = OptionsDrawer.getInactivityTimeout( this );
		return getSilentTextApplication().isInactive( timeout );
	}

	protected boolean isOnline() {
		return getSilentTextApplication().isOnline();
	}

	protected boolean isSelf( String username ) {
		String self = getUsername();
		if( username == null ) {
			return self == null;
		}
		return self != null && self.equalsIgnoreCase( username );
	}

	protected boolean isSilentPhoneInstalled() {
		Intent intent = createIntentToCallUser( "alice@silentcircle.com" );
		return getPackageManager().resolveActivity( intent, 0 ) != null;
	}

	protected boolean isUnlocked() {
		return getSilentTextApplication().isUnlocked();
	}

	protected void lock() {
		getSilentTextApplication().lock();
		requestUnlock();
	}

	protected void lockContentView() {
		View view = findViewById( R.id.blackout );
		if( view != null ) {
			view.setVisibility( View.VISIBLE );
		}
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		if( requestCode == R_id_activate || requestCode == R_id_unlock ) {
			if( resultCode == RESULT_CANCELED ) {
				finish();
				return;
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		handler = new Handler();
		log = new Log( getClass().getSimpleName() );
		log.onCreate();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		log.onDestroy();
		log = null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		ping();
		unregisterLockReceiver();
		if( isAccessibleTask != null ) {
			isAccessibleTask.cancel( true );
			isAccessibleTask = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		secureThisWindow();
		if( !isOnline() ) {
			adviseReconnect();
		}
		registerLockReceiver();
		getSilentTextApplication().cancelAutoLock();
		getSilentTextApplication().cancelAutoDisconnect();
		ServiceConfiguration.refresh( this );
		getSilentTextApplication().registerKeyManagerIfNecessary();
		// getSilentTextApplication().validateSharedSession();
	}

	@Override
	protected void onStop() {
		super.onStop();
		clearTasks();
	}

	protected void openDrawer() {
		DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer );
		drawer.openDrawer( Gravity.END );
	}

	protected void ping() {
		getSilentTextApplication().ping();
	}

	private void registerLockReceiver() {
		lockReceiver = new FinishActivityOnReceive( this );
		registerReceiver( lockReceiver, Action.LOCK, Manifest.permission.READ );
	}

	protected void registerReceiver( BroadcastReceiver receiver, Action action, String permission ) {
		registerReceiver( receiver, action.filter(), permission, null );
	}

	protected void requestActivation() {
		startActivityForResult( ActivationActivity.class, R_id_activate );
	}

	protected void requestUnlock() {
		startActivityForResult( UnlockActivity.class, R_id_unlock );
	}

	protected void save( Conversation conversation ) {
		getConversations().save( conversation );
	}

	protected void save( Server server ) {
		getServers().save( server );
	}

	protected void secureThisWindow() {
		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
			if( OptionsDrawer.isSecureOutputRequired( this ) ) {
				getWindow().setFlags( LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE );
			} else {
				getWindow().clearFlags( LayoutParams.FLAG_SECURE );
			}
		}
	}

	protected void setAdapter( int viewResourceId, ListAdapter<?> adapter ) {
		findListViewById( viewResourceId ).setAdapter( adapter );
	}

	protected void setHint( int viewResourceID, CharSequence hint ) {
		View view = findViewById( viewResourceID );
		if( view instanceof TextView ) {
			( (TextView) view ).setHint( hint );
		}
	}

	protected void setText( int viewResourceID, CharSequence text ) {
		View view = findViewById( viewResourceID );
		if( view instanceof TextView ) {
			( (TextView) view ).setText( text );
		}
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

	protected boolean shouldAbort() {

		if( !isUnlocked() ) {
			lockContentView();
			requestUnlock();
			return true;
		}

		if( !isActivated() ) {
			lockContentView();
			requestActivation();
			return true;
		}

		if( isInactive() ) {
			lockContentView();
			lock();
			return true;
		}

		return false;

	}

	protected void showError( int stringResourceID ) {
		showError( getString( stringResourceID ) );
	}

	protected void showError( String description ) {
		TextView error = (TextView) findViewById( R.id.error );
		error.setText( description == null || description.length() <= 0 ? getString( R.string.error_unknown ) : description );
		error.setVisibility( View.VISIBLE );
		error.startAnimation( AnimationUtils.loadAnimation( error.getContext(), R.anim.slide_down ) );
		handler.removeCallbacks( hideError );
		handler.postDelayed( hideError, 5000 );
	}

	protected void sos( int viewResourceID, Intent launchIntent ) {

		if( launchIntent == null ) {
			return;
		}

		View view = findViewById( viewResourceID );

		if( view == null ) {
			return;
		}

		SOSListener sos = new SOSListener( launchIntent );

		view.setOnClickListener( sos );
		view.setOnLongClickListener( sos );

	}

	protected void startActivityForResult( Class<? extends Activity> activity, int requestID ) {
		Intent intent = new Intent( this, activity );
		Extra.FOR_RESULT.flag( intent );
		startActivityForResult( intent, requestID );
	}

	protected void suppressDrawer() {
		View view = findViewById( R.id.drawer );
		if( view instanceof DrawerLayout ) {
			( (DrawerLayout) view ).setDrawerLockMode( DrawerLayout.LOCK_MODE_LOCKED_CLOSED );
		}
	}

	protected void toast( final int stringResourceID, final Object... args ) {
		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				Toast.makeText( getActivity(), getString( stringResourceID, args ), Toast.LENGTH_SHORT ).show();
			}

		} );
	}

	protected void toggleActionBar() {
		ActionBar bar = getSupportActionBar();
		if( bar.isShowing() ) {
			bar.hide();
		} else {
			bar.show();
		}
	}

	protected void toggleDrawer() {
		DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer );
		if( drawer.isDrawerOpen( Gravity.END ) ) {
			drawer.closeDrawer( Gravity.END );
		} else {
			drawer.openDrawer( Gravity.END );
		}
	}

	protected void transition( String remoteUserID, String packetID ) {
		Intent intent = Action.TRANSITION.intent();
		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );
		sendBroadcast( intent, Manifest.permission.READ );
	}

	protected boolean tryAutoUnlock() {
		if( !isUnlocked() ) {
			try {
				autoUnlock();
			} catch( Exception exception ) {
				return false;
			}
		}
		return true;
	}

	protected void unlockContentView() {
		View view = findViewById( R.id.blackout );
		if( view != null ) {
			view.setVisibility( View.GONE );
		}
	}

	private void unregisterLockReceiver() {
		if( lockReceiver != null ) {
			unregisterReceiver( lockReceiver );
			lockReceiver = null;
		}
	}

}
