/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BufferedWriterUtil.
 */
class BufferedWriterUtilTest {

    @Test
    @DisplayName("BufferedWriterUtil createBufferedWriter creates writer")
    void createBufferedWriter() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(writeFn);

        assertNotNull(writer);
        writer.dispose();
    }

    @Test
    @DisplayName("BufferedWriterUtil write in immediate mode writes immediately")
    void writeImmediateMode() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(
            writeFn, 1000, 100, Long.MAX_VALUE, true
        );

        writer.write("test");
        writer.flush();

        assertFalse(outputs.isEmpty());
        assertEquals("test", outputs.get(0));

        writer.dispose();
    }

    @Test
    @DisplayName("BufferedWriterUtil flush flushes buffer")
    void flushWorks() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(
            writeFn, 10000, 100, Long.MAX_VALUE, false
        );

        writer.write("test1");
        writer.write("test2");
        writer.flush();

        assertEquals(1, outputs.size());
        assertEquals("test1test2", outputs.get(0));

        writer.dispose();
    }

    @Test
    @DisplayName("BufferedWriterUtil flush on empty buffer does not throw")
    void flushEmptyBuffer() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(writeFn);

        assertDoesNotThrow(writer::flush);

        writer.dispose();
    }

    @Test
    @DisplayName("BufferedWriterUtil dispose flushes and shuts down")
    void disposeWorks() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(
            writeFn, 10000, 100, Long.MAX_VALUE, false
        );

        writer.write("test");
        writer.dispose();

        assertEquals(1, outputs.size());
        assertEquals("test", outputs.get(0));
    }

    @Test
    @DisplayName("BufferedWriterUtil write after dispose does nothing")
    void writeAfterDispose() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(writeFn);

        writer.dispose();
        writer.write("test");

        assertTrue(outputs.isEmpty());
    }

    @Test
    @DisplayName("BufferedWriterUtil flushes when buffer size exceeds max")
    void flushOnBufferSizeExceeded() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(
            writeFn, 10000, 2, Long.MAX_VALUE, false
        );

        writer.write("test1");
        writer.write("test2");
        writer.write("test3");  // Should trigger flush

        // Give time for async flush
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        assertFalse(outputs.isEmpty());

        writer.dispose();
    }

    @Test
    @DisplayName("BufferedWriterUtil createBufferedWriter with defaults")
    void createBufferedWriterDefaults() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriter writer = BufferedWriterUtil.createBufferedWriter(writeFn);

        assertNotNull(writer);
        writer.write("test");
        writer.flush();

        assertEquals(1, outputs.size());

        writer.dispose();
    }

    @Test
    @DisplayName("BufferedWriterUtil BufferedWriterI interface methods work")
    void bufferedWriterInterface() {
        List<String> outputs = new ArrayList<>();
        Consumer<String> writeFn = outputs::add;

        BufferedWriterUtil.BufferedWriterI writer = BufferedWriterUtil.createBufferedWriter(writeFn);

        writer.write("test");
        writer.flush();
        writer.dispose();

        assertEquals("test", outputs.get(0));
    }
}