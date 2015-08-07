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
package com.silentcircle.silenttext.fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silentcircle.silenttext.R;

public class FileViewerFragment extends BaseFragment {

	public static interface Callback {

		public void onError( Uri uri, String mimeType );

	}

	public static final String EXTRA_TYPE = "type";

	public static FileViewerFragment create( Uri uri, String mimeType ) {
		return instantiate( new FileViewerFragment(), uri, mimeType );
	}

	protected static <T extends FileViewerFragment> T instantiate( T fragment, Uri uri, String mimeType ) {
		Bundle arguments = new Bundle();
		arguments.putParcelable( Intent.EXTRA_STREAM, uri );
		arguments.putString( EXTRA_TYPE, mimeType );
		fragment.setArguments( arguments );
		return fragment;
	}

	protected void dispatchError() {

		Bundle arguments = getArguments();
		Callback callback = getCallback();

		if( arguments == null || callback == null ) {
			return;
		}

		Uri uri = arguments.getParcelable( Intent.EXTRA_STREAM );
		String mimeType = arguments.getString( EXTRA_TYPE );

		callback.onError( uri, mimeType );

	}

	protected Callback getCallback() {
		Activity activity = getActivity();
		return activity == null ? null : (Callback) activity;
	}

	protected File getFile() {
		Uri uri = getURI();
		return uri == null ? null : new File( uri.getPath() );
	}

	protected String getType() {
		Bundle arguments = getArguments();
		return arguments == null ? null : arguments.getString( EXTRA_TYPE );
	}

	protected Uri getURI() {
		Bundle arguments = getArguments();
		return arguments == null ? null : (Uri) arguments.getParcelable( Intent.EXTRA_STREAM );
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		return inflater.inflate( R.layout.file_viewer, container, false );
	}

	protected InputStream openFileForReading() {
		File file = getFile();
		try {
			return file == null ? null : new FileInputStream( file );
		} catch( FileNotFoundException exception ) {
			return null;
		}
	}

}
