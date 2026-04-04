/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/*
 */
/**
 * Bash command parsing and analysis utilities.
 *
 * This package provides:
 * - Shell quoting utilities (ShellQuoteUtils, ShellQuotingUtils)
 * - Heredoc extraction and restoration (HeredocUtils)
 * - Command parsing (ParsedCommand, IParsedCommand, RegexParsedCommand)
 * - Command analysis (CommandUtils, TreeSitterAnalysis)
 * - Output redirection handling (OutputRedirection)
 *
 * Security considerations:
 * - All utilities are designed to handle shell command injection patterns
 * - Heredoc extraction prevents shell-quote library misinterpretation
 * - Quote-aware parsing correctly handles complex quoting scenarios
 */
package com.anthropic.claudecode.utils.bash;