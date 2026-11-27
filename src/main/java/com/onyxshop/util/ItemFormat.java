package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class ItemFormat {
   private final String format;
   private final String path;
   private final int i;
   private final List<String> iTags = new ArrayList<>();
   private String errorMessage;
   private ItemStack stack;
   private String id;

   public ItemFormat(String itemId, String text, int value) {
      this.format = itemId;
      this.path = text;
      this.i = value;
      this.load();
   }

   public void load() {
      try {
         Map itemIds = ItemFormatter.serialize(this.format);
         if (!itemIds.containsKey("material")) {
            throw new ItemLoadException("Cannot find material of item, valid format: ITEM:material:<type>");
         }

         this.stack = ItemFormatter.build(itemIds, this.path, this.i);
         if (itemIds.containsKey("ignored-tags")) {
            this.iTags
               .addAll(
                  Arrays.asList(((String)itemIds.get("ignored-tags")).split(","))
                     .stream()
                     .map(element -> NBTMaps.getTag(element, this.path.split("\\.")[0], "." + this.path.split("\\.", 2)[1]))
                     .collect(Collectors.toList())
               );
         }

         this.id = itemIds.containsKey("id") ? (String)itemIds.get("id") : this.format.replace(" ", "-");
      } catch (IndexOutOfBoundsException | ItemLoadException exception) {
         this.errorMessage = "Failed to create item using format of " + this.format + ChatColor.RESET + " with the following reason:\n" + exception.getMessage();
      }
   }

   public String getErrorMessage() {
      return this.errorMessage;
   }

   public ItemStack getItem() {
      return this.stack;
   }

   public List<String> getiTags() {
      return this.iTags;
   }

   public String getFormat() {
      return this.format;
   }

   public String getId() {
      return this.id;
   }

   public String getPath() {
      return this.path;
   }
}
