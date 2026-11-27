package com.onyxshop.providers.priceModifiers.seasons;

import java.util.Objects;
import com.onyxshop.providers.priceModifiers.PriceModifier;
import org.bukkit.entity.Player;

public class SeasonModifier extends PriceModifier {
   private final int modifier;
   private final String world;

   public SeasonModifier(String text, int value) {
      this.world = text;
      this.modifier = value;
   }

   public String getWorld() {
      return this.world;
   }

   @Override
   public double modify(Player player, double amount) {
      return !player.getWorld().getName().equals(this.world) ? amount : amount + amount / 100.0 * this.modifier;
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         SeasonModifier seasonModifier = (SeasonModifier)value;
         return this.modifier == seasonModifier.modifier && this.world.equals(seasonModifier.world);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.modifier, this.world);
   }
}
