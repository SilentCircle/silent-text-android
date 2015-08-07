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
package com.silentcircle.silenttext.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;

import com.silentcircle.http.client.URLBuilder;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.model.Attachment;
import com.silentcircle.silenttext.util.IOUtils;

public class AttachmentDownloadTask extends AsyncTask<Attachment, Attachment, Attachment []> {

	private static void download( File file, byte [] buffer ) throws IOException {

		InputStream in = null;
		OutputStream out = null;

		try {
			in = new URLBuilder( ServiceConfiguration.getInstance().scloud.url ).component( file.getName() ).open();
			out = new FileOutputStream( file, false );
			IOUtils.pipe( in, out, buffer );
		} catch( IOException exception ) {
			IOUtils.close( in, out );
			file.delete();
			throw exception;
		} finally {
			IOUtils.close( in, out );
		}

	}

	private final File cacheDir;

	public AttachmentDownloadTask( File cacheDir ) {
		this.cacheDir = cacheDir;
	}

	@Override
	protected Attachment [] doInBackground( Attachment... args ) {

		byte [] buffer = new byte [64 * 1024];
		int size = args.length;

		for( int i = 0; i < size; i++ ) {
			Attachment attachment = args[i];
			try {
				download( attachment, buffer );
				publishProgress( attachment );
			} catch( IOException exception ) {
				publishException( attachment, exception );
			}
		}

		return args;

	}

	private void download( Attachment attachment, byte [] buffer ) throws IOException {
		download( new File( cacheDir, new String( attachment.getLocator() ) ), buffer );
	}

	/**
	 * @param attachment
	 * @param exception
	 */
	protected void onException( Attachment attachment, Throwable exception ) {
		// By default, do nothing.
	}

	private void publishException( Attachment attachment, Throwable exception ) {
		onException( attachment, exception );
	}

}
