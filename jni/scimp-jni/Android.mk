LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE   := scimp-jni
LOCAL_CFLAGS   := -DANDROID
LOCAL_LDLIBS   := -llog

LOCAL_SHARED_LIBRARIES := \
  scimp \
  tomcrypt

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/src

LOCAL_SRC_FILES  := \
  src/uint8_t_array.c \
  src/scimp_packet.c \
  src/scimp_keys.c \
  src/jni.c

include $(BUILD_SHARED_LIBRARY)
