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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.twuni.twoson.JSONParser;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.silentcircle.api.UserManager;
import com.silentcircle.api.model.UsernamePasswordCredential;
import com.silentcircle.api.web.model.BasicUser;
import com.silentcircle.api.web.model.json.JSONObjectParser;
import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.exception.http.HTTPException;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.fragment.AccountCreationFragment;
import com.silentcircle.silenttext.fragment.AccountCreationWelcomeFragment;
import com.silentcircle.silenttext.fragment.FragmentFlowListener;
import com.silentcircle.silenttext.fragment.HasFlow;
import com.silentcircle.silenttext.thread.ViewAnimator;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.DeviceUtils;
import com.silentcircle.silenttext.util.DeviceUtils.ApplicationLicenseCodeProvider;
import com.silentcircle.silenttext.util.DeviceUtils.DeviceIDLicenseCodeProvider;
import com.silentcircle.silenttext.util.DeviceUtils.EligibleLicenseCodeProvider;
import com.silentcircle.silenttext.util.DeviceUtils.LicenseCodeProvider;

public class AccountCreationActivity extends Activity implements FragmentFlowListener {

	private static Bundle copyCharSequence( Intent from, Bundle to, String extra ) {
		to.putCharSequence( extra, from.getCharSequenceExtra( extra ) );
		return to;
	}

	private static Bundle toBundle( Intent intent ) {
		Bundle bundle = new Bundle();
		if( intent != null ) {
			copyCharSequence( intent, bundle, AccountCreationFragment.EXTRA_USERNAME );
			copyCharSequence( intent, bundle, AccountCreationFragment.EXTRA_PASSWORD );
			copyCharSequence( intent, bundle, AccountCreationFragment.EXTRA_EMAIL );
			copyCharSequence( intent, bundle, AccountCreationFragment.EXTRA_FIRST_NAME );
			copyCharSequence( intent, bundle, AccountCreationFragment.EXTRA_LAST_NAME );
			copyCharSequence( intent, bundle, AccountCreationFragment.EXTRA_LICENSE_CODE );
		}
		return bundle;
	}

	protected Handler handler;
	protected ViewAnimator hideError;
	protected final List<AsyncTask<?, ?, ?>> tasks = new ArrayList<AsyncTask<?, ?, ?>>();

	protected void activate( UsernamePasswordCredential credential ) {
		if( credential == null ) {
			return;
		}

		Intent loginIntent = new Intent( this, LoginActivity.class );
		Extra.USERNAME.to( loginIntent, credential.getUsername() );
		Extra.PASSWORD.to( loginIntent, credential.getPassword() );
		startActivity( loginIntent );

		finish();
	}

	protected void clearTasks() {
		while( !tasks.isEmpty() ) {
			tasks.get( 0 ).cancel( true );
			tasks.remove( 0 );
		}
	}

	protected Activity getActivity() {
		return this;
	}

	@Override
	public void onCancel() {
		setResult( RESULT_CANCELED );
		finish();
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.fragment );
		findViewById( R.id.error ).setVisibility( View.GONE );

		handler = new Handler();
		hideError = new ViewAnimator( this, R.id.error, R.anim.slide_up );

		Fragment fragment = getFragmentManager().findFragmentById( R.id.content );

		if( fragment == null ) {
			Bundle bundle = toBundle( getIntent() );

			CharSequence licenseCode = bundle.getCharSequence( AccountCreationFragment.EXTRA_LICENSE_CODE );
			if( StringUtils.isEmpty( licenseCode ) ) {
				// check for application level license code
				LicenseCodeProvider provider = new ApplicationLicenseCodeProvider();
				licenseCode = provider.provideLicenseCode( this );

				if( StringUtils.isEmpty( licenseCode ) ) {
					// check for auto-generated partner license codes
					provider = new EligibleLicenseCodeProvider( DeviceUtils.getPartnerEligibility( this ), new DeviceIDLicenseCodeProvider() );
					licenseCode = provider.provideLicenseCode( this );
				}

				bundle.putCharSequence( AccountCreationFragment.EXTRA_LICENSE_CODE, licenseCode );
			}

			fragment = AccountCreationWelcomeFragment.create( bundle );
			getFragmentManager().beginTransaction().add( R.id.content, fragment ).commit();
		}

		if( fragment instanceof HasFlow ) {
			( (HasFlow) fragment ).setFlowListener( this );
		}

