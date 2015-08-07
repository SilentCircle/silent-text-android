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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.twuni.twoson.IllegalFormatException;
import org.twuni.twoson.JSONGenerator;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.silentcircle.api.model.Entitlement;
import com.silentcircle.api.model.User;
import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silentstorage.util.IOUtils;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.SCimpBridge;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.XMPPTransport;
import com.silentcircle.silenttext.dialog.SCDialogFragment;
import com.silentcircle.silenttext.fragment.ChatFragment;
import com.silentcircle.silenttext.graphics.AvatarUtils;
import com.silentcircle.silenttext.graphics.CircleClipDrawable;
import com.silentcircle.silenttext.listener.ClickSendOnEditorSendAction;
import com.silentcircle.silenttext.listener.SendMessageOnClick;
import com.silentcircle.silenttext.location.LocationObserver;
import com.silentcircle.silenttext.location.LocationUtils;
import com.silentcircle.silenttext.location.OnLocationReceivedListener;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.Siren;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.io.json.JSONSirenSerializer;
import com.silentcircle.silenttext.model.json.util.JSONSirenDecorator;
import com.silentcircle.silenttext.provider.PictureProvider;
import com.silentcircle.silenttext.provider.VideoProvider;
import com.silentcircle.silenttext.receiver.NotificationBroadcaster;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.Repository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;
import com.silentcircle.silenttext.repository.remote.RemoteContactRepository;
import com.silentcircle.silenttext.service.OrgNameService;
import com.silentcircle.silenttext.task.GetDeviceChangedTask;
import com.silentcircle.silenttext.task.GetSecureStatusTask;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.BurnDelay;
import com.silentcircle.silenttext.util.ClipboardUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.SilentPhone;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.util.ViewUtils;
import com.silentcircle.silenttext.view.ComposeText;
import com.silentcircle.silenttext.view.ConversationOptionsDrawer;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.UploadView;

public class ConversationActivity extends SilentActivity implements ChatFragment.Callback {

	private class AutoRefresh extends TimerTask {

		public AutoRefresh() {
		}

		@Override
		public void run() {
			refresh();
		}

	}

	public static class EncryptPacketTask extends AsyncTask<Void, Void, Void> {

		private final SCimpBridge bridge;
		private final String remoteUserID;
		private final String packetID;
		private final String text;
		private final boolean notifiable;
		private final boolean badgeworthy;

		public EncryptPacketTask( SCimpBridge bridge, String remoteUserID, String packetID, String text, boolean notifiable, boolean badgeworthy ) {
			this.bridge = bridge;
			this.remoteUserID = remoteUserID;
			this.packetID = packetID;
			this.text = text;
			this.notifiable = notifiable;
			this.badgeworthy = badgeworthy;
		}

		@Override
		protected Void doInBackground( Void... params ) {
			bridge.encrypt( remoteUserID, packetID, text, notifiable, badgeworthy );
			return null;
		}

	}

	class RefreshTask extends AsyncTask<String, Void, List<Event>> {

		@Override
		protected List<Event> doInBackground( String... args ) {

			String partner = args[0];

			if( partner == null ) {
				partner = getPartner();
			}
			boolean existed = getConversations().exists( partner );
			conversation = getOrCreateConversation( partner );
			ConversationActivity.this.partner = partner;
			if( !existed ) {
				getNative().connect( partner );
			}

			EventRepository history = getConversations().historyOf( conversation );
			List<Event> events = history.list();

			long autoRefreshTime = Long.MAX_VALUE;
			for( Event event : events ) {
				if( event instanceof Message ) {
					Message message = (Message) event;
					if( event instanceof IncomingMessage ) {
						if( MessageState.DECRYPTED.equals( message.getState() ) ) {
							message.setState( MessageState.READ );
							if( message.expires() ) {
								message.setExpirationTime( System.currentTimeMillis() + message.getBurnNotice() * 1000L );
							}
							history.save( message );
							transition( conversation.getPartner().getUsername(), message.getId() );
							if( shouldSendDeliveryNotification( message ) ) {
								sendDeliveryNotification( message.getId() );
							}
						}
					}
					if( message.expires() ) {
						autoRefreshTime = Math.min( autoRefreshTime, message.getExpirationTime() );
					}
				}
			}
			if( autoRefreshTime < Long.MAX_VALUE ) {
				scheduleAutoRefresh( autoRefreshTime );
			}
			if( conversation.getUnreadMessageCount() > 0 ) {
				conversation.setUnreadMessageCount( 0 );
				getConversations().save( conversation );
			}

			empty = events.isEmpty();

			filter( events );

			return events;

		}

		@Override
		protected void onPostExecute( List<Event> events ) {

			try {
				updateDisplayName();
				setText( R.id.device_name, conversation.getPartner().getDevice() );

				updateViews( events );

				findViewById( R.id.compose_send ).setOnClickListener( createSendMessageOnClickListener( conversation ) );

				updateActionBar();
				updateOptionsDrawer();
				invalidateSupportOptionsMenu();
				refreshing = false;
			} catch( Exception e ) {
				Log.e( "ConversationActivity", "RefreshTask.onPostExecute try to catch nullpiointer : " + e.getMessage() );
			}

		}

	}

	class RefreshUserTask extends AsyncTask<String, Void, User> {

		@Override
		protected User doInBackground( String... args ) {
			final User updatedUser = getUser( args[0], true );

			if( getPartner() == args[0] ) {
				AsyncUtils.execute( new GetDeviceChangedTask( getApplicationContext() ) {

					@Override
					protected void onPostExecute( Void result ) {
						try {
							if( deviceChanged ) {
								// Since the partner's device has changed, we invalidate the context
								// and
								// reconnect.
								Repository<ResourceState> resourceStates = getConversations().contextOf( conversation );
								String resource = conversation.getPartner().getDevice();

								if( resourceStates != null && resource != null ) {
									ResourceState resourceState = resourceStates.findById( resource );

									if( resourceState != null ) {
										resourceStates.remove( resourceState );
									}
								}

								if( updatedUser != null && updatedUser.getID() != null ) {
									getSilentTextApplication().getSCimpBridge().connect( updatedUser.getID().toString() );
								}
							}
						} catch( Exception e ) {
							Log.e( "ConversationActivity", "GetDeviceChanedTask.onPostExecute try to catch nullpiointer : " + e.getMessage() );
						}
					}

				}, args[0] );
			}

			return updatedUser;
		}

