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
package com.silentcircle.silenttext.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.silentcircle.silenttext.R;

public class WarningView extends RelativeLayout implements OnClickListener {

	static class Views {

		public TextView title;
		public LinearLayout details;
		public TextView description;
		public LinearLayout actions;
		public ImageView cancel;

	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
	public static void setDrawables( TextView view, int start, int top, int end, int bottom ) {
		view.setCompoundDrawablesRelativeWithIntrinsicBounds( start, top, end, bottom );
	}

	public static void setDrawablesSupport( TextView view, int start, int top, int end, int bottom ) {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
			view.setCompoundDrawablesWithIntrinsicBounds( start, top, end, bottom );
		} else {
			setDrawables( view, start, top, end, bottom );
		}
	}

	private Views views;
	private OnClickListener onCancelled;

	public WarningView( Context context ) {
		super( context );
		setOnClickListener( this );
	}

	public WarningView( Context context, AttributeSet attrs ) {
		super( context, attrs );
		setOnClickListener( this );
	}

	public WarningView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		setOnClickListener( this );
	}

	public void addAction( int actionIconResourceID, int actionTitleResourceID, OnClickListener onActionClicked ) {
		Resources resources = getResources();
		addAction( actionIconResourceID, resources.getString( actionTitleResourceID ), onActionClicked );
	}

	public void addAction( int actionIconResourceID, String actionTitle, OnClickListener onActionClicked ) {
		Button action = (Button) inflate( getContext(), R.layout.subtle_button, null );
		setDrawablesSupport( action, actionIconResourceID, 0, 0, 0 );
		action.setOnClickListener( onActionClicked );
		action.setText( actionTitle );
		getViews().actions.addView( action );
	}

	protected Views getViews() {
		if( views == null ) {
			views = new Views();
			views.title = (TextView) findViewById( R.id.warning_title );
			views.details = (LinearLayout) findViewById( R.id.warning_details );
			views.description = (TextView) findViewById( R.id.warning_description );
			views.actions = (LinearLayout) findViewById( R.id.warning_actions );
			views.cancel = (ImageView) findViewById( R.id.warning_cancel );
			views.cancel.setOnClickListener( this );
		}
		return views;
	}

	@Override
	public void onClick( View view ) {
		Views v = getViews();
		if( view == v.cancel ) {
			if( onCancelled != null ) {
				onCancelled.onClick( this );
			}
			( (ViewGroup) getParent() ).removeView( this );
			return;
		}
		v.details.setVisibility( VISIBLE );
	}

	public void setWarning( int titleResourceID, int descriptionResourceID, OnClickListener onCancelled ) {
		Resources resources = getResources();
		setWarning( resources.getString( titleResourceID ), resources.getString( descriptionResourceID ), onCancelled );
	}

	public void setWarning( String title, String description, OnClickListener onCancelled ) {
		Views v = getViews();
		v.title.setText( title );
		v.description.setText( description );
		v.actions.removeAllViews();
		this.onCancelled = onCancelled;
	}

}
