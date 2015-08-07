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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationListActivity;
import com.silentcircle.silenttext.util.Constants;

/**
 * Created by rli on 01/30/15.
 */
public class SCDialogFragment extends DialogFragment {

	private static String MESSAGE = "message";
	private static String TITLE = "title";
	private static String POSITIVE_BTN_LABEL = "positive_button_label";
	private static String NAGETIVE_BTN_LABEL = "negative_button_label";
	static String WHAT = "what";

	public static SCDialogFragment newInstance( String title, String msg, int positiveBtnLabel, int negativeBtnLabel, int what ) {
		SCDialogFragment f = new SCDialogFragment();

		Bundle args = new Bundle();
		args.putString( TITLE, title );
		args.putString( MESSAGE, msg );
		args.putInt( POSITIVE_BTN_LABEL, positiveBtnLabel );
		args.putInt( NAGETIVE_BTN_LABEL, negativeBtnLabel );
		args.putInt( WHAT, what );

		f.setArguments( args );

		return f;
	}

	private Activity mParent;

	public SCDialogFragment() {
	}

	@Override
	public void onAttach( Activity activity ) {
		super.onAttach( activity );
		mParent = activity;
	}

	@Override
	public Dialog onCreateDialog( Bundle savedInstanceState ) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder( mParent );
		final Bundle args = getArguments();
		if( args == null ) {
			return null;
		}
		builder.setTitle( args.getString( TITLE ) ).setMessage( args.getString( MESSAGE ) );
		if( args.getInt( POSITIVE_BTN_LABEL, -1 ) > 0 ) {
			builder.setPositiveButton( args.getInt( POSITIVE_BTN_LABEL ), new DialogInterface.OnClickListener() {

				@Override
				public void onClick( DialogInterface dialog, int id ) {
					if( args.getInt( WHAT, -1 ) == Constants.DIRECTORY_SEARCH_DIALOG ) {
						Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage( Constants.SPA_PACKAGE_NAME );
						if( intent != null ) {
							getActivity().startActivity( intent );
						} else {

							SCDialogFragment mSCDialog = SCDialogFragment.newInstance( getResources().getString( R.string.directory_search_dialog_information_title ), getResources().getString( R.string.directory_search_dialog_no_spa_msg ), android.R.string.ok, android.R.string.cancel, Constants.DIRECTORY_SEARCH_DIALOG_NO_SPA );
							mSCDialog.setCancelable( false );
							mSCDialog.show( getFragmentManager(), ConversationListActivity.SCDialog_TAG );

						}
					} else if( args.getInt( WHAT, -1 ) == Constants.DIRECTORY_SEARCH_DIALOG_NO_SPA ) {
						Intent intent = new Intent( Intent.ACTION_VIEW );
						intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
						intent.setData( Uri.parse( "market://details?id=" + Constants.SPA_PACKAGE_NAME ) );
						getActivity().startActivity( intent );
					}
				}

			} );
		}
		if( args.getInt( NAGETIVE_BTN_LABEL, -1 ) > 0 ) {
			builder.setNegativeButton( args.getInt( NAGETIVE_BTN_LABEL ), new DialogInterface.OnClickListener() {

				@Override
				public void onClick( DialogInterface dialog, int id ) {
					if( args.getInt( WHAT, -1 ) == Constants.DIRECTORY_SEARCH_DIALOG ) {
						// TODO: do nothing for now
						dialog.dismiss();
					} else if( args.getInt( WHAT, -1 ) == Constants.DIRECTORY_SEARCH_DIALOG_NO_SPA ) {
						// TODO: do nothing for now
						dialog.dismiss();
					}
				}
			} );
		}
		// Create the AlertDialog object and return it
		return builder.create();
	}
}
