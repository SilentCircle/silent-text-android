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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.receiver.LockApplicationOnReceive;
import com.silentcircle.silenttext.task.ResetPassphraseTask;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.view.OptionsDrawer;

public class LockActivity extends SilentActivity implements OnClickListener, OnEditorActionListener, OnCheckedChangeListener {

	static class Views {

		public final EditText currentPassphrase;
		public final EditText newPassphrase;
		public final View save;
		public final View cancel;
		public final CheckBox reveal;

		Views( LockActivity parent ) {

			currentPassphrase = (EditText) parent.findViewById( R.id.passcode_previous );

			newPassphrase = (EditText) parent.findViewById( R.id.passcode );
			if( newPassphrase != null ) {
				newPassphrase.setOnEditorActionListener( parent );
			}

			save = parent.findViewById( R.id.button_set_passcode );
			if( save != null ) {
				save.setOnClickListener( parent );
			}

			cancel = parent.findViewById( R.id.button_cancel );
			if( cancel != null ) {
				cancel.setOnClickListener( parent );
			}

			reveal = (CheckBox) parent.findViewById( R.id.passcode_reveal );
			if( reveal != null ) {
				reveal.setOnCheckedChangeListener( parent );
			}

		}

	}

	private Views views;
	public static final String PASS_CODE_SET = "passcode_is_set";

	private Views getViews() {
		if( views == null ) {
			views = new Views( this );
		}
		return views;
	}

	@Override
	public void onCheckedChanged( CompoundButton view, boolean isChecked ) {
		Views v = getViews();
		if( v.reveal.equals( view ) ) {
			int type = InputType.TYPE_CLASS_TEXT;
			if( isChecked ) {
				type |= InputType.TYPE_TEXT_VARIATION_NORMAL;
				type |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			} else {
				type |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}
			v.newPassphrase.setInputType( type );
			v.currentPassphrase.setInputType( type );
			return;
		}
	}

	@Override
	public void onClick( View view ) {

		Views v = getViews();

		if( v.save.equals( view ) ) {
			setPasscodeLock();
			return;
		}

		if( v.cancel.equals( view ) ) {
			setResult( RESULT_CANCELED );
			finish();
			return;
		}

	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_lock );

		if( Extra.SILENT.test( getIntent() ) ) {
			removePasscodeLock();
			return;
		}

		Views v = getViews();

		v.currentPassphrase.setVisibility( OptionsDrawer.isEmptyPasscode( this ) ? View.GONE : View.VISIBLE );

		initializeErrorView();

	}

	@Override
	public boolean onEditorAction( TextView view, int actionId, KeyEvent event ) {
		Views v = getViews();
		if( v.newPassphrase.equals( view ) ) {
			switch( actionId ) {
				case EditorInfo.IME_ACTION_GO:
					setPasscodeLock();
					hideSoftKeyboard( view );
					return true;
			}
		}
		return false;
	}

	protected void removePasscodeLock() {
		getSilentTextApplication().setEncryptionPassPhrase( new char [0] );
		OptionsDrawer.setEmptyPasscode( this, true );
		LockApplicationOnReceive.cancel( this );
		toast( R.string.notify_passcode_removed );
		finish();
	}

	protected void setPasscodeLock() {

		Views v = getViews();

		CharSequence currentPassphrase = v.currentPassphrase.getText();
		CharSequence newPassphrase = v.newPassphrase.getText();

		if( !OptionsDrawer.isEmptyPasscode( this ) && ( currentPassphrase == null || currentPassphrase.length() < 3 ) ) {
			showError( R.string.error_invalid_passcode );
			return;
		}

		if( newPassphrase == null || newPassphrase.length() < 3 ) {
			showError( R.string.error_invalid_passcode );
			return;
		}

		beginLoading( R.id.content );

		AsyncUtils.execute( new ResetPassphraseTask( this ) {

			@Override
			protected void onPassphraseReset() {
				// Passcode is set.
				SilentTextApplication app = SilentTextApplication.from( LockActivity.this );
				SharedPreferences prefs = app.getSharedPreferences( PASS_CODE_SET, Context.MODE_PRIVATE );
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean( PASS_CODE_SET, true ).apply();

				toast( R.string.notify_new_passcode_set );
				OptionsDrawer.setEmptyPasscode( getActivity(), false );
				LockApplicationOnReceive.prompt( getActivity() );
				finish();
			}

			@Override
			protected void onPassphraseResetFailed() {
				finishLoading( R.id.content );
				showError( R.string.error_incorrect_passcode );
			}

		}, currentPassphrase, newPassphrase );

	}

}
