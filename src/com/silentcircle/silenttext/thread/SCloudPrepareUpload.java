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
package com.silentcircle.silenttext.thread;

import java.util.List;

import org.twuni.twoson.JSONValue;

import com.silentcircle.core.util.JSONUtils;
import com.silentcircle.http.client.exception.EmptyResponseException;
import com.silentcircle.http.client.listener.JSONValueListener;
import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.client.SCloudBroker;

public class SCloudPrepareUpload implements Runnable {

	private static final long SECONDS = 1000;
	private static final long MINUTES = SECONDS * 60;
	private static final long HOURS = MINUTES * 60;
	private static final long DAYS = HOURS * 24;

	protected static CharSequence locatorOf( SCloudObject object ) {
		return object.getLocator();
	}

	private final long expirationTimestamp;
	private final SCloudBroker client;
	protected final List<SCloudObject> objects;

	public SCloudPrepareUpload( List<SCloudObject> objects, SCloudBroker client ) {
		this( objects, client, System.currentTimeMillis() + 2 * DAYS );
	}

	public SCloudPrepareUpload( List<SCloudObject> objects, SCloudBroker client, long expirationTimestamp ) {
		this.objects = objects;
		this.client = client;
		this.expirationTimestamp = expirationTimestamp;
	}

	/**
	 * @param object
	 */
	protected void onObjectPrepared( SCloudObject object ) {
		// By default, do nothing.
	}

	/**
	 * @param objects
	 */
	protected void onPrepareUploadComplete() {
		// By default, do nothing.
	}

	/**
	 * @param exception
	 */
	protected void onPrepareUploadError( Throwable exception ) {
		// By default, do nothing.
	}

	@Override
	public void run() {

		try {
			client.prepareSCloudUpload( objects, expirationTimestamp, new JSONValueListener() {

				@Override
				public void onObjectReceived( JSONValue json ) {
					if( json == null ) {
						throw new EmptyResponseException();
					}
					for( int i = 0; i < objects.size(); i++ ) {
						SCloudObject object = objects.get( i );
						object.setURL( JSONUtils.getString( json.get( String.valueOf( locatorOf( object ) ) ), "url" ) );
						onObjectPrepared( object );
					}
				}

			} );
		} catch( Throwable exception ) {
			onPrepareUploadError( exception );
			return;
		}

		onPrepareUploadComplete();

	}

}
