CC=/usr/bin/clang

SOURCE_DIR=src

SOURCES=\
  base64.c \
  uint8_t_array.c \
  scloud_encrypt_parameters.c \
  scloud_encrypt_packet.c \
  scloud_decrypt_parameters.c \
  scloud_decrypt_packet.c \
  transform.c

OUTPUT_DIR=bin
OUTPUT_FILE=$(OUTPUT_DIR)/transform

all: libs/scloud/bin/libscloud.so $(OUTPUT_DIR) $(OUTPUT_FILE)

libs/scloud/bin/libscloud.so:
	make -C libs/scloud

$(OUTPUT_FILE): $(addprefix $(SOURCE_DIR)/,$(SOURCES))
	$(CC) -Wall -I$(SOURCE_DIR) -Llibs/scloud/bin -lscloud -Ilibs/scloud/bin/include -Llibs/scloud/libs/tomcrypt/bin -ltomcrypt -Ilibs/scloud/libs/tomcrypt/bin/include -o $(OUTPUT_FILE) $^

$(OUTPUT_DIR):
	mkdir -p $(OUTPUT_DIR)

run: $(OUTPUT_FILE)
	@DYLD_LIBRARY_PATH=libs/scloud/bin:libs/scloud/libs/tommath/bin:libs/scloud/libs/tomcrypt/bin:libs/scloud/libs/yajl/bin:$(DYLD_LIBRARY_PATH) $(OUTPUT_FILE)

clean:
	make -C libs/scloud clean
	rm -fR $(OUTPUT_DIR)
