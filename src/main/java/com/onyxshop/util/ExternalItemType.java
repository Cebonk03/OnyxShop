package com.onyxshop.util;

import java.util.Objects;
import org.bukkit.plugin.Plugin;

public class ExternalItemType extends ItemType {
   private final String provider;
   private final String providerPrefix;

   public ExternalItemType(Plugin var1, String var2) {
      super(ItemProviderType.EXTERNAL);
      this.provider = var1.getName();
      this.providerPrefix = var2;
   }

   public ExternalItemType(String var1) {
      super(ItemProviderType.EXTERNAL);
      this.provider = "";
      this.providerPrefix = var1;
   }

   @Override
   public String getName() {
      return this.provider;
   }

   @Override
   public String getPrefix() {
      return this.providerPrefix;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 instanceof ExternalItemType) {
         ExternalItemType var2 = (ExternalItemType)var1;
         return var2.providerPrefix.equals(this.providerPrefix);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.providerPrefix);
   }
}
