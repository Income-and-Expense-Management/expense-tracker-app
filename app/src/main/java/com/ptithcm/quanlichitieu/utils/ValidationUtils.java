package com.ptithcm.quanlichitieu.utils;

import android.util.Patterns;

public class ValidationUtils {
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
