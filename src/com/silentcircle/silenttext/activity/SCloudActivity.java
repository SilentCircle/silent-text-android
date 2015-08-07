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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.twuni.twoson.IllegalFormatException;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.AbstractHTTPClient;
import com.silentcircle.http.client.CachingHTTPClient;
import com.silentcircle.http.client.HTTPResponse;
import com.silentcircle.http.client.URLBuilder;
import com.silentcircle.http.client.apache.ApacheHTTPClient;
import com.silentcircle.http.client.apache.HttpClient;
import com.silentcircle.http.client.listener.HTTPResponseListener;
import com.silentcircle.scloud.NativePacket;
import com.silentcircle.scloud.PacketInput;
import com.silentcircle.scloud.listener.OnBlockDecryptedListener;
import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.ServiceConfiguration;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.crypto.Hash;
import com.silentcircle.silenttext.listener.ConfirmDialogNoRepeat;
import com.silentcircle.silenttext.listener.OnConfirmNoRepeatListener;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.model.UserPreferences;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.io.json.JSONSirenSerializer;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.MIME;

public class SCloudActivity extends SilentActivity {

	class AppendToFileOnBlockDecrypted implements OnBlockDecryptedListener {

		@Override
		public void onBlockDecrypted( byte [] dataBytes, byte [] metaDataBytes ) {

			if( metaDataBytes == null || metaDataBytes.length < 1 ) {
				pipe( dataBytes );
				return;
			}

			String metaDataString = new String( metaDataBytes );
			if( metaDataString.contains( "MimeType" ) && metaDataString.contains( "quicktime" ) ) {
				mIsVideoNotSupported = true;
				mFormat = "(.MOV/ or quicktime)";
				return;
			}

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

				// TODO: Remove right of && when Voice Mail is correctly MimeType'd
				if( metaData.has( "MimeType" ) && !StringUtils.equals( metaData.getString( "MimeType" ), "application/octet-stream" ) ) {
					mimeType = metaData.getString( "MimeType" );
				} else {
					mimeType = MIME.fromUTI( mediaType );
				}

				int count = metaData.has( "Scloud_Segments" ) ? metaData.getInt( "Scloud_Segments" ) : 1;

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

					SCloudObject object = getSCloudObjectRepository().findById( locator );
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

	}

