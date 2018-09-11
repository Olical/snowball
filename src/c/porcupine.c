#include <stdio.h>
#include <pv_porcupine.h>
#include <snowball_porcupine_Porcupine.h>

JNIEXPORT jlong JNICALL Java_snowball_porcupine_Porcupine_init
  (JNIEnv *env, jobject obj, jstring model_raw, jstring keyword_raw, jfloat sens) {
   const char *model = (*env)->GetStringUTFChars(env, model_raw, 0);
   const char *keyword = (*env)->GetStringUTFChars(env, keyword_raw, 0);
   pv_porcupine_object_t *handle;

   const pv_status_t status = pv_porcupine_init(model, keyword, sens, &handle);

   if (status != PV_STATUS_SUCCESS) {
       printf("Error: Failed to initialise Snowball's Porcupine instance.");
   }

   (*env)->ReleaseStringUTFChars(env, model_raw, model);
   (*env)->ReleaseStringUTFChars(env, keyword_raw, keyword);

   return (long)handle;
}

JNIEXPORT void JNICALL Java_snowball_porcupine_Porcupine_delete
  (JNIEnv *env, jobject ob, jlong handle) {
  pv_porcupine_delete((pv_porcupine_object_t*)handle);
}

JNIEXPORT jint JNICALL Java_snowball_porcupine_Porcupine_getFrameLength
  (JNIEnv *env, jobject obj) {
  return pv_porcupine_frame_length();
}

JNIEXPORT jint JNICALL Java_snowball_porcupine_Porcupine_getSampleRate
  (JNIEnv *env, jobject obj) {
  return pv_sample_rate();
}

JNIEXPORT jboolean JNICALL Java_snowball_porcupine_Porcupine_process
  (JNIEnv *env, jobject obj, jlong handle, jshortArray pcm_raw) {
  jshort *pcm = (*env)->GetShortArrayElements(env, pcm_raw, 0);
  bool *result;

  pv_porcupine_process((pv_porcupine_object_t*)handle, pcm, result);

  return *result;
}
