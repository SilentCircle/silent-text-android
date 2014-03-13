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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.PrivacyPolicyActivity;
import com.silentcircle.silenttext.listener.LaunchActivityOnClick;
import com.silentcircle.silenttext.listener.ShowNextViewOnClick;

public class AccountCreationSummaryFragment extends AccountCreationFragment {

	public static AccountCreationSummaryFragment create( Bundle arguments ) {
		AccountCreationSummaryFragment fragment = new AccountCreationSummaryFragment();
		fragment.setArguments( arguments );
		return fragment;
	}

	public AccountCreationSummaryFragment() {
		super( R.layout.account_creation_summary );
	}

	@Override
	protected Bundle exportTo( Bundle outState ) {
		return outState;
	}

	@Override
	public Fragment getPreviousFragment() {
		return AccountCreationOptionalFieldsFragment.create( export() );
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		View view = super.onCreateView( inflater, container, savedInstanceState );
		onClick( view, R.id.button_privacy, new LaunchActivityOnClick( getActivity(), new Intent( getActivity(), PrivacyPolicyActivity.class ), R.string.error_activity_not_found ) );
		onClick( view, R.id.button_terms, new LaunchActivityOnClick( new Intent( Intent.ACTION_VIEW, Uri.parse( "https://accounts.silentcircle.com/terms/" ) ), R.string.error_activity_not_found ) );
		onClick( view, R.id.password_reveal, new ShowNextViewOnClick() );
		return view;
	}

	@Override
	protected void validate() {
		require( !isEmpty( R.id.username ), R.string.error_username_required );
		require( !isEmpty( R.id.password ), R.string.error_password_required );
		require( isChecked( R.id.accept_terms ), R.string.error_accept_terms );
	}

}
