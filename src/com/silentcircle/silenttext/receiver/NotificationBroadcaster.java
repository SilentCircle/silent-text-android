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

import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationActivity;
import com.silentcircle.silenttext.activity.ConversationListActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.UserPreferences;
import com.silentcircle.silenttext.repository.ConversationRepository;

public class NotificationBroadcaster extends BroadcastReceiver {

	public static final String PREFERENCES = "notification_options";
	public static final String KEY_ENABLE_NOTIFICATIONS = "enabled";
	public static final String KEY_ENABLE_SOUND = "sound";
	public static final String KEY_ENABLE_VIBRATE = "vibrate";

	private static final long [] VIBRATE_PATTERN = {
		0,
		250,
		250,
		250
	};

	private static long minimumTimeOfNextNotification = Long.MIN_VALUE;
	private static final long NOTIFICATION_WINDOW = 2 * 1000L;

	public static void cancel( Context context ) {
		NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
		if( manager != null ) {
			manager.cancel( R.id.messages );
		}
	}

	private static Intent createActionIntent( Context context ) {
		Intent intent = new Intent( context, ConversationListActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
		return intent;
	}

	private static Intent createActionIntent( Context context, String partner ) {
		if( partner == null ) {
			return createActionIntent( context );
		}
		Intent intent = new Intent( context, ConversationActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
		intent.putExtra( Extra.PARTNER.getName(), partner );
		return intent;
	}

	private static SilentTextApplication getApplication( Context context ) {
		return (SilentTextApplication) context.getApplicationContext();
	}

	private static ConversationRepository getConversations( Context context ) {
		return getApplication( context ).getConversations();
	}

	public static UserPreferences getPreferences( Context context ) {
		SilentTextApplication application = SilentTextApplication.from( context );
		UserPreferences preferences = application.getGlobalPreferences();
		return preferences == null ? application.createDefaultUserPreferences() : preferences;
	}

	public static SharedPreferences getPreferencesLegacy( Context context ) {
		return context.getSharedPreferences( PREFERENCES, Context.MODE_PRIVATE );
	}

	public static Uri getRingtone( Context context ) {
		return isEnabled( context ) ? getPreferences( context ).getRingtoneURI() : null;
	}

	private static boolean isActivated( Context context ) {
		return SilentTextApplication.from( context ).isUserKeyUnlocked();
	}

	public static boolean isApplicationInForeground( Context context ) {
		return SilentTextApplication.from( context ).isApplicationInForeground();
	}

	public static boolean isEnabled( Context context ) {
		return getPreferences( context ).notifications;
	}

	public static boolean isEnabled( SharedPreferences preferences ) {
		return preferences.getBoolean( KEY_ENABLE_NOTIFICATIONS, true );
	}

	public static boolean isEnabledLegacy( Context context ) {
		return isEnabled( getPreferencesLegacy( context ) );
	}

	public static boolean isOnline( Context context ) {
		return SilentTextApplication.from( context ).isXMPPTransportConnected();
	}

	public static boolean isReadyForNextNotification() {
		long now = SystemClock.elapsedRealtime();
		return now >= minimumTimeOfNextNotification;
	}

	public static boolean isSoundEnabled( Context context ) {
		AudioManager audioManager = (AudioManager) context.getSystemService( Context.AUDIO_SERVICE );
		return isEnabled( context ) && getPreferences( context ).notificationSound && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
	}

	public static boolean isSoundEnabled( SharedPreferences preferences ) {
		return isEnabled( preferences ) && preferences.getBoolean( KEY_ENABLE_SOUND, true );
	}

	public static boolean isSoundEnabledLegacy( Context context ) {
		return isSoundEnabled( getPreferencesLegacy( context ) );
	}

	private static boolean isUnlocked( Context context ) {
		return SilentTextApplication.from( context ).isUnlocked();
	}

	public static boolean isVibrateEnabled( Context context ) {
		AudioManager audioManager = (AudioManager) context.getSystemService( Context.AUDIO_SERVICE );
		return isEnabled( context ) && getPreferences( context ).notificationVibrate && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
	}

	public static boolean isVibrateEnabled( SharedPreferences preferences ) {
		return isEnabled( preferences ) && preferences.getBoolean( KEY_ENABLE_VIBRATE, true );
	}

	public static boolean isVibrateEnabledLegacy( Context context ) {
		return isVibrateEnabled( getPreferencesLegacy( context ) );
	}

	private static void playNotificationSound( Context context ) {
		if( !isSoundEnabled( context ) ) {
			return;
		}
		Uri uri = getRingtone( context );
		if( uri == null ) {
			uri = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION );
		}
		Ringtone ringtone = RingtoneManager.getRingtone( context.getApplicationContext(), uri );
		if( ringtone != null && !ringtone.isPlaying() ) {
			ringtone.play();
		}
	}

	private static void playNotificationVibration( Context context ) {
		if( !isVibrateEnabled( context ) ) {
			return;
		}
		Vibrator vibrator = (Vibrator) context.getSystemService( Context.VIBRATOR_SERVICE );
		vibrator.vibrate( VIBRATE_PATTERN, -1 );
	}

	private static void sendNotification( Context context, boolean silent ) {

		boolean foreground = isApplicationInForeground( context );
		boolean enabled = isEnabled( context );
		boolean unlocked = isUnlocked( context );
		boolean activated = isActivated( context );
		boolean online = isOnline( context );

		NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );

		if( foreground ) {
			manager.cancel( R.id.messages );
			if( !silent ) {
				playNotificationVibration( context );
			}
			return;
		}

		if( !enabled ) {
			return;
		}

		if( unlocked && !activated ) {
			return;
		}

		NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
		int unreadMessageCount = 0;
		List<String> partners = new ArrayList<String>();
		if( unlocked ) {
			List<Conversation> conversations = getConversations( context ).list();
			for( int i = 0; i < conversations.size(); i++ ) {
				Conversation conversation = conversations.get( i );
				if( conversation.containsUnreadMessages() ) {
					unreadMessageCount += conversation.getUnreadMessageCount();
					partners.add( conversation.getPartner().getUsername() );
					style.addLine( context.getResources().getQuantityString( R.plurals.notify_unread_messages_from_individual, conversation.getUnreadMessageCount(), Integer.valueOf( conversation.getUnreadMessageCount() ), withoutDomain( context, conversation.getPartner().getUsername() ) ) );
				}
			}
		}

		if( !online ) {
			unreadMessageCount++;
			partners.add( null );
		}

		if( unreadMessageCount < 1 ) {
			manager.cancel( R.id.messages );
			return;
		}

		String title = context.getResources().getQuantityString( R.plurals.notify_new_messages_title, unreadMessageCount, Integer.valueOf( unreadMessageCount ) );
		String subtitle = context.getResources().getQuantityString( R.plurals.notify_new_messages_subtitle, partners.size(), Integer.valueOf( partners.size() ), context.getResources().getQuantityString( R.plurals.n_messages, unreadMessageCount, Integer.valueOf( unreadMessageCount ) ) );

		Intent actionIntent = partners.size() > 1 ? createActionIntent( context ) : createActionIntent( context, partners.get( 0 ) );

		PendingIntent intent = PendingIntent.getActivity( context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT );

		NotificationCompat.Builder notification = new NotificationCompat.Builder( context );

		style.setBigContentTitle( title );

		notification.setLights( Color.GREEN, 100, 1000 );
		notification.setPriority( NotificationCompat.PRIORITY_HIGH );
		notification.setOnlyAlertOnce( true );
		notification.setAutoCancel( !online );
		notification.setStyle( style );
		notification.setSmallIcon( R.drawable.ic_notify_solid );
		notification.setContentIntent( intent );
		notification.setNumber( unreadMessageCount );
		notification.setContentTitle( title );
		notification.setContentText( subtitle );

		if( !silent ) {
			notification.setTicker( subtitle );
			playNotificationSound( context );
			playNotificationVibration( context );
		}

		manager.notify( R.id.messages, notification.build() );

	}

	private static void setDelayForNextNotification() {
		minimumTimeOfNextNotification = SystemClock.elapsedRealtime() + NOTIFICATION_WINDOW;
	}

	private static String withoutDomain( Context context, String emailAddress ) {
		return emailAddress == null ? context.getString( R.string.someone ) : emailAddress.replaceAll( "@.+$", "" );
	}

	@Override
	public void onReceive( Context context, Intent intent ) {
		if( isReadyForNextNotification() ) {
			setDelayForNextNotification();
			sendNotification( context, Extra.SILENT.test( intent ) );
		}
	}

}
