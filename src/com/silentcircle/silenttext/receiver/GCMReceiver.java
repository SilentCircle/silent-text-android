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
package com.silentcircle.silenttext.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.service.GCMService;

public class GCMReceiver extends BroadcastReceiver {

	private static final String ACTION_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
	private static final String ACTION_REGISTER = "com.google.android.c2dm.intent.REGISTRATION";

	private static final String EXTRA_REGISTRATION_ID = "registration_id";

	private static void onPush( Context context ) {
		SilentTextApplication.from( context ).onPushNotification();
	}

	private static void onRegister( Context context, Intent intent ) {
		String registrationID = intent.getStringExtra( EXTRA_REGISTRATION_ID );
		if( registrationID == null ) {
			return;
		}
		SilentTextApplication.from( context ).onNewPushNotificationToken( registrationID );
	}

	private static void onStart( Context context ) {
		Intent service = new Intent( context, GCMService.class );
		context.startService( service );
	}

	private static void onStop( Context context ) {
		Intent service = new Intent( context, GCMService.class );
		context.stopService( service );
	}

	@Override
	public void onReceive( Context context, Intent intent ) {

		if( Intent.ACTION_BOOT_COMPLETED.equals( intent.getAction() ) ) {
			onStart( context );
			return;
		}

		if( Intent.ACTION_SHUTDOWN.equals( intent.getAction() ) ) {
			onStop( context );
			return;
		}

		if( ACTION_REGISTER.equals( intent.getAction() ) ) {
			onRegister( context, intent );
			return;
		}

		if( ACTION_RECEIVE.equals( intent.getAction() ) ) {
			onPush( context );
			return;
		}

	}

}
