package com.onyxshop.versions.v1_17_R1;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.VersionHandler;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.AdventureUtil;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

public final class v1_17_R1 implements VersionHandler {
   @Override
   public boolean hasData(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (itemStack2.hasTag()) {
         NBTTagCompound nbtTagCompound = itemStack2.getTag();
         return nbtTagCompound.hasKey(itemId);
      } else {
         return false;
      }
   }

   @Override
   public Object emptyList() {
      return new NBTTagList();
   }

   @Override
   public String toNBT(String text) {
      return CraftChatMessage.fromStringToJSON(text, true);
   }

   @Override
   public ItemStack setDisplayname(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (itemStack2.hasTag()) {
         NBTTagCompound nbtTagCompound = itemStack2.getTag();
         if (!nbtTagCompound.hasKey("display")) {
            nbtTagCompound.set("display", new NBTTagCompound());
         }

         nbtTagCompound.getCompound("display").set("Name", NBTTagString.a(this.toNBT(itemId)));
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTDataAsString(ItemStack itemStack, boolean flag) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      if (itemStack2.hasTag() && !itemStack2.getTag().isEmpty()) {
         NBTTagCompound nbtTagCompound = itemStack2.getTag();
         if (!flag) {
            nbtTagCompound.remove("Enchantments");
            nbtTagCompound.remove("StoredEnchantments");
            nbtTagCompound.remove("Recipes");
            nbtTagCompound.remove("Fireworks");
            nbtTagCompound.remove("ONYX");
            nbtTagCompound.remove("SkullOwner");
         }

         return nbtTagCompound.toString().equals("{}") ? null : nbtTagCompound.toString();
      } else {
         return null;
      }
   }

   @Override
   public String getLoreAsNBT(ItemStack itemStack) {
      String lore = CraftItemStack.asNMSCopy(new ItemStack(itemStack)).getTag().getCompound("display").getList("Lore", 8).get(0).toString();
      return lore.substring(1).substring(0, lore.length() - 1);
   }

   @Override
   public Object getNMSLore(ItemStack itemStack) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (itemStack2.hasTag()) {
         NBTTagCompound nbtTagCompound = itemStack2.getTag();
         NBTTagCompound nbtTagCompound2 = nbtTagCompound.hasKey("display") ? nbtTagCompound.getCompound("display") : new NBTTagCompound();
         return nbtTagCompound2.hasKey("Lore") ? nbtTagCompound2.getList("Lore", 8) : new NBTTagList();
      } else {
         return new NBTTagList();
      }
   }

