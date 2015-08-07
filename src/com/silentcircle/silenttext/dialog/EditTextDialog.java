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
package com.silentcircle.silenttext.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class EditTextDialog implements DialogInterface.OnClickListener, OnEditorActionListener {

	public static interface Callback {

		public void onValue( int dialogID, CharSequence value );

	}

	public static void show( Context context, int dialogID, int titleStringResourceID, CharSequence currentValue, CharSequence defaultValue, Callback callback ) {
		int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
		show( context, dialogID, titleStringResourceID, currentValue, defaultValue, inputType, callback );
	}

	public static void show( Context context, int dialogID, int titleStringResourceID, CharSequence currentValue, CharSequence defaultValue, int inputType, Callback callback ) {

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( context );

		dialogBuilder.setTitle( titleStringResourceID );

		EditText input = new EditText( context );
		EditTextDialog handler = new EditTextDialog( dialogID, input, callback );

		input.setInputType( inputType );
		input.setImeOptions( EditorInfo.IME_ACTION_DONE );
		input.setText( currentValue );
		input.setHint( defaultValue );
		input.setOnEditorActionListener( handler );

		dialogBuilder.setView( input );

		dialogBuilder.setNegativeButton( android.R.string.cancel, handler );
		dialogBuilder.setPositiveButton( android.R.string.ok, handler );

		AlertDialog dialog = dialogBuilder.create();

		handler.dialog = dialog;

		dialog.show();

	}

	private DialogInterface dialog;
	private final TextView in;
	private final Callback out;
	private final int id;

	EditTextDialog( int id, TextView in, Callback out ) {
		this.id = id;
		this.in = in;
		this.out = out;
	}

	@Override
	public void onClick( DialogInterface dialog, int which ) {
		if( which == DialogInterface.BUTTON_POSITIVE ) {
			reportValue();
		} else {
			dialog.dismiss();
		}
	}

	@Override
	public boolean onEditorAction( TextView view, int action, KeyEvent event ) {

		if( action == EditorInfo.IME_ACTION_DONE ) {
			reportValue();
			if( dialog != null ) {
				dialog.dismiss();
			}
			return true;
		}

		return false;

	}

	private void reportValue() {
		out.onValue( id, in.getText() );
	}

}
