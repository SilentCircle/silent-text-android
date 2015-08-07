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
package com.silentcircle.silenttext.activity;

import java.util.ArrayList;

import org.twuni.twoson.JSONParser;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.Authenticator;
import com.silentcircle.api.Session;
import com.silentcircle.api.model.ActivationCodeCredential;
import com.silentcircle.api.model.Application;
import com.silentcircle.api.model.Device;
import com.silentcircle.api.model.UsernamePasswordCredential;
import com.silentcircle.api.web.model.BasicApplication;
import com.silentcircle.api.web.model.BasicDevice;
import com.silentcircle.api.web.model.json.JSONObjectParser;
import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.exception.http.HTTPException;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.content.SilentPhoneAccount;
import com.silentcircle.silenttext.listener.LoginOnClickWithRequiredInput;
import com.silentcircle.silenttext.listener.LoginOnRequiredInput;
import com.silentcircle.silenttext.listener.RequiredFieldListener;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.service.GCMService;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.DeviceUtils;

public class LoginActivity extends SilentActivity {

	class LoginTask extends AsyncTask<Void, Void, Void> {

		private final CharSequence deviceID;
		private final com.silentcircle.api.model.Credential credential;

		public LoginTask( CharSequence deviceID, com.silentcircle.api.model.Credential credential ) {
			this.deviceID = deviceID;
			this.credential = credential;
		}

		@Override
		protected Void doInBackground( Void... ignore ) {
			try {
				try {

					Server server = getServer( "broker" );

					if( server == null ) {
						server = new Server( "broker" );
					}

					if( server.getCredential() == null ) {
						server.setCredential( new Credential() );
					}

					Authenticator authenticator = getSilentTextApplication().getAuthenticator();
					Device device = new BasicDevice( deviceID, "android", getDeviceName() );
					Application app = new BasicApplication( "silent_text" );// TODO: ...or should
																			// this
																			// be getPackageName()?

					Session session = authenticator.authenticate( credential, device, app );

					if( session == null ) {
						throw new RuntimeException( getString( R.string.error_invalid_activation_code ) );
					}

					if( session instanceof AuthenticatedSession ) {
						onLoginSuccessful( session.getAccessToken(), session.getID() );
					}

				} catch( HTTPException exception ) {
					log.error( exception, "#login error:%s", exception.getLocalizedMessage() );
					try {
						CharSequence message = JSONObjectParser.parseFault( JSONParser.parse( exception.getBody() ) ).getMessage();
						if( !StringUtils.isEmpty( message ) ) {
							onLoginFailed( message.toString() );
						}
					} catch( Throwable jsonException ) {
						log.error( jsonException, "#login error: jsonException" );
						onLoginFailed( getString( R.string.error_unknown ) );
					}
				} catch( Throwable exception ) {
					log.error( exception, "#activate error:%s", exception.getLocalizedMessage() );
					onLoginFailed( exception );
				}
			} catch( Throwable e ) {
				// TODO: This super catch can be removed when we deprecate Devin's JSON parser
				log.error( e, "#login error: unknown" );
				onLoginFailed( getString( R.string.error_unknown ) );
			}

			return null;

		}
	}

