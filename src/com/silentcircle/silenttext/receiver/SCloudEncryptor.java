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
package com.silentcircle.silenttext.receiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.webkit.MimeTypeMap;

import com.silentcircle.core.util.StringUtils;
import com.silentcircle.http.client.exception.http.client.HTTPClientForbiddenException;
import com.silentcircle.scloud.model.SCloudObject;
import com.silentcircle.silentstorage.util.Base64;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.crypto.CryptoUtils;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.model.Attachment;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.provider.PictureProvider;
import com.silentcircle.silenttext.provider.VideoProvider;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.SCloudObjectRepository;
import com.silentcircle.silenttext.thread.CreateThumbnail;
import com.silentcircle.silenttext.thread.Deactivation;
import com.silentcircle.silenttext.thread.NamedThread;
import com.silentcircle.silenttext.thread.SCloudEncrypt;
import com.silentcircle.silenttext.thread.SCloudPrepareUpload;
import com.silentcircle.silenttext.thread.SCloudUpload;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.CursorUtils;
import com.silentcircle.silenttext.util.IOUtils;
import com.silentcircle.silenttext.util.MIME;
import com.silentcircle.silenttext.util.UTI;

public class SCloudEncryptor extends BroadcastReceiver {

	private static final String [] PROJECTION_getPathFromURI = new String [] {
		MediaColumns.DATA
	};
	private static final Log LOG = new Log( "SCloud" );

	protected static void abort( Context context, Intent intent ) {
		Constants.mIsSharePhoto = false;
		Intent cancel = Action.CANCEL.intent();

		Extra.PARTNER.to( cancel, Extra.PARTNER.from( intent ) );
		Extra.ID.to( cancel, Extra.ID.from( intent ) );
		Extra.TEXT.to( cancel, Extra.TEXT.from( intent ) );

		context.sendBroadcast( cancel, Manifest.permission.WRITE );

		reportProgress( context, R.string.cancelled, Extra.PARTNER.from( intent ), Extra.ID.from( intent ), 100 );

	}

	protected static void abort( Context context, Intent intent, String reason ) {
		LOG.error( "#abort reason:%s", reason );
		Extra.TEXT.to( intent, reason );
		abort( context, intent );
	}

	protected static void abort( Context context, Intent intent, Throwable exception ) {
		LOG.error( exception, "#abort" );
		if( exception != null ) {
			Extra.TEXT.to( intent, exception.getLocalizedMessage() );
		}
		abort( context, intent );
	}

	protected static Runnable createThumbnail( final Context context, final Intent intent ) {

		return new CreateThumbnail( context, intent, 120, 160 ) {

			@Override
			protected void onThumbnailCreated( Bitmap bitmap ) {

				SilentTextApplication application = (SilentTextApplication) context.getApplicationContext();

				if( !application.isUnlocked() ) {
					abort( context, intent, "locked" );
					return;
				}

				ConversationRepository conversations = application.getConversations();

				if( conversations == null || !conversations.exists() ) {
					abort( context, intent, "no conversations" );
					return;
				}

				Conversation conversation = conversations.findByPartner( Extra.PARTNER.from( intent ) );

				if( conversation == null ) {
					abort( context, intent, "conversation deleted" );
					return;
				}

				EventRepository events = conversations.historyOf( conversation );

				if( events == null || !events.exists() ) {
					abort( context, intent, "no history" );
					return;
				}

				Event event = events.findById( Extra.ID.from( intent ) );

				if( event == null ) {
					abort( context, intent, "message deleted" );
					return;
				}

				if( event instanceof OutgoingMessage ) {
					OutgoingMessage message = (OutgoingMessage) event;
					JSONObject json = null;
					try {
						json = new JSONObject( message.getText() );
						json.put( "thumbnail", encodeThumbnail( bitmap, intent.getType() ) );
					} catch( JSONException exception ) {
						// Whatever.
					}
					if( json != null ) {
						message.setText( json.toString() );
					}
					if( !isTalkingToSelf( context, conversation.getPartner().getUsername() ) ) {
						message.setState( MessageState.COMPOSED );
					}
					events.save( message );
					transition( context, conversation.getPartner().getUsername(), message.getId() );
					invalidate( context, conversation.getPartner().getUsername() );
				}

			}

		};

	}

