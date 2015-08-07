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
package com.silentcircle.silenttext.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SilentPhone {

	public static final String ACTION_SILENT_CALL = "com.silentcircle.silentphone.action.NEW_OUTGOING_CALL";
	public static final String PROTOCOL_SILENT_PHONE = "silenttel";

	public static void call( Context context, String remoteUserID ) {
		Intent intent = getCallIntent( remoteUserID );
		if( context.getPackageManager().resolveActivity( intent, 0 ) != null ) {
			context.startActivity( intent );
		}
	}

	public static Intent getCallIntent( String remoteUserID ) {
		Intent intent = new Intent( ACTION_SILENT_CALL, Uri.fromParts( PROTOCOL_SILENT_PHONE, remoteUserID, null ) );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		return intent;
	}

	public static boolean supports( Context context ) {
		Intent intent = getCallIntent( "alice@silentcircle.com" );
		return context.getPackageManager().resolveActivity( intent, 0 ) != null;
	}

}
