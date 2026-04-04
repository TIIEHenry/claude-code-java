/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/cyberRiskInstruction.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Cyber risk instruction for security-related requests.
 *
 * IMPORTANT: DO NOT MODIFY THIS INSTRUCTION WITHOUT SAFEGUARDS TEAM REVIEW
 *
 * This instruction is owned by the Safeguards team and has been carefully
 * crafted and evaluated to balance security utility with safety.
 */
public final class CyberRiskInstruction {
    private CyberRiskInstruction() {}

    public static final String CYBER_RISK_INSTRUCTION =
        "IMPORTANT: Assist with authorized security testing, defensive security, " +
        "CTF challenges, and educational contexts. Refuse requests for destructive " +
        "techniques, DoS attacks, mass targeting, supply chain compromise, or " +
        "detection evasion for malicious purposes. Dual-use security tools " +
        "(C2 frameworks, credential testing, exploit development) require clear " +
        "authorization context: pentesting engagements, CTF competitions, " +
        "security research, or defensive use cases.";
}