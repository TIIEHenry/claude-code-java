/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Attachments.
 */
class AttachmentsTest {

    @Test
    @DisplayName("Attachments AttachmentType enum values")
    void attachmentTypeEnum() {
        Attachments.AttachmentType[] types = Attachments.AttachmentType.values();
        assertEquals(5, types.length);
    }

    @Test
    @DisplayName("Attachments detectType image")
    void detectTypeImage() {
        assertEquals(Attachments.AttachmentType.IMAGE, Attachments.detectType("image/png"));
        assertEquals(Attachments.AttachmentType.IMAGE, Attachments.detectType("image/jpeg"));
        assertEquals(Attachments.AttachmentType.IMAGE, Attachments.detectType("image/gif"));
        assertEquals(Attachments.AttachmentType.IMAGE, Attachments.detectType("image/webp"));
    }

    @Test
    @DisplayName("Attachments detectType document")
    void detectTypeDocument() {
        assertEquals(Attachments.AttachmentType.DOCUMENT, Attachments.detectType("application/pdf"));
        assertEquals(Attachments.AttachmentType.DOCUMENT, Attachments.detectType("text/plain"));
        assertEquals(Attachments.AttachmentType.DOCUMENT, Attachments.detectType("text/markdown"));
        assertEquals(Attachments.AttachmentType.DOCUMENT, Attachments.detectType("application/json"));
        assertEquals(Attachments.AttachmentType.DOCUMENT, Attachments.detectType("text/csv"));
    }

    @Test
    @DisplayName("Attachments detectType file")
    void detectTypeFile() {
        assertEquals(Attachments.AttachmentType.FILE, Attachments.detectType("application/octet-stream"));
        assertEquals(Attachments.AttachmentType.FILE, Attachments.detectType("video/mp4"));
    }

    @Test
    @DisplayName("Attachments detectType unknown")
    void detectTypeUnknown() {
        assertEquals(Attachments.AttachmentType.UNKNOWN, Attachments.detectType(""));
    }

    @Test
    @DisplayName("Attachments detectType null throws NPE")
    void detectTypeNull() {
        assertThrows(NullPointerException.class, () -> Attachments.detectType(null));
    }

    @Test
    @DisplayName("Attachments detectMimeType png")
    void detectMimeTypePng() {
        assertEquals("image/png", Attachments.detectMimeType("test.png"));
    }

    @Test
    @DisplayName("Attachments detectMimeType jpg")
    void detectMimeTypeJpg() {
        assertEquals("image/jpeg", Attachments.detectMimeType("test.jpg"));
        assertEquals("image/jpeg", Attachments.detectMimeType("test.jpeg"));
    }

    @Test
    @DisplayName("Attachments detectMimeType gif")
    void detectMimeTypeGif() {
        assertEquals("image/gif", Attachments.detectMimeType("test.gif"));
    }

    @Test
    @DisplayName("Attachments detectMimeType pdf")
    void detectMimeTypePdf() {
        assertEquals("application/pdf", Attachments.detectMimeType("test.pdf"));
    }

    @Test
    @DisplayName("Attachments detectMimeType text")
    void detectMimeTypeText() {
        assertEquals("text/plain", Attachments.detectMimeType("test.txt"));
        assertEquals("text/markdown", Attachments.detectMimeType("test.md"));
    }

    @Test
    @DisplayName("Attachments detectMimeType json")
    void detectMimeTypeJson() {
        assertEquals("application/json", Attachments.detectMimeType("test.json"));
    }

    @Test
    @DisplayName("Attachments detectMimeType csv")
    void detectMimeTypeCsv() {
        assertEquals("text/csv", Attachments.detectMimeType("test.csv"));
    }

    @Test
    @DisplayName("Attachments detectMimeType html")
    void detectMimeTypeHtml() {
        assertEquals("text/html", Attachments.detectMimeType("test.html"));
    }

    @Test
    @DisplayName("Attachments detectMimeType xml")
    void detectMimeTypeXml() {
        assertEquals("application/xml", Attachments.detectMimeType("test.xml"));
    }

    @Test
    @DisplayName("Attachments detectMimeType unknown")
    void detectMimeTypeUnknown() {
        assertEquals("application/octet-stream", Attachments.detectMimeType("test.xyz"));
        assertEquals("application/octet-stream", Attachments.detectMimeType(null));
    }

