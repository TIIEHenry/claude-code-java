/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.AfterEach;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BufferedWriterUtils.
 */
class BufferedWriterUtilsTest {

    @TempDir
    Path tempDir;

    private BufferedWriterUtils writer;

    @AfterEach
    void tearDown() {
        if (writer != null) {
            writer.close();
        }
    }

    @Test
    @DisplayName("BufferedWriterUtils create default")
    void createDefault() throws IOException {
        Path file = tempDir.resolve("test.txt");
        writer = BufferedWriterUtils.create(file);

        assertNotNull(writer);
        assertEquals(file, writer.getFilePath());
        assertFalse(writer.isClosed());
        assertEquals(0, writer.getBufferCount());
    }

    @Test
    @DisplayName("BufferedWriterUtils create custom settings")
    void createCustomSettings() throws IOException {
        Path file = tempDir.resolve("test2.txt");
        writer = BufferedWriterUtils.create(file, 10, 1000);

        assertNotNull(writer);
        assertEquals(file, writer.getFilePath());
        assertFalse(writer.isClosed());
    }

    @Test
    @DisplayName("BufferedWriterUtils writeLine")
    void writeLine() throws IOException {
        Path file = tempDir.resolve("test3.txt");
        writer = BufferedWriterUtils.create(file, 100, 5000);

        writer.writeLine("Hello, World!");
        writer.flush();

        String content = Files.readString(file);
        assertTrue(content.contains("Hello, World!"));
        assertTrue(writer.getBufferCount() == 0); // reset after flush
    }

    @Test
    @DisplayName("BufferedWriterUtils writeLine multiple")
    void writeLineMultiple() throws IOException {
        Path file = tempDir.resolve("test4.txt");
        writer = BufferedWriterUtils.create(file, 100, 5000);

        writer.writeLine("Line 1");
        writer.writeLine("Line 2");
        writer.writeLine("Line 3");
        writer.flush();

        String content = Files.readString(file);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    @DisplayName("BufferedWriterUtils write")
    void write() throws IOException {
        Path file = tempDir.resolve("test5.txt");
        writer = BufferedWriterUtils.create(file);

        writer.write("No newline here");
        writer.flush();

        String content = Files.readString(file);
        assertEquals("No newline here", content);
    }

    @Test
    @DisplayName("BufferedWriterUtils writeLine triggers flush")
    void writeLineTriggersFlush() throws IOException {
        Path file = tempDir.resolve("test6.txt");
        writer = BufferedWriterUtils.create(file, 2, 5000);

        writer.writeLine("Line 1");
        assertEquals(1, writer.getBufferCount());

        writer.writeLine("Line 2");
        // After hitting bufferSize (2), flush is called, bufferCount resets
        assertEquals(0, writer.getBufferCount());
    }

    @Test
    @DisplayName("BufferedWriterUtils flush")
    void flush() throws IOException {
        Path file = tempDir.resolve("test7.txt");
        writer = BufferedWriterUtils.create(file);

        writer.writeLine("Test content");
        writer.flush();

        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).length() > 0);
    }

    @Test
    @DisplayName("BufferedWriterUtils getFilePath")
    void getFilePath() throws IOException {
        Path file = tempDir.resolve("test8.txt");
        writer = BufferedWriterUtils.create(file);

        assertEquals(file, writer.getFilePath());
    }

    @Test
    @DisplayName("BufferedWriterUtils getBufferCount")
    void getBufferCount() throws IOException {
        Path file = tempDir.resolve("test9.txt");
        writer = BufferedWriterUtils.create(file, 100, 5000);

        assertEquals(0, writer.getBufferCount());
        writer.writeLine("Line");
        assertEquals(1, writer.getBufferCount());
        writer.writeLine("Line");
        assertEquals(2, writer.getBufferCount());
    }

    @Test
    @DisplayName("BufferedWriterUtils isClosed")
    void isClosed() throws IOException {
        Path file = tempDir.resolve("test10.txt");
        writer = BufferedWriterUtils.create(file);

        assertFalse(writer.isClosed());
        writer.close();
        assertTrue(writer.isClosed());
    }

    @Test
    @DisplayName("BufferedWriterUtils close")
    void close() throws IOException {
        Path file = tempDir.resolve("test11.txt");
        writer = BufferedWriterUtils.create(file);

        writer.writeLine("Content before close");
        writer.close();

        assertTrue(writer.isClosed());
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("BufferedWriterUtils close twice")
    void closeTwice() throws IOException {
        Path file = tempDir.resolve("test12.txt");
        writer = BufferedWriterUtils.create(file);

        writer.close();
        writer.close(); // Should not throw

        assertTrue(writer.isClosed());
    }

    @Test
    @DisplayName("BufferedWriterUtils writeLine after close throws")
    void writeLineAfterClose() throws IOException {
        Path file = tempDir.resolve("test13.txt");
        writer = BufferedWriterUtils.create(file);
        writer.close();

        assertThrows(IOException.class, () -> writer.writeLine("Should fail"));
    }

    @Test
    @DisplayName("BufferedWriterUtils write after close throws")
    void writeAfterClose() throws IOException {
        Path file = tempDir.resolve("test14.txt");
        writer = BufferedWriterUtils.create(file);
        writer.close();

        assertThrows(IOException.class, () -> writer.write("Should fail"));
    }

    @Test
    @DisplayName("BufferedWriterUtils flush after close")
    void flushAfterClose() throws IOException {
        Path file = tempDir.resolve("test15.txt");
        writer = BufferedWriterUtils.create(file);
        writer.close();

        // Flush after close should not throw (just does nothing)
        writer.flush();
    }

    @Test
    @DisplayName("BufferedWriterUtils creates parent directories")
    void createsParentDirectories() throws IOException {
        Path nestedDir = tempDir.resolve("nested").resolve("dir");
        Path file = nestedDir.resolve("test.txt");

        assertFalse(Files.exists(nestedDir));

        writer = BufferedWriterUtils.create(file);

        assertTrue(Files.exists(nestedDir));
    }

    @Test
    @DisplayName("BufferedWriterUtils append mode")
    void appendMode() throws IOException {
        Path file = tempDir.resolve("test16.txt");

        // Write first content
        writer = BufferedWriterUtils.create(file);
        writer.writeLine("First line");
        writer.close();

        // Append second content
        BufferedWriterUtils writer2 = BufferedWriterUtils.create(file);
        writer2.writeLine("Second line");
        writer2.close();

        String content = Files.readString(file);
        assertTrue(content.contains("First line"));
        assertTrue(content.contains("Second line"));
    }
}