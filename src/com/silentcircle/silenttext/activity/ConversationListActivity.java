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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ProgressBar;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.Toast;

import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.fragment.ChatsFragment;
import com.silentcircle.silenttext.fragment.DirectorySearchListFragment;
import com.silentcircle.silenttext.listener.OnHomePressedListener;
import com.silentcircle.silenttext.listener.OnObjectReceiveListener;
import com.silentcircle.silenttext.loader.ContactUser;
import com.silentcircle.silenttext.loader.ScContactsLoader;
import com.silentcircle.silenttext.loader.ScDirectoryLoader;
import com.silentcircle.silenttext.loader.ScDirectoryLoader.UserData;
import com.silentcircle.silenttext.model.Server;
import com.silentcircle.silenttext.provider.ContactProvider;
import com.silentcircle.silenttext.receiver.NotificationBroadcaster;
import com.silentcircle.silenttext.service.OrgNameService;
import com.silentcircle.silenttext.service.PassphraseIntentService;
import com.silentcircle.silenttext.service.RefreshSelfIntentService;
import com.silentcircle.silenttext.util.AsyncUtils;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.SearchView;
import com.silentcircle.silenttext.view.adapter.ContactSuggestionAdapter;

public class ConversationListActivity extends SilentActivity implements OnClickListener, LoaderCallbacks<Cursor>, OnQueryTextListener, OnHomePressedListener {

	private class AutoRefresh extends TimerTask {

		public AutoRefresh() {
		}

		@Override
		public void run() {
			runOnUiThread( new Refresher() );
		}

	}

	class DeactivationListener extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {

			Action action = Action.from( intent );

