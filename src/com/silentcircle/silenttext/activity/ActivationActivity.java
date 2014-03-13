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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.api.AuthenticatedSession;
import com.silentcircle.api.Session;
import com.silentcircle.api.model.ActivationCodeCredential;
import com.silentcircle.api.model.UsernamePasswordCredential;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.BuildConfig;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.client.ActivationClient;
import com.silentcircle.silenttext.content.SilentPhoneAccount;
import com.silentcircle.silenttext.model.Credential;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.service.GCMService;
import com.silentcircle.silenttext.util.DeviceUtils;

public class ActivationActivity extends SilentActivity {

	class ToggleInputEnabled extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			toggleInputEnabled();
		}

	}

	private static final String KEY_ENTITLED_TO_IN_APP_ACCOUNT_CREATION = "entitled_to_in_app_account_creation";

	private static final int R_id_create_account = 0xFFFF & R.id.create_account;

	private ToggleInputEnabled toggleInputEnabled;
	private boolean startedForResult;
	protected boolean isEntitledToInAppAccountCreation;

	protected boolean activate() {

		EditText usernameView = (EditText) findViewById( R.id.username );
		EditText passwordView = (EditText) findViewById( R.id.password );

		CharSequence username = usernameView.getEditableText();
		CharSequence password = passwordView.getEditableText();

		if( username.length() < 1 || password.length() < 3 ) {
			onActivationError( getString( R.string.error_invalid_credentials ) );
			return false;
		}

		hideSoftKeyboard( R.id.username, R.id.password );

		return login( username, password );

	}

	protected void activate( CharSequence apiKey, CharSequence deviceID ) {

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

		GCMService.register( getActivity() );

		Session session = getSilentTextApplication().getSession();
		com.silentcircle.api.model.Credential credential = session.getCredential( "silent_text" );

		if( !( credential instanceof UsernamePasswordCredential ) ) {
			throw new RuntimeException( getString( R.string.error_invalid_api_key ) );
		}

		GCMService.register( this );
		getSilentTextApplication().setJabber( adapt( (UsernamePasswordCredential) credential ) );
		nextActivity();

	}

	protected boolean activate( final com.silentcircle.api.model.Credential credential ) {
		return activate( credential, generateDeviceID() );
	}

	protected boolean activate( final com.silentcircle.api.model.Credential credential, final CharSequence deviceID ) {

		beginLoading( R.id.activation );

		clearTasks();
		tasks.add( new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground( Void... ignore ) {

				try {

					Server server = getServer( "broker" );

					if( server == null ) {
						server = new Server( "broker" );
					}

					if( server.getCredential() == null ) {
						server.setCredential( new Credential() );
					}

					if( server.getURL() == null ) {
						server.setURL( getSilentTextApplication().getAPIURL() );
						save( server );
					}

					ActivationClient activator = new ActivationClient( server.getURL() );

					Session session = activator.authenticate( credential, deviceID, getDeviceName() );

					if( session == null ) {
						throw new RuntimeException( getString( R.string.error_invalid_activation_code ) );
					}

					activate( session );

				} catch( Throwable exception ) {
					log.error( exception, "#activate", (Object []) exception.getStackTrace() );
					onActivationError( exception.getMessage() );
				}

				return null;

			}

		}.execute() );

		return true;

	}

	protected void activate( Session session ) {
		if( session instanceof AuthenticatedSession ) {
			activate( session.getAccessToken(), session.getID() );
		}
	}

	protected Credential adapt( UsernamePasswordCredential in ) {
		Credential out = new Credential();
		out.setUsername( getSilentTextApplication().getFullJIDForUsername( in.getUsername() ).toString() );
		out.setPassword( in.getPassword().toString() );
		return out;
	}

	protected void autoActivate( SilentPhoneAccount... accounts ) {

		beginLoading( R.id.activation );

		clearTasks();
		tasks.add( new AsyncTask<SilentPhoneAccount, Void, Boolean>() {

			@Override
			protected Boolean doInBackground( SilentPhoneAccount... accounts ) {

				if( accounts.length > 0 ) {

					SilentPhoneAccount account = accounts[0];

					try {
						activate( account.apiKey, account.deviceID );
					} catch( Throwable exception ) {
						log.error( exception, "#autoActivate", (Object []) exception.getStackTrace() );
						return Boolean.valueOf( false );
					}

					return Boolean.valueOf( true );

				}

				return Boolean.valueOf( false );

			}

			@Override
			protected void onPostExecute( Boolean success ) {
				if( success.booleanValue() ) {
					nextActivity();
				} else {
					finishLoading( R.id.activation );
				}
			}

		}.execute( accounts ) );

	}

	protected Intent getAccountCreationIntent() {
		Intent webAccountCreation = new Intent( Intent.ACTION_VIEW, Uri.parse( "https://accounts.silentcircle.com/join/" ) );
		Intent inAppAccountCreation = new Intent( this, AccountCreationActivity.class );
		Intent silentPhoneAccountCreation = getPackageManager().getLaunchIntentForPackage( "com.silentcircle.silentphone" );
		if( BuildConfig.DEBUG ) {
			ChooserBuilder chooser = new ChooserBuilder( this );
			chooser.label( R.string.create_account );
			chooser.intent( webAccountCreation );
			chooser.intent( inAppAccountCreation );
			chooser.intent( silentPhoneAccountCreation );
			return chooser.build();
		}
		if( DeviceUtils.isEligibleForAccountCreation( this ) && isEntitledToInAppAccountCreation ) {
			if( isAccountCreationViaSilentPhoneAnOption() ) {
				return silentPhoneAccountCreation;
			}
			return inAppAccountCreation;
		}
		return webAccountCreation;
	}

	protected String getDeviceName() {
		return getSilentTextApplication().getLocalResourceName();
	}

	protected SilentPhoneAccount [] getSilentPhoneAccounts() {
		return SilentPhoneAccount.list( getContentResolver() );
	}

	protected boolean hasSharedSession() {
		return isKeyManagerSupported() && getSilentTextApplication().hasSharedSession();
	}

	private boolean isAccountCreationViaSilentPhoneAnOption() {
		Cursor cursor = getContentResolver().query( Uri.parse( "content://com.silentcircle.silentphone/status" ), null, null, null, null );
		if( cursor != null && cursor.moveToFirst() ) {
			int status = cursor.getInt( 0 );
			return status == 1 || status == 2;
		}
		return false;
	}

	protected boolean isKeyManagerSupported() {
		return getSilentTextApplication().isKeyManagerSupported();
	}

	/**
	 * @param username
	 * @param password
	 * @return
	 */
	protected boolean login( final CharSequence username, final CharSequence password ) {
		activate( new UsernamePasswordCredential( username, password ) );
		return true;
	}

	protected void nextActivity() {
		if( startedForResult ) {
			setResult( RESULT_OK );
			finish();
			return;
		}
		startActivity( new Intent( this, ConversationListActivity.class ) );
	}

	public void onActivationError( final String description ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				finishLoading( R.id.activation );
				showError( description );
			}

		} );

	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		switch( requestCode ) {
			case R_id_create_account:
				switch( resultCode ) {
					case RESULT_OK:
						if( data == null ) {
							return;
						}
						CharSequence activationCode = Extra.ACTIVATION_CODE.getCharSequence( data );
						CharSequence username = Extra.USERNAME.getCharSequence( data );
						CharSequence password = Extra.PASSWORD.getCharSequence( data );
						if( activationCode != null ) {
							activate( new ActivationCodeCredential( activationCode ) );
						}
						if( username != null && password != null ) {
							activate( new UsernamePasswordCredential( username, password ) );
						}
						return;
				}
				return;
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_activation );

		if( getSilentTextApplication().isActivated() ) {
			nextActivity();
			return;
		}

		initializeErrorView();

		findViewById( R.id.button_login ).setOnClickListener( new OnClickListener() {

			private boolean isEmpty( int viewResourceID ) {
				View view = findViewById( viewResourceID );
				if( view instanceof EditText ) {
					if( ( (EditText) view ).getEditableText().length() <= 0 ) {
						return true;
					}
				}
				return false;
			}

			@Override
			public void onClick( View v ) {
				if( isEmpty( R.id.username ) ) {
					findViewById( R.id.username ).requestFocus();
					return;
				}
				if( isEmpty( R.id.password ) ) {
					findViewById( R.id.password ).requestFocus();
					return;
				}
				activate();
			}

		} );

		findViewById( R.id.button_new_account ).setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				startActivityForResult( getAccountCreationIntent(), R_id_create_account );
			}

		} );

		sos( R.id.logo, getDebugSettingsIntent() );

		EditText view = (EditText) findViewById( R.id.password );

		if( view != null ) {

			toggleInputEnabled();

			view.setOnEditorActionListener( new OnEditorActionListener() {

				@Override
				public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
					switch( actionId ) {
						case EditorInfo.IME_ACTION_DONE:
						case EditorInfo.IME_ACTION_SEND:
						case EditorInfo.IME_ACTION_GO:
							return activate();
					}
					return false;
				}

			} );

		}

		togglePartnerMessageVisibility();

		if( savedInstanceState == null ) {

			onNewIntent( getIntent() );

			if( DeviceUtils.isEligibleForAccountCreation( this ) ) {

				tasks.add( new AsyncTask<Void, Void, Boolean>() {

					@Override
					protected Boolean doInBackground( Void... params ) {
						try {
							return Boolean.valueOf( getSilentTextApplication().getAccountCreationClient().isEntitledToAccountCreation() );
						} catch( Throwable exception ) {
							log.warn( exception, "#isEntitledToAccountCreation" );
							return Boolean.valueOf( false );
						}
					}

					@Override
					protected void onPostExecute( Boolean isEntitledToAccountCreation ) {
						isEntitledToInAppAccountCreation = isEntitledToAccountCreation.booleanValue();
					}

				}.execute() );

			}

		} else {
			isEntitledToInAppAccountCreation = savedInstanceState.getBoolean( KEY_ENTITLED_TO_IN_APP_ACCOUNT_CREATION );
		}

	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getSupportMenuInflater().inflate( R.menu.activation, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks( hideError );
	}

	@Override
	protected void onNewIntent( Intent intent ) {

		super.onNewIntent( intent );

		startedForResult = Extra.FOR_RESULT.test( intent );

		if( !isActivated() ) {

			if( Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
				Uri uri = intent.getData();
				if( uri != null ) {
					if( "silentcircleprovision".equals( uri.getScheme() ) ) {
						String activationCode = uri.getEncodedSchemeSpecificPart();
						if( activationCode != null ) {
							activationCode = activationCode.replaceAll( "^//", "" );
							activate( new ActivationCodeCredential( activationCode ) );
						}
					}
				}
			}

			SilentPhoneAccount [] accounts = getSilentPhoneAccounts();

			if( accounts.length > 0 ) {
				autoActivate( accounts );
				return;
			}

			if( hasSharedSession() ) {
				beginLoading( R.id.activation );
				new Thread() {

					@Override
					public void run() {
						activate( getSilentTextApplication().getSharedSession() );
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
				activate();
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
		outState.putBoolean( KEY_ENTITLED_TO_IN_APP_ACCOUNT_CREATION, isEntitledToInAppAccountCreation );
	}

	private void registerReceivers() {

		if( toggleInputEnabled == null ) {
			toggleInputEnabled = new ToggleInputEnabled();
			registerReceiver( toggleInputEnabled, Action.CONNECT, Manifest.permission.READ );
			registerReceiver( toggleInputEnabled, Action.DISCONNECT, Manifest.permission.READ );
		}

	}

	protected void toggleInputEnabled() {
		toggleInputEnabled( R.id.username, R.id.password );
	}

	protected void toggleInputEnabled( int... viewResourceIDs ) {
		boolean enabled = getSilentTextApplication().isConnected();
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
		boolean eligible = DeviceUtils.isEligibleForAccountCreation( this );
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
