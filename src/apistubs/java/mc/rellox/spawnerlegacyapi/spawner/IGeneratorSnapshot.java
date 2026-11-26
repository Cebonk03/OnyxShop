package mc.rellox.spawnerlegacyapi.spawner;

import mc.rellox.spawnerlegacyapi.spawner.type.SpawnerType;

/** TODO: unrecovered - stub of the SpawnerLegacy API (v4), compile-only. */
public interface IGeneratorSnapshot extends IVirtual {

    static Builder builder(SpawnerType type) {
        return new Builder();
    }

    final class Builder {

        public IGeneratorSnapshot build() {
            return null;
        }
    }
}
