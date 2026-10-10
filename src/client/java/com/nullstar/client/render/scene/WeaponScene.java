// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.CosmeticParticles;
import com.nullstar.client.render.StrikeView;
import net.minecraft.client.multiplayer.ClientLevel;

/** Per-weapon presentation. Implementations derive everything from the read-only view; they own no gameplay state. */
public interface WeaponScene {
    /** Extracts this frame's geometry with the canvas origin at the strike target. */
    void render(StrikeView view,Canvas canvas,ClientConfig.Quality quality);
    /** Presentation tick (20 Hz): spawns bounded cosmetic particles. */
    default void tick(StrikeView view,CosmeticParticles particles,ClientLevel level,ClientConfig.Quality quality) {}
    /** Called once when an authoritative cue for this strike arrives. */
    default void event(StrikeView view,StrikeView.Event event,CosmeticParticles particles,ClientLevel level,ClientConfig.Quality quality) {}
}
