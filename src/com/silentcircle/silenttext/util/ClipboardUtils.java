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
package com.silentcircle.silenttext.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.silentcircle.silenttext.R;

public class ClipboardUtils {

	public static void copy( Context context, String text ) {
		if( text == null ) {
			return;
		}
		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
			copyViaHoneycomb( context, text );
		} else {
			( (android.text.ClipboardManager) context.getSystemService( Context.CLIPBOARD_SERVICE ) ).setText( text );
		}
		Toast.makeText( context, context.getString( R.string.copied_to_clipboard ), Toast.LENGTH_SHORT ).show();
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	private static void copyViaHoneycomb( Context context, String text ) {
		( (android.content.ClipboardManager) context.getSystemService( Context.CLIPBOARD_SERVICE ) ).setPrimaryClip( android.content.ClipData.newPlainText( context.getResources().getString( R.string.silent_text ), text ) );
	}

}
