// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

import com.nullstar.config.ConfigFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Six named operator profiles; STANDARD always resolves the existing weapon definition. */
public final class ScaleCatalog {
    public static final class Data {
        public List<ScaleProfile> profiles = defaults();
    }
    private final List<ScaleProfile> profiles;
    public ScaleCatalog(Path path, WeaponCatalog weapons) throws IOException {
        Data data=ConfigFiles.load(path,Data.class,Data::new);
        if (data==null || data.profiles==null || data.profiles.size()!=6) throw new IllegalArgumentException("Expected six colossal scale profiles");
        // Part 3 migration: a profile that still equals an untouched earlier default adopts the current default.
        // Operator-edited profiles never match exactly and are kept as written.
        var migrated=new java.util.ArrayList<ScaleProfile>();boolean changed=false;
        for(ScaleProfile profile:data.profiles) {
            ScaleProfile current=migrate(profile);
            changed|=current!=profile;migrated.add(current);
        }
        if(changed) {
            data.profiles=migrated;
            java.nio.file.Files.writeString(path,ConfigFiles.JSON.toJson(data));
        }
        profiles=List.copyOf(migrated);
        for(String id:WeaponCatalog.IDS) for(ScaleTier tier:List.of(ScaleTier.TITANIC,ScaleTier.APOCALYPSE)) {
            if(profiles.stream().filter(p->p.weapon().equals(id)&&p.tier()==tier).count()!=1) throw new IllegalArgumentException("Duplicate or missing scale profile");
            get(id,tier,weapons.get(id)).apply(weapons.get(id));
        }
    }
    public ScaleProfile get(String id,ScaleTier tier,WeaponDefinition base) {
        if(tier==ScaleTier.STANDARD)return ScaleProfile.standard(base);
        return profiles.stream().filter(p->p.weapon().equals(id)&&p.tier()==tier).findFirst().orElseThrow();
    }
    /** Earlier shipped defaults that are upgraded in place when found unmodified. */
    static List<ScaleProfile> legacyDefaults() {
        return List.of(profile("helios",ScaleTier.APOCALYPSE,500,96,2000,0));
    }
    static ScaleProfile migrate(ScaleProfile profile) {
        for(ScaleProfile legacy:legacyDefaults())if(legacy.equals(profile))
            return defaults().stream().filter(p->p.weapon().equals(profile.weapon())&&p.tier()==profile.tier()).findFirst().orElse(profile);
        return profile;
    }
    public static List<ScaleProfile> defaults() {
        return List.of(profile("helios",ScaleTier.TITANIC,128,48,1400,0),profile("helios",ScaleTier.APOCALYPSE,500,96,3600,0),
                profile("singularity",ScaleTier.TITANIC,96,64,1200,0),profile("singularity",ScaleTier.APOCALYPSE,320,160,1800,0),
                profile("glassfall",ScaleTier.TITANIC,112,40,1400,8),profile("glassfall",ScaleTier.APOCALYPSE,360,80,2000,16));
    }
    private static ScaleProfile profile(String id,ScaleTier tier,int radius,int depth,int ticks,int impacts) {
        boolean apocalypse=tier==ScaleTier.APOCALYPSE;
        return new ScaleProfile(id,tier,radius,depth,ticks,apocalypse?1e9:1e5,apocalypse?1e6:1000,
                apocalypse?48:12,radius*4,Math.min(256,radius),Math.min(384,radius),impacts,
                id+"_colossal","stellar",12,List.of(new ScaleProfile.Ring(1.15,.88,.3),new ScaleProfile.Ring(1.3,1.08,.12)));
    }
}
