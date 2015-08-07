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
package com.silentcircle.silenttext.health;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.LockActivity;
import com.silentcircle.silenttext.listener.LaunchActivityOnClick;
import com.silentcircle.silenttext.util.DeviceUtils;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.WarningView;

public class DataStorageHealthCheck {

	public static Intent createSetPasscodeIntent( Context context ) {
		return new Intent( context, LockActivity.class );
	}

	public static WarningView createView( Activity activity ) {
		WarningView view = (WarningView) View.inflate( activity, R.layout.warning, null );
		view.setWarning( R.string.warning_weak_disk_encryption_title, R.string.warning_weak_disk_encryption_description, null );
		view.addAction( R.drawable.ic_action_silent_text, R.string.warning_weak_disk_encryption_action_encrypt_application, new LaunchActivityOnClick( activity, createSetPasscodeIntent( activity ), R.string.none ) );
		if( DeviceUtils.isFullDiskEncryptionSupported( activity ) ) {
			view.addAction( R.drawable.ic_action_device, R.string.warning_weak_disk_encryption_action_encrypt_device, new LaunchActivityOnClick( activity, new Intent( DevicePolicyManager.ACTION_START_ENCRYPTION ), R.string.none ) );
		}
		return view;
	}

	public static boolean test( Context context ) {

		if( !OptionsDrawer.isEmptyPasscode( context ) ) {
			return true;
		}

		if( !DeviceUtils.isFullDiskEncryptionSupported( context ) ) {
			return false;
		}

		return DeviceUtils.isEncrypted( context );

	}

}
