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
package com.silentcircle.silenttext.preference;

import java.util.Stack;

import org.json.JSONObject;

import android.preference.PreferenceGroup;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.util.JSONUtils;
import com.silentcircle.silenttext.util.PreferenceUtils;

public class PreferenceTreeBuilder {

	private final Stack<PreferenceGroup> tree = new Stack<PreferenceGroup>();

	public PreferenceTreeBuilder( PreferenceGroup root ) {
		tree.push( root );
	}

	public PreferenceTreeBuilder add( CharSequence title, CharSequence summary ) {
		PreferenceUtils.add( tree.peek(), title, summary );
		return this;
	}

	public PreferenceTreeBuilder addBoolean( CharSequence title, JSONObject json, String... path ) {
		if( path.length < 1 ) {
			return this;
		}
		JSONObject parent = json;
		for( int i = 0; i < path.length - 1; i++ ) {
			parent = JSONUtils.getJSONObject( parent, path[i] );
		}
		PreferenceUtils.add( tree.peek(), title, JSONUtils.getBoolean( parent, path[path.length - 1] ) ? R.string.yes : R.string.no );
		return this;
	}

	public PreferenceTreeBuilder addInt( CharSequence title, JSONObject json, String... path ) {
		if( path.length < 1 ) {
			return this;
		}
		JSONObject parent = json;
		for( int i = 0; i < path.length - 1; i++ ) {
			parent = JSONUtils.getJSONObject( parent, path[i] );
		}
		int value = JSONUtils.getInt( parent, path[path.length - 1], Integer.MIN_VALUE );
		if( value != Integer.MIN_VALUE ) {
			PreferenceUtils.add( tree.peek(), title, Integer.toString( value ) );
		}
		return this;
	}

	public PreferenceTreeBuilder addString( CharSequence title, JSONObject json, String... path ) {
		if( path.length < 1 ) {
			return this;
		}
		JSONObject parent = json;
		for( int i = 0; i < path.length - 1; i++ ) {
			parent = JSONUtils.getJSONObject( parent, path[i] );
		}
		String value = JSONUtils.getString( parent, path[path.length - 1] );
		if( value != null ) {
			PreferenceUtils.add( tree.peek(), title, value );
		}
		return this;
	}

	public PreferenceTreeBuilder close() {
		tree.pop();
		return this;
	}

	public PreferenceTreeBuilder open( CharSequence title ) {
		PreferenceGroup parent = tree.peek();
		PreferenceGroup category = parent.getPreferenceManager().createPreferenceScreen( parent.getContext() );
		category.setTitle( title );
		parent.addPreference( category );
		tree.push( category );
		return this;
	}

}
