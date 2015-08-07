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

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationListActivity;
import com.silentcircle.silenttext.fragment.DirectorySearchListFragment;
import com.silentcircle.silenttext.util.Constants;

public class HeaderListView extends RelativeLayout {

	// TODO: Handle listViews with fast scroll
	// TODO: See if there are methods to dispatch to mListView

	private class HeaderListViewOnScrollListener implements AbsListView.OnScrollListener {

		private int previousFirstVisibleItem = -1;
		private int direction = 0;
		private int actualSection = 0;
		private boolean scrollingStart = false;
		private boolean doneMeasuring = false;
		private int lastResetSection = -1;
		private int nextH;
		private int prevH;
		private View previous;
		private View next;
		private final AlphaAnimation fadeOut = new AlphaAnimation( 1f, 0f );
		private boolean noHeaderUpToHeader = false;
		private boolean didScroll = false;

		private boolean mLoading;

		HeaderListViewOnScrollListener() {

		}

		private void addSectionHeader( int actualSection ) {
			View previousHeader = mHeader.getChildAt( 0 );
			if( previousHeader != null ) {
				mHeader.removeViewAt( 0 );
			}

			if( mAdapter.hasSectionHeaderView( actualSection ) ) {
				View header = mAdapter.getSectionHeaderView( actualSection, null, null );
				header.setLayoutParams( new LayoutParams( android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT ) );

				header.measure( MeasureSpec.makeMeasureSpec( mHeader.getWidth(), MeasureSpec.EXACTLY ), MeasureSpec.makeMeasureSpec( 0, MeasureSpec.UNSPECIFIED ) );

				mHeader.getLayoutParams().height = header.getMeasuredHeight();
				header.scrollTo( 0, 0 );
				mHeader.scrollTo( 0, 0 );
				mHeader.addView( header, 0 );
			} else {
				mHeader.getLayoutParams().height = 0;
				mHeader.scrollTo( 0, 0 );
			}

			mScrollView.bringToFront();
		}

		private int getRealFirstVisibleItem( int firstVisibleItem, int visibleItemCount ) {
			if( visibleItemCount == 0 ) {
				return -1;
			}
			int relativeIndex = 0, totalHeight = mListView.getChildAt( 0 ).getTop();
			for( relativeIndex = 0; relativeIndex < visibleItemCount && totalHeight < mHeader.getHeight(); relativeIndex++ ) {
				totalHeight += mListView.getChildAt( relativeIndex ).getHeight();
			}
			int realFVI = Math.max( firstVisibleItem, firstVisibleItem + relativeIndex - 1 );
			return realFVI;
		}

