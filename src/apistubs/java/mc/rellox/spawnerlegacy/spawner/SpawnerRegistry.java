package mc.rellox.spawnerlegacy.spawner;

import mc.rellox.spawnerlegacy.spawner.type.SpawnerType;
import org.bukkit.inventory.ItemStack;

/** TODO: unrecovered - stub of the SpawnerLegacy API, compile-only. */
public final class SpawnerRegistry {

    private SpawnerRegistry() {
    }

    public static ItemStack asItem(SpawnerType type, int amount) {
        return null;
    }

    /** Overload used by the bundled (v2) API layout, which ships its own SpawnerType. */
    public static ItemStack asItem(mc.rellox.spawnerlegacy.lib.spawnerlegacyapi.spawner.type.SpawnerType type, int amount) {
        return null;
    }
}
