package com.onyxshop.providers.priceModifiers.seasons;

public class Season {
   private final String icon;
   private final String name;
   private final SeasonType type;

   public Season(String type, String text, SeasonType seasonType) {
      this.icon = type;
      this.name = text;
      this.type = seasonType;
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
