// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

import com.nullstar.config.ConfigFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Part 3 regression: the flagship Helios timeline gives the impact and aftermath real screen time. */
class HeliosPacingTest {
    private WeaponCatalog weapons() throws Exception {return new WeaponCatalog(Path.of("build/test-overrides"));}
    private static int ticks(WeaponDefinition def,Phase phase){return def.phases().stream().filter(s->s.phase()==phase).mapToInt(WeaponDefinition.Stage::ticks).sum();}
    private static int start(WeaponDefinition def,Phase phase) {
        int t=0;for(var s:def.phases()){if(s.phase()==phase)return t;t+=s.ticks();}
        throw new AssertionError("missing "+phase);
    }
    private static ScaleProfile apocalypse(){return ScaleCatalog.defaults().stream().filter(p->p.weapon().equals("helios")&&p.tier()==ScaleTier.APOCALYPSE).findFirst().orElseThrow();}

    @Test void apocalypseHeliosRunsThreeMinutesWithALongAftermath() throws Exception {
        var def=apocalypse().apply(weapons().get("helios"));
        assertEquals(3600,def.duration());
        int impact=start(def,Phase.IMPACT);
        assertEquals(2500,impact,"impact tick");
        // Impact, both devastation passes and the aftermath together must hold at least 50 seconds.
        assertTrue(def.duration()-impact>=1000,"post-impact time "+(def.duration()-impact));
        assertTrue(ticks(def,Phase.AFTERMATH)>=500,"aftermath "+ticks(def,Phase.AFTERMATH));
        // The cosmic voyage, reveal/deployment and charge each get their authored share.
        assertTrue(ticks(def,Phase.DEPLOYMENT)>=300&&ticks(def,Phase.CHARGING)>=300);
        // The primary impact cue lands exactly on IMPACT and each ring pass starts its own wave stage.
        var impacts=def.events().stream().filter(e->e.effect().equals("impact")).toList();
        assertEquals(impact,impacts.getFirst().tick());
        assertEquals(start(def,Phase.DEVASTATION_WAVE_1)+1,impacts.get(1).tick());
        assertEquals(start(def,Phase.DEVASTATION_WAVE_2)+1,impacts.get(2).tick());
        // Phase order is still chronological for every derived helper.
        Phase previous=null;
        for(var stage:def.phases()){if(previous!=null)assertTrue(stage.phase().ordinal()>previous.ordinal());previous=stage.phase();}
    }

    @Test void pacingKeepsItsShapeAtTheNinetySecondFloor() throws Exception {
        var base=weapons().get("helios");
        var def=apocalypse().apply(base,.01);
        assertTrue(def.duration()>=1800,"floor "+def.duration());
        assertTrue(ticks(def,Phase.AFTERMATH)>=200,"aftermath at the floor "+ticks(def,Phase.AFTERMATH));
        assertTrue(def.events().stream().allMatch(e->e.tick()<def.duration()));
    }

    @Test void standardHeliosAndOtherColossalWeaponsAreUnchanged() throws Exception {
        var weapons=weapons();
        var helios=weapons.get("helios");
        assertSame(helios,ScaleProfile.standard(helios).apply(helios));
        assertEquals(170,helios.events().getFirst().tick());
        var singularity=ScaleCatalog.defaults().stream().filter(p->p.weapon().equals("singularity")&&p.tier()==ScaleTier.APOCALYPSE).findFirst().orElseThrow();
        assertEquals(1800,singularity.apply(weapons.get("singularity")).duration());
    }

    @Test void untouchedLegacyDefaultMigratesAndEditedProfilesStay(@TempDir Path dir) throws Exception {
        var weapons=weapons();
        Path file=dir.resolve("scales.json");
        var data=new ScaleCatalog.Data();
        var legacy=new java.util.ArrayList<>(ScaleCatalog.defaults());
        legacy.replaceAll(p->p.weapon().equals("helios")&&p.tier()==ScaleTier.APOCALYPSE?ScaleCatalog.legacyDefaults().getFirst():p);
        data.profiles=legacy;
        Files.writeString(file,ConfigFiles.JSON.toJson(data));
        var migrated=new ScaleCatalog(file,weapons);
        assertEquals(3600,migrated.get("helios",ScaleTier.APOCALYPSE,weapons.get("helios")).durationTicks());
        assertTrue(Files.readString(file).contains("3600"),"migration persisted");
        // An operator's own duration is respected exactly.
        var edited=new java.util.ArrayList<>(legacy);
        var old=ScaleCatalog.legacyDefaults().getFirst();
        edited.replaceAll(p->p.equals(old)?new ScaleProfile(old.weapon(),old.tier(),old.radius(),old.depth(),2400,old.cinematicScale(),old.structureScale(),
                old.beamWidth(),old.shockwaveRadius(),old.blastRadius(),old.entityRadius(),old.subimpacts(),old.script(),old.anchor(),old.alignmentNodes(),old.rings()):p);
        data.profiles=edited;
        Files.writeString(file,ConfigFiles.JSON.toJson(data));
        assertEquals(2400,new ScaleCatalog(file,weapons).get("helios",ScaleTier.APOCALYPSE,weapons.get("helios")).durationTicks());
    }
}
