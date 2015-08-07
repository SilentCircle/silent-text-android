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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.silentcircle.silenttext.activity.LoginActivity;
import com.silentcircle.silenttext.util.ViewUtils;

public class LoginOnClickWithRequiredInput implements OnClickListener {

	private final SoftReference<Context> contextReference;
	private final ArrayList<TextView> requiredInputs;

	public LoginOnClickWithRequiredInput( Context context, ArrayList<TextView> requiredInputs ) {
		contextReference = new SoftReference<Context>( context );
		this.requiredInputs = requiredInputs;
	}

	private boolean focusRequiredInputs() {
		if( requiredInputs != null ) {
			for( TextView v : requiredInputs ) {
				if( ViewUtils.isEmpty( v ) ) {
					ViewUtils.focus( v );

					Context context = contextReference.get();
					if( context instanceof LoginActivity ) {
						InputMethodManager imm = (InputMethodManager) context.getSystemService( Context.INPUT_METHOD_SERVICE );
						if( imm != null ) {
							imm.toggleSoftInput( InputMethodManager.SHOW_IMPLICIT, 0 );
						}
					}
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void onClick( View v ) {
		if( focusRequiredInputs() ) {
			Context context = contextReference.get();
			if( context instanceof LoginActivity ) {
				( (LoginActivity) context ).login();
			}
		}
	}
}
