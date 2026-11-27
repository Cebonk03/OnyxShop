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

   public ItemProviderType(String itemId, String type) {
      this.name = itemId;
      this.prefix = type;
      this.serializer = null;
      this.taggable = null;
   }

   public ItemProviderType(String itemId, String type, TaggableItemType taggableItemType) {
      this.name = itemId;
      this.prefix = type;
      this.taggable = taggableItemType;
      this.serializer = null;
   }

   public ItemProviderType(String itemId, String type, SerializerType serializerType) {
      this.name = itemId;
      this.prefix = type;
      this.taggable = null;
      this.serializer = serializerType;
   }

   public ItemProviderType(String itemId, String type, TaggableItemType taggableItemType, SerializerType serializerType) {
      this.name = itemId;
      this.prefix = type;
      this.taggable = taggableItemType;
      this.serializer = serializerType;
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

   public static ItemProviderType fromName(String name) {
      for (ItemProviderType itemProviderType : VALUES) {
         if (itemProviderType.name.equals(name)) {
            return itemProviderType;
         }
      }

      return null;
   }

   public static ItemProviderType fromPrefix(String itemId) {
      for (ItemProviderType itemProviderType : VALUES) {
         if (itemProviderType.prefix.equals(itemId)) {
            return itemProviderType;
         }
      }

      return null;
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (!(value instanceof ItemProviderType)) {
         if (value instanceof ItemType) {
            ItemType itemType = (ItemType)value;
            return itemType.providerType == this;
         } else {
            return false;
         }
      } else {
         ItemProviderType itemProviderType = (ItemProviderType)value;
         return this.name.equals(itemProviderType.name) && this.prefix.equals(itemProviderType.prefix);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name, this.prefix);
   }
}
