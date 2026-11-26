package com.onyxshop.versions.v1_20_R1;

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
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public final class v1_20_R1 implements VersionHandler {
   @Override
   public boolean hasData(ItemStack var1, String var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (this.hasTag(var3)) {
         NBTTagCompound var4 = this.getC(var3);
         return this.hasKey(var4, var2);
      } else {
         return false;
      }
   }

   @Override
   public Object emptyList() {
      return new NBTTagList();
   }

   @Override
   public String toNBT(String var1) {
      return CraftChatMessage.fromStringToJSON(var1, true);
   }

   @Override
   public ItemStack setDisplayname(ItemStack var1, String var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (this.hasTag(var3)) {
         NBTTagCompound var4 = this.getC(var3);
         if (!this.hasKey(var4, "display")) {
            this.set(var4, "display", new NBTTagCompound());
         }

         this.set(this.getOrCreate(var4, "display"), "Name", this.toNBT(var2));
      }

      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public String getNBTDataAsString(ItemStack var1, boolean var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      if (this.hasTag(var3) && !this.getC(var3).g()) {
         NBTTagCompound var4 = this.getC(var3);
         if (!var2) {
            this.remove(var4, "Enchantments");
            this.remove(var4, "StoredEnchantments");
            this.remove(var4, "Recipes");
            this.remove(var4, "Fireworks");
            this.remove(var4, "ONYX");
            this.remove(var4, "SkullOwner");
            this.remove(var4, "Trim");
            this.remove(var4, "instrument");
         }

         return var4.toString().equals("{}") ? null : var4.toString();
      } else {
         return null;
      }
   }

   @Override
   public String getLoreAsNBT(ItemStack var1) {
      String var2 = ((NBTBase)CraftItemStack.asNMSCopy(new ItemStack(var1)).v().p("display").c("Lore", 8).get(0)).toString();
      return var2.substring(1).substring(0, var2.length() - 1);
   }

   @Override
   public Object getNMSLore(ItemStack var1) {
      net.minecraft.world.item.ItemStack var2 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (this.hasTag(var2)) {
         NBTTagCompound var3 = this.getC(var2);
         NBTTagCompound var4 = this.getOrCreate(var3, "display");
         return this.getOrCreate(var4, "Lore", 8);
      } else {
         return new NBTTagList();
      }
   }

   @Override
   public ItemStack applyNMSLore(ItemStack var1, Object var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (this.hasTag(var3)) {
         NBTTagCompound var4 = this.getC(var3);
         if (!this.hasKey(var4, "display")) {
            this.set(var4, "display", new NBTTagCompound());
         }

         this.set(this.getOrCreate(var4, "display"), "Lore", (NBTTagList)var2);
      }

      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public void setItemLore(Object var1, String var2, int var3) {
      ((NBTTagList)var1).set(var3, NBTTagString.a(var2));
   }

   @Override
   public void addItemLore(Object var1, String var2, int var3) {
      ((NBTTagList)var1).add(var3, NBTTagString.a(var2));
   }

   @Override
   public void addItemLore(Object var1, String var2) {
      ((NBTTagList)var1).add(NBTTagString.a(var2));
   }

   @Override
   public void updateInventoryTitle(Player var1, Translatable var2) {
      EntityPlayer var3 = ((CraftPlayer)var1).getHandle();
      var3.c
         .a(
            new PacketPlayOutOpenWindow(
               var3.bR.j,
               var3.bR.a(),
               ServerInfo.supportsPaper() ? (IChatBaseComponent)AdventureUtil.asVanilla(var2) : CraftChatMessage.fromStringOrNull(var2.getLegacy())
            )
         );
      var1.updateInventory();
   }

   @Override
   public ItemStack setNBTString(ItemStack var1, String var2, String var3) {
      net.minecraft.world.item.ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var5 = this.getOrCreate(var4);
      String[] var6 = var2.split("::");
      NBTTagCompound var7 = var5;

      for (int var8 = 0; var8 < var6.length - 1; var8++) {
         String var9 = var6[var8];
         NBTTagCompound var10;
         if (this.hasKey(var7, var9)) {
            var10 = this.getC(var7, var9);
         } else {
            var10 = new NBTTagCompound();
            this.set(var7, var9, var10);
         }

         var7 = var10;
      }

      this.set(var7, var6[var6.length - 1], var3);
      this.set(var4, var5);
      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public String getNBTString(ItemStack var1, String var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var4 = this.getOrCreate(var3);
      String[] var5 = var2.split("::");

      for (int var6 = 0; var6 < var5.length - 1; var6++) {
         if (!this.hasKey(var4, var5[var6])) {
            return null;
         }

         var4 = this.getC(var4, var5[var6]);
      }

      return this.hasKey(var4, var5[var5.length - 1]) ? this.getS(var4, var5[var5.length - 1]) : null;
   }

   @Override
   public ItemStack setNBTInt(ItemStack var1, String var2, Integer var3) {
      net.minecraft.world.item.ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var5 = this.getOrCreate(var4);
      if (var2.contains("::")) {
         NBTTagCompound var6 = this.getOrCreate(var5, var2.split("::")[0]);
         this.set(var6, var2.split("::")[1], var3);
         this.set(var5, var2.split("::")[0], var6);
      } else {
         this.set(var5, var2, var3);
      }

      this.set(var4, var5);
      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public Integer getNBTInt(ItemStack var1, String var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var4 = this.getOrCreate(var3);
      return this.hasKey(var4, var2) ? this.getI(var4, var2) : null;
   }

   @Override
   public ItemStack setPages(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "Pages", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getPages(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.getS(var2, "Pages");
   }

   @Override
   public ItemStack setOnyxNbtString(ItemStack var1, String var2, String var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      this.set(var4, var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public String getOnyxNbtString(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      return this.hasKey(var3, var2) ? this.getS(var3, var2) : null;
   }

   @Override
   public ItemStack setOnyxNbtInt(ItemStack var1, String var2, Integer var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      this.set(var4, var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public Integer getOnyxNbtInt(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      return this.hasKey(var3, var2) ? this.getI(var3, var2) : null;
   }

   @Override
   public ItemStack setIndex(ItemStack var1, Integer var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "Index", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public Integer getIndex(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "Index") ? this.getI(var2, "Index") : -1;
   }

   @Override
   public ItemStack setAction(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "Action", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getAction(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "Action") ? this.getS(var2, "Action") : null;
   }

   @Override
   public ItemStack setEditOption(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "EditOption", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getEditOption(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "EditOption") ? this.getS(var2, "EditOption") : null;
   }

   @Override
   public ItemStack getShopAdminItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      this.set(var2, "AdminItem", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isShopAdminItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.getB(var2, "AdminItem");
   }

   @Override
   public ItemStack addNBTtag(ItemStack var1, String var2, boolean var3) throws ItemLoadException {
      net.minecraft.world.item.ItemStack var4 = CraftItemStack.asNMSCopy(var1);

      try {
         NBTTagCompound var5 = MojangsonParser.a(var2);
         if (var3 && this.hasKey(var5, "display")) {
            this.remove(var5, "display");
         }

         NBTTagCompound var9 = this.getOrCreate(var4);
         this.set(var9, var5);
         this.set(var4, var9);
         return CraftItemStack.asBukkitCopy(var4);
      } catch (Exception var8) {
         StringWriter var6 = new StringWriter();
         PrintWriter var7 = new PrintWriter(var6);
         var8.printStackTrace(var7);
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get().getLegacy() + ": " + var6.toString());
      }
   }

   @Override
   public ItemStack addNBTCommands(ItemStack var1, List<String> var2, CommandType var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      NBTTagList var5 = this.getOrCreate(var4, var3.getCompound(), 8);
      var2.forEach(var1x -> var5.add(NBTTagString.a(var1x.startsWith("/") ? var1x.substring(1) : var1x)));
      this.set(var4, var3.getCompound(), var5);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public boolean hasNBTCommand(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return !this.getOrCreate(var2, "buy-commands", 8).isEmpty() || !this.getOrCreate(var2, "sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(ItemStack var1, CommandType var2) {
      NBTTagList var3 = this.getOrCreate(this.getOnyxCompound(var1), var2.getCompound(), 8);
      return var3.stream().<String>map(NBTBase::toString).collect(Collectors.toList());
   }

   private NBTTagCompound getC(NBTTagCompound var1, String var2) {
      return var1.p(var2);
   }

   private NBTTagCompound getC(NBTTagList var1, int var2) {
      return var1.a(var2);
   }

   private NBTTagCompound getC(net.minecraft.world.item.ItemStack var1) {
      return var1.v();
   }

   private NBTTagList getL(NBTTagCompound var1, String var2, int var3) {
      return var1.c(var2, var3);
   }

   private String getS(NBTTagCompound var1, String var2) {
      return var1.l(var2);
   }

   private double getD(NBTTagCompound var1, String var2) {
      return var1.k(var2);
   }

   private boolean getB(NBTTagCompound var1, String var2) {
      return var1.q(var2);
   }

   private int getI(NBTTagCompound var1, String var2) {
      return var1.h(var2);
   }

   private NBTTagCompound getOrCreate(net.minecraft.world.item.ItemStack var1) {
      return var1.u() ? var1.v() : new NBTTagCompound();
   }

   private NBTTagCompound getOrCreate(NBTTagCompound var1, String var2) {
      return var1.e(var2) ? var1.p(var2) : new NBTTagCompound();
   }

   private NBTTagList getOrCreate(NBTTagCompound var1, String var2, int var3) {
      return var1.e(var2) ? var1.c(var2, var3) : new NBTTagList();
   }

   private boolean hasKey(NBTTagCompound var1, String var2) {
      return var1.e(var2);
   }

   private boolean hasTag(net.minecraft.world.item.ItemStack var1) {
      return var1.u();
   }

   private String getString(NBTTagCompound var1, String var2) {
      return var1.l(var2);
   }

   private void setString(NBTTagCompound var1, String var2, String var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, NBTTagList var3) {
      var1.a(var2, var3);
   }

   private void remove(NBTTagCompound var1, String var2) {
      var1.r(var2);
   }

   private void remove(net.minecraft.world.item.ItemStack var1, String var2) {
      var1.c(var2);
   }

   private void set(NBTTagCompound var1, String var2, boolean var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, double var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, NBTTagCompound var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, String var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, Integer var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, NBTTagCompound var2) {
      var1.a(var2);
   }

   private void set(net.minecraft.world.item.ItemStack var1, NBTTagCompound var2) {
      var1.c(var2);
   }

   private NBTTagCompound getOnyxCompound(ItemStack var1) {
      net.minecraft.world.item.ItemStack var2 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var3 = this.getOrCreate(var2);
      return this.getOrCreate(var3, "ONYX");
   }

   private ItemStack setOnyxCompound(ItemStack var1, NBTTagCompound var2) {
      net.minecraft.world.item.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var4 = this.getOrCreate(var3);
      this.set(var4, "ONYX", var2);
      this.set(var3, var4);
      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public String getTitle(Inventory var1) {
      return "";
   }

   @Override
   public ItemStack setDisabledBackButton(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      this.set(var2, "disabledBackButton", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isDisabledBackButton(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "disabledBackButton") && this.getB(var2, "disabledBackButton");
   }

   @Override
   public ItemStack addNBTPrices(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      NBTTagCompound var4 = this.getOrCreate(var3, "Prices");
      this.set(var4, "buy", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".buy"));
      this.set(var4, "sell", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".sell"));
      this.set(var3, "Prices", var4);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public double getBuyPrice(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      NBTTagCompound var3 = this.getOrCreate(var2, "Prices");
      return this.getD(var3, "buy");
   }

   @Override
   public double getSellPrice(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      NBTTagCompound var3 = this.getOrCreate(var2, "Prices");
      return this.getD(var3, "sell");
   }

   @Override
   public ItemStack setItemErrorToItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      this.set(var2, "ItemError", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public ItemStack getItemInHand(Player var1) {
      return var1.getInventory().getItemInMainHand();
   }

   @Override
   public ItemStack setSpawnerType(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "SpawnerType", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public ItemStack setPotionType(ItemStack var1, PotionTypes var2) {
      PotionMeta var3 = (PotionMeta)var1.getItemMeta();
      if (var3.getBasePotionData().getType().getEffectType() == null) {
         var3.setBasePotionData(new PotionData(var2.parsePotionType(), var2.isExtended(), var2.isUpgraded()));
      } else {
         if (!var2.hasEffect()) {
            return var1;
         }

         var3.addCustomEffect(new PotionEffect(var2.parsePotionEffectType(), var2.getDuration(), var2.getAmplifier(), false, true, true), false);
      }

      var1.setItemMeta(var3);
      return var1;
   }

   @Override
   public Optional<PotionTypes> getBasePotion(ItemStack var1) {
      PotionMeta var2 = (PotionMeta)var1.getItemMeta();
      if (var2.getBasePotionData().getType().getEffectType() != null) {
         PotionData var3 = var2.getBasePotionData();
         return PotionTypes.matchPotionDataLegacy(var3.getType(), var3.isExtended(), var3.isUpgraded());
      } else {
         return Optional.empty();
      }
   }

   @Override
   public ItemStack setPathToItem(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "PathToItem", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public ItemStack getSpawnerToGive(EntityType var1) {
      ItemStack var2 = OnyxShop.getInstance().createItem.createSpawner(var1);
      NBTTagCompound var3 = this.getOnyxCompound(var2);
      this.set(var3, "SpawnerType", var1.toString());
      return this.setOnyxCompound(var2, var3);
   }

   @Override
   public String getPathToItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.getS(var2, "PathToItem");
   }

   @Override
   public Boolean hasItemError(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return !this.hasKey(var2, "ItemError") ? false : this.getB(var2, "ItemError");
   }

   @Override
   public String getSpawnerType(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "SpawnerType") && !this.getS(var2, "SpawnerType").isEmpty() ? this.getS(var2, "SpawnerType") : null;
   }

   private NBTBase get(NBTBase var1, String var2) {
      switch (var1.b()) {
         case 9:
            return (NBTBase)((NBTTagList)var1).get(0);
         case 10:
            return ((NBTTagCompound)var1).c(var2);
         default:
            return null;
      }
   }

   private void remove(NBTTagCompound var1, String var2, String var3) {
      try {
         NBTBase var4 = var1.c(var2);
         if (var4 == null) {
            return;
         }

         switch (var4.b()) {
            case 8:
               JSONObject var5 = JsonUtil.parseJson(this.strip(var4.toString()));
               var5.remove(var3);
               this.set(var1, var2, var5.toJSONString());
               break;
            case 9:
               ((NBTTagList)var4).remove(0);
               break;
            case 10:
               this.remove((NBTTagCompound)var4, var3);
         }
      } catch (ParseException var6) {
         var6.printStackTrace();
      }
   }

   private String strip(String var1) {
      if (var1.startsWith("'") && var1.endsWith("'")) {
         var1 = var1.substring(1, var1.length() - 1);
      }

      if (var1.startsWith("\"") && var1.endsWith("\"")) {
         var1 = var1.substring(1, var1.length() - 1);
      }

      return var1;
   }

   @Override
   public boolean isSimilar(ItemStack var1, ItemStack var2, List<String> var3) {
      net.minecraft.world.item.ItemStack var4 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      net.minecraft.world.item.ItemStack var5 = CraftItemStack.asNMSCopy(new ItemStack(var2));
      if (!this.hasTag(var4) && !this.hasTag(var5)) {
         return var1.isSimilar(var2);
      } else {
         for (String var7 : var3) {
            String[] var8 = var7.split("::");
            if (var8.length >= 2) {
               NBTTagCompound var9 = this.getC(var4);
               if (var9 != null) {
                  for (int var10 = 0; var10 < var8.length - 1; var10++) {
                     if (var10 == var8.length - 2) {
                        this.remove(var9, var8[var10], var8[var10 + 1]);
                        break;
                     }

                     var9 = this.getC(var9, var8[var10]);
                  }
               }

               NBTTagCompound var12 = this.getC(var5);
               if (var12 != null) {
                  for (int var11 = 0; var11 < var8.length - 1; var11++) {
                     if (var11 == var8.length - 2) {
                        this.remove(var12, var8[var11], var8[var11 + 1]);
                        break;
                     }

                     var12 = this.getC(var12, var8[var11]);
                  }
               }
            } else {
               this.remove(var4, var7);
               this.remove(var5, var7);
            }
         }

         return var1.getType().equals(Material.PLAYER_HEAD) && !var3.contains("SkullOwner") && !this.isSimilarSkull(var4, var5)
            ? false
            : CraftItemStack.asBukkitCopy(var4).isSimilar(CraftItemStack.asBukkitCopy(var5));
      }
   }

   private boolean isSimilarSkull(net.minecraft.world.item.ItemStack var1, net.minecraft.world.item.ItemStack var2) {
      if (this.hasTag(var1) && this.hasTag(var2)) {
         try {
            String var3 = null;
            String var4 = null;
            NBTTagList var5 = this.getL(this.getC(this.getC(this.getC(var1), "SkullOwner"), "Properties"), "textures", 10);
            if (var5 != null && !var5.isEmpty()) {
               for (int var6 = 0; var6 < var5.size(); var6++) {
                  NBTTagCompound var7 = this.getC(var5, var6);
                  if (this.hasKey(var7, "Value")) {
                     var3 = this.getS(var7, "Value");
                  }
               }

               if (var3 == null) {
                  return false;
               }
            }

            NBTTagList var10 = this.getL(this.getC(this.getC(this.getC(var2), "SkullOwner"), "Properties"), "textures", 10);
            if (var10 != null && !var10.isEmpty()) {
               for (int var11 = 0; var11 < var10.size(); var11++) {
                  NBTTagCompound var8 = this.getC(var10, var11);
                  if (this.hasKey(var8, "Value")) {
                     var4 = this.getS(var8, "Value");
                  }
               }

               if (var4 == null) {
                  return false;
               }
            }

            boolean var12 = false;
            if (this.hasKey(this.getC(this.getC(var1), "SkullOwner"), "Name")
               && this.hasKey(this.getC(this.getC(var2), "SkullOwner"), "Name")
               && this.getS(this.getC(this.getC(var1), "SkullOwner"), "Name").equalsIgnoreCase(this.getS(this.getC(this.getC(var2), "SkullOwner"), "Name"))) {
               this.remove(this.getC(var1), "SkullOwner");
               this.remove(this.getC(var2), "SkullOwner");
               var12 = true;
            }

            return var12 && var3 == null && var4 == null
               || var3 != null && var4 != null && (var3.equals(var4) || SkullUtil.getTextureURL(var3).equals(SkullUtil.getTextureURL(var4)));
         } catch (NullPointerException var9) {
            SendMessage.errorMessage("Error occurred while comparing skulls");
            var9.printStackTrace();
            return false;
         }
      } else {
         return false;
      }
   }
}
