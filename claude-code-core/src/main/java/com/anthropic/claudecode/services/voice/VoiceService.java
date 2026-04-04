/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/voice.ts
 */
package com.anthropic.claudecode.services.voice;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Voice service: audio recording for push-to-talk voice input.
 *
 * Recording uses native audio capture on macOS, Linux, and Windows.
 * Falls back to SoX `rec` or arecord (ALSA) on Linux if native unavailable.
 */
public final class VoiceService {
    private VoiceService() {}

    // Constants
    private static final int RECORDING_SAMPLE_RATE = 16000;
    private static final int RECORDING_CHANNELS = 1;
    private static final String SILENCE_DURATION_SECS = "2.0";
    private static final String SILENCE_THRESHOLD = "3%";

    // State
    private static volatile Process activeRecorder = null;
    private static volatile boolean nativeRecordingActive = false;
    private static volatile boolean initialized = false;

    // Executor for async operations
    private static final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    // Cached availability
    private static volatile Boolean nativeAudioAvailable = null;
    private static volatile CompletableFuture<Boolean> arecordProbe = null;

    /**
     * Check voice dependencies.
     */
    public static CompletableFuture<VoiceDependencies> checkVoiceDependencies() {
        return CompletableFuture.supplyAsync(() -> {
            // Check native audio
            if (isNativeAudioAvailable()) {
                return new VoiceDependencies(true, Collections.emptyList(), null);
            }

            // Windows requires native module
            if (isWindows()) {
                return new VoiceDependencies(
                    false,
                    Collections.singletonList("Voice mode requires the native audio module (not loaded)"),
                    null
                );
            }

            // On Linux, arecord is a valid fallback
            if (isLinux() && hasCommand("arecord")) {
                return new VoiceDependencies(true, Collections.emptyList(), null);
            }

            List<String> missing = new ArrayList<>();
            if (!hasCommand("rec")) {
                missing.add("sox (rec command)");
            }

            String installCommand = missing.isEmpty() ? null : detectPackageManagerCommand();
            return new VoiceDependencies(missing.isEmpty(), missing, installCommand);
        });
    }

    /**
     * Check recording availability.
     */
    public static CompletableFuture<RecordingAvailability> checkRecordingAvailability() {
        return CompletableFuture.supplyAsync(() -> {
            // Remote environments have no local microphone
            String remote = System.getenv("CLAUDE_CODE_REMOTE");
            if ("true".equalsIgnoreCase(remote)) {
                return new RecordingAvailability(
                    false,
                    "Voice mode requires microphone access, but no audio device is available in this environment.\n\nTo use voice mode, run Claude Code locally instead."
                );
            }

            // Native audio available
            if (isNativeAudioAvailable()) {
                return new RecordingAvailability(true, null);
            }

            // Windows requires native
            if (isWindows()) {
                return new RecordingAvailability(
                    false,
                    "Voice recording requires the native audio module, which could not be loaded."
                );
            }

            // WSL without audio
            String wslReason = "Voice mode could not access an audio device in WSL.\n\n" +
                "WSL2 with WSLg (Windows 11) provides audio via PulseAudio — " +
                "if you are on Windows 10 or WSL1, run Claude Code in native Windows instead.";

            // On Linux, probe arecord
            if (isLinux() && hasCommand("arecord")) {
                boolean probeOk = probeArecordSync();
                if (probeOk) {
                    return new RecordingAvailability(true, null);
                }
                // Could add WSL detection here
            }

            // Check for SoX
            if (!hasCommand("rec")) {
                String pm = detectPackageManagerCommand();
                String reason = pm != null
                    ? "Voice mode requires SoX for audio recording. Install it with: " + pm
                    : "Voice mode requires SoX for audio recording. Install SoX manually:\n" +
                      "  macOS: brew install sox\n" +
                      "  Ubuntu/Debian: sudo apt-get install sox\n" +
                      "  Fedora: sudo dnf install sox";
                return new RecordingAvailability(false, reason);
            }

            return new RecordingAvailability(true, null);
        });
    }

