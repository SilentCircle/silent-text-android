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
package com.silentcircle.silenttext.fragment;

import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.silentcircle.silenttext.view.ListView;
import com.silentcircle.silenttext.view.adapter.BaseListAdapter;

public class ListFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener {

	private List<?> items;

	protected List<?> getItems() {
		return items;
	}

	protected ListView getListView() {
		View view = getUnwrappedView();
		return view instanceof ListView ? (ListView) view : null;
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		ListView view = new ListView( inflater.getContext() );
		view.setOnItemClickListener( this );
		view.setOnItemLongClickListener( this );
		return view;
	}

	@Override
	public void onItemClick( AdapterView<?> parent, View view, int position, long itemID ) {
		onItemClick( parent.getAdapter().getItem( position ) );
	}

	/**
	 * @param item
	 */
	protected void onItemClick( Object item ) {
		// By default, do nothing.
	}

	@Override
	public boolean onItemLongClick( AdapterView<?> parent, View view, int position, long itemID ) {
		return onItemLongClick( parent.getAdapter().getItem( position ) );
	}

	/**
	 * @param item
	 */
	protected boolean onItemLongClick( Object item ) {
		return false;
	}

	public void setItems( int layoutResourceID, List<?> items ) {
		this.items = items;
		ListView view = getListView();
		if( view != null && items != null ) {
			view.setAdapter( new BaseListAdapter( layoutResourceID, items ) );
		}
	}

}
