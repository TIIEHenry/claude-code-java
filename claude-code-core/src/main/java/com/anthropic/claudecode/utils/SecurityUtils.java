/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code security utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;
import java.security.*;
import java.nio.charset.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Security and sanitization utilities.
 */
public final class SecurityUtils {
    private SecurityUtils() {}

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(('|\")(--|;|\\/\\*|\\*\\/)|((union|select|insert|delete|update|drop|create|alter|exec)\\s))"
    );
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\.\\/|\\.\\.\\\\)");
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "[;&|`$(){}\\[\\]\\*\\?<>]"
    );

    /**
     * Sanitize HTML by removing script tags.
     */
    public static String sanitizeHtml(String html) {
        if (html == null) return null;
        return SCRIPT_PATTERN.matcher(html).replaceAll("");
    }

    /**
     * Strip all HTML tags.
     */
    public static String stripHtmlTags(String html) {
        if (html == null) return null;
        return HTML_TAG_PATTERN.matcher(html).replaceAll("");
    }

    /**
     * Escape HTML entities.
     */
    public static String escapeHtml(String text) {
        if (text == null) return null;
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Unescape HTML entities.
     */
    public static String unescapeHtml(String html) {
        if (html == null) return null;
        return html
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
            .replace("&nbsp;", " ");
    }

    /**
     * Sanitize input by removing potentially dangerous characters.
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'&]", "");
    }

    /**
     * Escape for JavaScript string.
     */
    public static String escapeJavaScript(String text) {
        if (text == null) return null;
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Escape for SQL LIKE pattern.
     */
    public static String escapeSqlLike(String pattern) {
        if (pattern == null) return null;
        return pattern
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    /**
     * Check for potential SQL injection.
     */
    public static boolean hasSqlInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check for potential path traversal.
     */
    public static boolean hasPathTraversal(String input) {
        if (input == null) return false;
        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Check for potential command injection.
     */
    public static boolean hasCommandInjection(String input) {
        if (input == null) return false;
        return COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize filename by removing unsafe characters.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) return null;
        // Remove path separators and null bytes
        return filename.replaceAll("[/\\\\\\x00]", "_")
                      .replaceAll("\\.\\.", "_");
    }

    /**
     * Sanitize path to prevent traversal.
     */
    public static String sanitizePath(String path) {
        if (path == null) return null;
        // Remove path traversal sequences
        return PATH_TRAVERSAL_PATTERN.matcher(path).replaceAll("");
    }

    /**
     * Generate secure hash.
     */
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }

    /**
     * Generate SHA-1 hash.
     */
    public static String sha1(String input) {
        return hash(input, "SHA-1");
    }

    /**
     * Generate MD5 hash.
     */
    public static String md5(String input) {
        return hash(input, "MD5");
    }

    /**
     * Hash with algorithm.
     */
    public static String hash(String input, String algorithm) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate HMAC.
     */
    public static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert bytes to hex string.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Generate random salt.
     */
    public static String generateSalt(int length) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Simple password hashing with salt.
     */
    public static String hashPassword(String password, String salt) {
        return sha256(salt + password + salt);
    }

    /**
     * Verify password against hash.
     */
    public static boolean verifyPassword(String password, String salt, String hash) {
        return hashPassword(password, salt).equals(hash);
    }

    /**
     * Generate secure token.
     */
    public static String generateToken(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate API key.
     */
    public static String generateApiKey() {
        return "sk-" + generateToken(32);
    }

    /**
     * Check password strength.
     */
    public static PasswordStrength checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.EMPTY;
        }

        int score = 0;

        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;

        if (score < 3) return PasswordStrength.WEAK;
        if (score < 5) return PasswordStrength.MEDIUM;
        if (score < 7) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }

    /**
     * Password strength enumeration.
     */
    public enum PasswordStrength {
        EMPTY, WEAK, MEDIUM, STRONG, VERY_STRONG
    }

    /**
     * Mask sensitive data.
     */
    public static String mask(String data, int visibleChars) {
        if (data == null) return null;
        if (data.length() <= visibleChars * 2) {
            return "*".repeat(data.length());
        }
        return data.substring(0, visibleChars) +
               "*".repeat(data.length() - visibleChars * 2) +
               data.substring(data.length() - visibleChars);
    }

    /**
     * Mask credit card number.
     */
    public static String maskCreditCard(String cardNumber) {
        if (cardNumber == null) return null;
        String cleaned = cardNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 8) return cleaned;
        return cleaned.substring(0, 4) + " **** **** " + cleaned.substring(cleaned.length() - 4);
    }

    /**
     * Mask email address.
     */
    public static String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex < 2) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Mask phone number.
     */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() < 7) return cleaned;
        return cleaned.substring(0, 3) + "-***-" + cleaned.substring(cleaned.length() - 4);
    }

    /**
     * Check if string contains sensitive data patterns.
     */
    public static boolean containsSensitiveData(String text) {
        if (text == null) return false;

        // Credit card pattern
        if (text.matches(".*\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b.*")) {
            return true;
        }

        // SSN pattern
        if (text.matches(".*\\b\\d{3}-\\d{2}-\\d{4}\\b.*")) {
            return true;
        }

        // Email pattern
        if (text.matches(".*\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b.*")) {
            return true;
        }

        return false;
    }

    /**
     * Redact sensitive data from string.
     */
    public static String redactSensitiveData(String text) {
        if (text == null) return null;

        // Credit card
        text = text.replaceAll("\\b(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})\\b", "$1-****-****-$4");

        // SSN
        text = text.replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "***-**-****");

        // Email
        text = text.replaceAll("\\b([A-Za-z0-9])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Z|a-z]{2,})\\b", "$1***@$2");

        return text;
    }

    /**
     * Validate credit card number (Luhn algorithm).
     */
    public static boolean isValidCreditCard(String cardNumber) {
        if (cardNumber == null) return false;

        String cleaned = cardNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 13 || cleaned.length() > 19) return false;

        int sum = 0;
        boolean alternate = false;

        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int digit = cleaned.charAt(i) - '0';

            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    /**
     * Validate email address.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(pattern);
    }

    /**
     * Validate URL.
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    }