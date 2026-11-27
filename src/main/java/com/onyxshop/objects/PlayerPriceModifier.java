package com.onyxshop.objects;

import java.util.HashMap;
import java.util.Map;

public class PlayerPriceModifier {
   private final Map<String, PriceModifierData> sectionModifiers = new HashMap<>();
   private final Map<String, PriceModifierData> itemModifiers = new HashMap<>();

   public PlayerPriceModifier() {
   }

   public Map<String, PriceModifierData> getSectionModifiers() {
      return this.sectionModifiers;
   }

   public Map<String, PriceModifierData> getItemModifiers() {
      return this.itemModifiers;
   }

   public void setSectionModifier(String sectionId, PriceModifierType priceModifierType, double price) {
      this.sectionModifiers.computeIfAbsent(sectionId, key -> new PriceModifierData()).set(priceModifierType, price);
   }

   public void removeSectionModifier(String sectionId, PriceModifierType priceModifierType) {
      this.sectionModifiers.get(sectionId).remove(priceModifierType);
      if (this.sectionModifiers.get(sectionId).keySet().isEmpty()) {
         this.sectionModifiers.remove(sectionId);
      }
   }

   public void removeSectionModifier(String sectionId) {
      this.sectionModifiers.remove(sectionId);
   }

   public void setItemModifier(String itemId, PriceModifierType priceModifierType, double price) {
      this.itemModifiers.computeIfAbsent(itemId, key -> new PriceModifierData()).set(priceModifierType, price);
   }

   public void removeItemModifier(String itemId, PriceModifierType priceModifierType) {
      this.itemModifiers.get(itemId).remove(priceModifierType);
      if (this.itemModifiers.get(itemId).keySet().isEmpty()) {
         this.itemModifiers.remove(itemId);
      }
   }

   public void removeItemModifier(String itemId) {
      this.itemModifiers.remove(itemId);
   }
}
