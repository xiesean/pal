package org.libsdl.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author shx
 * 压缩和解压缩工具
 */
public class ZipUtils {

    /**
     * 压缩方法
     *
     * @param str  要压缩的字符串
     * @param path 路径
     * @throws IOException
     */
    public static void compress(String str, String path) throws IOException {
        if (null == str || str.length() <= 0) {
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(path);
        GZIPOutputStream gzip = new GZIPOutputStream(fileOutputStream);
        gzip.write(str.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        fileOutputStream.close();

    }

    /**
     * 解压缩
     *
     * @param path
     * @return
     */
    public static String unCompress(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return "文件不存在";
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // 创建一个新的输出流
            FileInputStream fileInputStream = new FileInputStream(path);
            GZIPInputStream gzip = new GZIPInputStream(fileInputStream);

            byte[] buffer = new byte[256];
            int n = 0;

            // 将未压缩数据读入字节数组
            while ((n = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }

            return out.toString("utf-8");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}