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
package com.silentcircle.scloud.model;

public class SCloudObject {

	private String key;
	private String url;
	private String locator;
	private int size;
	private byte [] data;
	private int offset;
	private boolean uploaded;
	private boolean downloaded;

	public SCloudObject() {
	}

	public SCloudObject( String key, String locator ) {
		setKey( key );
		setLocator( locator );
	}

	public SCloudObject( String key, String locator, byte [] data ) {
		this( key, locator, data, 0, data == null ? 0 : data.length );
	}

	public SCloudObject( String key, String locator, byte [] data, int offset, int length ) {
		this( key, locator );
		setData( data, offset, length );
	}

	public SCloudObject( String key, String locator, int size ) {
		this( key, locator );
		this.size = size;
	}

	@Override
	public boolean equals( Object o ) {
		return o != null && hashCode() == o.hashCode();
	}

	public byte [] getData() {
		return data;
	}

	public String getKey() {
		return key;
	}

	public String getLocator() {
		return locator;
	}

	public int getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}

	public String getURL() {
		return url;
	}

	@Override
	public int hashCode() {
		return locator == null ? 0 : locator.hashCode();
	}

	public boolean isDownloaded() {
		return downloaded;
	}

	public boolean isUploaded() {
		return uploaded;
	}

	public void setData( byte [] data ) {
		setData( data, 0, data == null ? 0 : data.length );
	}

	public void setData( byte [] data, int offset, int length ) {
		this.data = data;
		this.offset = offset;
		setSize( data == null ? 0 : length );
	}

	public void setDownloaded( boolean downloaded ) {
		this.downloaded = downloaded;
	}

	public void setKey( String key ) {
		this.key = key;
	}

	public void setLocator( String locator ) {
		this.locator = locator;
	}

	public void setSize( int size ) {
		this.size = size;
	}

	public void setUploaded( boolean uploaded ) {
		this.uploaded = uploaded;
	}

	public void setURL( String url ) {
		this.url = url;
	}

	@Override
	public String toString() {
		return String.format( "%s[%d]", getLocator(), Integer.valueOf( getSize() ) );
	}

}
