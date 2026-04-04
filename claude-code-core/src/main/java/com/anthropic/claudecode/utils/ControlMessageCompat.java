/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code control message compatibility utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Normalize control message keys for compatibility with older iOS app builds.
 * Converts camelCase `requestId` to snake_case `request_id`.
 */
public final class ControlMessageCompat {
    private ControlMessageCompat() {}

    /**
     * Normalize control message keys in place.
     * Converts camelCase `requestId` to snake_case `request_id`.
     *
     * @param obj The object to normalize (modified in place if it's a Map)
     * @return The normalized object
     */
    @SuppressWarnings("unchecked")
    public static Object normalizeControlMessageKeys(Object obj) {
        if (obj == null) return null;
        if (!(obj instanceof Map)) return obj;

        Map<String, Object> record = (Map<String, Object>) obj;

        // Normalize top-level requestId -> request_id
        if (record.containsKey("requestId") && !record.containsKey("request_id")) {
            record.put("request_id", record.remove("requestId"));
        }

        // Normalize nested response.requestId -> request_id
        Object response = record.get("response");
        if (response instanceof Map) {
            Map<String, Object> responseMap = (Map<String, Object>) response;
            if (responseMap.containsKey("requestId") && !responseMap.containsKey("request_id")) {
                responseMap.put("request_id", responseMap.remove("requestId"));
            }
        }

        return obj;
    }

    /**
     * Check if object has request_id field.
     */
    public static boolean hasRequestId(Object obj) {
        if (!(obj instanceof Map)) return false;
        Map<?, ?> record = (Map<?, ?>) obj;
        return record.containsKey("request_id");
    }

    /**
     * Get request_id from object.
     */
    public static String getRequestId(Object obj) {
        if (!(obj instanceof Map)) return null;
        Map<?, ?> record = (Map<?, ?>) obj;
        Object value = record.get("request_id");
        return value != null ? value.toString() : null;
    }

    /**
     * Check if object is a control request.
     */
    public static boolean isControlRequest(Object obj) {
        if (!(obj instanceof Map)) return false;
        Map<?, ?> record = (Map<?, ?>) obj;
        return "control_request".equals(record.get("type"));
    }

    /**
     * Check if object is a control response.
     */
    public static boolean isControlResponse(Object obj) {
        if (!(obj instanceof Map)) return false;
        Map<?, ?> record = (Map<?, ?>) obj;
        return "control_response".equals(record.get("type"));
    }
}