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
package com.silentcircle.silenttext.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.health.DataStorageHealthCheck;
import com.silentcircle.silenttext.util.DeviceUtils;

public class WelcomeView extends LinearLayout implements OnClickListener {

	static class Views {

		public TextView header;
		public LinearLayout encryptDeviceTip;
		public LinearLayout setPasscodeTip;
		public TextView encryptDeviceAction;
		public TextView setPasscodeAction;

		public Views( WelcomeView parent ) {
			header = (TextView) parent.findViewById( R.id.welcome_tips_header );
			encryptDeviceTip = (LinearLayout) parent.findViewById( R.id.tip_encrypt_device );
			setPasscodeTip = (LinearLayout) parent.findViewById( R.id.tip_set_passcode );
			encryptDeviceAction = (TextView) encryptDeviceTip.findViewById( R.id.action_encrypt_device );
			setPasscodeAction = (TextView) setPasscodeTip.findViewById( R.id.action_set_passcode );
			encryptDeviceAction.setOnClickListener( parent );
			setPasscodeAction.setOnClickListener( parent );
		}

	}

	private Views views;

	public WelcomeView( Context context ) {
		super( context );
	}

	public WelcomeView( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	public WelcomeView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	protected Views getViews() {
		if( views == null ) {
			views = new Views( this );
		}
		return views;
	}

	@Override
	public void onClick( View v ) {
		Context context = v.getContext();
		int id = v.getId();
		if( R.id.action_encrypt_device == id ) {
			context.startActivity( DeviceUtils.createEncryptDeviceIntent() );
			return;
		}
		if( R.id.action_set_passcode == id ) {
			context.startActivity( DataStorageHealthCheck.createSetPasscodeIntent( context ) );
			return;
		}
	}

	public void update() {
		Views v = getViews();
		boolean passcodeEmpty = OptionsDrawer.isEmptyPasscode( getContext() );
		boolean supported = DeviceUtils.isFullDiskEncryptionSupported( getContext() );
		boolean encrypted = supported && DeviceUtils.isEncrypted( getContext() );
		v.setPasscodeTip.setVisibility( passcodeEmpty ? VISIBLE : GONE );
		v.encryptDeviceTip.setVisibility( supported && !encrypted ? VISIBLE : GONE );
		v.header.setText( !passcodeEmpty && ( !supported || encrypted ) ? R.string.welcome_no_tips : R.string.welcome_tips_header );
	}

}
