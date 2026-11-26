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

   public ItemFormat(String var1, String var2, int var3) {
      this.format = var1;
      this.path = var2;
      this.i = var3;
      this.load();
   }

   public void load() {
      try {
         Map var1 = ItemFormatter.serialize(this.format);
         if (!var1.containsKey("material")) {
            throw new ItemLoadException("Cannot find material of item, valid format: ITEM:material:<type>");
         }

         this.stack = ItemFormatter.build(var1, this.path, this.i);
         if (var1.containsKey("ignored-tags")) {
            this.iTags
               .addAll(
                  Arrays.asList(((String)var1.get("ignored-tags")).split(","))
                     .stream()
                     .map(var1x -> NBTMaps.getTag(var1x, this.path.split("\\.")[0], "." + this.path.split("\\.", 2)[1]))
                     .collect(Collectors.toList())
               );
         }

         this.id = var1.containsKey("id") ? (String)var1.get("id") : this.format.replace(" ", "-");
      } catch (IndexOutOfBoundsException | ItemLoadException var2) {
         this.errorMessage = "Failed to create item using format of " + this.format + ChatColor.RESET + " with the following reason:\n" + var2.getMessage();
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
