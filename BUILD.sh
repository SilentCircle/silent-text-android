#!/bin/bash

# edit as needed to match local environment
#
export ANDROID_ANT=/usr/bin/ant
export ANDROID_SDK=$HOME/opt/android-sdk-linux
export JAVA_JDK=$HOME/opt/jdk1.7.0_51/bin

export WORKSPACE=$(pwd)

# update path
#
export PATH=$ANDROID_ANT:$ANDROID_SDK/tools:$JAVA_JDK:$PATH

# update local.properties
#
echo "sdk.dir=$ANDROID_SDK" > local.properties

# update android project
#
android update project --target android-19 --path support/ImageViewTouch/ImageViewTouch

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

# put the result at the top
#
cp bin/SilentText-release.apk ./silenttext.apk

