package com.onyxshop.util;

import net.kyori.adventure.text.Component;

public abstract class AdventureUtils {
   public AdventureUtils() {
   }

   public abstract String formatMiniToLegacy(String var1);

   public abstract String formatLegacyToMini(String var1);

   public abstract Component formatMini(String var1);

   public abstract Component formatLegacyAndMini(String var1);

   public abstract String formatJsonString(String var1);

   public abstract String getAsJson(String var1);

   public abstract String getGsonComponent(String var1);

   public abstract String stripColor(String var1);

   public abstract String getLastColors(String var1);
}
