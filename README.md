## Introduction

These are the sources for Silent Circle's Silent Text for Android project.

### Overview

Share encrypted texts and transfer files up to 100 MB. Includes Burn functionality to destroy selected messages automatically.

### What's New In This Update

The sources are updated for version 1.12.2 of the project.

### Prerequisites

To build Silent Phone for Android you will need the following resources:

- Ant
- Java Development Kit (JDK) 7
- the stand-alone Android SDK Tools

Using the Android SDK the following command will install the required additional packages:

```
$ android  update sdk --all --no-ui --filter tools,platform-tools,build-tools-19.1.0,android-19
```

### How to Build

- download the repository
- create a terminal window
- cd to the top of the repository
- edit build.sh to reference the correct locations of the prerequisites
- bash build.sh

The build produces silenttext.apk which can be used on an Android device.
