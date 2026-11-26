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

   public static SeasonType getFromString(String var0) {
      try {
         return valueOf(var0.toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   public static SeasonType get(me.casperge.realisticseasons.season.Season var0) {
      try {
         return valueOf(var0.name());
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }
}