   @Override
   public ItemStack applyNMSLore(ItemStack itemStack, Object value) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (itemStack2.hasTag()) {
         NBTTagCompound nbtTagCompound = itemStack2.getTag();
         if (!nbtTagCompound.hasKey("display")) {
            nbtTagCompound.set("display", new NBTTagCompound());
         }

         nbtTagCompound.getCompound("display").set("Lore", (NBTTagList)value);
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public void setItemLore(Object value, String itemId, int value2) {
      ((NBTTagList)value).set(value2, NBTTagString.a(itemId));
   }

   @Override
   public void addItemLore(Object value, String itemId, int value2) {
      ((NBTTagList)value).add(value2, NBTTagString.a(itemId));
   }

   @Override
   public void addItemLore(Object value, String itemId) {
      ((NBTTagList)value).add(NBTTagString.a(itemId));
   }

   @Override
   public void updateInventoryTitle(Player player, Translatable text) {
      EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
      entityPlayer.b
         .sendPacket(
            new PacketPlayOutOpenWindow(
               entityPlayer.bV.j,
               entityPlayer.bV.getType(),
               ServerInfo.supportsPaper() ? (IChatBaseComponent)AdventureUtil.asVanilla(text) : CraftChatMessage.fromStringOrNull(text.getLegacy())
            )
         );
      player.updateInventory();
   }

   @Override
   public ItemStack setNBTString(ItemStack itemStack, String itemId, String text) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
      String[] values = itemId.split("::");
      NBTTagCompound nbtTagCompound2 = nbtTagCompound;

      for (int index = 0; index < values.length - 1; index++) {
         String text2 = values[index];
         NBTTagCompound nbtTagCompound3;
         if (nbtTagCompound2.hasKey(text2)) {
            nbtTagCompound3 = nbtTagCompound2.getCompound(text2);
         } else {
            nbtTagCompound3 = new NBTTagCompound();
            nbtTagCompound2.set(text2, nbtTagCompound3);
         }

         nbtTagCompound2 = nbtTagCompound3;
      }

      nbtTagCompound2.setString(values[values.length - 1], text);
      itemStack2.setTag(nbtTagCompound);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTString(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
      String[] values = itemId.split("::");

      for (int index = 0; index < values.length - 1; index++) {
         if (!nbtTagCompound.hasKey(values[index])) {
            return null;
         }

         nbtTagCompound = nbtTagCompound.getCompound(values[index]);
      }

      return nbtTagCompound.hasKey(values[values.length - 1]) ? nbtTagCompound.getString(values[values.length - 1]) : null;
   }

   @Override
   public ItemStack setNBTInt(ItemStack itemStack, String itemId, Integer value) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
      if (itemId.contains("::")) {
         NBTTagCompound nbtTagCompound2 = nbtTagCompound.hasKey(itemId.split("::")[0]) ? nbtTagCompound.getCompound(itemId.split("::")[0]) : new NBTTagCompound();
         nbtTagCompound2.setInt(itemId.split("::")[1], value);
         nbtTagCompound.set(itemId.split("::")[0], nbtTagCompound2);
      } else {
         nbtTagCompound.setInt(itemId, value);
      }

      itemStack2.setTag(nbtTagCompound);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public Integer getNBTInt(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
      return nbtTagCompound.hasKey(itemId) ? nbtTagCompound.getInt(itemId) : null;
   }

   @Override
   public ItemStack setPages(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString("Pages", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getPages(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.getString("Pages");
   }

   @Override
   public ItemStack setOnyxNbtString(ItemStack itemStack, String itemId, String text) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString(itemId, text);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getOnyxNbtString(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey(itemId) ? nbtTagCompound.getString(itemId) : null;
   }

   @Override
   public ItemStack setOnyxNbtInt(ItemStack itemStack, String itemId, Integer value) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setInt(itemId, value);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public Integer getOnyxNbtInt(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey(itemId) ? nbtTagCompound.getInt(itemId) : null;
   }

   @Override
   public ItemStack setIndex(ItemStack itemStack, Integer index) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setInt("Index", index);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public Integer getIndex(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey("Index") ? nbtTagCompound.getInt("Index") : -1;
   }

   @Override
   public ItemStack setAction(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString("Action", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getAction(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey("Action") ? nbtTagCompound.getString("Action") : null;
   }

   @Override
   public ItemStack setEditOption(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString("EditOption", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getEditOption(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey("EditOption") ? nbtTagCompound.getString("EditOption") : null;
   }

   @Override
   public ItemStack getShopAdminItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setBoolean("AdminItem", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean isShopAdminItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.getBoolean("AdminItem");
   }

   @Override
   public ItemStack addNBTtag(ItemStack itemStack, String itemId, boolean flag) throws ItemLoadException {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);

      try {
         NBTTagCompound nbtTagCompound = MojangsonParser.parse(itemId);
         if (flag && nbtTagCompound.hasKey("display")) {
            nbtTagCompound.remove("display");
         }

         NBTTagCompound nbtTagCompound2 = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
         nbtTagCompound2.a(nbtTagCompound);
         itemStack2.setTag(nbtTagCompound2);
         return CraftItemStack.asBukkitCopy(itemStack2);
      } catch (CommandSyntaxException exception) {
         StringWriter stringWriter = new StringWriter();
         PrintWriter printWriter = new PrintWriter(stringWriter);
         exception.printStackTrace(printWriter);
         SendMessage.logDebugMessage(stringWriter.toString());
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get());
      }
   }

   @Override
   public ItemStack addNBTCommands(ItemStack itemStack, List<String> itemIds, CommandType commandType) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagList nbtTagList = nbtTagCompound.hasKey(commandType.getCompound()) ? nbtTagCompound.getList(commandType.getCompound(), 8) : new NBTTagList();
      itemIds.forEach(entry -> nbtTagList.add(NBTTagString.a(entry.startsWith("/") ? entry.substring(1) : entry)));
      nbtTagCompound.set(commandType.getCompound(), nbtTagList);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean hasNBTCommand(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey("buy-commands") && !nbtTagCompound.getList("buy-commands", 8).isEmpty()
         ? true
         : nbtTagCompound.hasKey("sell-commands") && !nbtTagCompound.getList("sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(ItemStack itemStack, CommandType commandType) {
      NBTTagList nbtTagList = this.getOnyxCompound(itemStack).getList(commandType.getCompound(), 8);
      return nbtTagList.stream().<String>map(NBTBase::toString).collect(Collectors.toList());
   }

   private NBTTagCompound getOnyxCompound(ItemStack itemStack) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
      return nbtTagCompound.hasKey("ONYX") ? nbtTagCompound.getCompound("ONYX") : new NBTTagCompound();
   }

   private ItemStack setOnyxCompound(ItemStack itemStack, NBTTagCompound nbtTagCompound) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound2 = itemStack2.hasTag() ? itemStack2.getTag() : new NBTTagCompound();
      nbtTagCompound2.set("ONYX", nbtTagCompound);
      itemStack2.setTag(nbtTagCompound2);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getTitle(Inventory inventory) {
      return "";
   }

   @Override
   public ItemStack setDisabledBackButton(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setBoolean("disabledBackButton", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean isDisabledBackButton(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey("disabledBackButton") && nbtTagCompound.getBoolean("disabledBackButton");
   }

   @Override
   public ItemStack addNBTPrices(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = nbtTagCompound.hasKey("Prices") ? nbtTagCompound.getCompound("Prices") : new NBTTagCompound();
      nbtTagCompound2.setDouble("buy", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".buy"));
      nbtTagCompound2.setDouble("sell", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".sell"));
      nbtTagCompound.set("Prices", nbtTagCompound2);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public double getBuyPrice(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = nbtTagCompound.getCompound("Prices");
      return nbtTagCompound2.getDouble("buy");
   }

   @Override
   public double getSellPrice(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = nbtTagCompound.getCompound("Prices");
      return nbtTagCompound2.getDouble("sell");
   }

   @Override
   public ItemStack setItemErrorToItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setBoolean("ItemError", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public ItemStack getItemInHand(Player player) {
      return player.getInventory().getItemInMainHand();
   }

   @Override
   public ItemStack setSpawnerType(ItemStack itemStack, String type) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString("SpawnerType", type);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public ItemStack setPotionType(ItemStack itemStack, PotionTypes potionTypes) {
      PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
      if (potionMeta.getBasePotionData().getType().getEffectType() == null) {
         potionMeta.setBasePotionData(new PotionData(potionTypes.parsePotionType(), potionTypes.isExtended(), potionTypes.isUpgraded()));
      } else {
         if (!potionTypes.hasEffect()) {
            return itemStack;
         }

         potionMeta.addCustomEffect(new PotionEffect(potionTypes.parsePotionEffectType(), potionTypes.getDuration(), potionTypes.getAmplifier(), false, true, true), false);
      }

      itemStack.setItemMeta(potionMeta);
      return itemStack;
   }

   @Override
   public Optional<PotionTypes> getBasePotion(ItemStack itemStack) {
      PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
      if (potionMeta.getBasePotionData().getType().getEffectType() != null) {
         PotionData potionData = potionMeta.getBasePotionData();
         return PotionTypes.matchPotionDataLegacy(potionData.getType(), potionData.isExtended(), potionData.isUpgraded());
      } else {
         return Optional.empty();
      }
   }

   @Override
   public ItemStack setPathToItem(ItemStack itemStack, String path) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString("PathToItem", path);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public ItemStack getSpawnerToGive(EntityType entityType) {
      ItemStack itemStack = OnyxShop.getInstance().createItem.createSpawner(entityType);
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      nbtTagCompound.setString("SpawnerType", entityType.toString());
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getPathToItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.getString("PathToItem");
   }

   @Override
   public Boolean hasItemError(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return !nbtTagCompound.hasKey("ItemError") ? false : nbtTagCompound.getBoolean("ItemError");
   }

   @Override
   public String getSpawnerType(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return nbtTagCompound.hasKey("SpawnerType") && !nbtTagCompound.getString("SpawnerType").isEmpty() ? nbtTagCompound.getString("SpawnerType") : null;
   }

   @Override
   public boolean isSimilar(ItemStack itemStack, ItemStack itemStack2, List<String> itemIds) {
      net.minecraft.world.item.ItemStack itemStack3 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      net.minecraft.world.item.ItemStack itemStack4 = CraftItemStack.asNMSCopy(new ItemStack(itemStack2));
      if (!itemStack3.hasTag() && !itemStack4.hasTag()) {
         return itemStack.isSimilar(itemStack2);
      } else {
         for (String text : itemIds) {
            String[] values = text.split("::");
            if (values.length >= 2) {
               NBTTagCompound nbtTagCompound = itemStack3.getTag();
               if (nbtTagCompound != null) {
                  for (int index = 0; index < values.length; index++) {
                     nbtTagCompound = nbtTagCompound.getCompound(values[index]);
                     if (index == values.length - 2) {
                        nbtTagCompound.remove(values[index + 1]);
                     }
                  }
               }

               NBTTagCompound nbtTagCompound2 = itemStack4.getTag();
               if (nbtTagCompound2 != null) {
                  for (int index = 0; index < values.length; index++) {
                     nbtTagCompound2 = nbtTagCompound2.getCompound(values[index]);
                     if (index == values.length - 2) {
                        nbtTagCompound2.remove(values[index + 1]);
                     }
                  }
               }
            } else {
               itemStack3.removeTag(text);
               itemStack4.removeTag(text);
            }
         }

         return itemStack.getType().equals(Material.PLAYER_HEAD) && !itemIds.contains("SkullOwner") && !this.isSimilarSkull(itemStack3, itemStack4)
            ? false
            : CraftItemStack.asBukkitCopy(itemStack3).isSimilar(CraftItemStack.asBukkitCopy(itemStack4));
      }
   }

   private boolean isSimilarSkull(net.minecraft.world.item.ItemStack itemStack, net.minecraft.world.item.ItemStack itemStack2) {
      if (itemStack.hasTag() && itemStack2.hasTag()) {
         try {
            String text = null;
            String text2 = null;
            NBTTagList nbtTagList = itemStack.getTag().getCompound("SkullOwner").getCompound("Properties").getList("textures", 10);
            if (nbtTagList != null && !nbtTagList.isEmpty()) {
               for (int size = 0; size < nbtTagList.size(); size++) {
                  NBTTagCompound nbtTagCompound = nbtTagList.getCompound(size);
                  if (nbtTagCompound.hasKey("Value")) {
                     text = nbtTagCompound.getString("Value");
                  }
               }

               if (text == null) {
                  return false;
               }
            }

            NBTTagList nbtTagList2 = itemStack2.getTag().getCompound("SkullOwner").getCompound("Properties").getList("textures", 10);
            if (nbtTagList2 != null && !nbtTagList2.isEmpty()) {
               for (int index = 0; index < nbtTagList2.size(); index++) {
                  NBTTagCompound nbtTagCompound = nbtTagList2.getCompound(index);
                  if (nbtTagCompound.hasKey("Value")) {
                     text2 = nbtTagCompound.getString("Value");
                  }
               }

               if (text2 == null) {
                  return false;
               }
            }

            boolean flag = false;
            if (itemStack.getTag().getCompound("SkullOwner").hasKey("Name")
               && itemStack2.getTag().getCompound("SkullOwner").hasKey("Name")
               && itemStack.getTag().getCompound("SkullOwner").getString("Name").equals(itemStack2.getTag().getCompound("SkullOwner").getString("Name"))) {
               itemStack.getTag().remove("SkullOwner");
               itemStack2.getTag().remove("SkullOwner");
               flag = true;
            }

            return flag && text == null && text2 == null
               || text != null && text2 != null && (text.equals(text2) || SkullUtil.getTextureURL(text).equals(SkullUtil.getTextureURL(text2)));
         } catch (NullPointerException exception) {
            SendMessage.errorMessage("Error occurred while comparing skulls");
            exception.printStackTrace();
            return false;
         }
      } else {
         return false;
      }
   }
}
