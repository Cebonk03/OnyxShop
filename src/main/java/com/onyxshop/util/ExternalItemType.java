package com.onyxshop.util;

import java.util.Objects;
import org.bukkit.plugin.Plugin;

public class ExternalItemType extends ItemType {
   private final String provider;
   private final String providerPrefix;

   public ExternalItemType(Plugin plugin, String itemId) {
      super(ItemProviderType.EXTERNAL);
      this.provider = plugin.getName();
      this.providerPrefix = itemId;
   }

   public ExternalItemType(String itemId) {
      super(ItemProviderType.EXTERNAL);
      this.provider = "";
      this.providerPrefix = itemId;
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
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value instanceof ExternalItemType) {
         ExternalItemType externalItemType = (ExternalItemType)value;
         return externalItemType.providerPrefix.equals(this.providerPrefix);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.providerPrefix);
   }
}
