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

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class IOUtilsTest extends Assert {

	@Test
	public void delete_shouldDeleteDirectoryWithFiles() throws IOException {

		File parent = new File( "delete_shouldDeleteDirectoryWithFiles" );
		File child1 = new File( parent, "child1" );
		File child2 = new File( parent, "child2" );
		File child3 = new File( parent, "child3" );

		parent.mkdir();
		child1.createNewFile();
		child2.createNewFile();
		child3.createNewFile();

		IOUtils.delete( parent );

		assertFalse( parent.exists() );

	}

	@Test
	public void delete_shouldDeleteEmptyDirectory() {
		File file = new File( "delete_shouldDeleteEmptyDirectory" );
		file.mkdir();
		IOUtils.delete( file );
		assertFalse( file.exists() );
	}

	@Test
	public void delete_shouldDeleteSingleFile() throws IOException {
		File file = File.createTempFile( "delete_shouldDeleteSingleFile", ".test" );
		IOUtils.delete( file );
		assertFalse( file.exists() );
	}

	@Test
	public void delete_shouldNotDeleteParentDirectory() throws IOException {

		File parent = new File( "delete_shouldNotDeleteParentDirectory" );
		File child = new File( parent, "child" );

		parent.mkdir();
		child.createNewFile();

		IOUtils.delete( child );

		assertTrue( parent.exists() );

		IOUtils.delete( parent );

	}

	@Test
	public void delete_shouldNotExplodeWhenGivenFileThatDoesNotExist() {
		IOUtils.delete( new File( "delete_shouldNotExplodeWhenGivenFileThatDoesNotExist" ) );
	}

	@Test
	public void delete_shouldNotExplodeWhenGivenNull() {
		IOUtils.delete( null );
	}

}
