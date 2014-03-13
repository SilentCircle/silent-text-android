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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.util.DeviceUtils;

public class AccountCreationWelcomeFragment extends AccountCreationFragment {

	public static AccountCreationWelcomeFragment create( Bundle arguments ) {
		AccountCreationWelcomeFragment fragment = new AccountCreationWelcomeFragment();
		fragment.setArguments( arguments );
		return fragment;
	}

	public AccountCreationWelcomeFragment() {
		super( R.layout.account_creation_welcome );
	}

	@Override
	protected Bundle exportTo( Bundle outState ) {
		return outState;
	}

	@Override
	public Fragment getNextFragment() {
		return AccountCreationRequiredFieldsFragment.create( export() );
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		View view = super.onCreateView( inflater, container, savedInstanceState );
		setText( view, R.id.partner_welcome, getString( R.string.partner_welcome, getString( R.string.silent_circle ), DeviceUtils.getManufacturer() ) );
		setText( view, R.id.partner_message, getString( R.string.partner_account_creation_prologue, getString( R.string.silent_circle ), DeviceUtils.getManufacturer(), getString( R.string.partner_free_trial_period ) ) );
		return view;
	}

}
