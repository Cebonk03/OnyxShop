package com.onyxshop.providers.priceModifiers;

import java.util.ArrayList;
import com.onyxshop.providers.priceModifiers.seasons.SeasonModifier;

public class Modifiers extends ArrayList<PriceModifier> {
   public Modifiers() {
   }

   public PriceModifier getByType(ModifierType modifierType) {
      for (PriceModifier priceModifier : this) {
         if (priceModifier.getClass() == modifierType.getClazz()) {
            return priceModifier;
         }
      }

      return null;
   }

   public PriceModifier putIfAbsent(ModifierType modifierType, PriceModifier priceModifier) {
      for (PriceModifier priceModifier2 : this) {
         if (priceModifier2.getClass() == modifierType.getClazz()) {
            return priceModifier2;
         }
      }

      this.add(priceModifier);
      return priceModifier;
   }

   public boolean hasType(ModifierType modifierType) {
      for (PriceModifier priceModifier : this) {
         if (priceModifier.getClass() == modifierType.getClazz()) {
            return true;
         }
      }

      return false;
   }

   public boolean removeType(ModifierType modifierType) {
      for (PriceModifier priceModifier : this) {
         if (priceModifier.getClass() == modifierType.getClazz()) {
            return super.remove(priceModifier);
         }
      }

      return false;
   }

   public boolean hasSeasonModifier(String text) {
      for (PriceModifier priceModifier : this) {
         if (priceModifier instanceof SeasonModifier && ((SeasonModifier)priceModifier).getWorld().equals(text)) {
            return true;
         }
      }

      return false;
   }
}
