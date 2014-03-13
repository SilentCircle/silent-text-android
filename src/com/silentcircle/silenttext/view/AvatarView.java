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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.graphics.HandleDrawable;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.task.LoadAvatarTask;

public class AvatarView extends ImageView implements OnClickListener {

	private final HandleDrawable handle = new HandleDrawable( 24, 0xCC888888 );
	private String username;
	private int overlay = 0x80e46226;
	private OnClickListener secondaryOnClickListener;
	protected LoadAvatarTask task;

	public AvatarView( Context context ) {
		super( context );
		setOnClickListener( this );
	}

	public AvatarView( Context context, AttributeSet attrs ) {
		super( context, attrs );
		setOnClickListener( this );
	}

	public AvatarView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		setOnClickListener( this );
	}

	private void cancelTask() {
		if( task != null ) {
			task.cancel( true );
			task = null;
		}
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		invalidate();
	}

	private ContactRepository getContacts() {
		return SilentTextApplication.from( getContext() ).getContacts();
	}

	public void loadAvatar( String userID ) {

		if( !LoadAvatarTask.isNecessary( this, userID ) ) {
			return;
		}

		if( task != null ) {
			return;
		}

		if( !LoadAvatarTask.isRefreshing( this ) ) {
			setAvatar( null );
		}

		cancelTask();

		task = new LoadAvatarTask( this, getContacts(), userID ) {

			@Override
			protected void onCancelled() {
				super.onCancelled();
				task = null;
			}

			@Override
			protected void onPostExecute( Bitmap bitmap ) {
				super.onPostExecute( bitmap );
				task = null;
			}

		};
		task.execute();
	}

	@Override
	public void onClick( View view ) {
		if( username == null ) {
			return;
		}
		if( !getContacts().exists( username ) && !getContacts().isWritable() ) {
			if( secondaryOnClickListener != null ) {
				secondaryOnClickListener.onClick( this );
			}
		}
		getContacts().showQuickContact( view, username );
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		cancelTask();
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );
		int [] states = getDrawableState();
		for( int i = 0; i < states.length; i++ ) {
			if( states[i] == android.R.attr.state_pressed ) {
				canvas.drawColor( overlay );
				break;
			}
		}
		handle.draw( canvas );
	}

	@Override
	public void onWindowFocusChanged( boolean hasWindowFocus ) {
		super.onWindowFocusChanged( hasWindowFocus );
		if( !hasWindowFocus ) {
			LoadAvatarTask.flagForRefresh( this );
		}
	}

	public void setAvatar( Bitmap avatar ) {
		if( avatar != null ) {
			setImageBitmap( avatar );
		} else {
			setImageResource( R.drawable.ic_avatar_placeholder );
		}
	}

	public void setContact( Contact contact ) {
		if( contact == null ) {
			return;
		}
		setContactUsername( contact.getUsername() );
		loadAvatar( contact.getUsername() );
	}

	public void setContactUsername( String username ) {
		this.username = username;
	}

	public void setOverlayColor( int overlay ) {
		Paint paint = new Paint();
		paint.setColor( overlay );
		paint.setAlpha( 0x80 );
		this.overlay = paint.getColor();
	}

	public void setSecondaryOnClickListener( OnClickListener secondaryOnClickListener ) {
		this.secondaryOnClickListener = secondaryOnClickListener;
	}

	@Override
	public void setTag( Object tag ) {
		super.setTag( tag );
		if( tag != null && tag instanceof Contact ) {
			setContact( (Contact) tag );
		}
	}

}
