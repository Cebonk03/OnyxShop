package com.onyxshop.versions.v1_16_R3;

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
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.MojangsonParser;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagString;
import net.minecraft.server.v1_16_R3.PacketPlayOutOpenWindow;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

public class v1_16_R3 implements VersionHandler {
   @Override
   public boolean hasData(ItemStack var1, String var2) {
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (var3.hasTag()) {
         NBTTagCompound var4 = var3.getTag();
         return var4.hasKey(var2);
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
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (var3.hasTag()) {
         NBTTagCompound var4 = var3.getTag();
         if (!var4.hasKey("display")) {
            var4.set("display", new NBTTagCompound());
         }

         var4.getCompound("display").set("Name", NBTTagString.a(this.toNBT(var2)));
      }

      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public String getNBTDataAsString(ItemStack var1, boolean var2) {
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      if (var3.hasTag() && !var3.getTag().isEmpty()) {
         NBTTagCompound var4 = var3.getTag();
         if (!var2) {
            var4.remove("Enchantments");
            var4.remove("StoredEnchantments");
            var4.remove("Recipes");
            var4.remove("Fireworks");
            var4.remove("ONYX");
            var4.remove("SkullOwner");
         }

         return var4.toString().equals("{}") ? null : var4.toString();
      } else {
         return null;
      }
   }

   @Override
   public String getLoreAsNBT(ItemStack var1) {
      String var2 = CraftItemStack.asNMSCopy(new ItemStack(var1)).getTag().getCompound("display").getList("Lore", 8).get(0).toString();
      return var2.substring(1).substring(0, var2.length() - 1);
   }

   @Override
   public Object getNMSLore(ItemStack var1) {
      net.minecraft.server.v1_16_R3.ItemStack var2 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (var2.hasTag()) {
         NBTTagCompound var3 = var2.getTag();
         NBTTagCompound var4 = var3.hasKey("display") ? var3.getCompound("display") : new NBTTagCompound();
         return var4.hasKey("Lore") ? var4.getList("Lore", 8) : new NBTTagList();
      } else {
         return new NBTTagList();
      }
   }

   @Override
   public ItemStack applyNMSLore(ItemStack var1, Object var2) {
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      if (var3.hasTag()) {
         NBTTagCompound var4 = var3.getTag();
         if (!var4.hasKey("display")) {
            var4.set("display", new NBTTagCompound());
         }

         var4.getCompound("display").set("Lore", (NBTTagList)var2);
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
      var3.playerConnection
         .sendPacket(
            new PacketPlayOutOpenWindow(
               var3.activeContainer.windowId,
               var3.activeContainer.getType(),
               ServerInfo.supportsPaper() ? (IChatBaseComponent)AdventureUtil.asVanilla(var2) : CraftChatMessage.fromStringOrNull(var2.getLegacy())
            )
         );
      var1.updateInventory();
   }

   @Override
   public ItemStack setNBTString(ItemStack var1, String var2, String var3) {
      net.minecraft.server.v1_16_R3.ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var5 = var4.hasTag() ? var4.getTag() : new NBTTagCompound();
      String[] var6 = var2.split("::");
      NBTTagCompound var7 = var5;

      for (int var8 = 0; var8 < var6.length - 1; var8++) {
         String var9 = var6[var8];
         NBTTagCompound var10;
         if (var7.hasKey(var9)) {
            var10 = var7.getCompound(var9);
         } else {
            var10 = new NBTTagCompound();
            var7.set(var9, var10);
         }

         var7 = var10;
      }

      var7.setString(var6[var6.length - 1], var3);
      var4.setTag(var5);
      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public String getNBTString(ItemStack var1, String var2) {
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var4 = var3.hasTag() ? var3.getTag() : new NBTTagCompound();
      String[] var5 = var2.split("::");

      for (int var6 = 0; var6 < var5.length - 1; var6++) {
         if (!var4.hasKey(var5[var6])) {
            return null;
         }

         var4 = var4.getCompound(var5[var6]);
      }

      return var4.hasKey(var5[var5.length - 1]) ? var4.getString(var5[var5.length - 1]) : null;
   }

   @Override
   public ItemStack setNBTInt(ItemStack var1, String var2, Integer var3) {
      net.minecraft.server.v1_16_R3.ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var5 = var4.hasTag() ? var4.getTag() : new NBTTagCompound();
      if (var2.contains("::")) {
         NBTTagCompound var6 = var5.hasKey(var2.split("::")[0]) ? var5.getCompound(var2.split("::")[0]) : new NBTTagCompound();
         var6.setInt(var2.split("::")[1], var3);
         var5.set(var2.split("::")[0], var6);
      } else {
         var5.setInt(var2, var3);
      }

      var4.setTag(var5);
      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public Integer getNBTInt(ItemStack var1, String var2) {
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var4 = var3.hasTag() ? var3.getTag() : new NBTTagCompound();
      return var4.hasKey(var2) ? var4.getInt(var2) : null;
   }

   @Override
   public ItemStack setPages(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      var3.setString("Pages", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getPages(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.getString("Pages");
   }

   @Override
   public ItemStack setOnyxNbtString(ItemStack var1, String var2, String var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      var4.setString(var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public String getOnyxNbtString(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      return var3.hasKey(var2) ? var3.getString(var2) : null;
   }

   @Override
   public ItemStack setOnyxNbtInt(ItemStack var1, String var2, Integer var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      var4.setInt(var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public Integer getOnyxNbtInt(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      return var3.hasKey(var2) ? var3.getInt(var2) : null;
   }

   @Override
   public ItemStack setIndex(ItemStack var1, Integer var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      var3.setInt("Index", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public Integer getIndex(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.hasKey("Index") ? var2.getInt("Index") : -1;
   }

   @Override
   public ItemStack setAction(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      var3.setString("Action", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getAction(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.hasKey("Action") ? var2.getString("Action") : null;
   }

   @Override
   public ItemStack setEditOption(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      var3.setString("EditOption", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getEditOption(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.hasKey("EditOption") ? var2.getString("EditOption") : null;
   }

   @Override
   public ItemStack getShopAdminItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      var2.setBoolean("AdminItem", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isShopAdminItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.getBoolean("AdminItem");
   }

   @Override
   public ItemStack addNBTtag(ItemStack var1, String var2, boolean var3) throws ItemLoadException {
      net.minecraft.server.v1_16_R3.ItemStack var4 = CraftItemStack.asNMSCopy(var1);

      try {
         NBTTagCompound var5 = MojangsonParser.parse(var2);
         if (var3 && var5.hasKey("display")) {
            var5.remove("display");
         }

         NBTTagCompound var9 = var4.hasTag() ? var4.getTag() : new NBTTagCompound();
         var9.a(var5);
         var4.setTag(var9);
         return CraftItemStack.asBukkitCopy(var4);
      } catch (CommandSyntaxException var8) {
         StringWriter var6 = new StringWriter();
         PrintWriter var7 = new PrintWriter(var6);
         var8.printStackTrace(var7);
         SendMessage.logDebugMessage(var6.toString());
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get());
      }
   }

   @Override
   public ItemStack addNBTCommands(ItemStack var1, List<String> var2, CommandType var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      NBTTagList var5 = var4.hasKey(var3.getCompound()) ? var4.getList(var3.getCompound(), 8) : new NBTTagList();
      var2.forEach(var1x -> var5.add(NBTTagString.a(var1x.startsWith("/") ? var1x.substring(1) : var1x)));
      var4.set(var3.getCompound(), var5);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public boolean hasNBTCommand(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.hasKey("buy-commands") && !var2.getList("buy-commands", 8).isEmpty()
         ? true
         : var2.hasKey("sell-commands") && !var2.getList("sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(ItemStack var1, CommandType var2) {
      NBTTagList var3 = this.getOnyxCompound(var1).getList(var2.getCompound(), 8);
      return var3.stream().<String>map(NBTBase::toString).collect(Collectors.toList());
   }

   private NBTTagCompound getOnyxCompound(ItemStack var1) {
      net.minecraft.server.v1_16_R3.ItemStack var2 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var3 = var2.hasTag() ? var2.getTag() : new NBTTagCompound();
      return var3.hasKey("ONYX") ? var3.getCompound("ONYX") : new NBTTagCompound();
   }

   private ItemStack setOnyxCompound(ItemStack var1, NBTTagCompound var2) {
      net.minecraft.server.v1_16_R3.ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var4 = var3.hasTag() ? var3.getTag() : new NBTTagCompound();
      var4.set("ONYX", var2);
      var3.setTag(var4);
      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public String getTitle(Inventory var1) {
      return "";
   }

   @Override
   public ItemStack setDisabledBackButton(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      var2.setBoolean("disabledBackButton", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isDisabledBackButton(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.hasKey("disabledBackButton") && var2.getBoolean("disabledBackButton");
   }

   @Override
   public ItemStack addNBTPrices(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      NBTTagCompound var4 = var3.hasKey("Prices") ? var3.getCompound("Prices") : new NBTTagCompound();
      var4.setDouble("buy", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".buy"));
      var4.setDouble("sell", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".sell"));
      var3.set("Prices", var4);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public double getBuyPrice(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      NBTTagCompound var3 = var2.getCompound("Prices");
      return var3.getDouble("buy");
   }

   @Override
   public double getSellPrice(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      NBTTagCompound var3 = var2.getCompound("Prices");
      return var3.getDouble("sell");
   }

   @Override
   public ItemStack setItemErrorToItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      var2.setBoolean("ItemError", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public ItemStack getItemInHand(Player var1) {
      return var1.getInventory().getItemInMainHand();
   }

   @Override
   public ItemStack setSpawnerType(ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      var3.setString("SpawnerType", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public ItemStack setPotionType(ItemStack var1, PotionTypes var2) {
      PotionMeta var3 = (PotionMeta)var1.getItemMeta();
      if (var3.getBasePotionData().getType().getEffectType() == null) {
         var3.setBasePotionData(new PotionData(var2.parsePotionType(), var2.isExtended(), var2.isUpgraded()));
      } else {
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
      var3.setString("PathToItem", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public ItemStack getSpawnerToGive(EntityType var1) {
      ItemStack var2 = OnyxShop.getInstance().createItem.createSpawner(var1);
      NBTTagCompound var3 = this.getOnyxCompound(var2);
      var3.setString("SpawnerType", var1.toString());
      return this.setOnyxCompound(var2, var3);
   }

   @Override
   public String getPathToItem(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.getString("PathToItem");
   }

   @Override
   public Boolean hasItemError(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return !var2.hasKey("ItemError") ? false : var2.getBoolean("ItemError");
   }

   @Override
   public String getSpawnerType(ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return var2.hasKey("SpawnerType") && !var2.getString("SpawnerType").isEmpty() ? var2.getString("SpawnerType") : null;
   }

   @Override
   public boolean isSimilar(ItemStack var1, ItemStack var2, List<String> var3) {
      net.minecraft.server.v1_16_R3.ItemStack var4 = CraftItemStack.asNMSCopy(new ItemStack(var1));
      net.minecraft.server.v1_16_R3.ItemStack var5 = CraftItemStack.asNMSCopy(new ItemStack(var2));
      if (!var4.hasTag() && !var5.hasTag()) {
         return var1.isSimilar(var2);
      } else {
         for (String var7 : var3) {
            String[] var8 = var7.split("::");
            if (var8.length >= 2) {
               NBTTagCompound var9 = var4.getTag();
               if (var9 != null) {
                  for (int var10 = 0; var10 < var8.length; var10++) {
                     var9 = var9.getCompound(var8[var10]);
                     if (var10 == var8.length - 2) {
                        var9.remove(var8[var10 + 1]);
                     }
                  }
               }

               NBTTagCompound var12 = var5.getTag();
               if (var12 != null) {
                  for (int var11 = 0; var11 < var8.length; var11++) {
                     var12 = var12.getCompound(var8[var11]);
                     if (var11 == var8.length - 2) {
                        var12.remove(var8[var11 + 1]);
                     }
                  }
               }
            } else {
               var4.removeTag(var7);
               var5.removeTag(var7);
            }
         }

         return var1.getType().equals(Material.PLAYER_HEAD) && !var3.contains("SkullOwner") && !this.isSimilarSkull(var4, var5)
            ? false
            : CraftItemStack.asBukkitCopy(var4).isSimilar(CraftItemStack.asBukkitCopy(var5));
      }
   }

   private boolean isSimilarSkull(net.minecraft.server.v1_16_R3.ItemStack var1, net.minecraft.server.v1_16_R3.ItemStack var2) {
      if (var1.hasTag() && var2.hasTag()) {
         try {
            String var3 = null;
            String var4 = null;
            NBTTagList var5 = var1.getTag().getCompound("SkullOwner").getCompound("Properties").getList("textures", 10);
            if (var5 != null && !var5.isEmpty()) {
               for (int var6 = 0; var6 < var5.size(); var6++) {
                  NBTTagCompound var7 = var5.getCompound(var6);
                  if (var7.hasKey("Value")) {
                     var3 = var7.getString("Value");
                  }
               }

               if (var3 == null) {
                  return false;
               }
            }

            NBTTagList var10 = var2.getTag().getCompound("SkullOwner").getCompound("Properties").getList("textures", 10);
            if (var10 != null && !var10.isEmpty()) {
               for (int var11 = 0; var11 < var10.size(); var11++) {
                  NBTTagCompound var8 = var10.getCompound(var11);
                  if (var8.hasKey("Value")) {
                     var4 = var8.getString("Value");
                  }
               }

               if (var4 == null) {
                  return false;
               }
            }

            boolean var12 = false;
            if (var1.getTag().getCompound("SkullOwner").hasKey("Name")
               && var2.getTag().getCompound("SkullOwner").hasKey("Name")
               && var1.getTag().getCompound("SkullOwner").getString("Name").equals(var2.getTag().getCompound("SkullOwner").getString("Name"))) {
               var1.getTag().remove("SkullOwner");
               var2.getTag().remove("SkullOwner");
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
