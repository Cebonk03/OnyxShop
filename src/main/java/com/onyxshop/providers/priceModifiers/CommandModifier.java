package com.onyxshop.providers.priceModifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CommandModifier extends PriceModifier {
   private final Map<UUID, Double> modifiers = new HashMap<>();

   public CommandModifier() {
   }

   public void setBestModifier(UUID uniqueId, double amount) {
      this.modifiers.compute(uniqueId, (key, value) -> value == null ? amount : Math.max(value, amount));
   }

   public void setModifier(UUID uniqueId, double amount) {
      this.modifiers.put(uniqueId, amount);
   }

   public boolean removeModifier(UUID uniqueId) {
      return this.modifiers.remove(uniqueId) != null;
   }

   @Override
   public double modify(Player player, double amount) {
      return !this.modifiers.containsKey(player.getUniqueId()) ? amount : amount * (this.modifiers.get(player.getUniqueId()) / 100.0);
   }
}
