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

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.actionbarsherlock.widget.SearchView.OnSuggestionListener;
import com.silentcircle.silenttext.Action;
import com.silentcircle.silenttext.Extra;
import com.silentcircle.silenttext.Manifest;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.listener.ClickthroughWhenNotInChoiceMode;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector.ActionPerformer;
import com.silentcircle.silenttext.listener.OnAccountSelectedListener;
import com.silentcircle.silenttext.listener.OnObjectReceiveListener;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.task.ClearConversationsTask;
import com.silentcircle.silenttext.task.ConversationListTask;
import com.silentcircle.silenttext.util.StringUtils;
import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.OptionsDrawer;
import com.silentcircle.silenttext.view.adapter.AccountNavigationAdapter;
import com.silentcircle.silenttext.view.adapter.ContactSuggestionAdapter;
import com.silentcircle.silenttext.view.adapter.ConversationAdapter;
import com.silentcircle.silenttext.view.adapter.ListAdapter;
import com.silentcircle.silenttext.view.adapter.ListAdapter.OnItemRemovedListener;

public class ConversationListActivity extends SilentActivity implements ActionPerformer, LoaderCallbacks<Cursor>, OnQueryTextListener {

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

	class ClearConversationsAndRefreshTask extends ClearConversationsTask {

		@Override
		protected void onPostExecute( Void result ) {
			refresh();
		}

	}

