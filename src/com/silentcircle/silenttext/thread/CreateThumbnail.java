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

import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.IOUtils;

public class CreateThumbnail implements Runnable {

	private static enum ContentType {

		UNKNOWN,
		IMAGE,
		VIDEO,
		AUDIO;

		public static ContentType forName( String name ) {
			if( name == null ) {
				return UNKNOWN;
			}
			if( name.startsWith( "image/" ) ) {
				return IMAGE;
			}
			if( name.startsWith( "video/" ) ) {
				return VIDEO;
			}
			if( name.startsWith( "audio/" ) ) {
				return AUDIO;
			}
			return UNKNOWN;
		}

	}

	private static final Uri ALBUM_ART_URI = Uri.parse( "content://media/external/audio/albumart" );

	private static Bitmap createEmptyThumbnail( int targetSize ) {
		return createEmptyThumbnail( targetSize, targetSize );
	}

	private static Bitmap createEmptyThumbnail( int targetWidth, int targetHeight ) {
		Bitmap bitmap = Bitmap.createBitmap( targetWidth, targetHeight, Bitmap.Config.ARGB_8888 );
		new Canvas( bitmap ).drawColor( 0xFF000000 );
		return bitmap;
	}

	private static Bitmap decoratePlayableThumbnail( Bitmap bitmap ) {

		int centerX = bitmap.getWidth() / 2;
		int centerY = bitmap.getHeight() / 2;
		int innerBound = Math.min( bitmap.getWidth(), bitmap.getHeight() );
		int outerRadius = innerBound / 4;
		int innerRadius = innerBound / 8;

		Canvas canvas = new Canvas( bitmap );
		Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );
		paint.setStrokeWidth( 2 );

		Path triangle = new Path();
		triangle.moveTo( centerX - innerRadius, centerY - innerRadius );
		triangle.lineTo( centerX + innerRadius, centerY );
		triangle.lineTo( centerX - innerRadius, centerY + innerRadius );
		triangle.close();

		paint.setColor( 0x88000000 );
		paint.setStyle( Style.FILL );
		canvas.drawCircle( centerX, centerY, outerRadius, paint );

		paint.setColor( 0xCCFFFFFF );
		paint.setStyle( Style.STROKE );
		canvas.drawCircle( centerX, centerY, outerRadius, paint );

		paint.setStyle( Style.FILL );
		canvas.drawPath( triangle, paint );

