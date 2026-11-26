package mc.rellox.spawnerlegacyapi;

import mc.rellox.spawnerlegacyapi.spawner.ISpawners;

/** TODO: unrecovered - stub of the SpawnerLegacy API (v3+), compile-only. */
public final class SLAPI {

    private static final ISpawners SPAWNERS = new ISpawners();

    private SLAPI() {
    }

    public static ISpawners spawners() {
        return SPAWNERS;
    }
}