    @Test
    @DisplayName("Attachments isValidSize true for valid")
    void isValidSizeTrue() {
        assertTrue(Attachments.isValidSize(1));
        assertTrue(Attachments.isValidSize(1024));
        assertTrue(Attachments.isValidSize(20 * 1024 * 1024));
    }

    @Test
    @DisplayName("Attachments isValidSize false for invalid")
    void isValidSizeFalse() {
        assertFalse(Attachments.isValidSize(0));
        assertFalse(Attachments.isValidSize(-1));
        assertFalse(Attachments.isValidSize(20 * 1024 * 1024 + 1));
    }

    @Test
    @DisplayName("Attachments isSupportedType true for supported")
    void isSupportedTypeTrue() {
        assertTrue(Attachments.isSupportedType("image/png"));
        assertTrue(Attachments.isSupportedType("application/pdf"));
        assertTrue(Attachments.isSupportedType("text/plain"));
    }

    @Test
    @DisplayName("Attachments isSupportedType false for unsupported")
    void isSupportedTypeFalse() {
        assertFalse(Attachments.isSupportedType("video/mp4"));
        assertFalse(Attachments.isSupportedType("application/octet-stream"));
    }

    @Test
    @DisplayName("Attachments fromContent creates attachment")
    void fromContent() {
        Attachments.AttachmentInfo info = Attachments.fromContent("test.txt", "Hello", "text/plain");
        assertNotNull(info);
        assertEquals("test.txt", info.name());
        assertEquals("text/plain", info.mimeType());
        assertEquals(5, info.size());
    }

    @Test
    @DisplayName("Attachments AttachmentInfo isImage")
    void attachmentInfoIsImage() {
        Attachments.AttachmentInfo info = new Attachments.AttachmentInfo(
            "id", "test.png", null, Attachments.AttachmentType.IMAGE, "image/png", 100, null
        );
        assertTrue(info.isImage());
        assertFalse(info.isDocument());
    }

    @Test
    @DisplayName("Attachments AttachmentInfo isDocument")
    void attachmentInfoIsDocument() {
        Attachments.AttachmentInfo info = new Attachments.AttachmentInfo(
            "id", "test.pdf", null, Attachments.AttachmentType.DOCUMENT, "application/pdf", 100, null
        );
        assertTrue(info.isDocument());
        assertFalse(info.isImage());
    }

    @Test
    @DisplayName("Attachments AttachmentInfo isSupported")
    void attachmentInfoIsSupported() {
        Attachments.AttachmentInfo info = new Attachments.AttachmentInfo(
            "id", "test.png", null, Attachments.AttachmentType.IMAGE, "image/png", 100, null
        );
        assertTrue(info.isSupported());
    }

    @Test
    @DisplayName("Attachments AttachmentCollection empty")
    void attachmentCollectionEmpty() {
        Attachments.AttachmentCollection collection = Attachments.AttachmentCollection.empty();
        assertEquals(0, collection.totalCount());
        assertEquals(0, collection.totalSize());
    }

    @Test
    @DisplayName("Attachments AttachmentCollection add")
    void attachmentCollectionAdd() {
        Attachments.AttachmentInfo info = new Attachments.AttachmentInfo(
            "id", "test.png", null, Attachments.AttachmentType.IMAGE, "image/png", 100, null
        );
        Attachments.AttachmentCollection collection = Attachments.AttachmentCollection.empty();
        collection = collection.add(info);
        assertEquals(1, collection.totalCount());
        assertEquals(100, collection.totalSize());
    }

    @Test
    @DisplayName("Attachments AttachmentCollection getImages")
    void attachmentCollectionGetImages() {
        Attachments.AttachmentInfo image = new Attachments.AttachmentInfo(
            "id1", "test.png", null, Attachments.AttachmentType.IMAGE, "image/png", 100, null
        );
        Attachments.AttachmentInfo doc = new Attachments.AttachmentInfo(
            "id2", "test.pdf", null, Attachments.AttachmentType.DOCUMENT, "application/pdf", 100, null
        );
        Attachments.AttachmentCollection collection = Attachments.AttachmentCollection.empty()
            .add(image).add(doc);
        
        assertEquals(1, collection.getImages().size());
        assertEquals(1, collection.getDocuments().size());
    }
}
