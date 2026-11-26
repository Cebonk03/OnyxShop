package com.onyxshop.versions.v26_2;

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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.DataComponentPatch.Builder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.LockCode;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.Rarity;
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
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.SerializableMeta;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class v26_2 implements VersionHandler {
   private static final Field CUSTOM_TAG = getFieldByType(CustomData.class, CompoundTag.class);
   private static final Field COMPONENT_MAP = getFieldByType(ItemStack.class, PatchedDataComponentMap.class);
   private static final Field DATA_MAP = getFieldByType(PatchedDataComponentMap.class, Reference2ObjectMap.class);

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
         DataComponentType var5 = v26_2.Components.valueOf(var2, var2.getClass());
         return var4.hasNonDefault(var5);
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
      Component var2 = CraftChatMessage.fromString(var1, true)[0];
      return CraftChatMessage.toJSON(var2);
   }

   public void setLived(Item var1) {
      ((CraftItem)var1).getHandle().pickupDelay = -32768;
   }

   public org.bukkit.inventory.ItemStack setComponent(org.bukkit.inventory.ItemStack var1, String var2, Object var3) {
      ItemStack var4 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      if (this.hasTag(var4)) {
         PatchedDataComponentMap var5 = this.getC(var4);

         try {
            var5.set(v26_2.Components.valueOf(var2, Object.class), var3);
         } catch (Exception var7) {
            SendMessage.warnMessage("Unsupported component tag found for " + var2 + " while injecting item data, this is a bug, please report");
         }
      }

      return CraftItemStack.asCraftMirror(var4);
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisplayname(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      if (this.hasTag(var3)) {
         PatchedDataComponentMap var4 = this.getC(var3);
         this.set(var4, DataComponents.CUSTOM_NAME, CraftChatMessage.fromStringOrNull(this.toNBT(var2)));
      }

      return CraftItemStack.asCraftMirror(var3);
   }

   @Override
   public String getNBTDataAsString(org.bukkit.inventory.ItemStack var1, boolean var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      if (this.hasTag(var3)) {
         PatchedDataComponentMap var4 = this.getC(var3);
         this.setAccessible(var4);
         if (!var2 && var4 != null) {
            this.remove(var4, DataComponents.ENCHANTMENTS);
            this.remove(var4, DataComponents.STORED_ENCHANTMENTS);
            this.remove(var4, DataComponents.FIREWORK_EXPLOSION);
            this.remove(var4, DataComponents.FIREWORKS);
            this.remove(var4, DataComponents.INSTRUMENT);
            this.remove(var4, DataComponents.TRIM);
            this.remove(var4, DataComponents.LORE);
            this.remove(var4, DataComponents.CUSTOM_NAME);
            this.remove(var4, DataComponents.SUSPICIOUS_STEW_EFFECTS);
            this.remove(var4, DataComponents.RECIPES);
            this.remove(var4, DataComponents.DYED_COLOR);
            this.remove(var4, DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
            this.remove(var4, DataComponents.PROFILE);
            this.removeCustom(var4, "ONYX");
         }

         try {
            String var5 = v26_2.NBTSerializer.serialize(var3);
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
      String var3 = CraftChatMessage.fromComponent((Component)this.<ItemLore>getOrNull(var2, DataComponents.LORE).lines().get(0));
      return var3.substring(1).substring(0, var3.length() - 1);
   }

   @Override
   public Object getNMSLore(org.bukkit.inventory.ItemStack var1) {
      ItemStack var2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      PatchedDataComponentMap var3 = this.getC(var2);
      List var4 = this.<ItemLore>getOrNull(var3, DataComponents.LORE).lines();
      return var4 != null ? var4 : new ArrayList();
   }

   @Override
   public org.bukkit.inventory.ItemStack applyNMSLore(org.bukkit.inventory.ItemStack var1, Object var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(var1));
      if (this.hasTag(var3)) {
         PatchedDataComponentMap var4 = this.getC(var3);
         this.set(var4, DataComponents.LORE, new ItemLore((List)var2));
      }

      return CraftItemStack.asCraftMirror(var3);
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
      ServerPlayer var3 = ((CraftPlayer)var1).getHandle();
      var3.connection
         .send(
            new ClientboundOpenScreenPacket(
               var3.containerMenu.containerId,
               var3.containerMenu.getType(),
               ServerInfo.supportsPaper() ? (Component)AdventureUtil.asVanilla(var2) : CraftChatMessage.fromStringOrNull(var2.getLegacy())
            )
         );
      var1.updateInventory();
   }

   @Override
   public org.bukkit.inventory.ItemStack setNBTString(org.bukkit.inventory.ItemStack var1, String var2, String var3) {
      ItemStack var4 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var5 = this.getC(var4);
      CompoundTag var6 = this.getCustom(var5);
      String[] var7 = var2.split("::");
      CompoundTag var8 = var6;

      for (int var9 = 0; var9 < var7.length - 1; var9++) {
         String var10 = var7[var9];
         CompoundTag var11;
         if (this.hasKey(var8, var10)) {
            var11 = (CompoundTag)var8.getCompound(var10).get();
         } else {
            var11 = new CompoundTag();
            this.set(var8, var10, var11);
         }

         var8 = var11;
      }

      this.set(var8, var7[var7.length - 1], var3);
      return CraftItemStack.asCraftMirror(var4);
   }

   @Override
   public String getNBTString(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var4 = this.getC(var3);
      CompoundTag var5 = this.getCustom(var4);
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
         CompoundTag var6 = this.getCustom(var5);
         CompoundTag var7 = this.getOrCreate(var6, var2.split("::")[0]);
         this.set(var7, var2.split("::")[1], var3);
         this.set(var6, var2.split("::")[0], var7);
      } else {
         CompoundTag var8 = this.getCustom(var5);
         this.set(var8, var2, var3);
      }

      return CraftItemStack.asCraftMirror(var4);
   }

   @Override
   public Integer getNBTInt(org.bukkit.inventory.ItemStack var1, String var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var4 = this.getC(var3);
      String[] var5 = var2.split("::");
      if (var5[0].equals("custom_data")) {
         CompoundTag var8 = this.getCustom(var4);

         for (int var7 = 1; var7 < var5.length - 1; var7++) {
            if (!this.hasKey(var8, var5[var7])) {
               return null;
            }

            var8 = this.getC(var8, var5[var7]);
         }

         return this.hasKey(var8, var5[var5.length - 1]) ? this.getI(var8, var5[var5.length - 1]) : null;
      } else {
         Tag var6 = this.getC(var4, var2);
         return var6 != null ? (Integer)var6.asInt().orElse(null) : null;
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack setPages(org.bukkit.inventory.ItemStack var1, String var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      this.set(var3, "Pages", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getPages(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.getS(var2, "Pages");
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtString(org.bukkit.inventory.ItemStack var1, String var2, String var3) {
      CompoundTag var4 = this.getOnyxCompound(var1);
      this.set(var4, var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public String getOnyxNbtString(org.bukkit.inventory.ItemStack var1, String var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      return this.hasKey(var3, var2) ? this.getS(var3, var2) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtInt(org.bukkit.inventory.ItemStack var1, String var2, Integer var3) {
      CompoundTag var4 = this.getOnyxCompound(var1);
      this.set(var4, var2, var3);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public Integer getOnyxNbtInt(org.bukkit.inventory.ItemStack var1, String var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      return this.hasKey(var3, var2) ? this.getI(var3, var2) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setIndex(org.bukkit.inventory.ItemStack var1, Integer var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      this.set(var3, "Index", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public Integer getIndex(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "Index") ? this.getI(var2, "Index") : -1;
   }

   @Override
   public org.bukkit.inventory.ItemStack setAction(org.bukkit.inventory.ItemStack var1, String var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      this.set(var3, "Action", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getAction(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "Action") ? this.getS(var2, "Action") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setEditOption(org.bukkit.inventory.ItemStack var1, String var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      this.set(var3, "EditOption", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public String getEditOption(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "EditOption") ? this.getS(var2, "EditOption") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack getShopAdminItem(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      this.set(var2, "AdminItem", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isShopAdminItem(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.getB(var2, "AdminItem");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTtag(org.bukkit.inventory.ItemStack var1, String var2, boolean var3) throws ItemLoadException {
      ItemStack var4 = CraftItemStack.asNMSCopy(var1);

      try {
         Optional<DataComponentPatch> var5 = v26_2.NBTSerializer.deserialize(var2, var3);
         return (org.bukkit.inventory.ItemStack)var5.<CraftItemStack>map(var1x -> {
            var4.applyComponents(var1x);
            return CraftItemStack.asCraftMirror(var4);
         }).orElse(null);
      } catch (Throwable var8) {
         StringWriter var6 = new StringWriter();
         PrintWriter var7 = new PrintWriter(var6);
         var8.printStackTrace(var7);
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get() + ": " + var6.toString());
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTCommands(org.bukkit.inventory.ItemStack var1, List<String> var2, CommandType var3) {
      CompoundTag var4 = this.getOnyxCompound(var1);
      ListTag var5 = this.getOrCreate(var4, var3.getCompound(), 8);
      var2.forEach(var1x -> var5.add(StringTag.valueOf(var1x.startsWith("/") ? var1x.substring(1) : var1x)));
      this.set(var4, var3.getCompound(), var5);
      return this.setOnyxCompound(var1, var4);
   }

   @Override
   public boolean hasNBTCommand(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return !this.getOrCreate(var2, "buy-commands", 8).isEmpty() || !this.getOrCreate(var2, "sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(org.bukkit.inventory.ItemStack var1, CommandType var2) {
      ListTag var3 = this.getOrCreate(this.getOnyxCompound(var1), var2.getCompound(), 8);
      return var3.stream().<String>map(Tag::toString).collect(Collectors.toList());
   }

   private CompoundTag getC(CompoundTag var1, String var2) {
      return var1.getCompoundOrEmpty(var2);
   }

   private Tag getC(PatchedDataComponentMap var1, String var2) {
      try {
         DataComponentType var3 = v26_2.Components.valueOf(var2);
         return this.encodeC(var3, var1);
      } catch (Exception var4) {
         return null;
      }
   }

   private <T> Tag encodeC(DataComponentType<T> var1, PatchedDataComponentMap var2) {
      T var3 = (T)var2.get(var1);
      return (Tag)var1.codecOrThrow().encodeStart(NbtOps.INSTANCE, var3).getOrThrow();
   }

   private CompoundTag getC(ListTag var1, int var2) {
      return var1.getCompoundOrEmpty(var2);
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

   private CompoundTag getCustom(PatchedDataComponentMap var1) {
      return this.getCustom(var1, false);
   }

   private CompoundTag getCustom(PatchedDataComponentMap var1, boolean var2) {
      try {
         CustomData var3 = this.getOrNull(var1, DataComponents.CUSTOM_DATA);
         if (var3 == null) {
            if (var2) {
               return null;
            }

            CompoundTag var4 = new CompoundTag();

            try {
               this.addCustom(var1, DataComponents.CUSTOM_DATA, var4);
            } catch (Exception var6) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               var6.printStackTrace();
            }

            var3 = this.getOrNull(var1, DataComponents.CUSTOM_DATA);
         }

         return this.getCustom(var3);
      } catch (IllegalAccessException var7) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         var7.printStackTrace();
         return null;
      }
   }

   private CompoundTag getCustom(CustomData var1) throws IllegalAccessException {
      return (CompoundTag)CUSTOM_TAG.get(var1);
   }

   private CompoundTag getCustom(Optional<?> var1) throws IllegalAccessException {
      return var1 != null && var1.isPresent() ? this.getCustom((CustomData)var1.get()).copy() : new CompoundTag();
   }

   private CompoundTag getClonedCustom(PatchedDataComponentMap var1) {
      return this.getClonedCustom(var1, false);
   }

   private CompoundTag getClonedCustom(PatchedDataComponentMap var1, boolean var2) {
      try {
         CustomData var3 = this.getOrNull(var1, DataComponents.CUSTOM_DATA);
         if (var3 == null) {
            if (var2) {
               return null;
            }

            CompoundTag var4 = new CompoundTag();

            try {
               this.addCustom(var1, DataComponents.CUSTOM_DATA, var4);
            } catch (Exception var6) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               var6.printStackTrace();
            }

            var3 = this.getOrNull(var1, DataComponents.CUSTOM_DATA);
         }

         return this.getClonedCustom(var1, var3);
      } catch (Exception var7) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         var7.printStackTrace();
         return null;
      }
   }

   private CompoundTag getClonedCustom(PatchedDataComponentMap var1, CustomData var2) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException {
      Field var3 = CustomData.class.getDeclaredField("tag");
      var3.setAccessible(true);
      CompoundTag var4 = (CompoundTag)var3.get(var2);
      Method var5 = CompoundTag.class.getDeclaredMethod("copy");
      var5.setAccessible(true);
      CompoundTag var6 = (CompoundTag)var5.invoke(var4);
      Constructor var7 = Class.forName("net.minecraft.world.item.component.CustomData").getDeclaredConstructor(CompoundTag.class);
      var7.setAccessible(true);
      CustomData var8 = (CustomData)var7.newInstance(var6);
      this.set(var1, DataComponents.CUSTOM_DATA, var8);
      return var6;
   }

   private void addCustom(PatchedDataComponentMap var1, DataComponentType<CustomData> var2, CompoundTag var3) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      Constructor var4 = CustomData.class.getDeclaredConstructor(var3.getClass());
      var4.setAccessible(true);
      CustomData var5 = (CustomData)var4.newInstance(var3);
      this.set(var1, var2, var5);
   }

   private void removeCustom(PatchedDataComponentMap var1, String var2) {
      CompoundTag var3 = this.getCustom(var1, true);
      if (var3 != null) {
         var3.remove(var2);
      }
   }

   private String getS(CompoundTag var1, String var2) {
      return var1.getStringOr(var2, "");
   }

   private double getD(CompoundTag var1, String var2) {
      return var1.getDoubleOr(var2, 0.0);
   }

   private boolean getB(CompoundTag var1, String var2) {
      return var1.getBooleanOr(var2, false);
   }

   private int getI(CompoundTag var1, String var2) {
      return var1.getIntOr(var2, 0);
   }

   private ListTag getOrCreate(CompoundTag var1, String var2, int var3) {
      return var1.getListOrEmpty(var2);
   }

   private CompoundTag getOrCreate(CompoundTag var1, String var2) {
      return var1.getCompoundOrEmpty(var2);
   }

   private boolean hasKey(CompoundTag var1, String var2) {
      return var1.contains(var2);
   }

   private <T> T getOrNull(PatchedDataComponentMap var1, DataComponentType<? extends T> var2) {
      return (T)var1.get(var2);
   }

   private CustomData getOrCreateCustom(PatchedDataComponentMap var1, DataComponentType<CustomData> var2) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      CustomData var3 = this.getOrNull(var1, var2);
      if (var3 == null) {
         this.addCustom(var1, var2, new CompoundTag());
         return this.getOrNull(var1, var2);
      } else {
         return var3;
      }
   }

   private boolean hasTag(ItemStack var1) {
      return !var1.isEmpty();
   }

   private String getString(CompoundTag var1, String var2) {
      return var1.getStringOr(var2, "");
   }

   private void setString(CompoundTag var1, String var2, String var3) {
      var1.putString(var2, var3);
   }

   private void set(CompoundTag var1, String var2, ListTag var3) {
      var1.put(var2, var3);
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
         Method var2 = var1.getClass().getDeclaredMethod("ensureMapOwnership");
         var2.setAccessible(true);
         var2.invoke(var1);
      } catch (Exception var3) {
         SendMessage.errorMessage("Failed to make data map accessible");
         var3.printStackTrace();
      }
   }

   private void set(CompoundTag var1, String var2, boolean var3) {
      var1.putBoolean(var2, var3);
   }

   private void set(CompoundTag var1, String var2, double var3) {
      var1.putDouble(var2, var3);
   }

   private void set(CompoundTag var1, String var2, CompoundTag var3) {
      var1.put(var2, var3);
   }

   private <T> T set(PatchedDataComponentMap var1, DataComponentType<T> var2, T var3) {
      return (T)var1.set(var2, var3);
   }

   private void set(CompoundTag var1, String var2, String var3) {
      var1.putString(var2, var3);
   }

   private void set(CompoundTag var1, String var2, Integer var3) {
      var1.putInt(var2, var3);
   }

   private void merge(CompoundTag var1, CompoundTag var2) {
      var1.merge(var2);
   }

   private void set(ItemStack var1, PatchedDataComponentMap var2) {
      try {
         COMPONENT_MAP.set(var1, var2);
      } catch (IllegalAccessException var4) {
         SendMessage.logDebugMessage("Failed to retrieve compounds from item");
         var4.printStackTrace();
      }
   }

   private CompoundTag getOnyxCompound(org.bukkit.inventory.ItemStack var1) {
      ItemStack var2 = CraftItemStack.asNMSCopy(var1);
      CompoundTag var3 = this.getCustom(this.getC(var2));
      return this.getOrCreate(var3, "ONYX");
   }

   private org.bukkit.inventory.ItemStack setOnyxCompound(org.bukkit.inventory.ItemStack var1, CompoundTag var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      PatchedDataComponentMap var4 = this.getC(var3);
      CompoundTag var5 = this.getCustom(var4);
      this.set(var5, "ONYX", var2);
      return CraftItemStack.asCraftMirror(var3);
   }

   @Override
   public String getTitle(Inventory var1) {
      return "";
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisabledBackButton(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      this.set(var2, "disabledBackButton", true);
      return this.setOnyxCompound(var1, var2);
   }

   @Override
   public boolean isDisabledBackButton(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "disabledBackButton") && this.getB(var2, "disabledBackButton");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTPrices(org.bukkit.inventory.ItemStack var1, String var2) {
      CompoundTag var3 = this.getOnyxCompound(var1);
      CompoundTag var4 = this.getOrCreate(var3, "Prices");
      this.set(var4, "buy", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".buy"));
      this.set(var4, "sell", ConfigManager.getShop(var2.split("\\.")[0]).getDouble(var2 + ".sell"));
      this.set(var3, "Prices", var4);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public double getBuyPrice(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      CompoundTag var3 = this.getOrCreate(var2, "Prices");
      return this.getD(var3, "buy");
   }

   @Override
   public double getSellPrice(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      CompoundTag var3 = this.getOrCreate(var2, "Prices");
      return this.getD(var3, "sell");
   }

   @Override
   public org.bukkit.inventory.ItemStack setItemErrorToItem(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
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
      CompoundTag var4 = this.getOnyxCompound(var1);
      this.set(var4, "SpawnerType", var2);
      ItemStack var5 = CraftItemStack.asNMSCopy(this.setOnyxCompound(var1, var4));
      PatchedDataComponentMap var6 = this.getC(var5);
      TooltipDisplay var7 = this.getOrNull(var6, DataComponents.TOOLTIP_DISPLAY);
      var7.withHidden(DataComponents.BLOCK_ENTITY_DATA, true);
      return this.setOnyxCompound(var1, var4);
   }

   public org.bukkit.inventory.ItemStack setOminousStrength(org.bukkit.inventory.ItemStack var1, Integer var2) {
      ItemStack var3 = CraftItemStack.asNMSCopy(var1);
      this.set(this.getC(var3), DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(var2));
      return CraftItemStack.asCraftMirror(var3);
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
      CompoundTag var3 = this.getOnyxCompound(var1);
      this.set(var3, "PathToItem", var2);
      return this.setOnyxCompound(var1, var3);
   }

   @Override
   public org.bukkit.inventory.ItemStack getSpawnerToGive(EntityType var1) {
      org.bukkit.inventory.ItemStack var2 = OnyxShop.getInstance().createItem.createSpawner(var1);
      CompoundTag var3 = this.getOnyxCompound(var2);
      this.set(var3, "SpawnerType", var1.toString());
      return this.setOnyxCompound(var2, var3);
   }

   @Override
   public String getPathToItem(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.getS(var2, "PathToItem");
   }

   @Override
   public Boolean hasItemError(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return !this.hasKey(var2, "ItemError") ? false : this.getB(var2, "ItemError");
   }

   @Override
   public String getSpawnerType(org.bukkit.inventory.ItemStack var1) {
      CompoundTag var2 = this.getOnyxCompound(var1);
      return this.hasKey(var2, "SpawnerType") && !this.getS(var2, "SpawnerType").isEmpty() ? this.getS(var2, "SpawnerType") : null;
   }

   private Tag get(Tag var1, String var2) {
      switch (var1.getId()) {
         case 9:
            return ((ListTag)var1).get(0);
         case 10:
            return ((CompoundTag)var1).get(var2);
         default:
            return null;
      }
   }

   private void remove(CompoundTag var1, String var2) {
      var1.remove(var2);
   }

   private void remove(CompoundTag var1, String var2, String var3) {
      try {
         Tag var4 = var1.get(var2);
         if (var4 == null) {
            return;
         }

         switch (var4.getId()) {
            case 8:
               JSONObject var5 = JsonUtil.parseJson(this.strip(var4.toString()));
               var5.remove(var3);
               String var6 = var5.toJSONString();
               if (!var6.equals("{}")) {
                  this.set(var1, var2, var6);
               } else {
                  var1.remove(var2);
               }
               break;
            case 9:
               ((ListTag)var4).remove(0);
               if (((ListTag)var4).isEmpty()) {
                  var1.remove(var2);
               }
               break;
            case 10:
               this.remove((CompoundTag)var4, var3);
               if (((CompoundTag)var4).isEmpty()) {
                  var1.remove(var2);
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

   private boolean matchCustom(CompoundTag var1, CompoundTag var2, List<String> var3) {
      for (String var5 : var3) {
         String[] var6 = var5.split("::");
         if (var6.length >= 2) {
            CompoundTag var7 = var1;

            for (int var8 = 0; var8 < var6.length - 1; var8++) {
               if (var8 == var6.length - 2) {
                  this.remove(var7, var6[var8], var6[var8 + 1]);
                  break;
               }

               var7 = this.getC(var7, var6[var8]);
            }

            CompoundTag var11 = var2;

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
            if (!var4.is(var5.getItem())) {
               return false;
            }

            PatchedDataComponentMap var6 = this.getC(var4);
            PatchedDataComponentMap var7 = this.getC(var5);
            Reference2ObjectMap var8 = (Reference2ObjectMap)DATA_MAP.get(var6);
            Reference2ObjectMap var9 = (Reference2ObjectMap)DATA_MAP.get(var7);
            HashSet var10 = new HashSet(var8.keySet());
            var10.addAll(var9.keySet());

            for (Object var12x : var10) {
               DataComponentType var12 = (DataComponentType)var12x;
               String var13 = String.valueOf(var12).replace("minecraft:", "");
               if (!var3.contains(var13)) {
                  if (var13.equals("custom_data")) {
                     CompoundTag var14 = this.getCustom((Optional<?>)var8.get(var12));
                     CompoundTag var15 = this.getCustom((Optional<?>)var9.get(var12));
                     if (!this.matchCustom(
                        var14,
                        var15,
                        var3.stream()
                           .filter(var0 -> var0.startsWith("custom_data::"))
                           .map(var0 -> var0.replace("custom_data::", ""))
                           .collect(Collectors.toList())
                     )) {
                        return false;
                     }
                  } else {
                     if (var12 == DataComponents.PROFILE && var8.containsKey(DataComponents.PROFILE) && var9.containsKey(DataComponents.PROFILE)) {
                        return this.compareProfile(
                           (ResolvableProfile)((Optional)var8.get(var12)).orElse(null), (ResolvableProfile)((Optional)var9.get(var12)).orElse(null)
                        );
                     }

                     if (!Objects.equals(var8.get(var12), var9.get(var12))) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } catch (Exception var16) {
         SendMessage.errorMessage("Failed to match NMS item");
         var16.printStackTrace();
      }

      return var1.isSimilar(var2);
   }

   private boolean compareProfile(ResolvableProfile var1, ResolvableProfile var2) {
      return var1 != null
         && var2 != null
         && var1.name().orElse("").equals(var2.name().orElse(""))
         && SkullUtil.isSimilarTexture(var1.partialProfile(), var2.partialProfile());
   }

   static {
      try {
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
      static final DataComponentType<CustomData> custom_data = DataComponents.CUSTOM_DATA;
      static final DataComponentType<Integer> max_stack_size = DataComponents.MAX_STACK_SIZE;
      static final DataComponentType<Integer> max_damage = DataComponents.MAX_DAMAGE;
      static final DataComponentType<Integer> damage = DataComponents.DAMAGE;
      static final DataComponentType<Unit> unbreakable = DataComponents.UNBREAKABLE;
      static final DataComponentType<Component> custom_name = DataComponents.CUSTOM_NAME;
      static final DataComponentType<Component> item_name = DataComponents.ITEM_NAME;
      static final DataComponentType<ItemLore> lore = DataComponents.LORE;
      static final DataComponentType<Rarity> rarity = DataComponents.RARITY;
      static final DataComponentType<ItemEnchantments> enchantments = DataComponents.ENCHANTMENTS;
      static final DataComponentType<AdventureModePredicate> can_place_on = DataComponents.CAN_PLACE_ON;
      static final DataComponentType<AdventureModePredicate> can_break = DataComponents.CAN_BREAK;
      static final DataComponentType<ItemAttributeModifiers> attribute_modifiers = DataComponents.ATTRIBUTE_MODIFIERS;
      static final DataComponentType<CustomModelData> custom_model_data = DataComponents.CUSTOM_MODEL_DATA;
      static final DataComponentType<Identifier> item_model = DataComponents.ITEM_MODEL;
      static final DataComponentType<TooltipDisplay> tooltip_display = DataComponents.TOOLTIP_DISPLAY;
      static final DataComponentType<Integer> repair_cost = DataComponents.REPAIR_COST;
      static final DataComponentType<Unit> creative_slot_lock = DataComponents.CREATIVE_SLOT_LOCK;
      static final DataComponentType<Boolean> enchantment_glint_override = DataComponents.ENCHANTMENT_GLINT_OVERRIDE;
      static final DataComponentType<Unit> intangible_projectile = DataComponents.INTANGIBLE_PROJECTILE;
      static final DataComponentType<FoodProperties> food = DataComponents.FOOD;
      static final DataComponentType<Consumable> consumable = DataComponents.CONSUMABLE;
      static final DataComponentType<UseRemainder> use_remainder = DataComponents.USE_REMAINDER;
      static final DataComponentType<UseCooldown> use_cooldown = DataComponents.USE_COOLDOWN;
      static final DataComponentType<DamageResistant> damage_resistant = DataComponents.DAMAGE_RESISTANT;
      static final DataComponentType<Tool> tool = DataComponents.TOOL;
      static final DataComponentType<Enchantable> enchantable = DataComponents.ENCHANTABLE;
      static final DataComponentType<Equippable> equippable = DataComponents.EQUIPPABLE;
      static final DataComponentType<Repairable> repairable = DataComponents.REPAIRABLE;
      static final DataComponentType<Unit> glider = DataComponents.GLIDER;
      static final DataComponentType<DeathProtection> death_protection = DataComponents.DEATH_PROTECTION;
      static final DataComponentType<ItemEnchantments> stored_enchantments = DataComponents.STORED_ENCHANTMENTS;
      static final DataComponentType<DyedItemColor> dyed_color = DataComponents.DYED_COLOR;
      static final DataComponentType<MapItemColor> map_color = DataComponents.MAP_COLOR;
      static final DataComponentType<MapId> map_id = DataComponents.MAP_ID;
      static final DataComponentType<MapDecorations> map_decorations = DataComponents.MAP_DECORATIONS;
      static final DataComponentType<MapPostProcessing> map_post_processing = DataComponents.MAP_POST_PROCESSING;
      static final DataComponentType<ChargedProjectiles> charged_projectiles = DataComponents.CHARGED_PROJECTILES;
      static final DataComponentType<BundleContents> bundle_contents = DataComponents.BUNDLE_CONTENTS;
      static final DataComponentType<PotionContents> potion_contents = DataComponents.POTION_CONTENTS;
      static final DataComponentType<SuspiciousStewEffects> suspicious_stew_effects = DataComponents.SUSPICIOUS_STEW_EFFECTS;
      static final DataComponentType<WritableBookContent> writable_book_content = DataComponents.WRITABLE_BOOK_CONTENT;
      static final DataComponentType<WrittenBookContent> written_book_content = DataComponents.WRITTEN_BOOK_CONTENT;
      static final DataComponentType<ArmorTrim> trim = DataComponents.TRIM;
      static final DataComponentType<DebugStickState> debug_stick_state = DataComponents.DEBUG_STICK_STATE;
      static final DataComponentType<TypedEntityData<net.minecraft.world.entity.EntityType<?>>> entity_data = DataComponents.ENTITY_DATA;
      static final DataComponentType<CustomData> bucket_entity_data = DataComponents.BUCKET_ENTITY_DATA;
      static final DataComponentType<TypedEntityData<BlockEntityType<?>>> block_entity_data = DataComponents.BLOCK_ENTITY_DATA;
      static final DataComponentType<InstrumentComponent> instrument = DataComponents.INSTRUMENT;
      static final DataComponentType<OminousBottleAmplifier> ominous_bottle_amplifier = DataComponents.OMINOUS_BOTTLE_AMPLIFIER;
      static final DataComponentType<JukeboxPlayable> jukebox_playable = DataComponents.JUKEBOX_PLAYABLE;
      static final DataComponentType<List<ResourceKey<Recipe<?>>>> recipes = DataComponents.RECIPES;
      static final DataComponentType<LodestoneTracker> lodestone_tracker = DataComponents.LODESTONE_TRACKER;
      static final DataComponentType<FireworkExplosion> firework_explosion = DataComponents.FIREWORK_EXPLOSION;
      static final DataComponentType<Fireworks> fireworks = DataComponents.FIREWORKS;
      static final DataComponentType<ResolvableProfile> profile = DataComponents.PROFILE;
      static final DataComponentType<BannerPatternLayers> banner_patterns = DataComponents.BANNER_PATTERNS;
      static final DataComponentType<DyeColor> base_color = DataComponents.BASE_COLOR;
      static final DataComponentType<PotDecorations> pot_decorations = DataComponents.POT_DECORATIONS;
      static final DataComponentType<ItemContainerContents> container = DataComponents.CONTAINER;
      static final DataComponentType<BlockItemStateProperties> block_state = DataComponents.BLOCK_STATE;
      static final DataComponentType<Bees> bees = DataComponents.BEES;
      static final DataComponentType<LockCode> lock = DataComponents.LOCK;
      static final DataComponentType<SeededContainerLoot> container_loot = DataComponents.CONTAINER_LOOT;

      public static DataComponentType<?> valueOf(String var0) throws NoSuchFieldException, IllegalAccessException {
         Field var1 = v26_2.Components.class.getDeclaredField(var0);
         var1.setAccessible(true);
         return (DataComponentType<?>)var1.get(null);
      }

      public static String getName(DataComponentType<?> var0) {
         for (Field var4 : v26_2.Components.class.getDeclaredFields()) {
            if (var4.getType().equals(var0.getClass())) {
               return var4.getName();
            }
         }

         return null;
      }

      public static <T> DataComponentType<T> valueOf(String var0, Class<T> var1) throws NoSuchFieldException , IllegalAccessException{
         Field var2 = v26_2.Components.class.getDeclaredField(var0);
         var2.setAccessible(true);
         return (DataComponentType<T>)var2.get(null);
      }

      private Set<String> getRegistered() {
         Field var1 = v26_2.getFieldByName(BuiltInRegistries.class, "DATA_COMPONENT_TYPE");
         var1.setAccessible(true);

         try {
            return ((Set<?>)ReflectUtil.invokeLegacyVersionMethod("keySet", var1.get(null))).stream().<String>map(var0x -> ((Identifier)var0x).getPath()).collect(Collectors.toSet());
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
         CompoundTag var1 = (CompoundTag)ItemStack.CODEC.encodeStart(((Provider)HOLDER).createSerializationContext(NbtOps.INSTANCE), var0).getOrThrow();
         return var1.getCompoundOrEmpty("components").toString();
      }

      public static Optional<DataComponentPatch> deserialize(String var0, boolean var1) throws CommandSyntaxException {
         CompoundTag var2 = TagParser.parseCompoundFully(var0);
         return DataComponentPatch.CODEC
            .parse(((Provider)HOLDER).createSerializationContext(NbtOps.INSTANCE), var2)
            .resultOrPartial(var0x -> SendMessage.errorMessage("Tried to load invalid item: '{}'", var0x));
      }

      private String convert(org.bukkit.inventory.ItemStack var1, String var2) throws ItemLoadException {
         try {
            String var3 = String.format("{id:\"minecraft:%1$s\",Count:%2$d,tag:%3$s}", var1.getType().getKey().getKey(), 1, var2);
            CompoundTag var4 = TagParser.parseCompoundFully(var3);
            DataFixer var5 = DataFixers.getDataFixer();
            TypeReference var6 = References.ITEM_STACK;
            NbtOps var7 = NbtOps.INSTANCE;
            Dynamic var8 = var5.update(var6, new Dynamic(var7, var4), 1343, 4661);
            var4 = (CompoundTag)var8.getValue();
            return var4.toString();
         } catch (Exception var9) {
            SendMessage.errorMessage("Failed to convert NBT to compound, please see error below:");
            var9.printStackTrace();
            throw new ItemLoadException("Invalid NBT data found while converting to components");
         }
      }

      private String update(NBTUtils.NBTFixer var1, org.bukkit.inventory.ItemStack var2, String var3) throws ItemLoadException {
         try {
            String var4 = String.format("{id:\"minecraft:%1$s\",count:%2$d,components:%3$s}", var2.getType().getKey().getKey(), 1, var3);
            CompoundTag var5 = TagParser.parseCompoundFully(var4);
            DataFixer var6 = DataFixers.getDataFixer();
            TypeReference var7 = References.ITEM_STACK;
            NbtOps var8 = NbtOps.INSTANCE;
            Dynamic var9 = var6.update(var7, new Dynamic(var8, var5), var1.from, var1.to);
            var5 = (CompoundTag)var9.getValue();
            return var5.toString();
         } catch (Exception var10) {
            SendMessage.errorMessage("Failed to update component data of item, see error below:");
            var10.printStackTrace();
            throw new ItemLoadException("Unknown error occurred while updating component data");
         }
      }

      private void convertMeta(ItemStack var1) throws IllegalAccessException {
         PatchedDataComponentMap var2 = (PatchedDataComponentMap)v26_2.COMPONENT_MAP.get(var1);
         ItemLore var3 = (ItemLore)var2.get(DataComponents.LORE);
         if (var3 != null) {
            List<Component> var4 = var3.lines();
            var3 = new ItemLore(var4.stream().map(var0 -> CraftChatMessage.fromJSONOrString(var0.getString())).collect(Collectors.toList()));
            var2.set(DataComponents.LORE, var3);
         }

         Component var6 = (Component)var2.get(DataComponents.CUSTOM_NAME);
         if (var6 != null) {
            var2.set(DataComponents.CUSTOM_NAME, CraftChatMessage.fromJSONOrString(var6.getString()));
         }
      }

      private static ItemStack removeEmpty(ItemStack var0) throws IllegalAccessException {
         PatchedDataComponentMap var1 = (PatchedDataComponentMap)v26_2.COMPONENT_MAP.get(var0);
         if (var1.get(DataComponents.STORED_ENCHANTMENTS) == null) {
            var1.set(DataComponents.STORED_ENCHANTMENTS, null);
         }

         if (var1.get(DataComponents.ENCHANTMENTS) == null) {
            var1.set(DataComponents.ENCHANTMENTS, null);
         }

         if (var1.get(DataComponents.RECIPES) == null) {
            var1.set(DataComponents.RECIPES, null);
         }

         if (var1.get(DataComponents.FIREWORK_EXPLOSION) == null) {
            var1.set(DataComponents.FIREWORK_EXPLOSION, null);
         }

         if (var1.get(DataComponents.FIREWORKS) == null) {
            var1.set(DataComponents.FIREWORKS, null);
         }

         if (var1.get(DataComponents.PROFILE) == null) {
            var1.set(DataComponents.PROFILE, null);
         }

         if (var1.get(DataComponents.INSTRUMENT) == null) {
            var1.set(DataComponents.INSTRUMENT, null);
         }

         if (var1.get(DataComponents.TRIM) == null) {
            var1.set(DataComponents.TRIM, null);
         }

         if (var1.get(DataComponents.LORE) == null) {
            var1.set(DataComponents.LORE, null);
         }

         if (var1.get(DataComponents.CUSTOM_NAME) == null) {
            var1.set(DataComponents.CUSTOM_NAME, null);
         }

         if (var1.get(DataComponents.ITEM_NAME) == null) {
            var1.set(DataComponents.ITEM_NAME, null);
         }

         if (var1.get(DataComponents.SUSPICIOUS_STEW_EFFECTS) == null) {
            var1.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, null);
         }

         v26_2.COMPONENT_MAP.set(var0, var1);
         return var0;
      }

      private static DataComponentPatch parseAndApply(DataComponentPatch var0, String var1, boolean var2) throws Throwable {
         Map var3 = (Map)new Gson().fromJson(var1, Map.class);
         if (var2 && var3.containsKey("display")) {
            var3.remove("display");
         }

         ItemMeta var4 = SerializableMeta.deserialize(var3);
         Builder var5 = buildMeta(var4);
         var5.copy(var0);
         return var5.build();
      }

      private static Builder buildMeta(ItemMeta var0) throws ClassNotFoundException, InstantiationException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, InvocationTargetException {
         Class var1 = Class.forName("org.bukkit.craftbukkit." + ServerInfo.getVersion().name() + ".inventory.CraftMetaItem$Applicator");
         Object var2 = var1.newInstance();
         Method var3 = var0.getClass().getDeclaredMethod("applyToItem", var1);
         var3.setAccessible(true);
         var3.invoke(var0, var2);
         Field var4 = var1.getDeclaredField("builder");
         var4.setAccessible(true);
         return (Builder)var4.get(var2);
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
