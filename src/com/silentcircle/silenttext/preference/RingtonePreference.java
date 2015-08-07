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
package com.silentcircle.silenttext.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.silentcircle.silenttext.R;

public class RingtonePreference {

	public static interface OnRingtoneChangeListener {

		public void onRingtoneChange( Uri ringtone );

	}

	private static final int REQUEST_CODE = 0xFF & R.id.ringtone;

	public static Uri getDefaultRingtone() {
		return RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION );
	}

	private Uri ringtone;

	private OnRingtoneChangeListener onRingtoneChangeListener;

	public RingtonePreference() {
		this( null );
	}

	public RingtonePreference( Uri ringtone ) {
		this.ringtone = ringtone;
	}

	private Intent getIntent() {

		int ringtoneType = RingtoneManager.TYPE_NOTIFICATION;
		Uri defaultRingtoneURI = RingtoneManager.getDefaultUri( ringtoneType );
		Uri currentRingtoneURI = ringtone != null ? ringtone : defaultRingtoneURI;

		Intent intent = new Intent( RingtoneManager.ACTION_RINGTONE_PICKER );

		intent.putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true );
		intent.putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true );
		intent.putExtra( RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType );
		intent.putExtra( RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultRingtoneURI );
		intent.putExtra( RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneURI );

		return intent;

	}

	public Uri getRingtone() {
		return ringtone;
	}

	public String getRingtoneTitle( Context context ) {
		if( ringtone == null ) {
			return context.getString( R.string.ringtone_none );
		}
		Ringtone r = RingtoneManager.getRingtone( context, ringtone );
		return r != null ? r.getTitle( context ) : null;
	}

	public boolean onActivityResult( int requestCode, int resultCode, Intent extras ) {
		if( REQUEST_CODE == requestCode ) {
			if( resultCode == Activity.RESULT_OK ) {
				setRingtone( (Uri) extras.getParcelableExtra( RingtoneManager.EXTRA_RINGTONE_PICKED_URI ) );
				return true;
			}
		}
		return false;
	}

	public void pickRingtone( Activity activity ) {
		activity.startActivityForResult( getIntent(), REQUEST_CODE );
	}

	public void setOnRingtoneChangeListener( OnRingtoneChangeListener onRingtoneChangeListener ) {
		this.onRingtoneChangeListener = onRingtoneChangeListener;
	}

	public void setRingtone( char [] ringtone ) {
		setRingtone( ringtone != null ? new String( ringtone ) : null );
	}

	public void setRingtone( String ringtone ) {
		setRingtone( ringtone != null ? Uri.parse( ringtone ) : null );
	}

	public void setRingtone( Uri ringtone ) {
		this.ringtone = ringtone;
		if( onRingtoneChangeListener != null ) {
			onRingtoneChangeListener.onRingtoneChange( ringtone );
		}
	}

}
