LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE   := yajl
LOCAL_CFLAGS   := -Werror

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/src

LOCAL_EXPORT_C_INCLUDES := \
  $(LOCAL_PATH)/src

LOCAL_SRC_FILES  := \
  src/yajl.c \
  src/yajl_alloc.c \
  src/yajl_buf.c \
  src/yajl_encode.c \
  src/yajl_gen.c \
  src/yajl_lex.c \
  src/yajl_parser.c \
  src/yajl_tree.c \
  src/yajl_version.c

include $(BUILD_SHARED_LIBRARY)
