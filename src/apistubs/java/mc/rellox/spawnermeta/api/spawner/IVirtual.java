package mc.rellox.spawnermeta.api.spawner;

import mc.rellox.spawnermeta.spawner.type.SpawnerType;
import org.bukkit.inventory.ItemStack;

/** TODO: unrecovered - stub of the SpawnerMeta API, compile-only. */
public interface IVirtual {

    static IVirtual of(ItemStack item) {
        return null;
    }

    SpawnerType getType();
}
