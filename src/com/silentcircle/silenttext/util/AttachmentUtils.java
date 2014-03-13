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

import java.io.File;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.MediaColumns;
import android.webkit.MimeTypeMap;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.listener.CancelUploadOnClick;
import com.silentcircle.silenttext.listener.DismissDialogOnClick;

public class AttachmentUtils {

	public static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024;
	public static final long FILE_SIZE_WARNING_THRESHOLD = FILE_SIZE_LIMIT / 2;

	public static String getExtensionFromFileName( String fileName ) {
		int slash = fileName.lastIndexOf( File.separatorChar );
		int dot = fileName.lastIndexOf( '.' );
		if( slash >= dot ) {
			return null;
		}
		String extension = fileName.substring( dot + 1 ).toLowerCase( Locale.ENGLISH );
		return extension;
	}

	public static File getFileFromURI( Context context, Uri uri ) {
		String path = getPathFromURI( context, uri );
		return path == null ? null : new File( path );
	}

	public static String getFileNameFromURI( Context context, Uri uri ) {
		File file = getFileFromURI( context, uri );
		return file == null ? null : file.getName();
	}

	public static long getFileSize( Context context, Uri uri ) {
		try {
			ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor( uri, "r" );
			long size = fd.getStatSize();
			fd.close();
			return size;
		} catch( Throwable exception ) {
			return 0;
		}
	}

	public static String getMIMEType( Context context, Uri uri ) {
		String mimeType = context.getContentResolver().getType( uri );
		if( mimeType != null ) {
			return mimeType;
		}
		String fileName = getFileNameFromURI( context, uri );
		return getMIMETypeFromFileName( fileName );
	}

	public static String getMIMEType( File file ) {
		return getMIMETypeFromFileName( file.getName() );
	}

	public static String getMIMETypeFromFileName( String fileName ) {
		if( fileName == null ) {
			return null;
		}
		String extension = getExtensionFromFileName( fileName );
		if( extension == null ) {
			return null;
		}
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension( extension );
	}

	public static String getPathFromURI( Context context, Uri uri ) {
		String path = uri.getPath();
		Cursor cursor = context.getContentResolver().query( uri, new String [] {
			MediaColumns.DATA
		}, null, null, null );
		if( cursor == null ) {
			return path;
		}
		if( cursor.moveToFirst() ) {
			path = cursor.getString( cursor.getColumnIndex( MediaColumns.DATA ) );
		}
		cursor.close();
		return path;
	}

	public static void showFileSizeErrorDialog( Context context ) {
		showFileSizeErrorDialog( context, null );
	}

	public static void showFileSizeErrorDialog( Context context, DialogInterface.OnClickListener onDismissed ) {

		AlertDialog.Builder alert = new AlertDialog.Builder( context );

		alert.setTitle( R.string.error );
		alert.setMessage( R.string.error_large_file );
		alert.setNegativeButton( R.string.cancel, new DismissDialogOnClick( onDismissed ) );

		alert.show();

	}

	public static void showFileSizeWarningDialog( Context context, String remoteUserID, String messageID ) {
		showFileSizeWarningDialog( context, remoteUserID, messageID, null );
	}

	public static void showFileSizeWarningDialog( Context context, String remoteUserID, String messageID, DialogInterface.OnClickListener onDismissed ) {

		AlertDialog.Builder alert = new AlertDialog.Builder( context );

		alert.setTitle( R.string.warning );
		alert.setMessage( R.string.warning_large_file );

		alert.setNegativeButton( R.string.cancel, new CancelUploadOnClick( context, remoteUserID, messageID ) );
		alert.setPositiveButton( R.string.yes, new DismissDialogOnClick( onDismissed ) );

		alert.show();

	}

}
