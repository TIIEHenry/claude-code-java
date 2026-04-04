/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ActivityManager.
 */
class ActivityManagerTest {

    private ActivityManager manager;

    @BeforeEach
    void setUp() {
        manager = new ActivityManager();
    }

    @Test
    @DisplayName("ActivityManager startActivity creates activity")
    void startActivity() {
        ActivityManager.Activity activity = manager.startActivity("test-1", "Test Activity");
        assertNotNull(activity);
        assertEquals("test-1", activity.id());
        assertEquals("Test Activity", activity.description());
        assertEquals(ActivityManager.ActivityState.RUNNING, activity.state());
    }

    @Test
    @DisplayName("ActivityManager getActivity returns activity")
    void getActivity() {
        manager.startActivity("test-1", "Test");
        ActivityManager.Activity activity = manager.getActivity("test-1");
        assertNotNull(activity);
        assertEquals("test-1", activity.id());
    }

    @Test
    @DisplayName("ActivityManager getActivity returns null for unknown")
    void getActivityUnknown() {
        assertNull(manager.getActivity("unknown"));
    }

    @Test
    @DisplayName("ActivityManager completeActivity changes state")
    void completeActivity() {
        manager.startActivity("test-1", "Test");
        manager.completeActivity("test-1");
        
        ActivityManager.Activity activity = manager.getActivity("test-1");
        assertEquals(ActivityManager.ActivityState.COMPLETED, activity.state());
    }

    @Test
    @DisplayName("ActivityManager failActivity changes state")
    void failActivity() {
        manager.startActivity("test-1", "Test");
        manager.failActivity("test-1", "Error");
        
        ActivityManager.Activity activity = manager.getActivity("test-1");
        assertEquals(ActivityManager.ActivityState.FAILED, activity.state());
    }

    @Test
    @DisplayName("ActivityManager cancelActivity changes state")
    void cancelActivity() {
        manager.startActivity("test-1", "Test");
        manager.cancelActivity("test-1");
        
        ActivityManager.Activity activity = manager.getActivity("test-1");
        assertEquals(ActivityManager.ActivityState.CANCELLED, activity.state());
    }

    @Test
    @DisplayName("ActivityManager getAllActivities returns all")
    void getAllActivities() {
        manager.startActivity("test-1", "Test 1");
        manager.startActivity("test-2", "Test 2");
        
        Collection<ActivityManager.Activity> activities = manager.getAllActivities();
        assertEquals(2, activities.size());
    }

    @Test
    @DisplayName("ActivityManager getRunningActivities filters")
    void getRunningActivities() {
        manager.startActivity("test-1", "Test 1");
        manager.startActivity("test-2", "Test 2");
        manager.completeActivity("test-2");
        
        List<ActivityManager.Activity> running = manager.getRunningActivities();
        assertEquals(1, running.size());
        assertEquals("test-1", running.get(0).id());
    }

    @Test
    @DisplayName("ActivityManager hasRunningActivities true")
    void hasRunningActivitiesTrue() {
        manager.startActivity("test-1", "Test");
        assertTrue(manager.hasRunningActivities());
    }

    @Test
    @DisplayName("ActivityManager hasRunningActivities false")
    void hasRunningActivitiesFalse() {
        manager.startActivity("test-1", "Test");
        manager.completeActivity("test-1");
        assertFalse(manager.hasRunningActivities());
    }

    @Test
    @DisplayName("ActivityManager clearCompleted removes completed")
    void clearCompleted() {
        manager.startActivity("test-1", "Test 1");
        manager.startActivity("test-2", "Test 2");
        manager.completeActivity("test-1");
        manager.failActivity("test-2", "Error occurred");
        manager.startActivity("test-3", "Test 3");

        manager.clearCompleted();
        
        assertEquals(1, manager.getAllActivities().size());
        assertEquals("test-3", manager.getActivity("test-3").id());
    }

    @Test
    @DisplayName("ActivityManager listener receives events")
    void listenerReceivesEvents() {
        AtomicInteger eventCount = new AtomicInteger(0);
        manager.addListener((activity, event) -> eventCount.incrementAndGet());
        
        manager.startActivity("test-1", "Test");
        manager.completeActivity("test-1");
        
        assertEquals(2, eventCount.get());
    }

    @Test
    @DisplayName("ActivityManager removeListener stops events")
    void removeListener() {
        AtomicInteger eventCount = new AtomicInteger(0);
        ActivityManager.ActivityListener listener = (activity, event) -> eventCount.incrementAndGet();
        
        manager.addListener(listener);
        manager.startActivity("test-1", "Test");
        manager.removeListener(listener);
        manager.completeActivity("test-1");
        
        assertEquals(1, eventCount.get());
    }

    @Test
    @DisplayName("ActivityManager Activity getDuration")
    void activityGetDuration() throws InterruptedException {
        ActivityManager.Activity activity = manager.startActivity("test-1", "Test");
        Thread.sleep(50);
        assertTrue(activity.getDuration() >= 50);
    }

    @Test
    @DisplayName("ActivityManager getInstance returns singleton")
    void getInstance() {
        ActivityManager instance1 = ActivityManager.getInstance();
        ActivityManager instance2 = ActivityManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("ActivityManager lastInteractionTime")
    void lastInteractionTime() throws InterruptedException {
        long time1 = manager.getLastInteractionTime();
        Thread.sleep(10);
        manager.updateLastInteractionTime();
        long time2 = manager.getLastInteractionTime();
        assertTrue(time2 >= time1);
    }

    @Test
    @DisplayName("ActivityManager ActivityState enum values")
    void activityStateEnum() {
        ActivityManager.ActivityState[] states = ActivityManager.ActivityState.values();
        assertEquals(4, states.length);
    }

    @Test
    @DisplayName("ActivityManager ActivityEvent enum values")
    void activityEventEnum() {
        ActivityManager.ActivityEvent[] events = ActivityManager.ActivityEvent.values();
        assertEquals(4, events.length);
    }
}
