/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.task;

import java.lang.ref.SoftReference;

import android.content.Context;
import android.os.AsyncTask;

import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.repository.ResourceStateRepository;

public class GetSecureStatusTask extends AsyncTask<Conversation, Void, Void> {

	private final SoftReference<Context> contextReference;
	protected boolean supportsPKI = false;
	protected boolean secure = false;
	protected boolean verified = false;

	public GetSecureStatusTask( Context context ) {
		contextReference = new SoftReference<Context>( context );
	}

	@Override
	protected Void doInBackground( Conversation... args ) {

		if( args.length < 1 ) {
			return null;
		}

		Conversation conversation = args[0];
		Context context = contextReference.get();
		SilentTextApplication application = SilentTextApplication.from( context );
		if( conversation == null || context == null || application == null ) {
			return null;
		}
		String localUsername = application.getUsername();
		String remoteUsername = conversation.getPartner().getUsername();
		if( localUsername.equals( remoteUsername ) ) {
			supportsPKI = true;
			secure = true;
			verified = true;
		} else {
			supportsPKI = application.userSupportsPKI( remoteUsername );
			ResourceStateRepository states = application.getConversations().contextOf( conversation );
			ResourceState state = states.findById( conversation.getPartner().getDevice() );
			if( state == null || !state.isSecure() ) {
				secure = false;
				verified = false;
			} else {
				secure = true;
				verified = states.isVerified( state );
			}
		}

		return null;

	}

}
