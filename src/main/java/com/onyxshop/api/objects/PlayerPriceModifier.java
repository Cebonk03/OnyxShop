package com.onyxshop.api.objects;

import java.util.Collections;
import java.util.Map;
import com.onyxshop.objects.PriceModifierData;

public class PlayerPriceModifier {
   private final Map<String, PriceModifierData> sectionModifiers;
   private final Map<String, PriceModifierData> itemModifiers;

   public PlayerPriceModifier(com.onyxshop.objects.PlayerPriceModifier var1) {
      this.sectionModifiers = Collections.unmodifiableMap(var1.getSectionModifiers());
      this.itemModifiers = Collections.unmodifiableMap(var1.getItemModifiers());
   }

   public Map<String, PriceModifierData> getSectionModifiers() {
      return this.sectionModifiers;
   }

   public Map<String, PriceModifierData> getItemModifiers() {
      return this.itemModifiers;
   }
}
