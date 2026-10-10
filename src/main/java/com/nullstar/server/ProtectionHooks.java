// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Claim integrations register a veto. Called before every mutation and entity interaction. */
public final class ProtectionHooks {
    @FunctionalInterface public interface Protection { boolean allows(ServerLevel level, UUID owner, BlockPos position); }
    private static final CopyOnWriteArrayList<Protection> HOOKS=new CopyOnWriteArrayList<>();
    private ProtectionHooks() {}
    public static AutoCloseable register(Protection protection) { HOOKS.add(protection);return ()->HOOKS.remove(protection); }
    public static boolean allows(ServerLevel level, UUID owner, BlockPos pos) {
        for (var protection:HOOKS) {
            try { if (!protection.allows(level,owner,pos)) return false; }
            catch (RuntimeException exception) { com.nullstar.Nullstar.LOGGER.error("Protection hook failed; denied operation",exception);return false; }
        }
        return true;
    }
}
