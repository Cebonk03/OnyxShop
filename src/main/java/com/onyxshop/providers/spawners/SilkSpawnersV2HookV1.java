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
         File file = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/SilkSpawners_v2/config.yml");
         this.ssConfig = OnyxShop.getInstance().loadConfiguration(file, "SilkSpawners config");
         this.loadValues();
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to load custom spawner names from SilkSpawnersV2, using default...");
         this.customName = false;
         exception.printStackTrace();
      }

      return "SILKSPAWNERSV2";
   }

   private void loadValues() {
      this.itemName = ChatColor.translateAlternateColorCodes('$', this.ssConfig.getString("spawner.item.name"));
      this.itemLore = this.ssConfig
         .getStringList("spawner.item.lore")
         .stream()
         .map(element -> ChatColor.translateAlternateColorCodes('$', element))
         .collect(Collectors.toList());
      this.prefix = ChatColor.translateAlternateColorCodes('$', this.ssConfig.getString("spawner.item.prefix"));
      String itemId = this.ssConfig.contains("spawner.item.prefix-old") ? "spawner.item.prefix-old" : "spawner.item.prefixOld";
      this.oldPrefix = ChatColor.translateAlternateColorCodes('$', this.ssConfig.getString(itemId));
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      String type = null;
      List values = null;
      if (shopItem != null) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            type = itemMeta.getDisplayName();
         }

         if (itemMeta.hasLore()) {
            values = itemMeta.getLore();
         }
      }

      if (this.customName) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         itemMeta.setDisplayName(this.itemName);
         List<String> lores = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
         lores.add(0, this.serializedName(entityType));
         lores.addAll(1, this.itemLore);
         itemMeta.setLore((List)lores);
         itemStack.setItemMeta(itemMeta);
      }

      if (shopItem != null) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (type != null) {
            itemMeta.setDisplayName(type);
         }

         if (this.itemName == null || this.itemName.isEmpty()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
         }

         if (values != null) {
            itemMeta.setLore(values);
         }

         itemStack.setItemMeta(itemMeta);
      }

      ItemMeta itemMeta = itemStack.getItemMeta();
      if (shopItem == null) {
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
         CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
         creatureSpawner.setSpawnedType(entityType);
         blockStateMeta.setBlockState(creatureSpawner);
      }

      return itemStack;
   }

   private String serializedName(EntityType entityType) {
      return this.prefix + OnyxShop.getInstance().getMaterialName(entityType.name());
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "display::Name", "display::Lore", "BlockEntityTag");
      tags.addAll(shopIds);
      return this.getSpawnedType(itemStack).equals(this.getSpawnedType(itemStack2)) && OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      try {
         if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
            String text = (String)itemStack.getItemMeta().getLore().get(0);
            if (text.startsWith(this.prefix)) {
               return EntityType.fromName(text.replace(this.prefix, "").replace(" ", "_").toLowerCase()).name();
            }

            if (!this.oldPrefix.equals("") && text.startsWith(this.oldPrefix)) {
               return EntityType.fromName(text.replace(this.oldPrefix, "").replace(" ", "_").toLowerCase()).name();
            }
         }
      } catch (NullPointerException exception) {
      }

      ItemMeta itemMeta = itemStack.getItemMeta();
      BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
      CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
      return creatureSpawner.getSpawnedType().name();
   }
}
