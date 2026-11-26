package su.nightexpress.excellentenchants.api;

import java.util.Collections;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;

/** TODO: unrecovered - stub of the pre-5.4 ExcellentEnchants registry (5.x moved it), compile-only. */
public final class EnchantRegistry {

    private EnchantRegistry() {
    }

    public static Set<CustomEnchantment> getRegistered() {
        return Collections.emptySet();
    }

    @Nullable
    public static CustomEnchantment getByKey(NamespacedKey key) {
        return null;
    }
}
