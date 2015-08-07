/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.service;

import android.app.IntentService;
import android.content.Intent;

import com.silentcircle.api.model.User;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.task.GetUserFromServerTask;
import com.silentcircle.silenttext.util.AsyncUtils;

/**
 * This class exists as a short-lived service solely used for refreshing the user's own information.
 */
public class RefreshSelfIntentService extends IntentService {

	public static boolean isRunning = false;
	public static boolean hasCompleted = false;
	public static boolean wasSuccessful = false;

	public RefreshSelfIntentService() {
		super( "RefreshSelfIntentService" );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		isRunning = false;
	}

	@Override
	protected void onHandleIntent( Intent intent ) {
		if( wasSuccessful ) {
			return;
		}

		if( !isRunning ) {
			isRunning = true;
		}

		if( hasCompleted ) {
			hasCompleted = false;
		}

		final SilentTextApplication application = (SilentTextApplication) getApplication();

		User self = application.getUserFromCache( application.getUsername() );

		if( self == null || self.getKeys() == null || self.getKeys().isEmpty() ) {
			AsyncUtils.execute( new GetUserFromServerTask( application, true ) {

				@Override
				protected void onPostExecute( User user ) {
					if( user != null && user.getKeys() != null && !user.getKeys().isEmpty() ) {
						application.getUsers().save( user );
						wasSuccessful = true;
						hasCompleted = true;
					} else {
						hasCompleted = true;
					}

					onDestroy();
				}
			}, application.getUsername() );
		}
	}
}
