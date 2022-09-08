#include <jni.h>
#include "SDL.h"
#include "video.h"
#include "input.h"
#include "hack.h"
#include "util.h"
#include "md5.h"
#include <stdio.h>
#include <string.h>

#include <android/log.h>

#define LOGINFO(...)  __android_log_print(ANDROID_LOG_INFO,"JNI",__VA_ARGS__)
#define LOGERROR(...)  __android_log_print(ANDROID_LOG_ERROR,"JNI",__VA_ARGS__)

extern unsigned short  g_wInitialWidth;
extern unsigned short  g_wInitialHeight;

static jmethodID g_methodShowJoystick;
static jmethodID g_methodHideJoystick;
static jmethodID g_methodShowBack;
static jmethodID g_methodHideBack;
static jmethodID g_methodShowSearch;
static jmethodID g_methodHideSearch;
static jmethodID g_methodRewordAd;
static JNIEnv* g_env;
static jclass g_class;

static bool g_isJoystickShow = false;
static bool g_isSearchShow = false;
static bool g_isBackShow = false;
static bool g_useJoyStick = true;


static bool g_hasInMainGame = false;

char g_resource_dir[1024];

extern "C" void Java_org_libsdl_app_SDLActivity_nativeSetGameDir(
                                    JNIEnv* env, jclass jcls,
                                    jstring jstr)
{
    const char* tmp =  env->GetStringUTFChars(jstr, 0);

    LOGI("tmp string is :%s",tmp);
    strcpy(g_resource_dir, tmp);

    LOGI("g_resource_dir string is :%s",g_resource_dir);
    env->ReleaseStringUTFChars(jstr,tmp);
}

// Resize
extern "C" void Java_org_libsdl_app_SDLActivity_nativeSetSize(
                                    JNIEnv* env, jclass jcls,
                                    jint width, jint height)
{
    if (width != 0 && height != 0) {
        g_wInitialWidth = (width >= height ? width : height);
        g_wInitialHeight = (width >= height ? height : width);
    }
}

extern "C" void Java_org_libsdl_app_SDLActivity_nativeDirChange(
                                    JNIEnv* env, jclass jcls,
                                    jint dir)
{
    switch (dir) {
        case 1:
            g_InputState.dir = kDirNorth;
            break;
        case 2:
            g_InputState.dir = kDirSouth;
            break;
        case 3:
            g_InputState.dir = kDirWest;
            break;
        case 4:
            g_InputState.dir = kDirEast;
            break;
        default:
            g_InputState.dir = kDirUnknown;
            break;
    }
}

extern "C" void Java_org_libsdl_app_SDLActivity_nativeSearch(
                                    JNIEnv* env, jclass jcls)
{
    g_InputState.dwKeyPress |= kKeySearch;
}


extern "C" void Java_org_libsdl_app_SDLActivity_nativeBack(
                                    JNIEnv* env, jclass jcls)
{
    g_InputState.dwKeyPress |= kKeyMenu;
}

extern "C" void Java_org_libsdl_app_SDLActivity_nativeHack(JNIEnv* env, jclass jcls, jint type, jint value)
{
    switch (type) {
        case 0:
        setFlyMode(value != 0);
        break;
        case 1:
        uplevel();
        break;
        case 2:
        addMoney();
        case 3:
        addExp(value);
        case 4:
        addMoney2(value);
        break;
    }
}

extern "C" int Java_org_libsdl_app_SDLActivity_nativeGetFlyMode(JNIEnv* env, jclass jcls)
{
    return isFlyMode();
}

extern "C" void Java_org_libsdl_app_SDLActivity_nativeChangeBattleMode(
                                    JNIEnv* env, jclass jcls, jint mode)
{
    extern BOOL g_isClassicMode;
    g_isClassicMode = mode != 0 ? true : false;
}


extern "C" void Java_org_libsdl_app_SDLActivity_nativeChangeJoystick(
                                    JNIEnv* env, jclass jcls, jint mode)
{
    g_useJoyStick = mode != 0 ? true : false;


}



