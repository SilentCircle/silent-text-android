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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.receiver.LockApplicationOnReceive;

public class UnlockActivity extends SilentActivity {

	protected boolean auto;

	private AsyncTask<CharSequence, Void, Boolean> task;

	private Intent nextIntent;

	@Override
	protected void beginLoading( int contentViewId ) {
		super.beginLoading( contentViewId );
		getSupportActionBar().hide();
	}

	private void cancelTask() {
		if( task != null ) {
			task.cancel( true );
			task = null;
		}
	}

	@Override
	protected void finishLoading( int contentViewId ) {
		super.finishLoading( contentViewId );
		getSupportActionBar().show();
	}

	private void handleIntent( Intent intent ) {
		if( Extra.FORCE.test( intent ) ) {
			getSilentTextApplication().lock();
		}
		nextIntent = Extra.NEXT.getParcelable( intent );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_unlock );
		beginLoading( R.id.passcode );

		if( getIntent() != null ) {
			handleIntent( getIntent() );
		}

		initializeErrorView();

		EditText view = (EditText) findViewById( R.id.passcode );

		if( view != null ) {

			view.setOnEditorActionListener( new OnEditorActionListener() {

				@Override
				public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
					switch( actionId ) {
						case EditorInfo.IME_ACTION_GO:
							unlock();
							hideSoftKeyboard( v );
							return true;
					}
					return false;
				}

			} );

		}

	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getSupportMenuInflater().inflate( R.menu.unlock, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onNewIntent( Intent intent ) {
		super.onNewIntent( intent );
		setIntent( intent );
		handleIntent( intent );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.unlock:
				unlock();
				return true;
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onPause() {
		super.onPause();
		cancelTask();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( isUnlocked() ) {
			finish();
			return;
		}
		auto = true;
		unlock();
	}

	protected void setAsLocked() {
		if( !auto ) {
			showError( R.string.error_incorrect_passcode );
		}
		auto = false;
		LockApplicationOnReceive.cancel( this );
		findEditTextById( R.id.passcode ).setText( "" );
		finishLoading( R.id.passcode );
	}

	public void setAsUnlocked() {
		if( !auto ) {
			LockApplicationOnReceive.prompt( this );
		}
		if( nextIntent != null ) {
			startActivity( nextIntent );
			nextIntent = null;
		} else {
			setResult( RESULT_OK );
			finish();
		}
	}

	protected void unlock() {

		cancelTask();

		task = new AsyncTask<CharSequence, Void, Boolean>() {

			@Override
			protected Boolean doInBackground( CharSequence... codes ) {
				return Boolean.valueOf( unlock( codes[0] ) );
			}

			@Override
			protected void onPostExecute( Boolean success ) {
				if( success.booleanValue() ) {
					setAsUnlocked();
				} else {
					setAsLocked();
				}
			}

			@Override
			protected void onPreExecute() {
				beginLoading( R.id.passcode );
			}

		}.execute( findEditTextById( R.id.passcode ).getText() );

	}

	protected boolean unlock( CharSequence code ) {
		try {
			getSilentTextApplication().unlock( code );
			return isUnlocked();
		} catch( Exception exception ) {
			log.warn( "Failed to unlock: %s", exception.getMessage() );
			return false;
		}
	}

}
