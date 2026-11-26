package com.onyxshop.objects.stands;

import java.util.Objects;

public final class StandSettings {
   private boolean hologram = true;

   public StandSettings(String var1) {
      if (var1 != null) {
         String[] var2 = var1.split("\\|");
         this.hologram = var2[0].equals("1");
      }
   }

   public StandSettings() {
   }

   public boolean isHologram() {
      return this.hologram;
   }

   public void toggleHologram() {
      this.hologram = !this.hologram;
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(this.hologram ? 1 : 0).append("|");
      return var1.toString();
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         StandSettings var2 = (StandSettings)var1;
         return this.hologram == var2.hologram;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.hologram);
   }
}
