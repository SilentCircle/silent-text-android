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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.fragment.AccountCreationWelcomeFragment;
import com.silentcircle.silenttext.fragment.FragmentFlowListener;
import com.silentcircle.silenttext.fragment.HasFlow;
import com.silentcircle.silenttext.task.CreateAccountTask;
import com.silentcircle.silenttext.thread.ViewAnimator;

public class AccountCreationActivity extends SherlockFragmentActivity implements FragmentFlowListener {

	protected Handler handler;
	protected ViewAnimator hideError;
	protected final List<AsyncTask<?, ?, ?>> tasks = new ArrayList<AsyncTask<?, ?, ?>>();

	protected void activate( CharSequence activationCode ) {
		if( activationCode == null ) {
			return;
		}
		startActivity( new Intent( Intent.ACTION_VIEW, Uri.fromParts( "silentcircleprovision", activationCode.toString(), null ) ) );
		Intent data = new Intent();
		Extra.ACTIVATION_CODE.to( data, activationCode );
		setResult( RESULT_OK, data );
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

		Fragment fragment = getSupportFragmentManager().findFragmentById( R.id.content );

		if( fragment == null ) {
			fragment = AccountCreationWelcomeFragment.create( new Bundle() );
			getSupportFragmentManager().beginTransaction().add( R.id.content, fragment ).commit();
		}

		if( fragment instanceof HasFlow ) {
			( (HasFlow) fragment ).setFlowListener( this );
		}

		getSupportFragmentManager().addOnBackStackChangedListener( new OnBackStackChangedListener() {

			@Override
			public void onBackStackChanged() {
				Fragment f = getSupportFragmentManager().findFragmentById( R.id.content );
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
	public void onFinish( Bundle arguments ) {

		if( !tasks.isEmpty() ) {
			return;
		}

		tasks.add( new CreateAccountTask( SilentTextApplication.from( this ).getAccountCreationClient() ) {

			@Override
			protected void onError( String message ) {
				AccountCreationActivity.this.onError( message );
			}

			@Override
			protected void onPostExecute( CharSequence activationCode ) {
				tasks.clear();
				activate( activationCode );
			}

			@Override
			protected void onPreExecute() {
				Toast.makeText( getActivity(), getString( R.string.creating_account ), Toast.LENGTH_LONG ).show();
			}

		}.execute( arguments ) );

	}

	@Override
	public void onNext( Fragment fragment ) {
		FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
		tx.setCustomAnimations( R.anim.slide_in_from_right, R.anim.slide_out_to_left, R.anim.slide_in_from_left, R.anim.slide_out_to_right );
		if( tx.isAddToBackStackAllowed() ) {
			tx.addToBackStack( null );
		}
		tx.replace( R.id.content, fragment );
		tx.commit();
	}

	@Override
	public void onPrevious( Fragment fragment ) {
		clearTasks();
		getSupportFragmentManager().popBackStack();
	}

	@Override
	protected void onStop() {
		super.onStop();
		clearTasks();
	}

}
