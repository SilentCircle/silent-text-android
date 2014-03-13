## Introduction

This repository contains the sources for Silent Circle's Silent Text for Android project. Using these sources and the the instructions that follow, you should be able to build a Silent Text APK that can be installed and run on an Android device.

### Overview

Silent Text is a secure text messaging application built on strong cryptography and XMPP.

### Prerequisites

This short description assumes that you have a good understanding of the Android SDK, the Android NDK (Native Development Kit), and their build procedures.

To compile and build Silent Text you need a full Android development environment that includes the Android Java SDK and the Android NDK. Because Silent Text depends upon Android API 17 (Jelly Bean), make sure you also download and install the necessary SDK modules. You may wish to use the Eclipse Android SDK extensions.

#### Specific Recommendations

This section describes an environment capable of building Silent Phone Android 1.8.1, Silent Contacts 1.0.1 and Silent Text 1.2.0.

Using the following components, we have built the product in a virtual machine, on bare iron, and on a laptop that also serves as a personal machine. Other successful configurations are certainly possible, this is the one we tested with.

- minimum system configuration: 1,536 Mb memory, 20 GB disk

- debian-7.0.0-amd64-DVD-1.iso
  
  In response to "`software selection`" all you need is
         
         - [*] ssh server          <- not used by the build
         - [*] Standard system utilities

         Use a mirror

 - If appropriate, install virtual machine additions.

- Install additional packages:

         $ dpkg --add-architecture i386
         $ apt-get update
         #    some of the tools are 32 bit only
         $ apt-get install ia32-libs
         $ apt-get install git
         $ apt-get install unzip
         $ apt-get install sudo  

- Create and login to a non-privileged account

         $ adduser pat

- Create a local directory to hold the packages and tools needed to build the product

         $ mkdir ~/opt

- java

  The version of java that gives the least trouble is Sun/Oracle JDK6. For the test build, we used the `jdk-6u45-linux-x64.bin` system. It can be acquired from 
  [https://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html#jdk-6u45-oth-JPR]().

  The hash of the version that we tested with is 
         $ openssl sha1 jdk-6u45-linux-x64.bin
         SHA1(jdk-6u45-linux-x64.bin)= 24425cdb69c11e86d6b58757b29e5ba4e4977660

  Unpack java and move it under the local opt directory
         $ sh jdk-6u45-linux-x64.bin
         $ sudo mv jdk1.6.0_45 ~/opt

- ADT - Android Development Toolkit

  While the build doesn't use Eclipse, pulling the whole ADT ensures that many needed things are fetched.

         $ wget http://dl.google.com/android/adt/adt-bundle-linux-x86_64-20130729.zip
         $ unzip adt-bundle-linux-x86_64-20130729.zip -d ~/opt

- NDK - Native Development Kit

  Silent Phone has native components. The lastest NDK causes compiler errors, so use this one instead:

         $ wget https://dl.google.com/android/ndk/android-ndk-r8e-linux-x86_64.tar.bz2
         $ tar -xjf android-ndk-r8e-linux-x86_64.tar.bz2 -C ~/opt

- Android configuration

  Note: We had do the following few steps to update with the needed configurations.

  First, add java and android tools to the path:
  
         $ export PATH=$HOME/opt/jdk1.6.0_45/bin:$HOME/opt/adt-bundle-linux-x86_64-20130729/sdk/tools:$PATH
         
  Next, use the android tool to find the following the following tools and libraries, this list will be reduced in the next release, but for now you need the following: 

    Android SDK Tools, revision 22.3
    Android SDK Platform-tools, revision 19.0.1
    Android SDK Build-tools, revision 19.0.1
    Android SDK Build-tools, revision 19
    Android SDK Build-tools, revision 17
    SDK Platform Android 4.4.2, API 19, revision 2
    SDK Platform Android 4.2.2, API 17, revision 2
    Android Support Repository, revision 4

      $ android list sdk --all 

  For us it was entries 1,2,3,4,8,10,12,82:

     1- Android SDK Tools, revision 22.3
     2- Android SDK Platform-tools, revision 19.0.1
     3- Android SDK Build-tools, revision 19.0.1
     4- Android SDK Build-tools, revision 19
     8- Android SDK Build-tools, revision 17
    10- SDK Platform Android 4.4.2, API 19, revision 2
    12- SDK Platform Android 4.2.2, API 17, revision 2
    82- Android Support Repository, revision 4
                  
  Now use the update tool to fetch the components:

      $ android update sdk --no-ui --all --filter 1,2,3,4,8,10,12,82 
         ...license stuff...
         Do you accept the license 'android-sdk-license-bcbbd656' [y/n]: y

   Installing Archives:
           ...
   Done. 8 packages installed.


  It may also be necessary to change the privileges on one of the files, to permit the build script to execute it:
  
         $ chmod g+x ~/opt/adt-bundle-linux-x86_64-20130729/eclipse/plugins/org.apache.ant_1.8.3.v201301120609/bin/ant

- Gradle 

  The Silent Text build does not use Gradle however the Silent Contacts and Silent Phone builds do.  You can find out more about Gradle at www.gradle.org.  On starting the build the Gradle tool will download updates to itself and other select components used in the production of this product.


### Directory structure

.
|-- assets
|-- bin                               # where the apk is found
|-- .build-release
|-- gen
|-- jni
|   |-- scimp
|   |-- scimp-jni
|   |-- scloud
|   |-- scloud-jni
|   |-- tomcrypt
|   |-- tommath
|   `-- yajl
|-- libs
|-- obj
|-- res
|-- .settings
|-- src
|-- support
|   |-- ActionBarSherlock
|   `-- ImageViewTouch
|-- template
`-- test



##Building Silent Text from the existing Git Repository

Clone the sources from github. If you followed the "Specific Recommendations" above, the supporting toolsets will all be in the expected places. If not, edit `BUILD.sh` and adjust the symbols to match your environment.

cd to the top level repository directory and invoke BUILD.sh

     $ bash -x BUILD.sh >& build.log


##Post-Build Instructions

A successful build will result in a `SilentText-release.apk` file in the bin directory. Move this file to your Android device and run it to install Silent Text just as would be done if downloaded from the Google Play Store.  Log in with you Silent Circle credentials, send and receive messages.

Note: Silent Circle has three products that run on Andriod:  Silent Contacts, Silent Phone and Silent Text.  To work together locally the three applications must be signed by the same code signing certificate.  A certificate for use with the open source code is included in each of the projects.  The included certificate is not the one used to sign the Play Store version of the products so while equivalent the Play Store version of the project can not share local data with the open source version.  The signing certificate does not effect external communication, either version of the product can be use to send encrypted voice and data.


##Further Notes:

### Testing

Unit and functional tests for this application live within the **test** directory. It can be imported as an Eclipse Java project (*not* an Android Test Project). You can run all tests by invoking `ant test`. Test reports are generated and placed within **test/bin/reports**.

### Build Properties

The following properties may be specified on the command-line like `-Dname=value` or in a **build.properties** file:

 * `build.environment`: The base domain of the deployment environment. For production, this is *silentcircle.com*.
 * `build.commit`: For debugging, this is a reference to the commit ID in source control that corresponds with the build.
 * `build.version`: For debugging, this is a reference to the automated build ID.
 * `build.date`: For debugging, this is a reference to the date on which this build occurred.
 * `build.gcm_sender`: The sender ID used for Google Cloud Messaging (GCM) push notifications.
 * `build.scloud_url`: The base URL of the Amazon S3 bucket to which files will be uploaded and from which they will be downloaded.