	/**
	 * @param bitmap
	 * @param mimeType
	 * @return
	 */
	protected static String encodeThumbnail( Bitmap bitmap, String mimeType ) {
		if( bitmap == null ) {
			return null;
		}
		ByteArrayOutputStream thumbnailBytes = new ByteArrayOutputStream();
		bitmap.compress( CompressFormat.JPEG, 60, thumbnailBytes );
		String encodedThumbnail = Base64.encodeBase64String( thumbnailBytes.toByteArray() );
		return encodedThumbnail;
	}

	private static File getFileFromURI( ContentResolver resolver, Uri uri, String contentType ) {
		String path = getPathFromURI( resolver, uri, contentType );
		return path == null ? null : new File( path );
	}

	protected static String getFileName( ContentResolver resolver, Uri uri, String contentType ) {
		String fileName = getFileNameFromURI( resolver, uri, contentType );
		if( fileName == null ) {
			String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType( contentType );
			fileName = Integer.toHexString( uri == null ? CryptoUtils.randomInt() : uri.getPath().hashCode() );
			if( extension != null ) {
				fileName += "." + extension;
			}
		}
		return fileName;
	}

	private static String getFileNameFromURI( ContentResolver resolver, Uri uri, String contentType ) {
		File file = getFileFromURI( resolver, uri, contentType );
		return file == null ? null : file.getName();
	}

	private static JSONObject getMetaDataFromIntent( Context context, Uri uri, String contentType ) {

		JSONObject metaData = new JSONObject();

		try {
			metaData.put( "MediaType", getUTI( contentType ) );
			metaData.put( "MimeType", contentType );
			metaData.put( "FileName", getFileName( context.getContentResolver(), uri, contentType ) );
			metaData.put( "FileSize", AttachmentUtils.getFileSize( context, uri ) );
		} catch( JSONException ignore ) {
			// Damn.
		}

		return metaData;

	}

	/**
	 * @param resolver
	 * @param uri
	 * @param contentType
	 * @return
	 */
	private static String getPathFromURI( ContentResolver resolver, Uri uri, String contentType ) {
		if( uri == null ) {
			return null;
		}
		// we temp saved picture into internal storage, uri is for contentProvider and it's
		// different from path to the file.Same as Video.
		if( uri.toString().contains( PictureProvider.CONTENT_URL_PREFIX ) ) {
			return uri.getPath();
		} else if( uri.equals( VideoProvider.CONTENT_URL_PREFIX ) ) {
			return uri.getPath();
		}

		Cursor cursor = null;
		try {
			cursor = resolver.query( uri, PROJECTION_getPathFromURI, null, null, null );
		} catch( UnsupportedOperationException exception ) {
			return null;
		} catch( IllegalArgumentException exception ) {
			return null;
		}
		String path = uri.getPath();
		if( cursor == null ) {
			return path;
		}
		if( cursor.moveToFirst() ) {
			path = CursorUtils.getString( cursor, MediaColumns.DATA );
		}
		cursor.close();
		return path;
	}

	private static String getUTI( String mimeType ) {
		return UTI.fromMIMEType( mimeType );
	}

	protected static void invalidate( Context context, String remoteUserID ) {

		Intent intent = Action.UPDATE_CONVERSATION.intent();

		Extra.PARTNER.to( intent, remoteUserID );

		context.sendBroadcast( intent, Manifest.permission.READ );

	}