    /**
     * Request microphone permission.
     */
    public static CompletableFuture<Boolean> requestMicrophonePermission() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isNativeAudioAvailable()) {
                return true; // non-native platforms skip this check
            }

            // Try to probe Java Sound API for microphone availability
            try {
                javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(
                    RECORDING_SAMPLE_RATE,
                    16,
                    RECORDING_CHANNELS,
                    true,
                    false
                );

                javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
                    javax.sound.sampled.TargetDataLine.class,
                    format
                );

                if (!javax.sound.sampled.AudioSystem.isLineSupported(info)) {
                    return false;
                }

                // Try to open the line briefly to test access
                javax.sound.sampled.TargetDataLine line = (javax.sound.sampled.TargetDataLine)
                    javax.sound.sampled.AudioSystem.getLine(info);
                line.open(format, 1024);
                line.close();

                return true;
            } catch (Exception e) {
                // Line not available or permission denied
                return false;
            }
        });
    }

    /**
     * Start recording.
     */
    public static CompletableFuture<Boolean> startRecording(
        Consumer<byte[]> onData,
        Runnable onEnd,
        RecordingOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Try Java Sound API first (native Java recording)
            if (tryJavaSoundRecording(onData, onEnd, options)) {
                return true;
            }

            // Windows has no supported fallback beyond Java Sound
            if (isWindows()) {
                return false;
            }

            // On Linux, try arecord before SoX
            if (isLinux() && hasCommand("arecord") && probeArecordSync()) {
                return startArecordRecording(onData, onEnd);
            }

            // Fallback: SoX rec
            return startSoxRecording(onData, onEnd, options);
        });
    }

    /**
     * Try recording using Java Sound API.
     */
    private static boolean tryJavaSoundRecording(
        Consumer<byte[]> onData,
        Runnable onEnd,
        RecordingOptions options
    ) {
        try {
            javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(
                RECORDING_SAMPLE_RATE,
                16,
                RECORDING_CHANNELS,
                true,
                false
            );

            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
                javax.sound.sampled.TargetDataLine.class,
                format
            );

            if (!javax.sound.sampled.AudioSystem.isLineSupported(info)) {
                return false;
            }

            javax.sound.sampled.TargetDataLine line = (javax.sound.sampled.TargetDataLine)
                javax.sound.sampled.AudioSystem.getLine(info);
            line.open(format, 1024);
            line.start();

            nativeRecordingActive = true;
            activeRecorder = null; // Not using external process

            // Read audio data in background thread
            Thread readerThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    while (nativeRecordingActive && line.isOpen()) {
                        int bytesRead = line.read(buffer, 0, buffer.length);
                        if (bytesRead > 0 && nativeRecordingActive) {
                            byte[] chunk = new byte[bytesRead];
                            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                            onData.accept(chunk);
                        }
                    }
                } catch (Exception e) {
                    // Recording stopped or error
                } finally {
                    line.stop();
                    line.close();
                    nativeRecordingActive = false;
                    onEnd.run();
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            return true;
        } catch (Exception e) {
            nativeRecordingActive = false;
            return false;
        }
    }

    /**
     * Stop recording.
     */
    public static void stopRecording() {
        if (nativeRecordingActive) {
            // Would call native stop via JNI
            nativeRecordingActive = false;
            return;
        }

        if (activeRecorder != null) {
            activeRecorder.destroyForcibly();
            activeRecorder = null;
        }
    }

    /**
     * Shutdown the service.
     */
    public static void shutdown() {
        stopRecording();
        scheduler.shutdown();
    }

    // ─── Private helpers ────────────────────────────────────────────────

    private static boolean startSoxRecording(
        Consumer<byte[]> onData,
        Runnable onEnd,
        RecordingOptions options
    ) {
        boolean useSilenceDetection = options == null || options.silenceDetection;

        List<String> args = new ArrayList<>();
        args.add("-q"); // quiet
        args.add("--buffer");
        args.add("1024");
        args.add("-t");
        args.add("raw");
        args.add("-r");
        args.add(String.valueOf(RECORDING_SAMPLE_RATE));
        args.add("-e");
        args.add("signed");
        args.add("-b");
        args.add("16");
        args.add("-c");
        args.add(String.valueOf(RECORDING_CHANNELS));
        args.add("-"); // stdout

        // Add silence detection filter
        if (useSilenceDetection) {
            args.add("silence");
            args.add("1");
            args.add("0.1");
            args.add(SILENCE_THRESHOLD);
            args.add("1");
            args.add(SILENCE_DURATION_SECS);
            args.add(SILENCE_THRESHOLD);
        }

        try {
            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.add("rec");
            cmdArgs.addAll(args);
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            activeRecorder = process;

            // Read stdout in background
            Thread readerThread = new Thread(() -> {
                try (InputStream stdout = process.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = stdout.read(buffer)) != -1) {
                        if (bytesRead > 0) {
                            byte[] chunk = new byte[bytesRead];
                            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                            onData.accept(chunk);
                        }
                    }
                } catch (IOException e) {
                    // Stream closed
                } finally {
                    activeRecorder = null;
                    onEnd.run();
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Consume stderr to prevent backpressure
            Thread stderrThread = new Thread(() -> {
                try (InputStream stderr = process.getErrorStream()) {
                    while (stderr.read() != -1) {}
                } catch (IOException e) {
                    // Ignore
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            return true;
        } catch (IOException e) {
            activeRecorder = null;
            return false;
        }
    }

    private static boolean startArecordRecording(
        Consumer<byte[]> onData,
        Runnable onEnd
    ) {
        List<String> args = new ArrayList<>();
        args.add("-f");
        args.add("S16_LE"); // signed 16-bit little-endian
        args.add("-r");
        args.add(String.valueOf(RECORDING_SAMPLE_RATE));
        args.add("-c");
        args.add(String.valueOf(RECORDING_CHANNELS));
        args.add("-t");
        args.add("raw"); // raw PCM
        args.add("-q"); // quiet
        args.add("-"); // stdout

        try {
            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.add("arecord");
            cmdArgs.addAll(args);
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            activeRecorder = process;

            // Read stdout in background
            Thread readerThread = new Thread(() -> {
                try (InputStream stdout = process.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = stdout.read(buffer)) != -1) {
                        if (bytesRead > 0) {
                            byte[] chunk = new byte[bytesRead];
                            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                            onData.accept(chunk);
                        }
                    }
                } catch (IOException e) {
                    // Stream closed
                } finally {
                    activeRecorder = null;
                    onEnd.run();
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            return true;
        } catch (IOException e) {
            activeRecorder = null;
            return false;
        }
    }

    private static boolean hasCommand(String cmd) {
        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("where", cmd);
            } else {
                pb = new ProcessBuilder("which", cmd);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean probeArecordSync() {
        if (arecordProbe != null) {
            try {
                return arecordProbe.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                return false;
            }
        }

        arecordProbe = CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "arecord",
                    "-f", "S16_LE",
                    "-r", String.valueOf(RECORDING_SAMPLE_RATE),
                    "-c", String.valueOf(RECORDING_CHANNELS),
                    "-t", "raw",
                    "/dev/null"
                );
                pb.redirectErrorStream(false);
                Process process = pb.start();

                // Wait a short time - if still alive, device opened
                Thread.sleep(150);
                boolean alive = process.isAlive();
                process.destroyForcibly();

                return alive;
            } catch (Exception e) {
                return false;
            }
        });

        try {
            return arecordProbe.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isNativeAudioAvailable() {
        if (nativeAudioAvailable != null) {
            return nativeAudioAvailable;
        }

        // Check if Java Sound API can access microphone
        try {
            javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(
                RECORDING_SAMPLE_RATE,
                16,
                RECORDING_CHANNELS,
                true,
                false
            );

            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
                javax.sound.sampled.TargetDataLine.class,
                format
            );

            nativeAudioAvailable = javax.sound.sampled.AudioSystem.isLineSupported(info);
            return nativeAudioAvailable;
        } catch (Exception e) {
            nativeAudioAvailable = false;
            return false;
        }
    }

    private static String detectPackageManagerCommand() {
        if (isMacOS()) {
            if (hasCommand("brew")) {
                return "brew install sox";
            }
            return null;
        }

        if (isLinux()) {
            if (hasCommand("apt-get")) {
                return "sudo apt-get install sox";
            }
            if (hasCommand("dnf")) {
                return "sudo dnf install sox";
            }
            if (hasCommand("pacman")) {
                return "sudo pacman -S sox";
            }
        }

        return null;
    }

    private static boolean isMacOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux") || os.contains("nux");
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    // ─── Data classes ────────────────────────────────────────────────────

    public record VoiceDependencies(
        boolean available,
        List<String> missing,
        String installCommand
    ) {}

    public record RecordingAvailability(
        boolean available,
        String reason
    ) {}

    public record RecordingOptions(
        boolean silenceDetection
    ) {
        public RecordingOptions() {
            this(true);
        }
    }
}