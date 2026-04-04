/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AttachmentUtils.
 */
class AttachmentUtilsTest {

    @Test
    @DisplayName("AttachmentUtils AttachmentType enum values")
    void attachmentTypeEnum() {
        AttachmentUtils.AttachmentType[] types = AttachmentUtils.AttachmentType.values();
        assertEquals(3, types.length);
    }

    @Test
    @DisplayName("AttachmentUtils isImage true for images")
    void isImageTrue() {
        assertTrue(AttachmentUtils.isImage("test.png"));
        assertTrue(AttachmentUtils.isImage("test.jpg"));
        assertTrue(AttachmentUtils.isImage("test.jpeg"));
        assertTrue(AttachmentUtils.isImage("test.gif"));
        assertTrue(AttachmentUtils.isImage("test.webp"));
        assertTrue(AttachmentUtils.isImage("test.svg"));
        assertTrue(AttachmentUtils.isImage("test.bmp"));
    }

    @Test
    @DisplayName("AttachmentUtils isImage false for non-images")
    void isImageFalse() {
        assertFalse(AttachmentUtils.isImage("test.pdf"));
        assertFalse(AttachmentUtils.isImage("test.txt"));
        assertFalse(AttachmentUtils.isImage(null));
        assertFalse(AttachmentUtils.isImage(""));
        assertFalse(AttachmentUtils.isImage("noextension"));
    }

    @Test
    @DisplayName("AttachmentUtils isDocument true for documents")
    void isDocumentTrue() {
        assertTrue(AttachmentUtils.isDocument("test.pdf"));
        assertTrue(AttachmentUtils.isDocument("test.txt"));
        assertTrue(AttachmentUtils.isDocument("test.md"));
        assertTrue(AttachmentUtils.isDocument("test.csv"));
        assertTrue(AttachmentUtils.isDocument("test.json"));
        assertTrue(AttachmentUtils.isDocument("test.xml"));
        assertTrue(AttachmentUtils.isDocument("test.html"));
    }

    @Test
    @DisplayName("AttachmentUtils isDocument false for non-documents")
    void isDocumentFalse() {
        assertFalse(AttachmentUtils.isDocument("test.png"));
        assertFalse(AttachmentUtils.isDocument(null));
        assertFalse(AttachmentUtils.isDocument(""));
    }

    @Test
    @DisplayName("AttachmentUtils isAttachable true")
    void isAttachableTrue() {
        assertTrue(AttachmentUtils.isAttachable("test.png"));
        assertTrue(AttachmentUtils.isAttachable("test.pdf"));
        assertTrue(AttachmentUtils.isAttachable("test.txt"));
    }

    @Test
    @DisplayName("AttachmentUtils isAttachable false")
    void isAttachableFalse() {
        assertFalse(AttachmentUtils.isAttachable("test.xyz"));
        assertFalse(AttachmentUtils.isAttachable(null));
    }

    @Test
    @DisplayName("AttachmentUtils getType image")
    void getTypeImage() {
        assertEquals(AttachmentUtils.AttachmentType.IMAGE, AttachmentUtils.getType("test.png"));
    }

    @Test
    @DisplayName("AttachmentUtils getType document")
    void getTypeDocument() {
        assertEquals(AttachmentUtils.AttachmentType.DOCUMENT, AttachmentUtils.getType("test.pdf"));
    }

    @Test
    @DisplayName("AttachmentUtils getType other")
    void getTypeOther() {
        assertEquals(AttachmentUtils.AttachmentType.OTHER, AttachmentUtils.getType("test.xyz"));
    }

    @Test
    @DisplayName("AttachmentUtils formatSize bytes")
    void formatSizeBytes() {
        assertEquals("100 B", AttachmentUtils.formatSize(100));
        assertEquals("0 B", AttachmentUtils.formatSize(0));
    }

    @Test
    @DisplayName("AttachmentUtils formatSize KB")
    void formatSizeKB() {
        String result = AttachmentUtils.formatSize(2048);
        assertTrue(result.contains("KB"));
    }

    @Test
    @DisplayName("AttachmentUtils formatSize MB")
    void formatSizeMB() {
        String result = AttachmentUtils.formatSize(5 * 1024 * 1024);
        assertTrue(result.contains("MB"));
    }

    @Test
    @DisplayName("AttachmentUtils AttachmentInfo record")
    void attachmentInfoRecord() {
        AttachmentUtils.AttachmentInfo info = new AttachmentUtils.AttachmentInfo(
            "test.png", "/path/to/test.png", "image/png", 1024,
            AttachmentUtils.AttachmentType.IMAGE, true
        );
        assertEquals("test.png", info.filename());
        assertEquals("/path/to/test.png", info.path());
        assertEquals("image/png", info.mimeType());
        assertEquals(1024, info.size());
        assertTrue(info.withinSizeLimit());
    }

    @Test
    @DisplayName("AttachmentUtils AttachmentInfo formattedSize")
    void attachmentInfoFormattedSize() {
        AttachmentUtils.AttachmentInfo info = new AttachmentUtils.AttachmentInfo(
            "test.png", "/path", "image/png", 1024,
            AttachmentUtils.AttachmentType.IMAGE, true
        );
        assertTrue(info.formattedSize().contains("KB"));
    }

    @Test
    @DisplayName("AttachmentUtils isWithinSizeLimit null path")
    void isWithinSizeLimitNullPath() {
        assertFalse(AttachmentUtils.isWithinSizeLimit(null));
    }

    @Test
    @DisplayName("AttachmentUtils createAttachmentInfo null path")
    void createAttachmentInfoNullPath() {
        assertNull(AttachmentUtils.createAttachmentInfo(null));
    }

    @Test
    @DisplayName("AttachmentUtils readContent null path")
    void readContentNullPath() {
        assertNull(AttachmentUtils.readContent(null));
    }

    @Test
    @DisplayName("AttachmentUtils readAsBase64 null path")
    void readAsBase64NullPath() {
        assertNull(AttachmentUtils.readAsBase64(null));
    }

    @Test
    @DisplayName("AttachmentUtils readAsText null path")
    void readAsTextNullPath() {
        assertNull(AttachmentUtils.readAsText(null));
    }
}
