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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.scimp.model.ResourceState;
import com.silentcircle.silentstorage.util.IOUtils;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.client.JabberClient;
import com.silentcircle.silenttext.listener.ClickSendOnEditorSendAction;
import com.silentcircle.silenttext.listener.ClickthroughWhenNotInChoiceMode;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector.ActionPerformer;
import com.silentcircle.silenttext.listener.SendMessageOnClick;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.siren.SirenObject;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.repository.ResourceStateRepository;
import com.silentcircle.silenttext.util.AttachmentUtils;
import com.silentcircle.silenttext.util.BurnDelay;
import com.silentcircle.silenttext.util.ClipboardUtils;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.view.ComposeText;
import com.silentcircle.silenttext.view.ConversationOptionsDrawer;
import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.UploadView;
import com.silentcircle.silenttext.view.adapter.EventAdapter;
import com.silentcircle.silenttext.view.adapter.ListAdapter.OnItemRemovedListener;

public class ConversationActivity extends SilentActivity implements ActionPerformer {

	private class AutoRefresh extends TimerTask {

		public AutoRefresh() {
		}

		@Override
		public void run() {
			runOnUiThread( new Runnable() {

				@Override
				public void run() {
					refresh();
				}

			} );
		}

	}

	public static final String [] SUPPORTED_IMTO_HOSTS = {
		"silentcircle",
		"silent text",
		"silenttext",
		"silentcircle.com",
		"com.silentcircle",
		"silent circle"
	};

	private static final int R_id_share = 0xFFFF & R.id.share;

