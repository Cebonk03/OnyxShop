package mc.rellox.spawnerlegacy.api.spawner;

import mc.rellox.spawnerlegacy.spawner.type.SpawnerType;
import org.bukkit.inventory.ItemStack;

/** TODO: unrecovered - stub of the SpawnerLegacy API (v1), compile-only. */
public interface IVirtual {

    static IVirtual of(ItemStack item) {
        return null;
    }

    SpawnerType type();
}
