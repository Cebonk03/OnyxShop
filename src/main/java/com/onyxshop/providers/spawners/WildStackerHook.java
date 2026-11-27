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

   public WildStackerHook(WildStacker wildStacker) {
      this.ws = wildStacker;
      this.wsconfig = OnyxShop.getInstance()
         .loadConfiguration(new File(OnyxShop.getInstance().getDataFolder().getParent() + "/WildStacker/config.yml"), "WildStacker config");
      String text = this.wsconfig.getString("spawners.spawner-item.name", "Spawner");
      this.itemName = text != null
         ? ChatColor.translateAlternateColorCodes('&', text.replace("{0}", String.valueOf(1)).replace("{1}", "%spawner-type%").replace("{2}", ""))
         : ChatColor.GOLD + "x1 &f&o%spawner-type% Spawners";
      List<String> stringList = this.wsconfig.getStringList("spawners.spawner-item.lore");
      this.itemLore = stringList != null
         ? stringList.stream()
            .map(element -> ChatColor.translateAlternateColorCodes('&', element).replace("{0}", String.valueOf(1)).replace("{1}", "%spawner-type%"))
            .collect(Collectors.toList())
         : Collections.singletonList(ChatColor.GRAY + "%spawner-type%");
      if (!this.upgrades.isEmpty()) {
         this.upgrades.clear();
      }

      ArrayList<Integer> values = new ArrayList<>();

      for (SpawnerUpgrade spawnerUpgrade : this.ws.getUpgradesManager().getAllUpgrades()) {
         values.add(spawnerUpgrade.getId());
      }

      Collections.sort(values);

      for (int value : values) {
         this.upgrades.add(this.ws.getUpgradesManager().getUpgrade(value));
      }
   }

   @Override
   public String getProviderName() {
      return "WILDSTACKER";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (shopItem == null) {
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
         CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
         creatureSpawner.setSpawnedType(entityType);
         blockStateMeta.setBlockState(creatureSpawner);
         itemStack = OnyxShop.getInstance().versionHandler.setNBTInt(itemStack, "spawners-amount", 1);
      }

      String type = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : null;
      List lores = itemMeta.hasLore() ? itemMeta.getLore() : null;
      if (type == null && this.itemName != null) {
         itemMeta.setDisplayName(this.itemName.replace("%spawner-type%", this.getEntityName(entityType)));
      }

      if (type == null && this.itemName == null) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      if (lores == null && this.itemLore != null) {
         itemMeta.setLore(this.itemLore.stream().map(element -> element.replace("%spawner-type%", this.getEntityName(entityType))).collect(Collectors.toList()));
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "spawners-amount", "spawners-upgrade", "display::Name", "display::Lore");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return this.getSellPrice(itemStack, price);
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
      CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
      return creatureSpawner.getSpawnedType().name();
   }

   private Double getSellPrice(ItemStack itemStack, double price) {
      Integer nbtInt = OnyxShop.getInstance().versionHandler.getNBTInt(itemStack, NBTMaps.CUSTOM_DATA.getTag() + "spawners-amount");
      nbtInt = nbtInt == null ? 1 : nbtInt;
      boolean flag = this.wsconfig.getBoolean("spawners.spawner-upgrades.multiply-stack-amount", true);
      Integer nbtInt2 = OnyxShop.getInstance().versionHandler.getNBTInt(itemStack, NBTMaps.CUSTOM_DATA.getTag() + "spawners-upgrade");
      nbtInt2 = nbtInt2 != null && nbtInt2 != -1 ? nbtInt2 : 0;
      double totalUpgradeCost = this.getTotalUpgradeCost(nbtInt2);
      return flag ? nbtInt.intValue() * totalUpgradeCost + nbtInt.intValue() * price : totalUpgradeCost + nbtInt.intValue() * price;
   }

   private double getTotalUpgradeCost(Integer value) {
      if (value != null && value != 0) {
         double amount = 0.0;

         for (SpawnerUpgrade spawnerUpgrade : this.upgrades) {
            amount += spawnerUpgrade.getCost();
            if (spawnerUpgrade.getId() == value) {
               break;
            }
         }

         return amount;
      } else {
         return 0.0;
      }
   }

   private String getEntityName(EntityType entityType) {
      String[] names = entityType.name().toLowerCase().replace("_", " ").split(" ");
      StringBuffer stringBuffer = new StringBuffer();

      for (int index = 0; index < names.length; index++) {
         stringBuffer.append(Character.toUpperCase(names[index].charAt(0))).append(names[index].substring(1)).append(" ");
      }

      return stringBuffer.toString().trim();
   }
}