	protected static boolean isTalkingToSelf( Context context, String remoteUserID ) {
		SilentTextApplication application = (SilentTextApplication) context.getApplicationContext();
		String self = application.getUsername();
		return self != null && self.equals( remoteUserID );
	}

	protected static Runnable prepareUpload( final Context context, final Intent intent, final SCloudObjectRepository objectRepository ) {

		reportProgress( context, R.string.progress_preparing, Extra.PARTNER.from( intent ), Extra.ID.from( intent ), 1 );
		return new SCloudPrepareUpload( objectRepository.list(), SilentTextApplication.from( context ).getBroker() ) {

			private int count;

			@Override
			protected void onObjectPrepared( SCloudObject object ) {
				objectRepository.save( object );
				count++;
				reportProgress( context, R.string.progress_preparing, Extra.PARTNER.from( intent ), Extra.ID.from( intent ), (int) Math.ceil( 100 * ( (double) count / objects.size() ) ) );
			}

			@Override
			protected void onPrepareUploadComplete() {
				upload( context, intent, objectRepository ).run();
			}

			@Override
			protected void onPrepareUploadError( Throwable exception ) {
				abort( context, intent, exception );
				if( exception instanceof HTTPClientForbiddenException ) {
					new Thread( new Deactivation( context ), "deactivation" ).start();
				}
			}

		};

	}

	protected static void reportProgress( Context context, int labelResourceID, String partner, String eventID, int progress ) {

		Intent intent = Action.PROGRESS.intent();

		Extra.TEXT.to( intent, labelResourceID );
		Extra.PARTNER.to( intent, partner );
		Extra.ID.to( intent, eventID );
		Extra.PROGRESS.to( intent, progress );

		context.sendBroadcast( intent, Manifest.permission.READ );

	}

	protected static void transition( Context context, String remoteUserID, String eventID ) {

		Intent intent = Action.TRANSITION.intent();

		Extra.PARTNER.to( intent, remoteUserID );
		Extra.ID.to( intent, eventID );

		context.sendBroadcast( intent, Manifest.permission.WRITE );

	}

	protected static Runnable upload( final Context context, final Intent intent, final SCloudObjectRepository objectRepository ) {

		return new SCloudUpload( objectRepository ) {

			@Override
			protected void onObjectUploaded( SCloudObject object ) {
				SilentTextApplication application = SilentTextApplication.from( context );
				if( !application.isUnlocked() ) {
					throw new RuntimeException( context.getString( R.string.application_locked ) );
				}
			}

			@Override
			protected void onProgressUpdate( int progress, int max ) {
				reportProgress( context, R.string.progress_uploading, Extra.PARTNER.from( intent ), Extra.ID.from( intent ), (int) Math.ceil( 100 * ( (double) progress / max ) ) );
			}

			@Override
			protected void onUploadCancelled() {
				abort( context, intent, "Upload cancelled" );
				AttachmentUtils.deleteFile( intent.getData() );
			}

			@Override
			protected void onUploadComplete() {
				createThumbnail( context, intent ).run();
				AttachmentUtils.deleteFile( intent.getData() );
			}

			@Override
			protected void onUploadError( Throwable exception ) {
				abort( context, intent, exception );
				AttachmentUtils.deleteFile( intent.getData() );
			}

		};

	}

