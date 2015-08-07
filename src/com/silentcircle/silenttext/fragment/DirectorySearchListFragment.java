/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

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
package com.silentcircle.silenttext.fragment;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.silentcircle.silenttext.R;
import com.silentcircle.silenttext.activity.ConversationListActivity;
import com.silentcircle.silenttext.application.SilentTextApplication;
import com.silentcircle.silenttext.dialog.SCDialogFragment;
import com.silentcircle.silenttext.headerlistview.HeaderListView;
import com.silentcircle.silenttext.headerlistview.SectionAdapter;
import com.silentcircle.silenttext.loader.ContactUser;
import com.silentcircle.silenttext.loader.ScContactsLoader;
import com.silentcircle.silenttext.loader.ScDirectoryLoader;
import com.silentcircle.silenttext.loader.ScDirectoryLoader.UserData;
import com.silentcircle.silenttext.log.Log;
import com.silentcircle.silenttext.service.ScContactSaveService;
import com.silentcircle.silenttext.util.Constants;
import com.silentcircle.silenttext.util.SilentPhone;
import com.silentcircle.silenttext.view.CircularImageView;

public class DirectorySearchListFragment extends Fragment {

	public static final String TAG = "DirectorySearchListFragment";
	public static final String ACTION_SET_STARRED = "setStarred";
	public static final String EXTRA_CONTACT_URI = "contactUri";
	public static final String EXTRA_STARRED_FLAG = "starred";

	ConversationListActivity mActivity;
	LinearLayout mLayout, mClickedLayout;
	public SectionAdapter mAdapter;
	List<UserData> mDirectorySearchList = new ArrayList<UserData>();

	List<ContactUser> mScContactsList = new ArrayList<ContactUser>();
	HeaderListView mListView;

	String mSearchQuery;
	int mClickedSection = -1, mClickedRow = -1;

	View mClickedItemView;

	private RequestQueue mVolleyQueue;
	ImageLoader mImageLoader;

	public DirectorySearchListFragment() {
	}

	public DirectorySearchListFragment( ConversationListActivity activity, List<UserData> directorySearchList, List<ContactUser> scContactsList ) {
		mActivity = activity;
		mDirectorySearchList = directorySearchList;
		mScContactsList = scContactsList;
		createList();

		mVolleyQueue = Volley.newRequestQueue( mActivity );
		mImageLoader = new ImageLoader( mVolleyQueue, new BitmapLruCache( BitmapLruCache.getDefaultLruCacheSize() ) );
	}

	private void addContact( boolean isFavorite ) {
		UserData user = mDirectorySearchList.get( mClickedRow );
		String display_name = user.getDisplayName();
		String jid = user.getJid();
		String phone_number = user.getNumbers();

		launchSPA( display_name, jid, phone_number, isFavorite );
	}

	void colorSearchString( TextView textView ) {
		String text = textView.getText().toString();
		String [] nameAry = text.split( " " );
		boolean isAddedFullname = false;
		String name = "";
		int start = 0;
		for( int l = 0; l < nameAry.length; l++ ) {
			if( nameAry[l].toUpperCase().startsWith( mSearchQuery.toUpperCase() ) ) {
				isAddedFullname = true;
				name = nameAry[l];
				break;
			}
			start += nameAry[l].length() + 1;
		}
		if( isAddedFullname ) {

			SpannableStringBuilder sb = new SpannableStringBuilder( text );
			Pattern p = Pattern.compile( mSearchQuery, Pattern.CASE_INSENSITIVE );
			// Matcher m = p.matcher( text );
			Matcher m = p.matcher( name );
			if( m.find() ) {
				sb.setSpan( new ForegroundColorSpan( mActivity.getResources().getColor( R.color.directory_search_yellow ) ), start + m.start(), start + m.end(), Spanned.SPAN_INCLUSIVE_INCLUSIVE );
				textView.setText( sb );
			}
		}
	}

