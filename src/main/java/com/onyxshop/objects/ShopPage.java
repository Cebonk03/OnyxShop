package com.onyxshop.objects;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Translatable;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.util.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ShopPage extends ShopInventory {
   private final int allPages;
   private final boolean disabledBackButton;
   private final String rootSection;
   private final Set<Integer> fillItems = new HashSet<>();
   private final Player player;
   private List<String> itemOrder;
   private ScheduledTask updateTask;
   private final int size;
   private int page;
   private final String section;
   private final Inventory inventory;

   public int getAllPages() {
      return this.allPages;
   }

   public boolean isDisabledBackButton() {
      return this.disabledBackButton;
   }

   public String getSection() {
      return this.section;
   }

   public String getItem(int value) {
      return this.itemOrder.get(value);
   }

   public ShopPage(
      OnyxShop plugin,
      Map<Integer, ItemStack> map,
      List<String> values,
      ItemStack itemStack,
      Player player,
      Translatable text,
      String text2,
      int value,
      int value2,
      boolean flag,
      boolean flag2,
      int value3,
      @Nullable String text3
   ) {
      this.page = value;
      this.itemOrder = values;
      this.allPages = value2;
      this.section = text2;
      this.disabledBackButton = flag;
      this.size = value3;
      this.rootSection = text3;
      this.player = player;
      this.inventory = plugin.getMetaUtils().createInventory(this, value3, text);
      this.init(plugin, player, map, itemStack, flag2);
      this.startUpdateTask(plugin);
   }

   private void startUpdateTask(OnyxShop plugin) {
      long updateInterval = plugin.getSection(this.section).getUpdateInterval();
      if (updateInterval > 0L) {
         this.updateTask = plugin.runTaskTimer(() -> {
            if (this.inventory.getViewers().isEmpty()) {
               this.updateTask.cancel();
            } else {
               this.updateItems(plugin);
            }
         }, updateInterval, updateInterval);
      }
   }

   private void updateItems(OnyxShop plugin) {
      ShopSection section = plugin.getSection(this.section);

      for (int size = 0; size < this.itemOrder.size(); size++) {
         if (!this.fillItems.contains(size)) {
            String itemId = this.itemOrder.get(size);
            ShopItem shopItem = itemId == null ? null : section.getShopItem(this.section + "." + itemId);
            if (shopItem != null
               && (
                  shopItem.isDynamicPricing()
                     || shopItem.getLimitedStockMode() != 0
                     || shopItem.getLimitedSellMode() != 0
                     || !shopItem.getPlaceholders().isEmpty()
                     || !shopItem.getItgPlaceholders().isEmpty()
               )) {
               this.inventory.setItem(size, section.updateItem(this.player, shopItem));
            }
         }
      }
   }

   private void init(OnyxShop plugin, Player player, Map<Integer, ItemStack> map, ItemStack itemStack, boolean flag) {
      for (int size = 0; size < (flag ? this.size - 9 : this.size); size++) {
         if (map.containsKey(size)) {
            this.inventory.setItem(size, (ItemStack)map.get(size));
         } else if (itemStack != null) {
            this.inventory.setItem(size, itemStack);
            this.fillItems.add(size);
         } else {
            this.inventory.setItem(size, new ItemStack(Material.AIR));
         }
      }

      plugin.navBar.addShopsNavBar(this.inventory, player, this.section, this.page, this.allPages);
   }

   public void updateContents(OnyxShop plugin, Player player, Map<Integer, ItemStack> map, List<String> shopIds, ItemStack itemStack, int value, boolean flag) {
      this.page = value;
      this.itemOrder = shopIds;

      for (int size = 0; size < (flag ? this.size - 9 : this.size); size++) {
         if (map.containsKey(size)) {
            this.inventory.setItem(size, (ItemStack)map.get(size));
         } else if (itemStack != null) {
            if (!this.fillItems.contains(size)) {
               this.inventory.setItem(size, itemStack);
            }
         } else {
            this.inventory.setItem(size, new ItemStack(Material.AIR));
         }
      }

      plugin.navBar.updateShopsNavBar(this.inventory, player, this.section, value, this.allPages);
   }

   public int getPage() {
      return this.page;
   }

   public int getSize() {
      return this.size;
   }

   public boolean isSubSection() {
      return this.rootSection != null;
   }

   public String getRootSection() {
      return this.rootSection;
   }

   @Override
   public Inventory getInventory() {
      return this.inventory;
   }
}
