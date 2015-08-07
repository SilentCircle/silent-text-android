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
package com.silentcircle.silenttext.listener;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.silentcircle.silenttext.activity.LoginActivity;
import com.silentcircle.silenttext.util.ViewUtils;

public class LoginOnRequiredInput implements OnEditorActionListener {

	private final SoftReference<Context> contextReference;
	private final ArrayList<TextView> requiredInputs;
	private final boolean isSelfRequired;

	public LoginOnRequiredInput( Context context, ArrayList<TextView> requiredInputs, boolean isSelfRequired ) {
		contextReference = new SoftReference<Context>( context );
		this.requiredInputs = requiredInputs;
		this.isSelfRequired = isSelfRequired;
	}

	private boolean focusRequiredInputs() {
		if( requiredInputs != null ) {
			for( TextView v : requiredInputs ) {
				if( ViewUtils.isEmpty( v ) ) {
					ViewUtils.focus( v );
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean onEditorAction( TextView view, int actionId, KeyEvent event ) {
		if( focusRequiredInputs() ) {
			if( isSelfRequired && !ViewUtils.isEmpty( view ) ) {
				Context context = contextReference.get();
				if( context instanceof LoginActivity ) {
					( (LoginActivity) context ).login();
					return false;
				}
			} else {
				return true;
			}
		} else if( isSelfRequired && ViewUtils.isEmpty( view ) ) {
			return true;
		}

		return true;
	}
}
