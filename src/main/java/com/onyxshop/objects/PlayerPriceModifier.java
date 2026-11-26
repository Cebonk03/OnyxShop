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

   public void setSectionModifier(String var1, PriceModifierType var2, double var3) {
      this.sectionModifiers.computeIfAbsent(var1, var0 -> new PriceModifierData()).set(var2, var3);
   }

   public void removeSectionModifier(String var1, PriceModifierType var2) {
      this.sectionModifiers.get(var1).remove(var2);
      if (this.sectionModifiers.get(var1).keySet().isEmpty()) {
         this.sectionModifiers.remove(var1);
      }
   }

   public void removeSectionModifier(String var1) {
      this.sectionModifiers.remove(var1);
   }

   public void setItemModifier(String var1, PriceModifierType var2, double var3) {
      this.itemModifiers.computeIfAbsent(var1, var0 -> new PriceModifierData()).set(var2, var3);
   }

   public void removeItemModifier(String var1, PriceModifierType var2) {
      this.itemModifiers.get(var1).remove(var2);
      if (this.itemModifiers.get(var1).keySet().isEmpty()) {
         this.itemModifiers.remove(var1);
      }
   }

   public void removeItemModifier(String var1) {
      this.itemModifiers.remove(var1);
   }
}