	class LoadSCloudObjectTask extends AsyncTask<SCloudObject, Void, Boolean> {

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
						String message = exception.getLocalizedMessage();
						if( message != null ) {
							toast( R.string.error_format, message.split( "\n" )[0] );
						}
						Log.e( TAG, exception.getMessage() );
						log.error( exception, "#load" );
						finish();
						return Boolean.valueOf( false );
					}
				}
			} finally {
				decryptor.onDestroy();
				IOUtils.flush( toFileStream );
				IOUtils.close( toFileStream );
			}
			return Boolean.valueOf( true );
		}

		@Override
		protected void onPostExecute( Boolean success ) {

			if( success == null || !success.booleanValue() ) {
				return;
			}
			if( mIsVideoNotSupported ) {
				Toast.makeText( SCloudActivity.this, getString( R.string.not_supported_video_format, mFormat ), Toast.LENGTH_LONG ).show();
				finish();
				return;
			}
			if( originalFileName == null ) {
				if( mimeType != null ) {
					originalFileName = String.format( "%s.%s", toFile.getName(), MimeTypeMap.getSingleton().getExtensionFromMimeType( mimeType ) );
				}
			}

			if( originalFileName != null && mimeType == null ) {
				String extension = AttachmentUtils.getExtensionFromFileName( originalFileName );
				mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension( extension );
			}

			if( originalFileName != null ) {
				File targetFile = new File( toFile.getParentFile(), originalFileName );
				if( toFile.renameTo( targetFile ) ) {
					toFile = targetFile;
				}
			}

			Intent intent = new Intent( getActivity(), FileViewerActivity.class );

			intent.setDataAndType( Uri.fromFile( toFile ), mimeType );
			intent.setFlags( Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP );

			startActivity( intent );

			finish();

		}
	}

	protected class ProcessIntentOnConfirmListener implements OnConfirmNoRepeatListener, DialogInterface.OnClickListener {

		@Override
		public void onClick( DialogInterface dialog, int which ) {
			if( which == DialogInterface.BUTTON_NEGATIVE ) {
				finish();
				return;
			}
		}

		@Override
		public void onConfirm( Context context, boolean shouldNotShowAgain ) {
			UserPreferences preferences = getSilentTextApplication().getUserPreferences();
			preferences.ignoreWarningDecryptInternalStore = shouldNotShowAgain;
			SilentTextApplication.from( context ).saveUserPreferences( preferences );
			processIntent( getIntent() );
		}

	}

	private final static String TAG = "SCloudActivity";

	private static String getURL( SCloudObject object ) {
		return new URLBuilder( ServiceConfiguration.getInstance().scloud.url ).component( String.valueOf( object.getLocator() ) ).build().toString();
	}

	boolean mIsVideoNotSupported;
	String mFormat = "";

	private SCloudObjectRepository scloudObjectRepository;
	protected AbstractHTTPClient http;
	protected PacketInput decryptor;
	protected File toFile;
	protected OutputStream toFileStream;
	protected String mimeType;
	protected final byte [] buffer = new byte [64 * 1024];
	protected String originalFileName;
	protected boolean cancelled;

	@Override
	protected String getLogTag() {
		return "SCloudActivity";
	}

	protected SCloudObjectRepository getSCloudObjectRepository() {
		return scloudObjectRepository;
	}

	private void handleIntent() {
		handleIntent( getIntent() );
	}

	private void handleIntent( Intent intent ) {

		UserPreferences preferences = getSilentTextApplication().getUserPreferences();

		if( !preferences.ignoreWarningDecryptInternalStore ) {

			ProcessIntentOnConfirmListener listener = new ProcessIntentOnConfirmListener();
			ConfirmDialogNoRepeat alert = new ConfirmDialogNoRepeat( R.string.security_warning, R.string.verify_ok_media_to_be_decrypted, R.string.cancel, R.string._continue, this, listener, listener );

			alert.show();

			return;

		}

		processIntent( intent );

	}

	protected void load( SCloudObject object ) {
		try {
			loadFromLocalStorage( object );
		} catch( Exception exception ) {
			loadFromAmazon( object );
		}
	}

	private void loadFromAmazon( final SCloudObject object ) {

		object.setURL( getURL( object ) );
		object.setUploaded( true );
		object.setDownloaded( false );
		getSCloudObjectRepository().save( object );

		reportProgress( R.string.downloading );
		http.get( String.valueOf( object.getURL() ), new HTTPResponseListener() {

			@Override
			public void onResponse( HTTPResponse response ) {

				int status = response.getStatusCode();
				InputStream body = null;

				try {
					body = response.getContent();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					for( int size = body.read( buffer ); size > 0; size = body.read( buffer ) ) {
						out.write( buffer, 0, size );
					}
					if( status > 200 ) {
						String errorMessage = String.format( "HTTP %d %s\n%s\n%s", Integer.valueOf( status ), response.getStatusReason(), object.getURL(), out.toString() );
						throw new RuntimeException( errorMessage );
					}
					object.setData( out.toByteArray() );
					getSCloudObjectRepository().write( object );
					object.setDownloaded( true );
					getSCloudObjectRepository().save( object );
					object.setData( null );

					loadFromLocalStorage( object );

				} catch( IOException exception ) {
					getLog().error( exception, "#loadFromAmazon -> #onResponse" );
				} finally {
					IOUtils.close( body );
				}
			}

		} );

	}

	protected void loadFromLocalStorage( SCloudObject object ) {
		if( !object.isDownloaded() ) {
			throw new RuntimeException( getString( R.string.not_downloaded ) );
		}
		reportProgress( R.string.decrypting );
		scloudObjectRepository.read( object );

		// TODO: it would be nice to avoid String when converting from CharSequence to byte array
		decryptor.decrypt( object.getData(), String.valueOf( object.getKey() ) );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.loading );
		reportProgress( R.string.downloading );
		getActionBar().hide();
	}

	@Override
	protected void onNewIntent( Intent intent ) {
		super.onNewIntent( intent );
		setIntent( intent );
		handleIntent( intent );
	}

	@Override
	protected void onResume() {

		super.onResume();

		try {
			assertPermissionToView( this, true, true, true );
		} catch( IllegalStateException exception ) {
			return;
		}

		handleIntent();

	}

	protected void processIntent( Intent intent ) {
		String eventID = Extra.ID.from( intent );
		String locator = Extra.LOCATOR.from( intent );
		String key = Extra.KEY.from( intent );

		setSCloudObjects( eventID );

		SCloudObject object = getSCloudObjectRepository().findById( locator );

		if( object == null ) {
			object = new SCloudObject( key, locator, null );
		}

		File parent = getCacheStagingDir();
		parent.mkdirs();
		toFile = new File( parent, Hash.sha1( locator ) );

		try {
			toFileStream = new FileOutputStream( toFile, false );
		} catch( IOException exception ) {
			IOUtils.close( toFileStream );
			return;
		}

		decryptor = new NativePacket();
		decryptor.onCreate();
		http = new CachingHTTPClient( new ApacheHTTPClient( new HttpClient() ), getSilentTextApplication().getHTTPResponseCache() );

		decryptor.setOnBlockDecryptedListener( new AppendToFileOnBlockDecrypted() );
		tasks.add( AsyncUtils.execute( new LoadSCloudObjectTask(), object ) );

	}

	private void reportProgress( final int labelResourceID ) {
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
					setVisibleIf( progress > 0 && progress < total, R.id.progress_container );
				}

			}

		} );

	}

	private void setPreviewImage( Message message ) {

		ImageView view = (ImageView) findViewById( R.id.preview );

		if( view == null ) {
			return;
		}

		try {
			Siren siren = new JSONSirenSerializer().parse( message.getText() );
			Bitmap bitmap = AttachmentUtils.getPreviewImage( this, siren );
			view.setImageBitmap( bitmap );
			view.setVisibility( bitmap == null ? View.GONE : View.VISIBLE );
		} catch( IllegalFormatException exception ) {
			view.setVisibility( View.GONE );
		} catch( IOException exception ) {
			view.setVisibility( View.GONE );
		}

	}

	private void setSCloudObjects( Conversation conversation, String eventID ) {
		ConversationRepository conversations = getConversations();
		EventRepository events = conversations.historyOf( conversation );
		Event event = events.findById( eventID );
		if( event != null ) {
			scloudObjectRepository = events.objectsOf( event );
			if( event instanceof Message ) {
				setPreviewImage( (Message) event );
			}
		}
	}

	private void setSCloudObjects( String eventID ) {
		// look up the conversation for this eventID
		ConversationRepository conversations = getConversations();
		for( Conversation conversation : conversations.list() ) {
			setSCloudObjects( conversation, eventID );
		}
	}

}
