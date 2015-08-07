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
package com.silentcircle.silenttext.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;

public class LockApplicationOnReceive extends BroadcastReceiver {

	public static void cancel( Context context ) {
		if( context == null ) {
			return;
		}
		NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
		if( manager != null ) {
			manager.cancel( R.id.lock );
		}
	}

	public static void prompt( Context context ) {

		NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );

		PendingIntent lock = PendingIntent.getBroadcast( context, 0, Action.LOCK.intent(), PendingIntent.FLAG_ONE_SHOT );

		NotificationCompat.Builder builder = new NotificationCompat.Builder( context );

		builder.setContentTitle( context.getString( R.string.unlocked_title ) );
		builder.setContentText( context.getString( R.string.unlocked_description, context.getString( R.string.silent_text ) ) );
		builder.setContentIntent( lock );
		builder.setSmallIcon( R.drawable.ic_action_unlock );
		builder.setAutoCancel( true );
		builder.setOngoing( true );

		manager.notify( R.id.lock, builder.build() );

	}

	@Override
	public void onReceive( Context context, Intent intent ) {
		SilentTextApplication application = SilentTextApplication.from( context );
		if( application != null ) {
			application.lock();
		}
	}

}
