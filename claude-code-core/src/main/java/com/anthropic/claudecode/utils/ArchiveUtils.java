/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code archive utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
// Note: TAR support requires external library like Apache Commons Compress

/**
 * Archive compression and extraction utilities.
 */
public final class ArchiveUtils {
    private ArchiveUtils() {}

    /**
     * Archive type enumeration.
     */
    public enum ArchiveType {
        ZIP,
        TAR,
        TAR_GZ,
        TAR_BZ2,
        GZIP,
        UNKNOWN
    }

    /**
     * Detect archive type from file extension.
     */
    public static ArchiveType detectArchiveType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".zip")) return ArchiveType.ZIP;
        if (name.endsWith(".tar")) return ArchiveType.TAR;
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) return ArchiveType.TAR_GZ;
        if (name.endsWith(".tar.bz2") || name.endsWith(".tbz2")) return ArchiveType.TAR_BZ2;
        if (name.endsWith(".gz")) return ArchiveType.GZIP;
        return ArchiveType.UNKNOWN;
    }

    /**
     * Extract a ZIP file to a directory.
     */
    public static void extractZip(Path zipFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName());

                // Security check - prevent zip slip
                if (!targetPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Potential zip slip attack detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Create a ZIP file from a directory.
     */
    public static void createZip(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Path normalizedSource = sourceDir.normalize();
            Files.walk(normalizedSource)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String relativePath = normalizedSource.relativize(path).toString();
                        ZipEntry entry = new ZipEntry(relativePath);
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    /**
     * Create a ZIP file from specific files.
     */
    public static void createZipFromFiles(List<Path> files, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path file : files) {
                if (Files.exists(file) && !Files.isDirectory(file)) {
                    ZipEntry entry = new ZipEntry(file.getFileName().toString());
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * List contents of a ZIP file.
     */
    public static List<String> listZipContents(Path zipFile) throws IOException {
        List<String> contents = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                contents.add(entry.getName());
                zis.closeEntry();
            }
        }
        return contents;
    }

    /**
     * Extract a TAR file to a directory.
     */
    public static void extractTar(Path tarFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (InputStream is = Files.newInputStream(tarFile);
             TarInputStream tis = new TarInputStream(is)) {
            TarEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName());

                // Security check
                if (!targetPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Potential tar slip attack detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(tis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Extract a TAR.GZ file to a directory.
     */
    public static void extractTarGz(Path tarGzFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(tarGzFile));
             TarInputStream tis = new TarInputStream(gzis)) {
            TarEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName());

                if (!targetPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Potential tar slip attack detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(tis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Create a TAR.GZ file from a directory.
     */
    public static void createTarGz(Path sourceDir, Path tarGzFile) throws IOException {
        try (GZIPOutputStream gzos = new GZIPOutputStream(Files.newOutputStream(tarGzFile));
             TarOutputStream tos = new TarOutputStream(gzos)) {
            Path normalizedSource = sourceDir.normalize();
            Files.walk(normalizedSource)
                .forEach(path -> {
                    try {
                        String relativePath = normalizedSource.relativize(path).toString();
                        TarEntry entry = new TarEntry(relativePath);

                        if (Files.isDirectory(path)) {
                            entry.setDirectory(true);
                            tos.putNextEntry(entry);
                            tos.closeEntry();
                        } else {
                            entry.setSize(Files.size(path));
                            tos.putNextEntry(entry);
                            Files.copy(path, tos);
                            tos.closeEntry();
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    /**
     * Compress a single file with GZIP.
     */
    public static Path gzipFile(Path sourceFile) throws IOException {
        Path gzipFile = Paths.get(sourceFile.toString() + ".gz");
        try (GZIPOutputStream gzos = new GZIPOutputStream(Files.newOutputStream(gzipFile))) {
            Files.copy(sourceFile, gzos);
        }
        return gzipFile;
    }

    /**
     * Decompress a GZIP file.
     */
    public static Path gunzipFile(Path gzipFile) throws IOException {
        String sourceName = gzipFile.getFileName().toString();
        if (sourceName.endsWith(".gz")) {
            sourceName = sourceName.substring(0, sourceName.length() - 3);
        }
        Path targetFile = gzipFile.getParent().resolve(sourceName);

        try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(gzipFile))) {
            Files.copy(gzis, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetFile;
    }

    /**
     * Extract an archive based on its detected type.
     */
    public static void extractArchive(Path archiveFile, Path targetDir) throws IOException {
        ArchiveType type = detectArchiveType(archiveFile);
        switch (type) {
            case ZIP:
                extractZip(archiveFile, targetDir);
                break;
            case TAR:
                extractTar(archiveFile, targetDir);
                break;
            case TAR_GZ:
                extractTarGz(archiveFile, targetDir);
                break;
            case GZIP:
                gunzipFile(archiveFile);
                break;
            default:
                throw new IOException("Unknown archive type: " + archiveFile);
        }
    }

    /**
     * Check if a file is a valid ZIP archive.
     */
    public static boolean isValidZip(Path file) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry = zis.getNextEntry();
            return entry != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get uncompressed size estimate for an archive.
     */
    public static long getUncompressedSize(Path archiveFile) throws IOException {
        ArchiveType type = detectArchiveType(archiveFile);
        long totalSize = 0;

        if (type == ArchiveType.ZIP) {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archiveFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        totalSize += entry.getSize();
                    }
                    zis.closeEntry();
                }
            }
        }

        return totalSize;
    }

    /**
     * Add a file to an existing ZIP archive.
     */
    public static void addToZip(Path zipFile, Path fileToAdd, String entryName) throws IOException {
        // Need to recreate the ZIP to add a new entry
        Path tempZip = Paths.get(zipFile.toString() + ".tmp");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
            // Copy existing entries
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    ZipEntry newEntry = new ZipEntry(entry.getName());
                    zos.putNextEntry(newEntry);
                    zis.transferTo(zos);
                    zos.closeEntry();
                    zis.closeEntry();
                }
            }

            // Add new entry
            ZipEntry newEntry = new ZipEntry(entryName);
            zos.putNextEntry(newEntry);
            Files.copy(fileToAdd, zos);
            zos.closeEntry();
        }

        Files.move(tempZip, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Simple TarInputStream implementation.
     */
    private static class TarInputStream extends FilterInputStream {
        private TarEntry currentEntry;

        TarInputStream(InputStream in) {
            super(in);
        }

        TarEntry getNextEntry() throws IOException {
            byte[] header = new byte[512];
            int bytesRead = read(header);
            if (bytesRead != 512) {
                return null;
            }

            // Check for empty block (end of archive)
            boolean empty = true;
            for (byte b : header) {
                if (b != 0) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return null;
            }

            String name = new String(header, 0, 100).trim();
            long size = parseOctal(header, 124, 12);
            boolean isDir = header[156] == '5';

            currentEntry = new TarEntry(name, size, isDir);
            return currentEntry;
        }

        private long parseOctal(byte[] header, int offset, int length) {
            String str = new String(header, offset, length).trim();
            if (str.isEmpty()) return 0;
            return Long.parseLong(str, 8);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (currentEntry != null && currentEntry.bytesRead >= currentEntry.size) {
                return -1;
            }
            int result = super.read(b, off, len);
            if (result > 0 && currentEntry != null) {
                currentEntry.bytesRead += result;
            }
            return result;
        }
    }

    /**
     * Simple TarOutputStream implementation.
     */
    private static class TarOutputStream extends FilterOutputStream {
        TarOutputStream(OutputStream out) {
            super(out);
        }

        void putNextEntry(TarEntry entry) throws IOException {
            byte[] header = new byte[512];

            // Name
            byte[] nameBytes = entry.name.getBytes();
            System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));

            // Mode
            String mode = String.format("%07o", 0644);
            System.arraycopy(mode.getBytes(), 0, header, 100, 7);

            // Size
            String size = String.format("%011o", entry.size);
            System.arraycopy(size.getBytes(), 0, header, 124, 11);

            // Type flag
            header[156] = (byte) (entry.isDirectory ? '5' : '0');

            // Write header
            out.write(header);
        }

        void closeEntry() throws IOException {
            // Pad to 512-byte block boundary
            out.write(new byte[512]);
        }
    }

    /**
     * Tar entry representation.
     */
    private static class TarEntry {
        final String name;
        final long size;
        final boolean isDirectory;
        long bytesRead = 0;

        TarEntry(String name, long size, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.isDirectory = isDirectory;
        }

        TarEntry(String name) {
            this(name, 0, false);
        }

        void setDirectory(boolean isDir) {
            // Used during creation
        }

        void setSize(long size) {
            // Used during creation
        }

        boolean isDirectory() {
            return isDirectory;
        }

        String getName() {
            return name;
        }
    }
}