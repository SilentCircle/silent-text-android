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

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;
import android.view.View;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.UserPreferences;

public class SilentRingtonePreference extends RingtonePreference {

	public static char [] getRingtoneName( Context context ) {
		SilentTextApplication application = SilentTextApplication.from( context );
		UserPreferences preferences = application.getGlobalPreferences();
		if( preferences.notificationSound ) {
			if( preferences.ringtoneName == null ) {
				try {
					return com.silentcircle.silenttext.preference.RingtonePreference.getDefaultRingtone().toString().toCharArray();
				} catch( NullPointerException exception ) {
					return preferences.ringtoneName;
				}
			}
			return preferences.ringtoneName;
		}
		return preferences.ringtoneName;
	}

	public static String getRingtoneTitle( Context context ) {
		Uri ringtone = getRingtoneURI( context );
		Ringtone r = ringtone != null ? RingtoneManager.getRingtone( context, ringtone ) : null;
		return r != null ? r.getTitle( context ) : context.getString( R.string.ringtone_none );
	}

	public static Uri getRingtoneURI( Context context ) {
		char [] ringtoneName = getRingtoneName( context );
		return ringtoneName != null ? Uri.parse( new String( ringtoneName ) ) : null;
	}

	public SilentRingtonePreference( Context context ) {
		super( context );
	}

	public SilentRingtonePreference( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public SilentRingtonePreference( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	@Override
	protected void onBindView( View view ) {
		super.onBindView( view );
		updateSummary();
	}

	@Override
	protected Uri onRestoreRingtone() {
		return getRingtoneURI( getContext() );
	}

	@Override
	protected void onSaveRingtone( Uri ringtoneUri ) {
		Context context = getContext();
		SilentTextApplication application = SilentTextApplication.from( context );
		UserPreferences preferences = application.getGlobalPreferences();
		preferences.ringtoneName = ringtoneUri != null ? ringtoneUri.toString().toCharArray() : null;
		preferences.notificationSound = ringtoneUri != null;
		application.saveApplicationPreferences( preferences );
		updateSummary();
	}

	public void updateSummary() {
		setSummary( getRingtoneTitle( getContext() ) );
	}

}
