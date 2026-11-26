package net.advancedplugins.ae.enchanthandler.enchantments;

import org.bukkit.Material;

/** TODO: unrecovered - stub of an AdvancedEnchantments enchantment, compile-only. */
public interface AdvancedEnchantment {
    boolean isEnabled();

    boolean canBeApplied(Material material);

    String getName();
}
