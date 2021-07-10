package com.hoho.android.usbserial.examples;

import android.os.Environment;

import java.io.File;

/**
 * Created by ning on 17/9/1.
 */

public class Utils {


    /**
     在Java中:
     char 16位 范围是2负的2的15次方到2的15次方的整数
     byte 虽然是8位，但是取值范围是负的2的7次方到2的7次方的整数
     在Java中，不存在Unsigned无符号数据类型，但可以轻而易举的完成Unsigned转换。

     方案一：如果在Java中进行流(Stream)数据处理，可以用DataInputStream类对Stream中的数据以Unsigned读取。
     Java在这方面提供了支持，可以用java.io.DataInputStream 类对象来完成对流内数据的Unsigned读取，该类提供了如下方法：
     （1）int readUnsignedByte () //从流中读取一个0~255(0xFF)的单字节数据，并以int数据类型的数据返回。返回的数据相当于C/C++语言中所谓的“BYTE”。
     （2）int readUnsignedShort () //从流中读取一个0~65535(0xFFFF)的双字节数据，并以int数据类型的数据返回。返回的数据相当于C/C++语言中所谓的“WORD”， 并且是以“低地址低字节”的方式返回的，所以程序员不需要额外的转换。
     方案二：利用Java位运算符，完成Unsigned转换。
     正常情况下，Java提供的数据类型是有符号signed类型的，可以通过位运算的方式得到它们相对应的无符号值，参见几个方法中的代码：
     public int getUnsignedByte (byte data){ //将data字节型数据转换为0~255 (0xFF 即BYTE)。
     return data&0x0FF ;
     }
     public int getUnsignedByte (short data){ //将data字节型数据转换为0~65535 (0xFFFF 即 WORD)。
     return data&0x0FFFF ;
     }
     public long getUnsignedIntt (int data){ //将int数据转换为0~4294967295 (0xFFFFFFFF即DWORD)。
     return data&0x0FFFFFFFF ;
     }
     */

    /**
     * 将data字节型数据转换为0~255 (0xFF 即BYTE)。
     * @param data data
     * @return int
     */
    public static int getUnsignedByte(byte data) {
        return data & 0x0FF;
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

    public static int byteArrayToInt(byte[] b, int offset) {
        return   b[0 + offset] & 0xFF |
                (b[1 + offset] & 0xFF) << 8 |
                (b[2 + offset] & 0xFF) << 16 |
                (b[3 + offset] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /**
     * 通过byte数组取到short
     *
     * @param b byte
     * @param offset  第几位开始取
     * @return short
     */
    public static short byteArrayToShort(byte[] b, int offset) {
        return (short) (((b[offset + 1] << 8) | b[offset + 0] & 0xff));
    }


    //递归删除文件夹
    public static boolean deleteFile(File file) {
        if (file.exists()) {//判断文件是否存在
            if (file.isFile()) {//判断是否是文件
                return  file.delete();//删除文件
            } else if (file.isDirectory()) {//否则如果它是一个目录
                File[] files = file.listFiles();//声明目录下所有的文件 files[];
                for (int i = 0;i < files.length;i ++) {//遍历目录下所有的文件
                    deleteFile(files[i]);//把每个文件用这个方法进行迭代
                }
                return file.delete();//删除文件夹
            }
        } else {
            System.out.println("File not found");
            return false;
        }
        return false;
    }

    public boolean isExternalStorageEnough() {
        File sdcard_filedir = Environment.getExternalStorageDirectory();//得到sdcard的目录作为一个文件对象
        long usableSpace = sdcard_filedir.getUsableSpace();//获取文件目录对象剩余空间
        long totalSpace = sdcard_filedir.getTotalSpace();
        if(usableSpace < 1024 * 1024 * 100){//判断剩余空间是否小于100M
            return false;
        }
        return true;
    }
}
