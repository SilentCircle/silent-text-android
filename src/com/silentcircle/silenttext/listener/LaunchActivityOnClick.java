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
package com.silentcircle.silenttext.listener;

import java.lang.ref.SoftReference;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.silentcircle.silenttext.R;

public class LaunchActivityOnClick implements OnClickListener {

	private final Intent intent;
	private final SoftReference<Activity> launcherReference;
	private final int activityNotFoundErrorStringResourceID;

	public LaunchActivityOnClick( Activity launcher, Intent intent, int activityNotFoundErrorStringResourceID ) {
		launcherReference = new SoftReference<Activity>( launcher );
		this.intent = intent;
		this.activityNotFoundErrorStringResourceID = activityNotFoundErrorStringResourceID;
	}

	public LaunchActivityOnClick( Context context, Class<? extends Activity> activityClass ) {
		this( context instanceof Activity ? (Activity) context : null, new Intent( context, activityClass ), 0 );
	}

	public LaunchActivityOnClick( Intent intent ) {
		this( intent, R.string.error_activity_not_found );
	}

	public LaunchActivityOnClick( Intent intent, int activityNotFoundErrorStringResourceID ) {
		this( null, intent, activityNotFoundErrorStringResourceID );
	}

	@Override
	public void onClick( View view ) {
		try {
			Context launcher = launcherReference == null ? null : launcherReference.get();
			if( launcher == null ) {
				launcher = view.getContext();
			}
			launcher.startActivity( intent );
		} catch( ActivityNotFoundException exception ) {
			Toast.makeText( view.getContext(), activityNotFoundErrorStringResourceID, Toast.LENGTH_SHORT ).show();
		}
	}

}
