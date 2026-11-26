package com.onyxshop.providers.priceModifiers.seasons;

public class Season {
   private final String icon;
   private final String name;
   private final SeasonType type;

   public Season(String var1, String var2, SeasonType var3) {
      this.icon = var1;
      this.name = var2;
      this.type = var3;
   }

   public String getIcon() {
      return this.icon;
   }

   public String getName() {
      return this.name;
   }

   public SeasonType getType() {
      return this.type;
   }
}
