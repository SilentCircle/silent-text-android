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
package com.silentcircle.silenttext.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.silentcircle.silenttext.listener.OnHomePressedListener;

public class HomeWatcher {

	class InnerRecevier extends BroadcastReceiver {

		final String SYSTEM_DIALOG_REASON_KEY = "reason";
		final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
		final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
		final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

		@Override
		public void onReceive( Context context, Intent intent ) {
			String action = intent.getAction();
			if( action.equals( Intent.ACTION_CLOSE_SYSTEM_DIALOGS ) ) {
				String reason = intent.getStringExtra( SYSTEM_DIALOG_REASON_KEY );
				if( reason != null ) {
					Log.e( TAG, "action:" + action + ",reason:" + reason );
					if( mListener != null ) {
						if( reason.equals( SYSTEM_DIALOG_REASON_HOME_KEY ) ) {
							mListener.onHomePressed();
						} else if( reason.equals( SYSTEM_DIALOG_REASON_RECENT_APPS ) ) {
							mListener.onHomeLongPressed();
						}
					}
				}
			}
		}
	}

	static final String TAG = "hg";
	private final Context mContext;
	private final IntentFilter mFilter;
	OnHomePressedListener mListener;

	private InnerRecevier mRecevier;

	public HomeWatcher( Context context ) {
		mContext = context;
		mFilter = new IntentFilter( Intent.ACTION_CLOSE_SYSTEM_DIALOGS );
	}

	public void setOnHomePressedListener( OnHomePressedListener listener ) {
		mListener = listener;
		mRecevier = new InnerRecevier();
	}

	public void startWatch() {
		if( mRecevier != null ) {
			mContext.registerReceiver( mRecevier, mFilter );
		}
	}

	public void stopWatch() {
		if( mRecevier != null ) {
			mContext.unregisterReceiver( mRecevier );
		}
	}
}