// 传0 sdl会自动全屏
extern "C" void getScreenSize(int* width, int *height)
{
    if (width) {
        *width = 0;
    }

    if (height) {
        *height = 0;
    }
}

extern "C" void Hex2Str( const char *sSrc,  char *sDest, int nSrcLen )
           {
               int  i;
               char szTmp[3];

               for( i = 0; i < nSrcLen; i++ )
               {
                   sprintf( szTmp, "%02X", (unsigned char) sSrc[i] );
                   memcpy( &sDest[i * 2], szTmp, 2 );
               }
               return ;
           }

extern "C" jstring ToMd5(JNIEnv *env, jbyteArray source) {
               // MessageDigest类
               jclass classMessageDigest = env->FindClass("java/security/MessageDigest");
               // MessageDigest.getInstance()静态方法
               jmethodID midGetInstance = env->GetStaticMethodID(classMessageDigest, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
               // MessageDigest object
               jobject objMessageDigest = env->CallStaticObjectMethod(classMessageDigest, midGetInstance, env->NewStringUTF("md5"));
               // update方法，这个函数的返回值是void，写V
               jmethodID midUpdate = env->GetMethodID(classMessageDigest, "update", "([B)V");
               env->CallVoidMethod(objMessageDigest, midUpdate, source);
               // digest方法
               jmethodID midDigest = env->GetMethodID(classMessageDigest, "digest", "()[B");
               jbyteArray objArraySign = (jbyteArray) env->CallObjectMethod(objMessageDigest, midDigest);
               jsize intArrayLength = env->GetArrayLength(objArraySign);
               jbyte* byte_array_elements = env->GetByteArrayElements(objArraySign, NULL);
               size_t length = (size_t) intArrayLength * 2 + 1;
               char* char_result = (char*) malloc(length);
               memset(char_result, 0, length);
               // 将byte数组转换成16进制字符串，发现这里不用强转，jbyte和unsigned char应该字节数是一样的
               Hex2Str((const char*)byte_array_elements, char_result, intArrayLength);
               // 在末尾补\0
               *(char_result + intArrayLength * 2) = '\0';
               jstring stringResult = env->NewStringUTF(char_result);
               // release
               env->ReleaseByteArrayElements(objArraySign, byte_array_elements, JNI_ABORT);
               // 释放指针使用free
               free(char_result);
               return stringResult;
           }
extern "C" jstring loadSignature(JNIEnv *env, jobject context)
{
    // 获取Context类
    jclass contextClass = env->GetObjectClass(context);
   // if (DEBUG_MODE)
        LOGI("获取Context类");
    // 得到getPackageManager方法的ID
    jmethodID getPkgManagerMethodId = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
   // if (DEBUG_MODE)
        LOGI("得到getPackageManager方法的ID");
    // PackageManager
    jobject pm = env->CallObjectMethod(context, getPkgManagerMethodId);
    //if (DEBUG_MODE)
        LOGI("PackageManager");
    // 得到应用的包名
    jmethodID pkgNameMethodId = env->GetMethodID(contextClass, "getPackageName", "()Ljava/lang/String;");
    jstring  pkgName = (jstring) env->CallObjectMethod(context, pkgNameMethodId);
   // if (DEBUG_MODE)
        LOGI("get pkg name: %s", getCharFromString(env, pkgName));
    // 获得PackageManager类
    jclass cls = env->GetObjectClass(pm);
    // 得到getPackageInfo方法的ID
    jmethodID mid = env->GetMethodID(cls, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    // 获得应用包的信息
    jobject packageInfo = env->CallObjectMethod(pm, mid, pkgName, 0x40); //GET_SIGNATURES = 64;
    // 获得PackageInfo 类
    cls = env->GetObjectClass(packageInfo);
    // 获得签名数组属性的ID
    jfieldID fid = env->GetFieldID(cls, "signatures", "[Landroid/content/pm/Signature;");
    // 得到签名数组
    jobjectArray signatures = (jobjectArray) env->GetObjectField(packageInfo, fid);
    // 得到签名
    jobject signature = env->GetObjectArrayElement(signatures, 0);
    // 获得Signature类
    cls = env->GetObjectClass(signature);
    // 得到toCharsString方法的ID
    mid = env->GetMethodID(cls, "toByteArray", "()[B");
    // 返回当前应用签名信息
    jbyteArray signatureByteArray = (jbyteArray) env->CallObjectMethod(signature, mid);
    return ToMd5(env, signatureByteArray);
}
extern "C" void initJNI(JNIEnv* env, jclass cls)
{
    if (!env) {
        return;
    }

    g_env = env;
    g_class = cls;
    g_methodRewordAd = env->GetStaticMethodID(cls, "rewordAd", "(II)V");
    g_methodShowJoystick = env->GetStaticMethodID(cls, "showJoystick", "()V");
    g_methodShowBack = env->GetStaticMethodID(cls, "showBackButton", "()V");
    g_methodShowSearch = env->GetStaticMethodID(cls, "showSearchButton", "()V");

    g_methodHideJoystick = env->GetStaticMethodID(cls, "hideJoystick", "()V");
    g_methodHideBack = env->GetStaticMethodID(cls, "hideBackButton", "()V");
    g_methodHideSearch = env->GetStaticMethodID(cls, "hideSearchButton", "()V");

    jclass localClass = env->FindClass("android/app/ActivityThread");
    if (localClass != NULL) {
        jmethodID getapplication = env->GetStaticMethodID(localClass, "currentApplication",
                                                          "()Landroid/app/Application;");
        if (getapplication != NULL) {
            jobject application = env->CallStaticObjectMethod(localClass, getapplication);
            jclass context = env->GetObjectClass(application);

            // 签名验证
            jmethodID methodID_sign = env->GetMethodID(context, "getPackageCodePath",
                                                       "()Ljava/lang/String;");
            jstring s_path = static_cast<jstring>(env->CallObjectMethod(application, methodID_sign));
            const char *ch_path = env->GetStringUTFChars(s_path, 0);;
            LOGI("%s", ch_path);
            //uncompress_apk(ch_path, "META-INF/CERT.RSA");//.SF
            env->ReleaseStringUTFChars(s_path, ch_path);


            // 包名验证
            jmethodID methodID_pgk = env->GetMethodID(context, "getPackageName",
                                                       "()Ljava/lang/String;");
            jstring path = static_cast<jstring>(env->CallObjectMethod(application, methodID_pgk));
            const char *ch = env->GetStringUTFChars(path, 0);;


// 简单签名验证
// 获取应用当前的签名信息
    jstring signature = loadSignature(env, application);
    // 期待的签名信息
    jstring keystoreSigature = env->NewStringUTF("c9ddc135857a5b0953fb8047ec7098e5");
    jstring keystoreSigature2 = env->NewStringUTF("8789d24b83c9773a0711028848ab9cba");
    const char *keystroreMD5 = env->GetStringUTFChars(keystoreSigature, NULL);
    const char *keystroreMD52 = env->GetStringUTFChars(keystoreSigature2, NULL);
    const char *releaseMD5 = env->GetStringUTFChars(signature, NULL);


            char src[100];
            sprintf(src, "%s%s%s",releaseMD5,"palsb",releaseMD5);
        //LOGINFO("签名md5 %s", src);

            MD5_CTX ctx1 = { 0 };
            MD5Init(&ctx1);
            MD5Update(&ctx1, (unsigned char*)src, strlen(src));
            unsigned char dest1[16] = { 0 };
            MD5Final(dest1, &ctx1);

            int i = 0;
            char szMd51[33] = { 0 };
            for (i = 0; i < 16; i++)
            {
                sprintf(szMd51, "%s%02x", szMd51, dest1[i]);
            }
        //LOGINFO("签名md5 %s", szMd51);

            // 比较两个签名信息是否相等
            int result = strcmp(keystroreMD5, szMd51);
            if(result != 0 ){
                result = strcmp(keystroreMD52, szMd51);
            }
            if(result !=0){
                exit(0);
            }
    // 这里记得释放内存
    env->ReleaseStringUTFChars(signature, releaseMD5);
    env->ReleaseStringUTFChars(keystoreSigature, keystroreMD5);
    env->ReleaseStringUTFChars(keystoreSigature2, keystroreMD52);
    // 得到的签名一样，验证通过
    //if (result == 0){
     //   LOGI("strcmp %d", result);
    //}
// 签名验证结束
            //LOGINFO("Sign: %s", ch);
            MD5_CTX ctx = { 0 };
            MD5Init(&ctx);
            MD5Update(&ctx, (unsigned char*)ch, strlen(ch));
            unsigned char dest[16] = { 0 };
            MD5Final(dest, &ctx);

            int j = 0;
            char szMd5[33] = { 0 };
            for (j = 0; j < 16; j++)
            {
                sprintf(szMd5, "%s%02x", szMd5, dest[j]);
            }
            //LOGINFO("Sign:MD5->%s", szMd5);

            if (strcmp(szMd5, "0179a1e7c0afa74eaea26fac6cfa27d4") == 0) {
                LOGINFO("Sign:MD5->验证通过 ");
            } else if (strcmp(szMd5, "ea2a3f056d66d0e436c2d660a9c614b7") == 0) {
                LOGINFO("Sign:MD5->验证通过 ");
            } else {
                LOGINFO("Sign:MD5->验证失败 ");
                exit(0);
            }


            env->ReleaseStringUTFChars(path, ch);
        }
    }
}

extern "C" void initButton()
{
    if (!g_env || !g_class) {
        return;
    }
}

extern "C" void hideMenu()
{
    hideJoystick();
    //hideSearchButton();
}

extern "C" void showMenu()
{
    if (!g_env || !g_class) {
        return;
    }

// 显示菜单按钮，android下面只需要显示调查按钮
    if (g_isSearchShow == false) {
        g_env->CallStaticVoidMethod(g_class, g_methodShowSearch);
        g_isSearchShow = true;
    }
    // 显示菜单按钮
    if (g_isBackShow == false) {
        g_env->CallStaticVoidMethod(g_class, g_methodShowBack);
        g_isBackShow = true;
    }

}

extern "C" void showRewordAd(int exp,int money)
{
        g_env->CallStaticVoidMethod(g_class, g_methodRewordAd,exp,money);
}

extern "C" void showJoystick()
{

    if (!g_env || !g_class) {
        return;
    }

    g_hasInMainGame = true;

    if (!g_useJoyStick)
    {
        return;
    }

    if (g_isJoystickShow == false)
    {
        LOGINFO("showJoystick");
        g_env->CallStaticVoidMethod(g_class, g_methodShowJoystick);
        g_isJoystickShow = true;
    }
}

extern "C" void hideJoystick()
{
    if (!g_env || !g_class) {
        return;
    }

    if (g_useJoyStick)
    {
        return;
    }

    if (g_isJoystickShow == true)
    {
        LOGINFO("hideJoystick");
        g_env->CallStaticVoidMethod(g_class, g_methodHideJoystick);
        g_isJoystickShow = false;
    }
}

extern "C" void initDir()
{
}

extern "C" void showBackButton()
{
    if (!g_env || !g_class) {
        return;
    }

    if (g_isBackShow == false)
    {
        g_env->CallStaticVoidMethod(g_class, g_methodShowBack);
        g_isBackShow = true;
    }
};
extern "C" void hideBackButton()
{
    if (!g_env || !g_class) {
        return;
    }

    if (g_isBackShow == true)
    {
        g_env->CallStaticVoidMethod(g_class, g_methodHideBack);
        g_isBackShow = false;
    }
    
};
extern "C" void hideSearchButton()
{
    if (!g_env || !g_class) {
        return;
    }

    if (g_isSearchShow == true)
    {
        g_env->CallStaticVoidMethod(g_class, g_methodHideSearch);
        g_isSearchShow = false;
    }
};


extern "C" void switchShowMenu()
{
    extern BOOL g_showSystemMenu;
    if (g_showSystemMenu) {
        g_InputState.dwKeyPress |= kKeyMenu;
    } else {
        g_InputState.dwKeyPress |= kKeyMainMenu;
    }
}