package com.onyxshop.versions.v1_20_R2;

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
import com.onyxshop.util.JsonUtil;
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
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public final class v1_20_R2 implements VersionHandler {
   @Override
   public boolean hasData(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         NBTTagCompound nbtTagCompound = this.getC(itemStack2);
         return this.hasKey(nbtTagCompound, itemId);
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
      if (this.hasTag(itemStack2)) {
         NBTTagCompound nbtTagCompound = this.getC(itemStack2);
         if (!this.hasKey(nbtTagCompound, "display")) {
            this.set(nbtTagCompound, "display", new NBTTagCompound());
         }

         this.set(this.getOrCreate(nbtTagCompound, "display"), "Name", this.toNBT(itemId));
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTDataAsString(ItemStack itemStack, boolean flag) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      if (this.hasTag(itemStack2) && !this.getC(itemStack2).g()) {
         NBTTagCompound nbtTagCompound = this.getC(itemStack2);
         if (!flag) {
            this.remove(nbtTagCompound, "Enchantments");
            this.remove(nbtTagCompound, "StoredEnchantments");
            this.remove(nbtTagCompound, "Recipes");
            this.remove(nbtTagCompound, "Fireworks");
            this.remove(nbtTagCompound, "ONYX");
            this.remove(nbtTagCompound, "SkullOwner");
            this.remove(nbtTagCompound, "Trim");
            this.remove(nbtTagCompound, "instrument");
         }

         return nbtTagCompound.toString().equals("{}") ? null : nbtTagCompound.toString();
      } else {
         return null;
      }
   }

   @Override
   public String getLoreAsNBT(ItemStack itemStack) {
      String lore = ((NBTBase)CraftItemStack.asNMSCopy(new ItemStack(itemStack)).v().p("display").c("Lore", 8).get(0)).toString();
      return lore.substring(1).substring(0, lore.length() - 1);
   }

   @Override
   public Object getNMSLore(ItemStack itemStack) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         NBTTagCompound nbtTagCompound = this.getC(itemStack2);
         NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "display");
         return this.getOrCreate(nbtTagCompound2, "Lore", 8);
      } else {
         return new NBTTagList();
      }
   }

   @Override
   public ItemStack applyNMSLore(ItemStack itemStack, Object value) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         NBTTagCompound nbtTagCompound = this.getC(itemStack2);
         if (!this.hasKey(nbtTagCompound, "display")) {
            this.set(nbtTagCompound, "display", new NBTTagCompound());
         }

         this.set(this.getOrCreate(nbtTagCompound, "display"), "Lore", (NBTTagList)value);
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
      entityPlayer.c
         .b(
            new PacketPlayOutOpenWindow(
               entityPlayer.bS.j,
               entityPlayer.bS.a(),
               ServerInfo.supportsPaper() ? (IChatBaseComponent)AdventureUtil.asVanilla(text) : CraftChatMessage.fromStringOrNull(text.getLegacy())
            )
         );
      player.updateInventory();
   }

   @Override
   public ItemStack setNBTString(ItemStack itemStack, String itemId, String text) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = this.getOrCreate(itemStack2);
      String[] values = itemId.split("::");
      NBTTagCompound nbtTagCompound2 = nbtTagCompound;

      for (int index = 0; index < values.length - 1; index++) {
         String text2 = values[index];
         NBTTagCompound nbtTagCompound3;
         if (this.hasKey(nbtTagCompound2, text2)) {
            nbtTagCompound3 = this.getC(nbtTagCompound2, text2);
         } else {
            nbtTagCompound3 = new NBTTagCompound();
            this.set(nbtTagCompound2, text2, nbtTagCompound3);
         }

         nbtTagCompound2 = nbtTagCompound3;
      }

      this.set(nbtTagCompound2, values[values.length - 1], text);
      this.set(itemStack2, nbtTagCompound);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTString(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = this.getOrCreate(itemStack2);
      String[] values = itemId.split("::");

      for (int index = 0; index < values.length - 1; index++) {
         if (!this.hasKey(nbtTagCompound, values[index])) {
            return null;
         }

         nbtTagCompound = this.getC(nbtTagCompound, values[index]);
      }

      return this.hasKey(nbtTagCompound, values[values.length - 1]) ? this.getS(nbtTagCompound, values[values.length - 1]) : null;
   }

   @Override
   public ItemStack setNBTInt(ItemStack itemStack, String itemId, Integer value) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = this.getOrCreate(itemStack2);
      if (itemId.contains("::")) {
         NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, itemId.split("::")[0]);
         this.set(nbtTagCompound2, itemId.split("::")[1], value);
         this.set(nbtTagCompound, itemId.split("::")[0], nbtTagCompound2);
      } else {
         this.set(nbtTagCompound, itemId, value);
      }

      this.set(itemStack2, nbtTagCompound);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public Integer getNBTInt(ItemStack itemStack, String itemId) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = this.getOrCreate(itemStack2);
      return this.hasKey(nbtTagCompound, itemId) ? this.getI(nbtTagCompound, itemId) : null;
   }

   @Override
   public ItemStack setPages(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "Pages", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getPages(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.getS(nbtTagCompound, "Pages");
   }

   @Override
   public ItemStack setOnyxNbtString(ItemStack itemStack, String itemId, String text) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, itemId, text);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getOnyxNbtString(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, itemId) ? this.getS(nbtTagCompound, itemId) : null;
   }

   @Override
   public ItemStack setOnyxNbtInt(ItemStack itemStack, String itemId, Integer value) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, itemId, value);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public Integer getOnyxNbtInt(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, itemId) ? this.getI(nbtTagCompound, itemId) : null;
   }

   @Override
   public ItemStack setIndex(ItemStack itemStack, Integer index) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "Index", index);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public Integer getIndex(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "Index") ? this.getI(nbtTagCompound, "Index") : -1;
   }

   @Override
   public ItemStack setAction(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "Action", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getAction(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "Action") ? this.getS(nbtTagCompound, "Action") : null;
   }

   @Override
   public ItemStack setEditOption(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "EditOption", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getEditOption(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "EditOption") ? this.getS(nbtTagCompound, "EditOption") : null;
   }

   @Override
   public ItemStack getShopAdminItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "AdminItem", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean isShopAdminItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.getB(nbtTagCompound, "AdminItem");
   }

   @Override
   public ItemStack addNBTtag(ItemStack itemStack, String itemId, boolean flag) throws ItemLoadException {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);

      try {
         NBTTagCompound nbtTagCompound = MojangsonParser.a(itemId);
         if (flag && this.hasKey(nbtTagCompound, "display")) {
            this.remove(nbtTagCompound, "display");
         }

         NBTTagCompound nbtTagCompound2 = this.getOrCreate(itemStack2);
         this.set(nbtTagCompound2, nbtTagCompound);
         this.set(itemStack2, nbtTagCompound2);
         return CraftItemStack.asBukkitCopy(itemStack2);
      } catch (Exception exception) {
         StringWriter stringWriter = new StringWriter();
         PrintWriter printWriter = new PrintWriter(stringWriter);
         exception.printStackTrace(printWriter);
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get().getLegacy() + ": " + stringWriter.toString());
      }
   }

   @Override
   public ItemStack addNBTCommands(ItemStack itemStack, List<String> itemIds, CommandType commandType) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagList nbtTagList = this.getOrCreate(nbtTagCompound, commandType.getCompound(), 8);
      itemIds.forEach(entry -> nbtTagList.add(NBTTagString.a(entry.startsWith("/") ? entry.substring(1) : entry)));
      this.set(nbtTagCompound, commandType.getCompound(), nbtTagList);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean hasNBTCommand(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return !this.getOrCreate(nbtTagCompound, "buy-commands", 8).isEmpty() || !this.getOrCreate(nbtTagCompound, "sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(ItemStack itemStack, CommandType commandType) {
      NBTTagList nbtTagList = this.getOrCreate(this.getOnyxCompound(itemStack), commandType.getCompound(), 8);
      return nbtTagList.stream().<String>map(NBTBase::toString).collect(Collectors.toList());
   }

   private NBTTagCompound getC(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.p(text);
   }

   private NBTTagCompound getC(NBTTagList nbtTagList, int value) {
      return nbtTagList.a(value);
   }

   private NBTTagCompound getC(net.minecraft.world.item.ItemStack itemStack) {
      return itemStack.v();
   }

   private NBTTagList getL(NBTTagCompound nbtTagCompound, String text, int value) {
      return nbtTagCompound.c(text, value);
   }

   private String getS(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.l(text);
   }

   private double getD(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.k(text);
   }

   private boolean getB(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.q(text);
   }

   private int getI(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.h(text);
   }

   private NBTTagCompound getOrCreate(net.minecraft.world.item.ItemStack itemStack) {
      return itemStack.u() ? itemStack.v() : new NBTTagCompound();
   }

   private NBTTagCompound getOrCreate(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.e(text) ? nbtTagCompound.p(text) : new NBTTagCompound();
   }

   private NBTTagList getOrCreate(NBTTagCompound nbtTagCompound, String text, int value) {
      return nbtTagCompound.e(text) ? nbtTagCompound.c(text, value) : new NBTTagList();
   }

   private boolean hasKey(NBTTagCompound nbtTagCompound, String key) {
      return nbtTagCompound.e(key);
   }

   private boolean hasTag(net.minecraft.world.item.ItemStack itemStack) {
      return itemStack.u();
   }

   private String getString(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.l(text);
   }

   private void setString(NBTTagCompound nbtTagCompound, String text, String text2) {
      nbtTagCompound.a(text, text2);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, NBTTagList nbtTagList) {
      nbtTagCompound.a(text, nbtTagList);
   }

   private void remove(NBTTagCompound nbtTagCompound, String text) {
      nbtTagCompound.r(text);
   }

   private void remove(net.minecraft.world.item.ItemStack itemStack, String worldName) {
      itemStack.c(worldName);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, boolean flag) {
      nbtTagCompound.a(text, flag);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, double amount) {
      nbtTagCompound.a(text, amount);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, NBTTagCompound nbtTagCompound2) {
      nbtTagCompound.a(text, nbtTagCompound2);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, String text2) {
      nbtTagCompound.a(text, text2);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, Integer value) {
      nbtTagCompound.a(text, value);
   }

   private void set(NBTTagCompound nbtTagCompound, NBTTagCompound nbtTagCompound2) {
      nbtTagCompound.a(nbtTagCompound2);
   }

   private void set(net.minecraft.world.item.ItemStack itemStack, NBTTagCompound nbtTagCompound) {
      itemStack.c(nbtTagCompound);
   }

   private NBTTagCompound getOnyxCompound(ItemStack itemStack) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = this.getOrCreate(itemStack2);
      return this.getOrCreate(nbtTagCompound, "ONYX");
   }

   private ItemStack setOnyxCompound(ItemStack itemStack, NBTTagCompound nbtTagCompound) {
      net.minecraft.world.item.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(itemStack2);
      this.set(nbtTagCompound2, "ONYX", nbtTagCompound);
      this.set(itemStack2, nbtTagCompound2);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getTitle(Inventory inventory) {
      return "";
   }

   @Override
   public ItemStack setDisabledBackButton(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "disabledBackButton", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean isDisabledBackButton(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "disabledBackButton") && this.getB(nbtTagCompound, "disabledBackButton");
   }

   @Override
   public ItemStack addNBTPrices(ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "Prices");
      this.set(nbtTagCompound2, "buy", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".buy"));
      this.set(nbtTagCompound2, "sell", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".sell"));
      this.set(nbtTagCompound, "Prices", nbtTagCompound2);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public double getBuyPrice(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "Prices");
      return this.getD(nbtTagCompound2, "buy");
   }

   @Override
   public double getSellPrice(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "Prices");
      return this.getD(nbtTagCompound2, "sell");
   }

   @Override
   public ItemStack setItemErrorToItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "ItemError", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public ItemStack getItemInHand(Player player) {
      return player.getInventory().getItemInMainHand();
   }

   @Override
   public ItemStack setSpawnerType(ItemStack itemStack, String type) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "SpawnerType", type);
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
      this.set(nbtTagCompound, "PathToItem", path);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public ItemStack getSpawnerToGive(EntityType entityType) {
      ItemStack itemStack = OnyxShop.getInstance().createItem.createSpawner(entityType);
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "SpawnerType", entityType.toString());
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getPathToItem(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.getS(nbtTagCompound, "PathToItem");
   }

   @Override
   public Boolean hasItemError(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return !this.hasKey(nbtTagCompound, "ItemError") ? false : this.getB(nbtTagCompound, "ItemError");
   }

   @Override
   public String getSpawnerType(ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "SpawnerType") && !this.getS(nbtTagCompound, "SpawnerType").isEmpty() ? this.getS(nbtTagCompound, "SpawnerType") : null;
   }

   private NBTBase get(NBTBase nbtBase, String text) {
      switch (nbtBase.b()) {
         case 9:
            return (NBTBase)((NBTTagList)nbtBase).get(0);
         case 10:
            return ((NBTTagCompound)nbtBase).c(text);
         default:
            return null;
      }
   }

   private void remove(NBTTagCompound nbtTagCompound, String text, String text2) {
      try {
         NBTBase nbtBase = nbtTagCompound.c(text);
         if (nbtBase == null) {
            return;
         }

         switch (nbtBase.b()) {
            case 8:
               JSONObject jsonObject = JsonUtil.parseJson(this.strip(nbtBase.toString()));
               jsonObject.remove(text2);
               this.set(nbtTagCompound, text, jsonObject.toJSONString());
               break;
            case 9:
               ((NBTTagList)nbtBase).remove(0);
               break;
            case 10:
               this.remove((NBTTagCompound)nbtBase, text2);
         }
      } catch (ParseException exception) {
         exception.printStackTrace();
      }
   }

   private String strip(String text) {
      if (text.startsWith("'") && text.endsWith("'")) {
         text = text.substring(1, text.length() - 1);
      }

      if (text.startsWith("\"") && text.endsWith("\"")) {
         text = text.substring(1, text.length() - 1);
      }

      return text;
   }

   @Override
   public boolean isSimilar(ItemStack itemStack, ItemStack itemStack2, List<String> itemIds) {
      net.minecraft.world.item.ItemStack itemStack3 = CraftItemStack.asNMSCopy(new ItemStack(itemStack));
      net.minecraft.world.item.ItemStack itemStack4 = CraftItemStack.asNMSCopy(new ItemStack(itemStack2));
      if (!this.hasTag(itemStack3) && !this.hasTag(itemStack4)) {
         return itemStack.isSimilar(itemStack2);
      } else {
         for (String text : itemIds) {
            String[] values = text.split("::");
            if (values.length >= 2) {
               NBTTagCompound nbtTagCompound = this.getC(itemStack3);
               if (nbtTagCompound != null) {
                  for (int index = 0; index < values.length - 1; index++) {
                     if (index == values.length - 2) {
                        this.remove(nbtTagCompound, values[index], values[index + 1]);
                        break;
                     }

                     nbtTagCompound = this.getC(nbtTagCompound, values[index]);
                  }
               }

               NBTTagCompound nbtTagCompound2 = this.getC(itemStack4);
               if (nbtTagCompound2 != null) {
                  for (int index = 0; index < values.length - 1; index++) {
                     if (index == values.length - 2) {
                        this.remove(nbtTagCompound2, values[index], values[index + 1]);
                        break;
                     }

                     nbtTagCompound2 = this.getC(nbtTagCompound2, values[index]);
                  }
               }
            } else {
               this.remove(itemStack3, text);
               this.remove(itemStack4, text);
            }
         }

         return itemStack.getType().equals(Material.PLAYER_HEAD) && !itemIds.contains("SkullOwner") && !this.isSimilarSkull(itemStack3, itemStack4)
            ? false
            : CraftItemStack.asBukkitCopy(itemStack3).isSimilar(CraftItemStack.asBukkitCopy(itemStack4));
      }
   }

   private boolean isSimilarSkull(net.minecraft.world.item.ItemStack itemStack, net.minecraft.world.item.ItemStack itemStack2) {
      if (this.hasTag(itemStack) && this.hasTag(itemStack2)) {
         try {
            String text = null;
            String text2 = null;
            NBTTagList nbtTagList = this.getL(this.getC(this.getC(this.getC(itemStack), "SkullOwner"), "Properties"), "textures", 10);
            if (nbtTagList != null && !nbtTagList.isEmpty()) {
               for (int size = 0; size < nbtTagList.size(); size++) {
                  NBTTagCompound nbtTagCompound = this.getC(nbtTagList, size);
                  if (this.hasKey(nbtTagCompound, "Value")) {
                     text = this.getS(nbtTagCompound, "Value");
                  }
               }

               if (text == null) {
                  return false;
               }
            }

            NBTTagList nbtTagList2 = this.getL(this.getC(this.getC(this.getC(itemStack2), "SkullOwner"), "Properties"), "textures", 10);
            if (nbtTagList2 != null && !nbtTagList2.isEmpty()) {
               for (int index = 0; index < nbtTagList2.size(); index++) {
                  NBTTagCompound nbtTagCompound = this.getC(nbtTagList2, index);
                  if (this.hasKey(nbtTagCompound, "Value")) {
                     text2 = this.getS(nbtTagCompound, "Value");
                  }
               }

               if (text2 == null) {
                  return false;
               }
            }

            boolean flag = false;
            if (this.hasKey(this.getC(this.getC(itemStack), "SkullOwner"), "Name")
               && this.hasKey(this.getC(this.getC(itemStack2), "SkullOwner"), "Name")
               && this.getS(this.getC(this.getC(itemStack), "SkullOwner"), "Name").equalsIgnoreCase(this.getS(this.getC(this.getC(itemStack2), "SkullOwner"), "Name"))) {
               this.remove(this.getC(itemStack), "SkullOwner");
               this.remove(this.getC(itemStack2), "SkullOwner");
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
