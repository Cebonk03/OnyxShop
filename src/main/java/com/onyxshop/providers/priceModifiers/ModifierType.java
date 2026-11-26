package com.onyxshop.providers.priceModifiers;

import com.onyxshop.providers.priceModifiers.seasons.SeasonModifier;

public enum ModifierType {
   SEASON(SeasonModifier.class),
   COMMAND(CommandModifier.class);

   private final Class<? extends PriceModifier> clazz;

   private ModifierType(Class<? extends PriceModifier> nullxx) {
      this.clazz = nullxx;
   }

   public Class<? extends PriceModifier> getClazz() {
      return this.clazz;
   }
}
