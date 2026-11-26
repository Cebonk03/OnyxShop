package mc.rellox.spawnerlegacyapi.spawner;

import mc.rellox.spawnerlegacyapi.spawner.type.SpawnerType;
import org.bukkit.inventory.ItemStack;

/** TODO: unrecovered - stub of the SpawnerLegacy API (v3+), compile-only. */
public interface IVirtual {

    static Builder builder(SpawnerType type) {
        return new Builder();
    }

    SpawnerType type();

    ItemStack toItem();

    final class Builder {

        public IVirtual build() {
            return null;
        }
    }
}
