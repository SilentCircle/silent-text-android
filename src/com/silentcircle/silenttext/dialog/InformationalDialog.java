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
package com.silentcircle.silenttext.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.silentcircle.silenttext.listener.DismissDialogOnClick;

public class InformationalDialog implements OnClickListener {

	public static void show( Context context, int titleStringResourceID, int contentLayoutResourceID ) {
		LayoutInflater inflater = LayoutInflater.from( context );
		View contentView = inflater.inflate( contentLayoutResourceID, null, false );
		show( context, titleStringResourceID, contentView );
	}

	public static void show( Context context, int titleStringResourceID, View contentView ) {

		AlertDialog.Builder dialog = new AlertDialog.Builder( context );

		dialog.setTitle( titleStringResourceID );
		dialog.setView( contentView );
		dialog.setPositiveButton( android.R.string.ok, new DismissDialogOnClick() );

		dialog.create().show();

	}

	private final int titleStringResourceID;
	private final int contentLayoutResourceID;

	public InformationalDialog( int titleStringResourceID, int contentLayoutResourceID ) {
		this.titleStringResourceID = titleStringResourceID;
		this.contentLayoutResourceID = contentLayoutResourceID;
	}

	@Override
	public void onClick( View v ) {
		show( v.getContext(), titleStringResourceID, contentLayoutResourceID );
	}

}
