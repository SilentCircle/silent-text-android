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
package com.silentcircle.silenttext.listener;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.silentcircle.silenttext.view.ListView.MultiChoiceModeListener;
import com.silentcircle.silenttext.view.adapter.ListAdapter;

public class MultipleChoiceSelector<T> implements MultiChoiceModeListener {

	public static interface ActionPerformer {

		public void onActionPerformed();

		public void performAction( int menuActionId, int position );

	}

	private final MultipleChoiceSelector.ActionPerformer performer;
	protected final ListAdapter<T> adapter;
	private final int menuResourceID;

	public MultipleChoiceSelector( ListAdapter<T> adapter, int menuResourceID, MultipleChoiceSelector.ActionPerformer performer ) {
		this.adapter = adapter;
		this.menuResourceID = menuResourceID;
		this.performer = performer;
	}

	@Override
	public boolean onActionItemClicked( ActionMode mode, MenuItem item ) {
		mode.finish();
		performer.onActionPerformed();
		return true;
	}

	@Override
	public boolean onCreateActionMode( ActionMode mode, Menu menu ) {
		mode.getMenuInflater().inflate( menuResourceID, menu );
		adapter.setInChoiceMode( true );
		return true;
	}

	@Override
	public void onDestroyActionMode( ActionMode mode ) {
		adapter.setInChoiceMode( false );
	}

	@Override
	public void onItemCheckedStateChanged( ActionMode mode, int position, long itemId, boolean checked ) {
		// Do nothing.
	}

	@Override
	public boolean onPrepareActionMode( ActionMode mode, Menu menu ) {
		return false;
	}

	@Override
	public void performAction( int menuActionId, int position ) {
		performer.performAction( menuActionId, position );
	}

}
