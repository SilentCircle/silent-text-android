LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE   := scloud-jni
LOCAL_CFLAGS   := -DANDROID
LOCAL_LDLIBS   := -llog

LOCAL_SHARED_LIBRARIES := \
  scloud \
  tomcrypt

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/src

LOCAL_SRC_FILES  := \
  src/base64.c \
  src/uint8_t_array.c \
  src/scloud_encrypt_parameters.c \
  src/scloud_encrypt_packet.c \
  src/scloud_decrypt_parameters.c \
  src/scloud_decrypt_packet.c \
  src/jni.c

LOCAL_EXPORT_C_INCLUDES := \
  $(LOCAL_PATH)/src

include $(BUILD_SHARED_LIBRARY)