	@Override
	public void onReceive( final Context context, final Intent intent ) {

		Uri uri = intent.getData();
		final String mimeType = intent.getType();
		final CharSequence plainText = Extra.TEXT.getCharSequence( intent );
		final boolean isPlainText = MIME.isText( mimeType ) && plainText != null;

		if( !isPlainText ) {

			if( uri == null ) {
				uri = (Uri) intent.getParcelableExtra( Intent.EXTRA_STREAM );
			}

			if( uri == null ) {
				return;
			}

		}

		SilentTextApplication application = SilentTextApplication.from( context );
		ConversationRepository conversations = application.getConversations();

		if( conversations == null || !conversations.exists() ) {
			return;
		}

		Conversation conversation = conversations.findByPartner( Extra.PARTNER.from( intent ) );
		EventRepository events = conversations.historyOf( conversation );

		if( events == null || !events.exists() ) {
			return;
		}

		Event event = events.findById( Extra.ID.from( intent ) );

		if( event == null ) {
			return;
		}

		InputStream input = null;

		try {
			if( isPlainText ) {
				input = new ByteArrayInputStream( String.valueOf( plainText ).getBytes() );
			} else {
				input = context.getContentResolver().openInputStream( uri );
			}
		} catch( IOException exception ) {
			IOUtils.close( input );
			return;
		}

		String encryptionContext = Base64.encodeBase64String( CryptoUtils.randomBytes( 16 ) );
		new NamedThread( new SCloudEncrypt( encryptionContext, getMetaDataFromIntent( context, uri, mimeType ), events.objectsOf( event ), input ) {

			@Override
			protected void onEncrypted( SCloudObject object ) {
				reportProgress( context, R.string.progress_encrypting, Extra.PARTNER.from( intent ), Extra.ID.from( intent ), getProgressPercent() );
			}

			@Override
			protected void onEncryptionCancelled() {
				abort( context, intent, "encryption cancelled" );
			}

			@Override
			protected void onEncryptionComplete( SCloudObject index ) {

				if( index == null ) {
					abort( context, intent, "no index" );
					return;
				}

				SilentTextApplication application = (SilentTextApplication) context.getApplicationContext();

				if( !application.isUnlocked() ) {
					abort( context, intent, "locked" );
					return;
				}

				ConversationRepository conversations = application.getConversations();

				if( conversations == null || !conversations.exists() ) {
					abort( context, intent, "no conversations" );
					return;
				}

				Conversation conversation = conversations.findByPartner( Extra.PARTNER.from( intent ) );

				if( conversation == null ) {
					abort( context, intent, "conversation deleted" );
					return;
				}

				EventRepository events = conversations.historyOf( conversation );

				if( events == null || !events.exists() ) {
					abort( context, intent, "no history" );
					return;
				}

				Event event = events.findById( Extra.ID.from( intent ) );

				if( event == null ) {
					abort( context, intent, "message deleted" );
					return;
				}

				final SCloudObjectRepository objectRepository = events.objectsOf( event );

				Uri uri = intent.getData();

				if( uri == null ) {
					uri = (Uri) intent.getParcelableExtra( Intent.EXTRA_STREAM );
				}

				Attachment attachment = new Attachment();
				attachment.setKey( StringUtils.toByteArray( index.getKey() ) );
				attachment.setLocator( StringUtils.toByteArray( index.getLocator() ) );
				attachment.setType( StringUtils.toByteArray( mimeType ) );
				attachment.setName( StringUtils.toByteArray( getFileName( context.getContentResolver(), uri, mimeType ) ) );
				application.getAttachments().save( attachment );

				JSONObject json = null;
				try {

					json = new JSONObject( event.getText() );

					json.put( "mime_type", mimeType );
					json.put( "media_type", UTI.fromMIMEType( mimeType ) );
					json.put( "cloud_url", index.getLocator() );
					json.put( "cloud_key", index.getKey() );

				} catch( JSONException ignore ) {
					// This can never happen.
				}

				if( json != null ) {
					event.setText( json.toString() );
				}

				boolean isTalkingToSelf = isTalkingToSelf( context, Extra.PARTNER.from( intent ) );
				( (OutgoingMessage) event ).setState( isTalkingToSelf ? MessageState.SENT : MessageState.UNKNOWN );

				events.save( event );

				if( isTalkingToSelf ) {
					createThumbnail( context, intent ).run();
				} else {
					prepareUpload( context, intent, objectRepository ).run();
				}

			}

			@Override
			protected void onEncryptionError( Throwable exception ) {
				abort( context, intent, exception );
			}

		} ).start();

	}

}
