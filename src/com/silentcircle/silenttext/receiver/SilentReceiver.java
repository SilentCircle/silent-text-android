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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.silentcircle.silenttext.Extra;

abstract class SilentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive( Context context, Intent intent ) {

		String id = Extra.ID.from( intent );
		String partner = Extra.PARTNER.from( intent );
		String text = Extra.TEXT.from( intent );

		if( partner == null || text == null ) {
			return;
		}

		onReceive( context, partner, id, text );

	}

	/**
	 * Receives a broadcast with convenient access to all potentially available extras.
	 * 
	 * @param context
	 *            The context of this broadcast.
	 * @param partner
	 *            The username of the conversation partner who this broadcast involves.
	 * @param id
	 *            The XMPP packet ID of the message this broadcast involves. If this is not a
	 *            message broadcast, this will be <code>null</code>.
	 * @param text
	 *            Additional text related to this broadcast, as defined by the receiver
	 *            implementation.
	 */
	protected abstract void onReceive( Context context, String partner, String id, String text );

}
