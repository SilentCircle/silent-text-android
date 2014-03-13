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
package com.silentcircle.silenttext.activity;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchSingleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.jivesoftware.smack.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.scloud.NativePacket;
import com.silentcircle.scloud.PacketInput;
import com.silentcircle.scloud.listener.OnBlockDecryptedListener;
import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.client.SimpleHTTPClient;
import com.silentcircle.silenttext.client.URLBuilder;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.listener.HTTPResponseListener;
import com.silentcircle.silenttext.listener.ToggleActionBarOnClick;
import com.silentcircle.silenttext.media.AudioController;
import com.silentcircle.silenttext.media.AudioPlayer;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.siren.SirenObject;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.MIME;
import com.silentcircle.silenttext.util.UTI;

public class SCloudActivity extends SilentActivity implements OnPreparedListener {

	protected class AskUserWhatToDo extends TimerTask {

		@Override
		public void run() {
			runOnUiThread( new Runnable() {

				@Override
				public void run() {
					askUserWhatToDoWithThisFile();
				}

			} );
		}

	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	public static void setLayerType( ImageView view ) {
		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
			view.setLayerType( View.LAYER_TYPE_SOFTWARE, null );
		}
	}

	protected SCloudObjectRepository objects;
	protected OutputStream temp;
	protected SimpleHTTPClient http;
	protected PacketInput decryptor;
	protected File toFile;
	protected OutputStream toFileStream;
	protected String toFilePath;
	protected String mediaType;
	protected String mimeType;
	protected boolean decrypted;
	protected final byte [] buffer = new byte [64 * 1024];
	protected AsyncTask<SCloudObject, Void, Boolean> downloader;
	protected int startTime;
	protected AudioPlayer player;
	protected String originalFileName;
	protected Timer delayAskUserWhatToDo;
	private AsyncTask<MenuItem, Void, MenuItem> saveTask;

