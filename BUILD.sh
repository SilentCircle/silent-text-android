#!/bin/bash

# edit as needed to match local environment
#
export ANDROID_ADT=$HOME/opt/adt-bundle-linux-x86_64-20130729
export ANDROID_ANT=$ANDROID_ADT/eclipse/plugins/org.apache.ant_1.8.3.v201301120609/bin/

export ANDROID_SDK=$ANDROID_ADT/sdk
export ANDROID_NDK=$HOME/opt/android-ndk-r8e

export JAVA_JDK=$HOME/opt/jdk1.6.0_45/bin

export WORKSPACE=`pwd`

# update path
#
export PATH=$ANDROID_NDK:$ANDROID_ADT:$ANDROID_ANT:$ANDROID_SDK/tools:$JAVA_JDK:$PATH

# update local.properties
# 
echo "sdk.dir=$HOME/opt/adt-bundle-linux-x86_64-20130729/sdk" > local.properties
echo "ndk.dir=$HOME/opt/android-ndk-r8e" >> local.properties

# update android project
#
android update project --target android-17 --path support/ActionBarSherlock/library
android update project --path support/ImageViewTouch/ImageViewTouch

# update build.properties
#
echo "build.environment=silentcircle.com" > build.properties
echo "build.version=open" >> build.properties
echo "build.commit=$(git log -n 1 --pretty=format:'%h')" >> build.properties
echo "build.date=`date +%F_%H-%M-%S`" >> build.properties
echo "build.debug=false" >> build.properties
echo "build.scloud_url=https://s3.amazonaws.com/com.silentcircle.silenttext.scloud/" >> build.properties
echo "build.gcm_sender=77924239850" >> build.properties
echo "key.store=.build-release/test-debug.keystore" >> build.properties
echo "key.alias=androiddebugkey" >> build.properties
echo "key.store.password=android" >> build.properties
echo "key.alias.password=android" >> build.properties

# do the build!
#
ant clean release >& build.log
