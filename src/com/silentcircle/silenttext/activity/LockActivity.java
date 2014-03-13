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

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.receiver.LockApplicationOnReceive;
import com.silentcircle.silenttext.view.OptionsDrawer;

public class LockActivity extends SilentActivity {

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_lock );

		if( Extra.SILENT.test( getIntent() ) ) {
			removePasscodeLock();
			return;
		}

		initializeErrorView();

		( (TextView) findViewById( R.id.passcode_verify ) ).setOnEditorActionListener( new OnEditorActionListener() {

			@Override
			public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
				switch( actionId ) {
					case EditorInfo.IME_ACTION_GO:
						setPasscodeLock();
						hideSoftKeyboard( v );
						return true;
				}
				return false;
			}
		} );

	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getSupportMenuInflater().inflate( R.menu.lock, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.set_passcode:
				setPasscodeLock();
				return true;
			case R.id.remove_passcode:
				removePasscodeLock();
				return true;
		}
		return super.onOptionsItemSelected( item );
	}

	protected void removePasscodeLock() {
		getSilentTextApplication().setUnlockCode( new char [0] );
		OptionsDrawer.setEmptyPasscode( this, true );
		LockApplicationOnReceive.cancel( this );
		toast( R.string.notify_passcode_removed );
		finish();
	}

	protected void setPasscodeLock() {
		CharSequence a = ( (TextView) findViewById( R.id.passcode ) ).getText();
		CharSequence b = ( (TextView) findViewById( R.id.passcode_verify ) ).getText();
		if( a == null || b == null ) {
			showError( R.string.error_invalid_passcode );
			return;
		}
		if( a.length() != b.length() ) {
			showError( R.string.error_passcode_mismatch );
			return;
		}
		if( a.length() < 3 ) {
			showError( R.string.error_invalid_passcode );
			return;
		}
		for( int i = 0; i < a.length(); i++ ) {
			if( a.charAt( i ) != b.charAt( i ) ) {
				showError( R.string.error_passcode_mismatch );
				return;
			}
		}
		toast( R.string.notify_new_passcode_set );
		getSilentTextApplication().setUnlockCode( a );
		OptionsDrawer.setEmptyPasscode( this, false );
		LockApplicationOnReceive.prompt( this );
		finish();
	}

}
