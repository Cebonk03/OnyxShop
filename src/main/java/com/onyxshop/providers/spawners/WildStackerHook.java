package com.onyxshop.providers.spawners;

import com.bgsoftware.wildstacker.api.WildStacker;
import com.bgsoftware.wildstacker.api.upgrades.SpawnerUpgrade;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.NBTMaps;
import org.bukkit.ChatColor;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class WildStackerHook implements SpawnerProvider {
   private WildStacker ws;
   private FileConfiguration wsconfig;
   private List<SpawnerUpgrade> upgrades = new ArrayList<>();
   private final String itemName;
   private final List<String> itemLore;

   public WildStackerHook(WildStacker var1) {
      this.ws = var1;
      this.wsconfig = OnyxShop.getInstance()
         .loadConfiguration(new File(OnyxShop.getInstance().getDataFolder().getParent() + "/WildStacker/config.yml"), "WildStacker config");
      String var2 = this.wsconfig.getString("spawners.spawner-item.name", "Spawner");
      this.itemName = var2 != null
         ? ChatColor.translateAlternateColorCodes('&', var2.replace("{0}", String.valueOf(1)).replace("{1}", "%spawner-type%").replace("{2}", ""))
         : ChatColor.GOLD + "x1 &f&o%spawner-type% Spawners";
      List<String> var3 = this.wsconfig.getStringList("spawners.spawner-item.lore");
      this.itemLore = var3 != null
         ? var3.stream()
            .map(var0 -> ChatColor.translateAlternateColorCodes('&', var0).replace("{0}", String.valueOf(1)).replace("{1}", "%spawner-type%"))
            .collect(Collectors.toList())
         : Collections.singletonList(ChatColor.GRAY + "%spawner-type%");
      if (!this.upgrades.isEmpty()) {
         this.upgrades.clear();
      }

      ArrayList<Integer> var4 = new ArrayList<>();

      for (SpawnerUpgrade var6 : this.ws.getUpgradesManager().getAllUpgrades()) {
         var4.add(var6.getId());
      }

      Collections.sort(var4);

      for (int var8 : var4) {
         this.upgrades.add(this.ws.getUpgradesManager().getUpgrade(var8));
      }
   }

   @Override
   public String getProviderName() {
      return "WILDSTACKER";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      ItemMeta var4 = var2.getItemMeta();
      if (var1 == null) {
         BlockStateMeta var5 = (BlockStateMeta)var4;
         CreatureSpawner var6 = (CreatureSpawner)var5.getBlockState();
         var6.setSpawnedType(var3);
         var5.setBlockState(var6);
         var2 = OnyxShop.getInstance().versionHandler.setNBTInt(var2, "spawners-amount", 1);
      }

      String var7 = var4.hasDisplayName() ? var4.getDisplayName() : null;
      List var8 = var4.hasLore() ? var4.getLore() : null;
      if (var7 == null && this.itemName != null) {
         var4.setDisplayName(this.itemName.replace("%spawner-type%", this.getEntityName(var3)));
      }

      if (var7 == null && this.itemName == null) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(var4, SpawnerManager.getDefaultName(var3));
      }

      if (var8 == null && this.itemLore != null) {
         var4.setLore(this.itemLore.stream().map(var2x -> var2x.replace("%spawner-type%", this.getEntityName(var3))).collect(Collectors.toList()));
      }

      var2.setItemMeta(var4);
      return var2;
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "spawners-amount", "spawners-upgrade", "display::Name", "display::Lore");
      var4.addAll(var3);
      return OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return this.getSellPrice(var1, var2);
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      ItemMeta var2 = var1.getItemMeta();
      BlockStateMeta var3 = (BlockStateMeta)var2;
      CreatureSpawner var4 = (CreatureSpawner)var3.getBlockState();
      return var4.getSpawnedType().name();
   }

   private Double getSellPrice(ItemStack var1, double var2) {
      Integer var4 = OnyxShop.getInstance().versionHandler.getNBTInt(var1, NBTMaps.CUSTOM_DATA.getTag() + "spawners-amount");
      var4 = var4 == null ? 1 : var4;
      boolean var5 = this.wsconfig.getBoolean("spawners.spawner-upgrades.multiply-stack-amount", true);
      Integer var6 = OnyxShop.getInstance().versionHandler.getNBTInt(var1, NBTMaps.CUSTOM_DATA.getTag() + "spawners-upgrade");
      var6 = var6 != null && var6 != -1 ? var6 : 0;
      double var7 = this.getTotalUpgradeCost(var6);
      return var5 ? var4.intValue() * var7 + var4.intValue() * var2 : var7 + var4.intValue() * var2;
   }

   private double getTotalUpgradeCost(Integer var1) {
      if (var1 != null && var1 != 0) {
         double var2 = 0.0;

         for (SpawnerUpgrade var5 : this.upgrades) {
            var2 += var5.getCost();
            if (var5.getId() == var1) {
               break;
            }
         }

         return var2;
      } else {
         return 0.0;
      }
   }

   private String getEntityName(EntityType var1) {
      String[] var2 = var1.name().toLowerCase().replace("_", " ").split(" ");
      StringBuffer var3 = new StringBuffer();

      for (int var4 = 0; var4 < var2.length; var4++) {
         var3.append(Character.toUpperCase(var2[var4].charAt(0))).append(var2[var4].substring(1)).append(" ");
      }

      return var3.toString().trim();
   }
}