	private static final SimpleDateFormat ISO8601 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );

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
						if( SilentTextApplication.isResendRequest( message ) ) {
							events.remove( i );
							i--;
						}
						break;
					default:
						break;
				}
			}
		}
		return events;
	}

	protected static int getDelayForLevel( int level ) {
		return BurnDelay.Defaults.getDelay( level );
	}

	protected static int getLevelForDelay( int delay ) {
		return BurnDelay.Defaults.getLevel( delay );
	}

	private static String parseMessageTextFrom( Event event ) {
		if( !( event instanceof Message ) ) {
			return null;
		}
		try {
			return new SirenObject( event.getText() ).getString( "message" );
		} catch( JSONException exception ) {
			return null;
		}
	}

	protected String partner;
	private BroadcastReceiver viewUpdater;
	protected Conversation conversation;
	private Timer timer;
	protected boolean empty;
	protected AsyncTask<Void, Void, Void> toggleVerifiedTask;
	protected AsyncTask<Void, Void, Void> resetSecureContextTask;
	protected AsyncTask<Void, Void, Void> toggleBurnNoticeTask;
	protected AsyncTask<Void, Void, Void> toggleLocationSharingTask;

	private File imageCaptureFile;
	private File videoCaptureFile;

	static {
		ISO8601.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	protected void applyConversationSettings( JSONObject json ) throws JSONException {

		Conversation conversation = getConversation();

		if( conversation == null ) {
			return;
		}

		if( conversation.hasBurnNotice() ) {
			json.put( "shred_after", conversation.getBurnDelay() );
		}

		if( conversation.isLocationEnabled() ) {

			Location location = getSilentTextApplication().getLocation();

			if( location != null ) {

				JSONObject jsonLocation = new JSONObject();

				jsonLocation.put( "latitude", location.getLatitude() );
				jsonLocation.put( "longitude", location.getLongitude() );
				jsonLocation.put( "timestamp", location.getTime() );
				jsonLocation.put( "altitude", location.getAltitude() );
				jsonLocation.put( "horizontalAccuracy", location.getAccuracy() );
				jsonLocation.put( "verticalAccuracy", location.getAccuracy() );

				json.put( "location", jsonLocation.toString() );

			}

		}

	}

	protected void applyConversationSettings( OutgoingMessage message ) {

		message.setConversationID( getPartner() );

		if( conversation != null && conversation.hasBurnNotice() ) {
			message.setBurnNotice( conversation.getBurnDelay() );
		}

	}

	protected void backToConversationList() {
		Intent intent = new Intent( this, ConversationListActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
		startActivity( intent );
	}

	protected void callUser( String remoteUserID ) {
		Intent intent = createIntentToCallUser( remoteUserID );
		startActivity( intent );
	}

	protected void cancelAutoRefresh() {
		if( timer != null ) {
			timer.cancel();
			timer = null;
		}
	}

	protected void clearConversationLog() {
		getConversations().historyOf( conversation ).clear();
		EventAdapter adapter = getAdapter( R.id.messages );
		adapter.setItems( filter( getConversations().historyOf( conversation ).list() ) );
		adapter.notifyDataSetChanged();
		empty = true;
		updateOptionsDrawer();
	}

	private Intent createCaptureImageIntent() {
		imageCaptureFile = createTempFile( ".jpg" );
		if( imageCaptureFile == null ) {
			return null;
		}
		Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
		intent.putExtra( MediaStore.EXTRA_OUTPUT, Uri.fromFile( imageCaptureFile ) );
		return intent;
	}

	private Intent createCaptureVideoIntent() {
		videoCaptureFile = createTempFile( ".mp4" );
		if( videoCaptureFile == null ) {
			return null;
		}
		Intent intent = new Intent( MediaStore.ACTION_VIDEO_CAPTURE );
		intent.putExtra( MediaStore.EXTRA_OUTPUT, Uri.fromFile( videoCaptureFile ) );
		return intent;
	}

	protected String createDeliveryNotificationText( String messageID ) throws JSONException {
		JSONObject receipt = new JSONObject();
		receipt.put( "received_id", messageID );
		receipt.put( "received_time", ISO8601.format( new Date() ) );
		String receiptText = receipt.toString();
		return receiptText;
	}

	protected void createOptionsDrawer() {
		getOptionsDrawer().attach( this, partner );
	}

	private OutgoingMessage createOutgoingMessage() {

		JSONObject json = new JSONObject();

		try {
			applyConversationSettings( json );
		} catch( JSONException exception ) {
			log.error( exception, "While trying to create outgoing message" );
		}

		OutgoingMessage message = new OutgoingMessage( getUsername(), json.toString() );

		applyConversationSettings( message );

		return message;

	}

	protected SendMessageOnClick createSendMessageOnClickListener( Conversation conversation ) {

		TextView composeText = (TextView) findViewById( R.id.compose_text );

		return new SendMessageOnClick( composeText, getUsername(), conversation, getConversations(), getNative(), shouldRequestDeliveryNotification() ) {

			@Override
			protected Location getLocation() {
				if( conversation != null && conversation.isLocationEnabled() ) {
					return getSilentTextApplication().getLocation();
				}
				return super.getLocation();
			}

			@Override
			public void onClick( View button ) {
				if( source.getText().length() <= 0 ) {
					selectFile();
				} else {
					super.onClick( button );
				}
			}

			@Override
			protected void withMessage( Message message ) {
				updateViews( message );
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

	private File createTempFile( String extension ) {
		File parent = getTempDir();
		if( parent == null ) {
			return null;
		}
		parent.mkdirs();
		return new File( parent, "CAPTURE_" + Long.toHexString( System.currentTimeMillis() ) + extension );
	}

	private void destroyResetSecureContextTask() {
		if( resetSecureContextTask != null ) {
			resetSecureContextTask.cancel( true );
			resetSecureContextTask = null;
		}
	}

	private void destroyToggleBurnNoticeTask() {
		if( toggleBurnNoticeTask != null ) {
			toggleBurnNoticeTask.cancel( true );
			toggleBurnNoticeTask = null;
		}
	}

	private void destroyToggleLocationSharingTask() {
		if( toggleLocationSharingTask != null ) {
			toggleLocationSharingTask.cancel( true );
			toggleLocationSharingTask = null;
		}
	}

	private void destroyToggleVerifiedTask() {
		if( toggleVerifiedTask != null ) {
			toggleVerifiedTask.cancel( true );
			toggleVerifiedTask = null;
		}
	}

	private Conversation getConversation() {
		if( conversation == null ) {
			conversation = getConversation( getPartner() );
		}
		return conversation;
	}

	protected String getLabelForLevel( int level ) {
		return BurnDelay.Defaults.getLabel( this, level );
	}

	public Location getLocation() {
		return getSilentTextApplication().getLocation();
	}

	public Location getLocationOnlyIfListening() {
		return getSilentTextApplication().isListeningForLocationUpdates() ? getLocation() : null;
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

	private String getPartner() {
		if( partner == null ) {
			Intent intent = getIntent();
			if( intent != null ) {
				partner = Extra.PARTNER.from( intent );
			}
		}
		if( conversation == null ) {
			return partner;
		}
		return conversation.getPartner().getUsername();
	}

	private String getSubtitle() {
		if( isTalkingToSelf() ) {
			return getSubtitle( true, true );
		}
		ResourceStateRepository states = getConversations().contextOf( conversation );
		ResourceState state = states.findById( conversation.getPartner().getDevice() );
		if( state == null || !state.isSecure() ) {
			return getSubtitle( false, false );
		}
		return getSubtitle( true, states.isVerified( state ) );
	}

	private String getSubtitle( boolean secure, boolean verified ) {
		String secureString = getString( secure ? R.string.secure : R.string.securing );
		if( !secure ) {
			return secureString;
		}
		String verifiedString = getString( verified ? R.string.verified : R.string.unverified );
		return String.format( "%s, %s", secureString, verifiedString );
	}

	protected boolean isTalkingToSelf() {
		String self = getUsername();
		return self != null && self.equals( conversation.getPartner().getUsername() );
	}

	@Override
	protected void lockContentView() {
		super.lockContentView();
		setTitle( "" );
		getSupportActionBar().setSubtitle( "" );
		getSupportActionBar().setIcon( R.drawable.ic_launcher );
	}

	@Override
	public void onActionPerformed() {
		refresh();
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent intent ) {
		switch( requestCode ) {
			case R_id_share:
				switch( resultCode ) {
					case RESULT_OK:

						if( intent != null ) {
							sendFile( intent );
							return;
						}

						if( imageCaptureFile != null && imageCaptureFile.exists() ) {
							Intent captured = new Intent();
							captured.setDataAndType( Uri.fromFile( imageCaptureFile ), AttachmentUtils.getMIMEType( imageCaptureFile ) );
							sendFile( captured );
						} else if( videoCaptureFile != null && videoCaptureFile.exists() ) {
							Intent captured = new Intent();
							captured.setDataAndType( Uri.fromFile( videoCaptureFile ), AttachmentUtils.getMIMEType( videoCaptureFile ) );
							sendFile( captured );
						}

						return;

					default:
						break;
				}
				break;
			default:
				break;
		}
		super.onActivityResult( requestCode, resultCode, intent );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_conversation_with_drawer );
		hideSoftKeyboardOnDrawerToggle();
		findViewById( R.id.upload ).setVisibility( View.GONE );

		if( savedInstanceState != null ) {

			String imageCaptureFileName = savedInstanceState.getString( "imageCaptureFile" );
			if( imageCaptureFileName != null ) {
				imageCaptureFile = new File( imageCaptureFileName );
			} else {
				imageCaptureFile = null;
			}

			String videoCaptureFileName = savedInstanceState.getString( "videoCaptureFile" );
			if( videoCaptureFileName != null ) {
				videoCaptureFile = new File( videoCaptureFileName );
			} else {
				videoCaptureFile = null;
			}

		}

		Intent intent = getIntent();

		if( intent == null ) {
			finish();
			return;
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
		ListView listView = findListViewById( R.id.messages );
		EventAdapter adapter = new EventAdapter();
		listView.setAdapter( adapter );
		listView.setOnItemClickListener( new ClickthroughWhenNotInChoiceMode() );

		listView.setMultiChoiceModeListener( new MultipleChoiceSelector<Event>( adapter, R.menu.multiselect_event, this ) {

			@Override
			public boolean onCreateActionMode( ActionMode mode, Menu menu ) {
				findViewById( R.id.compose ).setVisibility( View.INVISIBLE );
				hideSoftKeyboard( R.id.compose_text );
				return super.onCreateActionMode( mode, menu );
			}

			@Override
			public void onDestroyActionMode( ActionMode mode ) {
				findViewById( R.id.compose ).setVisibility( View.VISIBLE );
				super.onDestroyActionMode( mode );
			}

			@Override
			public void onItemCheckedStateChanged( ActionMode mode, int position, long itemId, boolean checked ) {
				super.onItemCheckedStateChanged( mode, position, itemId, checked );
				mode.invalidate();
			}

			@Override
			public boolean onPrepareActionMode( ActionMode mode, Menu menu ) {
				MenuItem item = menu.findItem( R.id.copy );
				boolean before = item.isVisible();
				item.setVisible( !findListViewById( R.id.messages ).hasMultipleCheckedItems() );
				boolean after = item.isVisible();
				return super.onPrepareActionMode( mode, menu ) || before != after;
			}

		} );

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
				( (ImageView) findViewById( R.id.compose_send ) ).setImageResource( s.length() > 0 ? R.drawable.ic_action_send : R.drawable.ic_action_camera );
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

		getSupportMenuInflater().inflate( R.menu.conversation, menu );

		if( conversation != null ) {

			MenuItem burnNotice = menu.findItem( R.id.action_burn_notice );
			burnNotice.setIcon( conversation.hasBurnNotice() ? R.drawable.ic_burn_selected : R.drawable.ic_burn_unselected );

			MenuItem shareLocation = menu.findItem( R.id.action_share_location );
			shareLocation.setIcon( conversation.isLocationEnabled() ? R.drawable.ic_location_selected : R.drawable.ic_location_unselected );

		}

		return super.onCreateOptionsMenu( menu );

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyToggleVerifiedTask();
		destroyToggleBurnNoticeTask();
		destroyToggleLocationSharingTask();
		destroyResetSecureContextTask();
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
		createOptionsDrawer();
		if( shouldAbort() ) {
			return;
		}
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
		getSilentTextApplication().stopListeningForLocationUpdates();
	}

	@Override
	protected void onRestoreInstanceState( Bundle savedInstanceState ) {
		super.onRestoreInstanceState( savedInstanceState );
		( (ComposeText) findViewById( R.id.compose_text ) ).restore( savedInstanceState );
	}

	@Override
	protected void onResume() {

		super.onResume();

		if( shouldAbort() ) {
			return;
		}

		unlockContentView();
		registerViewUpdater();
		refresh();
		IOUtils.delete( getCacheStagingDir() );

	}

	@Override
	protected void onSaveInstanceState( Bundle outState ) {
		if( imageCaptureFile != null ) {
			outState.putString( "imageCaptureFile", imageCaptureFile.getAbsolutePath() );
		}
		if( videoCaptureFile != null ) {
			outState.putString( "videoCaptureFile", videoCaptureFile.getAbsolutePath() );
		}
		( (ComposeText) findViewById( R.id.compose_text ) ).save( outState );
	}

	protected void onSoftKeyboardChanged( boolean enabled ) {
		if( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
			if( enabled ) {
				getSupportActionBar().hide();
			} else {
				getSupportActionBar().show();
			}
		}
	}

	protected void performAction( int actionID, Event event ) {

		switch( actionID ) {

			case R.id.burn:
				if( event instanceof OutgoingMessage ) {
					OutgoingMessage message = (OutgoingMessage) event;
					if( !isTalkingToSelf() && MessageState.SENT.compareTo( message.getState() ) <= 0 ) {
						message.setState( MessageState.BURNED );
						getConversations().historyOf( conversation ).save( event );
						transition( conversation.getPartner().getUsername(), event.getId() );
						JabberClient jabber = getJabber();
						if( jabber != null ) {
							jabber.removeOfflineMessage( conversation.getPartner().getUsername(), event.getId() );
						}
						return;
					}
				}
				getConversations().historyOf( conversation ).remove( event );
				break;

			case R.id.copy:
				ClipboardUtils.copy( this, parseMessageTextFrom( event ) );
				break;

			case R.id.action_resend:
				if( event instanceof OutgoingMessage ) {
					OutgoingMessage message = (OutgoingMessage) event;
					if( !isTalkingToSelf() ) {
						message.setState( MessageState.COMPOSED );
						getConversations().historyOf( conversation ).save( event );
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
	public void performAction( int menuActionId, int position ) {
		performAction( menuActionId, (Event) getAdapter( R.id.messages ).getItem( position ) );
	}

	protected void refresh() {

		conversation = getOrCreateConversation( partner );

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

		EventAdapter adapter = getAdapter( R.id.messages );
		adapter.setItems( filter( events ) );
		adapter.setOnItemRemovedListener( new OnItemRemovedListener() {

			@Override
			public void onItemRemoved( Object item ) {
				if( item instanceof Event ) {
					performAction( R.id.burn, (Event) item );
				}
			}

		} );
		adapter.notifyDataSetChanged();

		findViewById( R.id.compose_send ).setOnClickListener( createSendMessageOnClickListener( conversation ) );

		if( conversation.isLocationEnabled() && !getSilentTextApplication().isListeningForLocationUpdates() ) {
			getSilentTextApplication().startListeningForLocationUpdates();
		}

		empty = events.isEmpty();

		updateActionBar();
		updateOptionsDrawer();
		_invalidateOptionsMenu();

	}

	private void registerViewUpdater() {

		viewUpdater = new BroadcastReceiver() {

			@Override
			public void onReceive( Context context, Intent intent ) {
				String p = intent.getStringExtra( Extra.PARTNER.getName() );
				if( p != null && p.equals( partner ) ) {
					switch( Action.from( intent ) ) {
						case UPDATE_CONVERSATION:
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

	protected void resetSecureContext() {

		if( isTalkingToSelf() ) {
			return;
		}

		destroyResetSecureContextTask();

		resetSecureContextTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground( Void... ignore ) {
				getNative().connect( conversation.getPartner().getUsername() );
				return null;
			}

			@Override
			protected void onPostExecute( Void result ) {
				toast( R.string.refreshing_keys );
				refresh();
			}

		};

		resetSecureContextTask.execute();

	}

	protected void save( OutgoingMessage message ) {
		getConversations().historyOf( conversation ).save( message );
	}

	protected void scheduleAutoRefresh( long autoRefreshTime ) {
		cancelAutoRefresh();
		timer = new Timer();
		timer.schedule( new AutoRefresh(), new Date( autoRefreshTime ) );
	}

	protected void selectFile() {
		selectFileOfType( "*/*" );
	}

	private void selectFileOfType( String type ) {

		ChooserBuilder chooser = new ChooserBuilder( this );

		chooser.label( R.string.share_from );

		chooser.intent( createCaptureImageIntent() );
		chooser.intent( createCaptureVideoIntent() );
		chooser.intent( createSelectFileIntent( type ) );

		startActivityForResult( chooser.build(), R_id_share );

	}

	protected void selectPhoto() {
		selectFileOfType( "image/*" );
	}

	protected void sendDeliveryNotification( String messageID ) {
		try {
			getNative().encrypt( conversation.getPartner().getUsername(), null, createDeliveryNotificationText( messageID ), false, false );
		} catch( JSONException exception ) {
			getLog().warn( exception, "#sendDeliveryNotification" );
		}
	}

	private void sendFile( Intent intent ) {

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

		long size = AttachmentUtils.getFileSize( this, uri );

		if( size <= 0 ) {
			return;
		}

		if( size >= AttachmentUtils.FILE_SIZE_LIMIT ) {
			AttachmentUtils.showFileSizeErrorDialog( this );
			return;
		}

		OutgoingMessage message = createOutgoingMessage();
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

	protected void setProgress( int labelResourceID, int progress, Intent cancelIntent ) {
		( (UploadView) findViewById( R.id.upload ) ).setProgress( labelResourceID, progress, cancelIntent );
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

	protected void toggleBurnNotice() {

		destroyToggleBurnNoticeTask();

		toggleBurnNoticeTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground( Void... ignore ) {
				conversation.setBurnNotice( !conversation.hasBurnNotice() );
				if( conversation.hasBurnNotice() && conversation.getBurnDelay() <= 0 ) {
					conversation.setBurnDelay( 60 );
				}
				save( conversation );
				return null;
			}

			@Override
			protected void onPostExecute( Void result ) {
				_invalidateOptionsMenu();
				updateOptionsDrawer();
			}

		};
		toggleBurnNoticeTask.execute();
	}

	protected void toggleLocationSharing() {

		destroyToggleLocationSharingTask();

		toggleLocationSharingTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground( Void... params ) {
				conversation.setLocationEnabled( !conversation.isLocationEnabled() );
				save( conversation );
				return null;
			}

			@Override
			protected void onPostExecute( Void result ) {
				_invalidateOptionsMenu();
				updateOptionsDrawer();
			}

		};

		toggleLocationSharingTask.execute();

	}

	protected void toggleVerified() {
		if( isTalkingToSelf() ) {
			return;
		}
		destroyToggleVerifiedTask();
		toggleVerifiedTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground( Void... ignore ) {
				ResourceStateRepository states = getConversations().contextOf( conversation );
				ResourceState state = states.findById( conversation.getPartner().getDevice() );
				states.toggleVerified( state );
				return null;
			}

			@Override
			protected void onPostExecute( Void result ) {
				_invalidateOptionsMenu();
				updateActionBar();
			}

		};
		toggleVerifiedTask.execute();
	}

	protected void updateActionBar() {

		if( conversation == null ) {
			return;
		}

		conversation.getPartner().setAlias( getContacts().getDisplayName( conversation.getPartner().getUsername() ) );
		setTitle( conversation.getPartner().getAlias() );
		getSupportActionBar().setSubtitle( getSubtitle() );

		Bitmap avatar = BitmapFactory.decodeStream( getContacts().getAvatar( conversation.getPartner().getUsername() ) );

		if( avatar == null ) {
			getSupportActionBar().setIcon( R.drawable.ic_avatar_placeholder );
		} else {
			getSupportActionBar().setIcon( new BitmapDrawable( getResources(), avatar ) );
		}

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

	protected void updateViews( Message message ) {
		EventAdapter adapter = getAdapter( R.id.messages );
		adapter.update( message );
		adapter.notifyDataSetChanged();
		empty = false;
		updateOptionsDrawer();
	}

}
