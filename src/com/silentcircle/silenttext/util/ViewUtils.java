/*
Copyright (C) 2014-2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ChooserBuilder;
import com.silentcircle.silenttext.model.SCIMPError;

public class ViewUtils {

	public static final int [] STATE_CHECKED = {
		android.R.attr.state_checked
	};

	public static Intent createIntentForLinks( TextView view ) {

		CharSequence text = view.getText();

		if( text instanceof Spannable ) {

			Spannable stext = (Spannable) text;
			URLSpan [] spans = stext.getSpans( 0, stext.length(), URLSpan.class );

			if( spans != null && spans.length > 0 ) {

				ChooserBuilder chooser = new ChooserBuilder( view.getContext() );

				chooser.label( R.string.view );

				for( URLSpan span : spans ) {
					String url = span.getURL();
					CharSequence label = stext.subSequence( stext.getSpanStart( span ), stext.getSpanEnd( span ) );
					chooser.intent( new Intent( Intent.ACTION_VIEW, Uri.parse( url ) ), label );
				}

				return chooser.build();

			}

		}

		return null;

	}

	public static boolean focus( Context context, int viewResourceID ) {
		if( context instanceof Activity ) {
			return focus( ( (Activity) context ).findViewById( viewResourceID ) );
		}

		return false;
	}

	public static boolean focus( View view ) {
		return view.requestFocus();
	}

	public static String getLocalizedErrorString( Context context, SCIMPError error ) {
		Resources resources = context.getResources();
		String key = String.format( "SCIMPError_%s", error.getName() );
		int identifier = resources.getIdentifier( key, "string", context.getPackageName() );
		return resources.getString( identifier );
	}

	public static boolean isEmpty( Context context, int viewResourceID ) {
		if( context instanceof Activity ) {
			return isEmpty( ( (Activity) context ).findViewById( viewResourceID ) );
		}

		return true;
	}

	public static boolean isEmpty( View view ) {
		if( view instanceof EditText ) {
			if( ( (EditText) view ).getEditableText().toString().trim().length() <= 0 ) {
				return true;
			}
		}

		if( view instanceof TextView ) {
			return ( (TextView) view ).getText() == "";
		}

		return true;
	}

	public static void removeDrawableState( Drawable drawable, int stateToRemove ) {
		if( drawable instanceof StateListDrawable ) {
			removeDrawableState( (StateListDrawable) drawable, stateToRemove );
		}
	}

	public static int [] removeDrawableState( int [] initialState, int stateToRemove ) {

		int [] outState = initialState;
		int index = -1;

		for( int i = 0; i < initialState.length; i++ ) {
			if( initialState[i] == stateToRemove ) {
				index = i;
				break;
			}
		}

		if( index >= 0 ) {
			outState = new int [initialState.length - 1];
			for( int i = 0; i < initialState.length; i++ ) {
				if( i < index ) {
					outState[i] = initialState[i];
				} else if( i > index ) {
					outState[i - 1] = initialState[i];
				}
			}
		}

		return outState;

	}

	public static void removeDrawableState( StateListDrawable drawable, int stateToRemove ) {
		if( drawable.setState( removeDrawableState( drawable.getState(), stateToRemove ) ) ) {
			drawable.invalidateSelf();
		}
	}

	public static void removeDrawableState( View view, int stateToRemove ) {
		removeDrawableState( view.getBackground(), stateToRemove );
	}

	@TargetApi( Build.VERSION_CODES.HONEYCOMB )
	public static void setAlpha( View view, float alpha ) {
		if( view != null ) {
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
				view.setAlpha( alpha );
			}
		}
	}

	private static void setDrawableLeft( TextView view, int drawableResourceID ) {
		view.setCompoundDrawablesWithIntrinsicBounds( drawableResourceID, 0, 0, 0 );
	}

	public static void setDrawableStart( TextView view, int drawableResourceID ) {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
			setDrawableLeft( view, drawableResourceID );
		} else {
			setDrawableStartForReal( view, drawableResourceID );
		}
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR1 )
	private static void setDrawableStartForReal( TextView view, int drawableResourceID ) {
		view.setCompoundDrawablesRelativeWithIntrinsicBounds( drawableResourceID, 0, 0, 0 );
	}

	@TargetApi( Build.VERSION_CODES.JELLY_BEAN )
	public static void startActivity( Context context, Intent intent, int enterAnimationResourceID, int exitAnimationResourceID ) {

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
			context.startActivity( intent, ActivityOptions.makeCustomAnimation( context, enterAnimationResourceID, exitAnimationResourceID ).toBundle() );
		} else {
			context.startActivity( intent );
		}

	}
}
