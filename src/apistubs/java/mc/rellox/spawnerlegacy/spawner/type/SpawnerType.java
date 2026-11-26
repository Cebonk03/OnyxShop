package mc.rellox.spawnerlegacy.spawner.type;

import org.bukkit.entity.EntityType;

/** TODO: unrecovered - stub of the SpawnerLegacy API, compile-only. */
public final class SpawnerType {

    private SpawnerType() {
    }

    public static SpawnerType of(EntityType type) {
        return new SpawnerType();
    }

    public EntityType entity() {
        return null;
    }
}
