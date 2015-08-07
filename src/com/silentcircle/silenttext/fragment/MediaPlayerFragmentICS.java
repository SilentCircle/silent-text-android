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
import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.media.MediaPlayerWrapper;
import com.silentcircle.silenttext.util.MIME;

public class MediaPlayerFragmentICS extends FileViewerFragment implements MediaPlayer.OnErrorListener, SurfaceTextureListener, OnClickListener, MediaPlayer.OnPreparedListener {

	public static MediaPlayerFragmentICS create( Uri uri, String mimeType ) {
		return instantiate( new MediaPlayerFragmentICS(), uri, mimeType );
	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	private static String extractMetadata( MediaMetadataRetriever metaData, int metaDataKey ) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1 ? null : metaData.extractMetadata( metaDataKey );
	}

	private static void setViewBounds( final int width, final int height, final View view ) {

		view.post( new Runnable() {

			@Override
			public void run() {
				ViewGroup.LayoutParams dimensions = view.getLayoutParams();
				if( dimensions != null ) {
					dimensions.width = width;
					dimensions.height = height <= 0 ? 1 : height;
					view.requestLayout();
				}
			}

		} );

	}

	private MediaController controller;
	private SurfaceTexture texture;
	private MediaPlayer player;
	private MediaPlayerWrapper playerWrapper;

	protected void createController() {
		controller = new MediaController( getActivity() );
		controller.setAnchorView( getUnwrappedView() );
		controller.setMediaPlayer( playerWrapper );
		controller.setEnabled( true );
	}

	protected void createPlayer() {
		player = new MediaPlayer();
		playerWrapper = new MediaPlayerWrapper( player );
		try {
			player.setDataSource( getFile().getAbsolutePath() );
			player.setSurface( new Surface( texture ) );
			player.setOnBufferingUpdateListener( playerWrapper );
			player.setOnPreparedListener( this );
			player.prepareAsync();
		} catch( IOException exception ) {
			dispatchError();
		}
	}

	protected boolean hasActivity() {
		return getActivity() != null;
	}

	protected boolean hasView() {
		return getView() != null;
	}

	protected void hideController() {
		if( controller != null ) {
			controller.hide();
		}
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	protected boolean isChangingConfigurations() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getActivity().isChangingConfigurations();
	}

	protected boolean isFinishing() {
		Activity activity = getActivity();
		return activity == null || activity.isFinishing();
	}

	@Override
	public void onClick( View view ) {
		showController();
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		View view = inflater.inflate( R.layout.video_player, container, false );
		TextureView textureView = (TextureView) view.findViewById( R.id.texture );
		textureView.setSurfaceTextureListener( this );
		updateSurfaceTexture( textureView );
		view.setOnClickListener( this );
		return view;
	}

	@Override
	public boolean onError( MediaPlayer player, int what, int extra ) {
		dispatchError();
		return true;
	}

	@Override
	public void onPause() {

		super.onPause();

		hideController();

		if( !isFinishing() && !isDetached() && !isRemoving() && isAdded() ) {
			return;
		}

		if( controller != null ) {
			controller.hide();
			controller.setAnchorView( null );
			controller = null;
			getUnwrappedView().setOnClickListener( null );
		}

		if( !isChangingConfigurations() ) {
			if( player != null ) {
				if( player.isPlaying() ) {
					player.stop();
				}
				player.release();
				player = null;
			}
		}

	}

	@Override
	public void onPrepared( MediaPlayer player ) {

		getUnwrappedView().post( new Runnable() {

			@Override
			public void run() {
				resetViewBounds();
				createController();
				play();
				showController();
			}

		} );

	}

	@Override
	public void onResume() {

		super.onResume();

		if( texture != null ) {
			if( player == null ) {
				createPlayer();
			} else {
				resetViewBounds();
				createController();
			}
		}

		if( MIME.isAudio( getType() ) ) {
			showAudioMetaData();
		}

	}

	@Override
	public void onSurfaceTextureAvailable( SurfaceTexture texture, int width, int height ) {
		this.texture = texture;
		if( isResumed() ) {
			if( player == null ) {
				createPlayer();
			} else {
				createController();
			}
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed( SurfaceTexture texture ) {
		this.texture = texture;
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged( SurfaceTexture texture, int width, int height ) {
		this.texture = texture;
	}

	@Override
	public void onSurfaceTextureUpdated( SurfaceTexture texture ) {
		this.texture = texture;
	}

	protected void play() {
		if( playerWrapper != null ) {
			playerWrapper.start();
		}
	}

	protected void resetViewBounds() {
		if( player != null ) {
			setViewBounds( player.getVideoWidth(), player.getVideoHeight() );
		}
	}

	private void setViewBounds( int videoWidth, int videoHeight ) {

		DisplayMetrics dm = getResources().getDisplayMetrics();

		double rd, rv;

		rd = (double) dm.widthPixels / dm.heightPixels;
		rv = (double) videoWidth / videoHeight;

		final int wf, hf;

		if( rd > rv ) {
			wf = (int) ( (double) videoWidth * dm.heightPixels / videoHeight + 0.5 );
			hf = dm.heightPixels;
		} else {
			wf = dm.widthPixels;
			hf = (int) ( (double) videoHeight * dm.widthPixels / videoWidth + 0.5 );
		}

		setViewBounds( wf, hf, findViewById( R.id.texture ) );

	}

	protected void showAudioMetaData() {
		showAudioMetaData( getFile() );
	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	protected void showAudioMetaData( File file ) {

		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1 ) {

			MediaMetadataRetriever metaData = new MediaMetadataRetriever();

			try {
				metaData.setDataSource( file.getAbsolutePath() );
			} catch( RuntimeException exception ) {
				return;
			}

			String title = extractMetadata( metaData, MediaMetadataRetriever.METADATA_KEY_TITLE );
			String artist = extractMetadata( metaData, MediaMetadataRetriever.METADATA_KEY_ARTIST );
			String album = extractMetadata( metaData, MediaMetadataRetriever.METADATA_KEY_ALBUM );
			String subtitle = artist == null && album == null ? null : artist == null ? album : album == null ? artist : String.format( "%s - %s", artist, album );

			if( title != null ) {
				getActivity().getActionBar().setTitle( title );
			}

			if( subtitle != null ) {
				getActivity().getActionBar().setSubtitle( subtitle );
			}

			showEmbeddedPicture( metaData, R.id.artwork );

			return;

		}

	}

	protected void showController() {
		if( controller != null ) {
			if( isResumed() && isAdded() && isVisible() && hasActivity() && hasView() ) {
				if( !( isDetached() || isChangingConfigurations() || isFinishing() || isRemoving() ) ) {
					controller.show();
				}
			}
		}
	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	private void showEmbeddedPicture( MediaMetadataRetriever metaData, int viewResourceID ) {
		ImageView view = (ImageView) findViewById( viewResourceID );
		if( view != null ) {
			byte [] rawArtwork = metaData.getEmbeddedPicture();
			if( rawArtwork != null ) {
				Bitmap artwork = BitmapFactory.decodeByteArray( rawArtwork, 0, rawArtwork.length );
				view.setImageBitmap( artwork );
			}
		}
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN )
	private void updateSurfaceTexture( TextureView view ) {
		if( texture != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
			view.setSurfaceTexture( texture );
		}
	}

}
