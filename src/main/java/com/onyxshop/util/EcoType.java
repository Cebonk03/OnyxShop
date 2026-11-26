package com.onyxshop.util;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class EcoType {
   private final EconomyType type;
   private final String currency;

   public EcoType(EconomyType var1) {
      this.type = var1;
      this.currency = null;
   }

   public EcoType(EconomyType var1, String var2) {
      this.type = var1;
      this.currency = var2;
   }

   public EconomyType getType() {
      return this.type;
   }

   @Nullable
   public String getCurrency() {
      return this.currency;
   }

   @Override
   public String toString() {
      return this.currency == null ? this.type.name() : this.type.name() + ":" + this.currency;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         EcoType var2 = (EcoType)var1;
         return this.type.equals(var2.type) && Objects.equals(this.currency, var2.currency);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.type, this.currency);
   }
}
