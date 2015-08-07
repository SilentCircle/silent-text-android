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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.model.DownloadManagerEntry;
import com.silentcircle.silenttext.fragment.FileViewerFragment;
import com.silentcircle.silenttext.fragment.ImageViewerFragment;
import com.silentcircle.silenttext.fragment.MediaPlayerFragmentICS;
import com.silentcircle.silenttext.fragment.TextViewerFragment;
import com.silentcircle.silenttext.listener.ConfirmDialogNoRepeat;
import com.silentcircle.silenttext.listener.OnConfirmNoRepeatListener;
import com.silentcircle.silenttext.listener.OnProgressUpdateListener;
import com.silentcircle.silenttext.model.UserPreferences;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.MIME;
import com.silentcircle.silenttext.view.UploadView;

public class FileViewerActivity extends SilentActivity implements OnProgressUpdateListener, FileViewerFragment.Callback {

	class ExportFileTask extends AsyncTask<MenuItem, Void, MenuItem> {

		@Override
		protected MenuItem doInBackground( MenuItem... items ) {
			copyToExternalStorage( file );
			return items[0];
		}

		@Override
		protected void onPostExecute( MenuItem item ) {
			super.onPostExecute( item );
			item.setEnabled( true );

			getSilentTextApplication().getDownloadManagerRepository().save( new DownloadManagerEntry( AttachmentUtils.addToDownloadManager( getApplicationContext(), file.getName(), file.getName(), true, mimeType, AttachmentUtils.getExternalStorageFile( getActivity(), file ).getPath(), fileSize, true ), Uri.fromFile( AttachmentUtils.getExternalStorageFile( getActivity(), file ) ) ) );

			export( file, mimeType, false );
		}
	}

	protected class ProcessOptionsOnConfirmListener implements OnConfirmNoRepeatListener, DialogInterface.OnClickListener {

		private final MenuItem item;

		ProcessOptionsOnConfirmListener( MenuItem item ) {
			this.item = item;
		}

		@Override
		public void onClick( DialogInterface arg0, int arg1 ) {
			// export cancelled
			// do nothing
		}

		@Override
		public void onConfirm( Context context, boolean shouldNotShowAgain ) {
			UserPreferences preferences = getSilentTextApplication().getUserPreferences();
			preferences.ignoreWarningDecryptExternalStore = shouldNotShowAgain;
			// save prefs
			SilentTextApplication.from( context ).saveUserPreferences( preferences );

			// process the item as intended
			processMenuItem( item );
		}

	}

	class ShareFileTask extends AsyncTask<MenuItem, Void, MenuItem> {

		@Override
		protected MenuItem doInBackground( MenuItem... args ) {
			share( file, mimeType );
			return args[0];
		}
	}

	class ViewExportedFileTask extends AsyncTask<MenuItem, Void, MenuItem> {

		@Override
		protected MenuItem doInBackground( MenuItem... args ) {
			export( file, mimeType, true );
			return args[0];
		}
	}

	private static final int EXTERNAL_ACTIVITY_REQUEST = R.id.export & 0xFFFF;

	private static Fragment createMediaPlayerFragment( Uri uri, String mimeType ) {
		return MediaPlayerFragmentICS.create( uri, mimeType );
	}

	protected File file;
	protected String mimeType;
	protected boolean exporting;
	protected long fileSize;

	private Uri uri;

	protected void askUserWhatToDoWithThisFile( Uri fileURI, String fileMimeType ) {
		setContentFragment( FileViewerFragment.create( fileURI, fileMimeType ) );
		getActionBar().show();
	}

	private void burn( File burnFile ) {
		File externalFile = AttachmentUtils.getExternalStorageFile( this, burnFile );
		if( externalFile != null && externalFile.exists() ) {
			externalFile.delete();

			DownloadManagerEntry entry = getSilentTextApplication().getDownloadManagerRepository().findByID( externalFile.getPath().toCharArray() );
			if( entry != null ) {
				AttachmentUtils.removeFromDownloadManager( getApplicationContext(), entry.getDownloadId() );
			}
			getSilentTextApplication().getDownloadManagerRepository().removeByID( externalFile.getPath().toCharArray() );
		}
		finish();
	}

	protected File copyToExternalStorage( File file ) {
		return copyToExternalStorage( file, this );
	}

