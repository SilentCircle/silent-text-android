/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.fragment.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.silentcircle.silenttext.R;

public class DirectorySearchListAdapter extends CursorAdapter {

	Cursor mDirectorySearchCursor;

	public DirectorySearchListAdapter( Context context, Cursor mDirectorySearchCursor ) {
		super( context, mDirectorySearchCursor, 0 );
	}

	@Override
	public void bindView( View view, Context context, Cursor cursor ) {

		TextView textViewPersonName = (TextView) view.findViewById( R.id.search_display_name_tv_id );
		// textViewPersonName.setText( cursor.getString( cursor.getColumnIndex(
		// ContactProvider.DISPLAY_NAME ) ) );
		textViewPersonName.setText( cursor.getString( cursor.getColumnIndex( "display_name" ) ) );

		TextView textViewPersonPIN = (TextView) view.findViewById( R.id.search_jid_tv_id );
		// textViewPersonPIN.setText( cursor.getString( cursor.getColumnIndex(
		// ContactProvider.SOURCE ) ) );
		textViewPersonPIN.setText( cursor.getString( cursor.getColumnIndex( "source" ) ) );
	}

	@Override
	public View newView( Context context, Cursor cursor, ViewGroup parent ) {
		LayoutInflater inflater = LayoutInflater.from( parent.getContext() );
		View retView = inflater.inflate( R.layout.directory_search_item, parent, false );

		return retView;
	}

}