		getFragmentManager().addOnBackStackChangedListener( new OnBackStackChangedListener() {

			@Override
			public void onBackStackChanged() {
				Fragment f = getFragmentManager().findFragmentById( R.id.content );
				if( f instanceof HasFlow ) {
					( (HasFlow) f ).setFlowListener( AccountCreationActivity.this );
				}
			}
		} );

	}

	@Override
	public void onError( final CharSequence message ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				TextView error = (TextView) findViewById( R.id.error );
				error.setText( message == null || message.length() <= 0 ? getString( R.string.error_unknown ) : message );
				error.setVisibility( View.VISIBLE );
				error.startAnimation( AnimationUtils.loadAnimation( error.getContext(), R.anim.slide_down ) );
				handler.removeCallbacks( hideError );
				handler.postDelayed( hideError, 5000 );
			}

		} );

	}

	@Override
	public void onFinish( final Bundle arguments ) {

		if( !tasks.isEmpty() ) {
			return;
		}

		tasks.add( AsyncUtils.execute( new AsyncTask<Void, Void, UsernamePasswordCredential>() {

			@Override
			protected UsernamePasswordCredential doInBackground( Void... params ) {

				UserManager manager = SilentTextApplication.from( getActivity() ).getUserManager();

				BasicUser user = new BasicUser();

				user.setFirstName( arguments.getCharSequence( AccountCreationFragment.EXTRA_FIRST_NAME ) );
				user.setLastName( arguments.getCharSequence( AccountCreationFragment.EXTRA_LAST_NAME ) );
				user.setEmailAddress( arguments.getCharSequence( AccountCreationFragment.EXTRA_EMAIL ) );

				CharSequence username = arguments.getCharSequence( AccountCreationFragment.EXTRA_USERNAME );
				CharSequence password = arguments.getCharSequence( AccountCreationFragment.EXTRA_PASSWORD );
				CharSequence licenseCode = arguments.getCharSequence( AccountCreationFragment.EXTRA_LICENSE_CODE );

				try {
					UsernamePasswordCredential credential = new UsernamePasswordCredential( username, password );
					manager.createUser( credential, user, licenseCode );
					return credential;
				} catch( HTTPException e ) {
					onError( e );
				} catch( Throwable t ) {
					onError( t );
				}
				return null;
			}

			protected void onError( HTTPException e ) {
				try {
					String errorString = new String();
					JSONObject errorJSON = new JSONObject( e.getBody() );

					if( errorJSON.has( "error_fields" ) ) {
						JSONObject errorFieldsJSON = errorJSON.getJSONObject( "error_fields" );

						if( errorFieldsJSON.length() > 0 ) {
							if( errorFieldsJSON.has( "license_code" ) ) {
								errorString = errorFieldsJSON.getJSONObject( "license_code" ).getString( "error_msg" );
							} else {
								if( !StringUtils.isEmpty( JSONObjectParser.parseFault( JSONParser.parse( e.getBody() ) ).getFields()[0].getMessage() ) ) {
									errorString = JSONObjectParser.parseFault( JSONParser.parse( e.getBody() ) ).getFields()[0].getMessage().toString();
								}
							}
						} else {
							if( !StringUtils.isEmpty( errorJSON.getString( "error_msg" ) ) ) {
								errorString = errorJSON.getString( "error_msg" );
							}
						}
					}

					AccountCreationActivity.this.onError( errorString );
				} catch( Throwable t ) {
					t.printStackTrace();
				}
			}

			protected void onError( Throwable t ) {
				AccountCreationActivity.this.onError( t.getLocalizedMessage() );
			}

			@Override
			protected void onPostExecute( UsernamePasswordCredential credential ) {
				tasks.clear();

				if( credential != null ) {
					activate( credential );
					credential.burn();
				}
			}

			@Override
			protected void onPreExecute() {
				Toast.makeText( getActivity(), getString( R.string.creating_account ), Toast.LENGTH_LONG ).show();
			}

		} ) );

	}

	@Override
	public void onNext( Fragment fragment ) {
		FragmentTransaction tx = getFragmentManager().beginTransaction();

		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
			tx.setCustomAnimations( R.animator.slide_in_from_right, R.animator.slide_out_to_left, R.animator.slide_in_from_left, R.animator.slide_out_to_right );
		}

		if( tx.isAddToBackStackAllowed() ) {
			tx.addToBackStack( null );
		}
		tx.replace( R.id.content, fragment );
		tx.commit();
	}

	@Override
	public void onPrevious( Fragment fragment ) {
		clearTasks();
		getFragmentManager().popBackStack();
	}

	@Override
	protected void onStop() {
		super.onStop();
		clearTasks();
	}

}
