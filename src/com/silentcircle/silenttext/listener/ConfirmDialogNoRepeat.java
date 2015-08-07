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
package com.silentcircle.silenttext.listener;

import java.lang.ref.SoftReference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Build;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silentcircle.silenttext.R;

public class ConfirmDialogNoRepeat implements OnDismissListener, OnCancelListener {

	protected class ConfirmDialogNoRepeatListener implements DialogInterface.OnClickListener, OnCheckedChangeListener {

		private final OnConfirmNoRepeatListener noRepeatListener;
		private boolean shouldNotShowAgain;

		public ConfirmDialogNoRepeatListener( OnConfirmNoRepeatListener noRepeatListener ) {
			shouldNotShowAgain = false;
			this.noRepeatListener = noRepeatListener;
		}

		@Override
		public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
			shouldNotShowAgain = isChecked;
		}

		@Override
		public void onClick( DialogInterface dialog, int which ) {
			noRepeatListener.onConfirm( ( (AlertDialog) dialog ).getContext(), shouldNotShowAgain );
		}

	}

	private final SoftReference<Activity> launcherReference;
	private final OnConfirmNoRepeatListener onConfirmListener;
	private final DialogInterface.OnClickListener onCancelListener;
	private final int titleResourceID;
	private final int messageResourceID;
	private final int cancelLabelResourceID;
	private final int confirmLabelResourceID;

	public ConfirmDialogNoRepeat( Activity launcher, OnConfirmNoRepeatListener onConfirmListener, DialogInterface.OnClickListener onCancelListener ) {
		this( R.string.are_you_sure, R.string.cannot_be_undone, launcher, onConfirmListener, onCancelListener );
	}

	public ConfirmDialogNoRepeat( int titleResourceID, int messageResourceID, Activity launcher, OnConfirmNoRepeatListener onConfirmListener, DialogInterface.OnClickListener onCancelListener ) {
		this( titleResourceID, messageResourceID, R.string.cancel, R.string.yes, launcher, onConfirmListener, onCancelListener );
	}

	public ConfirmDialogNoRepeat( int titleResourceID, int messageResourceID, int cancelLabelResourceID, int confirmLabelResourceID, Activity launcher, OnConfirmNoRepeatListener onConfirmListener, DialogInterface.OnClickListener onCancelListener ) {
		this.titleResourceID = titleResourceID;
		this.messageResourceID = messageResourceID;
		this.cancelLabelResourceID = cancelLabelResourceID;
		this.confirmLabelResourceID = confirmLabelResourceID;
		launcherReference = new SoftReference<Activity>( launcher );
		this.onConfirmListener = onConfirmListener;
		this.onCancelListener = onCancelListener;
	}

	@Override
	public void onCancel( DialogInterface arg0 ) {
		onCancelListener.onClick( null, 0 );
	}

	@Override
	public void onDismiss( DialogInterface arg0 ) {
		onCancelListener.onClick( null, 0 );
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
	private void setOnDismissListenerForAlert( AlertDialog.Builder alert ) {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
			alert.setOnDismissListener( this );
		} else {
			alert.setOnCancelListener( this );
		}
	}

	public void show() {

		Context context = launcherReference != null ? launcherReference.get() : null;

		if( context == null ) {
			return;
		}

		AlertDialog.Builder alert = new AlertDialog.Builder( context );

		alert.setTitle( titleResourceID );

		ConfirmDialogNoRepeatListener listener = new ConfirmDialogNoRepeatListener( onConfirmListener );

		LinearLayout layout = (LinearLayout) LayoutInflater.from( context ).inflate( R.layout.dialog_with_checkbox, null );

		TextView message = (TextView) layout.findViewById( R.id.message );

		message.setText( messageResourceID );

		CheckBox checkbox = (CheckBox) layout.findViewById( R.id.checkbox );

		checkbox.setOnCheckedChangeListener( listener );
		checkbox.setChecked( false );
		checkbox.setText( R.string.dont_ask_again );

		alert.setView( layout );

		alert.setNegativeButton( cancelLabelResourceID, new DismissDialogOnClick( onCancelListener ) );
		alert.setPositiveButton( confirmLabelResourceID, listener );

		setOnDismissListenerForAlert( alert );

		alert.show();

	}

}
