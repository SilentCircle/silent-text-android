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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.graphics.AvatarUtils;
import com.silentcircle.silenttext.graphics.CircleClipDrawable;
import com.silentcircle.silenttext.model.Contact;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.task.LoadAvatarTask;
import com.silentcircle.silenttext.util.AsyncUtils;

public class AvatarView extends ImageView implements OnClickListener {

	static class QuickContactTask extends AsyncTask<String, Void, Boolean> {

		protected final ContactRepository contacts;
		protected final String username;
		protected final OnClickListener delegate;
		protected final SoftReference<View> viewReference;

		public QuickContactTask( ContactRepository contacts, String username, OnClickListener delegate, View view ) {
			this.contacts = contacts;
			this.username = username;
			this.delegate = delegate;
			viewReference = new SoftReference<View>( view );
		}

		@Override
		protected Boolean doInBackground( String... args ) {
			return Boolean.valueOf( contacts.isWritable() || contacts.exists( username ) );
		}

		@Override
		protected void onPostExecute( Boolean shouldShowQuickContact ) {
			View view = viewReference.get();
			if( view == null ) {
				return;
			}
			if( shouldShowQuickContact.booleanValue() ) {
				contacts.showQuickContact( view, username );
			} else {
				if( delegate != null ) {
					delegate.onClick( view );
				}
			}
		}

	}

	private String username;
	private OnClickListener secondaryOnClickListener;

	protected final List<AsyncTask<?, ?, ?>> tasks = new ArrayList<AsyncTask<?, ?, ?>>();

	private boolean interactive = true;

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

	private void cancelTasks() {
		while( !tasks.isEmpty() ) {
			AsyncTask<?, ?, ?> task = tasks.get( 0 );
			if( task != null ) {
				task.cancel( true );
				task = null;
			}
			tasks.remove( 0 );
		}
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	private void disableHardwareAcceleration() {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
			setLayerType( View.LAYER_TYPE_SOFTWARE, null );
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

	private ContactRepository getDefaultContactRepository() {
		return SilentTextApplication.from( getContext() ).getContacts();
	}

	public boolean isInteractive() {
		return interactive;
	}

	public void loadAvatar( String username ) {
		loadAvatar( username, R.dimen.avatar_normal );
	}

	public void loadAvatar( String username, ContactRepository repository, int avatarSizeResourceID ) {
		Bitmap bitmap = LoadAvatarTask.getCached( username );
		if( bitmap != null ) {
			setAvatar( bitmap );
			return;
		}
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ) {
			loadAvatarSync( username, repository, avatarSizeResourceID );
		} else {
			loadAvatarAsync( username, repository, avatarSizeResourceID );
		}
	}

	public void loadAvatar( String username, int avatarSizeResourceID ) {
		loadAvatar( username, getDefaultContactRepository(), avatarSizeResourceID );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	private void loadAvatarAsync( String userID, ContactRepository repository, int avatarSizeResourceID ) {

		if( !LoadAvatarTask.isNecessary( this, userID ) ) {
			return;
		}

		if( !tasks.isEmpty() ) {
			return;
		}

		cancelTasks();

		tasks.add( AsyncUtils.execute( new LoadAvatarTask( this, repository, userID, avatarSizeResourceID ) {

			@Override
			protected void onCancelled() {
				super.onCancelled();
				tasks.remove( this );
			}

			@Override
			protected void onPostExecute( Bitmap bitmap ) {
				super.onPostExecute( bitmap );
				tasks.remove( this );
			}

		} ) );

	}

	/**
	 * Load avatars synchronously on Android 2.x because on-screen views may be recycled, causing
	 * the wrong avatar to flash until the correct one is loaded.
	 */
	private void loadAvatarSync( String username, ContactRepository repository, int avatarSizeResourceID ) {
		setAvatar( AvatarUtils.getAvatar( getContext(), repository, username, avatarSizeResourceID ) );
	}

	@Override
	public void onClick( View view ) {
		if( !isInteractive() ) {
			if( secondaryOnClickListener != null ) {
				secondaryOnClickListener.onClick( view );
			}
			return;
		}
		if( username == null ) {
			return;
		}
		tasks.add( AsyncUtils.execute( new QuickContactTask( getContacts(), username, secondaryOnClickListener, view ) {

			@Override
			protected void onCancelled() {
				super.onCancelled();
				tasks.remove( this );
			}

			@Override
			protected void onPostExecute( Boolean shouldShowQuickContact ) {
				super.onPostExecute( shouldShowQuickContact );
				tasks.remove( this );
			}

		} ) );
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		cancelTasks();
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );
		int [] states = getDrawableState();
		for( int i = 0; i < states.length; i++ ) {
			switch( states[i] ) {
				case android.R.attr.state_pressed:
				case android.R.attr.state_hovered:
					canvas.drawColor( 0x50808080, Mode.DARKEN );
					break;
			}
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		setAvatar( null );
	}

	@Override
	public void onWindowFocusChanged( boolean hasWindowFocus ) {
		super.onWindowFocusChanged( hasWindowFocus );
		if( !hasWindowFocus ) {
			LoadAvatarTask.flagForRefresh( this );
			LoadAvatarTask.forget( username );
		}
	}

	public void setAvatar( Bitmap avatar ) {
		if( avatar != null ) {
			setClippedBitmap( avatar );
		} else {
			setClippedResource( R.drawable.ic_avatar_placeholder );
		}
	}

	public void setClippedBitmap( Bitmap bitmap ) {
		setClippedDrawable( new BitmapDrawable( getResources(), bitmap ) );
	}

	public void setClippedDrawable( Drawable drawable ) {
		disableHardwareAcceleration();
		setImageDrawable( new CircleClipDrawable( drawable, getResources(), R.color.silent_dark_grey, R.color.silent_translucent_dark_grey, R.dimen.stroke_normal ) );
	}

	public void setClippedResource( int drawableResourceID ) {
		setClippedDrawable( getResources().getDrawable( drawableResourceID ) );
	}

	public void setContact( Contact contact ) {
		setContact( contact != null ? contact.getUsername() : null );
	}

	public void setContact( Contact contact, ContactRepository repository ) {
		setContact( contact != null ? contact.getUsername() : null, repository );
	}

	public void setContact( Contact contact, ContactRepository repository, int avatarSizeResourceID ) {
		setContact( contact != null ? contact.getUsername() : null, repository, avatarSizeResourceID );
	}

	public void setContact( Contact contact, int avatarSizeResourceID ) {
		setContact( contact != null ? contact.getUsername() : null, avatarSizeResourceID );
	}

	public void setContact( String username ) {
		setContact( username, getDefaultContactRepository() );
	}

	public void setContact( String username, ContactRepository repository ) {
		setContact( username, repository, R.dimen.avatar_normal );
	}

	public void setContact( String username, ContactRepository repository, int avatarSizeResourceID ) {
		if( username != null && this.username != username ) {
			setTag( R.id.username, username );
			this.username = username;
			setContentDescription( username );
			loadAvatar( username, repository, avatarSizeResourceID );
		}
	}

	public void setContact( String username, int avatarSizeResourceID ) {
		setContact( username, getDefaultContactRepository(), avatarSizeResourceID );
	}

	public void setInteractive( boolean interactive ) {
		this.interactive = interactive;
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
