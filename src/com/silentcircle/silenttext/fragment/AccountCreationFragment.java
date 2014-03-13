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
package com.silentcircle.silenttext.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Checkable;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.silentcircle.silenttext.R;

public abstract class AccountCreationFragment extends Fragment implements HasFlow {

	protected static void hideSoftKeyboard( View v ) {
		if( v == null ) {
			return;
		}
		InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		if( imm == null ) {
			return;
		}
		imm.hideSoftInputFromWindow( v.getWindowToken(), 0 );
		v.clearFocus();
	}

	protected static void hideSoftKeyboard( View parent, int... viewResourceIDs ) {
		if( parent == null ) {
			return;
		}
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			hideSoftKeyboard( parent.findViewById( id ) );
		}
	}

	protected static boolean isChecked( View parent, int viewResourceID ) {
		try {
			return ( (Checkable) parent.findViewById( viewResourceID ) ).isChecked();
		} catch( Throwable exception ) {
			return false;
		}
	}

	protected static boolean isEmpty( View parent, int viewResourceID ) {
		try {
			CharSequence value = valueOf( parent, viewResourceID );
			return value == null || value.length() <= 0;
		} catch( Throwable exception ) {
			return true;
		}
	}

	protected static void onClick( View parent, int viewResourceID, OnClickListener onClickListener ) {
		try {
			parent.findViewById( viewResourceID ).setOnClickListener( onClickListener );
		} catch( Throwable exception ) {
			// Fail silently because we expect the element may not exist.
		}
	}

	protected static void put( View parent, int viewResourceID, CharSequence value ) {
		put( parent, viewResourceID, value, 0 );
	}

	protected static void put( View parent, int viewResourceID, CharSequence value, int defaultValue ) {
		try {
			CharSequence v = value == null ? defaultValue == 0 ? null : parent.getResources().getString( defaultValue ) : value;
			( (TextView) parent.findViewById( viewResourceID ) ).setText( v );
		} catch( Throwable exception ) {
			// We don't particularly care if we drop a value here.
		}
	}

	protected static void setText( View parent, int viewResourceID, CharSequence text ) {
		try {
			( (TextView) parent.findViewById( viewResourceID ) ).setText( text );
		} catch( Throwable exception ) {
			// Don't worry about it. If something went wrong, let's just let it be.
		}
	}

	protected static void setText( View parent, int viewResourceID, int stringResourceID ) {
		try {
			( (TextView) parent.findViewById( viewResourceID ) ).setText( stringResourceID );
		} catch( Throwable exception ) {
			// Don't worry about it. If something went wrong, let's just let it be.
		}
	}

	protected static void setVisibleIf( boolean condition, View parent, int... viewResourceIDs ) {
		int visibility = condition ? View.VISIBLE : View.INVISIBLE;
		for( int i = 0; i < viewResourceIDs.length; i++ ) {
			int id = viewResourceIDs[i];
			try {
				parent.findViewById( id ).setVisibility( visibility );
			} catch( Throwable exception ) {
				// Ignore IDs that do not have corresponding views.
			}
		}
	}

	protected static CharSequence valueOf( View parent, int viewResourceID ) {
		try {
			return ( (TextView) parent.findViewById( viewResourceID ) ).getText();
		} catch( Throwable exception ) {
			// If there was some problem retrieving the value, then let's just assume there is no
			// value.
			return null;
		}
	}

	private final int layoutResourceID;

	private FragmentFlowListener flowListener;

	public AccountCreationFragment( int layoutResourceID ) {
		this.layoutResourceID = layoutResourceID;
	}

	protected final Bundle export() {
		return exportTo( getArguments() );
	}

	protected Bundle exportTo( Bundle outState ) {
		outState.putCharSequence( "username", valueOf( R.id.username ) );
		outState.putCharSequence( "password", valueOf( R.id.password ) );
		outState.putCharSequence( "email", valueOf( R.id.email ) );
		outState.putCharSequence( "first_name", valueOf( R.id.first_name ) );
		outState.putCharSequence( "last_name", valueOf( R.id.last_name ) );
		return outState;
	}

	protected Fragment getNextFragment() {
		return null;
	}

	protected Fragment getPreviousFragment() {
		return null;
	}

	protected void hideSoftKeyboard( int... viewResourceIDs ) {
		hideSoftKeyboard( getView(), viewResourceIDs );
	}

	protected final boolean isChecked( int viewResourceID ) {
		return isChecked( getView(), viewResourceID );
	}

	protected final boolean isEmpty( int viewResourceID ) {
		return isEmpty( getView(), viewResourceID );
	}

	public final void next() {
		try {
			validate();
			Fragment fragment = getNextFragment();
			if( fragment == null ) {
				onFinish();
			} else {
				onNext( fragment );
			}
		} catch( Throwable exception ) {
			onError( exception.getMessage() );
		}
	}

	protected void onCancel() {
		if( flowListener == null ) {
			return;
		}
		flowListener.onCancel();
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {

		View view = inflater.inflate( layoutResourceID, null );

		setText( view, R.id.button_previous, getPreviousFragment() == null ? R.string.cancel : R.string.previous );

		onClick( view, R.id.button_previous, new OnClickListener() {

			@Override
			public void onClick( View v ) {
				previous();
			}

		} );

		setText( view, R.id.button_next, getNextFragment() == null ? R.string.finish : R.string.next );

		onClick( view, R.id.button_next, new OnClickListener() {

			@Override
			public void onClick( View v ) {
				next();
			}

		} );

		update( view, getArguments() );

		return view;

	}

	protected void onError( CharSequence message ) {
		if( flowListener == null ) {
			return;
		}
		flowListener.onError( message );
	}

	protected void onFinish() {
		if( flowListener == null ) {
			return;
		}
		flowListener.onFinish( getArguments() );
	}

	protected void onNext( Fragment fragment ) {
		if( flowListener == null ) {
			return;
		}
		if( fragment instanceof AccountCreationFragment ) {
			( (AccountCreationFragment) fragment ).setFlowListener( flowListener );
		}
		flowListener.onNext( fragment );
	}

	protected void onPrevious( Fragment fragment ) {
		if( flowListener == null ) {
			return;
		}
		if( fragment instanceof AccountCreationFragment ) {
			( (AccountCreationFragment) fragment ).setFlowListener( flowListener );
		}
		flowListener.onPrevious( fragment );
	}

	public final void previous() {
		Fragment fragment = getPreviousFragment();
		if( fragment == null ) {
			onCancel();
		} else {
			onPrevious( fragment );
		}
	}

	protected final void put( int viewResourceID, CharSequence value ) {
		put( getView(), viewResourceID, value );
	}

	protected final void require( boolean condition, int errorStringResourceID ) {
		if( !condition ) {
			throw new IllegalStateException( getString( errorStringResourceID ) );
		}
	}

	@Override
	public void setFlowListener( FragmentFlowListener flowListener ) {
		this.flowListener = flowListener;
	}

	protected void setTrigger( View parent, int viewResourceID ) {

		if( parent == null ) {
			return;
		}

		View view = parent.findViewById( viewResourceID );

		if( !( view instanceof TextView ) ) {
			return;
		}

		( (TextView) view ).setOnEditorActionListener( new OnEditorActionListener() {

			@Override
			public boolean onEditorAction( TextView v, int action, KeyEvent event ) {
				switch( action ) {
					case EditorInfo.IME_ACTION_DONE:
					case EditorInfo.IME_ACTION_GO:
					case EditorInfo.IME_ACTION_SEND:
					case EditorInfo.IME_ACTION_NEXT:
						hideSoftKeyboard( v );
						next();
						return true;
					case EditorInfo.IME_ACTION_PREVIOUS:
						hideSoftKeyboard( v );
						previous();
						return true;
				}
				return false;
			}

		} );

	}

	public final void update() {
		update( getView(), getArguments() );
	}

	protected void update( View view, Bundle arguments ) {
		if( view != null && arguments != null ) {
			put( view, R.id.username, arguments.getCharSequence( "username" ) );
			put( view, R.id.password, arguments.getCharSequence( "password" ) );
			put( view, R.id.email, arguments.getCharSequence( "email" ) );
			put( view, R.id.first_name, arguments.getCharSequence( "first_name" ) );
			put( view, R.id.last_name, arguments.getCharSequence( "last_name" ) );
		}
	}

	protected void validate() {
		// By default, assume everything is valid.
	}

	protected final CharSequence valueOf( int viewResourceID ) {
		return valueOf( getView(), viewResourceID );
	}

}
