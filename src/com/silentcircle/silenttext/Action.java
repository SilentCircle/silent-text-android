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
package com.silentcircle.silenttext;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public enum Action {

	UPDATE_CONVERSATION( "UPDATE_CONVERSATION" ),
	BEGIN_DEACTIVATE( "BEGIN_DEACTIVATE" ),
	FINISH_DEACTIVATE( "FINISH_DEACTIVATE" ),
	CONVERSATION_EVENT( "CONVERSATION_EVENT" ),
	TRANSITION( "TRANSITION" ),
	VERIFY( "VERIFY" ),
	SAVE_STATE( "SAVE_STATE" ),
	ERROR( "ERROR" ),
	WARNING( "WARNING" ),
	SEND_MESSAGE( "SEND_MESSAGE" ),
	RECEIVE_MESSAGE( "RECEIVE_MESSAGE" ),
	CONNECT( "CONNECT" ),
	DISCONNECT( "DISCONNECT" ),
	XMPP_STATE_CHANGED( "XMPP_STATE_CHANGED" ),
	SYSTEM_NET_CHANGE( "SYSTEM_NET_CHANGE" ),
	CANCEL( "CANCEL" ),
	PROGRESS( "PROGRESS" ),
	UPLOAD( "UPLOAD" ),
	ENCRYPT( "ENCRYPT" ),
	LOCK( "LOCK" ),
	NOTIFY( "NOTIFY" ),
	REFRESH_SELF( "REFRESH_SELF" );

	public static Action from( Intent intent ) {
		return from( intent.getAction() );
	}

	public static Action from( String name ) {
		for( Action action : Action.values() ) {
			if( name.equals( action.getName() ) ) {
				return action;
			}
		}
		return null;
	}

	private final String name;

	private Action( String name ) {
		this.name = String.format( "com.silentcircle.silenttext.action.%s", name );
	}

	public void broadcast( Context context ) {
		context.sendBroadcast( intent() );
	}

	public void broadcast( Context context, String permission ) {
		context.sendBroadcast( intent(), permission );
	}

	public IntentFilter filter() {
		return new IntentFilter( getName() );
	}

	public String getName() {
		return name;
	}

	public Intent intent() {
		return new Intent( getName() );
	}

}
