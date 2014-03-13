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
package com.silentcircle.silenttext.thread;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.client.SCloudBroker;
import com.silentcircle.silenttext.listener.JSONObjectListener;

public class PrepareUpload implements Runnable {

	private static final long SECONDS = 1000;
	private static final long MINUTES = SECONDS * 60;
	private static final long HOURS = MINUTES * 60;
	private static final long DAYS = HOURS * 24;
	private static final DateFormat ISO8601 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH );

	protected static String locatorOf( SCloudObject object ) {
		return object.getLocator();
	}

	private final String expires;
	private final SCloudBroker client;
	protected final List<SCloudObject> objects;

	public PrepareUpload( List<SCloudObject> objects, SCloudBroker client ) {
		this( objects, client, System.currentTimeMillis() + 2 * DAYS );
	}

	public PrepareUpload( List<SCloudObject> objects, SCloudBroker client, long expires ) {
		this.objects = objects;
		this.client = client;
		this.expires = ISO8601.format( new Date( expires ) );
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
	protected void onPrepareUploadComplete( List<SCloudObject> objects ) {
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

		JSONObject files = new JSONObject();

		for( int i = 0; i < objects.size(); i++ ) {

			SCloudObject object = objects.get( i );

			if( object.getURL() != null ) {
				continue;
			}

			try {

				JSONObject file = new JSONObject();

				file.put( "shred_date", expires );
				file.put( "size", object.getSize() );

				files.put( locatorOf( object ), file );

			} catch( JSONException exception ) {
				onPrepareUploadError( exception );
				return;
			}

		}

		try {
			client.prepareSCloudUpload( files, new JSONObjectListener() {

				@Override
				public void onObjectReceived( JSONObject json ) {
					for( int i = 0; i < objects.size(); i++ ) {
						SCloudObject object = objects.get( i );
						try {
							JSONObject file = json.getJSONObject( locatorOf( object ) );
							String url = file.getString( "url" );
							object.setURL( url );
							onObjectPrepared( object );
						} catch( JSONException exception ) {
							throw new RuntimeException( exception );
						}
					}
				}

			} );
		} catch( RuntimeException exception ) {
			onPrepareUploadError( exception );
			return;
		}

		onPrepareUploadComplete( objects );

	}

}
