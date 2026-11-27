package com.onyxshop.util;

import net.kyori.adventure.text.Component;

public abstract class AdventureUtils {
   public AdventureUtils() {
   }

   public abstract String formatMiniToLegacy(String text);

   public abstract String formatLegacyToMini(String text);

   public abstract Component formatMini(String text);

   public abstract Component formatLegacyAndMini(String text);

   public abstract String formatJsonString(String text);

   public abstract String getAsJson(String text);

   public abstract String getGsonComponent(String text);

   public abstract String stripColor(String text);

   public abstract String getLastColors(String text);
}