		return bitmap;

	}

	private static Bitmap resize( Bitmap bitmap, int targetWidth, int targetHeight ) {
		return bitmap == null ? bitmap : Bitmap.createScaledBitmap( bitmap, targetWidth, targetHeight, false );
	}

	protected final PackageManager packageManager;
	protected final ContentResolver resolver;
	protected final Uri uri;
	protected final String contentType;
	private final int maximumWidth;
	private final int maximumHeight;

	private static final Log LOG = new Log( CreateThumbnail.class.getSimpleName() );

	public CreateThumbnail( Context context, Intent intent, int maximumWidth, int maximumHeight ) {
		this( context, intent.getData(), intent.getType(), maximumWidth, maximumHeight );
	}

	public CreateThumbnail( Context context, Uri uri, String contentType, int maximumWidth, int maximumHeight ) {
		this( context.getPackageManager(), context.getContentResolver(), uri, contentType, maximumWidth, maximumHeight );
	}

	public CreateThumbnail( PackageManager packageManager, ContentResolver resolver, Uri uri, String contentType, int maximumWidth, int maximumHeight ) {
		this.packageManager = packageManager;
		this.resolver = resolver;
		this.uri = uri;
		this.contentType = contentType;
		this.maximumWidth = maximumWidth;
		this.maximumHeight = maximumHeight;
	}

	private Bitmap decorateAudioThumbnail( Bitmap bitmap ) {
		return decoratePlayableThumbnail( bitmap == null ? createEmptyThumbnail( Math.min( maximumWidth, maximumHeight ) ) : bitmap );
	}

	protected Bitmap decorateUnknownThumbnail( Bitmap bitmap ) {
		Intent intent = new Intent( Intent.ACTION_VIEW );
		intent.setDataAndType( uri, contentType );
		ResolveInfo activity = packageManager.resolveActivity( intent, 0 );
		if( activity != null ) {
			Drawable icon = activity.loadIcon( packageManager );
			icon.setBounds( 0, 0, bitmap.getWidth(), bitmap.getHeight() );
			icon.draw( new Canvas( bitmap ) );
		}
		return bitmap;
	}

	private Bitmap decorateVideoThumbnail( Bitmap bitmap ) {
		return decoratePlayableThumbnail( bitmap == null ? createEmptyThumbnail( maximumWidth, maximumWidth * 9 / 16 ) : bitmap );
	}

	private Bitmap getAudioThumbnail() {

		Bitmap bitmap = null;

		Cursor cursor = resolver.query( uri, new String [] {
			AudioColumns.ALBUM_ID
		}, null, null, null );

		if( cursor == null ) {
			return decorateAudioThumbnail( resize( bitmap ) );
		}

		if( cursor.moveToNext() ) {
			try {
				long albumID = cursor.getLong( cursor.getColumnIndexOrThrow( AudioColumns.ALBUM_ID ) );
				Uri albumArtURI = ContentUris.withAppendedId( ALBUM_ART_URI, albumID );
				bitmap = MediaStore.Images.Media.getBitmap( resolver, albumArtURI );
			} catch( IOException exception ) {
				bitmap = getUnknownThumbnail();
			} catch( IllegalArgumentException exception ) {
				bitmap = getUnknownThumbnail();
			}
		}
		cursor.close();

		return decorateAudioThumbnail( resize( bitmap ) );

	}

	private ContentType getContentType() {
		return ContentType.forName( contentType );
	}

	private Bitmap getImageThumbnail() {

		InputStream input = null;
		BitmapFactory.Options options = new BitmapFactory.Options();

		try {
			input = resolver.openInputStream( uri );
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream( input, null, options );
		} catch( IOException exception ) {
			LOG.warn( exception, "#getImageThumbnail" );
		} catch( SecurityException exception ) {
			LOG.warn( exception, "#getImageThumbnail" );
		} finally {
			IOUtils.close( input );
		}

		try {
			input = resolver.openInputStream( uri );
			setSampleSize( options );
			options.inJustDecodeBounds = false;
			return BitmapFactory.decodeStream( input, null, options );
		} catch( IOException exception ) {
			LOG.warn( exception, "#getImageThumbnail" );
		} catch( SecurityException exception ) {
			LOG.warn( exception, "#getImageThumbnail" );
		} finally {
			IOUtils.close( input );
		}

		return decorateUnknownThumbnail( createEmptyThumbnail( Math.min( maximumWidth, maximumHeight ) ) );

	}

	private Bitmap getThumbnail() {
		return getThumbnail( getContentType() );
	}

	private Bitmap getThumbnail( ContentType type ) {
		switch( type ) {
			case IMAGE:
				return getImageThumbnail();
			case VIDEO:
				return getVideoThumbnail();
			case AUDIO:
				return getAudioThumbnail();
			default:
				return getUnknownThumbnail();
		}
	}

	private Bitmap getUnknownThumbnail() {
		return decorateUnknownThumbnail( createEmptyThumbnail( Math.min( maximumWidth, maximumHeight ) ) );
	}

	private Bitmap getVideoThumbnail() {
		Bitmap bitmap = null;
		Cursor cursor = MediaStore.Video.query( resolver, uri, new String [] {
			BaseColumns._ID
		} );
		if( cursor == null ) {
			return decorateVideoThumbnail( resize( bitmap ) );
		}
		if( cursor.moveToNext() ) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			bitmap = MediaStore.Video.Thumbnails.getThumbnail( resolver, cursor.getInt( 0 ), MediaStore.Video.Thumbnails.MINI_KIND, options );
		} else {
			bitmap = getVideoThumbnailFroyo();
		}
		cursor.close();
		return decorateVideoThumbnail( resize( bitmap ) );
	}

	@TargetApi( Build.VERSION_CODES.FROYO )
	private Bitmap getVideoThumbnailFroyo() {
		Bitmap bitmap = null;
		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO ) {
			bitmap = ThumbnailUtils.createVideoThumbnail( uri.getPath(), MediaStore.Video.Thumbnails.MINI_KIND );
		}
		return bitmap;
	}

	/**
	 * @param bitmap
	 */
	protected void onThumbnailCreated( Bitmap bitmap ) {
		// By default, do nothing.
	}

	private Bitmap resize( Bitmap bitmap ) {
		if( bitmap == null ) {
			return bitmap;
		}
		int targetWidth = bitmap.getWidth();
		int targetHeight = bitmap.getHeight();
		double scale = 1;
		scale = Math.min( scale, (double) maximumWidth / targetWidth );
		scale = Math.min( scale, (double) maximumHeight / targetHeight );
		targetWidth = (int) Math.floor( targetWidth * scale );
		targetHeight = (int) Math.floor( targetHeight * scale );
		return resize( bitmap, targetWidth, targetHeight );
	}

	@Override
	public void run() {
		onThumbnailCreated( getThumbnail() );
	}

	private void setSampleSize( BitmapFactory.Options options ) {
		int sampleWidth = options.outWidth / maximumWidth;
		int sampleHeight = options.outHeight / maximumHeight;
		options.inSampleSize = Math.max( sampleWidth, sampleHeight );
		if( options.inSampleSize < 1 ) {
			options.inSampleSize = 1;
		}
	}

}
