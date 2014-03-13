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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationActivity;

public class SCloudProgressNotification extends BroadcastReceiver {

	private static PendingIntent getCancelIntent( Context context, Intent intent ) {
		Intent cancel = Action.CANCEL.intent();
		Extra.PARTNER.to( cancel, Extra.PARTNER.from( intent ) );
		Extra.ID.to( cancel, Extra.ID.from( intent ) );
		return PendingIntent.getBroadcast( context, 0, cancel, PendingIntent.FLAG_UPDATE_CURRENT );
	}

	private static PendingIntent getContentIntent( Context context, Intent intent ) {
		Intent content = new Intent( context, ConversationActivity.class );
		Extra.PARTNER.to( content, Extra.PARTNER.from( intent ) );
		return PendingIntent.getActivity( context, 0, content, PendingIntent.FLAG_UPDATE_CURRENT );
	}

	@Override
	public void onReceive( Context context, Intent intent ) {

		NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );

		int progress = Extra.PROGRESS.getInt( intent );
		int labelResourceID = Extra.TEXT.getInt( intent );

		if( progress >= 100 ) {
			manager.cancel( R.id.compose );
			return;
		}

		PendingIntent cancel = getCancelIntent( context, intent );
		PendingIntent content = getContentIntent( context, intent );

		NotificationCompat.Builder builder = new NotificationCompat.Builder( context );

		builder.setContentTitle( context.getString( labelResourceID, Integer.valueOf( progress ) ) );
		builder.setContentIntent( content );
		builder.setDeleteIntent( cancel );
		builder.setProgress( 100, progress, false );
		builder.addAction( R.drawable.ic_action_cancel, context.getString( R.string.cancel ), cancel );
		builder.setSmallIcon( R.drawable.ic_action_upload );

		manager.notify( R.id.compose, builder.build() );

	}
}
