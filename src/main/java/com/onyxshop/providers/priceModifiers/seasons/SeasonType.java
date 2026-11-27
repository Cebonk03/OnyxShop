package com.onyxshop.providers.priceModifiers.seasons;

import java.util.Locale;

public enum SeasonType {
   SPRING,
   FALL,
   SUMMER,
   WINTER,
   DISABLED;

   private SeasonType() {
   }

   public static SeasonType getFromString(String type) {
      try {
         return valueOf(type.toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   public static SeasonType get(me.casperge.realisticseasons.season.Season season) {
      try {
         return valueOf(season.name());
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }
}
