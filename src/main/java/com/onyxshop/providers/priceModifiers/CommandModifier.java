package com.onyxshop.providers.priceModifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CommandModifier extends PriceModifier {
   private final Map<UUID, Double> modifiers = new HashMap<>();

   public CommandModifier() {
   }

   public void setBestModifier(UUID var1, double var2) {
      this.modifiers.compute(var1, (var2x, var3) -> var3 == null ? var2 : Math.max(var3, var2));
   }

   public void setModifier(UUID var1, double var2) {
      this.modifiers.put(var1, var2);
   }

   public boolean removeModifier(UUID var1) {
      return this.modifiers.remove(var1) != null;
   }

   @Override
   public double modify(Player var1, double var2) {
      return !this.modifiers.containsKey(var1.getUniqueId()) ? var2 : var2 * (this.modifiers.get(var1.getUniqueId()) / 100.0);
   }
}
