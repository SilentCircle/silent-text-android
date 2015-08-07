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
package com.silentcircle.silenttext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.silentcircle.silentstorage.io.BufferedBlockCipherFactory;
import com.silentcircle.silentstorage.io.MacFactory;
import com.silentcircle.silentstorage.repository.file.FileRepository;
import com.silentcircle.silentstorage.repository.file.SecureFileRepository;
import com.silentcircle.silenttext.R;

public class UserPreferences {

	public static class Helper extends com.silentcircle.silentstorage.repository.helper.RepositoryHelper<UserPreferences> {

		public Helper() {
			super( new Serializer() );
		}

		@Override
		public char [] identify( UserPreferences preferences ) {
			return preferences == null ? null : preferences.userName;
		}

	}

	public static class Serializer extends com.silentcircle.silentstorage.io.Serializer<UserPreferences> {

		private static final int VERSION = 2;

		private static final int FLAG_PASSCODE_SET = 0x04;
		private static final int FLAG_SEND_DELIVERY_ACKNOWLEDGMENTS = 0x10;
		private static final int FLAG_NOTIFICATION_VIBRATE = 0x20;
		private static final int FLAG_NOTIFICATION_SOUND = 0x40;
		private static final int FLAG_NOTIFICATIONS = 0x80;

		private static final int FLAG_IGNORE_WARNING_DECRYPT_INTERNAL_STORE = 0x100;
		private static final int FLAG_IGNORE_WARNING_DECRYPT_EXTERNAL_STORE = 0x200;

		// Texas

		private static int getFlags( UserPreferences in ) {

			int flags = 0;

			if( in.notifications ) {
				flags |= FLAG_NOTIFICATIONS;
			}

			if( in.notificationSound ) {
				flags |= FLAG_NOTIFICATION_SOUND;
			}

			if( in.notificationVibrate ) {
				flags |= FLAG_NOTIFICATION_VIBRATE;
			}

			if( in.sendDeliveryAcknowledgments ) {
				flags |= FLAG_SEND_DELIVERY_ACKNOWLEDGMENTS;
			}

			if( in.isPasscodeSet ) {
				flags |= FLAG_PASSCODE_SET;
			}

			if( in.ignoreWarningDecryptInternalStore ) {
				flags |= FLAG_IGNORE_WARNING_DECRYPT_INTERNAL_STORE;

			}

			if( in.ignoreWarningDecryptExternalStore ) {
				flags |= FLAG_IGNORE_WARNING_DECRYPT_EXTERNAL_STORE;

			}

			return flags;
		}

		@Override
		public UserPreferences read( DataInputStream in ) throws IOException {
			return read( in, new UserPreferences() );
		}

		@Override
		public UserPreferences read( DataInputStream in, UserPreferences out ) throws IOException {

			int version = in.readInt();

			switch( version ) {

				case 2:

					out.ringtoneName = readChars( in );
					//$FALL-THROUGH$

				case 1:

					out.userName = readChars( in );
					out.deviceName = readChars( in );
					out.passcodeUnlockValidityPeriod = in.readInt();

					int flags = in.readInt();

					out.notifications = ( FLAG_NOTIFICATIONS & flags ) != 0;
					out.notificationSound = ( FLAG_NOTIFICATION_SOUND & flags ) != 0;
					out.notificationVibrate = ( FLAG_NOTIFICATION_VIBRATE & flags ) != 0;
					out.sendDeliveryAcknowledgments = ( FLAG_SEND_DELIVERY_ACKNOWLEDGMENTS & flags ) != 0;
					out.isPasscodeSet = ( FLAG_PASSCODE_SET & flags ) != 0;

					out.ignoreWarningDecryptInternalStore = ( FLAG_IGNORE_WARNING_DECRYPT_INTERNAL_STORE & flags ) != 0;
					out.ignoreWarningDecryptExternalStore = ( FLAG_IGNORE_WARNING_DECRYPT_EXTERNAL_STORE & flags ) != 0;

					break;

			}

			return out;

		}

		@Override
		public UserPreferences write( UserPreferences in, DataOutputStream out ) throws IOException {
			out.writeInt( VERSION );
			writeChars( in.ringtoneName, out );
			writeChars( in.userName, out );
			writeChars( in.deviceName, out );
			out.writeInt( in.passcodeUnlockValidityPeriod );
			out.writeInt( getFlags( in ) );
			return in;
		}

	}

	public static FileRepository<UserPreferences> repository( File root ) {
		return new FileRepository<UserPreferences>( root, new Helper() );
	}

	public static SecureFileRepository<UserPreferences> repository( File root, BufferedBlockCipherFactory cipherFactory, MacFactory macFactory ) {
		return new SecureFileRepository<UserPreferences>( root, new Helper(), cipherFactory, macFactory );
	}

	public char [] userName;
	public char [] deviceName;
	public char [] ringtoneName;
	public boolean notifications;
	public boolean notificationSound;
	public boolean notificationVibrate;
	public boolean sendDeliveryAcknowledgments;
	public boolean isPasscodeSet;
	public int passcodeUnlockValidityPeriod;

	public boolean ignoreWarningDecryptInternalStore;
	public boolean ignoreWarningDecryptExternalStore;

	@Override
	public boolean equals( Object o ) {
		return o instanceof UserPreferences && o.hashCode() == hashCode();
	}

	public String getRingtoneTitle( Context context ) {
		if( !notificationSound ) {
			return context.getString( R.string.ringtone_none );
		}
		Uri uri = getRingtoneURI();
		if( uri == null ) {
			uri = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION );
		}
		Ringtone ringtone = RingtoneManager.getRingtone( context, uri );
		return ringtone != null ? ringtone.getTitle( context ) : context.getString( R.string.ringtone_none );
	}

	public Uri getRingtoneURI() {
		return ringtoneName != null ? Uri.parse( new String( ringtoneName ) ) : null;
	}

	@Override
	public int hashCode() {
		return userName == null ? 0 : Arrays.hashCode( userName );
	}

}
