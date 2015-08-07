/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

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

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ShareActivity;
import com.silentcircle.silenttext.util.MessageUtils;

/**
 * Created by rli on 04/14/15.
 */
public class ShareDialogFragment extends DialogFragment {

	public static String SAVED_ID = "saved_id";

	public static ShareDialogFragment newInstance( String savedId ) {
		ShareDialogFragment f = new ShareDialogFragment();

		Bundle args = new Bundle();
		args.putString( SAVED_ID, savedId );

		f.setArguments( args );

		return f;
	}

	static String withoutDomain( String fullAddress ) {
		return fullAddress == null ? null : fullAddress.replaceAll( "^(.+)@(.+)$", "$1" );
	}

	// @Override
	// public void onStart() {
	// super.onStart();
	// AlertDialog d = (AlertDialog) getDialog();
	// if( d != null ) {
	// Button positiveButton = d.getButton( DialogInterface.BUTTON1 );
	// positiveButton.setOnClickListener( new View.OnClickListener() {
	//
	// @Override
	// public void onClick( View v ) {
	// if( mWhich != -1 ) {
	// dismiss();
	// }
	// }
	// } );
	// }
	// }

	Activity mParent;

	int mWhich = 0;

	public ShareDialogFragment() {
	}

	@Override
	public void onAttach( Activity activity ) {
		super.onAttach( activity );
		mParent = activity;
	}

	@Override
	public Dialog onCreateDialog( Bundle savedInstanceState ) {
		AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
		final Bundle args = getArguments();
		if( args == null ) {
			return null;
		}

		builder.setTitle( "Select App" ).setSingleChoiceItems( R.array.share_app_array, mWhich, new DialogInterface.OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				mWhich = which;
			}

		} ).setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int id ) {
				if( mWhich == 0 ) {
					Intent intent = new Intent( mParent, ShareActivity.class );
					intent.putExtra( SAVED_ID, args.getString( SAVED_ID ) );
					mParent.startActivity( intent );
				} else if( mWhich == 1 ) {
					Intent intent = new Intent( Intent.ACTION_SENDTO, Uri.fromParts( "mailto", "", null ) );
					intent.putExtra( android.content.Intent.EXTRA_SUBJECT, mParent.getString( R.string.conversation_with, withoutDomain( args.getString( SAVED_ID ) ) ) );
					intent.putExtra( android.content.Intent.EXTRA_STREAM, Uri.fromFile( new File( MessageUtils.getFileAbsolutePath() ) ) );
					mParent.startActivity( intent );

				} else {
					Toast.makeText( mParent, "You did not select app to share the saved conversation", Toast.LENGTH_LONG ).show();
				}
			}
		} ).setNegativeButton( android.R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int id ) {
				// TODO:
			}
		} );

		return builder.create();
	}
}
