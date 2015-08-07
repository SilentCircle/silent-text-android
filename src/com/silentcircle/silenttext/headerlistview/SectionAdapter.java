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
package com.silentcircle.silenttext.headerlistview;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.fragment.DirectorySearchListFragment;
import com.silentcircle.silenttext.loader.ContactUser;
import com.silentcircle.silenttext.loader.ScDirectoryLoader.UserData;
import com.silentcircle.silenttext.log.Log;

public abstract class SectionAdapter extends BaseAdapter implements OnItemClickListener {

	private final static String TAG = "SectionAdapter";
	private int mCount = -1;
	private List<UserData> mDirectorySearchList = new ArrayList<UserData>();
	private List<ContactUser> mScContactsList = new ArrayList<ContactUser>();
	DirectorySearchListFragment mFragment;

	public static View mCurView, mPreView;

	public SectionAdapter( DirectorySearchListFragment fragment, List<UserData> directorySearchList, List<ContactUser> scContactsList ) {
		mFragment = fragment;
		mDirectorySearchList = directorySearchList;
		mScContactsList = scContactsList;
	}

	public boolean disableHeaders() {
		return false;
	}

	@Override
	/**
	 * Counts the amount of cells = headers + rows
	 */
	public final int getCount() {
		if( mCount < 0 ) {
			mCount = numberOfCellsBeforeSection( numberOfSections() );
			if( mDirectorySearchList.size() != 0 && mScContactsList.size() != 0 ) {
				mCount = mDirectorySearchList.size() + mScContactsList.size() + 2;
				return mCount;
			}
			if( mDirectorySearchList.size() == 0 ) {
				if( mScContactsList.size() != 0 ) {
					mCount = mScContactsList.size() + 1;
				}
				return mCount;
			}
			if( mScContactsList.size() == 0 ) {
				if( mDirectorySearchList.size() != 0 ) {
					mCount = mDirectorySearchList.size() + 1;
				}
				return mCount;
			}
		}
		return mCount;
	}

	@Override
	/**
	 * Dispatched to call getRowItem or getSectionHeaderItem
	 */
	public final Object getItem( int position ) {
		int section = getSection( position );
		if( isSectionHeader( position ) ) {
			if( hasSectionHeaderView( section ) ) {
				return getSectionHeaderItem( section );
			}
			return null;
		}
		return getRowItem( section, getRowInSection( position ) );
	}

	@Override
	public long getItemId( int position ) {
		return position;
	}

	@Override
	/**
	 * Dispatched to call getRowItemViewType or getSectionHeaderItemViewType
	 */
	public final int getItemViewType( int position ) {
		int section = getSection( position );
		if( isSectionHeader( position ) ) {
			return getRowViewTypeCount() + getSectionHeaderItemViewType( section );
		}
		return getRowItemViewType( section, getRowInSection( position ) );

	}

	/**
	 * Returns the row index of the indicated cell Should not be call with positions directing to
	 * section headers
	 */
	protected int getRowInSection( int position ) {
		int section = getSection( position );
		int row = position - numberOfCellsBeforeSection( section );
		if( hasSectionHeaderView( section ) ) {
			return row - 1;
		}
		return row;
	}

	public abstract Object getRowItem( int section, int row );

	/**
	 * Must return a value between 0 and getRowViewTypeCount() (excluded)
	 */
	public int getRowItemViewType( int section, int row ) {
		if( section == -1 ) {
			Log.i( TAG, "section = " + section + " row = " + row );
		}
		return 0;
	}

	public abstract View getRowView( int section, int row, View convertView, ViewGroup parent );

	public int getRowViewTypeCount() {
		return 1;
	}

	/**
	 * Returns the section number of the indicated cell
	 */
	protected int getSection( int position ) {
		int section = 0;
		int cellCounter = 0;
		while( cellCounter <= position && section <= numberOfSections() ) {
			cellCounter += numberOfCellsInSection( section );
			section++;
		}
		return section - 1;
	}

	public Object getSectionHeaderItem( int section ) {
		if( section == -1 ) {
			Log.i( TAG, "section = " + section );
		}
		return null;
	}

	/**
	 * Must return a value between 0 and getSectionHeaderViewTypeCount() (excluded, if > 0)
	 */
	public int getSectionHeaderItemViewType( int section ) {
		if( section == -1 ) {
			Log.i( TAG, "section = " + section );
		}
		return 0;
	}

	public View getSectionHeaderView( int section, View convertView, ViewGroup parent ) {
		if( convertView != null && parent != null && section == -1 ) {
			Log.i( TAG, "section = " + section );
		}
		return null;
	}

	public int getSectionHeaderViewTypeCount() {
		return 1;
	}

	@Override
	/**
	 * Dispatched to call getRowView or getSectionHeaderView
	 */
	public final View getView( int position, View convertView, ViewGroup parent ) {
		int section = getSection( position );
		if( isSectionHeader( position ) ) {
			if( hasSectionHeaderView( section ) ) {
				return getSectionHeaderView( section, convertView, parent );
			}
			return null;
		}

		return getRowView( section, getRowInSection( position ), convertView, parent );
	}

	@Override
	/**
	 * Dispatched to call getRowViewTypeCount and getSectionHeaderViewTypeCount
	 */
	public final int getViewTypeCount() {
		return getRowViewTypeCount() + getSectionHeaderViewTypeCount();
	}

	public boolean hasSectionHeaderView( int section ) {
		if( section == -1 ) {
			Log.i( TAG, "section = " + section );
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	@Override
	/**
	 * By default, disables section headers
	 */
	public boolean isEnabled( int position ) {
		return ( disableHeaders() || !isSectionHeader( position ) ) && isRowEnabled( getSection( position ), getRowInSection( position ) );
	}

	public boolean isRowEnabled( int section, int row ) {
		if( section == -1 ) {
			Log.i( TAG, "section = " + section + " row = " + row );
		}
		return true;
	}

	/**
	 * Returns true if the cell at this index is a section header
	 */
	protected boolean isSectionHeader( int position ) {
		int section = getSection( position );
		return hasSectionHeaderView( section ) && numberOfCellsBeforeSection( section ) == position;
	}

	@Override
	public void notifyDataSetChanged() {
		mCount = numberOfCellsBeforeSection( numberOfSections() );
		super.notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetInvalidated() {
		mCount = numberOfCellsBeforeSection( numberOfSections() );
		super.notifyDataSetInvalidated();
	}

	/**
	 * Returns the number of cells (= headers + rows) before the indicated section
	 */
	protected int numberOfCellsBeforeSection( int section ) {
		int count = 0;
		for( int i = 0; i < Math.min( numberOfSections(), section ); i++ ) {
			count += numberOfCellsInSection( i );
		}
		return count;
	}

	private int numberOfCellsInSection( int section ) {
		return numberOfRows( section ) + ( hasSectionHeaderView( section ) ? 1 : 0 );
	}

	public abstract int numberOfRows( int section );

	public abstract int numberOfSections();

	@Override
	/**
	 * Dispatched to call onRowItemClick
	 */
	public final void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
		onRowItemClick( parent, view, getSection( position ), getRowInSection( position ), id );
	}

	public void onRowItemClick( AdapterView<?> parent, View view, int section, int row, long id ) {
		if( parent != null && view != null && section == 1 && row == 1 && id == 1 ) {
			// Log.i( TAG, "onRowItemClick section = " + section + " row = " + row + " id = " + id
			// );
		}
		mCurView = view;
		if( mPreView != null ) {
			mPreView.findViewById( R.id.contact_list_icons_layout_id ).setVisibility( View.GONE );
		}
		mPreView = mCurView;
	}
}
