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
package com.silentcircle.silenttext.graphics;

import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.silentcircle.core.util.StringUtils;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.repository.remote.RemoteContactRepository;

public class AvatarUtils {

	private static final Rect junkRectangle = new Rect();

	private static Bitmap bitmapForSize( int size ) {
		try {
			return Bitmap.createBitmap( size, size, Bitmap.Config.ARGB_8888 );
		} catch( OutOfMemoryError e ) {
			return null;
		}
	}

	private static int colorFor( String username ) {
		return 0xFFFFFFFF & username.hashCode();
	}

	private static Bitmap decorate( Bitmap bitmap, char label, int color, boolean glossy ) {

		Canvas canvas = new Canvas( bitmap );

		canvas.drawColor( color );

		String labelString = String.valueOf( label );
		Paint textPaint = getTextPaint( bitmap.getHeight() * 0.75f );
		float cx = bitmap.getWidth() / 2;
		float cy = bitmap.getHeight() / 2;
		float oy = 0;

		synchronized( junkRectangle ) {
			textPaint.getTextBounds( labelString, 0, 1, junkRectangle );
			oy = ( junkRectangle.top + junkRectangle.bottom ) / 2;
		}

		canvas.drawText( labelString, cx, cy - oy, textPaint );

		if( glossy ) {
			canvas.drawPath( gloss( bitmap.getWidth() ), getGlossPaint() );
		}

		return bitmap;

	}

	private static Bitmap drawToBitmap( Resources resources, int drawableResourceID, Bitmap bitmap ) {
		Drawable drawable = resources.getDrawable( drawableResourceID );
		drawable.setBounds( 0, 0, bitmap.getWidth(), bitmap.getHeight() );
		drawable.draw( new Canvas( bitmap ) );
		return bitmap;
	}

	private static Bitmap drawToBitmap( Resources resources, int drawableResourceID, int size ) {
		return drawToBitmap( resources, drawableResourceID, size, size );
	}

	private static Bitmap drawToBitmap( Resources resources, int drawableResourceID, int width, int height ) {
		return drawToBitmap( resources, drawableResourceID, Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 ) );
	}

	public static Bitmap generateAvatar( Context context, String username, int avatarDimenResourceID ) {
		Resources resources = context.getResources();
		return generateAvatar( username, resources.getDimensionPixelSize( avatarDimenResourceID ) );
	}

	public static Bitmap generateAvatar( String username, int size ) {
		return generateAvatar( username, size, false );
	}

	public static Bitmap generateAvatar( String username, int size, boolean glossy ) {
		Bitmap bitmap = bitmapForSize( size );
		if( bitmap == null ) {
			return null; // out of memory?
		}
		return decorate( bitmap, labelFor( username ), colorFor( username ), glossy );
	}

	public static Bitmap getAvatar( Context context, ContactRepository repository, String username ) {
		if( repository == null ) {
			return null;
		}

		Bitmap bitmap = null;

		InputStream in = repository.getAvatar( username );

		if( in == null ) {
			Bitmap cachedBitmap = RemoteContactRepository.getCachedAvatar( username );

			if( cachedBitmap != null ) {
				return cachedBitmap;
			}

			in = RemoteContactRepository.getAvatar( context, username );
		}

		bitmap = in != null ? BitmapFactory.decodeStream( in ) : null;

		if( bitmap != null ) {
			RemoteContactRepository.cacheAvatar( username, bitmap );
		}

		return bitmap;
	}

	public static Bitmap getAvatar( Context context, ContactRepository repository, String username, int avatarDimensionResourceID ) {
		Bitmap bitmap = getAvatar( context, repository, username );

		if( bitmap == null ) {
			if( SilentTextApplication.from( context ).getUser( username ) != null && SilentTextApplication.from( context ).getUser( username ).getDisplayName() != null && StringUtils.equals( SilentTextApplication.from( context ).getUser( username ).getDisplayName().toString(), "scvoicemail" ) ) {
				Resources resources = context.getResources();
				int size = resources.getDimensionPixelSize( avatarDimensionResourceID );
				bitmap = drawToBitmap( resources, R.drawable.ic_voicemail_icon, size );
			} else {
				if( ServiceConfiguration.getInstance().features.generateDefaultAvatars ) {
					bitmap = generateAvatar( context, username, avatarDimensionResourceID );
				} else {
					Resources resources = context.getResources();
					int size = resources.getDimensionPixelSize( avatarDimensionResourceID );
					bitmap = drawToBitmap( resources, R.drawable.ic_avatar_placeholder, size );
				}
			}
		}

		return bitmap;
	}

	private static Paint getGlossPaint() {
		Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );
		paint.setColor( 0x22FFFFFF );
		return paint;
	}

	private static Paint getTextPaint( float size ) {
		Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );
		paint.setShadowLayer( 1, 1, 1, 0xFF000000 );
		paint.setColor( 0xFFFFFFFF );
		paint.setTextAlign( Align.CENTER );
		paint.setTextSize( size );
		paint.setTypeface( Typeface.DEFAULT );
		return paint;

	}

	private static Path gloss( int size ) {
		Path gloss = new Path();
		gloss.moveTo( 0, size );
		gloss.cubicTo( 0, size, size * 3 / 4, size * 3 / 4, size, 0 );
		gloss.lineTo( 0, 0 );
		gloss.close();
		return gloss;
	}

	private static char labelFor( String username ) {
		return Character.toUpperCase( username.charAt( 0 ) );
	}

}
