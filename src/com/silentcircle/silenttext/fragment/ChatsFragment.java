/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector.ActionPerformer;
import com.silentcircle.silenttext.model.Conversation;
import com.silentcircle.silenttext.model.MessageState;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.Message;
import com.silentcircle.silenttext.repository.ConversationRepository;
import com.silentcircle.silenttext.repository.EventRepository;
import com.silentcircle.silenttext.util.MessageUtils;
import com.silentcircle.silenttext.view.ConversationView;
import com.silentcircle.silenttext.view.HasChoiceMode;
import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.adapter.ModelViewAdapter;
import com.silentcircle.silenttext.view.adapter.MultiSelectModelViewAdapter;
import com.silentcircle.silenttext.view.adapter.ViewType;

public class ChatsFragment extends BaseFragment implements ActionPerformer, OnItemClickListener {

	public static final String TAG = "ChatsFragment";

	private static final ViewType [] VIEW_TYPES = new ViewType [] {
		new ViewType( ConversationView.class, R.layout.conversation_summary )
	};

	private static final List<Conversation> EMPTY_CONVERSATION_LIST = Arrays.asList( new Conversation [0] );

	public static final String DELETED_USER_FROM_CONVERSATION_LIST = "deleted_user_from_conversation_list";

	private static void attachConversationsToAdapter( ModelViewAdapter adapter, List<Conversation> conversations ) {
		adapter.setModels( conversations );
		adapter.notifyDataSetChanged();
	}

	private static void clearConversation( Context context, Conversation conversation ) {
		// save deleted user.
		SilentTextApplication app = SilentTextApplication.from( context );
		List<String> list = app.getDeletedUsers();
		list.add( conversation.getPartner().getUsername() );
		SharedPreferences prefs = app.getSharedPreferences( DELETED_USER_FROM_CONVERSATION_LIST, Context.MODE_PRIVATE );
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString( DELETED_USER_FROM_CONVERSATION_LIST, TextUtils.join( ",", list ) ).apply();

		ConversationRepository conversationRepository = getConversationRepository( context );
		if( conversationRepository != null ) {
			EventRepository events = conversationRepository.historyOf( conversation );

			events.clear();
		}
	}

	private static SilentTextApplication getApplication( Context context ) {
		return SilentTextApplication.from( context );
	}

	private static Conversation getConversation( View view, int position ) {
		return (Conversation) getListAdapter( view ).getItem( position );
	}

	private static ConversationRepository getConversationRepository( Context context ) {
		SilentTextApplication application = getApplication( context );
		return application.getConversations();
	}

	private static ListAdapter getListAdapter( View view ) {
		return getListView( view ).getAdapter();
	}

	private static ListView getListView( View view ) {
		return (ListView) view.findViewById( R.id.chats_list );
	}

	private static List<Conversation> listConversations( Context context ) {
		ConversationRepository conversationRepository = getConversationRepository( context );
		return conversationRepository != null ? conversationRepository.list() : EMPTY_CONVERSATION_LIST;
	}

	private void attachConversationsToView( ListView listView, List<Conversation> conversations ) {

		MultiSelectModelViewAdapter adapter = new MultiSelectModelViewAdapter( conversations, VIEW_TYPES );

		listView.setAdapter( adapter );

		MultipleChoiceSelector<Conversation> multipleChoiceSelector = new MultipleChoiceSelector<Conversation>( adapter, R.menu.multiselect_conversation, this, getContext().getString( R.string.n_selected ) );
		listView.setMultiChoiceModeListener( multipleChoiceSelector );

	}

	private Context getContext() {
		Context context = getActivity();
		if( context == null ) {
			View view = getView();
			if( view != null ) {
				context = view.getContext();
			}
		}
		return context;
	}

	private ConversationRepository getConversations() {
		return ( (SilentTextApplication) getActivity().getApplication() ).getConversations();
	}

	private EventRepository getEvents( Conversation conversation ) {
		ConversationRepository conversations = getConversations();
		return conversations != null ? conversations.historyOf( conversation ) : null;
	}

	@Override
	public void onActionPerformed() {
		update();
	}

	@Override
	public void onAttach( Activity activity ) {
		super.onAttach( activity );
		update();
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		return inflater.inflate( R.layout.chats, container, false );
	}

	// @Override
	// public void onDetach() {
	// super.onDetach();
	// }

	@Override
	public void onItemClick( AdapterView<?> parent, View view, int position, long itemID ) {

		int id = parent.getId();

		if( id == R.id.chats_list ) {

			HasChoiceMode adapter = (HasChoiceMode) parent.getAdapter();

			if( adapter.isInChoiceMode() ) {
				return;
			}

			if( view instanceof OnClickListener ) {
				( (OnClickListener) view ).onClick( view );
			}

			if( parent instanceof ListView ) {
				( (ListView) parent ).setItemChecked( position, false );
			}

			return;

		}

	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState ) {

		super.onViewCreated( view, savedInstanceState );

		ListView listView = getListView( view );

		listView.setEmptyView( view.findViewById( R.id.chats_empty ) );
		listView.setOnItemClickListener( this );

		update( view );

	}

	@Override
	public void performAction( int menuActionID, int position ) {

		if( menuActionID == R.id.burn ) {

			View view = getView();
			Context context = getContext();
			Conversation conversation = getConversation( view, position );
			clearConversation( context, conversation );

			return;

		}

	}

	public void setConversations( List<Conversation> conversations ) {
		ListView listView = (ListView) findViewById( R.id.chats_list );

		if( listView != null ) {
			ListAdapter adapter = listView.getAdapter();
			if( adapter instanceof ModelViewAdapter ) {
				attachConversationsToAdapter( (ModelViewAdapter) adapter, conversations );
			} else {
				attachConversationsToView( listView, conversations );
			}
		}
	}

	public void update() {
		update( getView() );
	}

	private void update( View view ) {
		if( view == null || isDetached() ) {
			return;
		}

		List<Conversation> conversations = listConversations( view.getContext() );

		List<String> deletedUserList = SilentTextApplication.from( view.getContext() ).getDeletedUsers();

		for( Iterator<Conversation> it = conversations.iterator(); it.hasNext(); ) {
			Conversation conversation = it.next();
			EventRepository events = getEvents( conversation );

			boolean shouldSee = false;

			for( Event event : events.list() ) {
				if( event instanceof Message ) {
					if( ( ( (Message) event ).getState() == MessageState.DECRYPTED || ( (Message) event ).getState() == MessageState.READ || ( (Message) event ).getState() == MessageState.SENT ) && MessageUtils.shouldSee( (Message) event ) ) {
						shouldSee = true;
						if( deletedUserList.contains( conversation.getPartner().getUsername() ) ) {
							deletedUserList.remove( new String( conversation.getPartner().getUsername() ) );

							// remove deleted user and save it
							SilentTextApplication app = SilentTextApplication.from( view.getContext() );
							SharedPreferences prefs = app.getSharedPreferences( DELETED_USER_FROM_CONVERSATION_LIST, Context.MODE_PRIVATE );
							SharedPreferences.Editor editor = prefs.edit();
							editor.putString( DELETED_USER_FROM_CONVERSATION_LIST, TextUtils.join( ",", deletedUserList ) ).apply();

						}
					}
				}
			}

			if( !shouldSee && deletedUserList.contains( conversation.getPartner().getUsername() ) ) {
				it.remove();
			}
		}
		Collections.sort( conversations );
		setConversations( conversations );

	}
}
