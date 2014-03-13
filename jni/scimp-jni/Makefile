CC=/usr/bin/clang

SOURCE_DIR=src

SOURCES=\
  uint8_t_array.c \
  scimp_keys.c \
  scimp_packet.c \
  scimp.c

OUTPUT_DIR=bin
OUTPUT_FILE=$(OUTPUT_DIR)/scimp
LIB_PREFIX := libs/scimp/

SHLIB_PATHS = bin \
			  libs/tommath/bin \
			  libs/tomcrypt/bin \
			  libs/yajl/bin

EXTRA_SHLIBS := $(addprefix $(LIB_PREFIX),$(SHLIB_PATHS))

empty :=
space := $(empty) $(empty)

# Add these shared library paths to run the test program
# The subst will of course mess up if there are spaces in the directory names.
EXTRA_SHLIB_PATH = $(subst $(space),:,$(EXTRA_SHLIBS))

# Do some OS-specific stuff
PATHNAME := LD_LIBRARY_PATH
UNAME = $(shell uname -s)
ifeq ("$(UNAME)", "Linux")
  CFLAGS+=-DLINUX
else
  ifeq ("$(UNAME)", "Darwin")
    CFLAGS+=-DBSD
    PATHNAME := DYLD_LIBRARY_PATH
  endif
endif

export CFLAGS

all: libs/scimp/bin/libscimp.so $(OUTPUT_DIR) $(OUTPUT_FILE)

libs/scimp/bin/libscimp.so:
	$(MAKE) -C libs/scimp

$(OUTPUT_FILE): $(addprefix $(SOURCE_DIR)/,$(SOURCES))
	$(CC) -Wall -I$(SOURCE_DIR) \
		-Llibs/scimp/bin -lscimp -Ilibs/scimp/bin/include \
		-Llibs/scimp/libs/tomcrypt/bin -ltomcrypt -Ilibs/scimp/libs/tomcrypt/bin/include \
		-Llibs/scimp/libs/tommath/bin -ltommath -Ilibs/scimp/libs/tommath/bin/include \
		-Llibs/scimp/libs/yajl/bin -lyajl -Ilibs/scimp/libs/yajl/bin/include \
		-lpthread \
		-o $(OUTPUT_FILE) $^

$(OUTPUT_DIR):
	mkdir -p $(OUTPUT_DIR)

prepend_libpath = $(value $(1))=$(2):$${$(value $(1))}

run: $(OUTPUT_FILE)
	$(call prepend_libpath,PATHNAME,$(EXTRA_SHLIB_PATH)) $(OUTPUT_FILE)

clean:
	make -C libs/scimp clean
	rm -fR $(OUTPUT_DIR)
