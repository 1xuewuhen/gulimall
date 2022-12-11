package com.xwh.gulimall.auth.util;

import java.util.Random;

public class VerificationCodeUtil {

    public static String generateVerificationCode() {
        String code = "1234567890";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int j = new Random().nextInt(10);
            builder.append(code.charAt(j));
        }
        return builder.toString();
    }
}
