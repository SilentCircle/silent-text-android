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
package it.sephiroth.android.library.imagezoom.test;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchDoubleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchSingleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnDrawableChangeListener;
import it.sephiroth.android.library.imagezoom.test.utils.DecodeUtils;
import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class ImageViewTestActivity extends Activity {
	
	private static final String LOG_TAG = "image-test";

	ImageViewTouch mImage;
	Button mButton1;
	Button mButton2;
	static int displayTypeCount = 0; 

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		setContentView( R.layout.main );
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mImage = (ImageViewTouch) findViewById( R.id.image );
		
		// set the default image display type
		mImage.setDisplayType( DisplayType.FIT_IF_BIGGER );
		
		mButton1 = (Button) findViewById( R.id.button );
		mButton2 = (Button) findViewById( R.id.button2 );
		
		mButton1.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				selectRandomImage();
			}
		} );
		
		mButton2.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				
				int current = mImage.getDisplayType().ordinal() + 1;
				if( current >= DisplayType.values().length ) {
					current = 0;
				}
				
				mImage.setDisplayType( DisplayType.values()[current] );
			}
		} );
		
		mImage.setSingleTapListener( new OnImageViewTouchSingleTapListener() {
			
			@Override
			public void onSingleTapConfirmed() {
				Log.d( LOG_TAG, "onSingleTapConfirmed" );
			}
		} );
		
		mImage.setDoubleTapListener( new OnImageViewTouchDoubleTapListener() {
			
			@Override
			public void onDoubleTap() {
				Log.d( LOG_TAG, "onDoubleTap" );
			}
		} );
		
		mImage.setOnDrawableChangedListener( new OnDrawableChangeListener() {
			
			@Override
			public void onDrawableChanged( Drawable drawable ) {
				Log.i( LOG_TAG, "onBitmapChanged: " + drawable );
			}
		} );
	}
	
	@Override
	public void onConfigurationChanged( Configuration newConfig ) {
		super.onConfigurationChanged( newConfig );
	}
	
	Matrix imageMatrix;

	public void selectRandomImage() {
		Cursor c = getContentResolver().query( Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null );
		if ( c != null ) {
			int count = c.getCount();
			int position = (int) ( Math.random() * count );
			if ( c.moveToPosition( position ) ) {
				long id = c.getLong( c.getColumnIndex( Images.Media._ID ) );

				Uri imageUri = Uri.parse( Images.Media.EXTERNAL_CONTENT_URI + "/" + id );
				
				// imageUri = Uri.parse( "content://media/external/images/media/7993" ); // test

				Log.d( "image", imageUri.toString() );
				
				final int size = -1; // use the original image size
				Bitmap bitmap = DecodeUtils.decode( this, imageUri, size, size );
				if( null != bitmap )
				{
					// calling this will force the image to fit the ImageView container width/height
					
					if( null == imageMatrix ) {
						imageMatrix = new Matrix();
					} else {
						// get the current image matrix, if we want restore the 
						// previous matrix once the bitmap is changed
						// imageMatrix = mImage.getDisplayMatrix();
					}
					
					mImage.setImageBitmap( bitmap, imageMatrix.isIdentity() ? null : imageMatrix, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );
					
				} else {
					Toast.makeText( this, "Failed to load the image", Toast.LENGTH_LONG ).show();
				}
			}
			c.close();
			c = null;
			return;
		}
	}
}
