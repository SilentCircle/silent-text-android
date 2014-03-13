LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE   := scimp
LOCAL_CFLAGS   := -DANDROID
LOCAL_LDLIBS   := -llog

LOCAL_SHARED_LIBRARIES := \
  tommath \
  tomcrypt \
  yajl

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/shared \
  $(LOCAL_PATH)/src

LOCAL_SRC_FILES  := \
  shared/SCccm.c \
  shared/SCgcm.c \
  shared/SCkeys.c \
  shared/SCutilities.c \
  shared/SirenHash.c \
  shared/tomcryptwrappers.c \
  src/SCimp.c \
  src/SCimpProtocol.c \
  src/SCimpProtocolFmtJSON.c \
  src/SCimpProtocolFmtXML.c

LOCAL_EXPORT_C_INCLUDES := \
  $(LOCAL_PATH)/src \
  $(LOCAL_PATH)/shared

include $(BUILD_SHARED_LIBRARY)
