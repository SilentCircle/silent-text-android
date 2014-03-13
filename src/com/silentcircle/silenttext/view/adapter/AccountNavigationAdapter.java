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
package com.silentcircle.silenttext.view.adapter;

import android.accounts.Account;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.silentcircle.silenttext.R;

public class AccountNavigationAdapter implements SpinnerAdapter {

	private final int dropdownLayoutResourceID = R.layout.navigation_dropdown_item_account;
	private final int layoutResourceID = R.layout.navigation_item_account;
	private final Account [] accounts;
	private int selectedIndex;

	public AccountNavigationAdapter( Account [] accounts ) {
		this.accounts = accounts;
	}

	@Override
	public int getCount() {
		return accounts.length + 1;
	}

	@Override
	public View getDropDownView( int position, View convertView, ViewGroup parent ) {
		View view = convertView;
		if( view == null ) {
			view = View.inflate( parent.getContext(), dropdownLayoutResourceID, null );
		}
		TextView t = (TextView) view.findViewById( R.id.title );
		t.setText( position >= accounts.length ? "Other..." : accounts[position].name );
		t.setEnabled( position != selectedIndex );
		return view;
	}

	@Override
	public Object getItem( int position ) {
		return position >= accounts.length ? null : accounts[position];
	}

	@Override
	public long getItemId( int position ) {
		Object item = getItem( position );
		return item == null ? 0 : item.hashCode();
	}

	@Override
	public int getItemViewType( int position ) {
		return Adapter.IGNORE_ITEM_VIEW_TYPE;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent ) {
		View view = convertView;
		if( view == null ) {
			view = View.inflate( parent.getContext(), layoutResourceID, null );
		}
		TextView t = (TextView) view.findViewById( R.id.title );
		t.setText( position >= accounts.length ? "Other..." : accounts[position].name );
		return view;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return accounts.length == 0;
	}

	@Override
	public void registerDataSetObserver( DataSetObserver observer ) {
		// TODO Auto-generated method stub
	}

	public void setSelectedIndex( int selectedIndex ) {
		this.selectedIndex = selectedIndex;
	}

	@Override
	public void unregisterDataSetObserver( DataSetObserver observer ) {
		// TODO Auto-generated method stub
	}

}