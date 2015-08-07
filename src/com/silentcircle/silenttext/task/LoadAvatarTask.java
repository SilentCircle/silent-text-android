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
package com.silentcircle.silenttext.task;

import java.lang.ref.SoftReference;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.graphics.AvatarUtils;
import com.silentcircle.silenttext.repository.ContactRepository;
import com.silentcircle.silenttext.view.AvatarView;

public class LoadAvatarTask extends AsyncTask<String, Void, Bitmap> {

	private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>( 1 * 1024 * 1024 ) {

		@Override
		protected int sizeOf( String key, Bitmap value ) {
			return value.getByteCount();
		}

	};

	public synchronized static void cache( String userID, Bitmap bitmap ) {
		CACHE.put( userID, bitmap );
	}

	public static void flagForRefresh( AvatarView view ) {
		if( view != null ) {
			view.setTag( R.id.pending, Boolean.TRUE );
		}
	}

	public static void forget( AvatarView view ) {
		if( view != null ) {
			view.setTag( R.id.username, null );
		}
	}

	public synchronized static void forget( String userID ) {
		CACHE.remove( userID );
	}

	public synchronized static Bitmap getCached( String userID ) {
		return CACHE.get( userID );
	}

	public static boolean isCached( String userID ) {
		return getCached( userID ) != null;
	}

	public static boolean isNecessary( AvatarView view, String userID ) {
		if( isRefreshing( view ) ) {
			return true;
		}
		return view != null && userID != null;
	}

	public static boolean isRefreshing( AvatarView view ) {
		if( view == null ) {
			return false;
		}
		Object pending = view.getTag( R.id.pending );
		return pending != null && pending instanceof Boolean && ( (Boolean) pending ).booleanValue();
	}

	private final SoftReference<AvatarView> view;
	private final ContactRepository repository;
	private final String userID;
	private final int sizeResourceID;

	public LoadAvatarTask( AvatarView view, ContactRepository repository, String userID ) {
		this( view, repository, userID, R.dimen.avatar_normal );
	}

	public LoadAvatarTask( AvatarView view, ContactRepository repository, String userID, int sizeResourceID ) {
		this.view = new SoftReference<AvatarView>( view );
		this.repository = repository;
		this.userID = userID;
		this.sizeResourceID = sizeResourceID;
		view.setTag( R.id.pending, null );
	}

	@Override
	protected Bitmap doInBackground( String... params ) {
		Bitmap bitmap = getCached( userID );
		if( bitmap == null ) {
			bitmap = AvatarUtils.getAvatar( view.get().getContext(), repository, userID, sizeResourceID );
			if( bitmap != null ) {
				cache( userID, bitmap );
			}
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute( Bitmap bitmap ) {
		AvatarView v = view.get();
		if( v != null ) {
			Object tag = v.getTag( R.id.username );
			if( userID.equals( tag ) ) {
				v.setAvatar( bitmap );
				v.setTag( R.id.pending, null );
			}
		}
	}

}
