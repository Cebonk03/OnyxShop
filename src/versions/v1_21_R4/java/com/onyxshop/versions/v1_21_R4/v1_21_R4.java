package com.onyxshop.versions.v1_21_R4;

import java.lang.reflect.InvocationTargetException;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.google.gson.Gson;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import com.onyxshop.util.NBTUtils;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.minecraft.core.HolderLookup.a;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.util.datafix.fixes.DataConverterTypes;
import net.minecraft.world.ChestLock;
import net.minecraft.world.food.FoodInfo;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.EnumItemRarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R4.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R4.CraftServer;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftItem;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R4.inventory.SerializableMeta;
import org.bukkit.craftbukkit.v1_21_R4.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class v1_21_R4 implements VersionHandler {
   private static final Field CUSTOM_TAG = getFieldByType(CustomData.class, NBTTagCompound.class);
   private static final Field COMPONENT_MAP = getFieldByType(ItemStack.class, PatchedDataComponentMap.class);
   private static final Field DATA_MAP = getFieldByType(PatchedDataComponentMap.class, Reference2ObjectMap.class);
   private static final Field ITEM_ITEM = getFieldByType(ItemStack.class, Item.class);
   private static final Method ITEM_MATCHER = getMethodByParams(ItemStack.class, boolean.class, Item.class);

   private static Method getMethodByParams(Class<?> var0, Class<?> var1, Class<?>... var2) {
      for (Method var6 : var0.getDeclaredMethods()) {
         if (var6.getReturnType() == var1 && Arrays.equals((Object[])var6.getParameterTypes(), (Object[])var2)) {
            return var6;
         }
      }

      return null;
   }

   private static Field getFieldByType(Class<?> var0, Class<?> var1) {
      for (Field var5 : var0.getDeclaredFields()) {
         if (var5.getType().equals(var1)) {
            return var5;
         }
      }

      return null;
   }

   private static Field getFieldByName(Class<?> var0, String var1) {
      for (Field var5 : var0.getDeclaredFields()) {
         if (var5.getName().equals(var1)) {
            return var5;
         }
      }

      return null;
   }

   @Override
   public boolean hasData(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      PatchedDataComponentMap var4 = this.getC(var3);

      try {
         DataComponentType var5 = v1_21_R4.Components.valueOf(var2, var2.getClass());
         return var4.d(var5);
      } catch (Exception var6) {
         return false;
      }
   }

   @Override
   public Object emptyList() {
      return new ArrayList();
   }

   @Override
   public String toNBT(String var1) {
      IChatBaseComponent var2 = CraftChatMessage.fromString(var1, true)[0];
      return CraftChatMessage.toJSON(var2);
   }

   public void setLived(org.bukkit.entity.Item var1) {
      ((CraftItem)var1).getHandle().l = -32768;
   }

   public org.bukkit.inventory.ItemStack setComponent(org.bukkit.inventory.ItemStack var1, String var2, Object var3) {
      ItemStack var4 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      if (this.hasTag(var4)) {
         PatchedDataComponentMap var5 = this.getC(var4);

         try {
            var5.b(v1_21_R4.Components.valueOf(var2, Object.class), var3);
         } catch (Exception var7) {
            SendMessage.warnMessage("Unsupported component tag found for " + var2 + " while injecting item data, this is a bug, please report");
         }
      }

      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisplayname(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      if (this.hasTag(var3)) {
         PatchedDataComponentMap var4 = this.getC(var3);
         this.set(var4, v1_21_R4.Components.custom_name, CraftChatMessage.fromStringOrNull(this.toNBT(var2)));
      }

      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public String getNBTDataAsString(org.bukkit.inventory.ItemStack var1, boolean var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      if (this.hasTag(var3)) {
         PatchedDataComponentMap var4 = this.getC(var3);
         this.setAccessible(var4);
         if (!var2) {
            this.remove(var4, v1_21_R4.Components.enchantments);
            this.remove(var4, v1_21_R4.Components.stored_enchantments);
            this.remove(var4, v1_21_R4.Components.firework_explosion);
            this.remove(var4, v1_21_R4.Components.fireworks);
            this.remove(var4, v1_21_R4.Components.instrument);
            this.remove(var4, v1_21_R4.Components.trim);
            this.remove(var4, v1_21_R4.Components.lore);
            this.remove(var4, v1_21_R4.Components.custom_name);
            this.remove(var4, v1_21_R4.Components.suspicious_stew_effects);
            this.remove(var4, v1_21_R4.Components.recipes);
            this.remove(var4, v1_21_R4.Components.dyed_color);
            this.removeCustom(var4, "ONYX");
            this.remove(var4, v1_21_R4.Components.ominous_bottle_amplifier);
            this.remove(var4, v1_21_R4.Components.profile);
         }

         try {
            String var5 = v1_21_R4.NBTSerializer.serialize(var3);
            return var5.equals("{}") ? null : var5;
         } catch (Exception var6) {
            SendMessage.errorMessage("Failed to serialize NBT to string");
            var6.printStackTrace();
            return null;
         }
      } else {
         return null;
      }
   }

   @Override
   public String getLoreAsNBT(org.bukkit.inventory.ItemStack var1) {
      PatchedDataComponentMap var2 = this.getC(CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1)));
      String var3 = CraftChatMessage.fromComponent((IChatBaseComponent)this.<ItemLore>getOrNull(var2, v1_21_R4.Components.lore).a().get(0));
      return var3.substring(1).substring(0, var3.length() - 1);
   }

   @Override
   public Object getNMSLore(org.bukkit.inventory.ItemStack var1) {
      ItemStack var2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      PatchedDataComponentMap var3 = this.getC(var2);
      List var4 = this.<ItemLore>getOrNull(var3, v1_21_R4.Components.lore).a();
      return var4 != null ? var4 : new ArrayList();
   }

   @Override
   public org.bukkit.inventory.ItemStack applyNMSLore(org.bukkit.inventory.ItemStack var1, Object var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      if (this.hasTag(var3)) {
         PatchedDataComponentMap var4 = this.getC(var3);
         this.set(var4, v1_21_R4.Components.lore, new ItemLore((List)var2));
      }

      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public void setItemLore(Object var1, String var2, int var3) {
      ((List)var1).set(var3, CraftChatMessage.fromJSON(var2));
   }

   @Override
   public void addItemLore(Object var1, String var2, int var3) {
      ((List)var1).add(var3, CraftChatMessage.fromJSON(var2));
   }

   @Override
   public void addItemLore(Object var1, String var2) {
      ((List)var1).add(CraftChatMessage.fromJSON(var2));
   }

   @Override
   public void updateInventoryTitle(Player var1, Translatable var2) {
      EntityPlayer var3 = ((CraftPlayer)var1).getHandle();
      var3.f
         .b(
            new PacketPlayOutOpenWindow(
               var3.bR.l,
               var3.bR.a(),
               ServerInfo.supportsPaper() ? (IChatBaseComponent)AdventureUtil.asVanilla(var2) : CraftChatMessage.fromStringOrNull(var2.getLegacy())
            )
         );
      var1.updateInventory();
   }

   @Override
   public org.bukkit.inventory.ItemStack setNBTString(org.bukkit.inventory.ItemStack var1, String var2, String var3) {
      ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var5 = this.getC(var4);
      NBTTagCompound var6 = this.getCustom(var5);
      String[] var7 = var2.split("::");
      NBTTagCompound var8 = var6;

      for (int var9 = 0; var9 < var7.length - 1; var9++) {
         String var10 = var7[var9];
         NBTTagCompound var11;
         if (this.hasKey(var8, var10)) {
            var11 = (NBTTagCompound)var8.m(var10).get();
         } else {
            var11 = new NBTTagCompound();
            this.set(var8, var10, var11);
         }

         var8 = var11;
      }

      this.set(var8, var7[var7.length - 1], var3);
      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public String getNBTString(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var4 = this.getC(var3);
      NBTTagCompound var5 = this.getCustom(var4);
      String[] var6 = var2.split("::");

      for (int var7 = 0; var7 < var6.length - 1; var7++) {
         if (!this.hasKey(var5, var6[var7])) {
            return null;
         }

         var5 = this.getC(var5, var6[var7]);
      }

      return this.hasKey(var5, var6[var6.length - 1]) ? this.getS(var5, var6[var6.length - 1]) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setNBTInt(org.bukkit.inventory.ItemStack var1, String var2, Integer var3) {
      ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var5 = this.getC(var4);
      if (var2.contains("::")) {
         NBTTagCompound var6 = this.getCustom(var5);
         NBTTagCompound var7 = this.getOrCreate(var6, var2.split("::")[0]);
         this.set(var7, var2.split("::")[1], var3);
         this.set(var6, var2.split("::")[0], var7);
      } else {
         NBTTagCompound var8 = this.getCustom(var5);
         this.set(var8, var2, var3);
      }

      return CraftItemStack.asBukkitCopy(var4);
   }

   @Override
   public Integer getNBTInt(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var4 = this.getC(var3);
      String[] var5 = var2.split("::");
      if (var5[0].equals("custom_data")) {
         NBTTagCompound var8 = this.getCustom(var4);

         for (int var7 = 1; var7 < var5.length - 1; var7++) {
            if (!this.hasKey(var8, var5[var7])) {
               return null;
            }

            var8 = this.getC(var8, var5[var7]);
         }

         return this.hasKey(var8, var5[var5.length - 1]) ? this.getI(var8, var5[var5.length - 1]) : null;
      } else {
         NBTBase var6 = this.getC(var4, var2);
         return var6 != null ? (Integer)var6.r().orElse(null) : null;
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack setPages(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "Pages", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getPages(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.getS(var2, "Pages");
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtString(org.bukkit.inventory.ItemStack var1, String var2, String var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      this.set(var4, var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public String getOnyxNbtString(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      return this.hasKey(var3, var2) ? this.getS(var3, var2) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtInt(org.bukkit.inventory.ItemStack var1, String var2, Integer var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      this.set(var4, var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public Integer getOnyxNbtInt(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      return this.hasKey(var3, var2) ? this.getI(var3, var2) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setIndex(org.bukkit.inventory.ItemStack var1, Integer var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "Index", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public Integer getIndex(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "Index") ? this.getI(var2, "Index") : -1;
   }

   @Override
   public org.bukkit.inventory.ItemStack setAction(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "Action", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getAction(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "Action") ? this.getS(var2, "Action") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setEditOption(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "EditOption", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getEditOption(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "EditOption") ? this.getS(var2, "EditOption") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack getShopAdminItem(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      this.set(var2, "AdminItem", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isShopAdminItem(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.getB(var2, "AdminItem");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTtag(org.bukkit.inventory.ItemStack var1, String var2, boolean var3) throws ItemLoadException {
      ItemStack var4 = CraftItemStack.asNMSCopy(var1);

      try {
         Optional<DataComponentPatch> var5 = v1_21_R4.NBTSerializer.deserialize(var2, var3);
         return var5.<org.bukkit.inventory.ItemStack>map(var1x -> {
            var4.b(var1x);
            return CraftItemStack.asBukkitCopy(var4);
         }).orElse(null);
      } catch (Throwable var8) {
         StringWriter var6 = new StringWriter();
         PrintWriter var7 = new PrintWriter(var6);
         var8.printStackTrace(var7);
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get().getLegacy() + ": " + var6.toString());
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTCommands(org.bukkit.inventory.ItemStack var1, List<String> var2, CommandType var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      NBTTagList var5 = this.getOrCreate(var4, var3.getCompound(), 8);
      var2.forEach(var1x -> var5.add(NBTTagString.a(var1x.startsWith("/") ? var1x.substring(1) : var1x)));
      this.set(var4, var3.getCompound(), var5);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public boolean hasNBTCommand(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return !this.getOrCreate(var2, "buy-commands", 8).isEmpty() || !this.getOrCreate(var2, "sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(org.bukkit.inventory.ItemStack var1, CommandType var2) {
      NBTTagList var3 = this.getOrCreate(this.getOnyxCompound(var1), var2.getCompound(), 8);
      return var3.stream().<String>map(NBTBase::toString).collect(Collectors.toList());
   }

   private NBTTagCompound getC(NBTTagCompound var1, String var2) {
      return var1.n(var2);
   }

   private NBTBase getC(PatchedDataComponentMap var1, String var2) {
      try {
         DataComponentType var3 = v1_21_R4.Components.valueOf(var2);
         return this.encodeC(var3, var1);
      } catch (Exception var4) {
         return null;
      }
   }

   private <T> NBTBase encodeC(DataComponentType<T> var1, PatchedDataComponentMap var2) {
      T var3 = (T)var2.a(var1);
      return (NBTBase)var1.c().encodeStart(DynamicOpsNBT.a, var3).getOrThrow();
   }

   private NBTTagCompound getC(NBTTagList var1, int var2) {
      return var1.b(var2);
   }

   private PatchedDataComponentMap getC(ItemStack var1) {
      try {
         return (PatchedDataComponentMap)COMPONENT_MAP.get(var1);
      } catch (IllegalAccessException var3) {
         SendMessage.logDebugMessage("Failed to retrieve compounds from item");
         var3.printStackTrace();
         return null;
      }
   }

   private NBTTagCompound getCustom(PatchedDataComponentMap var1) {
      return this.getCustom(var1, false);
   }

   private NBTTagCompound getCustom(PatchedDataComponentMap var1, boolean var2) {
      try {
         CustomData var3 = this.getOrNull(var1, DataComponents.b);
         if (var3 == null) {
            if (var2) {
               return null;
            }

            NBTTagCompound var4 = new NBTTagCompound();

            try {
               this.addCustom(var1, DataComponents.b, var4);
            } catch (Exception var6) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               var6.printStackTrace();
            }

            var3 = this.getOrNull(var1, DataComponents.b);
         }

         return this.getCustom(var3);
      } catch (IllegalAccessException var7) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         var7.printStackTrace();
         return null;
      }
   }

   private NBTTagCompound getCustom(CustomData var1) throws IllegalAccessException {
      return (NBTTagCompound)CUSTOM_TAG.get(var1);
   }

   private NBTTagCompound getCustom(Optional<?> var1) throws IllegalAccessException {
      return var1 != null && var1.isPresent() ? this.getCustom((CustomData)var1.get()).l() : new NBTTagCompound();
   }

   private NBTTagCompound getClonedCustom(PatchedDataComponentMap var1) {
      return this.getClonedCustom(var1, false);
   }

   private NBTTagCompound getClonedCustom(PatchedDataComponentMap var1, boolean var2) {
      try {
         CustomData var3 = this.getOrNull(var1, DataComponents.b);
         if (var3 == null) {
            if (var2) {
               return null;
            }

            NBTTagCompound var4 = new NBTTagCompound();

            try {
               this.addCustom(var1, DataComponents.b, var4);
            } catch (Exception var6) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               var6.printStackTrace();
            }

            var3 = this.getOrNull(var1, DataComponents.b);
         }

         return this.getClonedCustom(var1, var3);
      } catch (Exception var7) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         var7.printStackTrace();
         return null;
      }
   }

   private NBTTagCompound getClonedCustom(PatchedDataComponentMap var1, CustomData var2) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException {
      Field var3 = CustomData.class.getDeclaredField("tag");
      var3.setAccessible(true);
      NBTTagCompound var4 = (NBTTagCompound)var3.get(var2);
      Method var5 = NBTTagCompound.class.getDeclaredMethod("copy");
      var5.setAccessible(true);
      NBTTagCompound var6 = (NBTTagCompound)var5.invoke(var4);
      Constructor var7 = Class.forName("net.minecraft.world.item.component.CustomData").getDeclaredConstructor(NBTTagCompound.class);
      var7.setAccessible(true);
      CustomData var8 = (CustomData)var7.newInstance(var6);
      this.set(var1, DataComponents.b, var8);
      return var6;
   }

   private void addCustom(PatchedDataComponentMap var1, DataComponentType<CustomData> var2, NBTTagCompound var3) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      Constructor var4 = CustomData.class.getDeclaredConstructor(var3.getClass());
      var4.setAccessible(true);
      CustomData var5 = (CustomData)var4.newInstance(var3);
      this.set(var1, var2, var5);
   }

   private void removeCustom(PatchedDataComponentMap var1, String var2) {
      NBTTagCompound var3 = this.getCustom(var1, true);
      if (var3 != null) {
         var3.r(var2);
      }
   }

   private String getS(NBTTagCompound var1, String var2) {
      return var1.b(var2, "");
   }

   private double getD(NBTTagCompound var1, String var2) {
      return var1.b(var2, 0.0);
   }

   private boolean getB(NBTTagCompound var1, String var2) {
      return var1.b(var2, false);
   }

   private int getI(NBTTagCompound var1, String var2) {
      return var1.b(var2, 0);
   }

   private NBTTagList getOrCreate(NBTTagCompound var1, String var2, int var3) {
      return var1.p(var2);
   }

   private NBTTagCompound getOrCreate(NBTTagCompound var1, String var2) {
      return var1.n(var2);
   }

   private boolean hasKey(NBTTagCompound var1, String var2) {
      return var1.b(var2);
   }

   private <T> T getOrNull(PatchedDataComponentMap var1, DataComponentType<? extends T> var2) {
      return (T)var1.a(var2);
   }

   private CustomData getOrCreateCustom(PatchedDataComponentMap var1, DataComponentType<CustomData> var2) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      CustomData var3 = this.getOrNull(var1, var2);
      if (var3 == null) {
         this.addCustom(var1, var2, new NBTTagCompound());
         return this.getOrNull(var1, var2);
      } else {
         return var3;
      }
   }

   private boolean hasTag(ItemStack var1) {
      return !var1.f();
   }

   private String getString(NBTTagCompound var1, String var2) {
      return var1.b(var2, "");
   }

   private void setString(NBTTagCompound var1, String var2, String var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, NBTTagList var3) {
      var1.a(var2, var3);
   }

   private void remove(PatchedDataComponentMap var1, DataComponentType<?> var2) {
      try {
         Reference2ObjectMap var3 = (Reference2ObjectMap)DATA_MAP.get(var1);
         var3.remove(var2);
      } catch (Exception var4) {
         SendMessage.errorMessage("Failed to remove value from data map");
         var4.printStackTrace();
      }
   }

   private void setAccessible(PatchedDataComponentMap var1) {
      try {
         Method var2 = var1.getClass().getDeclaredMethod("j");
         var2.setAccessible(true);
         var2.invoke(var1);
      } catch (Exception var3) {
         SendMessage.errorMessage("Failed to make data map accessible");
         var3.printStackTrace();
      }
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

   private <T> T set(PatchedDataComponentMap var1, DataComponentType<T> var2, T var3) {
      return (T)var1.b(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, String var3) {
      var1.a(var2, var3);
   }

   private void set(NBTTagCompound var1, String var2, Integer var3) {
      var1.a(var2, var3);
   }

   private void merge(NBTTagCompound var1, NBTTagCompound var2) {
      var1.a(var2);
   }

   private void set(ItemStack var1, PatchedDataComponentMap var2) {
      try {
         COMPONENT_MAP.set(var1, var2);
      } catch (IllegalAccessException var4) {
         SendMessage.logDebugMessage("Failed to retrieve compounds from item");
         var4.printStackTrace();
      }
   }

   private NBTTagCompound getOnyxCompound(org.bukkit.inventory.ItemStack var1) {
      ItemStack var2 = CraftItemStack.asNMSCopy(var1);
      NBTTagCompound var3 = this.getCustom(this.getC(var2));
      return this.getOrCreate(var3, "ONYX");
   }

   private org.bukkit.inventory.ItemStack setOnyxCompound(org.bukkit.inventory.ItemStack var1, NBTTagCompound var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var4 = this.getC(var3);
      NBTTagCompound var5 = this.getCustom(var4);
      this.set(var5, "ONYX", var2);
      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public String getTitle(Inventory var1) {
      return "";
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisabledBackButton(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      this.set(var2, "disabledBackButton", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isDisabledBackButton(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "disabledBackButton") && this.getB(var2, "disabledBackButton");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTPrices(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      NBTTagCompound var4 = this.getOrCreate(var3, "Prices");
      this.set(var4, "buy", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".buy"));
      this.set(var4, "sell", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".sell"));
      this.set(var3, "Prices", var4);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public double getBuyPrice(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      NBTTagCompound var3 = this.getOrCreate(var2, "Prices");
      return this.getD(var3, "buy");
   }

   @Override
   public double getSellPrice(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      NBTTagCompound var3 = this.getOrCreate(var2, "Prices");
      return this.getD(var3, "sell");
   }

   @Override
   public org.bukkit.inventory.ItemStack setItemErrorToItem(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      this.set(var2, "ItemError", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public org.bukkit.inventory.ItemStack getItemInHand(Player var1) {
      return var1.getInventory().getItemInMainHand();
   }

   @Override
   public org.bukkit.inventory.ItemStack setSpawnerType(org.bukkit.inventory.ItemStack var1, String var2) {
      return this.setSpawnerType(var1, var2, false);
   }

   public org.bukkit.inventory.ItemStack setSpawnerType(org.bukkit.inventory.ItemStack var1, String var2, boolean var3) {
      NBTTagCompound var4 = this.getOnyxCompound(var1);
      this.set(var4, "SpawnerType", var2);
      ItemStack var5 = CraftItemStack.asNMSCopy(this.setOnyxCompound(var1, var4));
      PatchedDataComponentMap var6 = this.getC(var5);
      TooltipDisplay var7 = this.getOrNull(var6, DataComponents.q);
      var7.a(DataComponents.aa, true);
      return this.setOnyxCompound(var1, var4);
   }

   public org.bukkit.inventory.ItemStack addSpawnPotential(org.bukkit.inventory.ItemStack var1, EntityType var2, Integer var3) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var5 = this.getC(var4);
      NBTTagCompound var6 = this.getCustom(this.getOrCreateCustom(var5, v1_21_R4.Components.block_entity_data));
      NBTTagCompound var7 = this.getOrCreate(var6, "normal_config");
      if (!this.hasKey(var6, "spawn_data")) {
         NBTTagCompound var8 = this.getOrCreate(var6, "spawn_data");
         NBTTagCompound var9 = new NBTTagCompound();
         this.set(var9, "id", "minecraft:" + var2.name().toLowerCase());
         this.set(var8, "entity", var9);
         this.set(var6, "spawn_data", var8);
         this.set(var6, "id", "minecraft:trial_spawner");
         this.set(var7, "ticks_between_spawn", 20.0);
         this.set(var7, "simultaneous_mobs", 1.0);
         this.set(var7, "simultaneous_mobs_added_per_player", 0.5);
         this.set(var7, "total_mobs_added_per_player", 1.0);
         this.set(var7, "total_mobs", 1.0);
      }

      NBTTagList var12 = this.getOrCreate(var7, "spawn_potentials", 10);
      NBTTagCompound var13 = new NBTTagCompound();
      NBTTagCompound var10 = new NBTTagCompound();
      NBTTagCompound var11 = new NBTTagCompound();
      this.set(var11, "id", "minecraft:" + var2.name().toLowerCase());
      this.set(var10, "entity", var11);
      this.set(var13, "data", var10);
      this.set(var13, "weight", var3);
      var12.add(var13);
      this.set(var7, "spawn_potentials", var12);
      this.set(var6, "normal_config", var7);
      return CraftItemStack.asBukkitCopy(var4);
   }

   public org.bukkit.inventory.ItemStack setOminousStrength(org.bukkit.inventory.ItemStack var1, Integer var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      this.set(this.getC(var3), v1_21_R4.Components.ominous_bottle_amplifier, new OminousBottleAmplifier(var2));
      return CraftItemStack.asBukkitCopy(var3);
   }

   @Override
   public org.bukkit.inventory.ItemStack setPotionType(org.bukkit.inventory.ItemStack var1, PotionTypes var2) {
      PotionMeta var3 = (PotionMeta)var1.getItemMeta();
      if (!var3.hasBasePotionType()) {
         var3.setBasePotionType(var2.parsePotionType());
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
   public Optional<PotionTypes> getBasePotion(org.bukkit.inventory.ItemStack var1) {
      PotionMeta var2 = (PotionMeta)var1.getItemMeta();
      return var2.hasBasePotionType() && var2.getBasePotionType().getEffectType() != null
         ? Optional.of(PotionTypes.matchPotionType(var2.getBasePotionType()))
         : Optional.empty();
   }

   @Override
   public org.bukkit.inventory.ItemStack setPathToItem(org.bukkit.inventory.ItemStack var1, String var2) {
      NBTTagCompound var3 = this.getOnyxCompound(var1);
      this.set(var3, "PathToItem", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public org.bukkit.inventory.ItemStack getSpawnerToGive(EntityType var1) {
      org.bukkit.inventory.ItemStack var2 = OnyxShop.getInstance().createItem.createSpawner(var1);
      NBTTagCompound var3 = this.getOnyxCompound(var2);
      this.set(var3, "SpawnerType", var1.toString());
      return this.setOnyxCompound(var2, var3);
   }

   @Override
   public String getPathToItem(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.getS(var2, "PathToItem");
   }

   @Override
   public Boolean hasItemError(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return !this.hasKey(var2, "ItemError") ? false : this.getB(var2, "ItemError");
   }

   @Override
   public String getSpawnerType(org.bukkit.inventory.ItemStack var1) {
      NBTTagCompound var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "SpawnerType") && !this.getS(var2, "SpawnerType").isEmpty() ? this.getS(var2, "SpawnerType") : null;
   }

   private NBTBase get(NBTBase var1, String var2) {
      switch (var1.b()) {
         case 9:
            return ((NBTTagList)var1).c(0);
         case 10:
            return ((NBTTagCompound)var1).a(var2);
         default:
            return null;
      }
   }

   private void remove(NBTTagCompound var1, String var2) {
      var1.r(var2);
   }

   private void remove(NBTTagCompound var1, String var2, String var3) {
      try {
         NBTBase var4 = var1.a(var2);
         if (var4 == null) {
            return;
         }

         switch (var4.b()) {
            case 8:
               JSONObject var5 = JsonUtil.parseJson(this.strip(var4.toString()));
               var5.remove(var3);
               String var6 = var5.toJSONString();
               if (!var6.equals("{}")) {
                  this.set(var1, var2, var6);
               } else {
                  var1.r(var2);
               }
               break;
            case 9:
               ((NBTTagList)var4).d(0);
               if (((NBTTagList)var4).isEmpty()) {
                  var1.r(var2);
               }
               break;
            case 10:
               this.remove((NBTTagCompound)var4, var3);
               if (((NBTTagCompound)var4).j()) {
                  var1.r(var2);
               }
         }
      } catch (ParseException var7) {
         var7.printStackTrace();
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

   private boolean matchCustom(NBTTagCompound var1, NBTTagCompound var2, List<String> var3) {
      for (String var5 : var3) {
         String[] var6 = var5.split("::");
         if (var6.length >= 2) {
            NBTTagCompound var7 = var1;

            for (int var8 = 0; var8 < var6.length - 1; var8++) {
               if (var8 == var6.length - 2) {
                  this.remove(var7, var6[var8], var6[var8 + 1]);
                  break;
               }

               var7 = this.getC(var7, var6[var8]);
            }

            NBTTagCompound var11 = var2;

            for (int var9 = 0; var9 < var6.length - 1; var9++) {
               if (var9 == var6.length - 2) {
                  this.remove(var11, var6[var9], var6[var9 + 1]);
                  break;
               }

               var11 = this.getC(var11, var6[var9]);
            }
         } else {
            try {
               this.remove(var1, var6[0]);
               this.remove(var2, var6[0]);
            } catch (Exception var10) {
            }
         }
      }

      return Objects.equals(var1, var2);
   }

   @Override
   public boolean isSimilar(org.bukkit.inventory.ItemStack var1, org.bukkit.inventory.ItemStack var2, List<String> var3) {
      try {
         ItemStack var4 = CraftItemStack.asNMSCopy(var1);
         ItemStack var5 = CraftItemStack.asNMSCopy(var2);
         if (this.hasTag(var4) && this.hasTag(var5)) {
            Item var6 = (Item)ITEM_ITEM.get(var4);
            if (!(Boolean)ITEM_MATCHER.invoke(var5, var6)) {
               return false;
            }

            PatchedDataComponentMap var7 = this.getC(var4);
            PatchedDataComponentMap var8 = this.getC(var5);
            Reference2ObjectMap var9 = (Reference2ObjectMap)DATA_MAP.get(var7);
            Reference2ObjectMap var10 = (Reference2ObjectMap)DATA_MAP.get(var8);
            HashSet var11 = new HashSet(var9.keySet());
            var11.addAll(var10.keySet());

            for (Object var13x : var11) {
               DataComponentType var13 = (DataComponentType)var13x;
               String var14 = String.valueOf(var13).replace("minecraft:", "");
               if (!var3.contains(var14)) {
                  if (var14.equals("custom_data")) {
                     NBTTagCompound var15 = this.getCustom((Optional<?>)var9.get(var13));
                     NBTTagCompound var16 = this.getCustom((Optional<?>)var10.get(var13));
                     if (!this.matchCustom(
                        var15,
                        var16,
                        var3.stream()
                           .filter(var0 -> var0.startsWith("custom_data::"))
                           .map(var0 -> var0.replace("custom_data::", ""))
                           .collect(Collectors.toList())
                     )) {
                        return false;
                     }
                  } else {
                     if (var13 == v1_21_R4.Components.profile
                        && var9.containsKey(v1_21_R4.Components.profile)
                        && var10.containsKey(v1_21_R4.Components.profile)) {
                        return this.compareProfile(
                           (ResolvableProfile)((Optional)var9.get(var13)).orElse(null), (ResolvableProfile)((Optional)var10.get(var13)).orElse(null)
                        );
                     }

                     if (!Objects.equals(var9.get(var13), var10.get(var13))) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } catch (Exception var17) {
         SendMessage.errorMessage("Failed to match NMS item");
         var17.printStackTrace();
      }

      return var1.isSimilar(var2);
   }

   private boolean compareProfile(ResolvableProfile var1, ResolvableProfile var2) {
      return var1 != null && var2 != null && var1.c().orElse("").equals(var2.c().orElse("")) && SkullUtil.isSimilarTexture(var1.f(), var2.f());
   }

   static {
      try {
         ITEM_ITEM.setAccessible(true);
         ITEM_MATCHER.setAccessible(true);
         COMPONENT_MAP.setAccessible(true);
         DATA_MAP.setAccessible(true);
         CUSTOM_TAG.setAccessible(true);
      } catch (Exception var1) {
         var1.printStackTrace();
         throw new UnsupportedOperationException("Failed to initialize VersionHandler, this is a bug");
      }
   }

   static final class ComponentKey<T> {
      final DataComponentType<T> type;
      final String nbt;

      ComponentKey(DataComponentType<T> var1, String var2) throws NoSuchFieldException , IllegalAccessException{
         this.type = var1;
         this.nbt = var2;
      }
   }

   private static final class Components {
      static final DataComponentType<CustomData> custom_data = DataComponents.b;
      static final DataComponentType<Integer> max_stack_size = DataComponents.c;
      static final DataComponentType<Integer> max_damage = DataComponents.d;
      static final DataComponentType<Integer> damage = DataComponents.e;
      static final DataComponentType<Unit> unbreakable = DataComponents.f;
      static final DataComponentType<IChatBaseComponent> custom_name = DataComponents.g;
      static final DataComponentType<IChatBaseComponent> item_name = DataComponents.h;
      static final DataComponentType<MinecraftKey> item_model = DataComponents.i;
      static final DataComponentType<ItemLore> lore = DataComponents.j;
      static final DataComponentType<EnumItemRarity> rarity = DataComponents.k;
      static final DataComponentType<ItemEnchantments> enchantments = DataComponents.l;
      static final DataComponentType<AdventureModePredicate> can_place_on = DataComponents.m;
      static final DataComponentType<AdventureModePredicate> can_break = DataComponents.n;
      static final DataComponentType<ItemAttributeModifiers> attribute_modifiers = DataComponents.o;
      static final DataComponentType<CustomModelData> custom_model_data = DataComponents.p;
      static final DataComponentType<TooltipDisplay> tooltip_display = DataComponents.q;
      static final DataComponentType<Integer> repair_cost = DataComponents.r;
      static final DataComponentType<Unit> creative_slot_lock = DataComponents.s;
      static final DataComponentType<Boolean> enchantment_glint_override = DataComponents.t;
      static final DataComponentType<Unit> intangible_projectile = DataComponents.u;
      static final DataComponentType<FoodInfo> food = DataComponents.v;
      static final DataComponentType<Consumable> consumable = DataComponents.w;
      static final DataComponentType<UseRemainder> use_remainder = DataComponents.x;
      static final DataComponentType<UseCooldown> use_cooldown = DataComponents.y;
      static final DataComponentType<DamageResistant> damage_resistant = DataComponents.z;
      static final DataComponentType<Tool> tool = DataComponents.A;
      static final DataComponentType<Enchantable> enchantable = DataComponents.C;
      static final DataComponentType<Equippable> equippable = DataComponents.D;
      static final DataComponentType<Repairable> repairable = DataComponents.E;
      static final DataComponentType<Unit> glider = DataComponents.F;
      static final DataComponentType<MinecraftKey> tooltip_style = DataComponents.G;
      static final DataComponentType<DeathProtection> death_protection = DataComponents.H;
      static final DataComponentType<ItemEnchantments> stored_enchantments = DataComponents.J;
      static final DataComponentType<DyedItemColor> dyed_color = DataComponents.K;
      static final DataComponentType<MapItemColor> map_color = DataComponents.L;
      static final DataComponentType<MapId> map_id = DataComponents.M;
      static final DataComponentType<MapDecorations> map_decorations = DataComponents.N;
      static final DataComponentType<MapPostProcessing> map_post_processing = DataComponents.O;
      static final DataComponentType<ChargedProjectiles> charged_projectiles = DataComponents.P;
      static final DataComponentType<BundleContents> bundle_contents = DataComponents.Q;
      static final DataComponentType<PotionContents> potion_contents = DataComponents.R;
      static final DataComponentType<SuspiciousStewEffects> suspicious_stew_effects = DataComponents.T;
      static final DataComponentType<WritableBookContent> writable_book_content = DataComponents.U;
      static final DataComponentType<WrittenBookContent> written_book_content = DataComponents.V;
      static final DataComponentType<ArmorTrim> trim = DataComponents.W;
      static final DataComponentType<DebugStickState> debug_stick_state = DataComponents.X;
      static final DataComponentType<CustomData> entity_data = DataComponents.Y;
      static final DataComponentType<CustomData> bucket_entity_data = DataComponents.Z;
      static final DataComponentType<CustomData> block_entity_data = DataComponents.aa;
      static final DataComponentType<InstrumentComponent> instrument = DataComponents.ab;
      static final DataComponentType<OminousBottleAmplifier> ominous_bottle_amplifier = DataComponents.ad;
      static final DataComponentType<JukeboxPlayable> jukebox_playable = DataComponents.ae;
      static final DataComponentType<List<ResourceKey<IRecipe<?>>>> recipes = DataComponents.ag;
      static final DataComponentType<LodestoneTracker> lodestone_tracker = DataComponents.ah;
      static final DataComponentType<FireworkExplosion> firework_explosion = DataComponents.ai;
      static final DataComponentType<Fireworks> fireworks = DataComponents.aj;
      static final DataComponentType<ResolvableProfile> profile = DataComponents.ak;
      static final DataComponentType<MinecraftKey> note_block_sound = DataComponents.al;
      static final DataComponentType<BannerPatternLayers> banner_patterns = DataComponents.am;
      static final DataComponentType<EnumColor> base_color = DataComponents.an;
      static final DataComponentType<PotDecorations> pot_decorations = DataComponents.ao;
      static final DataComponentType<ItemContainerContents> container = DataComponents.ap;
      static final DataComponentType<BlockItemStateProperties> block_state = DataComponents.aq;
      static final DataComponentType<Bees> bees = DataComponents.ar;
      static final DataComponentType<ChestLock> lock = DataComponents.as;
      static final DataComponentType<SeededContainerLoot> container_loot = DataComponents.at;

      public static DataComponentType<?> valueOf(String var0) throws NoSuchFieldException, IllegalAccessException {
         Field var1 = v1_21_R4.Components.class.getDeclaredField(var0);
         var1.setAccessible(true);
         return (DataComponentType<?>)var1.get(null);
      }

      public static String getName(DataComponentType<?> var0) {
         for (Field var4 : v1_21_R4.Components.class.getDeclaredFields()) {
            if (var4.getType().equals(var0.getClass())) {
               return var4.getName();
            }
         }

         return null;
      }

      public static <T> DataComponentType<T> valueOf(String var0, Class<T> var1) throws NoSuchFieldException , IllegalAccessException{
         Field var2 = v1_21_R4.Components.class.getDeclaredField(var0);
         var2.setAccessible(true);
         return (DataComponentType<T>)var2.get(null);
      }

      private Set<String> getRegistered() {
         Field var1 = v1_21_R4.getFieldByName(BuiltInRegistries.class, "DATA_COMPONENT_TYPE");
         var1.setAccessible(true);

         try {
            return ((Set<?>)ReflectUtil.invokeLegacyVersionMethod("keySet", var1.get(null))).stream().<String>map(var0x -> ((MinecraftKey)var0x).a()).collect(Collectors.toSet());
         } catch (Exception var3) {
            SendMessage.errorMessage("Failed to query data component types");
            var3.printStackTrace();
            return new HashSet<>();
         }
      }
   }

   private static final class NBTSerializer {
      private static final String NBT_FORMAT = "{id:\"minecraft:%1$s\",Count:%2$d,tag:%3$s}";
      private static final String COMP_FORMAT = "{id:\"minecraft:%1$s\",count:%2$d,components:%3$s}";
      private static final Object HOLDER;

      public static String serialize(ItemStack var0) {
         NBTTagCompound var1 = (NBTTagCompound)ItemStack.b.encodeStart(((a)HOLDER).a(DynamicOpsNBT.a), var0).getOrThrow();
         return var1.n("components").toString();
      }

      public static Optional<DataComponentPatch> deserialize(String var0, boolean var1) throws CommandSyntaxException {
         NBTTagCompound var2 = MojangsonParser.a(var0);
         return DataComponentPatch.b
            .parse(((a)HOLDER).a(DynamicOpsNBT.a), var2)
            .resultOrPartial(var0x -> SendMessage.errorMessage("Tried to load invalid item: '{}'", var0x));
      }

      private String convert(org.bukkit.inventory.ItemStack var1, String var2) throws ItemLoadException {
         try {
            String var3 = String.format("{id:\"minecraft:%1$s\",Count:%2$d,tag:%3$s}", var1.getType().getKey().getKey(), 1, var2);
            NBTTagCompound var4 = MojangsonParser.a(var3);
            DataFixer var5 = DataConverterRegistry.a();
            TypeReference var6 = DataConverterTypes.t;
            DynamicOpsNBT var7 = DynamicOpsNBT.a;
            Dynamic var8 = var5.update(var6, new Dynamic(var7, var4), 1343, 4323);
            var4 = (NBTTagCompound)var8.getValue();
            ItemStack var9 = (ItemStack)ItemStack.a((a)HOLDER, var4).get();
            this.convertMeta(var9);
            return serialize(var9);
         } catch (Exception var10) {
            SendMessage.errorMessage("Failed to convert NBT to compound, please see error below:");
            var10.printStackTrace();
            throw new ItemLoadException("Invalid NBT data found while converting to components");
         }
      }

      private String update(NBTUtils.NBTFixer var1, org.bukkit.inventory.ItemStack var2, String var3) throws ItemLoadException {
         try {
            String var4 = String.format("{id:\"minecraft:%1$s\",count:%2$d,components:%3$s}", var2.getType().getKey().getKey(), 1, var3);
            NBTTagCompound var5 = MojangsonParser.a(var4);
            DataFixer var6 = DataConverterRegistry.a();
            TypeReference var7 = DataConverterTypes.t;
            DynamicOpsNBT var8 = DynamicOpsNBT.a;
            Dynamic var9 = var6.update(var7, new Dynamic(var8, var5), var1.from, var1.to);
            var5 = (NBTTagCompound)var9.getValue();
            return serialize((ItemStack)ItemStack.a((a)HOLDER, var5).get());
         } catch (Exception var10) {
            SendMessage.errorMessage("Failed to update component data of item, see error below:");
            var10.printStackTrace();
            throw new ItemLoadException("Unknown error occurred while updating component data");
         }
      }

      private void convertMeta(ItemStack var1) throws IllegalAccessException {
         PatchedDataComponentMap var2 = (PatchedDataComponentMap)v1_21_R4.COMPONENT_MAP.get(var1);
         ItemLore var3 = (ItemLore)var2.a(v1_21_R4.Components.lore);
         if (var3 != null) {
            List<IChatBaseComponent> var4 = var3.a();
            var3 = new ItemLore(var4.stream().map(var0 -> CraftChatMessage.fromJSONOrString(var0.getString())).collect(Collectors.toList()));
            var2.b(v1_21_R4.Components.lore, var3);
         }

         IChatBaseComponent var6 = (IChatBaseComponent)var2.a(v1_21_R4.Components.custom_name);
         if (var6 != null) {
            var2.b(v1_21_R4.Components.custom_name, CraftChatMessage.fromJSONOrString(var6.getString()));
         }
      }

      private static ItemStack removeEmpty(ItemStack var0) throws IllegalAccessException {
         PatchedDataComponentMap var1 = (PatchedDataComponentMap)v1_21_R4.COMPONENT_MAP.get(var0);
         if (var1.a(v1_21_R4.Components.stored_enchantments) == null) {
            var1.b(v1_21_R4.Components.stored_enchantments, null);
         }

         if (var1.a(v1_21_R4.Components.enchantments) == null) {
            var1.b(v1_21_R4.Components.enchantments, null);
         }

         if (var1.a(v1_21_R4.Components.recipes) == null) {
            var1.b(v1_21_R4.Components.recipes, null);
         }

         if (var1.a(v1_21_R4.Components.firework_explosion) == null) {
            var1.b(v1_21_R4.Components.firework_explosion, null);
         }

         if (var1.a(v1_21_R4.Components.fireworks) == null) {
            var1.b(v1_21_R4.Components.fireworks, null);
         }

         if (var1.a(v1_21_R4.Components.profile) == null) {
            var1.b(v1_21_R4.Components.profile, null);
         }

         if (var1.a(v1_21_R4.Components.instrument) == null) {
            var1.b(v1_21_R4.Components.instrument, null);
         }

         if (var1.a(v1_21_R4.Components.trim) == null) {
            var1.b(v1_21_R4.Components.trim, null);
         }

         if (var1.a(v1_21_R4.Components.lore) == null) {
            var1.b(v1_21_R4.Components.lore, null);
         }

         if (var1.a(v1_21_R4.Components.custom_name) == null) {
            var1.b(v1_21_R4.Components.custom_name, null);
         }

         if (var1.a(v1_21_R4.Components.item_name) == null) {
            var1.b(v1_21_R4.Components.item_name, null);
         }

         if (var1.a(v1_21_R4.Components.suspicious_stew_effects) == null) {
            var1.b(v1_21_R4.Components.suspicious_stew_effects, null);
         }

         v1_21_R4.COMPONENT_MAP.set(var0, var1);
         return var0;
      }

      private static DataComponentPatch parseAndApply(DataComponentPatch var0, String var1, boolean var2) throws Throwable {
         Map var3 = (Map)new Gson().fromJson(var1, Map.class);
         if (var2 && var3.containsKey("display")) {
            var3.remove("display");
         }

         ItemMeta var4 = SerializableMeta.deserialize(var3);
         net.minecraft.core.component.DataComponentPatch.a var5 = buildMeta(var4);
         var5.copy(var0);
         return var5.a();
      }

      private static net.minecraft.core.component.DataComponentPatch.a buildMeta(ItemMeta var0) throws ClassNotFoundException , InstantiationException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, InvocationTargetException{
         Class var1 = Class.forName("org.bukkit.craftbukkit." + ServerInfo.getVersion().name() + ".inventory.CraftMetaItem$Applicator");
         Object var2 = var1.newInstance();
         Method var3 = var0.getClass().getDeclaredMethod("applyToItem", var1);
         var3.setAccessible(true);
         var3.invoke(var0, var2);
         Field var4 = var1.getDeclaredField("builder");
         var4.setAccessible(true);
         return (net.minecraft.core.component.DataComponentPatch.a)var4.get(var2);
      }

      static {
         try {
            Method var0 = CraftServer.class.getDeclaredMethod("getServer");
            var0.setAccessible(true);
            Object var1 = var0.invoke(Bukkit.getServer());
            Method var2 = CraftRegistry.class.getDeclaredMethod("getMinecraftRegistry");
            var2.setAccessible(true);
            HOLDER = var2.invoke(var1);
         } catch (Exception var3) {
            SendMessage.errorMessage("Failed to init item serializer");
            var3.printStackTrace();
            throw new NullPointerException("Failed to init item serializer");
         }
      }
   }
}
