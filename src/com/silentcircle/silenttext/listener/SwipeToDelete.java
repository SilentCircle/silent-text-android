/*
 * Copyright 2013 Google Inc. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.silentcircle.silenttext.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

@TargetApi( Build.VERSION_CODES.HONEYCOMB_MR1 )
public class SwipeToDelete implements View.OnTouchListener {

	public interface DismissCallbacks {

		public boolean canDismiss( int position );

		public void onDismiss( ListView listView, int [] reverseSortedPositions );

	}

	class PendingDismissData implements Comparable<PendingDismissData> {

		public int position;
		public View view;

		public PendingDismissData( int position, View view ) {
			this.position = position;
			this.view = view;
		}

		@Override
		public int compareTo( PendingDismissData other ) {
			return other.position - position;
		}

	}

	private final int mSlop;
	private final int mMinFlingVelocity;

	private final int mMaxFlingVelocity;
	private final long mAnimationTime;

	ListView mListView;

	DismissCallbacks mCallbacks;
	private int mViewWidth = 1;

	List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
	int mDismissAnimationRefCount = 0;
	private float mDownX;
	private boolean mSwiping;
	private VelocityTracker mVelocityTracker;
	private int mDownPosition;

	private View mDownView;

	private boolean mPaused;

	public SwipeToDelete( ListView listView, DismissCallbacks callbacks ) {
		ViewConfiguration vc = ViewConfiguration.get( listView.getContext() );
		mSlop = vc.getScaledTouchSlop();
		mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		mAnimationTime = listView.getContext().getResources().getInteger( android.R.integer.config_shortAnimTime );
		mListView = listView;
		mCallbacks = callbacks;
	}

	public AbsListView.OnScrollListener makeScrollListener() {

		return new AbsListView.OnScrollListener() {

			@Override
			public void onScroll( AbsListView absListView, int i, int i1, int i2 ) {
				// Ignore.
			}

			@Override
			public void onScrollStateChanged( AbsListView absListView, int scrollState ) {
				setEnabled( scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL );
			}

		};

	}

	@Override
	public boolean onTouch( View view, MotionEvent motionEvent ) {

		if( mViewWidth < 2 ) {
			mViewWidth = mListView.getWidth();
		}

		switch( motionEvent.getActionMasked() ) {

			case MotionEvent.ACTION_DOWN: {

				if( mPaused ) {
					return false;
				}

				Rect rect = new Rect();
				int childCount = mListView.getChildCount();
				int [] listViewCoords = new int [2];
				mListView.getLocationOnScreen( listViewCoords );
				int x = (int) motionEvent.getRawX() - listViewCoords[0];
				int y = (int) motionEvent.getRawY() - listViewCoords[1];
				View child;
				for( int i = 0; i < childCount; i++ ) {
					child = mListView.getChildAt( i );
					child.getHitRect( rect );
					if( rect.contains( x, y ) ) {
						mDownView = child;
						break;
					}
				}

				if( mDownView != null ) {
					mDownX = motionEvent.getRawX();
					mDownPosition = mListView.getPositionForView( mDownView );
					if( mCallbacks.canDismiss( mDownPosition ) ) {
						mVelocityTracker = VelocityTracker.obtain();
						mVelocityTracker.addMovement( motionEvent );
					}
				}
				view.onTouchEvent( motionEvent );
				return true;

			}

			case MotionEvent.ACTION_UP: {

				if( mVelocityTracker == null ) {
					break;
				}

				float deltaX = motionEvent.getRawX() - mDownX;
				mVelocityTracker.addMovement( motionEvent );
				mVelocityTracker.computeCurrentVelocity( 1000 );
				float velocityX = mVelocityTracker.getXVelocity();
				float absVelocityX = Math.abs( velocityX );
				float absVelocityY = Math.abs( mVelocityTracker.getYVelocity() );
				boolean dismiss = false;
				boolean dismissRight = false;
				if( Math.abs( deltaX ) > mViewWidth / 2 ) {
					dismiss = true;
					dismissRight = deltaX > 0;
				} else if( mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity && absVelocityY < absVelocityX ) {
					dismiss = velocityX < 0 == deltaX < 0;
					dismissRight = mVelocityTracker.getXVelocity() > 0;
				}

				if( dismiss ) {

					final View downView = mDownView;
					final int downPosition = mDownPosition;
					++mDismissAnimationRefCount;
					mDownView.animate().translationX( dismissRight ? mViewWidth : -mViewWidth ).alpha( 0 ).setDuration( mAnimationTime ).setListener( new AnimatorListenerAdapter() {

						@Override
						public void onAnimationEnd( Animator animation ) {
							performDismiss( downView, downPosition );
						}

					} );

				} else {
					mDownView.animate().translationX( 0 ).alpha( 1 ).setDuration( mAnimationTime ).setListener( null );
				}

				mVelocityTracker.recycle();
				mVelocityTracker = null;
				mDownX = 0;
				mDownView = null;
				mDownPosition = AdapterView.INVALID_POSITION;
				mSwiping = false;
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				if( mVelocityTracker == null || mPaused ) {
					break;
				}

				mVelocityTracker.addMovement( motionEvent );
				float deltaX = motionEvent.getRawX() - mDownX;
				if( Math.abs( deltaX ) > mSlop ) {
					mSwiping = true;
					mListView.requestDisallowInterceptTouchEvent( true );

					MotionEvent cancelEvent = MotionEvent.obtain( motionEvent );
					cancelEvent.setAction( MotionEvent.ACTION_CANCEL | motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT );
					mListView.onTouchEvent( cancelEvent );
					cancelEvent.recycle();
				}

				if( mSwiping ) {
					mDownView.setTranslationX( deltaX );
					mDownView.setAlpha( Math.max( 0f, Math.min( 1f, 1f - 2f * Math.abs( deltaX ) / mViewWidth ) ) );
					return true;
				}
				break;
			}
		}
		return false;
	}

	void performDismiss( final View dismissView, final int dismissPosition ) {

		final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
		final int originalHeight = dismissView.getHeight();

		ValueAnimator animator = ValueAnimator.ofInt( originalHeight, 1 ).setDuration( mAnimationTime );

		animator.addListener( new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd( Animator animation ) {
				--mDismissAnimationRefCount;
				if( mDismissAnimationRefCount == 0 ) {
					Collections.sort( mPendingDismisses );

					int [] dismissPositions = new int [mPendingDismisses.size()];
					for( int i = mPendingDismisses.size() - 1; i >= 0; i-- ) {
						dismissPositions[i] = mPendingDismisses.get( i ).position;
					}
					mCallbacks.onDismiss( mListView, dismissPositions );

					ViewGroup.LayoutParams lp;
					for( PendingDismissData pendingDismiss : mPendingDismisses ) {
						pendingDismiss.view.setAlpha( 1f );
						pendingDismiss.view.setTranslationX( 0 );
						lp = pendingDismiss.view.getLayoutParams();
						lp.height = originalHeight;
						pendingDismiss.view.setLayoutParams( lp );
					}

					mPendingDismisses.clear();
				}
			}
		} );

		animator.addUpdateListener( new ValueAnimator.AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate( ValueAnimator valueAnimator ) {
				dismissView.setLayoutParams( lp );
			}

		} );

		mPendingDismisses.add( new PendingDismissData( dismissPosition, dismissView ) );
		animator.start();

	}

	public void setEnabled( boolean enabled ) {
		mPaused = !enabled;
	}

}
