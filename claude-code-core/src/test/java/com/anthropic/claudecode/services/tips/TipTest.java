/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.tips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Tip.
 */
class TipTest {

    @Test
    @DisplayName("Tip record fields")
    void tipRecord() {
        Tip.TipContentProvider provider = ctx -> "Test content";
        Tip.TipRelevanceChecker checker = ctx -> true;

        Tip tip = new Tip("test-id", provider, 5, checker);

        assertEquals("test-id", tip.id());
        assertEquals(provider, tip.contentProvider());
        assertEquals(5, tip.cooldownSessions());
        assertEquals(checker, tip.isRelevant());
    }

    @Test
    @DisplayName("Tip of static content")
    void tipOfStatic() {
        Tip tip = Tip.of("static-tip", "Static content here", 3);

        assertEquals("static-tip", tip.id());
        assertEquals("Static content here", tip.getContent(new TipContext()));
        assertEquals(3, tip.cooldownSessions());
        assertTrue(tip.checkRelevance(new TipContext()));
    }

    @Test
    @DisplayName("Tip of dynamic content")
    void tipOfDynamic() {
        Tip.TipContentProvider provider = ctx -> "Content for theme: " + ctx.theme();
        Tip.TipRelevanceChecker checker = ctx -> ctx.bashTools() != null && ctx.bashTools().contains("Bash");

        Tip tip = Tip.of("dynamic-tip", provider, 2, checker);

        TipContext context = new TipContext("dark", Set.of("Bash", "Read"), null);
        assertEquals("Content for theme: dark", tip.getContent(context));
        assertTrue(tip.checkRelevance(context));

        TipContext noBash = new TipContext("light", Set.of("Read"), null);
        assertFalse(tip.checkRelevance(noBash));
    }

    @Test
    @DisplayName("Tip getContent uses contentProvider")
    void tipGetContent() {
        Tip.TipContentProvider provider = ctx -> {
            if (ctx.theme() != null) {
                return "Theme: " + ctx.theme();
            }
            return "No theme";
        };

        Tip tip = new Tip("tip-id", provider, 1, ctx -> true);

        assertEquals("Theme: dark", tip.getContent(new TipContext("dark", null, null)));
        assertEquals("No theme", tip.getContent(new TipContext(null, null, null)));
    }

    @Test
    @DisplayName("Tip checkRelevance uses relevance checker")
    void tipCheckRelevance() {
        Tip.TipRelevanceChecker alwaysRelevant = ctx -> true;
        Tip.TipRelevanceChecker neverRelevant = ctx -> false;
        Tip.TipRelevanceChecker dependsOnTheme = ctx -> "dark".equals(ctx.theme());

        Tip alwaysTip = new Tip("always", ctx -> "content", 1, alwaysRelevant);
        Tip neverTip = new Tip("never", ctx -> "content", 1, neverRelevant);
        Tip conditionalTip = new Tip("conditional", ctx -> "content", 1, dependsOnTheme);

        assertTrue(alwaysTip.checkRelevance(new TipContext()));
        assertFalse(neverTip.checkRelevance(new TipContext()));
        assertTrue(conditionalTip.checkRelevance(new TipContext("dark", null, null)));
        assertFalse(conditionalTip.checkRelevance(new TipContext("light", null, null)));
    }

    @Test
    @DisplayName("Tip TipContentProvider functional interface")
    void tipContentProviderFunctional() {
        Tip.TipContentProvider provider = ctx -> "Hello from provider";
        TipContext context = new TipContext();

        assertEquals("Hello from provider", provider.getContent(context));
    }

    @Test
    @DisplayName("Tip TipRelevanceChecker functional interface")
    void tipRelevanceCheckerFunctional() {
        Tip.TipRelevanceChecker checker = ctx -> ctx.theme() != null;
        TipContext withTheme = new TipContext("dark", null, null);
        TipContext withoutTheme = new TipContext(null, null, null);

        assertTrue(checker.isRelevant(withTheme));
        assertFalse(checker.isRelevant(withoutTheme));
    }
}