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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.silentcircle.silenttext.fragment.SettingsFragment;
import com.silentcircle.silenttext.listener.LaunchActivityOnClick;

public class SettingsActivity extends Activity {

	public static Intent getIntent( Context context ) {
		// return new Intent( context, SettingsActivity.class ).addFlags(
		// Intent.FLAG_ACTIVITY_NO_HISTORY );
		return new Intent( context, SettingsActivity.class );
	}

	public static void setAsTrigger( View v ) {
		if( v != null ) {
			v.setOnClickListener( new LaunchActivityOnClick( getIntent( v.getContext() ) ) );
		}
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		SilentActivity.onActivityResult( this, requestCode, resultCode, data );
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		getFragmentManager().beginTransaction().replace( android.R.id.content, new SettingsFragment() ).commit();
	}

	@Override
	protected void onResume() {

		super.onResume();

		try {
			SilentActivity.assertPermissionToView( this, true, false, false );
		} catch( IllegalStateException exception ) {
			return;
		}

	}

}
