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
package com.silentcircle.silenttext.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.silenttext.activity.SilentActivity;
import com.silentcircle.silenttext.listener.SwipeToDelete;

public class ListView extends android.widget.ListView {

	public static class DeleteOnSwipe implements SwipeToDelete.DismissCallbacks {

		@Override
		public boolean canDismiss( int position ) {
			return true;
		}

		@Override
		public void onDismiss( android.widget.ListView listView, int [] reverseSortedPositions ) {
			ListAdapter adapter = listView.getAdapter();
			if( adapter instanceof com.silentcircle.silenttext.view.adapter.ListAdapter ) {
				com.silentcircle.silenttext.view.adapter.ListAdapter<?> a = (com.silentcircle.silenttext.view.adapter.ListAdapter<?>) adapter;
				for( int i = 0; i < reverseSortedPositions.length; i++ ) {
					int position = reverseSortedPositions[i];
					a.remove( position );
				}
				a.notifyDataSetChanged();
			}
		}

	}

	protected class MultiChoiceModeCallback implements MultiChoiceModeListener {

		protected ActionMode currentMode;
		protected final MultiChoiceModeListener delegate;

		public MultiChoiceModeCallback( MultiChoiceModeListener delegate ) {
			this.delegate = delegate;
		}

		public boolean hasActionMode() {
			return currentMode != null;
		}

		@Override
		public boolean onActionItemClicked( ActionMode mode, MenuItem item ) {
			SparseBooleanArray checkedItems = getCheckedItemPositions();
			for( int i = 0; i < checkedItems.size(); i++ ) {
				int position = checkedItems.keyAt( i );
				if( checkedItems.get( position ) ) {
					performAction( item.getItemId(), position );
				}
			}
			return delegate.onActionItemClicked( mode, item );
		}

		@Override
		public boolean onCreateActionMode( ActionMode mode, Menu menu ) {
			currentMode = mode;
			clearChoices();
			return delegate.onCreateActionMode( mode, menu );
		}

		@Override
		public void onDestroyActionMode( ActionMode mode ) {
			delegate.onDestroyActionMode( mode );
			currentMode = null;
			clearChoices();
			requestLayout();
		}

		@Override
		public void onItemCheckedStateChanged( ActionMode mode, int position, long itemId, boolean checked ) {
			delegate.onItemCheckedStateChanged( mode, position, itemId, checked );
			if( !hasCheckedItems() ) {
				mode.finish();
			}
		}

		@Override
		public boolean onPrepareActionMode( ActionMode mode, Menu menu ) {
			return delegate.onPrepareActionMode( mode, menu );
		}

		@Override
		public void performAction( int menuActionId, int position ) {
			delegate.performAction( menuActionId, position );
		}

		public void setItemCheckedState( int position, boolean checked ) {
			setItemChecked( position, checked );
			onItemCheckedStateChanged( currentMode, position, 0, checked );
		}

		public boolean toggleItemCheckedState( int position ) {
			boolean checked = getCheckedItemPositions().get( position, false );
			setItemCheckedState( position, checked );
			return checked;
		}

	}

	public static interface MultiChoiceModeListener extends ActionMode.Callback {

		public void onItemCheckedStateChanged( ActionMode mode, int position, long itemId, boolean checked );

		public void performAction( int menuActionId, int position );

	}

	protected class SetCheckedStateOnItemClick implements OnItemClickListener {

		private OnItemClickListener delegate;

		@Override
		public void onItemClick( AdapterView<?> parentView, View view, int position, long itemId ) {

			if( multiChoiceModeCallback == null || !multiChoiceModeCallback.hasActionMode() ) {
				if( delegate != null ) {
					delegate.onItemClick( parentView, view, position, itemId );
				}
				return;
			}

			boolean checked = multiChoiceModeCallback.toggleItemCheckedState( position );

			if( parentView instanceof ListView ) {
				ListView listView = (ListView) parentView;
				listView.setItemChecked( position, checked );
			}

		}

		public void setDelegate( OnItemClickListener delegate ) {
			this.delegate = delegate;
		}

	}

	public static abstract class SimpleMultiChoiceModeListener implements MultiChoiceModeListener {

		private final int menuResourceId;

