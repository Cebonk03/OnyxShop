package com.onyxshop.objects;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.util.SimplePair;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SectionItem {
   private SimplePair<Integer, String> tickLine;
   private final boolean hideIfNoPerms;
   private final boolean rotating;
   private final Map<Integer, String> placeholders;
   private final Set<Integer> slots = new HashSet<>();
   private ItemStack item;
   private String bedrockTexture;
   public String section;

   public SectionItem(OnyxShop var1, String var2) {
      this.section = var2;

      try {
         this.item = var1.createItem.loadSectionItem(this.section, "item");
      } catch (ItemLoadException var4) {
         SendMessage.warnMessage(var4.getMessage());
         SendMessage.errorSections(var2, "item");
         this.item = var1.createItem.getInvalidItem(var4);
      }

      this.bedrockTexture = ConfigManager.getSection(var2).getString("item.bedrock-texture");
      this.placeholders = PlaceholderAPIHook.getPlaceholders(this.item);
      this.hideIfNoPerms = ConfigManager.getSection(var2).getBoolean("hide-if-no-perms");
      if (this.rotating = var1.getConfigManager().getShopType(this.section) == ShopType.ROTATING) {
         this.tickLine = this.getPlaceholder(this.item, "%nextRotation%");
      }

      if (ConfigManager.getSection(this.section).getBoolean("display-item")) {
         var1.removeShopSection(var2);
      }
   }

   public void addSlot(int var1) {
      this.slots.add(var1);
   }

   public Set<Integer> getSlots() {
      return this.slots;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public String getBedrockTexture() {
      return this.bedrockTexture;
   }

   public boolean isHideIfNoPerms() {
      return this.hideIfNoPerms;
   }

   public boolean isRotating() {
      return this.rotating && this.tickLine != null;
   }

   public ItemStack getItem(Player var1) {
      return !this.placeholders.isEmpty() ? this.parsePlaceholders(var1, this.item.clone()) : this.item;
   }

   private ItemStack parsePlaceholders(Player var1, ItemStack var2) {
      ItemMeta var3 = var2.getItemMeta();
      if (this.placeholders.containsKey(-1)) {
         var3.setDisplayName(PlaceholderAPIHook.translate(var1, var3.getDisplayName()));
      }

      if (var3.hasLore()) {
         List var4 = var3.getLore();

         for (Integer var6 : this.placeholders.keySet()) {
            if (var6 != -1) {
               var4.set(var6, PlaceholderAPIHook.translate(var1, (String)var4.get(var6)));
            }
         }

         var3.setLore(var4);
      }

      var2.setItemMeta(var3);
      return var2;
   }

   private SimplePair<Integer, String> getPlaceholder(ItemStack var1, String var2) {
      if (var1.hasItemMeta()) {
         ItemMeta var3 = var1.getItemMeta();
         if (var3.hasDisplayName() && var3.getDisplayName().contains(var2)) {
            return new SimplePair<>(-1, var3.getDisplayName());
         }

         if (var3.hasLore()) {
            List var4 = var3.getLore();

            for (int var5 = 0; var5 < var4.size(); var5++) {
               if (((String)var4.get(var5)).contains(var2)) {
                  return new SimplePair<>(var5, (String)var4.get(var5));
               }
            }
         }
      }

      return null;
   }

   public Runnable getTickTask(Inventory var1, ItemStack var2) {
      return () -> {
         ItemMeta var3 = var2.getItemMeta();
         if (this.tickLine.key == -1) {
            var3.setDisplayName(
               this.tickLine.value.replace("%nextRotation%", ((RotatingShop)OnyxShop.getInstance().getSection(this.section)).getNextRotation())
            );
         } else {
            List var4 = var3.getLore();
            var4.set(
               this.tickLine.key,
               this.tickLine.value.replace("%nextRotation%", ((RotatingShop)OnyxShop.getInstance().getSection(this.section)).getNextRotation())
            );
            var3.setLore(var4);
         }

         var2.setItemMeta(var3);

         for (Integer var5 : this.slots) {
            var1.setItem(var5, var2);
         }
      };
   }
}
