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
package com.silentcircle.silenttext.listener;

import java.lang.ref.SoftReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;

import com.silentcircle.silenttext.R;

public class LaunchConfirmDialogOnClick implements OnClickListener {

	private final SoftReference<Activity> launcherReference;
	private final OnConfirmListener onConfirmListener;
	private final int titleResourceID;
	private final int messageResourceID;
	private final int cancelLabelResourceID;
	private final int confirmLabelResourceID;

	public LaunchConfirmDialogOnClick( Activity launcher, OnConfirmListener onConfirmListener ) {
		this( R.string.are_you_sure, R.string.cannot_be_undone, launcher, onConfirmListener );
	}

	public LaunchConfirmDialogOnClick( int titleResourceID, int messageResourceID, Activity launcher, OnConfirmListener onConfirmListener ) {
		this( titleResourceID, messageResourceID, R.string.cancel, R.string.yes, launcher, onConfirmListener );
	}

	public LaunchConfirmDialogOnClick( int titleResourceID, int messageResourceID, int cancelLabelResourceID, int confirmLabelResourceID, Activity launcher, OnConfirmListener onConfirmListener ) {
		this.titleResourceID = titleResourceID;
		this.messageResourceID = messageResourceID;
		this.cancelLabelResourceID = cancelLabelResourceID;
		this.confirmLabelResourceID = confirmLabelResourceID;
		launcherReference = new SoftReference<Activity>( launcher );
		this.onConfirmListener = onConfirmListener;
	}

	@Override
	public void onClick( View v ) {

		Context launcher = launcherReference == null ? null : launcherReference.get();
		if( launcher == null ) {
			launcher = v.getContext();
		}
		AlertDialog.Builder alert = new AlertDialog.Builder( launcher );

		alert.setTitle( titleResourceID );
		alert.setMessage( messageResourceID );

		alert.setNegativeButton( cancelLabelResourceID, new DismissDialogOnClick() );
		alert.setPositiveButton( confirmLabelResourceID, new ConfirmOnClick( onConfirmListener ) );

		alert.show();

	}

}
