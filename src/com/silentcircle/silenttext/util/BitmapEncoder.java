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

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import android.graphics.Bitmap;

public class BitmapEncoder {

	static class Color {

		public static final int MASK_ALPHA = 0xFF000000;
		public static final int MASK_RED = 0x00FF0000;
		public static final int MASK_GREEN = 0x0000FF00;
		public static final int MASK_BLUE = 0x000000FF;

		public static int alpha( byte [] buffer, int offset ) {
			return MASK_ALPHA & buffer[offset] << 24;
		}

		public static int blue( byte [] buffer, int offset ) {
			return MASK_BLUE & buffer[offset + 3];
		}

		public static int from( byte [] buffer, int offset ) {
			int a = alpha( buffer, offset );
			int r = red( buffer, offset );
			int g = green( buffer, offset );
			int b = blue( buffer, offset );
			return a | r | g | b;
		}

		public static int green( byte [] buffer, int offset ) {
			return MASK_GREEN & buffer[offset + 2] << 8;
		}

		public static int red( byte [] buffer, int offset ) {
			return MASK_RED & buffer[offset + 1] << 16;
		}

		public static void to( int color, byte [] buffer, int offset ) {
			int i = offset;
			buffer[i++] = (byte) ( ( MASK_ALPHA & color ) >> 24 & 0xFF );
			buffer[i++] = (byte) ( ( MASK_RED & color ) >> 16 & 0xFF );
			buffer[i++] = (byte) ( ( MASK_GREEN & color ) >> 8 & 0xFF );
			buffer[i++] = (byte) ( MASK_BLUE & color );
		}

	}

	private static class Point {

		public int x;
		public int y;

		public Point() {
			// Nothing.
		}

	}

	public static byte [] decodeFromBitmap( Bitmap bitmap ) {
		if( bitmap == null ) {
			return null;
		}
		byte [] buffer = new byte [bitmap.getWidth() * bitmap.getHeight() * 4];
		bitmap.copyPixelsToBuffer( ByteBuffer.wrap( buffer ) );
		return buffer;
	}

	public static byte [] decodeFromBitmap( Bitmap bitmap, byte [] output ) {
		if( bitmap == null || output == null ) {
			return output;
		}
		if( output.length % 4 != 0 ) {
			throw new IllegalArgumentException( "array length must be divisible by 4" );
		}
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int sourcePixelCount = width * height;
		int targetPixelCount = output.length / 4;
		int extraPixelCount = sourcePixelCount - targetPixelCount;
		if( extraPixelCount < 0 ) {
			throw new IllegalArgumentException( "Not enough pixels in target bitmap to encode this array" );
		}
		for( int i = 0; i < targetPixelCount; i++ ) {
			int pixel = interpolate( i, targetPixelCount, sourcePixelCount );
			Color.to( bitmap.getPixel( pixel % width, pixel / width ), output, i * 4 );
		}
		return output;
	}

	public static Bitmap encodeToBitmap( byte [] input ) {
		if( input == null ) {
			return null;
		}
		boolean perfectFit = input.length % 4 == 0;
		if( !perfectFit ) {
			throw new IllegalArgumentException( "array length must be divisible by 4" );
		}
		Point dimensions = getDimensions( input.length / 4 );
		Bitmap bitmap = Bitmap.createBitmap( dimensions.x, dimensions.y, Bitmap.Config.ARGB_8888 );
		bitmap.copyPixelsFromBuffer( ByteBuffer.wrap( input ) );
		return bitmap;
	}

	public static Bitmap encodeToBitmap( byte [] input, Bitmap bitmap ) {
		if( bitmap == null || input == null ) {
			return bitmap;
		}
		if( input.length % 4 != 0 ) {
			throw new IllegalArgumentException( "array length must be divisible by 4" );
		}
		int targetPixelCount = bitmap.getWidth() * bitmap.getHeight();
		int sourcePixelCount = input.length / 4;
		int extraPixelCount = targetPixelCount - sourcePixelCount;
		if( extraPixelCount < 0 ) {
			throw new IllegalArgumentException( "Not enough pixels in target bitmap to encode this array" );
		}
		for( int i = 0; i < sourcePixelCount; i++ ) {
			int pixel = interpolate( i, sourcePixelCount, targetPixelCount );
			int x = pixel % bitmap.getWidth();
			int y = pixel / bitmap.getWidth();
			int color = Color.from( input, i * 4 );
			int before = bitmap.getPixel( x, y );
			bitmap.setPixel( x, y, color );
			int after = bitmap.getPixel( x, y );
			if( color != after ) {
				Integer da = Integer.valueOf( ( ( Color.MASK_ALPHA & color ) >> 24 ) - ( ( Color.MASK_ALPHA & after ) >> 24 ) );
				Integer dr = Integer.valueOf( ( ( Color.MASK_RED & color ) >> 16 ) - ( ( Color.MASK_RED & after ) >> 16 ) );
				Integer dg = Integer.valueOf( ( ( Color.MASK_GREEN & color ) >> 8 ) - ( ( Color.MASK_GREEN & after ) >> 8 ) );
				Integer db = Integer.valueOf( ( Color.MASK_BLUE & color ) - ( Color.MASK_BLUE & after ) );
				throw new IllegalStateException( String.format( "Pixel data at input offset %d (%d,%d) not set as expected (was:%x, expected:%x, actual:%x) %d,%d,%d,%d", Integer.valueOf( i ), Integer.valueOf( x ), Integer.valueOf( y ), Integer.valueOf( before ), Integer.valueOf( color ), Integer.valueOf( after ), da, dr, dg, db ) );
			}
		}
		return bitmap;
	}

	public static Bitmap encodeToBitmap( byte [] input, int width, int height ) {
		SecureRandom random = new SecureRandom();
		Bitmap bitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				bitmap.setPixel( x, y, random.nextInt() );
			}
		}
		return encodeToBitmap( input, bitmap );
	}

	private static Point getDimensions( int pixels ) {
		Point best = new Point();
		Point current = new Point();
		current.x = pixels;
		current.y = 1;
		best.x = current.x;
		best.y = current.y;
		for( current.y = 1; current.y < pixels / current.y; current.y++ ) {
			current.x = pixels / current.y;
			if( pixels % current.y == 0 ) {
				best.x = current.x;
				best.y = current.y;
			}
		}
		return best;
	}

	private static int interpolate( int offset, int sourceLength, int targetLength ) {
		double s = (double) offset / sourceLength;
		double t = s * targetLength;
		return (int) Math.floor( t );
	}

}
