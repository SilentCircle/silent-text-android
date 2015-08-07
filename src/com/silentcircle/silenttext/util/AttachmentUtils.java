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
package com.silentcircle.silenttext.util;

import java.io.File;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.MediaColumns;
import android.util.DisplayMetrics;
import android.webkit.MimeTypeMap;

import com.silentcircle.silentstorage.repository.Repository;
import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.SilentActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.listener.CancelUploadOnClick;
import com.silentcircle.silenttext.listener.DismissDialogOnClick;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Attachment;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.provider.PictureProvider;
import com.silentcircle.silenttext.provider.VideoProvider;

public class AttachmentUtils {

	public static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024;
	public static final long FILE_SIZE_WARNING_THRESHOLD = FILE_SIZE_LIMIT / 2;

	private static final Log LOG = new Log( "AttachmentUtils" );

	private static final String FILE_NAME_PATTERN = "^(.+)\\.([^\\.]+)$";

	private static String mPicturePath;

	private static String mVideoPath;

	/**
	 * Attempts to notify the system's download manager, if available, that a file has been
	 * downloaded.
	 * 
	 * @see {@link DownloadManager#addCompletedDownload(String, String, boolean, String, String, long, boolean)}
	 */
	@TargetApi( Build.VERSION_CODES.HONEYCOMB_MR1 )
	public static long addToDownloadManager( Context context, String title, String description, boolean isMediaScannerScannable, String mimeType, String path, long length, boolean showNotification ) {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 ) {
			DownloadManager downloadManager = (DownloadManager) context.getSystemService( Context.DOWNLOAD_SERVICE );
			if( downloadManager != null ) {
				return downloadManager.addCompletedDownload( title, description, isMediaScannerScannable, mimeType, path, length, showNotification );
			}
		}
		return 0L;
	}

	public static void deleteFile( Uri uri ) {
		File f = new File( uri.getPath() );
		if( f.exists() ) {
			f.delete();
		}

	}

	public static Attachment getAttachment( Context context, Siren siren ) {
		try {
			SilentTextApplication application = SilentTextApplication.from( context );
			Repository<Attachment> attachments = application.getAttachments();
			return attachments.findByID( siren.getCloudLocator().toCharArray() );
		} catch( RuntimeException exception ) {
			LOG.warn( exception, "#getAttachment(Context,Siren)" );
			return null;
		}
	}

	public static int getAttachmentLabelIcon( String contentType ) {
		if( MIME.isVideo( contentType ) || UTI.isVideo( contentType ) ) {
			return R.drawable.ic_action_video;
		}
		if( MIME.isAudio( contentType ) || UTI.isAudio( contentType ) ) {
			return R.drawable.ic_action_volume;
		}
		if( MIME.isImage( contentType ) || UTI.isImage( contentType ) ) {
			return R.drawable.ic_action_picture;
		}
		return R.drawable.ic_action_attachment_2;
	}

	public static String getContentType( Siren siren ) {

		if( siren == null ) {
			return null;
		}

		String contentType = siren.getMIMEType();

		if( contentType == null ) {
			contentType = siren.getMediaType();
		}

		return contentType;

	}

	public static String getContentType( Siren siren, Attachment attachment ) {

		String contentType = getContentType( siren );

		if( attachment != null ) {
			byte [] type = attachment.getType();
			if( type != null ) {
				contentType = new String( type );
			}
		}

		return contentType;

	}

	public static String getExtensionFromFileName( String fileName ) {
		int slash = fileName.lastIndexOf( File.separatorChar );
		int dot = fileName.lastIndexOf( '.' );
		if( slash >= dot ) {
			return null;
		}
		String extension = fileName.substring( dot + 1 ).toLowerCase( Locale.ENGLISH );
		return extension;
	}

	@TargetApi( Build.VERSION_CODES.FROYO )
	public static File getExternalStorageFile( Context context, File file ) {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO ) {
			return null;
		}
		if( !Environment.MEDIA_MOUNTED.equals( Environment.getExternalStorageState() ) ) {
			return null;
		}
		if( file == null ) {
			return null;
		}

		// Force requesting file to reside in cache
		if( context instanceof SilentActivity ) {
			if( !StringUtils.equals( file.getParent(), ( (SilentActivity) context ).getCacheStagingDir().getPath() ) ) {
				return null;
			}

		} else {
			if( !StringUtils.equals( file.getParent(), String.format( "%s/%s", context.getCacheDir(), ".temp" ) ) ) {
				return null;
			}
		}

		File externalCache = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS );
		externalCache.mkdirs();
		String externalFileName = file.getName();
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ) {
			externalFileName = sanitizeFileName( externalFileName );
		}
		File externalFile = new File( externalCache, externalFileName );
		return externalFile;
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

	public static String getLabelForDuration( int ms ) {
		if( ms <= 0 ) {
			return null;
		}
		return String.format( "%02d:%02d", Integer.valueOf( ms / 60000 ), Integer.valueOf( ms / 1000 % 60 ) );
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
		// we temp saved picture into internal storage, uri is for contentProvider and it's
		// different from path to the file. and this mPacturePath will be used by SCloudEncryptor
		// too. Same as video.
		if( uri.toString().contains( PictureProvider.CONTENT_URL_PREFIX ) ) {
			mPicturePath = uri.getPath();
			return mPicturePath;
		} else if( uri.equals( VideoProvider.CONTENT_URL_PREFIX ) ) {
			mVideoPath = uri.getPath();
			return mVideoPath;
		}

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

	public static Bitmap getPreviewImage( Context context, Siren siren ) {
		return getPreviewImage( siren, getAttachment( context, siren ), getResourceDisplayDensityDPI( context ) );
	}

	public static Bitmap getPreviewImage( Context context, Siren siren, Attachment attachment ) {
		return getPreviewImage( siren, attachment, getResourceDisplayDensityDPI( context ) );
	}

	public static Bitmap getPreviewImage( Siren siren, Attachment attachment, int targetDensity ) {
		return siren == null ? null : getPreviewImage( getContentType( siren, attachment ), siren.getThumbnail(), targetDensity );
	}

	public static Bitmap getPreviewImage( Siren siren, int targetDensity ) {
		return siren == null ? null : getPreviewImage( getContentType( siren ), siren.getThumbnail(), targetDensity );
	}

	public static Bitmap getPreviewImage( String contentType, String base64thumbnail, int targetDensity ) {

		Bitmap bitmap = null;

		if( base64thumbnail != null && MIME.isVisual( contentType ) || UTI.isVisual( contentType ) ) {
			byte [] thumbnail = Base64.decodeBase64( base64thumbnail );
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inDensity = DisplayMetrics.DENSITY_DEFAULT;
			options.inScaled = true;
			options.inTargetDensity = targetDensity;
			bitmap = BitmapFactory.decodeByteArray( thumbnail, 0, thumbnail.length, options );
		}

		return bitmap;

	}

	private static int getResourceDisplayDensityDPI( Context context ) {
		try {
			Resources resources = context.getResources();
			DisplayMetrics displayMetrics = resources.getDisplayMetrics();
			return displayMetrics.densityDpi;
		} catch( NullPointerException expected ) {
			return DisplayMetrics.DENSITY_DEFAULT;
		}
	}

	public static boolean isExported( Context context, File file ) {
		File externalFile = getExternalStorageFile( context, file );
		return externalFile != null && externalFile.exists();
	}

	/**
	 * Attempts to remove the download with the given ID from the system's download manager, if
	 * available.
	 * 
	 * @see {@link DownloadManager#remove(long...)}
	 */
	@TargetApi( Build.VERSION_CODES.HONEYCOMB_MR1 )
	public static void removeFromDownloadManager( Context context, long id ) {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 ) {
			DownloadManager downloadManager = (DownloadManager) context.getSystemService( Context.DOWNLOAD_SERVICE );
			if( downloadManager != null ) {
				downloadManager.remove( id );
			}
		}
	}

	public static boolean resolves( PackageManager packageManager, String action, Uri uri, String mimeType ) {
		Intent intent = new Intent( action );
		intent.setDataAndType( uri, mimeType );
		return intent.resolveActivity( packageManager ) != null;
	}

	private static String sanitizeFileName( String fileName ) {
		StringBuilder sanitized = new StringBuilder();
		sanitized.append( fileName.replaceAll( FILE_NAME_PATTERN, "$1" ).replaceAll( "[^A-Za-z0-9_\\-]", "_" ) );
		sanitized.append( fileName.replaceAll( FILE_NAME_PATTERN, "$2" ) );
		return sanitized.toString();
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
