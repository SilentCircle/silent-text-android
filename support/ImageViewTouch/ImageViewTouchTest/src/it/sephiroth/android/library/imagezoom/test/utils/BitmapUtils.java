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
package it.sephiroth.android.library.imagezoom.test.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Various bitmap utilities
 * 
 * @author alessandro
 * 
 */
public class BitmapUtils {

	/**
	 * Resize a bitmap
	 * 
	 * @param input
	 * @param destWidth
	 * @param destHeight
	 * @return
	 * @throws OutOfMemoryError
	 */
	public static Bitmap resizeBitmap( final Bitmap input, int destWidth, int destHeight ) throws OutOfMemoryError {
		return resizeBitmap( input, destWidth, destHeight, 0 );
	}

	/**
	 * Resize a bitmap object to fit the passed width and height
	 * 
	 * @param input
	 *           The bitmap to be resized
	 * @param destWidth
	 *           Desired maximum width of the result bitmap
	 * @param destHeight
	 *           Desired maximum height of the result bitmap
	 * @return A new resized bitmap
	 * @throws OutOfMemoryError
	 *            if the operation exceeds the available vm memory
	 */
	public static Bitmap resizeBitmap( final Bitmap input, int destWidth, int destHeight, int rotation ) throws OutOfMemoryError {

		int dstWidth = destWidth;
		int dstHeight = destHeight;
		final int srcWidth = input.getWidth();
		final int srcHeight = input.getHeight();

		if ( rotation == 90 || rotation == 270 ) {
			dstWidth = destHeight;
			dstHeight = destWidth;
		}

		boolean needsResize = false;
		float p;
		if ( ( srcWidth > dstWidth ) || ( srcHeight > dstHeight ) ) {
			needsResize = true;
			if ( ( srcWidth > srcHeight ) && ( srcWidth > dstWidth ) ) {
				p = (float) dstWidth / (float) srcWidth;
				dstHeight = (int) ( srcHeight * p );
			} else {
				p = (float) dstHeight / (float) srcHeight;
				dstWidth = (int) ( srcWidth * p );
			}
		} else {
			dstWidth = srcWidth;
			dstHeight = srcHeight;
		}

		if ( needsResize || rotation != 0 ) {
			Bitmap output;

			if ( rotation == 0 ) {
				output = Bitmap.createScaledBitmap( input, dstWidth, dstHeight, true );
			} else {
				Matrix matrix = new Matrix();
				matrix.postScale( (float) dstWidth / srcWidth, (float) dstHeight / srcHeight );
				matrix.postRotate( rotation );
				output = Bitmap.createBitmap( input, 0, 0, srcWidth, srcHeight, matrix, true );
			}
			return output;
		} else
			return input;
	}

}
