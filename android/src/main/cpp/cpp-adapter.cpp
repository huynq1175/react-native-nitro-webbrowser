#include <jni.h>
#include "nitrowebbrowserOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::nitrowebbrowser::initialize(vm);
}
