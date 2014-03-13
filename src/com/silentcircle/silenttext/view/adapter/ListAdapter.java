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

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.silentcircle.silenttext.view.HasChoiceMode;

public class ListAdapter<T> extends BaseAdapter implements HasChoiceMode {

	public static interface OnItemRemovedListener {

		public void onItemRemoved( Object item );

	}

	public static <T> ListAdapter<T> from( ListView view ) {
		return (ListAdapter<T>) view.getAdapter();
	}

	protected final int layoutResourceId;
	protected final List<T> list;
	protected boolean inChoiceMode;
	private OnItemRemovedListener onItemRemoved;

	public ListAdapter( int layoutResourceId ) {
		this( layoutResourceId, new ArrayList<T>() );
	}

	public ListAdapter( int layoutResourceId, List<T> list ) {
		this.layoutResourceId = layoutResourceId;
		this.list = list;
	}

	public T get( int position ) {
		return list.get( position % list.size() );
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem( int position ) {
		return get( position );
	}

	@Override
	public long getItemId( int position ) {
		return getItem( position ).hashCode();
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent ) {

		if( parent == null ) {
			return null;
		}

		View view = convertView == null ? View.inflate( parent.getContext(), layoutResourceId, null ) : convertView;
		Object item = getItem( position );

		view.setTag( item );

		if( view instanceof HasChoiceMode ) {
			( (HasChoiceMode) view ).setInChoiceMode( isInChoiceMode() );
		}

		return view;

	}

	@Override
	public boolean isInChoiceMode() {
		return inChoiceMode;
	}

	public void remove( int position ) {
		remove( getItem( position ) );
	}

	public void remove( Object item ) {
		list.remove( item );
		if( onItemRemoved != null ) {
			onItemRemoved.onItemRemoved( item );
		}
	}

	@Override
	public void setInChoiceMode( boolean inChoiceMode ) {
		this.inChoiceMode = inChoiceMode;
	}

	public void setItems( List<T> items ) {
		list.clear();
		list.addAll( items );
	}

	public void setOnItemRemovedListener( OnItemRemovedListener onItemRemoved ) {
		this.onItemRemoved = onItemRemoved;
	}

	public void update( List<T> items ) {
		for( T item : items ) {
			update( item );
		}
	}

	public void update( T item ) {
		list.remove( item );
		list.add( item );
	}

}
