# Silent Text Status API v1.0

Silent Text provides access to some very basic status information via the [Content Provider API][1].
As a consumer of this status information, you need the following information:

 1. The [Uri][2] parameter provided to [ContentResolver#query\(Uri,String,String,String,String\)][3].
 2. The column definition for its returned [Cursor][4].
 3. The meaning of the values in each returned row.

The answers are as follows.

 1. The [Uri][2] parameter is equal to `Uri.parse("content://com.silentcircle.silenttext/status")`.
 2. The returned [Cursor][4] contains a single column, whose name is "status".
 3. The returned [Cursor][4] contains a single row, whose value contains an integer according to the following definitions:

    * **0**: Silent Text is INSTALLED. This indicates that the application is installed, but no additional information about its state is currently available.
    * **1**: Silent Text is UNLOCKED. This indicates that the application is currently running and its data storage is unlocked, but it is not associated with a valid account.
    * **2**: Silent Text is ACTIVATED. This indicates that the application is currently running, its data storage is unlocked, and it is associated with a valid account.
    * **3**: Silent Text is ONLINE. This indicates that the application is currently connected to the XMPP server.
    * **-1**: The status of Silent Text is UNKNOWN.

[1]: https://developer.android.com/guide/topics/providers/content-providers.html
[2]: https://developer.android.com/reference/android/net/Uri.html
[3]: https://developer.android.com/reference/android/content/ContentResolver.html#query(android.net.Uri%2C%20java.lang.String%5B%5D%2C%20java.lang.String%2C%20java.lang.String%5B%5D%2C%20java.lang.String)
[4]: https://developer.android.com/reference/android/database/Cursor.html

# Sample Code

The following activity provides a reference implementation:

    package com.example;

    import android.app.Activity;
    import android.content.Intent;
    import android.database.Cursor;
    import android.net.Uri;
    import android.os.Bundle;
    import android.widget.Toast;

    public class SilentTextStatusActivity extends Activity {

    	public static final int STATUS_UNKNOWN = -1;
    	public static final int STATUS_INSTALLED = 0;
    	public static final int STATUS_UNLOCKED = 1;
    	public static final int STATUS_ACTIVATED = 2;
    	public static final int STATUS_ONLINE = 3;

    	public int getSilentTextStatusCode() {

    		Uri uri = Uri.parse( "content://com.silentcircle.silenttext/status" );
    		Cursor cursor = getContentResolver().query( uri, null, null, null, null );

    		if( cursor == null ) {
    			throw new NullPointerException( "The Silent Text status query returned a null cursor. Silent Text may not be installed." );
    		}

    		int status = -1;

    		try {

    			if( !cursor.moveToNext() ) {
    				throw new IllegalStateException( "The Silent Text status query returned an empty cursor. This violates the status API contract and should be considered a bug." );
    			}

    			int column = cursor.getColumnIndex( "status" );

    			if( column < 0 ) {
    				throw new IllegalStateException( "The Silent Text status query returned a cursor without a 'status' column. This violates the status API contract and should be considered a bug." );
    			}

    			status = cursor.getInt( column );

    		} finally {
    			cursor.close();
    		}

    		return status;

    	}

    	public void cancel() {
    		setResult( RESULT_CANCELED );
    		finish();
    	}

    	@Override
    	protected void onCreate( Bundle savedInstanceState ) {

    		super.onCreate( savedInstanceState );

    		try {

    			int status = getSilentTextStatusCode();

    			switch( status ) {
    				case STATUS_INSTALLED:
    					toast( "The application is installed, but no information beyond this is available." );
    					break;
    				case STATUS_UNLOCKED:
    					toast( "The application is installed and unlocked, but it has not been associated with a valid account." );
    					break;
    				case STATUS_ACTIVATED:
    					toast( "The application is installed, unlocked, and has been associated with a valid account." );
    					break;
    				case STATUS_ONLINE:
    					toast( "The application is installed, unlocked, associated with a valid account, and is currently in use." );
    					break;
    				default:
    					toast( "An unknown status code was encountered. Consult the latest API documentation." );
    					break;
    			}

    			setResult( RESULT_OK, status );
    			finish();

    		} catch( NullPointerException exception ) {
    			toast( exception.getMessage() );
    			cancel();
    		} catch( IllegalStateException exception ) {
    			toast( exception.getMessage() );
    			cancel();
    		}

    	}

    	public void setResult( int resultCode, int statusCode ) {
    		Intent data = new Intent();
    		data.putExtra( "status", statusCode );
    		setResult( resultCode, data );
    	}

    	public void toast( String message ) {
    		Toast.makeText( this, message, Toast.LENGTH_LONG ).show();
    	}

    }

