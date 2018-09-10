#include <stdio.h>
#include <pv_porcupine.h>
#include <porcupine.h>

/* JNIEXPORT jstring JNICALL Java_Test_print(JNIEnv *env, jobject obj, jstring name) { */
/*   const char* cname = (*env)->GetStringUTFChars(env, name, 0); */
/*   printf("Hello from C, %s!\n", cname); */
/*   (*env)->ReleaseStringUTFChars(env, name, cname); */
/*   return (*env)->NewStringUTF(env, "Hello from C!"); */
/* } */

/*
 * Class:     snowball_Porcupine
 * Method:    init
 * Signature: (Ljava/lang/String;Ljava/lang/String;F)J
 */
JNIEXPORT jlong JNICALL Java_snowball_Porcupine_init
  (JNIEnv *env, jobject obj, jstring model_raw, jstring keyword_raw, jfloat sens) {
   const char *model = (*env)->GetStringUTFChars(env, model_raw, 0);
   const char *keyword = (*env)->GetStringUTFChars(env, keyword_raw, 0);
   pv_porcupine_object_t *handle;

   const pv_status_t status = pv_porcupine_init(model, keyword, sens, &handle);

   (*env)->ReleaseStringUTFChars(env, model_raw, model);
   (*env)->ReleaseStringUTFChars(env, keyword_raw, keyword);

   return status;
}

/*
 * Class:     snowball_Porcupine
 * Method:    getFrameLength
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_snowball_Porcupine_getFrameLength
  (JNIEnv *, jobject);

/*
 * Class:     snowball_Porcupine
 * Method:    getSampleRate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_snowball_Porcupine_getSampleRate
  (JNIEnv *, jobject);

/*
 * Class:     snowball_Porcupine
 * Method:    process
 * Signature: (J[S)I
 */
JNIEXPORT jint JNICALL Java_snowball_Porcupine_process
  (JNIEnv *, jobject, jlong, jshortArray);

/*
 * Class:     snowball_Porcupine
 * Method:    delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_snowball_Porcupine_delete
  (JNIEnv *, jobject, jlong);
