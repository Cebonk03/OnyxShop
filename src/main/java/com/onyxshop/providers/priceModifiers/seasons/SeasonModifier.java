package com.onyxshop.providers.priceModifiers.seasons;

import java.util.Objects;
import com.onyxshop.providers.priceModifiers.PriceModifier;
import org.bukkit.entity.Player;

public class SeasonModifier extends PriceModifier {
   private final int modifier;
   private final String world;

   public SeasonModifier(String var1, int var2) {
      this.world = var1;
      this.modifier = var2;
   }

   public String getWorld() {
      return this.world;
   }

   @Override
   public double modify(Player var1, double var2) {
      return !var1.getWorld().getName().equals(this.world) ? var2 : var2 + var2 / 100.0 * this.modifier;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         SeasonModifier var2 = (SeasonModifier)var1;
         return this.modifier == var2.modifier && this.world.equals(var2.world);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.modifier, this.world);
   }
}