	class Refresh extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			refresh();
		}

	}

	class RefreshTask extends ConversationListTask {

		RefreshTask( ConversationRepository repository, ListAdapter<Conversation> adapter ) {
			super( repository, adapter );
		}

		@Override
		protected void onPostExecute( List<Conversation> conversations ) {
			super.onPostExecute( conversations );
			updateTitle();
			finishLoading( R.id.conversations );
		}

	}

	class TitleUpdater extends BroadcastReceiver {

		@Override
		public void onReceive( Context context, Intent intent ) {
			updateTitle();
		}

	}

	protected static final int R_id_search = 0xFFFF & R.id.search;

	private BroadcastReceiver viewUpdater;
	protected ConversationListTask task;
	private Menu actionMenu;
	private BroadcastReceiver titleUpdater;
	protected ClearConversationsAndRefreshTask clearConversationsAndRefreshTask;
	protected SearchView search;
	protected CursorAdapter searchSuggestions;
	protected Timer timer;

	protected void cancelAutoRefresh() {
		if( timer != null ) {
			timer.cancel();
			timer = null;
		}
	}

	protected void cancelTasks() {
		if( task != null ) {
			task.cancel( true );
			task = null;
		}
	}

	protected void createOptionsDrawer() {
		getOptionsDrawer().attach( this );
	}

	protected void destroyClearConversationsAndRefreshTask() {
		if( clearConversationsAndRefreshTask != null ) {
			clearConversationsAndRefreshTask.cancel( true );
			clearConversationsAndRefreshTask = null;
		}
	}

	protected Conversation getConversation( int position ) {
		return (Conversation) getAdapter( R.id.conversations ).getItem( position );
	}

	protected OptionsDrawer getOptionsDrawer() {
		return (OptionsDrawer) findViewById( R.id.drawer_content );
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
			onSearchDismissed();
		}
	}

	protected void handleSearch( String query ) {

		final String username = ( query.contains( "@" ) ? query : String.format( "%s@%s", query, getServer( "xmpp" ).getServiceName() ) ).toLowerCase( Locale.ENGLISH );
		final String displayUsername = username.contains( getServer( "xmpp" ).getServiceName() ) ? username.replaceAll( "@.+$", "" ) : username;

		isAccessible( username, new OnObjectReceiveListener<Boolean>() {

			@Override
			public void onObjectReceived( Boolean accessible ) {
				if( accessible.booleanValue() ) {
					Intent intent = new Intent( getBaseContext(), ConversationActivity.class );
					intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
					intent.putExtra( Extra.PARTNER.getName(), username );
					startActivityForResult( intent, R_id_search );
				} else {
					toast( R.string.username_unavailable, displayUsername );
				}
			}

		} );

	}

	protected void inflate( Menu menu ) {

		actionMenu = menu;
		getSupportMenuInflater().inflate( R.menu.conversation_list, menu );

		search = (SearchView) menu.findItem( R.id.search ).getActionView();

		search.setSuggestionsAdapter( searchSuggestions );
		search.setQueryHint( getString( R.string.search_hint ) );
		search.setOnQueryTextListener( this );
		search.setOnSuggestionListener( new OnSuggestionListener() {

			@Override
			public boolean onSuggestionClick( int position ) {
				Cursor cursor = searchSuggestions.getCursor();
				cursor.moveToPosition( position );
				Contact contact = getContacts().getContact( cursor );
				getSupportLoaderManager().destroyLoader( R.id.search );
				handleSearch( contact.getUsername() );
				return true;
			}

			@Override
			public boolean onSuggestionSelect( int position ) {
				// Do nothing.
				return false;
			}

		} );

	}

	@Override
	protected void lockContentView() {
		super.lockContentView();
		getSupportActionBar().setTitle( "" );
		getSupportActionBar().setSubtitle( "" );
	}

	@Override
	public void onActionPerformed() {
		refresh();
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent intent ) {
		switch( requestCode ) {
			case R_id_search:
				onSearchDismissed();
				break;
		}
		super.onActivityResult( requestCode, resultCode, intent );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_conversation_list_with_drawer );
		hideSoftKeyboardOnDrawerToggle();
		beginLoading( R.id.conversations );

		searchSuggestions = new ContactSuggestionAdapter( this, null, 0 );

		handleIntent( getIntent() );

		ListView conversations = findListViewById( R.id.conversations );

		conversations.setEmptyView( findViewById( R.id.empty ) );
		ConversationAdapter adapter = new ConversationAdapter();
		conversations.setAdapter( adapter );
		conversations.setOnItemClickListener( new ClickthroughWhenNotInChoiceMode() );
		conversations.setMultiChoiceModeListener( new MultipleChoiceSelector<Conversation>( adapter, R.menu.multiselect_conversation, this ) );
		conversations.setSwipeEnabled( false );
		conversations.setDivider( null );
		conversations.setDividerHeight( 0 );

		createOptionsDrawer();

		AccountManager accountManager = (AccountManager) getSystemService( ACCOUNT_SERVICE );
		if( accountManager != null ) {
			Account [] accounts = accountManager.getAccountsByType( "com.silentcircle" );
			if( accounts.length > 0 ) {
				getSupportActionBar().setDisplayShowHomeEnabled( false );
				getSupportActionBar().setDisplayShowTitleEnabled( false );
				getSupportActionBar().setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
				AccountNavigationAdapter accountAdapter = new AccountNavigationAdapter( accounts );
				getSupportActionBar().setListNavigationCallbacks( accountAdapter, new OnAccountSelectedListener( this, accountAdapter ) );
			}
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader( int id, Bundle arguments ) {
		String query = Extra.TEXT.from( arguments );
		CursorLoader cursor = getContacts().search( this, query );
		return cursor;
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		inflate( menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyClearConversationsAndRefreshTask();
	}

	@Override
	public void onLoaderReset( Loader<Cursor> loader ) {
		if( searchSuggestions != null ) {
			searchSuggestions.swapCursor( null );
		}
	}

	@Override
	public void onLoadFinished( Loader<Cursor> loader, Cursor cursor ) {
		if( searchSuggestions != null ) {
			if( cursor == null || cursor.isClosed() ) {
				searchSuggestions.swapCursor( null );
			} else {
				searchSuggestions.swapCursor( cursor );
			}
		}
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

			case R.id.options:
				toggleDrawer();
				hideSoftKeyboard( search );
				break;

		}

		return super.onOptionsItemSelected( item );

	}

	@Override
	protected void onPause() {
		super.onPause();
		cancelTasks();
		unregisterReceivers();
		cancelAutoRefresh();
	}

	@Override
	public boolean onQueryTextChange( String query ) {

		getSupportLoaderManager().destroyLoader( R.id.search );

		if( !StringUtils.isMinimumLength( query, 2 ) ) {
			return false;
		}

		Bundle arguments = new Bundle();
		Extra.TEXT.to( arguments, query );
		getSupportLoaderManager().initLoader( R.id.search, arguments, this );

		return false;

	}

	@Override
	public boolean onQueryTextSubmit( String query ) {
		handleSearch( query );
		return true;
	}

	@Override
	protected void onResume() {

		super.onResume();

		if( !isUnlocked() ) {
			lockContentView();
			requestUnlock();
			return;
		}

		if( !isActivated() ) {
			lockContentView();
			requestActivation();
			return;
		}

		if( isInactive() ) {
			lockContentView();
			lock();
			return;
		}

		unlockContentView();

		getOptionsDrawer().onPasscodeUpdate();

		registerReceivers();
		refresh();
		scheduleAutoRefresh();

	}

	protected boolean onSearchDismissed() {
		if( actionMenu == null ) {
			return false;
		}
		MenuItem item = actionMenu.findItem( R.id.search );
		if( item == null ) {
			return false;
		}
		item.collapseActionView();
		return true;
	}

	@Override
	public boolean onSearchRequested() {
		if( actionMenu == null ) {
			return false;
		}
		MenuItem item = actionMenu.findItem( R.id.search );
		if( item == null ) {
			return false;
		}
		item.expandActionView();
		return true;
	}

	protected void performAction( int actionId, Conversation conversation ) {

		switch( actionId ) {

			case R.id.burn:
				getSilentTextApplication().getUsers().removeByID( conversation.getPartner().getUsername().toCharArray() );
				getConversations().remove( conversation );
				break;

			default:
				// Unknown or unhandled action.
				break;

		}

	}

	@Override
	public void performAction( int actionId, int position ) {
		performAction( actionId, getConversation( position ) );
	}

	protected void refresh() {

		cancelTasks();
		updateTitle();

		ConversationAdapter adapter = getAdapter( R.id.conversations );
		adapter.setOnItemRemovedListener( new OnItemRemovedListener() {

			@Override
			public void onItemRemoved( Object item ) {
				if( item instanceof Conversation ) {
					performAction( R.id.burn, (Conversation) item );
				}
			}

		} );
		ConversationRepository conversations = getConversations();
		if( conversations != null ) {
			task = new RefreshTask( getConversations(), adapter );
			task.execute();
		}

	}

	protected void registerReceivers() {
		registerTitleUpdater();
		registerViewUpdater();
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

	protected void scheduleAutoRefresh() {
		scheduleAutoRefresh( 10000 );
	}

	protected void scheduleAutoRefresh( long autoRefreshInterval ) {
		cancelAutoRefresh();
		timer = new Timer();
		timer.schedule( new AutoRefresh(), autoRefreshInterval, autoRefreshInterval );
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
		if( !isActivated() ) {
			return;
		}
		getSupportActionBar().setTitle( getShortUsername() );
		getSupportActionBar().setSubtitle( getString( isOnline() ? R.string.online : R.string.offline ) );
	}

}
