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
package com.silentcircle.silenttext.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.client.JabberClient;
import com.silentcircle.silenttext.listener.ReconnectOnClick;

public class AdvancedActivity extends SilentActivity {

	class StatusUpdater extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			onStatusUpdate();
		}

	}

	private final BroadcastReceiver statusUpdater = new StatusUpdater();

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_advanced );

		setText( R.id.android_version_value, Build.VERSION.RELEASE );
		setText( R.id.application_version_value, getSilentTextApplication().getVersion() );
		setText( R.id.device_name_value, getSilentTextApplication().getLocalResourceName() );
		setHint( R.id.device_name_value, JabberClient.getDefaultResourceName() );

		sos( R.id.application_version, getDebugSettingsIntent() );

		findViewById( R.id.reconnect ).setOnClickListener( new ReconnectOnClick() );

	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver( statusUpdater );
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( !isUnlocked() ) {
			requestUnlock();
			return;
		}
		onStatusUpdate();
		registerReceiver( statusUpdater, Action.XMPP_STATE_CHANGED, Manifest.permission.READ );
		registerReceiver( statusUpdater, Action.CONNECT, Manifest.permission.READ );
		registerReceiver( statusUpdater, Action.DISCONNECT, Manifest.permission.READ );
	}

	protected void onStatusUpdate() {

		setVisibleIf( !isOnline(), R.id.reconnect );

		setText( R.id.xmpp_status, getOnlineStatus() );

		JabberClient jabber = getJabber();

		if( jabber == null ) {
			setVisibleIf( false, R.id.xmpp_host, R.id.xmpp_port );
			return;
		}

		setText( R.id.device_name_value, jabber.getResourceName() );

		setVisibleIf( true, R.id.xmpp_host, R.id.xmpp_port );
		setText( R.id.xmpp_host_value, jabber.getServerHost() );

		int port = jabber.getServerPort();
		setText( R.id.xmpp_port_value, port > 0 ? Integer.toString( port ) : getString( R.string.none ) );

	}

	@Override
	protected void onStop() {
		super.onStop();
		getSilentTextApplication().setLocalResourceName( getTextFromView( R.id.device_name_value ) );
	}

}
