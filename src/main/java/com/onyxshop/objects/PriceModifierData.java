package com.onyxshop.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PriceModifierData {
   private final Map<PriceModifierType, Double> modifiers = new HashMap<>();

   public PriceModifierData() {
   }

   public Double get(PriceModifierType var1) {
      return this.modifiers.get(var1);
   }

   public double getOr(PriceModifierType var1, double var2) {
      return this.modifiers.getOrDefault(var1, var2);
   }

   public void set(PriceModifierType var1, double var2) {
      this.modifiers.put(var1, var2);
   }

   public void remove(PriceModifierType var1) {
      this.modifiers.remove(var1);
   }

   public Set<PriceModifierType> keySet() {
      return this.modifiers.keySet();
   }
}
