package com.onyxshop.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PriceModifierData {
   private final Map<PriceModifierType, Double> modifiers = new HashMap<>();

   public PriceModifierData() {
   }

   public Double get(PriceModifierType priceModifierType) {
      return this.modifiers.get(priceModifierType);
   }

   public double getOr(PriceModifierType priceModifierType, double price) {
      return this.modifiers.getOrDefault(priceModifierType, price);
   }

   public void set(PriceModifierType priceModifierType, double price) {
      this.modifiers.put(priceModifierType, price);
   }

   public void remove(PriceModifierType priceModifierType) {
      this.modifiers.remove(priceModifierType);
   }

   public Set<PriceModifierType> keySet() {
      return this.modifiers.keySet();
   }
}
