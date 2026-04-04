/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/computerUse/executor
 */
package com.anthropic.claudecode.utils.computerUse;

import java.awt.Robot;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/**
 * Computer use executor - Execute computer actions.
 */
public final class ComputerUseExecutor {
    private final ScheduledExecutorService scheduler;
    private final Robot robot;
    private volatile boolean isExecuting = false;
    private final List<ActionLog> actionLog = new ArrayList<>();

    /**
     * Create executor.
     */
    public ComputerUseExecutor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(50);
            this.robot.setAutoWaitForIdle(true);
        } catch (AWTException e) {
            throw new RuntimeException("Failed to create Robot: " + e.getMessage(), e);
        }
    }

    /**
     * Execute action.
     */
    public CompletableFuture<ExecutionResult> execute(
        ComputerUseCommon.ComputerAction action
    ) {
        return CompletableFuture.supplyAsync(() -> {
            ComputerUseGates.GateResult gate = ComputerUseGates.checkGate(action);
            if (!gate.allowed()) {
                return new ExecutionResult(
                    false,
                    gate.reason(),
                    action,
                    System.currentTimeMillis(),
                    0
                );
            }

            isExecuting = true;
            long startTime = System.currentTimeMillis();

            try {
                ExecutionResult result = executeInternal(action);
                long endTime = System.currentTimeMillis();

                actionLog.add(new ActionLog(
                    action,
                    result,
                    startTime,
                    endTime
                ));

                isExecuting = false;
                return new ExecutionResult(
                    true,
                    "Action executed successfully",
                    action,
                    startTime,
                    endTime - startTime
                );
            } catch (Exception e) {
                isExecuting = false;
                return new ExecutionResult(
                    false,
                    e.getMessage(),
                    action,
                    startTime,
                    0
                );
            }
        });
    }

    /**
     * Internal execution - uses Java AWT Robot for automation.
     */
    private ExecutionResult executeInternal(ComputerUseCommon.ComputerAction action) {
        switch (action.action()) {
            case KEY -> simulateKeyPress(action.keys());
            case TYPE -> simulateTypeText(action.text());
            case MOUSE_MOVE -> simulateMouseMove(action.coordinate());
            case LEFT_CLICK -> simulateClick(action.coordinate(), "left");
            case RIGHT_CLICK -> simulateClick(action.coordinate(), "right");
            case DOUBLE_CLICK -> simulateDoubleClick(action.coordinate());
            case SCROLL -> simulateScroll(action.scrollDirection(), action.scrollAmount());
            case SCREENSHOT -> captureScreenshot();
            default -> throw new UnsupportedOperationException(
                "Unknown action: " + action.action()
            );
        }

        return new ExecutionResult(
            true,
            "Executed: " + action.action(),
            action,
            System.currentTimeMillis(),
            0
        );
    }

    /**
     * Simulate key press.
     */
    private void simulateKeyPress(List<String> keys) {
        for (String key : keys) {
            Integer keyCode = getKeyCode(key);
            if (keyCode != null) {
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
            }
        }
    }

    /**
     * Simulate type text.
     */
    private void simulateTypeText(String text) {
        if (text == null) return;
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ') {
                if (c == ' ') {
                    robot.keyPress(KeyEvent.VK_SPACE);
                    robot.keyRelease(KeyEvent.VK_SPACE);
                } else {
                    boolean upper = Character.isUpperCase(c);
                    int keyCode = KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(c));
                    if (upper) robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                    if (upper) robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
        }
    }

    /**
     * Simulate mouse move.
     */
    private void simulateMouseMove(ComputerUseCommon.Coordinate coord) {
        if (coord != null) {
            robot.mouseMove(coord.x(), coord.y());
        }
    }

    /**
     * Simulate click.
     */
    private void simulateClick(ComputerUseCommon.Coordinate coord, String button) {
        if (coord != null) {
            robot.mouseMove(coord.x(), coord.y());
        }
        int buttonMask = "right".equals(button) ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
    }

    /**
     * Simulate double click.
     */
    private void simulateDoubleClick(ComputerUseCommon.Coordinate coord) {
        if (coord != null) {
            robot.mouseMove(coord.x(), coord.y());
        }
        int buttonMask = InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
    }

    /**
     * Simulate scroll.
     */
    private void simulateScroll(int direction, int amount) {
        int notches = direction < 0 ? -amount : amount;
        robot.mouseWheel(notches);
    }

    /**
     * Capture screenshot.
     */
    private void captureScreenshot() {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(screenSize);
            BufferedImage image = robot.createScreenCapture(screenRect);
            ImageIO.write(image, "PNG", new File("screenshot_" + System.currentTimeMillis() + ".png"));
        } catch (Exception e) {
            // Log error but don't fail
        }
    }

    /**
     * Get key code for key name.
     */
    private Integer getKeyCode(String key) {
        return switch (key.toUpperCase()) {
            case "ENTER" -> KeyEvent.VK_ENTER;
            case "TAB" -> KeyEvent.VK_TAB;
            case "ESCAPE", "ESC" -> KeyEvent.VK_ESCAPE;
            case "SPACE" -> KeyEvent.VK_SPACE;
            case "BACKSPACE" -> KeyEvent.VK_BACK_SPACE;
            case "DELETE" -> KeyEvent.VK_DELETE;
            case "ARROWUP", "UP" -> KeyEvent.VK_UP;
            case "ARROWDOWN", "DOWN" -> KeyEvent.VK_DOWN;
            case "ARROWLEFT", "LEFT" -> KeyEvent.VK_LEFT;
            case "ARROWRIGHT", "RIGHT" -> KeyEvent.VK_RIGHT;
            case "SHIFT" -> KeyEvent.VK_SHIFT;
            case "CONTROL", "CTRL" -> KeyEvent.VK_CONTROL;
            case "ALT" -> KeyEvent.VK_ALT;
            case "META", "CMD", "COMMAND" -> KeyEvent.VK_META;
            default -> {
                if (key.length() == 1) {
                    yield KeyEvent.getExtendedKeyCodeForChar(key.charAt(0));
                }
                yield null;
            }
        };
    }

    /**
     * Check if executing.
     */
    public boolean isExecuting() {
        return isExecuting;
    }

    /**
     * Get action log.
     */
    public List<ActionLog> getActionLog() {
        return Collections.unmodifiableList(actionLog);
    }

    /**
     * Shutdown executor.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Execution result record.
     */
    public record ExecutionResult(
        boolean success,
        String message,
        ComputerUseCommon.ComputerAction action,
        long timestamp,
        long durationMs
    ) {}

    /**
     * Action log record.
     */
    public record ActionLog(
        ComputerUseCommon.ComputerAction action,
        ExecutionResult result,
        long startTime,
        long endTime
    ) {
        public long durationMs() {
            return endTime - startTime;
        }
    }
}