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

   public SectionItem(OnyxShop plugin, String sectionId) {
      this.section = sectionId;

      try {
         this.item = plugin.createItem.loadSectionItem(this.section, "item");
      } catch (ItemLoadException exception) {
         SendMessage.warnMessage(exception.getMessage());
         SendMessage.errorSections(sectionId, "item");
         this.item = plugin.createItem.getInvalidItem(exception);
      }

      this.bedrockTexture = ConfigManager.getSection(sectionId).getString("item.bedrock-texture");
      this.placeholders = PlaceholderAPIHook.getPlaceholders(this.item);
      this.hideIfNoPerms = ConfigManager.getSection(sectionId).getBoolean("hide-if-no-perms");
      if (this.rotating = plugin.getConfigManager().getShopType(this.section) == ShopType.ROTATING) {
         this.tickLine = this.getPlaceholder(this.item, "%nextRotation%");
      }

      if (ConfigManager.getSection(this.section).getBoolean("display-item")) {
         plugin.removeShopSection(sectionId);
      }
   }

   public void addSlot(int slot) {
      this.slots.add(slot);
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

   public ItemStack getItem(Player player) {
      return !this.placeholders.isEmpty() ? this.parsePlaceholders(player, this.item.clone()) : this.item;
   }

   private ItemStack parsePlaceholders(Player player, ItemStack itemStack) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (this.placeholders.containsKey(-1)) {
         itemMeta.setDisplayName(PlaceholderAPIHook.translate(player, itemMeta.getDisplayName()));
      }

      if (itemMeta.hasLore()) {
         List lore = itemMeta.getLore();

         for (Integer value : this.placeholders.keySet()) {
            if (value != -1) {
               lore.set(value, PlaceholderAPIHook.translate(player, (String)lore.get(value)));
            }
         }

         itemMeta.setLore(lore);
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   private SimplePair<Integer, String> getPlaceholder(ItemStack itemStack, String itemId) {
      if (itemStack.hasItemMeta()) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName() && itemMeta.getDisplayName().contains(itemId)) {
            return new SimplePair<>(-1, itemMeta.getDisplayName());
         }

         if (itemMeta.hasLore()) {
            List lore = itemMeta.getLore();

            for (int size = 0; size < lore.size(); size++) {
               if (((String)lore.get(size)).contains(itemId)) {
                  return new SimplePair<>(size, (String)lore.get(size));
               }
            }
         }
      }

      return null;
   }

   public Runnable getTickTask(Inventory inventory, ItemStack itemStack) {
      return () -> {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (this.tickLine.key == -1) {
            itemMeta.setDisplayName(
               this.tickLine.value.replace("%nextRotation%", ((RotatingShop)OnyxShop.getInstance().getSection(this.section)).getNextRotation())
            );
         } else {
            List lore = itemMeta.getLore();
            lore.set(
               this.tickLine.key,
               this.tickLine.value.replace("%nextRotation%", ((RotatingShop)OnyxShop.getInstance().getSection(this.section)).getNextRotation())
            );
            itemMeta.setLore(lore);
         }

         itemStack.setItemMeta(itemMeta);

         for (Integer value : this.slots) {
            inventory.setItem(value, itemStack);
         }
      };
   }
}
