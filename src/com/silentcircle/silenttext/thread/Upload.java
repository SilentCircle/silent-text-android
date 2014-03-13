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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.client.HTTPContent;
import com.silentcircle.silenttext.client.SimpleHTTPClient;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;

public class Upload implements Runnable {

	private static final String HEADER_AMAZON_ACL_VALUE_PUBLIC_READ = "public-read";
	private static final String HEADER_AMAZON_ACL = "x-amz-acl";
	private static final String HEADER_CONTENT_TYPE = "Content-Type";
	private static final String SCLOUD_CONTENT_TYPE = "application/x-scloud";

	private final SimpleHTTPClient http = new SimpleHTTPClient();
	private final SCloudObjectRepository repository;

	public Upload( SCloudObjectRepository repository ) {
		this.repository = repository;
	}

	/**
	 * @param object
	 */
	protected void onObjectUploaded( SCloudObject object ) {
		// By default, do nothing.
	}

	/**
	 * Override this to do some stuff on progress update.
	 * 
	 * @param progress
	 * @param max
	 */
	protected void onProgressUpdate( int progress, int max ) {
		// By default, do nothing.
	}

	protected void onUploadCancelled() {
		// By default, do nothing.
	}

	protected void onUploadComplete() {
		// By default, do nothing.
	}

	/**
	 * @param exception
	 */
	protected void onUploadError( Throwable exception ) {
		// By default, do nothing.
	}

	@Override
	public void run() {

		List<SCloudObject> objects = repository.list();
		List<SCloudObject> pending = new ArrayList<SCloudObject>();

		for( int i = 0; i < objects.size(); i++ ) {

			SCloudObject object = objects.get( i );

			if( object.getURL() == null ) {
				continue;
			}

			if( object.isUploaded() ) {
				continue;
			}

			pending.add( object );

		}

		onProgressUpdate( 0, pending.size() );

		for( int i = 0; i < pending.size(); i++ ) {

			if( !repository.exists() ) {
				onUploadCancelled();
				return;
			}

			SCloudObject object = pending.get( i );

			Map<String, String> headers = new HashMap<String, String>();

			headers.put( HEADER_AMAZON_ACL, HEADER_AMAZON_ACL_VALUE_PUBLIC_READ );
			headers.put( HEADER_CONTENT_TYPE, SCLOUD_CONTENT_TYPE );

			repository.read( object );

			try {

				http.put( object.getURL(), new HTTPContent( new ByteArrayInputStream( object.getData() ), SCLOUD_CONTENT_TYPE, object.getSize() ), headers );
				object.setDownloaded( true );
				object.setUploaded( true );

				if( !repository.exists() ) {
					object.setData( null );
					onUploadCancelled();
					return;
				}

				repository.save( object );

				object.setData( null );
				onObjectUploaded( object );
				onProgressUpdate( i + 1, pending.size() );

			} catch( RuntimeException exception ) {
				object.setData( null );
				onUploadError( exception );
				return;
			}

		}

		onUploadComplete();

	}

}