	protected File copyToExternalStorage( File file, OnProgressUpdateListener onProgressUpdate ) {
		File externalFile = AttachmentUtils.getExternalStorageFile( this, file );
		if( externalFile == null || externalFile.exists() ) {
			return externalFile;
		}
		fileSize = AttachmentUtils.getFileSize( this, Uri.fromFile( file ) );
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream( file );
			out = new FileOutputStream( externalFile, false );
			exporting = true;
			IOUtils.pipe( in, out, onProgressUpdate );
			exporting = false;
			setVisibleIf( false, R.id.export );
			toast( R.string.saved_to, externalFile.getAbsolutePath() );
			invalidateSupportOptionsMenu();
			return externalFile;
		} catch( IOException exception ) {
			IOUtils.close( in, out );
			externalFile.delete();
		} finally {
			IOUtils.close( in, out );
		}
		return null;
	}

	protected void export( File originalFile, String fileMimeType, boolean openAfterwards ) {

		File externalFile = copyToExternalStorage( originalFile );

		if( !openAfterwards ) {
			return;
		}

		if( externalFile != null && externalFile.exists() ) {

			Uri data = Uri.fromFile( externalFile );

			Intent viewer = new Intent( Intent.ACTION_VIEW, data );
			if( launch( R.string.view_with, externalFile.getName(), viewer, fileMimeType ) ) {
				finish();
				return;
			}

		}

		toast( R.string.unable_to_display_file );

	}

	private boolean launch( int labelResourceID, String filename, Intent intent, String type ) {
		intent.setDataAndType( intent.getData(), type );
		if( startExternalActivity( intent, labelResourceID, filename ) ) {
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		if( requestCode == EXTERNAL_ACTIVITY_REQUEST ) {
			if( resultCode == RESULT_OK ) {
				finish();
				return;
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.fragment );

		Intent intent = getIntent();

		if( intent != null ) {

			uri = intent.getData();
			file = new File( uri.getPath() );
			mimeType = intent.getType();

			setTitle( file.getName() );

			// check text/x-vcard before generic text/...
			if( MIME.isContact( mimeType ) ) {
				// setContentFragment( ContactViewerFragment.create( uri, mimeType ) );
				// no longer using native contact viewer, use file viewer
				setContentFragment( FileViewerFragment.create( uri, mimeType ) );
			} else if( MIME.isText( mimeType ) ) {
				setContentFragment( TextViewerFragment.create( uri, mimeType ) );
			} else if( MIME.isImage( mimeType ) ) {
				setContentFragment( ImageViewerFragment.create( uri, mimeType ) );
			} else if( MIME.isVideo( mimeType ) ) {
				setContentFragment( createMediaPlayerFragment( uri, mimeType ) );
			} else if( MIME.isAudio( mimeType ) ) {
				setContentFragment( createMediaPlayerFragment( uri, mimeType ) );
			} else {
				setContentFragment( FileViewerFragment.create( uri, mimeType ) );
			}

		}

	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {

		getMenuInflater().inflate( R.menu.player, menu );

		boolean exported = AttachmentUtils.isExported( this, file );

		menu.findItem( R.id.view ).setVisible( AttachmentUtils.resolves( getPackageManager(), Intent.ACTION_VIEW, uri, mimeType ) );
		menu.findItem( R.id.share ).setVisible( AttachmentUtils.resolves( getPackageManager(), Intent.ACTION_SEND, uri, mimeType ) );
		menu.findItem( R.id.save ).setVisible( !exported );
		menu.findItem( R.id.burn ).setVisible( exported );

		return super.onCreateOptionsMenu( menu );

	}

	@Override
	public void onError( Uri fileURI, String fileMimeType ) {
		askUserWhatToDoWithThisFile( fileURI, fileMimeType );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.save:
			case R.id.view:
			case R.id.share:
				if( !verifyUserOKToExport( item ) ) {
					return true;
				}
				break;
		}
		return processMenuItem( item );
	}

	@Override
	public void onProgressUpdate( long progress ) {

		final int percent = (int) Math.ceil( 100.0 * progress / fileSize );

		runOnUiThread( new Runnable() {

			@Override
			public void run() {

				UploadView view = (UploadView) findViewById( R.id.export );

				if( view == null ) {
					return;
				}

				if( exporting && fileSize > 0 ) {
					view.setProgress( R.string.progress_exporting, percent, null );
					view.setVisibility( View.VISIBLE );
				} else {
					view.setVisibility( View.GONE );
				}

			}

		} );
	}

	protected boolean processMenuItem( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.save:
				item.setEnabled( false );
				tasks.add( AsyncUtils.execute( new ExportFileTask(), item ) );
				return true;
			case R.id.burn:
				burn( file );
				return true;
			case R.id.view:
				tasks.add( AsyncUtils.execute( new ViewExportedFileTask(), item ) );
				return true;
			case R.id.share:
				Constants.mIsSharePhoto = true;
				tasks.add( AsyncUtils.execute( new ShareFileTask(), item ) );
				return true;
		}
		return super.onOptionsItemSelected( item );
	}

	protected void setContentFragment( Fragment fragment ) {
		initializeErrorView();
		FragmentManager manager = getFragmentManager();
		Fragment existing = manager.findFragmentById( R.id.content );
		if( existing == null ) {
			manager.beginTransaction().add( R.id.content, fragment ).commit();
		} else {
			if( !existing.getClass().equals( fragment.getClass() ) ) {
				manager.beginTransaction().replace( R.id.content, fragment ).commit();
			}
		}
	}

	protected void share( File shareFile, String shareMimeType ) {
		File externalFile = copyToExternalStorage( shareFile );
		if( externalFile == null || !externalFile.exists() ) {
			return;
		}
		Uri data = Uri.fromFile( externalFile );
		Intent sender = new Intent( Intent.ACTION_SEND );
		sender.putExtra( Intent.EXTRA_STREAM, data );
		launch( R.string.share_with, externalFile.getName(), sender, shareMimeType );
	}

	private boolean startExternalActivity( Intent intent, int chooserTitleID, Object... chooserTitleArgs ) {
		if( intent.resolveActivity( getPackageManager() ) != null ) {
			startActivityForResult( Intent.createChooser( intent, getString( chooserTitleID, chooserTitleArgs ) ), EXTERNAL_ACTIVITY_REQUEST );
			return true;
		}
		return false;
	}

	protected boolean verifyUserOKToExport( MenuItem item ) {

		UserPreferences preferences = getSilentTextApplication().getUserPreferences();

		if( !preferences.ignoreWarningDecryptExternalStore ) {

			ProcessOptionsOnConfirmListener listener = new ProcessOptionsOnConfirmListener( item );
			ConfirmDialogNoRepeat alert = new ConfirmDialogNoRepeat( R.string.security_warning, R.string.verify_ok_media_to_be_exported, R.string.cancel, R.string._continue, this, listener, listener );
			alert.show();

			return false;

		}

		return true;

	}

}