		@Override
		protected void onPostExecute( User user ) {

			if( user == null || user.getID() == null ) {
				return;
			}

			try {
				partnerDisplayName = getSilentTextApplication().getDisplayName( user );
				updateDisplayName();
				loadAvatar( user.getID().toString(), R.id.avatar );

				invalidateSupportOptionsMenu();
			} catch( Exception e ) {
				Log.e( "ConversationActivity", "RefreshUserTask.onPostExecute try to catch nullpiointer : " + e.getMessage() );
			}
		}

	}

	class ToggleBurnNoticeTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground( Void... ignore ) {
			conversation.setBurnNotice( !conversation.hasBurnNotice() );
			if( conversation.hasBurnNotice() && conversation.getBurnDelay() <= 0 ) {
				conversation.setBurnDelay( BurnDelay.getDefaultDelay() );
			}
			save( conversation );
			return null;
		}

		@Override
		protected void onPostExecute( Void result ) {
			try {
				invalidateSupportOptionsMenu();
				updateOptionsDrawer();
			} catch( Exception e ) {
				Log.e( "ConversationActivity", "ToggleBurnNoticeTask.onPostExecute try to catch nullpiointer : " + e.getMessage() );
			}
		}
	}

	class ToggleLocationEnabledTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground( Void... params ) {
			conversation.setLocationEnabled( !conversation.isLocationEnabled() );
			save( conversation );
			return null;
		}

		@Override
		protected void onPostExecute( Void result ) {
			try {
				invalidateSupportOptionsMenu();
				updateOptionsDrawer();
			} catch( Exception e ) {
				Log.e( "ConversationActivity", "ToggleLocationEnabledTask.onPostExecute try to catch nullpiointer : " + e.getMessage() );
			}
		}
	}

	class ToggleVerifiedTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground( Void... ignore ) {
			ResourceStateRepository states = getConversations().contextOf( conversation );
			ResourceState state = states.findById( conversation.getPartner().getDevice() );
			states.toggleVerified( state );
			return null;
		}

		@Override
		protected void onPostExecute( Void result ) {
			try {
				invalidateSupportOptionsMenu();
				updateActionBar();
			} catch( Exception e ) {
				Log.e( "ConversationActivity", "ToggleVerifiedTask.onPostExecute try to catch nullpiointer : " + e.getMessage() );
			}
		}
	}

	public static final String [] SUPPORTED_IMTO_HOSTS = {
		"jabber",
		"silentcircle",
		"silent text",
		"silenttext",
		"silentcircle.com",
		"com.silentcircle",
		"silent circle"
	};

	public static final int MESSAGE_TEXT_SIZE_LIMIT = 1024;

	private static final int R_id_share = 0xFFFF & R.id.share;

	private static final JSONSirenSerializer SIREN = new JSONSirenSerializer();

	private static final char STAR_ON = '\u2605';

	private static final char STAR_OFF = '\u2606';

	private static final SimpleDateFormat ISO8601 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );

	static {
		ISO8601.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	public static final String ACCOUNT_EXPIRED_DIALOG_TAG = "account_expired_dialog";

	// private static final String JPG_EXTENSION = ".jpg";
	// private static final String VIDEO_EXTENSION = ".mp4";
	// public static final String JPG_FILE_NAME = "newImage.jpg";
	// public static final String VIDEO_FILE_NAME = "newVideo.mp4";

	private static Intent createCaptureAudioIntent() {
		return new Intent( MediaStore.Audio.Media.RECORD_SOUND_ACTION );
	}

	private static Intent createSelectFileIntent( String type ) {

		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );

		intent.setType( type );
		intent.addCategory( Intent.CATEGORY_OPENABLE );

		return intent;

	}

	protected static List<Event> filter( List<Event> events ) {
		for( int i = 0; i < events.size(); i++ ) {
			Event event = events.get( i );
			if( event instanceof Message ) {
				Message message = (Message) event;
				switch( message.getState() ) {
					case RECEIVED:
					case BURNED:
					case UNKNOWN:
						events.remove( i );
						i--;
						break;
					case COMPOSED:
						if( SilentTextApplication.isOutgoingResendRequest( message ) ) {
							events.remove( i );
							i--;
						}
						break;
					default:
						break;
				}
				if( message instanceof IncomingMessage && MessageState.DECRYPTED.compareTo( message.getState() ) <= 0 ) {
					Siren siren = message.getSiren();
					if( siren == null || !( siren.isChatMessage() || siren.hasAttachments() ) ) {
						events.remove( i );
						i--;
					}
				}
			}
		}
		return events;
	}

	protected static long getDelayForLevel( int level ) {
		return BurnDelay.Defaults.getDelay( level );
	}

	protected static int getLevelForDelay( int delay ) {
		return BurnDelay.Defaults.getLevel( delay );
	}

	private static int getSecureStatusColor( boolean secureViaPublicKey, boolean secureViaDH, boolean verified ) {
		if( secureViaDH ) {
			return verified ? R.color.silent_green : R.color.silent_yellow;
		}
		if( secureViaPublicKey ) {
			return R.color.silent_orange;
		}
		return R.color.silent_white;
	}

	private static String getSecurityRating( boolean secureViaPublicKey, boolean secureViaDH, boolean verified, boolean rtl ) {

		StringBuilder s = new StringBuilder();

		if( secureViaDH ) {
			s.append( STAR_ON );
			s.append( STAR_ON );
			s.append( verified ? STAR_ON : STAR_OFF );
		} else if( secureViaPublicKey ) {
			s.append( STAR_ON );
			s.append( STAR_OFF );
			s.append( STAR_OFF );
		} else {
			s.append( STAR_OFF );
			s.append( STAR_OFF );
			s.append( STAR_OFF );
		}

		if( rtl ) {
			s.reverse();
		}

		return s.toString();

	}

	protected String partner;

	protected String partnerDisplayName;
	private BroadcastReceiver viewUpdater;
	protected Conversation conversation;
	private Timer timer;
	protected boolean empty;
	protected boolean refreshing;
	private boolean waitingForResult;
	private Uri pendingImageCaptureUri;

	private Uri pendingVideoCaptureUri;

	private boolean viewOnly = true;

	Runnable mAccountExpirationRunnable = new Runnable() {

		@Override
		public void run() {
			Intent intent = new Intent( ConversationActivity.this, OrgNameService.class );
			startService( intent );
		}
	};

	private final BroadcastReceiver mAccountExpiredReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive( Context context, Intent intent ) {
			Constants.mIsAccountExpired = true;
			showSCDialog();
		}
	};

	protected void applyConversationSettings( OutgoingMessage message ) {

		message.setConversationID( getPartner() );

		if( conversation != null && conversation.hasBurnNotice() ) {
			message.setBurnNotice( conversation.getBurnDelay() );
		}

	}

	protected void backToConversationList() {
		Intent intent = new Intent( this, ConversationListActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
		ViewUtils.startActivity( this, intent, R.anim.slide_in_from_left, R.anim.slide_out_to_right );
	}

	protected void cancelAutoRefresh() {
		if( timer != null ) {
			timer.cancel();
			timer = null;
			refreshing = false;
		}
	}

	protected void clearConversationLog() {
		getConversations().historyOf( conversation ).clear();
		updateViews();
		empty = true;
		updateOptionsDrawer();
	}

	private Intent createCaptureImageIntent() {
		// Uri uri = createTempURI( JPG_EXTENSION );
		// if( uri == null ) {
		// return null;
		// }
		// pendingImageCaptureUri = uri;
		// Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
		// intent.putExtra( MediaStore.EXTRA_OUTPUT, uri );
		// return intent;

		Intent intent = new Intent( android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
		Uri uri = Uri.parse( PictureProvider.CONTENT_URL_PREFIX + getFilesDir() + "/" + Long.toHexString( System.currentTimeMillis() ) + PictureProvider.JPG_EXTENSION );
		pendingImageCaptureUri = uri;
		intent.putExtra( MediaStore.EXTRA_OUTPUT, uri );
		return intent;
	}

	private Intent createCaptureVideoIntent() {
		// Uri uri = createTempURI( VIDEO_EXTENSION );
		// if( uri == null ) {
		// return null;
		// }
		// pendingVideoCaptureUri = uri;
		// Intent intent = new Intent( MediaStore.ACTION_VIDEO_CAPTURE );
		// intent.putExtra( MediaStore.EXTRA_OUTPUT, uri );
		// return intent;

		Intent intent = new Intent( MediaStore.ACTION_VIDEO_CAPTURE );
		Uri uri = Uri.parse( VideoProvider.CONTENT_URL_PREFIX + getFilesDir() + "/" + Long.toHexString( System.currentTimeMillis() ) + VideoProvider.VIDEO_EXTENSION );
		pendingVideoCaptureUri = uri;
		intent.putExtra( MediaStore.EXTRA_OUTPUT, uri );
		return intent;
	}

	protected String createDeliveryNotificationText( String messageID ) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		JSONGenerator json = new JSONGenerator( b );
		try {
			json.openObject();
			json.writeKey( "received_id" );
			json.writeString( messageID );
			json.next();
			json.writeKey( "received_time" );
			json.writeString( ISO8601.format( new Date() ) );
			json.closeObject();
		} catch( IOException exception ) {
			getLog().warn( exception, "#createDeliveryNotificationText" );
		}
		return b.toString();
	}

	protected void createOptionsDrawer() {
		getOptionsDrawer().attach( this, partner );
	}

	private OutgoingMessage createOutgoingMessage( Conversation conversation, JSONObject siren, Location location ) {

		JSONSirenDecorator.decorateSirenForConversation( siren, conversation, location, shouldRequestDeliveryNotification() );

		OutgoingMessage message = new OutgoingMessage( getUsername(), siren.toString() );
		message.setId( UUID.randomUUID().toString() );

		applyConversationSettings( message );

		return message;

	}

	private OutgoingMessage createOutgoingMessage( Conversation conversation, Location location ) {
		return createOutgoingMessage( conversation, new JSONObject(), location );
	}

	private OutgoingMessage createOutgoingMessage( Uri uri, String mimeType, Conversation conversation, Location location ) {
		JSONObject siren = new JSONObject();
		JSONSirenDecorator.decorateSirenForAttachment( this, siren, uri, mimeType );
		return createOutgoingMessage( conversation, siren, location );
	}

	protected SendMessageOnClick createSendMessageOnClickListener( Conversation conversation ) {

		TextView composeText = (TextView) findViewById( R.id.compose_text );

		return new SendMessageOnClick( composeText, getUsername(), conversation, getConversations(), getNative(), shouldRequestDeliveryNotification() ) {

			@Override
			public void onClick( View button ) {
				if( Constants.isAccountExpired() || Constants.mIsAccountExpired ) {
					String title = getResources().getString( R.string.directory_search_dialog_information_title );
					String msg = getResources().getString( R.string.account_expired_message );
					SCDialogFragment dialog = SCDialogFragment.newInstance( title, msg, android.R.string.ok, -1, Constants.ACCOUNT_EXPIRED_DIALOG );
					dialog.setCancelable( false );
					dialog.show( getFragmentManager(), ConversationActivity.ACCOUNT_EXPIRED_DIALOG_TAG );
				} else {
					getSilentTextApplication().validateAnySession();
					refreshUser();

					CharSequence text = source.getText();

					int size = text.length();

					if( size > MESSAGE_TEXT_SIZE_LIMIT ) {
						sendChatMessageAsAttachment( text );
						source.setText( null );
					} else if( size <= 0 ) {
						selectFile();
					} else {
						super.onClick( button );
					}
				}

			}

			@Override
			protected void withMessage( Message message ) {
				updateViews();
				empty = false;
				updateOptionsDrawer();
				if( isTalkingToSelf() ) {
					conversation.setPreviewEventID( message.getId() );
					conversation.setLastModified( message.getTime() );
					save( conversation );
				} else {
					transition( conversation.getPartner().getUsername(), message.getId() );
				}
			}

		};

	}

	// private Uri createTempURI( String extension ) {
	// // ContentValues values = new ContentValues();
	// // values.put( MediaColumns.TITLE, "CAPTURE_" + Long.toHexString( System.currentTimeMillis()
	// // ) + extension );
	// // return getContentResolver().insert( baseURI, values );
	//
	// Uri uri = null;
	// try {
	// String baseFolder = "";
	// if( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) ) {
	// baseFolder = Environment.getExternalStorageDirectory().getAbsolutePath();
	// } else {
	// baseFolder = getFilesDir().getAbsolutePath();
	// }
	// baseFolder = getFilesDir().getAbsolutePath();
	// File folder = new File( baseFolder );
	// if( !folder.exists() ) {
	// folder.mkdir();
	// }
	// String filename = "";
	// if( extension.equalsIgnoreCase( JPG_EXTENSION ) ) {
	// filename = JPG_FILE_NAME;
	// } else if( extension.equalsIgnoreCase( VIDEO_EXTENSION ) ) {
	// filename = VIDEO_FILE_NAME;
	// }
	// File file = new File( folder, filename );
	// if( !file.exists() ) {
	// file.createNewFile();
	// }
	// uri = Uri.fromFile( file );
	// } catch( Exception e ) {
	// android.util.Log.e( "MessageUtils", "Unable to save message to file: " + e.getMessage() );
	// }
	// return uri;
	// }

	// private void destroyURI( Uri uri ) {
	// if( uri != null ) {
	// getContentResolver().delete( uri, null, null );
	// }
	// }

	protected ChatFragment getChatFragment() {

		FragmentManager fm = getFragmentManager();
		ChatFragment fragment = (ChatFragment) fm.findFragmentById( R.id.chat );

		if( fragment == null ) {
			fragment = new ChatFragment();
			fm.beginTransaction().add( R.id.chat, fragment ).commit();
		}

		return fragment;

	}

	/**
	 * @param mimeType
	 */
	@TargetApi( Build.VERSION_CODES.GINGERBREAD_MR1 )
	private Conversation getConversation() {
		if( conversation == null ) {
			conversation = getConversation( getPartner() );
		}
		return conversation;
	}

	private List<Event> getConversationHistory() {
		return filter( getConversations().historyOf( conversation ).list() );
	}

	protected String getLabelForLevel( int level ) {
		return BurnDelay.Defaults.getLabel( this, level );
	}

	@Override
	protected String getLogTag() {
		return "ConversationActivity";
	}

	private String getMIMEType( Intent intent ) {
		String mimeType = null;
		mimeType = intent.getType();
		if( mimeType != null ) {
			return mimeType;
		}
		return AttachmentUtils.getMIMEType( this, intent.getData() );
	}

	protected ConversationOptionsDrawer getOptionsDrawer() {
		return (ConversationOptionsDrawer) findViewById( R.id.drawer_content );
	}

	protected String getPartner() {
		Intent intent = getIntent();
		if( intent != null ) {
			partner = Extra.PARTNER.from( intent );
			// implicitily called from Intent.ACTION_SENDTO
			if( TextUtils.isEmpty( partner ) ) {
				Uri uri = intent.getData();
				if( uri != null ) {
					if( "imto".equals( uri.getScheme() ) && StringUtils.isAnyOf( uri.getHost(), SUPPORTED_IMTO_HOSTS ) ) {
						partner = uri.getLastPathSegment();
						if( partner != null && partner.indexOf( '@' ) < 0 ) {
							partner = getSilentTextApplication().getFullJIDForUsername( partner ).toString();
						}
					}
				}

			}
		}

		if( conversation == null ) {
			return partner;
		}

		if( partner == null ) {
			return conversation.getPartner().getUsername();
		}

		return partner;
	}

	private String getSecureStatus( boolean secureViaPublicKey, boolean secureViaDH, boolean verified ) {
		boolean secure = secureViaPublicKey || secureViaDH;
		StringBuilder s = new StringBuilder();
		s.append( getString( secure ? R.string.secure : R.string.securing ) );
		if( secure ) {
			s.append( ", " );
			s.append( getString( verified ? R.string.verified : R.string.unverified ) );
		}
		return s.toString();
	}

	@Override
	protected boolean isInactive() {
		return !waitingForResult && super.isInactive();
	}

	protected boolean isTalkingToSelf() {
		String self = getUsername();
		return self != null && self.equals( conversation.getPartner().getUsername() );
	}

	protected void launchSilentPhoneCall() {
		try {
			startActivity( SilentPhone.getCallIntent( partner ) );
		} catch( ActivityNotFoundException exception ) {
			Toast.makeText( this, R.string.error_activity_not_found, Toast.LENGTH_SHORT ).show();
		}
	}

	@Override
	protected void lockContentView() {
		super.lockContentView();
		setTitle( "" );
		getActionBar().setSubtitle( null );
		setSecureStatus( null );
		getActionBar().setIcon( R.drawable.ic_launcher );
	}

	@Override
	public void onActionModeCreated() {
		findViewById( R.id.compose ).setVisibility( View.INVISIBLE );
		hideSoftKeyboard( R.id.compose_text );
	}

	@Override
	public void onActionModeDestroyed() {
		if( !viewOnly ) {
			findViewById( R.id.compose ).setVisibility( View.VISIBLE );
		}
	}

	@Override
	public void onActionPerformed() {
		refresh();
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {

		try {
			SilentActivity.assertPermissionToView( this, true, true, true );
		} catch( IllegalStateException exception ) {
			return;
		}

		switch( requestCode ) {
			case R_id_share:
				switch( resultCode ) {
					case RESULT_OK:
						Intent result = data;
						if( result == null ) {
							result = new Intent();
						}
						if( result.getData() == null ) {
							if( pendingImageCaptureUri != null ) {
								if( AttachmentUtils.getFileSize( this, pendingImageCaptureUri ) > 0 ) {
									result.setData( pendingImageCaptureUri );
									// destroyURI( pendingVideoCaptureUri );
								}
							}
							if( pendingVideoCaptureUri != null ) {
								if( AttachmentUtils.getFileSize( this, pendingVideoCaptureUri ) > 0 ) {
									result.setData( pendingVideoCaptureUri );
									// destroyURI( pendingImageCaptureUri );
								}
							}
						}
						pendingImageCaptureUri = null;
						pendingVideoCaptureUri = null;
						sendFile( result );
						return;
					default:
						break;
				}
				break;
			default:
				break;
		}
		// destroyURI( pendingImageCaptureUri );
		// destroyURI( pendingVideoCaptureUri );
		pendingImageCaptureUri = null;
		pendingVideoCaptureUri = null;
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		backToConversationList();
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_conversation_with_drawer );
		hideSoftKeyboardOnDrawerToggle();
		findViewById( R.id.upload ).setVisibility( View.GONE );
		findViewById( R.id.compose ).setVisibility( View.INVISIBLE );

		if( savedInstanceState != null ) {
			waitingForResult = savedInstanceState.getBoolean( "waitingForResult", waitingForResult );
		}

		Intent intent = getIntent();

		if( intent == null ) {
			finish();
			return;
		}

		getActionBar().setDisplayHomeAsUpEnabled( true );

		getChatFragment();

		final View activityRoot = findViewById( R.id.activity );

		activityRoot.getViewTreeObserver().addOnGlobalLayoutListener( new OnGlobalLayoutListener() {

			private boolean softKeyboardActive;

			@Override
			public void onGlobalLayout() {
				Rect r = new Rect();
				activityRoot.getWindowVisibleDisplayFrame( r );

				int heightDiff = activityRoot.getRootView().getHeight() - ( r.bottom - r.top );
				if( softKeyboardActive != heightDiff > 100 ) {
					softKeyboardActive = heightDiff > 100;
					onSoftKeyboardChanged( softKeyboardActive );
				}
			}

		} );

		TextView composeText = (TextView) findViewById( R.id.compose_text );

		composeText.addTextChangedListener( new TextWatcher() {

			@Override
			public void afterTextChanged( Editable s ) {
				( (ImageView) findViewById( R.id.compose_send ) ).setImageResource( s.length() > 0 ? R.drawable.ic_action_send : R.drawable.ic_action_attachment_2 );
			}

			@Override
			public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
				// Ignore.
			}

			@Override
			public void onTextChanged( CharSequence s, int start, int before, int count ) {
				// Ignore.
			}

		} );

		composeText.setOnEditorActionListener( new ClickSendOnEditorSendAction() );

		onNewIntent( intent );

	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		if( viewOnly ) {
			menu.clear();
		} else {
			getMenuInflater().inflate( R.menu.conversation, menu );

			if( conversation != null ) {

				MenuItem burnNotice = menu.findItem( R.id.action_burn_notice );
				boolean burnNoticeEnabled = conversation.hasBurnNotice();
				burnNotice.setIcon( burnNoticeEnabled ? R.drawable.ic_burn_selected : R.drawable.ic_burn_unselected );
				burnNotice.setVisible( burnNoticeEnabled );

				MenuItem shareLocation = menu.findItem( R.id.action_share_location );
				boolean locationSharingEnabled = LocationUtils.isLocationSharingAvailable( this ) && conversation.isLocationEnabled();
				shareLocation.setIcon( locationSharingEnabled ? R.drawable.ic_location_selected : R.drawable.ic_location_unselected );
				shareLocation.setVisible( locationSharingEnabled );

				MenuItem silentPhoneCall = menu.findItem( R.id.action_silent_phone_call );

				User cachedPartner = getSilentTextApplication().getUserFromCache( partner );

				if( cachedPartner != null ) {
					if( SilentPhone.supports( getActivity() ) && cachedPartner.getEntitlements() != null && cachedPartner.getEntitlements().contains( Entitlement.SILENT_PHONE ) ) {
						silentPhoneCall.setVisible( true );
					} else {
						silentPhoneCall.setVisible( false );
					}
				}
			}
		}

		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance( this ).unregisterReceiver( mAccountExpiredReceiver );
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		switch( keyCode ) {
			case KeyEvent.KEYCODE_MENU:
				toggleDrawer();
				break;
		}
		return super.onKeyDown( keyCode, event );
	}

	@Override
	protected void onNewIntent( Intent intent ) {
		super.onNewIntent( intent );
		String partner = null;
		if( Intent.ACTION_SENDTO.equals( intent.getAction() ) ) {
			Uri uri = intent.getData();
			if( uri != null ) {
				if( "imto".equals( uri.getScheme() ) && StringUtils.isAnyOf( uri.getHost(), SUPPORTED_IMTO_HOSTS ) ) {
					partner = uri.getLastPathSegment();
					if( partner != null && partner.indexOf( '@' ) < 0 ) {
						partner = getSilentTextApplication().getFullJIDForUsername( partner ).toString();
					}
				}
			}
		} else {
			partner = Extra.PARTNER.from( intent );
		}
		if( partner == null ) {
			finish();
			return;
		}
		onNewPartner( partner );
	}

	protected void onNewPartner( String partner ) {

		if( partner == null ) {
			return;
		}

		this.partner = partner;

		// the following causes -- "android.os.NetworkOnMainThreadException"
		// Sam S. uses hard code for temp solution.
		// if( StringUtils.equals( getSilentTextApplication().getUser( partner
		// ).getDisplayName().toString(), "scvoicemail" ) ) {
		// viewOnly = true;
		// } else {
		// viewOnly = false;
		//
		// findViewById( R.id.compose ).setVisibility( View.VISIBLE );
		// }

		if( StringUtils.equals( partner, "scvoicemail@silentcircle.com" ) ) {
			viewOnly = true;
		} else {
			viewOnly = false;

			findViewById( R.id.compose ).setVisibility( View.VISIBLE );
		}

		createOptionsDrawer();

		try {
			SilentActivity.assertPermissionToView( this, true, true, true );
		} catch( IllegalStateException exception ) {
			return;
		}

		refreshUser();
		this.partner = partner;
		refresh();
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case android.R.id.home:
				backToConversationList();
				return true;
			case R.id.action_burn_notice:
				toggleBurnNotice();
				return true;
			case R.id.action_share_location:
				toggleLocationSharing();
				return true;
			case R.id.action_silent_phone_call:
				launchSilentPhoneCall();
				return true;
			case R.id.options:
				toggleDrawer();
				hideSoftKeyboard( R.id.compose_text );
				return true;
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onPause() {
		super.onPause();

		if( viewUpdater != null ) {
			unregisterReceiver( viewUpdater );
			viewUpdater = null;
		}

		cancelAutoRefresh();

		findViewById( R.id.upload ).setVisibility( View.GONE );
	}

	@Override
	protected void onRestoreInstanceState( Bundle savedInstanceState ) {
		super.onRestoreInstanceState( savedInstanceState );
		if( savedInstanceState != null ) {
			pendingImageCaptureUri = savedInstanceState.getParcelable( "pendingImageCaptureUri" );
			pendingVideoCaptureUri = savedInstanceState.getParcelable( "pendingVideoCaptureUri" );
		}
		( (ComposeText) findViewById( R.id.compose_text ) ).restore( savedInstanceState );
	}

	@Override
	protected void onResume() {

		super.onResume();
		LocalBroadcastManager.getInstance( this ).registerReceiver( mAccountExpiredReceiver, new IntentFilter( OrgNameService.ACCOUNT_EXPIRED ) );

		try {
			SilentActivity.assertPermissionToView( this, true, true, true );
		} catch( IllegalStateException exception ) {
			return;
		}

		waitingForResult = false;
		unlockContentView();
		registerViewUpdater();
		if( partner == null ) {
			partner = getPartner();
		}
		refresh();
		IOUtils.delete( getCacheStagingDir() );
		NotificationBroadcaster.cancel( this );

		// check account expiration date.
		if( Constants.mAccountExpirationDate == null ) {
			new Handler().post( mAccountExpirationRunnable );
		}
	}

	@Override
	protected void onSaveInstanceState( Bundle outState ) {
		outState.putParcelable( "pendingImageCaptureUri", pendingImageCaptureUri );
		outState.putParcelable( "pendingVideoCaptureUri", pendingVideoCaptureUri );
		outState.putBoolean( "waitingForResult", waitingForResult );
		( (ComposeText) findViewById( R.id.compose_text ) ).save( outState );
	}

	protected void onSoftKeyboardChanged( boolean enabled ) {
		if( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
			if( enabled ) {
				getActionBar().hide();
			} else {
				getActionBar().show();
			}
		}
	}

	private String parseMessageTextFrom( Event event ) {
		if( !( event instanceof Message ) ) {
			return null;
		}
		try {
			Siren siren = SIREN.parse( event.getText() );
			return siren.getChatMessage();
		} catch( IllegalFormatException exception ) {
			getLog().warn( exception, "#parseMessageTextFrom message_id:%s", event.getId() );
		} catch( IOException exception ) {
			getLog().warn( exception, "#parseMessageTextFrom message_id:%s", event.getId() );
		}
		return null;
	}

	protected void performAction( int actionID, Event event ) {

		switch( actionID ) {

			case R.id.burn:

				if( event instanceof OutgoingMessage ) {

					final OutgoingMessage message = (OutgoingMessage) event;

					if( !isTalkingToSelf() && MessageState.SENT.compareTo( message.getState() ) <= 0 ) {

						AsyncUtils.execute( new AsyncTask<String, String, String []>() {

							@Override
							protected String [] doInBackground( String... ids ) {
								XMPPTransport jabber = getJabber();
								String partner = getPartner();
								if( jabber != null && partner != null ) {
									for( String id : ids ) {
										jabber.removeOfflineMessage( partner, id );
										publishProgress( id );
									}
								}
								return ids;
							}

							@Override
							protected void onPostExecute( String [] result ) {
								try {
									message.setState( MessageState.BURNED );
									getConversations().historyOf( conversation ).save( message );
									transition( conversation.getPartner().getUsername(), message.getId() );
								} catch( Exception e ) {
									Log.e( "ConversationActivity", "OutgoingMessage.onPostExecute try to catch nullpiointer : " + e.getMessage() );
								}
							}

						}, event.getId() );

						return;

					}

				}

				getConversations().historyOf( conversation ).remove( event );
				SilentTextApplication.from( this ).removeAttachments( event );

				break;

			case R.id.copy:
				ClipboardUtils.copy( this, parseMessageTextFrom( event ) );
				break;

			case R.id.action_resend:
				if( event instanceof OutgoingMessage ) {
					OutgoingMessage message = (OutgoingMessage) event;
					if( !isTalkingToSelf() ) {
						EventRepository events = getConversations().historyOf( conversation );
						events.remove( message );
						message.setId( UUID.randomUUID().toString() );
						message.setState( MessageState.COMPOSED );
						events.save( event );
						transition( conversation.getPartner().getUsername(), event.getId() );
						return;
					}
				}
				break;

			default:
				// Unknown or unhandled action.
				break;

		}

	}

	@Override
	public void performAction( int actionID, Object target ) {
		if( target instanceof Event ) {
			performAction( actionID, (Event) target );
		}
	}

	protected void refresh() {

		if( refreshing ) {
			return;
		}

		refreshing = true;

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				tasks.add( AsyncUtils.execute( new RefreshTask(), partner ) );
			}

		} );

	}

	protected void refreshUser() {

		partnerDisplayName = null;
		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				tasks.add( AsyncUtils.execute( new RefreshUserTask(), getPartner() ) );
			}

		} );

	}

	private void registerViewUpdater() {

		viewUpdater = new BroadcastReceiver() {

			@Override
			public void onReceive( Context context, Intent intent ) {
				String p = intent.getStringExtra( Extra.PARTNER.getName() );
				if( p != null && ( p.equals( partner ) || Constants.mIsSharePhoto ) ) {
					switch( Action.from( intent ) ) {
						case UPDATE_CONVERSATION:
							Constants.mIsSharePhoto = false;
							refresh();
							break;
						case PROGRESS:
							Intent cancel = Action.CANCEL.intent();
							Extra.PARTNER.to( cancel, Extra.PARTNER.from( intent ) );
							Extra.ID.to( cancel, Extra.ID.from( intent ) );
							setProgress( Extra.TEXT.getInt( intent ), Extra.PROGRESS.getInt( intent ), cancel );
							break;
						case CANCEL:
							findViewById( R.id.upload ).setVisibility( View.GONE );
							break;
						default:
							break;
					}
				}
			}

		};

		registerReceiver( viewUpdater, Action.UPDATE_CONVERSATION.filter(), Manifest.permission.READ, null );
		registerReceiver( viewUpdater, Action.PROGRESS.filter(), Manifest.permission.READ, null );
		registerReceiver( viewUpdater, Action.CANCEL.filter(), Manifest.permission.WRITE, null );

	}

	protected void save( OutgoingMessage message ) {
		if( message != null ) {
			ConversationRepository conversations = getConversations();
			if( conversations != null && conversation != null ) {
				EventRepository events = conversations.historyOf( conversation );
				if( events != null ) {
					events.save( message );
				}
			}
		}
	}

	protected void scheduleAutoRefresh( long autoRefreshTime ) {
		cancelAutoRefresh();
		timer = new Timer( "conversation:auto-refresh" );
		timer.schedule( new AutoRefresh(), new Date( autoRefreshTime ) );
	}

	protected void selectFile() {
		selectFileOfType( "*/*" );
	}

	private void selectFileOfType( String type ) {

		ChooserBuilder chooser = new ChooserBuilder( this );

		chooser.label( R.string.share_from );

		chooser.intent( createCaptureImageIntent() );
		chooser.intent( createCaptureAudioIntent(), R.string.record_voice_memo );
		chooser.intent( createCaptureVideoIntent() );
		chooser.intent( createSelectFileIntent( type ) );

		startActivityForResult( chooser.build(), R_id_share );

	}

	protected void selectPhoto() {
		selectFileOfType( "image/*" );
	}

	protected void sendChatMessageAsAttachment( final CharSequence text ) {

		Conversation conversation = getConversation();

		if( conversation != null && LocationUtils.isLocationSharingAvailable( this ) && conversation.isLocationEnabled() ) {

			LocationObserver.observe( this, new OnLocationReceivedListener() {

				@Override
				public void onLocationReceived( Location location ) {
					sendChatMessageAsAttachment( text, location );
				}

				@Override
				public void onLocationUnavailable() {
					onLocationReceived( null );
				}

			} );

			return;

		}

		sendChatMessageAsAttachment( text, null );

	}

	protected void sendChatMessageAsAttachment( CharSequence text, Location location ) {

		Conversation conversation = getConversation();
		OutgoingMessage message = createOutgoingMessage( conversation, location );
		message.setState( MessageState.UNKNOWN );
		save( message );

		Intent encrypt = Action.ENCRYPT.intent();

		encrypt.setDataAndType( null, "text/plain" );

		Extra.PARTNER.to( encrypt, conversation.getPartner().getUsername() );
		Extra.ID.to( encrypt, message.getId() );
		Extra.TEXT.to( encrypt, text );

		sendBroadcast( encrypt, Manifest.permission.WRITE );

	}

	protected void sendDeliveryNotification( String messageID ) {
		AsyncUtils.execute( new EncryptPacketTask( getNative(), conversation.getPartner().getUsername(), null, createDeliveryNotificationText( messageID ), false, false ) );
	}

	private void sendFile( final Intent intent ) {

		if( intent == null ) {
			return;
		}

		Uri uri = intent.getData();

		if( uri == null ) {
			uri = (Uri) intent.getParcelableExtra( Intent.EXTRA_STREAM );
		}

		if( uri == null ) {
			return;
		}

		final long size = AttachmentUtils.getFileSize( this, uri );

		if( size <= 0 ) {
			return;
		}

		if( size >= AttachmentUtils.FILE_SIZE_LIMIT ) {
			AttachmentUtils.showFileSizeErrorDialog( this );
			return;
		}

		Conversation conversation = getConversation();

		if( conversation != null && LocationUtils.isLocationSharingAvailable( this ) && conversation.isLocationEnabled() ) {

			LocationObserver.observe( this, new OnLocationReceivedListener() {

				@Override
				public void onLocationReceived( Location location ) {
					sendFile( intent, size, location );
				}

				@Override
				public void onLocationUnavailable() {
					// TODO Auto-generated method stub
				}

			} );

			return;

		}

		sendFile( intent, size, null );

	}

	protected void sendFile( Intent intent, long size, Location location ) {

		Conversation conversation = getConversation();
		Uri uri = intent.getData();

		if( uri == null ) {
			uri = (Uri) intent.getParcelableExtra( Intent.EXTRA_STREAM );
		}

		if( uri == null ) {
			return;
		}

		OutgoingMessage message = createOutgoingMessage( uri, intent.getType(), conversation, location );
		message.setState( MessageState.UNKNOWN );
		save( message );

		if( size > AttachmentUtils.FILE_SIZE_WARNING_THRESHOLD ) {
			AttachmentUtils.showFileSizeWarningDialog( this, conversation.getPartner().getUsername(), message.getId() );
		}

		Intent encrypt = Action.ENCRYPT.intent();

		encrypt.setDataAndType( uri, getMIMEType( intent ) );

		Extra.PARTNER.to( encrypt, conversation.getPartner().getUsername() );
		Extra.ID.to( encrypt, message.getId() );

		sendBroadcast( encrypt, Manifest.permission.WRITE );
	}

	@Override
	protected void sendToBackground() {
		if( !waitingForResult ) {
			super.sendToBackground();
		}
	}

	protected void setProgress( int labelResourceID, int progress, Intent cancelIntent ) {
		( (UploadView) findViewById( R.id.upload ) ).setProgress( labelResourceID, progress, cancelIntent );
	}

	private void setSecureStatus( String secureStatus ) {
		setSecureStatus( secureStatus, R.color.silent_white );
	}

	private void setSecureStatus( String secureStatus, int colorResourceID ) {
		TextView v = (TextView) findViewById( R.id.conversation_status );
		if( v != null ) {
			v.setText( secureStatus );
			v.setVisibility( secureStatus == null ? View.GONE : View.VISIBLE );
			v.setTextColor( getResources().getColor( colorResourceID ) );
		}
	}

	private void setSubtitle( String username, String securityRating ) {
		getActionBar().setSubtitle( String.format( "%s %s", StringUtils.formatUsername( username ), securityRating ) );
	}

	protected boolean shouldRequestDeliveryNotification() {
		return true;
	}

	protected boolean shouldSendDeliveryNotification( Message message ) {
		if( !OptionsDrawer.isSendReceiptsEnabled( this ) ) {
			return false;
		}
		try {
			JSONObject siren = new JSONObject( message.getText() );
			return siren.has( "request_receipt" );
		} catch( JSONException exception ) {
			return false;
		}
	}

	protected void showConversationHeader() {

		View activityRoot = findViewById( R.id.activity );

		if( activityRoot instanceof ViewGroup ) {
			View header = View.inflate( this, R.layout.conversation_header, null );
			( (ViewGroup) activityRoot ).addView( header, 0 );
		}

	}

	void showSCDialog() {
		String title = getResources().getString( R.string.directory_search_dialog_information_title );
		String msg = getResources().getString( R.string.account_expired_message );
		SCDialogFragment dialog = SCDialogFragment.newInstance( title, msg, android.R.string.ok, -1, Constants.ACCOUNT_EXPIRED_DIALOG );
		dialog.setCancelable( false );
		dialog.show( getFragmentManager(), ConversationActivity.ACCOUNT_EXPIRED_DIALOG_TAG );
	}

	@Override
	public void startActivityForResult( Intent intent, int requestCode ) {
		// cover ourselves when #sendToBackground is not called
		getSilentTextApplication().updateMostRecentActivity();
		waitingForResult = true;
		super.startActivityForResult( intent, requestCode );
	}

	@Override
	public void startActivityForResult( Intent intent, int requestCode, Bundle options ) {
		// cover ourselves when #sendToBackground is not called
		getSilentTextApplication().updateMostRecentActivity();
		waitingForResult = true;
		super.startActivityForResult( intent, requestCode, options );
	}

	protected void toggleBurnNotice() {
		tasks.add( AsyncUtils.execute( new ToggleBurnNoticeTask() ) );
	}

	protected void toggleLocationSharing() {

		if( !LocationUtils.isLocationSharingAvailable( this ) ) {
			LocationUtils.startLocationSettingsActivity( this );
			return;
		}

		tasks.add( AsyncUtils.execute( new ToggleLocationEnabledTask() ) );

	}

	protected void toggleVerified() {

		if( isTalkingToSelf() ) {
			return;
		}

		tasks.add( AsyncUtils.execute( new ToggleVerifiedTask() ) );

	}

	protected void updateActionBar() {

		if( conversation == null ) {
			return;
		}

		conversation.getPartner().setAlias( getSilentTextApplication().getDisplayName( conversation.getPartner().getUsername() ) );
		setTitle( conversation.getPartner().getAlias() );

		if( StringUtils.equals( conversation.getPartner().getAlias(), "Voice Mail" ) ) {
			// Don't display security rating or subtitle
		} else {
			updateSecureStatus();
		}

		Bitmap avatar = RemoteContactRepository.getCachedAvatar( conversation.getPartner().getUsername() );

		if( avatar == null ) {
			new AsyncTask<Void, Void, Bitmap>() {

				@Override
				protected Bitmap doInBackground( Void... params ) {
					return AvatarUtils.getAvatar( getActivity(), getSilentTextApplication().getContacts(), conversation.getPartner().getUsername(), R.dimen.avatar_small );
				}

				@Override
				protected void onPostExecute( Bitmap avatar ) {
					try {
						Drawable icon = null;
						if( avatar == null ) {
							icon = getResources().getDrawable( R.drawable.ic_avatar_placeholder );
						} else {
							icon = new BitmapDrawable( getResources(), avatar );
						}
						icon = new CircleClipDrawable( icon, getResources(), R.color.silent_dark_grey, R.color.silent_translucent_dark_grey, R.dimen.stroke_normal );
						getActionBar().setIcon( icon );
					} catch( Exception e ) {
						Log.e( "ConversationActivity", "avatar.onPostExecute try to catch nullpiointer : " + e.getMessage() );
					}
				}

			}.execute();
		} else {
			Drawable icon = null;

			icon = new BitmapDrawable( getResources(), avatar );
			icon = new CircleClipDrawable( icon, getResources(), R.color.silent_dark_grey, R.color.silent_translucent_dark_grey, R.dimen.stroke_normal );
			getActionBar().setIcon( icon );
		}

	}

	protected void updateDisplayName() {
		if( conversation != null && conversation.getPartner() != null ) {
			partnerDisplayName = getSilentTextApplication().getDisplayName( conversation.getPartner().getUsername() );
		}
		if( partnerDisplayName == null ) {
			return;
		}
		if( conversation != null && conversation.getPartner() != null ) {
			conversation.getPartner().setAlias( partnerDisplayName );
		}
		setTitle( partnerDisplayName );
		setText( R.id.display_name, partnerDisplayName );
	}

	protected void updateOptionsDrawer() {

		if( conversation == null ) {
			return;
		}

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				getOptionsDrawer().setPartner( partner );
			}

		} );

	}

	private void updateSecureStatus() {

		AsyncUtils.execute( new GetSecureStatusTask( this ) {

			@Override
			protected void onPostExecute( Void result ) {
				try {
					updateSecureStatus( supportsPKI, secure, verified );
				} catch( Exception e ) {
					Log.e( "ConversationActivity", "updateSecureStatus.onPostExecute try to catch nullpiointer : " + e.getMessage() );
				}
			}

		}, conversation );

	}

	protected void updateSecureStatus( boolean supportsPKI, boolean secure, boolean verified ) {
		setSecureStatus( getSecureStatus( supportsPKI, secure, verified ), getSecureStatusColor( supportsPKI, secure, verified ) );
		setSubtitle( conversation.getPartner().getUsername(), getSecurityRating( supportsPKI, secure, verified, isLayoutDirectionRTL() ) );
	}

	protected void updateViews() {
		updateViews( getConversationHistory() );
	}

	protected void updateViews( List<Event> events ) {
		if( !isFinishing() ) {
			getChatFragment().setEvents( events );
		}
	}

}
