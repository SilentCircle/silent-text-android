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
package com.silentcircle.silenttext.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.view.View;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

public class AudioPlayer extends MediaPlayer implements OnPreparedListener, MediaPlayerControl {

	private final List<OnPreparedListener> onPreparedListeners = new ArrayList<OnPreparedListener>();

	protected final MediaController controller;

	public AudioPlayer( MediaController controller ) {
		setOnPreparedListener( this );
		this.controller = controller;
	}

	public void addOnPreparedListener( OnPreparedListener onPreparedListener ) {
		onPreparedListeners.add( onPreparedListener );
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	public void finish() {
		controller.hide();
		stop();
		release();
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public void onPrepared( MediaPlayer player ) {
		controller.setMediaPlayer( this );
		for( int i = 0; i < onPreparedListeners.size(); i++ ) {
			OnPreparedListener onPreparedListener = onPreparedListeners.get( i );
			if( onPreparedListener != null ) {
				onPreparedListener.onPrepared( player );
			}
		}
	}

	public void removeOnPreparedListener( OnPreparedListener onPreparedListener ) {
		onPreparedListeners.remove( onPreparedListener );
	}

	public void setAnchorView( View view ) {
		controller.setAnchorView( view );
	}

	public void showControls() {
		controller.setEnabled( true );
		controller.show( 0 );
	}

	public void start( Context context, Uri uri ) throws IOException {
		setDataSource( context, uri );
		prepare();
		start();
	}

	public void start( String path ) throws IOException {
		setDataSource( path );
		prepare();
		start();
	}

}