		@Override
		public void onScroll( AbsListView view, int firstVisibleItem1, int visibleItemCount, int totalItemCount ) {
			// if mCurrentSearchQuery is empty string, stay as is and don't need to reload data.
			if( TextUtils.isEmpty( mActivity.getCurrentSearchQuery() ) ) {
				return;
			}
			// Incremental search
			int lastInScreen = firstVisibleItem1 + visibleItemCount;
			if( !mLoading && Constants.mIsIncrementalSearch ) {
				if( lastInScreen == totalItemCount ) {
					mLoading = true;

				}
			}
			if( mLoading ) {
				mLoading = false;
				// do incremental loading
				mActivity.loadingData();
				Fragment fragment = mActivity.getFragmentManager().findFragmentById( R.id.chats );
				if( fragment instanceof DirectorySearchListFragment ) {
					SectionAdapter adapter = ( (DirectorySearchListFragment) fragment ).getAdapter();
					adapter.notifyDataSetChanged();
				}
			}

			int firstVisibleItem = firstVisibleItem1;
			if( mExternalOnScrollListener != null ) {
				mExternalOnScrollListener.onScroll( view, firstVisibleItem, visibleItemCount, totalItemCount );
			}

			if( !didScroll ) {
				return;
			}

			firstVisibleItem -= mListView.getHeaderViewsCount();
			if( firstVisibleItem < 0 ) {
				mHeader.removeAllViews();
				return;
			}

			updateScrollBar();
			if( visibleItemCount > 0 && firstVisibleItem == 0 && mHeader.getChildAt( 0 ) == null ) {
				addSectionHeader( 0 );
				lastResetSection = 0;
			}

			int realFirstVisibleItem = getRealFirstVisibleItem( firstVisibleItem, visibleItemCount );
			if( totalItemCount > 0 && previousFirstVisibleItem != realFirstVisibleItem ) {
				direction = realFirstVisibleItem - previousFirstVisibleItem;

				actualSection = mAdapter.getSection( realFirstVisibleItem );

				boolean currIsHeader = mAdapter.isSectionHeader( realFirstVisibleItem );
				boolean prevHasHeader = mAdapter.hasSectionHeaderView( actualSection - 1 );
				boolean nextHasHeader = mAdapter.hasSectionHeaderView( actualSection + 1 );
				boolean currHasHeader = mAdapter.hasSectionHeaderView( actualSection );
				boolean currIsLast = mAdapter.getRowInSection( realFirstVisibleItem ) == mAdapter.numberOfRows( actualSection ) - 1;
				boolean prevHasRows = mAdapter.numberOfRows( actualSection - 1 ) > 0;
				boolean currIsFirst = mAdapter.getRowInSection( realFirstVisibleItem ) == 0;

				boolean needScrolling = currIsFirst && !currHasHeader && prevHasHeader && realFirstVisibleItem != firstVisibleItem;
				boolean needNoHeaderUpToHeader = currIsLast && currHasHeader && !nextHasHeader && realFirstVisibleItem == firstVisibleItem && Math.abs( mListView.getChildAt( 0 ).getTop() ) >= mListView.getChildAt( 0 ).getHeight() / 2;

				noHeaderUpToHeader = false;
				if( currIsHeader && !prevHasHeader && firstVisibleItem >= 0 ) {
					resetHeader( direction < 0 ? actualSection - 1 : actualSection );
				} else if( currIsHeader && firstVisibleItem > 0 || needScrolling ) {
					if( !prevHasRows ) {
						resetHeader( actualSection - 1 );
					}
					startScrolling();
				} else if( needNoHeaderUpToHeader ) {
					noHeaderUpToHeader = true;
				} else if( lastResetSection != actualSection ) {
					resetHeader( actualSection );
				}

				previousFirstVisibleItem = realFirstVisibleItem;
			}

			if( scrollingStart ) {
				int scrolled = realFirstVisibleItem >= firstVisibleItem ? mListView.getChildAt( realFirstVisibleItem - firstVisibleItem ).getTop() : 0;

				if( !doneMeasuring ) {
					setMeasurements( realFirstVisibleItem, firstVisibleItem );
				}

				int headerH = doneMeasuring ? ( prevH - nextH ) * direction * Math.abs( scrolled ) / ( direction < 0 ? nextH : prevH ) + ( direction > 0 ? nextH : prevH ) : 0;

				mHeader.scrollTo( 0, -Math.min( 0, scrolled - headerH ) );
				if( doneMeasuring && headerH != mHeader.getLayoutParams().height ) {
					LayoutParams p = (LayoutParams) ( direction < 0 ? next.getLayoutParams() : previous.getLayoutParams() );
					p.topMargin = headerH - p.height;
					mHeader.getLayoutParams().height = headerH;
					mHeader.requestLayout();
				}
			}

			if( noHeaderUpToHeader ) {
				if( lastResetSection != actualSection ) {
					addSectionHeader( actualSection );
					lastResetSection = actualSection + 1;
				}
				mHeader.scrollTo( 0, mHeader.getLayoutParams().height - ( mListView.getChildAt( 0 ).getHeight() + mListView.getChildAt( 0 ).getTop() ) );
			}
		}

		@Override
		public void onScrollStateChanged( AbsListView view, int scrollState ) {
			if( mExternalOnScrollListener != null ) {
				mExternalOnScrollListener.onScrollStateChanged( view, scrollState );
			}
			didScroll = true;
		}

		private void resetHeader( int section ) {
			scrollingStart = false;
			addSectionHeader( section );
			mHeader.requestLayout();
			lastResetSection = section;
		}

		private void setMeasurements( int realFirstVisibleItem, int firstVisibleItem ) {

			if( direction > 0 ) {
				nextH = realFirstVisibleItem >= firstVisibleItem ? mListView.getChildAt( realFirstVisibleItem - firstVisibleItem ).getMeasuredHeight() : 0;
			}

			previous = mHeader.getChildAt( 0 );
			prevH = previous != null ? previous.getMeasuredHeight() : mHeader.getHeight();

			if( direction < 0 ) {
				if( lastResetSection != actualSection - 1 ) {
					addSectionHeader( Math.max( 0, actualSection - 1 ) );
					next = mHeader.getChildAt( 0 );
				}
				nextH = mHeader.getChildCount() > 0 ? mHeader.getChildAt( 0 ).getMeasuredHeight() : 0;
				mHeader.scrollTo( 0, prevH );
			}
			doneMeasuring = previous != null && prevH > 0 && nextH > 0;
		}

		private void startScrolling() {
			scrollingStart = true;
			doneMeasuring = false;
			lastResetSection = -1;
		}

