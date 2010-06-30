package com.vodafone360.people.utils;

public class MathUtils {
    public static int convertBytesToInt(final byte b1, final byte b2, final byte b3, final byte b4) {
        int i = 0;
        i += b1 & 0xFF << 24;
        i += b2 & 0xFF << 16;
        i += b3 & 0xFF << 8;
        i += b4 & 0xFF << 0;
        
        return i;
    }
}
