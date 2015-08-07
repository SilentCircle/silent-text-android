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

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.listener.ClickthroughWhenNotInChoiceMode;
import com.silentcircle.silenttext.listener.MultipleChoiceSelector.ActionPerformer;
import com.silentcircle.silenttext.model.event.ErrorEvent;
import com.silentcircle.silenttext.model.event.Event;
import com.silentcircle.silenttext.model.event.IncomingMessage;
import com.silentcircle.silenttext.model.event.OutgoingMessage;
import com.silentcircle.silenttext.model.event.ResourceChangeEvent;
import com.silentcircle.silenttext.model.event.WarningEvent;
import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.adapter.ModelViewAdapter;
import com.silentcircle.silenttext.view.adapter.ModelViewType;
import com.silentcircle.silenttext.view.adapter.MultiSelectModelViewAdapter;
import com.silentcircle.silenttext.view.adapter.ViewType;
import com.silentcircle.silenttext.widget.FailureEventView;
import com.silentcircle.silenttext.widget.MessageEventView;
import com.silentcircle.silenttext.widget.ResourceChangeView;
import com.silentcircle.silenttext.widget.TextView;
import com.silentcircle.silenttext.widget.WarningEventView;

public class ChatFragment extends BaseFragment implements ActionPerformer {

	public static interface Callback {

		public void onActionModeCreated();

		public void onActionModeDestroyed();

		public void onActionPerformed();

		public void performAction( int actionID, Object target );

	}

	private static final ViewType [] VIEW_TYPES = {

		new ModelViewType( IncomingMessage.class, MessageEventView.class, R.layout.list_item_incoming_message ),
		new ModelViewType( OutgoingMessage.class, MessageEventView.class, R.layout.list_item_outgoing_message ),
		new ModelViewType( WarningEvent.class, WarningEventView.class, R.layout.list_item_warning ),
		new ModelViewType( ErrorEvent.class, FailureEventView.class, R.layout.list_item_error ),
		new ModelViewType( ResourceChangeEvent.class, ResourceChangeView.class, R.layout.list_item_resource_change ),
		new ModelViewType( Event.class, TextView.class, R.layout.list_item_text )

	};

	private static void attachEventsToAdapter( ModelViewAdapter adapter, List<Event> events ) {
		adapter.setModels( events );
		adapter.notifyDataSetChanged();
	}

	private Callback callback;

	private void attachEventsToView( ListView view, List<Event> events ) {
		MultiSelectModelViewAdapter adapter = new MultiSelectModelViewAdapter( events, VIEW_TYPES );
		view.setAdapter( adapter );
		view.setOnItemClickListener( ClickthroughWhenNotInChoiceMode.getInstance() );
		view.setMultiChoiceModeListener( new ChatFragmentMultipleChoiceSelector( this, adapter, R.menu.multiselect_event, getString( R.string.n_selected ) ) );
	}

	protected Callback getCallback() {
		if( callback != null ) {
			return callback;
		}
		Activity activity = getActivity();
		return activity instanceof Callback ? (Callback) activity : null;
	}

	public Event getEvent( int position ) {
		ListView eventsView = (ListView) findViewById( R.id.chat_events );
		if( eventsView != null ) {
			return (Event) eventsView.getItemAtPosition( position );
		}
		return null;
	}

	public boolean hasMultipleCheckedItems() {
		ListView eventsView = (ListView) findViewById( R.id.chat_events );
		return eventsView != null && eventsView.hasMultipleCheckedItems();
	}

	@Override
	public void onActionPerformed() {
		Callback c = getCallback();
		if( c != null ) {
			c.onActionPerformed();
		}
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		return inflater.inflate( R.layout.chat, container, false );
	}

	@Override
	public void performAction( int menuActionId, int position ) {
		Callback c = getCallback();
		if( c != null ) {
			c.performAction( menuActionId, getEvent( position ) );
		}
	}

	public void setCallback( Callback callback ) {
		this.callback = callback;
	}

	public void setEvents( List<Event> events ) {

		ListView eventsView = (ListView) findViewById( R.id.chat_events );

		if( eventsView != null ) {

			ListAdapter adapter = eventsView.getAdapter();

			if( adapter instanceof ModelViewAdapter ) {
				attachEventsToAdapter( (ModelViewAdapter) adapter, events );
			} else {
				attachEventsToView( eventsView, events );
			}

		}

	}

}
