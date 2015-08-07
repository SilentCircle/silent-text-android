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

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

public class SearchView extends android.widget.SearchView {

	private static final String SEARCH_PLATE_RESOURCE_ID_NAME = "android:id/search_plate";

	private int searchPlateResourceID;

	public SearchView( Context context ) {
		super( context );
	}

	public SearchView( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public void setSearchBackgroundResource( int drawableResourceID ) {
		View container = getSearchPlate();
		if( container != null ) {
			container.setBackgroundResource( drawableResourceID );
		}
	}

	private View getSearchPlate() {
		return findViewById( getSearchPlateResourceID() );
	}

	private int getSearchPlateResourceID() {
		if( searchPlateResourceID == 0 ) {
			Resources resources = getResources();
			searchPlateResourceID = resources.getIdentifier( SEARCH_PLATE_RESOURCE_ID_NAME, null, null );
		}
		return searchPlateResourceID;
	}


}
