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

   public String getItem(int var1) {
      return this.itemOrder.get(var1);
   }

   public ShopPage(
      OnyxShop var1,
      Map<Integer, ItemStack> var2,
      List<String> var3,
      ItemStack var4,
      Player var5,
      Translatable var6,
      String var7,
      int var8,
      int var9,
      boolean var10,
      boolean var11,
      int var12,
      @Nullable String var13
   ) {
      this.page = var8;
      this.itemOrder = var3;
      this.allPages = var9;
      this.section = var7;
      this.disabledBackButton = var10;
      this.size = var12;
      this.rootSection = var13;
      this.player = var5;
      this.inventory = var1.getMetaUtils().createInventory(this, var12, var6);
      this.init(var1, var5, var2, var4, var11);
      this.startUpdateTask(var1);
   }

   private void startUpdateTask(OnyxShop var1) {
      long var2 = var1.getSection(this.section).getUpdateInterval();
      if (var2 > 0L) {
         this.updateTask = var1.runTaskTimer(() -> {
            if (this.inventory.getViewers().isEmpty()) {
               this.updateTask.cancel();
            } else {
               this.updateItems(var1);
            }
         }, var2, var2);
      }
   }

   private void updateItems(OnyxShop var1) {
      ShopSection var2 = var1.getSection(this.section);

      for (int var3 = 0; var3 < this.itemOrder.size(); var3++) {
         if (!this.fillItems.contains(var3)) {
            String var4 = this.itemOrder.get(var3);
            ShopItem var5 = var4 == null ? null : var2.getShopItem(this.section + "." + var4);
            if (var5 != null
               && (
                  var5.isDynamicPricing()
                     || var5.getLimitedStockMode() != 0
                     || var5.getLimitedSellMode() != 0
                     || !var5.getPlaceholders().isEmpty()
                     || !var5.getItgPlaceholders().isEmpty()
               )) {
               this.inventory.setItem(var3, var2.updateItem(this.player, var5));
            }
         }
      }
   }

   private void init(OnyxShop var1, Player var2, Map<Integer, ItemStack> var3, ItemStack var4, boolean var5) {
      for (int var6 = 0; var6 < (var5 ? this.size - 9 : this.size); var6++) {
         if (var3.containsKey(var6)) {
            this.inventory.setItem(var6, (ItemStack)var3.get(var6));
         } else if (var4 != null) {
            this.inventory.setItem(var6, var4);
            this.fillItems.add(var6);
         } else {
            this.inventory.setItem(var6, new ItemStack(Material.AIR));
         }
      }

      var1.navBar.addShopsNavBar(this.inventory, var2, this.section, this.page, this.allPages);
   }

   public void updateContents(OnyxShop var1, Player var2, Map<Integer, ItemStack> var3, List<String> var4, ItemStack var5, int var6, boolean var7) {
      this.page = var6;
      this.itemOrder = var4;

      for (int var8 = 0; var8 < (var7 ? this.size - 9 : this.size); var8++) {
         if (var3.containsKey(var8)) {
            this.inventory.setItem(var8, (ItemStack)var3.get(var8));
         } else if (var5 != null) {
            if (!this.fillItems.contains(var8)) {
               this.inventory.setItem(var8, var5);
            }
         } else {
            this.inventory.setItem(var8, new ItemStack(Material.AIR));
         }
      }

      var1.navBar.updateShopsNavBar(this.inventory, var2, this.section, var6, this.allPages);
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