		private void updateScrollBar() {
			if( mHeader != null && mListView != null && mScrollView != null ) {
				int offset = mListView.computeVerticalScrollOffset();
				int range = mListView.computeVerticalScrollRange();
				int extent = mListView.computeVerticalScrollExtent();
				mScrollView.setVisibility( extent >= range ? View.INVISIBLE : View.VISIBLE );
				if( extent >= range ) {
					return;
				}
				int top = range == 0 ? mListView.getHeight() : mListView.getHeight() * offset / range;
				int bottom = range == 0 ? 0 : mListView.getHeight() - mListView.getHeight() * ( offset + extent ) / range;
				mScrollView.setPadding( 0, top, 0, bottom );
				fadeOut.reset();
				fadeOut.setFillBefore( true );
				fadeOut.setFillAfter( true );
				fadeOut.setStartOffset( FADE_DELAY );
				fadeOut.setDuration( FADE_DURATION );
				mScrollView.clearAnimation();
				mScrollView.startAnimation( fadeOut );
			}
		}
	}

	protected class InternalListView extends ListView {

		public InternalListView( Context context, AttributeSet attrs ) {
			super( context, attrs );
		}

		@Override
		protected int computeVerticalScrollExtent() {
			return super.computeVerticalScrollExtent();
		}

		@Override
		protected int computeVerticalScrollOffset() {
			return super.computeVerticalScrollOffset();
		}

		@Override
		protected int computeVerticalScrollRange() {
			return super.computeVerticalScrollRange();
		}
	}

	private static final int FADE_DELAY = 1000;
	private static final int FADE_DURATION = 2000;
	InternalListView mListView;
	SectionAdapter mAdapter;
	RelativeLayout mHeader;

	FrameLayout mScrollView;

	AbsListView.OnScrollListener mExternalOnScrollListener;

	ConversationListActivity mActivity;

	public HeaderListView( ConversationListActivity context ) {
		super( context );
		init( context, null );
	}

	public HeaderListView( ConversationListActivity context, AttributeSet attrs ) {
		super( context, attrs );
		init( context, attrs );
	}

	public void addHeaderView( View v ) {
		mListView.addHeaderView( v );
	}

	private float dpToPx( float dp ) {
		return TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics() );
	}

	public ListView getListView() {
		return mListView;
	}

	public ConversationListActivity getmActivity() {
		return mActivity;
	}

	private void init( ConversationListActivity context, AttributeSet attrs ) {
		mActivity = context;
		mListView = new InternalListView( getContext(), attrs );
		LayoutParams listParams = new LayoutParams( android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT );
		listParams.addRule( ALIGN_PARENT_TOP );
		mListView.setLayoutParams( listParams );
		mListView.setOnScrollListener( new HeaderListViewOnScrollListener() );
		mListView.setVerticalScrollBarEnabled( false );
		mListView.setOnItemClickListener( new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
				if( mAdapter != null ) {
					mAdapter.onItemClick( parent, view, position, id );
				}
			}
		} );
		addView( mListView );

		mHeader = new RelativeLayout( getContext() );
		LayoutParams headerParams = new LayoutParams( android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT );
		headerParams.addRule( ALIGN_PARENT_TOP );
		mHeader.setLayoutParams( headerParams );
		mHeader.setGravity( Gravity.BOTTOM );
		addView( mHeader );

		// The list view's scroll bar can be hidden by the header, so we display our own scroll bar
		// instead
		Drawable scrollBarDrawable = getResources().getDrawable( R.drawable.scrollbar_handle_holo_light );
		mScrollView = new FrameLayout( getContext() );
		LayoutParams scrollParams = new LayoutParams( scrollBarDrawable.getIntrinsicWidth(), android.view.ViewGroup.LayoutParams.MATCH_PARENT );
		scrollParams.addRule( ALIGN_PARENT_RIGHT );
		scrollParams.rightMargin = (int) dpToPx( 2 );
		mScrollView.setLayoutParams( scrollParams );

		ImageView scrollIndicator = new ImageView( context );
		scrollIndicator.setLayoutParams( new LayoutParams( android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT ) );
		scrollIndicator.setImageDrawable( scrollBarDrawable );
		scrollIndicator.setScaleType( ScaleType.FIT_XY );
		mScrollView.addView( scrollIndicator );
		mScrollView.setVisibility( INVISIBLE );

		addView( mScrollView );
	}

	public void setAdapter( SectionAdapter adapter ) {
		mAdapter = adapter;
		mListView.setAdapter( adapter );
	}

	public void setOnScrollListener( AbsListView.OnScrollListener l ) {
		mExternalOnScrollListener = l;
	}
}
