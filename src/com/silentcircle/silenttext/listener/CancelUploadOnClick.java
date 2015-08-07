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
package com.silentcircle.silenttext.listener;

import java.lang.ref.SoftReference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;

public class CancelUploadOnClick extends DismissDialogOnClick {

	private final SoftReference<Context> contextReference;
	private final String partner;
	private final String messageID;

	public CancelUploadOnClick( Context context, String partner, String messageID ) {
		contextReference = new SoftReference<Context>( context );
		this.partner = partner;
		this.messageID = messageID;
	}

	private Context getContext() {
		return contextReference.get();
	}

	@Override
	public void onClick( DialogInterface dialog, int which ) {

		super.onClick( dialog, which );

		Context context = getContext();

		if( context == null ) {
			return;
		}

		Intent cancel = Action.CANCEL.intent();

		Extra.PARTNER.to( cancel, partner );
		Extra.ID.to( cancel, messageID );

		context.sendBroadcast( cancel, Manifest.permission.WRITE );

	}
}