package com.onyxshop.objects.stands;

import java.util.Objects;

public final class StandSettings {
   private boolean hologram = true;

   public StandSettings(String text) {
      if (text != null) {
         String[] values = text.split("\\|");
         this.hologram = values[0].equals("1");
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
      StringBuilder builder = new StringBuilder();
      builder.append(this.hologram ? 1 : 0).append("|");
      return builder.toString();
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         StandSettings standSettings = (StandSettings)value;
         return this.hologram == standSettings.hologram;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.hologram);
   }
}
