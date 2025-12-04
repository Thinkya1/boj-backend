package com.yupi.yuoj.utils;

public class MemoryUnitUtil {

    // 将 bytes 转为 KB（向上取整）
    public static long bytesToKb(long bytes) {
        if (bytes <= 0) return 0L;
        return (bytes + 1023L) / 1024L;
    }

    // 将 bytes 转为 MB（向上取整）
    public static long bytesToMb(long bytes) {
        if (bytes <= 0) return 0L;
        long kb = bytesToKb(bytes);
        return (kb + 1023L) / 1024L; // 先转 KB 再转 MB，保证向上取整
    }

    // 将 KB 转为 bytes
    public static long kbToBytes(long kb) {
        if (kb <= 0) return 0L;
        return kb * 1024L;
    }
}