			if( Action.BEGIN_DEACTIVATE.equals( action ) ) {
				getActionBar().hide();
				beginLoading( R.id.chats );
				deactivating = true;
			} else {
				deactivating = false;
				onDeactivated();
				unregisterReceiver( this );
				deactivationListener = null;
			}

		}

	}

	class Refresh extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			refresh();
		}

	}

	class Refresher implements Runnable {

		@Override
		public void run() {
			refresh();
		}

	}

	class TitleUpdater extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			updateTitle();
		}

	}

	protected boolean deactivating;
	protected static final int R_id_search = 0xFFFF & R.id.action_search;

	private BroadcastReceiver viewUpdater;
	private Menu actionMenu;
	private BroadcastReceiver titleUpdater;
	protected BroadcastReceiver deactivationListener;
	protected SearchView search;
	protected CursorAdapter searchSuggestions;

	protected Timer timer;

	protected boolean intendToComposeNewMessage;
	protected boolean updatingTitle;
	// Directory Search
	public static final String KEY_IS_DIRECTORY_SEARCH_FRAGMENT = "is_directory_search_fragment";
	public static final String KEY_IS_LAUNCH_SPA = "is_launch_spa";
	private String mCurrentSearch;
	private CheckBox mSCDirectoryCheckBox, mOrgnizationCheckBox;
	ProgressBar mProgressBar;
	DirectorySearchListFragment mDirectorySearchListFragment;

	List<UserData> mDirectorySearchList = new ArrayList<UserData>();
	List<ContactUser> mScContactsList = new ArrayList<ContactUser>();

	Loader<Cursor> mLoader;

	Loader<Cursor> mContactsLoader;

	private ScContactsLoader mSCLoader;

	private final Handler mScHandler = new Handler() {

		@Override
		public void handleMessage( Message msg ) {
			if( msg.getData().getInt( ScDirectoryLoader.DIRECTORY_SEARCH_WHAT ) == ScDirectoryLoader.NO_ORGANIZATION ) {
				String error = msg.getData().getString( ScDirectoryLoader.DIRECTORY_SEARCH_ERROR_MESSAGE );
				Toast toast = Toast.makeText( getActivity(), error, Toast.LENGTH_LONG );
				toast.setGravity( Gravity.CENTER, 0, 0 );
				toast.show();
			}
		}
	};

	// This mOrgNameRunnable is originally used for find orgName for directory purpose.
	// It's now used for find Account Expiration date too.
	Runnable mOrgNameRunnable = new Runnable() {

		@Override
		public void run() {
			Intent intent = new Intent( ConversationListActivity.this, OrgNameService.class );
			startService( intent );
		}
	};

	boolean mQueryTextSubmit;

	public static final String SCDialog_TAG = "directory_search_dialog";

	private boolean mDontChangeQuery;

	private com.silentcircle.silenttext.receiver.HomeWatcher mHomeWatcher;

	private final Runnable userNameRunnable = new Runnable() {

		@Override
		public void run() {
			Intent debugLogIn = new Intent( ConversationListActivity.this, PassphraseIntentService.class );
			startService( debugLogIn );
		}
	};

	private final Handler mDelayedHandler = new Handler() {

		@Override
		public void handleMessage( Message msg ) {
			String query = msg.getData().getString( SEARCH_QUERY_KEY );
			boolean isResetStart = msg.getData().getBoolean( SEARCH_QUERY_RESET_START, false );
			Bundle args = new Bundle();
			Extra.TEXT.to( args, query );
			if( isResetStart ) {
				ScDirectoryLoader.setStart( 0 );
			}
			ScDirectoryLoader.setSearchQuery( query );
			ConversationListActivity.this.getLoaderManager().restartLoader( R.id.directory_search, args, ConversationListActivity.this ).forceLoad();

		}
	};

	private static final String SEARCH_QUERY_KEY = "search_query_key";
	private static final String SEARCH_QUERY_RESET_START = "search_query_reset_start";

	private static final int SEARCH_QUERY_WHAT = 113;

	protected void assertPermissionToView() {
		assertPermissionToView( this, true, true, true );
	}

	private void attachNewMessageAction() {
		View v = findViewById( R.id.action_new_message );

		if( v != null ) {

			v.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick( View v ) {
					intendToComposeNewMessage = true;
					invalidateSupportOptionsMenu();

					if( Constants.mIsDirectorySearchEnabled && Constants.mIsShowDirectorySerachCheckBox ) {
						setVisibleIf( true, R.id.actions, R.id.directory_cb_layout_id );
					}
				}

			} );

		}

	}

	protected void cancelAutoRefresh() {
		if( timer != null ) {
			timer.cancel();
			timer = null;
		}
	}

	public void directorySearch( String query, boolean isResetStart ) {

		Bundle args = new Bundle();
		Extra.TEXT.to( args, query );
		if( isResetStart ) {
			ScDirectoryLoader.setStart( 0 );
		}
		ScDirectoryLoader.setSearchQuery( query );
		// getLoaderManager().restartLoader( R.id.directory_search, args, this ).forceLoad();

		if( mLoader != null && mLoader.isReset() ) {
			getLoaderManager().restartLoader( R.id.directory_search, args, this ).forceLoad();
		} else {
			getLoaderManager().initLoader( R.id.directory_search, args, this ).forceLoad();
		}

		return;
	}

	public void directorySearchDelayed( String query, boolean isResetStart ) {
		mDelayedHandler.removeMessages( SEARCH_QUERY_WHAT );
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString( SEARCH_QUERY_KEY, query );
		b.putBoolean( SEARCH_QUERY_RESET_START, isResetStart );
		msg.setData( b );
		msg.what = SEARCH_QUERY_WHAT;
		mDelayedHandler.sendMessageDelayed( msg, 1000 );
	}

	protected void dismissSearch() {

		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				onSearchDismissed();
			}

		} );

	}

	protected ChatsFragment getChatsFragment() {
		if( Constants.mIsDirectorySearchEnabled ) {
			FragmentManager fm = getFragmentManager();
			ChatsFragment fragment = null;
			if( fm.findFragmentById( R.id.chats ) != null ) {
				if( fm.findFragmentById( R.id.chats ) instanceof ChatsFragment ) {
					fragment = (ChatsFragment) fm.findFragmentById( R.id.chats );
				}
			} else {
				fragment = new ChatsFragment();
				fm.beginTransaction().replace( R.id.chats, fragment ).addToBackStack( ChatsFragment.TAG ).commit();
			}
			return fragment;
		}

		// original implemented by Devin without DirectorySearchListFragment.
		FragmentManager fm = getFragmentManager();
		ChatsFragment fragment = (ChatsFragment) fm.findFragmentById( R.id.chats );

		if( fragment == null ) {
			fragment = new ChatsFragment();
			fm.beginTransaction().add( R.id.chats, fragment ).commit();
		}

		return fragment;
	}

	public String getCurrentSearchQuery() {
		return mCurrentSearch;
	}

	protected String getSearchQuery() {
		if( search == null ) {
			return null;
		}
		if( search.getQuery() == null ) {
			return null;
		}
		return search.getQuery().toString();
	}

	protected void handleIntent( Intent intent ) {
		if( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
			String query = intent.getStringExtra( SearchManager.QUERY );
			handleSearch( query );
		} else {
			dismissSearch();
		}
	}

	protected void handleSearch( String query ) {
		Server server = getServer( "xmpp" );
		String domain = server != null ? server.getServiceName() : null;
		domain = domain != null ? domain.replaceAll( "@", "" ) : null;

		if( domain == null ) {
			toast( R.string.username_unavailable, query );
			return;
		}

		final String username = ( query.contains( "@" ) ? query : String.format( "%s@%s", query.trim(), domain ) ).toLowerCase( Locale.ENGLISH );
		final String displayUsername = username.contains( domain ) ? username.replaceAll( "@.+$", "" ) : username;

		hideSoftKeyboard( search );
		beginLoading( R.id.chats );

		isAccessible( username, new OnObjectReceiveListener<Boolean>() {

			@Override
			public void onObjectReceived( Boolean accessible ) {
				finishLoading( R.id.chats );
				if( accessible.booleanValue() ) {
					// if DirectorySearchListFragment is shown before added Favorite Contact
					// (clicked on Favorite in directory search section. after come back from SPA,
					// DirectorySearchListFragment needs to be shown again.
					if( getFragmentManager().findFragmentById( R.id.chats ) instanceof DirectorySearchListFragment && !mQueryTextSubmit || !mQueryTextSubmit && !Constants.mIsMessageClicked ) {
						return;
					}
					mQueryTextSubmit = false;
					dismissSearch();
					launchConversationActivity( username );
					// Rong: moved the following code to launchConverstionActivity(),
					// in order to be shared by called from SPA app -- onResume().
					// Intent intent = new Intent( getBaseContext(), ConversationActivity.class );
					// intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
					// intent.putExtra( Extra.PARTNER.getName(), username );
					// startActivityForResult( intent, R_id_search );
				} else {
					// if DirectorySearchListFragment is shown, Toast with unrelated message should
					// not be shown
					if( Constants.mIsDirectorySearchEnabled ) {
						if( getFragmentManager().findFragmentById( R.id.chats ) instanceof DirectorySearchListFragment ) {
							return;
						}
					}
					if( !isActivated() ) {
						toast( R.string.error_invalid_api_key );
					} else {
						toast( R.string.username_unavailable, displayUsername );
					}
				}
			}

		} );

	}

	protected void inflate( Menu menu ) {

		actionMenu = menu;
		getMenuInflater().inflate( R.menu.conversation_list, menu );

		search = (SearchView) menu.findItem( R.id.action_search ).getActionView();

		// Rong: this list does not meet requirement. move into if{} block.
		// search.setSuggestionsAdapter( searchSuggestions );
		search.setQueryHint( getString( R.string.search_hint ) );
		search.setSearchBackgroundResource( R.drawable.bg_text_input );
		search.setOnQueryTextListener( this );
		if( !Constants.mIsDirectorySearchEnabled ) {
			search.setSuggestionsAdapter( searchSuggestions );
			search.setOnSuggestionListener( new OnSuggestionListener() {

				@Override
				public boolean onSuggestionClick( int position ) {
					Cursor cursor = searchSuggestions.getCursor();
					cursor.moveToPosition( position );
					String username = ContactProvider.getUsername( cursor );
					getLoaderManager().destroyLoader( R.id.action_search );
					handleSearch( username );
					return true;
				}

				@Override
				public boolean onSuggestionSelect( int position ) {
					// Do nothing.
					return false;
				}
			} );
		}

	}

	public boolean isSPARunnung() {
		ActivityManager activityManager = (ActivityManager) getSystemService( ACTIVITY_SERVICE );
		List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
		for( int i = 0; i < procInfos.size(); i++ ) {
			if( procInfos.get( i ).processName.equals( Constants.SPA_PACKAGE_NAME ) ) {
				if( procInfos.get( i ).importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND || procInfos.get( i ).importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE ) {
					return true;
				} else if( procInfos.get( i ).importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND && procInfos.get( i ).lru == Constants.BG_IMPORTANCE_LIVE ) {
					return true;
				}
			}
		}
		return false;
	}

	public void launchConversationActivity( String username ) {
		Intent intent = new Intent( getBaseContext(), ConversationActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
		intent.putExtra( Extra.PARTNER.getName(), username );
		startActivityForResult( intent, R.id.directory_search_request_code );
		finish();
	}

	public void loadingData() {
		directorySearch( mCurrentSearch, false );
	}

	@Override
	protected void lockContentView() {
		super.lockContentView();
		getActionBar().setTitle( "" );
		getActionBar().setSubtitle( null );
	}

	@Override
	public void onActionModeFinished( ActionMode mode ) {
		super.onActionModeFinished( mode );
		setVisibleIf( true, R.id.actions );
	}

	@Override
	public void onActionModeStarted( ActionMode mode ) {
		super.onActionModeStarted( mode );
		setVisibleIf( true, R.id.actions );
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent intent ) {
		switch( requestCode ) {
			case R_id_search:
				dismissSearch();
				break;
			case R.id.directory_search_request_code:
				// TODO: The following code is only for directory search.
				if( Constants.mIsDirectorySearchEnabled ) {
					if( Constants.mIsMessageClicked ) {
						// STA-832: result back from ConversationActivity
						Constants.mIsMessageClicked = false;
					} else {
						// SPA-454: result back from ConversationActivity which was called by SPA
						if( Constants.OnConversationListActivityCreateCalled == 0 ) {
							Constants.OnConversationListActivityCreateCalled++;
						} else {
							Constants.OnConversationListActivityCreateCalled++;
							finish();
						}
					}
				}
		}
		super.onActivityResult( requestCode, resultCode, intent );
	}

	@Override
	public void onBackPressed() {
		if( Constants.mIsDirectorySearchEnabled ) {
			int count = getFragmentManager().getBackStackEntryCount();
			if( count > 0 && getFragmentManager().getBackStackEntryAt( count - 1 ).getName().equals( DirectorySearchListFragment.TAG ) ) {
				getFragmentManager().popBackStack();
			}
			if( count > 0 && getFragmentManager().getBackStackEntryAt( count - 1 ).getName().equals( ChatsFragment.TAG ) ) {
				finish();
			}
		}
		super.onBackPressed();
	}

	@Override
	public void onClick( View v ) {
		if( v.getId() == R.id.sc_directory_cb_id ) {
			if( mSCDirectoryCheckBox.isChecked() ) {
				// mIsDirectorySearch = true;
				// getLoaderManager().destroyLoader( R.id.action_search );
				// directorySearch( mCurrentSearch );
			}
			// if( !useScDirectory( mSCDirectoryCheckBox.isChecked() ) ) {
			// return;
			// }
		}
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_conversations );

		// STA-832: for directory search only.
		mProgressBar = (ProgressBar) findViewById( R.id.progressBar_id );
		mSCDirectoryCheckBox = (CheckBox) findViewById( R.id.sc_directory_cb_id );
		mSCDirectoryCheckBox.setOnClickListener( this );
		mOrgnizationCheckBox = (CheckBox) findViewById( R.id.orgnization_directory_cb_id );
		mOrgnizationCheckBox.setOnClickListener( this );
		if( Constants.mIsDirectorySearchEnabled && Constants.mIsShowDirectorySerachCheckBox ) {
			mSCDirectoryCheckBox.setVisibility( View.VISIBLE );
			mOrgnizationCheckBox.setVisibility( View.VISIBLE );
		}

		if( !Constants.mIsDirectorySearchEnabled ) {
			searchSuggestions = new ContactSuggestionAdapter( this, null, 0 );
		}

		handleIntent( getIntent() );

		attachNewMessageAction();

		// listening Home button clicked.
		mHomeWatcher = new com.silentcircle.silenttext.receiver.HomeWatcher( this );
		mHomeWatcher.setOnHomePressedListener( this );
		mHomeWatcher.startWatch();

		SilentTextApplication.from( this ).getDeletedUsers();

		// the following block of code will not be executed if and only if the passCode be set.
		SilentTextApplication app = SilentTextApplication.from( this );
		SharedPreferences prefs = app.getSharedPreferences( LockActivity.PASS_CODE_SET, Context.MODE_PRIVATE );
		if( !prefs.getBoolean( LockActivity.PASS_CODE_SET, false ) ) {
			// Check username, once get valid username, it starts ConversationListActivity again
			// from UnlockActivity to get rid of "Passphrase" dialog
			new Handler().postDelayed( userNameRunnable, 1000 );
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader( int id, Bundle arguments ) {
		if( Constants.mIsDirectorySearchEnabled ) {
			String query = Extra.TEXT.from( arguments );
			if( id == R.id.directory_search ) {
				mLoader = new ScDirectoryLoader( this, query, mScHandler, mOrgnizationCheckBox.isChecked() );
				ScDirectoryLoader.setStart( 0 );
				return mLoader;
			}
			return null;
		}

		String query = Extra.TEXT.from( arguments );
		CursorLoader cursor = ContactProvider.loaderForSearch( this, query );
		return cursor;
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {

		inflate( menu );

		menu.findItem( R.id.action_lock ).setVisible( !OptionsDrawer.isEmptyPasscode( this ) );

		MenuItem searchMenuItem = menu.findItem( R.id.action_search );

		if( intendToComposeNewMessage ) {
			intendToComposeNewMessage = false;
			searchMenuItem.expandActionView();
		}

		searchMenuItem.setOnActionExpandListener( new OnActionExpandListener() {

			@SuppressLint( "NewApi" )
			@Override
			public boolean onMenuItemActionCollapse( MenuItem item ) {
				item.setVisible( false );
				setVisibleIf( true, R.id.actions );

				if( Constants.mIsDirectorySearchEnabled ) {
					setVisibleIf( false, R.id.directory_cb_layout_id );
					if( getFragmentManager().getBackStackEntryCount() > 0 ) {
						if( Build.VERSION.SDK_INT >= 17 ) {
							if( !ConversationListActivity.this.isDestroyed() || !ConversationListActivity.this.isFinishing() ) {
								getFragmentManager().popBackStack();
							}
						} else {
							if( !ConversationListActivity.this.isFinishing() ) {
								getFragmentManager().popBackStack();
							}
						}
						if( mDirectorySearchListFragment != null ) {
							mDirectorySearchListFragment = null;
						}
					}
				}

				return true;
			}

			@Override
			public boolean onMenuItemActionExpand( MenuItem item ) {
				item.setVisible( true );
				setVisibleIf( false, R.id.actions );
				return true;
			}

		} );

		searchMenuItem.setVisible( searchMenuItem.isActionViewExpanded() );
		setVisibleIf( !searchMenuItem.isActionViewExpanded(), R.id.actions );

		// try to reset the query string back after restore
		if( Constants.mIsDirectorySearchEnabled ) {
			search = (SearchView) searchMenuItem.getActionView();
			SearchManager searchManager = (SearchManager) getSystemService( Context.SEARCH_SERVICE );
			search.setSearchableInfo( searchManager.getSearchableInfo( getComponentName() ) );

			if( !TextUtils.isEmpty( mCurrentSearch ) ) {
				searchMenuItem.expandActionView();
				search.setQuery( mCurrentSearch, true );
				search.clearFocus();
			}
		}
		//
		return super.onCreateOptionsMenu( menu );

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHomeWatcher.stopWatch();
	}

	@Override
	public void onHomeLongPressed() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onHomePressed() {
		if( search != null ) {
			Constants.mIsHomeClicked = true;
			if( !TextUtils.isEmpty( search.getQuery() ) ) {
				mCurrentSearch = search.getQuery().toString();
			} else {
				mCurrentSearch = null;
			}
		}
	}

	@Override
	public void onLoaderReset( Loader<Cursor> loader ) {
		if( searchSuggestions != null ) {
			searchSuggestions.swapCursor( null );
		}
	}

	@Override
	public void onLoadFinished( Loader<Cursor> loader, Cursor cursor ) {
		if( Constants.mIsDirectorySearchEnabled ) {
			if( loader.getId() == R.id.directory_search && mDirectorySearchListFragment != null ) {
				mDirectorySearchListFragment.setDirectorySearchList( cursor, loader.getId() );
			}
			mProgressBar.setVisibility( View.INVISIBLE );
		} else {
			if( searchSuggestions != null ) {
				if( cursor == null || cursor.isClosed() ) {
					searchSuggestions.swapCursor( null );
				} else {
					searchSuggestions.swapCursor( cursor );
				}
			}
		}

	}

	@Override
	public boolean onMenuOpened( int featureId, Menu menu ) {
		setVisibleIf( false, R.id.actions );
		return super.onMenuOpened( featureId, menu );
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
			case R.id.action_lock:
				lock();
				break;

			case R.id.action_settings:
				startActivity( SettingsActivity.getIntent( this ) );
				break;

		}

		return super.onOptionsItemSelected( item );

	}

	@Override
	public void onPanelClosed( int featureId, Menu menu ) {
		super.onPanelClosed( featureId, menu );
		if( Constants.mIsDirectorySearchEnabled ) {
			if( search.getVisibility() == View.VISIBLE || getFragmentManager().findFragmentById( R.id.chats ) instanceof DirectorySearchListFragment ) {
				return;
			}
		}
		setVisibleIf( true, R.id.actions );
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceivers();
		cancelAutoRefresh();
		if( deactivating && !isFinishing() ) {
			finish();
			deactivating = false;
		}
	}

	@Override
	public boolean onQueryTextChange( String query ) {
		// STA-832: directory search feature.
		if( Constants.mIsDirectorySearchEnabled ) {
			// do not make a search if the search string is set #.
			if( mDontChangeQuery ) {
				mDontChangeQuery = false;
				return false;
			}
			if( !TextUtils.isEmpty( query ) ) {
				Constants.mIsExitApp = false;
				mProgressBar.setVisibility( View.VISIBLE );
				mSCDirectoryCheckBox.setChecked( true );
			} else {
				mProgressBar.setVisibility( View.GONE );
				Constants.mIsExitApp = true;
				ScDirectoryLoader.setStart( 0 );
				if( mDirectorySearchListFragment != null ) {
					mDirectorySearchListFragment.setSearchString( "" );
					mDirectorySearchListFragment.setDirectorySearchList( null, Constants.DIRECTORY_SECTION );
					mDirectorySearchListFragment.setContactsSearchList( null );
				}
				int count = getFragmentManager().getBackStackEntryCount();
				if( count > 0 && getFragmentManager().getBackStackEntryAt( count - 1 ).getName().equals( DirectorySearchListFragment.TAG ) ) {
					// if count = 2, just pop the top DirectorySearchListFragment, the next
					// ChatsFragment will be shown
					getFragmentManager().popBackStack();

					// if count = 1, the top DirectorySearchListFragment is the only fragment in the
					// stack, after pop, the ChatsFragment needs to be added.
					if( count == 1 ) {
						ChatsFragment fragment = new ChatsFragment();
						getFragmentManager().beginTransaction().replace( R.id.chats, fragment ).addToBackStack( ChatsFragment.TAG ).commit();
					}
				}

				return false;
			}
			mCurrentSearch = query;
			if( mSCDirectoryCheckBox.isChecked() ) {
				int count = getFragmentManager().getBackStackEntryCount();
				if( mDirectorySearchListFragment == null || count > 0 ) {
					if( count > 0 ) {
						getFragmentManager().popBackStack();
					}
					mDirectorySearchListFragment = new DirectorySearchListFragment( this, mDirectorySearchList, mScContactsList );
					FragmentTransaction transaction = getFragmentManager().beginTransaction();
					transaction.replace( R.id.chats, mDirectorySearchListFragment );
					transaction.addToBackStack( DirectorySearchListFragment.TAG );
					transaction.commit();
				}
				// if( mDirectorySearchListFragment == null ) {
				// mDirectorySearchListFragment = new DirectorySearchListFragment( this,
				// mDirectorySearchList, mScContactsList );
				// FragmentTransaction transaction = getFragmentManager().beginTransaction();
				// transaction.replace( R.id.chats, mDirectorySearchListFragment );
				// transaction.addToBackStack( DirectorySearchListFragment.TAG );
				// transaction.commit();
				// } else {
				// int count = getFragmentManager().getBackStackEntryCount();
				// if( count > 0 ) {
				// getFragmentManager().getBackStackEntryAt( count - 1 ).getName() );
				// }
				// if( count > 0 ) {
				// getFragmentManager().popBackStack();
				// mDirectorySearchListFragment = new DirectorySearchListFragment( this,
				// mDirectorySearchList, mScContactsList );
				// FragmentTransaction transaction = getFragmentManager().beginTransaction();
				// transaction.replace( R.id.chats, mDirectorySearchListFragment );
				// transaction.addToBackStack( DirectorySearchListFragment.TAG );
				// transaction.commit();
				// }
				// }
				mDirectorySearchListFragment.setSearchString( query );
				if( mLoader != null ) {
					( (ScDirectoryLoader) mLoader ).setQueryString( query );
				}
				ScDirectoryLoader.setStart( 0 );
				directorySearchDelayed( mCurrentSearch, true );
				if( isSPARunnung() ) {
					scContactsSearch( mCurrentSearch );
				}
				return false;
			}
			return false;
		}

		// implemented by Devin - Original search for user.
		getLoaderManager().destroyLoader( R.id.action_search );

		if( !StringUtils.isMinimumLength( query, 2 ) ) {
			return false;
		}

		Bundle arguments = new Bundle();
		Extra.TEXT.to( arguments, query );
		getLoaderManager().initLoader( R.id.action_search, arguments, this );

		return false;

	}

	@Override
	public boolean onQueryTextSubmit( String query ) {
		mQueryTextSubmit = true;
		handleSearch( query );
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// SPA-454: may not need the following code to access STA, commented out for now.
		// // SPA-454: called from SPA and pass jid to ConversationActivity
		// Bundle b = getIntent().getExtras();
		// if( b != null ) {
		// String jid = b.getString( "jid" );
		// if( jid != null ) {
		// launchConversationActivity( jid );
		// return;
		// }
		// }

		try {
			assertPermissionToView();
		} catch( IllegalStateException exception ) {
			return;
		}

		if( !RefreshSelfIntentService.wasSuccessful && !RefreshSelfIntentService.isRunning ) {
			getActivity().startService( new Intent( getActivity(), RefreshSelfIntentService.class ) );
		}

		unlockContentView();

		updateTitle();
		invalidateOptionsMenu();
		registerReceivers();
		scheduleAutoRefresh();
		NotificationBroadcaster.cancel( this );

		if( Constants.mIsDirectorySearchEnabled ) {
			// directory search results replaces ChatsFragment
			FragmentManager fm = getFragmentManager();
			if( fm.findFragmentById( R.id.chats ) instanceof DirectorySearchListFragment || Constants.mIsDirectorySearchFragment ) {
				if( Constants.mIsDirectorySearchFragment && Constants.mConversationListItemClicked ) {
					Constants.mConversationListItemClicked = false;
					getChatsFragment().update();
				}
				// else if( Constants.mIsHomeClicked ) {
				// Constants.mIsHomeClicked = false;
				// if( getChatsFragment() != null ) {
				// getChatsFragment().update();
				// }
				// }
				Constants.mIsDirectorySearchFragment = false;
				if( search != null ) {
					mDontChangeQuery = true;
					search.clearFocus();
					search.setQuery( mCurrentSearch, false );
				}
			} else {
				getChatsFragment().update();
			}
			if( Constants.mIsHomeClicked ) {
				Constants.mIsHomeClicked = false;
				if( search != null ) {
					mDontChangeQuery = true;
					search.clearFocus();
					search.setQuery( mCurrentSearch, false );
				}
			}
			if( TextUtils.isEmpty( Constants.mOrgName ) ) {
				new Handler().post( mOrgNameRunnable );
			}
		} else {
			getChatsFragment().update();
		}
	}

	protected boolean onSearchDismissed() {
		if( actionMenu == null ) {
			return false;
		}

		MenuItem item = actionMenu.findItem( R.id.action_search );

		if( item == null ) {
			return false;
		}

		item.collapseActionView();

		SearchView s = (SearchView) item.getActionView();

		if( s != null ) {
			s.setQuery( "", false );
		}

		if( Constants.mIsDirectorySearchEnabled ) {
			mSCDirectoryCheckBox.setChecked( false );
		}
		return true;

	}

	@Override
	public boolean onSearchRequested() {
		if( actionMenu == null ) {
			return false;
		}
		MenuItem item = actionMenu.findItem( R.id.action_search );
		if( item == null ) {
			return false;
		}
		item.expandActionView();
		return true;
	}

	protected void postRefresh() {
		runOnUiThread( new Refresher() );
	}

	protected void refresh() {

		try {
			assertPermissionToView();
		} catch( IllegalStateException exception ) {
			return;
		}

		updateTitle();
		if( getChatsFragment() != null ) {
			getChatsFragment().update();
		}
		finishLoading( R.id.chats );
		getActionBar().show();

	}

	private void registerBeginDeactivating() {
		deactivationListener = new DeactivationListener();
		registerReceiver( deactivationListener, Action.BEGIN_DEACTIVATE, Manifest.permission.READ );
		registerReceiver( deactivationListener, Action.FINISH_DEACTIVATE, Manifest.permission.READ );
	}

	protected void registerReceivers() {
		registerTitleUpdater();
		registerViewUpdater();
		registerBeginDeactivating();
	}

	private void registerTitleUpdater() {
		titleUpdater = new TitleUpdater();
		registerReceiver( titleUpdater, Action.XMPP_STATE_CHANGED, Manifest.permission.READ );
		registerReceiver( titleUpdater, Action.CONNECT, Manifest.permission.READ );
		registerReceiver( titleUpdater, Action.DISCONNECT, Manifest.permission.READ );
	}

	private void registerViewUpdater() {
		viewUpdater = new Refresh();
		registerReceiver( viewUpdater, Action.UPDATE_CONVERSATION, Manifest.permission.READ );
	}

	private void scContactsSearch( String query ) {
		if( mSCLoader == null ) {
			mSCLoader = new ScContactsLoader( this, query );
		}
		mSCLoader.setSearchQuery( query );
		mDirectorySearchListFragment.setContacts( mSCLoader.loadScContants() );
	}

	protected void scheduleAutoRefresh() {
		scheduleAutoRefresh( 10000 );
	}

	protected void scheduleAutoRefresh( long autoRefreshInterval ) {
		cancelAutoRefresh();
		timer = new Timer( "conversation-list:auto-refresh" );
		timer.schedule( new AutoRefresh(), autoRefreshInterval, autoRefreshInterval );
	}

	public void setCurrentQuery( String query ) {
		mCurrentSearch = query;
	}

	protected void unregisterReceivers() {

		if( viewUpdater != null ) {
			unregisterReceiver( viewUpdater );
			viewUpdater = null;
		}

		if( titleUpdater != null ) {
			unregisterReceiver( titleUpdater );
			titleUpdater = null;
		}

	}

	protected void updateTitle() {

		if( !isActivated() || updatingTitle ) {
			return;
		}

		getActionBar().setSubtitle( StringUtils.formatUsername( getUsername() ) );

		updatingTitle = true;
		AsyncUtils.execute( new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground( Void... params ) {
				return getSilentTextApplication().getDisplayName( getUsername() );
			}

			@Override
			protected void onPostExecute( String displayName ) {
				if( StringUtils.isMinimumLength( displayName, 1 ) ) {
					getActionBar().setTitle( displayName );
				} else {
					getActionBar().setTitle( R.string.silent_text );
				}
				updatingTitle = false;
			}

		} );

	}

}
