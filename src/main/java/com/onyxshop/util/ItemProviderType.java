package com.onyxshop.util;

import java.util.Objects;
import com.onyxshop.util.serializer.SerializerType;

public class ItemProviderType {
   public static final ItemProviderType EXECUTEABLE_ITEMS = new ItemProviderType("ExecutableItems", "ei", TaggableItemType.EXECUTABLE_ITEMS);
   public static final ItemProviderType HEAD_DATABASE = new ItemProviderType("HeadDatabase", "hdb");
   public static final ItemProviderType ITEMS_ADDER = new ItemProviderType("ItemsAdder", "ia", SerializerType.ADVENTURE);
   public static final ItemProviderType MMO_ITEMS = new ItemProviderType("MMOItems", "mmoitem", SerializerType.BUKKIT_V2);
   public static final ItemProviderType ORAXEN = new ItemProviderType("Oraxen", "oraxen");
   public static final ItemProviderType BREWERY = new ItemProviderType("Brewery", "brewery");
   public static final ItemProviderType NEXO = new ItemProviderType("Nexo", "nexo");
   public static final ItemProviderType SLIMEFUN = new ItemProviderType("Slimefun", "sf");
   public static final ItemProviderType ECOITEMS = new ItemProviderType("EcoItems", "ecoitems", SerializerType.ADVENTURE);
   public static final ItemProviderType MYTHIC_MOBS = new ItemProviderType("MythicMobs", "mythicmobs", TaggableItemType.MYTHIC_MOBS_ITEM);
   public static final ItemProviderType CRAFT_ENGINE = new ItemProviderType("CraftEngine", "craftengine", TaggableItemType.CRAFT_ENGINE);
   public static final ItemProviderType EXTERNAL = new ItemProviderType("EXTERNAL", "external");
   public static final ItemProviderType[] VALUES = new ItemProviderType[]{
      EXECUTEABLE_ITEMS, HEAD_DATABASE, ITEMS_ADDER, MMO_ITEMS, ORAXEN, BREWERY, NEXO, SLIMEFUN, ECOITEMS, MYTHIC_MOBS, CRAFT_ENGINE, EXTERNAL
   };
   public final String name;
   public final String prefix;
   public final TaggableItemType taggable;
   public final SerializerType serializer;

   public ItemProviderType(String var1, String var2) {
      this.name = var1;
      this.prefix = var2;
      this.serializer = null;
      this.taggable = null;
   }

   public ItemProviderType(String var1, String var2, TaggableItemType var3) {
      this.name = var1;
      this.prefix = var2;
      this.taggable = var3;
      this.serializer = null;
   }

   public ItemProviderType(String var1, String var2, SerializerType var3) {
      this.name = var1;
      this.prefix = var2;
      this.taggable = null;
      this.serializer = var3;
   }

   public ItemProviderType(String var1, String var2, TaggableItemType var3, SerializerType var4) {
      this.name = var1;
      this.prefix = var2;
      this.taggable = var3;
      this.serializer = var4;
   }

   public String getName() {
      return this.name;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public SerializerType getSerializer() {
      return this.serializer;
   }

   public TaggableItemType getTaggable() {
      return this.taggable;
   }

   public static ItemProviderType fromName(String var0) {
      for (ItemProviderType var4 : VALUES) {
         if (var4.name.equals(var0)) {
            return var4;
         }
      }

      return null;
   }

   public static ItemProviderType fromPrefix(String var0) {
      for (ItemProviderType var4 : VALUES) {
         if (var4.prefix.equals(var0)) {
            return var4;
         }
      }

      return null;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof ItemProviderType)) {
         if (var1 instanceof ItemType) {
            ItemType var3 = (ItemType)var1;
            return var3.providerType == this;
         } else {
            return false;
         }
      } else {
         ItemProviderType var2 = (ItemProviderType)var1;
         return this.name.equals(var2.name) && this.prefix.equals(var2.prefix);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name, this.prefix);
   }
}
