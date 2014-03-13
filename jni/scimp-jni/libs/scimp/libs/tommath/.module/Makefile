# This is the core Makefile. It is awesome.

ifeq ($(MODULE),)
	MODULE=_____
endif

ifeq ($(SOURCE_DIR),)
	SOURCE_DIR=.
endif

ifeq ($(EXPORTS),)
	EXPORTS=$(SOURCE_DIR)
endif

ifeq ($(INCLUDES),)
	INCLUDES=$(SOURCE_DIR)
endif

ifeq ($(SOURCES),)
	SOURCES=$(wildcard $(SOURCE_DIR)/*.c)
endif

OUTPUT_DIR=bin
LIBRARY_DIR=libs
OBJECTS_OUTPUT_DIR=$(OUTPUT_DIR)/objects
INCLUDES_OUTPUT_DIR=$(OUTPUT_DIR)/include
OUTPUT_FILE=$(OUTPUT_DIR)/lib$(MODULE).so
IMPORT_DIRS=$(addprefix $(LIBRARY_DIR)/,$(IMPORTS))
IMPORT_OUTPUT_DIRS=$(addsuffix /bin,$(IMPORT_DIRS))
IMPORT_INCLUDES=$(addsuffix /include,$(IMPORT_OUTPUT_DIRS))
IMPORT_LIBRARIES=$(addsuffix /$(wildcard lib*.so),$(IMPORT_OUTPUT_DIRS))

OBJECTS=$(addprefix $(OBJECTS_OUTPUT_DIR)/,$(SOURCES:.c=.o))
EXPORT_FILES=$(addsuffix /*.h,$(EXPORTS))

CFLAGS+=-fPIC -g -c -Wall $(addprefix -I,$(IMPORT_INCLUDES) $(INCLUDES))
LDFLAGS+=-shared -lc $(addprefix -L,$(IMPORT_OUTPUT_DIRS)) $(addprefix -l,$(IMPORTS))
COMPILE.c=$(CC) $(CFLAGS) $(IFLAGS)
LINK.c=$(CC) $(LDFLAGS)

.PHONY=\
  all \
  clean \
  -make-dependencies, \
  -clean-dependencies, \
  -mkdirs

all: $(addprefix -make-,$(IMPORTS)) -mkdirs $(OUTPUT_FILE)

$(OUTPUT_FILE): $(OBJECTS)

ifneq (,$(OBJECTS))
	@echo "$(MODULE) [$(CC)] $(OBJECTS_OUTPUT_DIR) -> $(OUTPUT_FILE)"
	@$(LINK.c) -o $(OUTPUT_FILE) $^
endif

ifneq (,$(EXPORT_FILES))
	@echo "$(MODULE) [cp] $(EXPORT_FILES) -> $(INCLUDES_OUTPUT_DIR)"
	@cp $(EXPORT_FILES) $(INCLUDES_OUTPUT_DIR)/
endif

clean: $(addprefix -clean-,$(IMPORTS))
	@echo "$(MODULE) [rm] $(OUTPUT_DIR)"
	@rm -fR $(OUTPUT_DIR)

-make-%:
	@$(MAKE) -C $(LIBRARY_DIR)/$(subst -make-,,$@)

-clean-%:
	@$(MAKE) -C $(LIBRARY_DIR)/$(subst -clean-,,$@) clean

-mkdirs:

ifneq (,$(OBJECTS))
	@mkdir -p $(OBJECTS_OUTPUT_DIR)
endif

ifneq (,$(EXPORT_FILES))
	@mkdir -p $(INCLUDES_OUTPUT_DIR)
endif

$(OBJECTS_OUTPUT_DIR)/%.o: $(addprefix $(SOURCE_DIR)/,%.c)
	@echo "$(MODULE) [$(CC)] $^ -> $@"
	@test -d $(dir $@) || mkdir -p $(dir $@)
	@$(COMPILE.c) -o $@ $^
