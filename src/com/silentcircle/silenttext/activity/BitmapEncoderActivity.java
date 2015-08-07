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
package com.silentcircle.silenttext.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.util.BitmapEncoder;

public class BitmapEncoderActivity extends Activity {

	protected final Log log = new Log( "BitmapEncoderActivity" );

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );

		Intent intent = getIntent();

		if( intent != null ) {
			onNewIntent( intent );
		}

	}

	@Override
	protected void onNewIntent( Intent intent ) {

		byte [] data = Extra.DATA.getByteArray( intent );

		if( data == null ) {
			setResult( RESULT_CANCELED );
			finish();
			return;
		}

		Bitmap bitmap = Bitmap.createBitmap( 320, 240, Bitmap.Config.ARGB_8888 );

		try {
			bitmap = BitmapEncoder.encodeToBitmap( data, bitmap );
		} catch( IllegalStateException exception ) {
			log.warn( exception, "#encodeToBitmap" );
			quit( exception );
			return;
		}

		setContentBitmap( bitmap );

	}

	private void quit( String message ) {
		Toast.makeText( this, message, Toast.LENGTH_SHORT ).show();
		setResult( RESULT_CANCELED );
		finish();
	}

	private void quit( Throwable exception ) {
		quit( exception.getMessage() );
	}

	private void setContentBitmap( Bitmap bitmap ) {

		if( bitmap == null ) {
			setResult( RESULT_CANCELED );
			finish();
			return;
		}

		ImageView v = new ImageView( this );

		v.setScaleType( ScaleType.CENTER_INSIDE );
		v.setImageBitmap( bitmap );

		setContentView( v );

	}

}