	class ToggleInputEnabled extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			toggleInputEnabled();
		}

	}

	private static final int R_id_create_account = 0xFFFF & R.id.create_account;

	private ToggleInputEnabled toggleInputEnabled;
	private boolean startedForResult;

	private final static String TAG = "LoginActivity";

	protected Credential adapt( UsernamePasswordCredential in ) {
		Credential out = new Credential();
		out.setUsername( getSilentTextApplication().getFullJIDForUsername( in.getUsername() ).toString() );
		out.setPassword( in.getPassword().toString() );
		return out;
	}

	protected Intent getAccountCreationIntent() {
		// STA-921 only enable account creation on partner devices
		if( !DeviceUtils.isPartnerDevice( this ) ) {
			return null;
		}

		return new Intent( this, AccountCreationActivity.class );
	}

	protected String getDeviceName() {
		return getSilentTextApplication().getLocalResourceName();
	}

	@Override
	protected String getLogTag() {
		return "LoginActivity";
	}

	protected SilentPhoneAccount [] getSilentPhoneAccounts() {
		return SilentPhoneAccount.list( getContentResolver() );
	}

	protected boolean hasAccountCreationIntent() {
		Intent intent = getAccountCreationIntent();
		if( intent == null ) {
			return false;
		}
		return getPackageManager().resolveActivity( intent, 0 ) != null;
	}

	protected boolean hasSharedSession() {
		return isExternalKeyManagerAvailable() && getSilentTextApplication().hasSharedSession();
	}

	protected boolean isExternalKeyManagerAvailable() {
		return getSilentTextApplication().isExternalKeyManagerAvailable();
	}

	public boolean login() {

		EditText usernameView = (EditText) findViewById( R.id.username );
		EditText passwordView = (EditText) findViewById( R.id.password );

		CharSequence username = usernameView.getEditableText();
		CharSequence password = passwordView.getEditableText();

		if( username.length() < 1 || password.length() < 3 ) {
			onLoginFailed( getString( R.string.error_invalid_credentials ) );
			return false;
		}

		hideSoftKeyboard( R.id.username, R.id.password );

		return login( username, password );

	}

	/**
	 * @param username
	 * @param password
	 * @return
	 */
	protected boolean login( final CharSequence username, final CharSequence password ) {
		login( new UsernamePasswordCredential( username, password ) );
		return true;
	}

	protected boolean login( com.silentcircle.api.model.Credential credential ) {
		return login( credential, generateDeviceID() );
	}

	protected boolean login( com.silentcircle.api.model.Credential credential, final CharSequence deviceID ) {
		beginLoading( R.id.activation );
		clearTasks();
		tasks.add( AsyncUtils.execute( new LoginTask( deviceID, credential ) ) );
		return true;
	}

	protected void nextActivity() {
		if( startedForResult ) {
			setResult( RESULT_OK );
			finish();
			return;
		}
		startActivity( new Intent( this, ConversationListActivity.class ).setFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP ) );
		finish();
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_activation );
		SettingsActivity.setAsTrigger( findViewById( R.id.action_settings ) );
		if( getSilentTextApplication().isUserKeyUnlocked() ) {
			nextActivity();
			return;
		}

		initializeErrorView();

		View createAccount = findViewById( R.id.button_new_account );

		if( createAccount != null ) {
			createAccount.setVisibility( hasAccountCreationIntent() ? View.VISIBLE : View.INVISIBLE );

			createAccount.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick( View v ) {
					Intent intent = getAccountCreationIntent();
					if( intent != null ) {
						startActivityForResult( intent, R_id_create_account );
					}
				}

			} );

		}

		EditText loginView = (EditText) findViewById( R.id.username );
		EditText passwordView = (EditText) findViewById( R.id.password );

		if( loginView != null ) {
			loginView.setOnEditorActionListener( new RequiredFieldListener() );
		}
		ArrayList<TextView> requiredViews = new ArrayList<TextView>();
		requiredViews.add( loginView );

		if( passwordView != null ) {
			passwordView.setOnEditorActionListener( new LoginOnRequiredInput( this, requiredViews, true ) );
		}

		requiredViews.add( passwordView );

		findViewById( R.id.button_login ).setOnClickListener( new LoginOnClickWithRequiredInput( this, requiredViews ) );

		registerShowPasswordListener();

		togglePartnerMessageVisibility();
		if( savedInstanceState == null ) {
			onNewIntent( getIntent() );
		}

	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getMenuInflater().inflate( R.menu.activation, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks( hideError );
	}

	public void onLoginFailed( final String description ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				finishLoading( R.id.activation );
				showError( description );
			}

		} );

	}

	public void onLoginFailed( final Throwable exception ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				finishLoading( R.id.activation );
				showError( exception );
			}

		} );

	}

	protected void onLoginSuccessful( CharSequence apiKey, CharSequence deviceID ) {

		if( !isUnlocked() ) {
			requestUnlock();
			return;
		}

		Server server = getServer( "broker" );

		if( server == null ) {
			server = new Server( "broker" );
		}

		if( server.getCredential() == null ) {
			server.setCredential( new Credential() );
		}

		server.getCredential().setPassword( apiKey.toString() );
		server.getCredential().setUsername( deviceID.toString() );

		if( server.getURL() == null ) {
			server.setURL( getSilentTextApplication().getAPIURL() );
		}

		save( server );

		onSessionAvailable( getSilentTextApplication().getSession() );

	}

	@Override
	protected void onNewIntent( Intent intent ) {

		super.onNewIntent( intent );

		startedForResult = Extra.FOR_RESULT.test( intent );

		if( Extra.DEACTIVATED.test( intent ) ) {

			AlertDialog.Builder logoutDialogBuilder = new AlertDialog.Builder( this );

			logoutDialogBuilder.setMessage( R.string.logged_out );

			logoutDialogBuilder.setNeutralButton( android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick( DialogInterface dialog, int buttonID ) {
					dialog.dismiss();
				}
			} );

			AlertDialog logoutDialog = logoutDialogBuilder.create();

			logoutDialog.setOnShowListener( new OnShowListener() {

				@Override
				public void onShow( DialogInterface dialog ) {
					TextView logoutMessageView = (TextView) ( (AlertDialog) dialog ).findViewById( android.R.id.message );

					FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT );
					layoutParams.gravity = Gravity.CENTER;
					layoutParams.setMargins( 0, 10, 0, 5 );

					logoutMessageView.setLayoutParams( layoutParams );
				}
			} );

			logoutDialog.show();

			return;
		}

		if( !isActivated() ) {

			com.silentcircle.api.model.Credential credential = null;

			if( Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
				Uri uri = intent.getData();
				if( uri != null ) {
					if( "silentcircleprovision".equals( uri.getScheme() ) ) {
						String activationCode = uri.getEncodedSchemeSpecificPart();
						if( activationCode != null ) {
							activationCode = activationCode.replaceAll( "^//", "" );
							credential = new ActivationCodeCredential( activationCode );
						}
					}
				}
			}

			CharSequence activationCode = Extra.ACTIVATION_CODE.getCharSequence( intent );
			CharSequence username = Extra.USERNAME.getCharSequence( intent );
			CharSequence password = Extra.PASSWORD.getCharSequence( intent );

			if( activationCode != null ) {
				credential = new ActivationCodeCredential( activationCode );
			}

			if( username != null && password != null ) {
				credential = new UsernamePasswordCredential( username, password );
			}

			if( credential != null ) {
				login( credential );
			}

			if( hasSharedSession() ) {
				beginLoading( R.id.activation );
				new Thread( "activate-via-shared-session" ) {

					@Override
					public void run() {
						try {
							onSessionAvailable( getSilentTextApplication().getSharedSession() );
						} catch( Throwable exception ) {
							Log.e( TAG, exception.getMessage() );
							getLog().error( exception, "#activate via:#getSharedSession" );
							toast( R.string.error_format, exception.getLocalizedMessage() );
							finish();
						}
					}

				}.start();
				return;
			}

		}

	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.activate:
				login();
				break;
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceivers();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( !isUnlocked() ) {
			requestUnlock();
			return;
		}
		registerReceivers();
	}

	@Override
	protected void onSaveInstanceState( Bundle outState ) {
		super.onSaveInstanceState( outState );
	}

	protected void onSessionAvailable( Session session ) {

		if( session == null ) {
			return;
		}

		com.silentcircle.api.model.Credential credential = session.fetchCredential( "silent_text" );

		if( !( credential instanceof UsernamePasswordCredential ) ) {
			throw new RuntimeException( getString( R.string.error_invalid_api_key ) );
		}

		if( ServiceConfiguration.getInstance().debug && !StringUtils.equals( ServiceConfiguration.getInstance().environment, "silentcircle.com" ) ) {
			String newDeviceID = getSilentTextApplication().getServer( "broker" ).getCredential().getUsername();

			if( newDeviceID != null ) {
				session.updateActiveDevice( getSilentTextApplication().getServer( "broker" ).getCredential().getUsername() );
			}
		}

		if( session instanceof AuthenticatedSession ) {
			getSilentTextApplication().shareSession( (AuthenticatedSession) session );
		}

		GCMService.register( this );
		getSilentTextApplication().setXMPPTransportCredential( adapt( (UsernamePasswordCredential) credential ) );
		nextActivity();

	}

	private void registerReceivers() {

		if( toggleInputEnabled == null ) {
			toggleInputEnabled = new ToggleInputEnabled();
			registerReceiver( toggleInputEnabled, Action.CONNECT, Manifest.permission.READ );
			registerReceiver( toggleInputEnabled, Action.DISCONNECT, Manifest.permission.READ );
		}

	}

	private void registerShowPasswordListener() {

		View view = findViewById( R.id.show_password );

		if( view instanceof CheckBox ) {

			( (CheckBox) view ).setOnCheckedChangeListener( new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged( CompoundButton view, boolean checked ) {
					setPasswordVisible( checked );
				}

			} );

		}

	}

	protected void setPasswordVisible( boolean visible ) {
		View view = findViewById( R.id.password );
		if( view instanceof TextView ) {
			int type = InputType.TYPE_CLASS_TEXT;
			if( visible ) {
				type |= InputType.TYPE_TEXT_VARIATION_NORMAL;
				type |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			} else {
				type |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}
			( (TextView) view ).setInputType( type );
		}
	}

	protected void toggleInputEnabled() {
		toggleInputEnabled( R.id.username, R.id.password );
	}

	protected void toggleInputEnabled( int... viewResourceIDs ) {
		boolean enabled = getSilentTextApplication().isNetworkConnected();
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			View view = findViewById( id );
			if( view != null ) {
				view.setEnabled( enabled );
			}
		}
		if( !enabled ) {
			showError( R.string.waiting_for_connection );
		}
	}

	private void togglePartnerMessageVisibility() {
		boolean eligible = DeviceUtils.isPartnerDevice( this );
		setVisibleIf( eligible, R.id.partner_welcome );
		if( eligible ) {
			setText( R.id.partner_welcome, getString( R.string.partner_welcome, getString( R.string.silent_circle ), DeviceUtils.getManufacturer() ) );
		}
	}

	private void unregisterReceivers() {
		if( toggleInputEnabled != null ) {
			unregisterReceiver( toggleInputEnabled );
			toggleInputEnabled = null;
		}
	}

}
