package com.onyxshop.providers.priceModifiers;

import java.util.ArrayList;
import com.onyxshop.providers.priceModifiers.seasons.SeasonModifier;

public class Modifiers extends ArrayList<PriceModifier> {
   public Modifiers() {
   }

   public PriceModifier getByType(ModifierType var1) {
      for (PriceModifier var3 : this) {
         if (var3.getClass() == var1.getClazz()) {
            return var3;
         }
      }

      return null;
   }

   public PriceModifier putIfAbsent(ModifierType var1, PriceModifier var2) {
      for (PriceModifier var4 : this) {
         if (var4.getClass() == var1.getClazz()) {
            return var4;
         }
      }

      this.add(var2);
      return var2;
   }

   public boolean hasType(ModifierType var1) {
      for (PriceModifier var3 : this) {
         if (var3.getClass() == var1.getClazz()) {
            return true;
         }
      }

      return false;
   }

   public boolean removeType(ModifierType var1) {
      for (PriceModifier var3 : this) {
         if (var3.getClass() == var1.getClazz()) {
            return super.remove(var3);
         }
      }

      return false;
   }

   public boolean hasSeasonModifier(String var1) {
      for (PriceModifier var3 : this) {
         if (var3 instanceof SeasonModifier && ((SeasonModifier)var3).getWorld().equals(var1)) {
            return true;
         }
      }

      return false;
   }
}
