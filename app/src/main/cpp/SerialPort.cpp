#include <stdlib.h>
#include <stdio.h>
#include <jni.h>

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <string.h>
#include "mtgpio.h"
#include "android/log.h"

static speed_t getBaudrate(jint baudrate) {
    switch (baudrate) {
        case 0:
            return B0;
        case 50:
            return B50;
        case 75:
            return B75;
        case 110:
            return B110;
        case 134:
            return B134;
        case 150:
            return B150;
        case 200:
            return B200;
        case 300:
            return B300;
        case 600:
            return B600;
        case 1200:
            return B1200;
        case 1800:
            return B1800;
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 500000:
            return B500000;
        case 576000:
            return B576000;
        case 921600:
            return B921600;
        case 1000000:
            return B1000000;
        case 1152000:
            return B1152000;
        case 1500000:
            return B1500000;
        case 2000000:
            return B2000000;
        case 2500000:
            return B2500000;
        case 3000000:
            return B3000000;
        case 3500000:
            return B3500000;
        case 4000000:
            return B4000000;
        default:
            return -1;
    }
}

#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static const char *TAG = "serial_port";

extern "C" JNIEXPORT jobject JNICALL
Java_com_leon_detonator_serial_SerialPort_open(JNIEnv *env, jobject thiz, jstring path,
                                             jint baudrate) {
    int fd;
    speed_t speed;
    jobject mFileDescriptor;

    LOGD("init native Check arguments");
    /* Check arguments */
    {
        speed = getBaudrate(baudrate);
        if (speed == -1) {
            /* TODO: throw an exception */
            LOGE("Invalid baudrate");
            return NULL;
        }
    }
    {
        int gpio_fd = open("/dev/mtgpio", O_RDWR);

        if (gpio_fd != -1) {
            ioctl(gpio_fd, GPIO_IOCQDATAOUT, 3);
            ioctl(gpio_fd, GPIO_IOCSDATAHIGH, 3);
            close(gpio_fd);
        }
    }

    LOGD("init native Opening device!");
    /* Opening device */
    {
        jboolean iscopy;
        const char *path_utf = env->GetStringUTFChars(path, &iscopy);

        LOGD("Opening serial port %s", path_utf);
//      fd = open(path_utf, O_RDWR | O_DIRECT | O_SYNC);
        fd = open(path_utf, O_RDWR | O_NOCTTY | O_NONBLOCK | O_NDELAY);

        LOGD("open() fd = %d", fd);
        env->ReleaseStringUTFChars(path, path_utf);
        if (fd == -1) {
            /* Throw an exception */
            LOGE("Cannot open port %d", baudrate);
            /* TODO: throw an exception */
            return NULL;
        }
    }

    LOGD("init native Configure device!");
    /* Configure device */
    {
        struct termios cfg;
        if (tcgetattr(fd, &cfg)) {

            LOGE("Configure device tcgetattr() failed 1");
            close(fd);
            return NULL;
        }

        cfmakeraw(&cfg);
        cfsetispeed(&cfg, speed);
        cfsetospeed(&cfg, speed);

        if (tcsetattr(fd, TCSANOW, &cfg)) {

            LOGE("Configure device tcsetattr() failed 2");
            close(fd);
            /* TODO: throw an exception */
            return NULL;
        }
    }

    /* Create a corresponding file descriptor */
    {
        jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
        jmethodID iFileDescriptor = env->GetMethodID(cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = env->GetFieldID(cFileDescriptor, "descriptor", "I");
        mFileDescriptor = env->NewObject(cFileDescriptor, iFileDescriptor);
        env->SetIntField(mFileDescriptor, descriptorID, (jint) fd);
    }

    return mFileDescriptor;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_leon_detonator_serial_SerialPort_close(JNIEnv *env, jobject thiz) {
    jclass SerialPortClass = env->GetObjectClass(thiz);
    jclass FileDescriptorClass = env->FindClass("java/io/FileDescriptor");

    jfieldID mFdID = env->GetFieldID(SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = env->GetFieldID(FileDescriptorClass, "descriptor", "I");

    jobject mFd = env->GetObjectField(thiz, mFdID);
    jint descriptor = env->GetIntField(mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
    {
        int gpio_fd = open("/dev/mtgpio", O_RDWR);

        if (gpio_fd != -1) {
            ioctl(gpio_fd, GPIO_IOCSDATALOW, 3);
            close(gpio_fd);
        }
    }
    return 1;
}