	private void createList() {
		mListView = new HeaderListView( mActivity );
		mAdapter = new SectionAdapter( this, mDirectorySearchList, mScContactsList ) {

			@Override
			public Object getRowItem( int section, int row ) {
				return null;
			}

			@Override
			public View getRowView( int section, int row, View convertView1, ViewGroup parent ) {

				View convertView = convertView1;
				if( convertView == null ) {
					convertView = mActivity.getLayoutInflater().inflate( R.layout.directory_search_item, null );
				}

				TextView displayName = (TextView) convertView.findViewById( R.id.search_display_name_tv_id );
				TextView jid = (TextView) convertView.findViewById( R.id.search_jid_tv_id );
				ImageView infoImg = (ImageView) convertView.findViewById( R.id.icon_info_id );
				infoImg.setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );
				ImageView addImg = (ImageView) convertView.findViewById( R.id.icon_add_contact_id );
				addImg.setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );
				ImageView favouriteIcon = (ImageView) convertView.findViewById( R.id.icon_favourite_id );
				favouriteIcon.setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );
				if( section == Constants.CONTACTS_SECTION ) {
					if( mScContactsList.get( row ).isStarred() ) {
						favouriteIcon.setColorFilter( Color.CYAN, PorterDuff.Mode.SRC_ATOP );
					}
				}
				ImageView phoneIcon = (ImageView) convertView.findViewById( R.id.icon_phone_id );
				phoneIcon.setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );
				ImageView messageIcon = (ImageView) convertView.findViewById( R.id.icon_message_id );
				messageIcon.setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );

				CircularImageView avarta = (CircularImageView) convertView.findViewById( R.id.avatar );

				if( section == 0 ) {
					String avartUrl = mScContactsList.get( row ).getAvatarUrl();
					if( !TextUtils.isEmpty( avartUrl ) ) {
						try {
							Bitmap bitmap = BitmapFactory.decodeStream( getActivity().getContentResolver().openInputStream( Uri.parse( mScContactsList.get( row ).getAvatarUrl() ) ) );
							avarta.setImageBitmap( bitmap );
						} catch( FileNotFoundException e ) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					displayName.setText( mScContactsList.get( row ).getDisplayName() );
					// jid.setText( mScContactsList.get( row ).getNumbers() + ":" +
					// mScContactsList.get( row ).getJid() );
					jid.setText( mScContactsList.get( row ).getNumbers() );
					infoImg.setVisibility( View.VISIBLE );
					addImg.setVisibility( View.GONE );
				} else if( section == 1 ) {
					String avartaUrl = SilentTextApplication.ACCOUNT_CREATION_CLIENT_BASE_URL + mDirectorySearchList.get( row ).getAvatarUrl();
					mImageLoader.get( avartaUrl, ImageLoader.getImageListener( avarta, R.drawable.ic_avatar_placeholder, R.drawable.ic_action_picture ) );
					displayName.setText( mDirectorySearchList.get( row ).getDisplayName() );
					// jid.setText( mDirectorySearchList.get( row ).getNumbers() + ":" +
					// mDirectorySearchList.get( row ).getJid() );
					jid.setText( mDirectorySearchList.get( row ).getNumbers() );
					addImg.setVisibility( View.VISIBLE );
					infoImg.setVisibility( View.GONE );
				}
				colorSearchString( displayName );
				colorSearchString( jid );
				mLayout = (LinearLayout) convertView.findViewById( R.id.contact_list_icons_layout_id );
				if( mClickedSection == section && mClickedRow == row ) {
					// programmatically click on the view to avoid problem.
					onRowItemClick( (AdapterView<?>) parent, convertView, section, row, getItemId( row ) );

					mLayout.setVisibility( View.VISIBLE );
					mClickedLayout = mLayout;
				} else {
					mLayout.setVisibility( View.GONE );
				}

				return convertView;
			}

			@Override
			public int getSectionHeaderItemViewType( int section ) {
				return section % 2;
			}

			@Override
			public View getSectionHeaderView( int section, View convertView1, ViewGroup parent ) {
				View convertView = convertView1;
				if( convertView == null ) {
					convertView = mActivity.getLayoutInflater().inflate( R.layout.directory_hearder, null );
				}
				TextView orgName = (TextView) convertView.findViewById( R.id.org_name_tv_id );
				orgName.setText( Constants.mOrgName );
				return convertView;
			}

			@Override
			public int getSectionHeaderViewTypeCount() {
				return 2;
			}

			@Override
			public boolean hasSectionHeaderView( int section ) {
				if( section == Constants.CONTACTS_SECTION ) {
					return false;
				}
				if( section == Constants.DIRECTORY_SECTION && !TextUtils.isEmpty( mSearchQuery ) ) {
					return true;
				}
				return false;
			}

			@Override
			public int numberOfRows( int section ) {
				// if( section == Constants.CONTACTS_SECTION ) {
				// return mScContactsList.size();
				// }
				// return mDirectorySearchList.size();

				if( section == Constants.DIRECTORY_SECTION && !TextUtils.isEmpty( Constants.mOrgName ) ) {
					return mDirectorySearchList.size();
				}
				if( section == Constants.CONTACTS_SECTION ) {
					return mScContactsList.size();
				}
				return 0;
			}

			@Override
			public int numberOfSections() {
				if( TextUtils.isEmpty( Constants.mOrgName ) ) {
					return 1;
				}
				return 2;
			}

			@Override
			public void onRowItemClick( AdapterView<?> parent, View view, int section, int row, long id ) {
				super.onRowItemClick( parent, view, section, row, id );

				if( mClickedLayout != null ) {
					mClickedLayout.setVisibility( View.GONE );
				}

				mClickedSection = section;
				mClickedRow = row;
				mClickedItemView = view;

				ImageView favouriteIcon = (ImageView) view.findViewById( R.id.icon_favourite_id );
				if( section == Constants.CONTACTS_SECTION && mScContactsList.get( row ).isStarred() ) {
					favouriteIcon.setColorFilter( Color.CYAN, PorterDuff.Mode.SRC_ATOP );
				} else {
					favouriteIcon.setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );
				}
				favouriteIcon.setOnClickListener( new OnClickListener() {

					@Override
					public void onClick( View v ) {
						onClickFavourite( v );
					}
				} );

				ImageView phoneIcon = (ImageView) view.findViewById( R.id.icon_phone_id );
				phoneIcon.setOnClickListener( new OnClickListener() {

					@Override
					public void onClick( View v ) {
						onClickPhone();
					}
				} );

				ImageView addContactIcon = (ImageView) view.findViewById( R.id.icon_add_contact_id );
				addContactIcon.setOnClickListener( new OnClickListener() {

					@Override
					public void onClick( View v ) {
						onClickAddContact();
					}
				} );

				ImageView messageIcon = (ImageView) view.findViewById( R.id.icon_message_id );
				messageIcon.setOnClickListener( new OnClickListener() {

					@Override
					public void onClick( View v ) {
						onClickMessage();
					}
				} );

				ImageView contactInfoIcon = (ImageView) view.findViewById( R.id.icon_info_id );
				contactInfoIcon.setOnClickListener( new OnClickListener() {

					@Override
					public void onClick( View v ) {
						onClickContactInfoIcon();
					}
				} );

				mLayout = (LinearLayout) view.findViewById( R.id.contact_list_icons_layout_id );
				mLayout.setVisibility( View.VISIBLE );

			}

		};
		mListView.setAdapter( mAdapter );
		mAdapter.notifyDataSetChanged();
	}

	public SectionAdapter getAdapter() {
		return mAdapter;
	}

	@SuppressLint( "UseValueOf" )
	private void launchSPA( String displayName, String jid, String phoneNumber, boolean isFavorite ) {
		// launch SPA to add contact, or make call
		Constants.mIsLaunchSPA = true;
		Constants.mCurrentRow = mClickedRow;
		Constants.mCurrentSection = mClickedSection;

		Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage( Constants.SPA_PACKAGE_NAME );
		if( intent != null ) {
			if( !SilentPhone.supports( mActivity ) ) {
				Toast.makeText( getActivity(), R.string.spa_locked, Toast.LENGTH_LONG ).show();
				return;
			}
			if( !TextUtils.isEmpty( phoneNumber ) && TextUtils.isEmpty( displayName ) && TextUtils.isEmpty( jid ) ) {
				// launch SPA dialer
				try {
					startActivity( SilentPhone.getCallIntent( phoneNumber ) );
				} catch( ActivityNotFoundException exception ) {
					Toast.makeText( getActivity(), R.string.spa_locked, Toast.LENGTH_LONG ).show();
				}
			} else if( !TextUtils.isEmpty( phoneNumber ) && !TextUtils.isEmpty( displayName ) && !TextUtils.isEmpty( jid ) ) {
				// launch SPA to add contact and add contact as favorite
				intent = new Intent( Intent.ACTION_INSERT, com.silentcircle.silentcontacts2.ScContactsContract.RawContacts.CONTENT_URI );
				intent.setType( com.silentcircle.silentcontacts2.ScContactsContract.RawContacts.CONTENT_TYPE );
				intent.putExtra( Constants.STARRED, isFavorite );
				intent.putExtra( com.silentcircle.silentcontacts2.ScContactsContract.Intents.Insert.NAME, displayName );
				intent.putExtra( com.silentcircle.silentcontacts2.ScContactsContract.Intents.Insert.PHONE, phoneNumber );
				intent.putExtra( com.silentcircle.silentcontacts2.ScContactsContract.Intents.Insert.IM_HANDLE, jid );
				intent.setFlags( Intent.FLAG_ACTIVITY_FORWARD_RESULT );
				startActivity( intent );
			}

		} else {
			String title = getResources().getString( R.string.directory_search_dialog_information_title );
			String msg = getResources().getString( R.string.directory_search_dialog_no_spa_msg );
			SCDialogFragment dialog = SCDialogFragment.newInstance( title, msg, android.R.string.ok, android.R.string.cancel, Constants.DIRECTORY_SEARCH_DIALOG_NO_SPA );
			dialog.setCancelable( false );
			dialog.show( getFragmentManager(), TAG );
		}
	}

	@Override
	public void onActivityCreated( Bundle savedInstanceState ) {
		super.onActivityCreated( savedInstanceState );
	}

	public void onClickAddContact() {
		addContact( false );
	}

	public void onClickContactInfoIcon() {
		// save variables for recovery later.
		Constants.mIsContactInfoClicked = true;
		Constants.mCurrentRow = mClickedRow;
		Constants.mCurrentSection = mClickedSection;

		long rowContactId = mScContactsList.get( mClickedRow ).getRaw_id();
		Intent intent = new Intent( Intent.ACTION_VIEW, ContentUris.withAppendedId( com.silentcircle.silentcontacts2.ScContactsContract.RawContacts.CONTENT_URI, rowContactId ) );
		intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
		getActivity().startActivity( intent );
	}

	public void onClickFavourite( View v ) {
		if( mClickedSection == Constants.DIRECTORY_SECTION ) {
			addContact( true );
		} else {
			boolean isStarred = mScContactsList.get( mClickedRow ).isStarred();
			setFavorite( mScContactsList.get( mClickedRow ).getRaw_id(), !isStarred );

			if( isStarred ) {
				( (ImageView) v ).setColorFilter( getResources().getColor( R.color.directory_search_yellow ), PorterDuff.Mode.SRC_ATOP );
			} else {
				( (ImageView) v ).setColorFilter( Color.CYAN, PorterDuff.Mode.SRC_ATOP );
			}

			mScContactsList.get( mClickedRow ).setStarred( !isStarred );
		}
	}

	public void onClickMessage() {
		String jid;
		if( mClickedSection == Constants.CONTACTS_SECTION ) {
			jid = mScContactsList.get( mClickedRow ).getJid();
		} else {
			jid = mDirectorySearchList.get( mClickedRow ).getJid();
		}
		if( TextUtils.isEmpty( jid ) ) {
			showToast( R.string.null_jid );
			return;
		}
		Constants.mIsMessageClicked = true;
		( (ConversationListActivity) getActivity() ).launchConversationActivity( jid );
	}

	public void onClickPhone() {
		String phoneNumber;
		if( mClickedSection == Constants.CONTACTS_SECTION ) {
			phoneNumber = mScContactsList.get( mClickedRow ).getNumbers();
		} else {
			phoneNumber = mDirectorySearchList.get( mClickedRow ).getNumbers();
		}
		if( TextUtils.isEmpty( phoneNumber ) ) {
			showToast( R.string.null_phone_number );
			return;
		}
		launchSPA( null, null, phoneNumber, false );
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		// reset variables.
		if( Constants.mCurrentRow != -1 ) {
			mClickedRow = Constants.mCurrentRow;
			Constants.mCurrentRow = -1;

			mClickedSection = Constants.mCurrentSection;
			Constants.mCurrentSection = -1;
		}

		return mListView;
	}

	@SuppressLint( "NewApi" )
	@Override
	public void onDetach() {
		super.onDetach();
		if( Constants.mIsHomeClicked || Constants.mIsLaunchSPA || Constants.mIsContactInfoClicked ) {
			Constants.mIsLaunchSPA = false;
			Constants.mIsContactInfoClicked = false;
			Log.i( TAG, "Must keep empty block and do nothing here." );
		} else {
			mActivity.setCurrentQuery( "" );
		}
		//
		// int count = getFragmentManager().getBackStackEntryCount();
		// if( count > 0 && getFragmentManager().getBackStackEntryAt( count - 1 ).getName().equals(
		// DirectorySearchListFragment.TAG ) ) {
		// if( Build.VERSION.SDK_INT < 17 ) {
		// if( mActivity.isFinishing() ) {
		// return;
		// }
		// } else {
		// if( mActivity.isFinishing() || mActivity.isDestroyed() ) {
		// return;
		// }
		// }
		// getFragmentManager().popBackStack();
		// }
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void setContacts( List<ContactUser> list ) {
		mScContactsList.clear();
		int size = list.size();
		if( list.size() == 0 ) {
			return;
		}
		for( int i = 0; i < size; i++ ) {
			mScContactsList.add( list.get( i ) );
		}
		mAdapter.notifyDataSetChanged();
	}

	public void setContactsSearchList( ScContactsLoader loader ) {
		mScContactsList.clear();
		if( loader != null && loader.getUserList() != null ) {
			for( int i = 0; i < loader.getUserList().size(); i++ ) {
				mScContactsList.add( loader.getUserList().get( i ) );
			}
		}

		mAdapter.notifyDataSetChanged();

	}

	public void setDirectorySearchList( Cursor cursor, int section ) {
		mDirectorySearchList.clear();
		if( cursor != null && section == R.id.directory_search ) {
			if( ScDirectoryLoader.getUserList() != null ) {
				for( int i = 0; i < ScDirectoryLoader.getUserList().size(); i++ ) {
					mDirectorySearchList.add( ScDirectoryLoader.getUserList().get( i ) );
				}
			}
		}
		mAdapter.notifyDataSetChanged();
	}

	// contactLookupUri = content://com.silentcircle.contacts2/raw_contacts/24
	private void setFavorite( long rawContactId, boolean isStarred ) {
		Intent serviceIntent = new Intent( getActivity(), ScContactSaveService.class );
		serviceIntent.setAction( ACTION_SET_STARRED );
		Uri lookupUri = ContentUris.withAppendedId( com.silentcircle.silentcontacts2.ScContactsContract.RawContacts.CONTENT_URI, rawContactId );
		serviceIntent.putExtra( EXTRA_CONTACT_URI, lookupUri );
		serviceIntent.putExtra( EXTRA_STARRED_FLAG, isStarred );

		getActivity().startService( serviceIntent );
	}

	public void setSearchString( String query ) {
		mSearchQuery = query;
	}

	private void showToast( int resId ) {
		Toast mytoast = Toast.makeText( mActivity, resId, Toast.LENGTH_LONG );
		mytoast.setGravity( Gravity.CENTER_VERTICAL, 0, 0 );
		mytoast.show();
	}
}