	protected void askUserWhatToDoWithThisFile() {

		setContentView( R.layout.activity_file );
		getSupportActionBar().show();

		View saveButton = findViewById( R.id.button_save );
		View shareButton = findViewById( R.id.button_share );
		View viewButton = findViewById( R.id.button_view );

		saveButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				export( false );
				updateActionSelector();
			}

		} );

		shareButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				share();
			}

		} );

		viewButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				export( true );
			}

		} );

		updateActionSelector();

	}

	private void burn() {
		File externalFile = getExternalStorageFile();
		if( externalFile != null && externalFile.exists() ) {
			externalFile.delete();
		}
		finish();
	}

	protected void cancelDelayedTasks() {
		if( delayAskUserWhatToDo != null ) {
			delayAskUserWhatToDo.cancel();
			delayAskUserWhatToDo = null;
		}
	}

	@TargetApi( Build.VERSION_CODES.FROYO )
	protected File copyToExternalStorage() {
		File externalFile = getExternalStorageFile();
		if( externalFile == null || externalFile.exists() ) {
			return externalFile;
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream( toFile );
			out = new FileOutputStream( externalFile, false );
			IOUtils.pipe( in, out );
			toast( R.string.saved_to, externalFile.getAbsolutePath() );
			_invalidateOptionsMenu();
			return externalFile;
		} catch( IOException exception ) {
			IOUtils.close( in, out );
			externalFile.delete();
		} finally {
			IOUtils.close( in, out );
		}
		return null;
	}

	protected void export() {
		export( true );
	}

	protected void export( boolean openAfterwards ) {

		File externalFile = copyToExternalStorage();

		if( !openAfterwards ) {
			return;
		}

		if( externalFile != null && externalFile.exists() ) {

			Uri data = Uri.fromFile( externalFile );

			Intent viewer = new Intent( Intent.ACTION_VIEW, data );
			if( launch( R.string.view_with, externalFile.getName(), viewer, mimeType ) ) {
				finish();
				return;
			}

		}

		toast( R.string.unable_to_display_file );

	}

	protected int getCurrentPosition() {
		if( player != null ) {
			return player.getCurrentPosition();
		}
		View view = findViewById( R.id.player );
		if( view instanceof VideoView ) {
			return ( (VideoView) view ).getCurrentPosition();
		}
		return 0;
	}

	@TargetApi( Build.VERSION_CODES.FROYO )
	protected File getExternalStorageFile() {
		if( !Environment.MEDIA_MOUNTED.equals( Environment.getExternalStorageState() ) ) {
			return null;
		}
		if( originalFileName == null ) {
			return null;
		}
		File externalCache = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS );
		externalCache.mkdirs();
		File externalFile = new File( externalCache, originalFileName );
		return externalFile;
	}

	protected SCloudObjectRepository getSCloudObjects() {
		return objects;
	}

	protected String getURL( SCloudObject object ) {
		return new URLBuilder( ServiceConfiguration.refresh( this ).scloud.url ).component( object.getLocator() ).build().toString();
	}

	protected void handleIntent() {
		handleIntent( getIntent() );
	}

	private void handleIntent( Intent intent ) {

		String eventID = Extra.ID.from( intent );
		String locator = Extra.LOCATOR.from( intent );
		String key = Extra.KEY.from( intent );

		setSCloudObjects( eventID );

		SCloudObject object = getSCloudObjects().findById( locator );

		if( object == null ) {
			object = new SCloudObject( key, locator, null );
		}

		File parent = getCacheStagingDir();
		parent.mkdirs();
		toFile = new File( parent, Hash.sha1( locator ) );
		toFilePath = toFile.getAbsolutePath();

		try {
			toFileStream = new FileOutputStream( toFile, false );
		} catch( IOException exception ) {
			IOUtils.close( toFileStream );
			return;
		}

		decryptor = new NativePacket();
		decryptor.onCreate();
		http = new SimpleHTTPClient();

		decryptor.setOnBlockDecryptedListener( new OnBlockDecryptedListener() {

			@Override
			public void onBlockDecrypted( byte [] dataBytes, byte [] metaDataBytes ) {

				if( downloader == null || downloader.isCancelled() ) {
					return;
				}

				if( metaDataBytes == null || metaDataBytes.length < 1 ) {
					pipe( dataBytes );
					return;
				}

				String metaDataString = new String( metaDataBytes );

				getLog().debug( "#onBlockDecrypted metadata:%s", metaDataString );

				try {

					JSONObject metaData = new JSONObject( metaDataString );
					String mediaType = metaData.getString( "MediaType" );
					if( metaData.has( "FileName" ) ) {
						originalFileName = metaData.getString( "FileName" );
					}

					if( "com.silentcircle.scloud.segment".equals( mediaType ) ) {
						pipe( dataBytes );
						return;
					}

					setMediaType( mediaType );

					int count = metaData.getInt( "Scloud_Segments" );

					if( count <= 1 ) {
						pipe( dataBytes );
						return;
					}

					String dataString = new String( dataBytes );
					JSONArray index = new JSONArray( dataString );

					int progressTotal = 1 + index.length();

					for( int i = 0; i < index.length(); i++ ) {

						reportProgress( 1 + i, progressTotal );

						JSONArray item = index.getJSONArray( i );
						String locator = item.getString( 1 );
						String key = item.getString( 2 );

						SCloudObject object = getSCloudObjects().findById( locator );
						if( object == null ) {
							object = new SCloudObject( key, locator, null );
						}

						load( object );

					}

					reportProgress( progressTotal, progressTotal );

				} catch( JSONException exception ) {
					pipe( dataBytes );
				}

			}

			private void pipe( byte [] data ) {
				if( data == null || data.length < 1 ) {
					return;
				}
				try {
					toFileStream.write( data );
				} catch( IOException exception ) {
					IOUtils.close( toFileStream );
				}
			}

		} );

		decrypted = false;
		downloader = new AsyncTask<SCloudObject, Void, Boolean>() {

			@Override
			protected Boolean doInBackground( SCloudObject... objects ) {
				try {
					for( int i = 0; i < objects.length; i++ ) {
						if( isCancelled() ) {
							return Boolean.valueOf( false );
						}
						try {
							load( objects[i] );
						} catch( Throwable exception ) {
							toast( R.string.error_not_connected );
							log.error( exception, "#load" );
							finish();
							return Boolean.valueOf( false );
						}
					}
				} finally {
					decryptor.onDestroy();
					IOUtils.flush( toFileStream );
					IOUtils.close( toFileStream );
					decrypted = true;
				}
				return Boolean.valueOf( true );
			}

			private Bitmap getBitmap() {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 1;
				while( true ) {
					try {
						return BitmapFactory.decodeFile( toFilePath, options );
					} catch( OutOfMemoryError error ) {
						options.inSampleSize *= 2;
					}
				}
			}

			@Override
			protected void onPostExecute( Boolean success ) {

				if( success == null || !success.booleanValue() ) {
					return;
				}

				_invalidateOptionsMenu();

				if( originalFileName != null ) {
					String extension = AttachmentUtils.getExtensionFromFileName( originalFileName );
					mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension( extension );
					setTitle( originalFileName );
				}

				if( mediaType != null ) {

					if( UTI.isImage( mediaType ) ) {
						viewImage();
						return;
					}

					if( UTI.isVideo( mediaType ) ) {
						try {
							viewVideo();
							return;
						} catch( Throwable exception ) {
							log.warn( exception, "FAIL type:VIDEO via:UTI file:%s uti:%s", toFilePath, mediaType );
						}
					}

					if( UTI.isAudio( mediaType ) ) {
						try {
							viewAudio();
							return;
						} catch( Throwable exception ) {
							log.warn( exception, "FAIL type:AUDIO via:UTI file:%s uti:%s", toFilePath, mediaType );
						}
					}

				}

				if( originalFileName != null ) {

					File targetFile = new File( toFile.getParentFile(), originalFileName );

					if( toFile.renameTo( targetFile ) ) {
						toFile = targetFile;
						toFilePath = toFile.getAbsolutePath();
					}

					if( mimeType != null ) {

						if( MIME.isImage( mimeType ) ) {
							viewImage();
							return;
						}

						if( MIME.isVideo( mimeType ) ) {
							try {
								viewVideo();
								return;
							} catch( Throwable exception ) {
								log.warn( exception, "FAIL type:VIDEO via:MIME file:%s uti:%s mime:%s", toFilePath, mediaType, mimeType );
							}
						}

						if( MIME.isAudio( mimeType ) ) {
							try {
								viewAudio();
								return;
							} catch( Throwable exception ) {
								log.warn( exception, "FAIL type:AUDIO via:MIME file:%s uti:%s mime:%s", toFilePath, mediaType, mimeType );
							}
						}

					}

				}

				askUserWhatToDoWithThisFile();

			}

			private void viewAudio() throws IOException {

				setContentView( R.layout.audio_player );

				viewAudioMetaData();

				player = new AudioPlayer( new AudioController( SCloudActivity.this ) );
				player.addOnPreparedListener( SCloudActivity.this );

				player.start( toFilePath );

			}

			private void viewImage() {
				Bitmap bitmap = getBitmap();
				if( bitmap == null ) {
					Toast.makeText( getBaseContext(), getString( R.string.unable_to_display_image ), Toast.LENGTH_SHORT ).show();
					return;
				}
				ImageViewTouch view = new ImageViewTouch( getBaseContext() );
				if( bitmap.getWidth() * bitmap.getHeight() > 2048 * 2048 ) {
					setLayerType( view );
				}
				view.setDisplayType( DisplayType.FIT_TO_SCREEN );
				view.setImageBitmap( bitmap );
				view.setSingleTapListener( new OnImageViewTouchSingleTapListener() {

					@Override
					public void onSingleTapConfirmed() {
						toggleActionBar();
					}

				} );
				setContentView( view );
			}

			private void viewVideo() {

				setContentView( R.layout.video_player );

				VideoView view = (VideoView) findViewById( R.id.player );
				MediaController controller = new MediaController( view.getContext() );
				view.setOnErrorListener( new OnErrorListener() {

					@Override
					public boolean onError( MediaPlayer mp, int what, int extra ) {
						cancelDelayedTasks();
						delayAskUserWhatToDo = new Timer();
						delayAskUserWhatToDo.schedule( new AskUserWhatToDo(), 20 );
						return true;
					}

				} );
				controller.setAnchorView( view );
				view.setOnClickListener( new ToggleActionBarOnClick() );
				view.setMediaController( controller );
				view.setVideoURI( Uri.fromFile( toFile ) );
				if( startTime > 0 ) {
					view.seekTo( startTime );
					startTime = 0;
				}
				view.requestFocus();
				view.start();

			}

		}.execute( object );

	}

	protected boolean isExported() {
		File externalFile = getExternalStorageFile();
		return externalFile != null && externalFile.exists();
	}

	protected boolean isShareable() {
		return isShareable( getExternalStorageFile() );
	}

	protected boolean isShareable( File file ) {
		return file != null && isShareable( Uri.fromFile( file ) );
	}

	protected boolean isShareable( Uri uri ) {
		if( uri == null ) {
			return false;
		}
		Intent sender = new Intent( Intent.ACTION_SEND );
		sender.setDataAndType( sender.getData(), mimeType );
		sender.putExtra( Intent.EXTRA_STREAM, uri );
		return sender.resolveActivity( getPackageManager() ) != null;
	}

	protected boolean isViewable() {
		return isViewable( getExternalStorageFile() );
	}

	protected boolean isViewable( File file ) {
		return file != null && isViewable( Uri.fromFile( file ) );
	}

	protected boolean isViewable( Uri uri ) {
		if( uri == null ) {
			return false;
		}
		Intent intent = new Intent( Intent.ACTION_VIEW, uri );
		intent.setDataAndType( intent.getData(), mimeType );
		return intent.resolveActivity( getPackageManager() ) != null;
	}

	protected boolean launch( int labelResourceID, String filename, Intent intent, String type ) {
		intent.setDataAndType( intent.getData(), type );
		if( startExternalActivity( intent, labelResourceID, filename ) ) {
			return true;
		}
		return false;
	}

	protected void load( SCloudObject object ) {
		try {
			loadFromLocalStorage( object );
		} catch( Exception exception ) {
			loadFromAmazon( object );
		}
	}

	protected void loadFromAmazon( final SCloudObject object ) {

		if( downloader == null || downloader.isCancelled() ) {
			return;
		}

		object.setURL( getURL( object ) );
		object.setUploaded( true );
		object.setDownloaded( false );
		getSCloudObjects().save( object );

		reportProgress( R.string.downloading );
		http.get( object.getURL(), new HTTPResponseListener() {

			@Override
			public void onResponse( HttpResponse response ) {

				if( downloader == null || downloader.isCancelled() ) {
					return;
				}

				int status = response.getStatusLine().getStatusCode();
				InputStream body = null;

				try {
					body = response.getEntity().getContent();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					for( int size = body.read( buffer ); size > 0; size = body.read( buffer, 0, size ) ) {
						out.write( buffer, 0, size );
					}
					response.getEntity().consumeContent();
					if( status > 200 ) {
						Log.e( "SCloudActivity", String.format( "[HTTP %d] %s\n%s", Integer.valueOf( status ), object.getURL(), new String( out.toByteArray() ) ) );
						return;
					}
					object.setData( out.toByteArray() );
					getSCloudObjects().write( object );
					object.setDownloaded( true );
					getSCloudObjects().save( object );
					object.setData( null );

					loadFromLocalStorage( object );

				} catch( IOException exception ) {
					// Whatever.
				} finally {
					try {
						response.getEntity().consumeContent();
					} catch( IOException ignore ) {
						// Ignore.
					}
					IOUtils.close( body );
				}
			}

		} );

	}

	protected void loadFromLocalStorage( SCloudObject object ) {
		if( downloader == null || downloader.isCancelled() ) {
			return;
		}
		if( !object.isDownloaded() ) {
			throw new RuntimeException( getString( R.string.not_downloaded ) );
		}
		reportProgress( R.string.decrypting );
		objects.read( object );
		decryptor.decrypt( object.getData(), object.getKey() );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.loading );
		getSupportActionBar().hide();
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {

		getSupportMenuInflater().inflate( R.menu.player, menu );

		boolean exported = isExported();

		menu.findItem( R.id.share ).setVisible( decrypted && exported && isShareable() );
		menu.findItem( R.id.save ).setVisible( decrypted && !exported );
		menu.findItem( R.id.burn ).setVisible( decrypted && exported );

		return super.onCreateOptionsMenu( menu );

	}

	@Override
	protected void onNewIntent( Intent intent ) {
		super.onNewIntent( intent );
		setIntent( intent );
		handleIntent( intent );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.save:
				item.setEnabled( false );
				saveTask = new AsyncTask<MenuItem, Void, MenuItem>() {

					@Override
					protected MenuItem doInBackground( MenuItem... items ) {
						copyToExternalStorage();
						return items[0];
					}

					@Override
					protected void onPostExecute( MenuItem item ) {
						super.onPostExecute( item );
						item.setEnabled( true );
						export( false );
						updateActionSelector();
					}

				};
				saveTask.execute( item );
				return true;
			case R.id.burn:
				burn();
				return true;
			case R.id.share:
				share();
				return true;
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	public void onPrepared( MediaPlayer mp ) {
		View view = findViewById( R.id.player );
		if( view == null ) {
			return;
		}
		view.setOnClickListener( new ToggleActionBarOnClick() );
		player.setAnchorView( view );
		if( startTime > 0 ) {
			player.seekTo( startTime );
			startTime = 0;
		}
		player.showControls();
	}

	@Override
	protected void onRestoreInstanceState( Bundle savedInstanceState ) {
		super.onRestoreInstanceState( savedInstanceState );
		startTime = savedInstanceState.getInt( "startTime" );
	}

	@Override
	protected void onResume() {

		super.onResume();

		if( !isUnlocked() ) {
			requestUnlock();
			return;
		}

		if( !isActivated() ) {
			requestActivation();
			return;
		}

		handleIntent();

	}

	@Override
	protected void onSaveInstanceState( Bundle outState ) {
		super.onSaveInstanceState( outState );
		outState.putInt( "startTime", getCurrentPosition() );
	}

	@Override
	protected void onStop() {
		super.onStop();
		if( player != null ) {
			player.finish();
		}
		if( saveTask != null ) {
			saveTask.cancel( true );
			saveTask = null;
		}
		if( downloader != null ) {
			downloader.cancel( true );
			downloader = null;
		}
		cancelDelayedTasks();
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		if( player != null ) {
			player.showControls();
		}
		return super.onTouchEvent( event );
	}

	protected void reportProgress( final int labelResourceID ) {
		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				TextView progressLabel = (TextView) findViewById( R.id.progress_label );
				if( progressLabel != null ) {
					progressLabel.setText( labelResourceID );
				}
			}

		} );
	}

	protected void reportProgress( final int progress, final int total ) {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				ProgressBar progressBar = (ProgressBar) findViewById( R.id.progress );
				if( progressBar != null ) {
					progressBar.setMax( total );
					progressBar.setProgress( progress );
				}
				setVisibleIf( progress > 0 && progress < total, R.id.progress_container );
			}

		} );

	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	private void setImageMetaData( MediaMetadataRetriever metaData, int viewResourceID ) {
		byte [] rawArtwork = metaData.getEmbeddedPicture();
		if( rawArtwork != null ) {
			Bitmap artwork = BitmapFactory.decodeByteArray( rawArtwork, 0, rawArtwork.length );
			( (ImageView) findViewById( viewResourceID ) ).setImageBitmap( artwork );
		}
	}

	protected void setMediaType( String mediaType ) {
		this.mediaType = mediaType;
	}

	private void setPreviewImage( Message message ) {
		ImageView view = (ImageView) findViewById( R.id.preview );
		if( view == null ) {
			return;
		}
		try {
			SirenObject siren = new SirenObject( message.getText() );
			String thumbnailString = siren.getString( "thumbnail" );
			byte [] thumbnailBytes = Base64.decode( thumbnailString );
			view.setVisibility( View.VISIBLE );
			view.setImageBitmap( BitmapFactory.decodeByteArray( thumbnailBytes, 0, thumbnailBytes.length ) );
		} catch( JSONException exception ) {
			view.setVisibility( View.GONE );
		}
	}

	private void setSCloudObjects( Conversation conversation, String eventID ) {
		ConversationRepository conversations = getConversations();
		EventRepository events = conversations.historyOf( conversation );
		Event event = events.findById( eventID );
		if( event != null ) {
			objects = events.objectsOf( event );
			if( event instanceof Message ) {
				setPreviewImage( (Message) event );
			}
		}
	}

	protected void setSCloudObjects( String eventID ) {
		ConversationRepository conversations = getConversations();
		for( Conversation conversation : conversations.list() ) {
			setSCloudObjects( conversation, eventID );
		}
	}

	protected void setSCloudObjects( String partner, String eventID ) {
		ConversationRepository conversations = getConversations();
		Conversation conversation = conversations.findByPartner( partner );
		setSCloudObjects( conversation, eventID );
	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	private void setTextMetaData( MediaMetadataRetriever metaData, int viewResourceID, int metaDataKey ) {
		String value = metaData.extractMetadata( metaDataKey );
		( (TextView) findViewById( viewResourceID ) ).setText( value );
	}

	protected void share() {
		File externalFile = copyToExternalStorage();
		if( externalFile == null || !externalFile.exists() ) {
			return;
		}
		Uri data = Uri.fromFile( externalFile );
		Intent sender = new Intent( Intent.ACTION_SEND );
		sender.putExtra( Intent.EXTRA_STREAM, data );
		launch( R.string.share_with, externalFile.getName(), sender, mimeType );
	}

	protected boolean startExternalActivity( Intent intent, int chooserTitleID, Object... chooserTitleArgs ) {
		if( intent.resolveActivity( getPackageManager() ) != null ) {
			startActivity( Intent.createChooser( intent, getString( chooserTitleID, chooserTitleArgs ) ) );
			return true;
		}
		return false;
	}

	protected void updateActionSelector() {

		View saveButton = findViewById( R.id.button_save );
		View shareButton = findViewById( R.id.button_share );
		View viewButton = findViewById( R.id.button_view );

		if( saveButton == null ) {
			return;
		}

		boolean exported = isExported();

		( (TextView) findViewById( R.id.file_action_description ) ).setText( exported ? R.string.cannot_open_file_saved : R.string.cannot_open_file_not_saved );

		saveButton.setVisibility( exported ? View.GONE : View.VISIBLE );
		viewButton.setVisibility( exported && isViewable() ? View.VISIBLE : View.GONE );
		shareButton.setVisibility( exported && isShareable() ? View.VISIBLE : View.GONE );

	}

	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	protected void viewAudioMetaData() {

		findViewById( R.id.metadata ).setVisibility( View.GONE );

		if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1 ) {

			MediaMetadataRetriever metaData = new MediaMetadataRetriever();

			metaData.setDataSource( toFilePath );

			setTextMetaData( metaData, R.id.artist, MediaMetadataRetriever.METADATA_KEY_ARTIST );
			setTextMetaData( metaData, R.id.album, MediaMetadataRetriever.METADATA_KEY_ALBUM );
			setTextMetaData( metaData, R.id.title, MediaMetadataRetriever.METADATA_KEY_TITLE );
			setImageMetaData( metaData, R.id.artwork );

			findViewById( R.id.metadata ).setVisibility( View.VISIBLE );
			findViewById( R.id.metadata ).startAnimation( AnimationUtils.loadAnimation( this, R.anim.slide_down ) );

			return;

		}

	}

}