		public SimpleMultiChoiceModeListener( int menuResourceId ) {
			this.menuResourceId = menuResourceId;
		}

		@Override
		public boolean onActionItemClicked( ActionMode mode, MenuItem item ) {
			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode( ActionMode mode, Menu menu ) {
			mode.getMenuInflater().inflate( menuResourceId, menu );
			return true;
		}

		@Override
		public void onDestroyActionMode( ActionMode mode ) {
			// Do nothing.
		}

		@Override
		public void onItemCheckedStateChanged( ActionMode mode, int position, long itemId, boolean checked ) {
			// Do nothing.
		}

		@Override
		public boolean onPrepareActionMode( ActionMode mode, Menu menu ) {
			// Don't change a thing.
			return false;
		}

	}

	protected class StartActionModeOnItemLongClick implements OnItemLongClickListener {

		private OnItemLongClickListener delegate;

		@Override
		public boolean onItemLongClick( AdapterView<?> parentView, View view, int position, long itemId ) {

			if( multiChoiceModeCallback == null ) {
				return delegate == null ? false : delegate.onItemLongClick( parentView, view, position, itemId );
			}

			if( multiChoiceModeCallback.hasActionMode() ) {
				return false;
			}

			Context context = getContext();

			if( context instanceof SilentActivity ) {
				( (SilentActivity) context ).startActionMode( multiChoiceModeCallback );
				multiChoiceModeCallback.setItemCheckedState( position, true );
				return true;
			}

			return false;

		}

		public void setDelegate( OnItemLongClickListener delegate ) {
			this.delegate = delegate;
		}

	}

	protected MultiChoiceModeCallback multiChoiceModeCallback;

	private SwipeToDelete swiper;

	public ListView( Context context ) {
		super( context );
		prepareItemListeners();
	}

	public ListView( Context context, AttributeSet attributes ) {
		super( context, attributes );
		prepareItemListeners();
	}

	public ListView( Context context, AttributeSet attributes, int defaultStyle ) {
		super( context, attributes, defaultStyle );
		prepareItemListeners();
	}

	@Override
	public int getCheckedItemCount() {
		int count = 0;
		SparseBooleanArray checkedItems = getCheckedItemPositions();
		for( int i = 0; i < checkedItems.size(); i++ ) {
			if( checkedItems.valueAt( i ) ) {
				count++;
			}
		}
		return count;
	}

	public boolean hasCheckedItems() {
		return getCheckedItemCount() > 0;
	}

	public boolean hasMultipleCheckedItems() {
		return getCheckedItemCount() > 1;
	}

	protected void prepareItemListeners() {
		prepareSwipeToDelete();
		super.setOnItemClickListener( new SetCheckedStateOnItemClick() );
		super.setOnItemLongClickListener( new StartActionModeOnItemLongClick() );
	}

	protected void prepareSwipeToDelete() {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 ) {
			return;
		}
		swiper = new SwipeToDelete( this, new DeleteOnSwipe() );
	}

	public void setItemsChecked( boolean checked ) {
		ListAdapter adapter = getAdapter();
		int length = adapter == null ? 0 : adapter.getCount();
		for( int i = 0; i < length; i++ ) {
			setItemChecked( i, checked );
		}
	}

	public void setMultiChoiceModeListener( MultiChoiceModeListener listener ) {
		multiChoiceModeCallback = new MultiChoiceModeCallback( listener );
		setChoiceMode( CHOICE_MODE_MULTIPLE );
	}

	@Override
	public void setOnItemClickListener( OnItemClickListener listener ) {
		( (SetCheckedStateOnItemClick) getOnItemClickListener() ).setDelegate( listener );
	}

	@Override
	public void setOnItemLongClickListener( OnItemLongClickListener listener ) {
		( (StartActionModeOnItemLongClick) getOnItemLongClickListener() ).setDelegate( listener );
	}

	public void setSwipeEnabled( boolean enabled ) {
		if( enabled ) {
			if( swiper != null ) {
				setOnTouchListener( swiper );
				setOnScrollListener( swiper.makeScrollListener() );
			}
		} else {
			setOnTouchListener( null );
			setOnScrollListener( null );
		}
	}

}
