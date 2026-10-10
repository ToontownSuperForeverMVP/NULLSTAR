// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.integration;

import com.nullstar.client.NullstarSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional client integration; only Mod Menu loads this entrypoint. */
public final class NullstarModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<NullstarSettingsScreen> getModConfigScreenFactory() {
        return NullstarSettingsScreen::new;
    }
}
