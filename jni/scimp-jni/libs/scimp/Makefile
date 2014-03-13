CC=/usr/bin/clang

MODULE=scimp

IMPORTS=\
  tommath \
  tomcrypt \
  yajl

EXPORTS=\
  src \
  shared

INCLUDES=\
  src \
  shared

SOURCES=\
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

include .module/Makefile
