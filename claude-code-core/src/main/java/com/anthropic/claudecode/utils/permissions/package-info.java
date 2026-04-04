/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/*
 */
/**
 * Permission utilities for tool access control and path validation.
 *
 * This package provides:
 * - Dangerous pattern detection (DangerousPatterns)
 * - Bash command classification (BashClassifier)
 * - Path validation (PathValidation)
 * - Permission decision types (PermissionDecisionReason, PermissionRuleValue)
 *
 * Security considerations:
 * - All permission checks follow deny-by-default principle
 * - Dangerous patterns are blocked even with broad permissions
 * - Path traversal and shell expansion are carefully validated
 */
package com.anthropic.claudecode.utils.permissions;