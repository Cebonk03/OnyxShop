package com.onyxshop.providers.spawners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
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

public class SilkSpawnersV2HookV1 implements SpawnerProvider {
   private FileConfiguration ssConfig;
   private boolean customName = true;
   private String itemName = ChatColor.LIGHT_PURPLE + "Spawner";
   private List<String> itemLore = new ArrayList<>();
   private String prefix = ChatColor.YELLOW + "";
   private String oldPrefix = "";

   public SilkSpawnersV2HookV1() {
   }

   @Override
   public String getProviderName() {
      try {
         File var1 = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/SilkSpawners_v2/config.yml");
         this.ssConfig = OnyxShop.getInstance().loadConfiguration(var1, "SilkSpawners config");
         this.loadValues();
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to load custom spawner names from SilkSpawnersV2, using default...");
         this.customName = false;
         var2.printStackTrace();
      }

      return "SILKSPAWNERSV2";
   }

   private void loadValues() {
      this.itemName = ChatColor.translateAlternateColorCodes('$', this.ssConfig.getString("spawner.item.name"));
      this.itemLore = this.ssConfig
         .getStringList("spawner.item.lore")
         .stream()
         .map(var0 -> ChatColor.translateAlternateColorCodes('$', var0))
         .collect(Collectors.toList());
      this.prefix = ChatColor.translateAlternateColorCodes('$', this.ssConfig.getString("spawner.item.prefix"));
      String var1 = this.ssConfig.contains("spawner.item.prefix-old") ? "spawner.item.prefix-old" : "spawner.item.prefixOld";
      this.oldPrefix = ChatColor.translateAlternateColorCodes('$', this.ssConfig.getString(var1));
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      String var4 = null;
      List var5 = null;
      if (var1 != null) {
         ItemMeta var6 = var2.getItemMeta();
         if (var6.hasDisplayName()) {
            var4 = var6.getDisplayName();
         }

         if (var6.hasLore()) {
            var5 = var6.getLore();
         }
      }

      if (this.customName) {
         ItemMeta var9 = var2.getItemMeta();
         var9.setDisplayName(this.itemName);
         List<String> var7 = var9.hasLore() ? var9.getLore() : new ArrayList<>();
         var7.add(0, this.serializedName(var3));
         var7.addAll(1, this.itemLore);
         var9.setLore((List)var7);
         var2.setItemMeta(var9);
      }

      if (var1 != null) {
         ItemMeta var10 = var2.getItemMeta();
         if (var4 != null) {
            var10.setDisplayName(var4);
         }

         if (this.itemName == null || this.itemName.isEmpty()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(var10, SpawnerManager.getDefaultName(var3));
         }

         if (var5 != null) {
            var10.setLore(var5);
         }

         var2.setItemMeta(var10);
      }

      ItemMeta var11 = var2.getItemMeta();
      if (var1 == null) {
         BlockStateMeta var12 = (BlockStateMeta)var11;
         CreatureSpawner var8 = (CreatureSpawner)var12.getBlockState();
         var8.setSpawnedType(var3);
         var12.setBlockState(var8);
      }

      return var2;
   }

   private String serializedName(EntityType var1) {
      return this.prefix + OnyxShop.getInstance().getMaterialName(var1.name());
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "display::Name", "display::Lore", "BlockEntityTag");
      var4.addAll(var3);
      return this.getSpawnedType(var1).equals(this.getSpawnedType(var2)) && OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      try {
         if (var1.hasItemMeta() && var1.getItemMeta().hasLore()) {
            String var2 = (String)var1.getItemMeta().getLore().get(0);
            if (var2.startsWith(this.prefix)) {
               return EntityType.fromName(var2.replace(this.prefix, "").replace(" ", "_").toLowerCase()).name();
            }

            if (!this.oldPrefix.equals("") && var2.startsWith(this.oldPrefix)) {
               return EntityType.fromName(var2.replace(this.oldPrefix, "").replace(" ", "_").toLowerCase()).name();
            }
         }
      } catch (NullPointerException var5) {
      }

      ItemMeta var6 = var1.getItemMeta();
      BlockStateMeta var3 = (BlockStateMeta)var6;
      CreatureSpawner var4 = (CreatureSpawner)var3.getBlockState();
      return var4.getSpawnedType().name();
   }
}
