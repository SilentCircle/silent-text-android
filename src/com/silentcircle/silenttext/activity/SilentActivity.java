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
package com.silentcircle.silenttext.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.silentcircle.api.model.Entitlement;
import com.silentcircle.api.model.User;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.SCimpBridge;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.XMPPTransport;
import com.silentcircle.silenttext.listener.DismissOnClick;
import com.silentcircle.silenttext.listener.OnObjectReceiveListener;
import com.silentcircle.silenttext.listener.SOSListener;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.receiver.FinishActivityOnReceive;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.ServerRepository;
import com.silentcircle.silenttext.thread.ViewAnimator;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.view.AvatarView;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.adapter.ListAdapter;

public abstract class SilentActivity extends Activity {

	class CheckUserEntitledToSilentTextTask extends AsyncTask<Void, Void, Void> {

		private final String username;
		private final OnObjectReceiveListener<Boolean> listener;

		CheckUserEntitledToSilentTextTask( String username, OnObjectReceiveListener<Boolean> listener ) {
			this.username = username;
			this.listener = listener;
		}

		@Override
		public Void doInBackground( Void... ignore ) {
			User user = getUser( username, true );
			listener.onObjectReceived( Boolean.valueOf( user != null && user.getEntitlements().contains( Entitlement.SILENT_TEXT ) ) );
			return null;
		}

	}

	public static final int REQUEST_UNLOCK = 0xFFFF & R.id.unlock;
	public static final int REQUEST_ACTIVATE = 0xFFFF & R.id.activate;

	private static final String CACHE_STAGING_DIR_NAME = ".temp";

	public static void assertPermissionToView( Activity activity, boolean requireUnlock, boolean requireLinkedAccount, boolean requireActive ) {

		SilentTextApplication global = SilentTextApplication.from( activity );
		View blackout = activity.findViewById( R.id.blackout );

		if( requireUnlock && !global.isUnlocked() ) {
			if( blackout != null ) {
				blackout.setVisibility( View.VISIBLE );
			}
			activity.startActivityForResult( new Intent( activity, UnlockActivity.class ), REQUEST_UNLOCK );
			throw new IllegalStateException();
		}

		if( requireLinkedAccount && !global.isUserKeyUnlocked() ) {
			if( blackout != null ) {
				blackout.setVisibility( View.VISIBLE );
			}
			activity.startActivityForResult( new Intent( activity, LoginActivity.class ), REQUEST_ACTIVATE );
			throw new IllegalStateException();
		}

		if( requireActive && global.isInactive( OptionsDrawer.getInactivityTimeout( activity ) ) ) {
			if( blackout != null ) {
				blackout.setVisibility( View.VISIBLE );
			}
			global.lock();
			activity.startActivityForResult( new Intent( activity, UnlockActivity.class ), REQUEST_UNLOCK );
			throw new IllegalStateException();
		}

		if( blackout != null ) {
			blackout.setVisibility( View.GONE );
		}

	}

	protected static boolean isDebuggable() {
		return ServiceConfiguration.getInstance().debug;
	}

