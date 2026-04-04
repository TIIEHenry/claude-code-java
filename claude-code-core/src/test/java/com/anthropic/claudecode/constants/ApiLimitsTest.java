/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiLimits.
 */
class ApiLimitsTest {

    @Test
    @DisplayName("ApiLimits API_IMAGE_MAX_BASE64_SIZE is 5MB")
    void apiImageMaxBase64Size() {
        assertEquals(5 * 1024 * 1024, ApiLimits.API_IMAGE_MAX_BASE64_SIZE);
    }

    @Test
    @DisplayName("ApiLimits IMAGE_TARGET_RAW_SIZE is approximately 3.75MB")
    void imageTargetRawSize() {
        assertEquals((5 * 1024 * 1024 * 3) / 4, ApiLimits.IMAGE_TARGET_RAW_SIZE);
    }

    @Test
    @DisplayName("ApiLimits IMAGE_MAX_WIDTH is 2000")
    void imageMaxWidth() {
        assertEquals(2000, ApiLimits.IMAGE_MAX_WIDTH);
    }

    @Test
    @DisplayName("ApiLimits IMAGE_MAX_HEIGHT is 2000")
    void imageMaxHeight() {
        assertEquals(2000, ApiLimits.IMAGE_MAX_HEIGHT);
    }

    @Test
    @DisplayName("ApiLimits PDF_TARGET_RAW_SIZE is 20MB")
    void pdfTargetRawSize() {
        assertEquals(20 * 1024 * 1024, ApiLimits.PDF_TARGET_RAW_SIZE);
    }

    @Test
    @DisplayName("ApiLimits API_PDF_MAX_PAGES is 100")
    void apiPdfMaxPages() {
        assertEquals(100, ApiLimits.API_PDF_MAX_PAGES);
    }

    @Test
    @DisplayName("ApiLimits PDF_EXTRACT_SIZE_THRESHOLD is 3MB")
    void pdfExtractSizeThreshold() {
        assertEquals(3 * 1024 * 1024, ApiLimits.PDF_EXTRACT_SIZE_THRESHOLD);
    }

    @Test
    @DisplayName("ApiLimits PDF_MAX_EXTRACT_SIZE is 100MB")
    void pdfMaxExtractSize() {
        assertEquals(100 * 1024 * 1024, ApiLimits.PDF_MAX_EXTRACT_SIZE);
    }

    @Test
    @DisplayName("ApiLimits PDF_MAX_PAGES_PER_READ is 20")
    void pdfMaxPagesPerRead() {
        assertEquals(20, ApiLimits.PDF_MAX_PAGES_PER_READ);
    }

    @Test
    @DisplayName("ApiLimits PDF_AT_MENTION_INLINE_THRESHOLD is 10")
    void pdfAtMentionInlineThreshold() {
        assertEquals(10, ApiLimits.PDF_AT_MENTION_INLINE_THRESHOLD);
    }

    @Test
    @DisplayName("ApiLimits API_MAX_MEDIA_PER_REQUEST is 100")
    void apiMaxMediaPerRequest() {
        assertEquals(100, ApiLimits.API_MAX_MEDIA_PER_REQUEST);
    }

    @Test
    @DisplayName("ApiLimits token limits for models")
    void tokenLimits() {
        assertEquals(8192, ApiLimits.CLAUDE_3_5_SONNET_MAX_TOKENS);
        assertEquals(8192, ApiLimits.CLAUDE_3_5_HAIKU_MAX_TOKENS);
        assertEquals(4096, ApiLimits.CLAUDE_3_OPUS_MAX_TOKENS);
        assertEquals(16384, ApiLimits.CLAUDE_SONNET_4_6_MAX_TOKENS);
        assertEquals(16384, ApiLimits.CLAUDE_OPUS_4_6_MAX_TOKENS);
    }

    @Test
    @DisplayName("ApiLimits DEFAULT_MODEL is claude-sonnet-4-6")
    void defaultModel() {
        assertEquals("claude-sonnet-4-6", ApiLimits.DEFAULT_MODEL);
    }

    @Test
    @DisplayName("ApiLimits rate limits are defined")
    void rateLimits() {
        assertEquals(60, ApiLimits.MAX_REQUESTS_PER_MINUTE);
        assertEquals(100000, ApiLimits.MAX_TOKENS_PER_MINUTE);
    }

    @Test
    @DisplayName("ApiLimits request limits are defined")
    void requestLimits() {
        assertEquals(100000, ApiLimits.MAX_MESSAGE_LENGTH);
        assertEquals(50000, ApiLimits.MAX_SYSTEM_PROMPT_LENGTH);
        assertEquals(64, ApiLimits.MAX_TOOL_COUNT);
        assertEquals(64, ApiLimits.MAX_TOOL_NAME_LENGTH);
    }

    @Test
    @DisplayName("ApiLimits retry settings are defined")
    void retrySettings() {
        assertEquals(3, ApiLimits.MAX_RETRIES);
        assertEquals(1000, ApiLimits.RETRY_DELAY_MS);
        assertEquals(30000, ApiLimits.MAX_RETRY_DELAY_MS);
    }

    @Test
    @DisplayName("ApiLimits cache settings are defined")
    void cacheSettings() {
        assertTrue(ApiLimits.CACHE_ENABLED_DEFAULT);
        assertEquals(3600000, ApiLimits.CACHE_TTL_MS);
    }

    @Test
    @DisplayName("ApiLimits all token limits are positive")
    void allTokenLimitsPositive() {
        assertTrue(ApiLimits.CLAUDE_3_5_SONNET_MAX_TOKENS > 0);
        assertTrue(ApiLimits.CLAUDE_3_5_HAIKU_MAX_TOKENS > 0);
        assertTrue(ApiLimits.CLAUDE_3_OPUS_MAX_TOKENS > 0);
        assertTrue(ApiLimits.CLAUDE_SONNET_4_6_MAX_TOKENS > 0);
        assertTrue(ApiLimits.CLAUDE_OPUS_4_6_MAX_TOKENS > 0);
    }

    @Test
    @DisplayName("ApiLimits retry delay increases")
    void retryDelayIncreases() {
        assertTrue(ApiLimits.MAX_RETRY_DELAY_MS > ApiLimits.RETRY_DELAY_MS);
    }

    @Test
    @DisplayName("ApiLimits image dimensions are reasonable")
    void imageDimensionsReasonable() {
        assertTrue(ApiLimits.IMAGE_MAX_WIDTH > 0);
        assertTrue(ApiLimits.IMAGE_MAX_HEIGHT > 0);
        assertTrue(ApiLimits.IMAGE_MAX_WIDTH <= 10000);
        assertTrue(ApiLimits.IMAGE_MAX_HEIGHT <= 10000);
    }
}