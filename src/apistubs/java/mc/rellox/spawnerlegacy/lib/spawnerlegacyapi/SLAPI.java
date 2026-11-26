package mc.rellox.spawnerlegacy.lib.spawnerlegacyapi;

import mc.rellox.spawnerlegacy.lib.spawnerlegacyapi.spawner.ISpawners;

/** TODO: unrecovered - stub of the SpawnerLegacy API (v2), compile-only. */
public final class SLAPI {

    private static final ISpawners SPAWNERS = new ISpawners();

    private SLAPI() {
    }

    public static ISpawners spawners() {
        return SPAWNERS;
    }
}