	/**
	 * @param activity
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	public static void onActivityResult( Activity activity, int requestCode, int resultCode, Intent data ) {
		if( requestCode == SilentActivity.REQUEST_UNLOCK || requestCode == SilentActivity.REQUEST_ACTIVATE ) {
			if( resultCode == RESULT_CANCELED ) {
				activity.finish();
				return;
			}
		}
	}

	protected BroadcastReceiver lockReceiver;
	protected Runnable hideError = new ViewAnimator( this, R.id.error, R.anim.slide_up );
	protected Handler handler;
	protected Log log;
	protected final List<AsyncTask<?, ?, ?>> tasks = new ArrayList<AsyncTask<?, ?, ?>>();

	protected void adviseReconnect() {
		getSilentTextApplication().adviseReconnect();
	}

	protected void beginLoading( final int contentViewId ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				setVisibleIf( false, contentViewId );
				setVisibleIf( true, R.id.progress );
			}

		} );

	}

	protected void clearTasks() {
		while( !tasks.isEmpty() ) {
			tasks.get( 0 ).cancel( true );
			tasks.remove( 0 );
		}
	}

	protected void closeDrawer() {
		closeDrawer( GravityCompat.START );
		closeDrawer( GravityCompat.END );
	}

	protected void closeDrawer( int gravity ) {
		DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer );
		try {
			drawer.closeDrawer( gravity );
		} catch( IllegalArgumentException ignore ) {
			// This just means there no drawer there, which means it cannot have been open anyway.
		}
	}

	protected <T extends AdapterView<android.widget.ListAdapter>> T findAdapterViewById( int viewResourceId ) {
		return (T) findViewById( viewResourceId );
	}

	protected EditText findEditTextById( int viewResourceId ) {
		return (EditText) findViewById( viewResourceId );
	}

	protected TextView findTextViewById( int viewResourceId ) {
		return (TextView) findViewById( viewResourceId );
	}

	protected void finishLoading( final int contentViewId ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				setVisibleIf( true, contentViewId );
				setVisibleIf( false, R.id.progress );
			}

		} );

	}

	protected CharSequence generateDeviceID() {
		return ( (SilentTextApplication) getApplication() ).getOrCreateUUID();
	}

	protected SilentActivity getActivity() {
		return this;
	}

	protected <T extends ListAdapter<?>> T getAdapter( int viewResourceId ) {
		return (T) findAdapterViewById( viewResourceId ).getAdapter();
	}

	public File getCacheStagingDir() {
		return new File( getCacheDir(), CACHE_STAGING_DIR_NAME );
	}

	protected ContactRepository getContacts() {
		return getSilentTextApplication().getContacts();
	}

	protected Conversation getConversation( String partner ) {
		return getSilentTextApplication().getConversation( partner );
	}

	protected ConversationRepository getConversations() {
		return getSilentTextApplication().getConversations();
	}

	protected XMPPTransport getJabber() {
		return getSilentTextApplication().getXMPPTransport();
	}

	protected String getLocalResourceName() {
		return getSilentTextApplication().getLocalResourceName();
	}

	protected Log getLog() {
		if( log == null ) {
			log = new Log( getLogTag() );
		}
		return log;
	}

	protected String getLogTag() {
		return "SilentActivity";
	}

	protected SCimpBridge getNative() {
		return getSilentTextApplication().getSCimpBridge();
	}

	protected String getOnlineStatus() {
		return getSilentTextApplication().getXMPPTransportConnectionStatus();
	}

	protected Conversation getOrCreateConversation( String partner ) {
		return getSilentTextApplication().getOrCreateConversation( partner );
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
		return getSilentTextApplication().getTimeRemainingUntilInactive( OptionsDrawer.getInactivityTimeout( this ) );
	}

	protected User getUser( CharSequence username, boolean forceUpdate ) {
		return getSilentTextApplication().getUser( username, forceUpdate );
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
					hideSoftKeyboard( R.id.compose_text, R.id.action_search, R.id.username, R.id.password, R.id.passcode, R.id.passcode_previous );
				}

				@Override
				public void onDrawerOpened( View view ) {
					hideSoftKeyboard( R.id.compose_text, R.id.action_search, R.id.username, R.id.password, R.id.passcode, R.id.passcode_previous );
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

	protected void invalidateSupportOptionsMenu() {
		invalidateOptionsMenu( true );
	}

	protected void isAccessible( final String username, final OnObjectReceiveListener<Boolean> listener ) {
		if( isAlwaysAccessible( username ) ) {
			listener.onObjectReceived( Boolean.valueOf( true ) );
			return;
		}
		tasks.add( AsyncUtils.execute( new CheckUserEntitledToSilentTextTask( username, listener ) ) );

	}

	protected boolean isActivated() {
		return getSilentTextApplication().isUserKeyUnlocked();
	}

	protected boolean isAlwaysAccessible( String username ) {
		return !ServiceConfiguration.getInstance().features.checkUserAvailability || isSelf( username );
	}

	protected boolean isInactive() {
		int timeout = OptionsDrawer.getInactivityTimeout( this );
		return getSilentTextApplication().isInactive( timeout );
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
	protected boolean isLayoutDirectionRTL() {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
			return false;
		}
		Resources resources = getResources();
		Configuration configuration = resources.getConfiguration();
		int layoutDirection = configuration.getLayoutDirection();
		return layoutDirection == View.LAYOUT_DIRECTION_RTL;
	}

	protected boolean isOnline() {
		return getSilentTextApplication().isXMPPTransportConnected();
	}

	protected boolean isSelf( String username ) {
		String self = getUsername();
		if( username == null ) {
			return self == null;
		}
		return self != null && self.equalsIgnoreCase( username );
	}

	protected boolean isUnlocked() {
		return getSilentTextApplication().isUnlocked();
	}

	protected void loadAvatar( String username, int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			View view = findViewById( viewResourceIDs[i] );
			if( view instanceof AvatarView ) {
				( (AvatarView) view ).loadAvatar( username );
			}
		}
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
		if( requestCode == REQUEST_ACTIVATE || requestCode == REQUEST_UNLOCK ) {
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
		getLog().onCreate();
	}

	protected void onDeactivated() {
		Intent intent = new Intent( this, LoginActivity.class );
		Extra.DEACTIVATED.flag( intent );
		startActivity( intent );
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
		sendToBackground();
		unregisterLockReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( !isOnline() ) {
			adviseReconnect();
		}
		registerLockReceiver();
		sendToForeground();
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

	private void registerLockReceiver() {
		lockReceiver = new FinishActivityOnReceive( this );
		registerReceiver( lockReceiver, Action.LOCK, Manifest.permission.READ );
	}

	protected void registerReceiver( BroadcastReceiver receiver, Action action, String permission ) {
		registerReceiver( receiver, action.filter(), permission, null );
	}

	protected void requestActivation() {
		startActivityForResult( LoginActivity.class, REQUEST_ACTIVATE );
	}

	protected void requestUnlock() {
		startActivityForResult( UnlockActivity.class, REQUEST_UNLOCK );
	}

	protected void save( Conversation conversation ) {
		getConversations().save( conversation );
	}

	protected void save( Server server ) {
		getServers().save( server );
	}

	protected void sendToBackground() {
		getSilentTextApplication().sendToBackground();
	}

	protected void sendToForeground() {
		getSilentTextApplication().sendToForeground();
	}

	protected void setAdapter( int viewResourceId, ListAdapter<?> adapter ) {
		findAdapterViewById( viewResourceId ).setAdapter( adapter );
	}

	protected void setHint( int viewResourceID, CharSequence hint ) {
		View view = findViewById( viewResourceID );
		if( view instanceof TextView ) {
			( (TextView) view ).setHint( hint );
		}
	}

	protected void setOnClickListener( OnClickListener onClickListener, int... viewResourceIDs ) {
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			View view = findViewById( viewResourceIDs[i] );
			if( view != null ) {
				view.setOnClickListener( onClickListener );
			}
		}
	}

	protected void setText( int viewResourceID, CharSequence text ) {
		View view = findViewById( viewResourceID );
		if( view instanceof TextView ) {
			( (TextView) view ).setText( text );
		}
	}

	protected void setVisibleIf( final boolean condition, final int... viewResourceIDs ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				int visibility = condition ? View.VISIBLE : View.GONE;
				for( int i = 0; i < viewResourceIDs.length; i++ ) {
					int viewResourceID = viewResourceIDs[i];
					View view = findViewById( viewResourceID );
					if( view != null ) {
						view.setVisibility( visibility );
					}
				}
			}

		} );

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

	protected void showError( Throwable exception ) {
		String message = exception.getLocalizedMessage();
		if( StringUtils.isEmpty( message ) ) {
			message = String.format( "%s [%s]", getString( R.string.error_unknown ), exception.getClass().getSimpleName() );
		}
		showError( message );
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
		ActionBar bar = getActionBar();
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

	protected void toggleDrawer( View drawerView ) {
		DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer );
		if( drawer.isDrawerOpen( drawerView ) ) {
			drawer.closeDrawer( drawerView );
		} else {
			drawer.openDrawer( drawerView );
		}
	}

	protected void transition( String remoteUserID, String packetID ) {

		final Intent intent = Action.TRANSITION.intent();

		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, packetID );

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				sendBroadcast( intent, Manifest.permission.READ );
			}

		} );

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
