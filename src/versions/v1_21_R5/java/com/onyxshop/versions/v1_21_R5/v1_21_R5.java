package com.onyxshop.versions.v1_21_R5;

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
import org.bukkit.craftbukkit.v1_21_R5.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftItem;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R5.inventory.SerializableMeta;
import org.bukkit.craftbukkit.v1_21_R5.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class v1_21_R5 implements VersionHandler {
   private static final Field CUSTOM_TAG = getFieldByType(CustomData.class, NBTTagCompound.class);
   private static final Field COMPONENT_MAP = getFieldByType(ItemStack.class, PatchedDataComponentMap.class);
   private static final Field DATA_MAP = getFieldByType(PatchedDataComponentMap.class, Reference2ObjectMap.class);
   private static final Field ITEM_ITEM = getFieldByType(ItemStack.class, Item.class);
   private static final Method ITEM_MATCHER = getMethodByParams(ItemStack.class, boolean.class, Item.class);

   private static Method getMethodByParams(Class<?> type, Class<?> type2, Class<?>... type3) {
      for (Method method : type.getDeclaredMethods()) {
         if (method.getReturnType() == type2 && Arrays.equals((Object[])method.getParameterTypes(), (Object[])type3)) {
            return method;
         }
      }

      return null;
   }

   private static Field getFieldByType(Class<?> type, Class<?> type2) {
      for (Field field : type.getDeclaredFields()) {
         if (field.getType().equals(type2)) {
            return field;
         }
      }

      return null;
   }

   private static Field getFieldByName(Class<?> type, String name) {
      for (Field field : type.getDeclaredFields()) {
         if (field.getName().equals(name)) {
            return field;
         }
      }

      return null;
   }

   @Override
   public boolean hasData(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);

      try {
         DataComponentType dataComponentType = v1_21_R5.Components.valueOf(itemId, itemId.getClass());
         return patchedDataComponentMap.d(dataComponentType);
      } catch (Exception exception) {
         return false;
      }
   }

   @Override
   public Object emptyList() {
      return new ArrayList();
   }

   @Override
   public String toNBT(String text) {
      IChatBaseComponent iChatBaseComponent = CraftChatMessage.fromString(text, true)[0];
      return CraftChatMessage.toJSON(iChatBaseComponent);
   }

   public void setLived(org.bukkit.entity.Item item) {
      ((CraftItem)item).getHandle().l = -32768;
   }

   public org.bukkit.inventory.ItemStack setComponent(org.bukkit.inventory.ItemStack itemStack, String itemId, Object value) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);

         try {
            patchedDataComponentMap.b(v1_21_R5.Components.valueOf(itemId, Object.class), value);
         } catch (Exception exception) {
            SendMessage.warnMessage("Unsupported component tag found for " + itemId + " while injecting item data, this is a bug, please report");
         }
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisplayname(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
         this.set(patchedDataComponentMap, v1_21_R5.Components.custom_name, CraftChatMessage.fromStringOrNull(this.toNBT(itemId)));
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTDataAsString(org.bukkit.inventory.ItemStack itemStack, boolean flag) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
         this.setAccessible(patchedDataComponentMap);
         if (!flag) {
            this.remove(patchedDataComponentMap, v1_21_R5.Components.enchantments);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.stored_enchantments);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.firework_explosion);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.fireworks);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.instrument);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.trim);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.lore);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.custom_name);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.suspicious_stew_effects);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.recipes);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.dyed_color);
            this.removeCustom(patchedDataComponentMap, "ONYX");
            this.remove(patchedDataComponentMap, v1_21_R5.Components.ominous_bottle_amplifier);
            this.remove(patchedDataComponentMap, v1_21_R5.Components.profile);
         }

         try {
            String text = v1_21_R5.NBTSerializer.serialize(itemStack2);
            return text.equals("{}") ? null : text;
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to serialize NBT to string");
            exception.printStackTrace();
            return null;
         }
      } else {
         return null;
      }
   }

   @Override
   public String getLoreAsNBT(org.bukkit.inventory.ItemStack itemStack) {
      PatchedDataComponentMap patchedDataComponentMap = this.getC(CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(itemStack)));
      String lore = CraftChatMessage.fromComponent((IChatBaseComponent)this.<ItemLore>getOrNull(patchedDataComponentMap, v1_21_R5.Components.lore).a().get(0));
      return lore.substring(1).substring(0, lore.length() - 1);
   }

   @Override
   public Object getNMSLore(org.bukkit.inventory.ItemStack itemStack) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      List lores = this.<ItemLore>getOrNull(patchedDataComponentMap, v1_21_R5.Components.lore).a();
      return lores != null ? lores : new ArrayList();
   }

   @Override
   public org.bukkit.inventory.ItemStack applyNMSLore(org.bukkit.inventory.ItemStack itemStack, Object value) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
         this.set(patchedDataComponentMap, v1_21_R5.Components.lore, new ItemLore((List)value));
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public void setItemLore(Object value, String itemId, int value2) {
      ((List)value).set(value2, CraftChatMessage.fromJSON(itemId));
   }

   @Override
   public void addItemLore(Object value, String itemId, int value2) {
      ((List)value).add(value2, CraftChatMessage.fromJSON(itemId));
   }

   @Override
   public void addItemLore(Object value, String itemId) {
      ((List)value).add(CraftChatMessage.fromJSON(itemId));
   }

   @Override
   public void updateInventoryTitle(Player player, Translatable text) {
      EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
      entityPlayer.g
         .b(
            new PacketPlayOutOpenWindow(
               entityPlayer.cn.l,
               entityPlayer.cn.a(),
               ServerInfo.supportsPaper() ? (IChatBaseComponent)AdventureUtil.asVanilla(text) : CraftChatMessage.fromStringOrNull(text.getLegacy())
            )
         );
      player.updateInventory();
   }

   @Override
   public org.bukkit.inventory.ItemStack setNBTString(org.bukkit.inventory.ItemStack itemStack, String itemId, String text) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      NBTTagCompound nbtTagCompound = this.getCustom(patchedDataComponentMap);
      String[] values = itemId.split("::");
      NBTTagCompound nbtTagCompound2 = nbtTagCompound;

      for (int index = 0; index < values.length - 1; index++) {
         String text2 = values[index];
         NBTTagCompound nbtTagCompound3;
         if (this.hasKey(nbtTagCompound2, text2)) {
            nbtTagCompound3 = (NBTTagCompound)nbtTagCompound2.m(text2).get();
         } else {
            nbtTagCompound3 = new NBTTagCompound();
            this.set(nbtTagCompound2, text2, nbtTagCompound3);
         }

         nbtTagCompound2 = nbtTagCompound3;
      }

      this.set(nbtTagCompound2, values[values.length - 1], text);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTString(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      NBTTagCompound nbtTagCompound = this.getCustom(patchedDataComponentMap);
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
   public org.bukkit.inventory.ItemStack setNBTInt(org.bukkit.inventory.ItemStack itemStack, String itemId, Integer value) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      if (itemId.contains("::")) {
         NBTTagCompound nbtTagCompound = this.getCustom(patchedDataComponentMap);
         NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, itemId.split("::")[0]);
         this.set(nbtTagCompound2, itemId.split("::")[1], value);
         this.set(nbtTagCompound, itemId.split("::")[0], nbtTagCompound2);
      } else {
         NBTTagCompound nbtTagCompound = this.getCustom(patchedDataComponentMap);
         this.set(nbtTagCompound, itemId, value);
      }

      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public Integer getNBTInt(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      String[] values = itemId.split("::");
      if (values[0].equals("custom_data")) {
         NBTTagCompound nbtTagCompound = this.getCustom(patchedDataComponentMap);

         for (int index = 1; index < values.length - 1; index++) {
            if (!this.hasKey(nbtTagCompound, values[index])) {
               return null;
            }

            nbtTagCompound = this.getC(nbtTagCompound, values[index]);
         }

         return this.hasKey(nbtTagCompound, values[values.length - 1]) ? this.getI(nbtTagCompound, values[values.length - 1]) : null;
      } else {
         NBTBase nbtBase = this.getC(patchedDataComponentMap, itemId);
         return nbtBase != null ? (Integer)nbtBase.r().orElse(null) : null;
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack setPages(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "Pages", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getPages(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.getS(nbtTagCompound, "Pages");
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtString(org.bukkit.inventory.ItemStack itemStack, String itemId, String text) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, itemId, text);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getOnyxNbtString(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, itemId) ? this.getS(nbtTagCompound, itemId) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtInt(org.bukkit.inventory.ItemStack itemStack, String itemId, Integer value) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, itemId, value);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public Integer getOnyxNbtInt(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, itemId) ? this.getI(nbtTagCompound, itemId) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setIndex(org.bukkit.inventory.ItemStack itemStack, Integer index) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "Index", index);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public Integer getIndex(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "Index") ? this.getI(nbtTagCompound, "Index") : -1;
   }

   @Override
   public org.bukkit.inventory.ItemStack setAction(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "Action", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getAction(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "Action") ? this.getS(nbtTagCompound, "Action") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setEditOption(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "EditOption", itemId);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getEditOption(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "EditOption") ? this.getS(nbtTagCompound, "EditOption") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack getShopAdminItem(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "AdminItem", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean isShopAdminItem(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.getB(nbtTagCompound, "AdminItem");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTtag(org.bukkit.inventory.ItemStack itemStack, String itemId, boolean flag) throws ItemLoadException {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);

      try {
         Optional<DataComponentPatch> optional = v1_21_R5.NBTSerializer.deserialize(itemId, flag);
         return optional.<org.bukkit.inventory.ItemStack>map(element -> {
            itemStack2.b(element);
            return CraftItemStack.asBukkitCopy(itemStack2);
         }).orElse(null);
      } catch (Throwable exception) {
         StringWriter stringWriter = new StringWriter();
         PrintWriter printWriter = new PrintWriter(stringWriter);
         exception.printStackTrace(printWriter);
         throw new ItemLoadException(Lang.CANNOT_GET_NBTDATA.get() + ": " + stringWriter.toString());
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTCommands(org.bukkit.inventory.ItemStack itemStack, List<String> itemIds, CommandType commandType) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagList nbtTagList = this.getOrCreate(nbtTagCompound, commandType.getCompound(), 8);
      itemIds.forEach(entry -> nbtTagList.add(NBTTagString.a(entry.startsWith("/") ? entry.substring(1) : entry)));
      this.set(nbtTagCompound, commandType.getCompound(), nbtTagList);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean hasNBTCommand(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return !this.getOrCreate(nbtTagCompound, "buy-commands", 8).isEmpty() || !this.getOrCreate(nbtTagCompound, "sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(org.bukkit.inventory.ItemStack itemStack, CommandType commandType) {
      NBTTagList nbtTagList = this.getOrCreate(this.getOnyxCompound(itemStack), commandType.getCompound(), 8);
      return nbtTagList.stream().<String>map(NBTBase::toString).collect(Collectors.toList());
   }

   private NBTTagCompound getC(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.n(text);
   }

   private NBTBase getC(PatchedDataComponentMap patchedDataComponentMap, String text) {
      try {
         DataComponentType dataComponentType = v1_21_R5.Components.valueOf(text);
         return this.encodeC(dataComponentType, patchedDataComponentMap);
      } catch (Exception exception) {
         return null;
      }
   }

   private <T> NBTBase encodeC(DataComponentType<T> dataComponentType, PatchedDataComponentMap patchedDataComponentMap) {
      T value = (T)patchedDataComponentMap.a(dataComponentType);
      return (NBTBase)dataComponentType.c().encodeStart(DynamicOpsNBT.a, value).getOrThrow();
   }

   private NBTTagCompound getC(NBTTagList nbtTagList, int value) {
      return nbtTagList.b(value);
   }

   private PatchedDataComponentMap getC(ItemStack itemStack) {
      try {
         return (PatchedDataComponentMap)COMPONENT_MAP.get(itemStack);
      } catch (IllegalAccessException exception) {
         SendMessage.logDebugMessage("Failed to retrieve compounds from item");
         exception.printStackTrace();
         return null;
      }
   }

   private NBTTagCompound getCustom(PatchedDataComponentMap patchedDataComponentMap) {
      return this.getCustom(patchedDataComponentMap, false);
   }

   private NBTTagCompound getCustom(PatchedDataComponentMap patchedDataComponentMap, boolean flag) {
      try {
         CustomData customData = this.getOrNull(patchedDataComponentMap, DataComponents.b);
         if (customData == null) {
            if (flag) {
               return null;
            }

            NBTTagCompound nbtTagCompound = new NBTTagCompound();

            try {
               this.addCustom(patchedDataComponentMap, DataComponents.b, nbtTagCompound);
            } catch (Exception exception) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               exception.printStackTrace();
            }

            customData = this.getOrNull(patchedDataComponentMap, DataComponents.b);
         }

         return this.getCustom(customData);
      } catch (IllegalAccessException exception) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         exception.printStackTrace();
         return null;
      }
   }

   private NBTTagCompound getCustom(CustomData customData) throws IllegalAccessException {
      return (NBTTagCompound)CUSTOM_TAG.get(customData);
   }

   private NBTTagCompound getCustom(Optional<?> optional) throws IllegalAccessException {
      return optional != null && optional.isPresent() ? this.getCustom((CustomData)optional.get()).l() : new NBTTagCompound();
   }

   private NBTTagCompound getClonedCustom(PatchedDataComponentMap patchedDataComponentMap) {
      return this.getClonedCustom(patchedDataComponentMap, false);
   }

   private NBTTagCompound getClonedCustom(PatchedDataComponentMap patchedDataComponentMap, boolean flag) {
      try {
         CustomData customData = this.getOrNull(patchedDataComponentMap, DataComponents.b);
         if (customData == null) {
            if (flag) {
               return null;
            }

            NBTTagCompound nbtTagCompound = new NBTTagCompound();

            try {
               this.addCustom(patchedDataComponentMap, DataComponents.b, nbtTagCompound);
            } catch (Exception exception) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               exception.printStackTrace();
            }

            customData = this.getOrNull(patchedDataComponentMap, DataComponents.b);
         }

         return this.getClonedCustom(patchedDataComponentMap, customData);
      } catch (Exception exception) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         exception.printStackTrace();
         return null;
      }
   }

   private NBTTagCompound getClonedCustom(PatchedDataComponentMap patchedDataComponentMap, CustomData customData) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException {
      Field field = CustomData.class.getDeclaredField("tag");
      field.setAccessible(true);
      NBTTagCompound nbtTagCompound = (NBTTagCompound)field.get(customData);
      Method method = NBTTagCompound.class.getDeclaredMethod("copy");
      method.setAccessible(true);
      NBTTagCompound nbtTagCompound2 = (NBTTagCompound)method.invoke(nbtTagCompound);
      Constructor constructor = Class.forName("net.minecraft.world.item.component.CustomData").getDeclaredConstructor(NBTTagCompound.class);
      constructor.setAccessible(true);
      CustomData customData2 = (CustomData)constructor.newInstance(nbtTagCompound2);
      this.set(patchedDataComponentMap, DataComponents.b, customData2);
      return nbtTagCompound2;
   }

   private void addCustom(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<CustomData> dataComponentType, NBTTagCompound nbtTagCompound) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      Constructor constructor = CustomData.class.getDeclaredConstructor(nbtTagCompound.getClass());
      constructor.setAccessible(true);
      CustomData customData = (CustomData)constructor.newInstance(nbtTagCompound);
      this.set(patchedDataComponentMap, dataComponentType, customData);
   }

   private void removeCustom(PatchedDataComponentMap patchedDataComponentMap, String text) {
      NBTTagCompound nbtTagCompound = this.getCustom(patchedDataComponentMap, true);
      if (nbtTagCompound != null) {
         nbtTagCompound.r(text);
      }
   }

   private String getS(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.b(text, "");
   }

   private double getD(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.b(text, 0.0);
   }

   private boolean getB(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.b(text, false);
   }

   private int getI(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.b(text, 0);
   }

   private NBTTagList getOrCreate(NBTTagCompound nbtTagCompound, String text, int value) {
      return nbtTagCompound.p(text);
   }

   private NBTTagCompound getOrCreate(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.n(text);
   }

   private boolean hasKey(NBTTagCompound nbtTagCompound, String key) {
      return nbtTagCompound.b(key);
   }

   private <T> T getOrNull(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<? extends T> dataComponentType) {
      return (T)patchedDataComponentMap.a(dataComponentType);
   }

   private CustomData getOrCreateCustom(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<CustomData> dataComponentType) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      CustomData customData = this.getOrNull(patchedDataComponentMap, dataComponentType);
      if (customData == null) {
         this.addCustom(patchedDataComponentMap, dataComponentType, new NBTTagCompound());
         return this.getOrNull(patchedDataComponentMap, dataComponentType);
      } else {
         return customData;
      }
   }

   private boolean hasTag(ItemStack itemStack) {
      return !itemStack.f();
   }

   private String getString(NBTTagCompound nbtTagCompound, String text) {
      return nbtTagCompound.b(text, "");
   }

   private void setString(NBTTagCompound nbtTagCompound, String text, String text2) {
      nbtTagCompound.a(text, text2);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, NBTTagList nbtTagList) {
      nbtTagCompound.a(text, nbtTagList);
   }

   private void remove(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<?> dataComponentType) {
      try {
         Reference2ObjectMap reference2ObjectMap = (Reference2ObjectMap)DATA_MAP.get(patchedDataComponentMap);
         reference2ObjectMap.remove(dataComponentType);
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to remove value from data map");
         exception.printStackTrace();
      }
   }

   private void setAccessible(PatchedDataComponentMap patchedDataComponentMap) {
      try {
         Method method = patchedDataComponentMap.getClass().getDeclaredMethod("j");
         method.setAccessible(true);
         method.invoke(patchedDataComponentMap);
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to make data map accessible");
         exception.printStackTrace();
      }
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

   private <T> T set(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<T> dataComponentType, T value) {
      return (T)patchedDataComponentMap.b(dataComponentType, value);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, String text2) {
      nbtTagCompound.a(text, text2);
   }

   private void set(NBTTagCompound nbtTagCompound, String text, Integer value) {
      nbtTagCompound.a(text, value);
   }

   private void merge(NBTTagCompound nbtTagCompound, NBTTagCompound nbtTagCompound2) {
      nbtTagCompound.a(nbtTagCompound2);
   }

   private void set(ItemStack itemStack, PatchedDataComponentMap patchedDataComponentMap) {
      try {
         COMPONENT_MAP.set(itemStack, patchedDataComponentMap);
      } catch (IllegalAccessException exception) {
         SendMessage.logDebugMessage("Failed to retrieve compounds from item");
         exception.printStackTrace();
      }
   }

   private NBTTagCompound getOnyxCompound(org.bukkit.inventory.ItemStack itemStack) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      NBTTagCompound nbtTagCompound = this.getCustom(this.getC(itemStack2));
      return this.getOrCreate(nbtTagCompound, "ONYX");
   }

   private org.bukkit.inventory.ItemStack setOnyxCompound(org.bukkit.inventory.ItemStack itemStack, NBTTagCompound nbtTagCompound) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      NBTTagCompound nbtTagCompound2 = this.getCustom(patchedDataComponentMap);
      this.set(nbtTagCompound2, "ONYX", nbtTagCompound);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public String getTitle(Inventory inventory) {
      return "";
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisabledBackButton(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "disabledBackButton", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public boolean isDisabledBackButton(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "disabledBackButton") && this.getB(nbtTagCompound, "disabledBackButton");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTPrices(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "Prices");
      this.set(nbtTagCompound2, "buy", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".buy"));
      this.set(nbtTagCompound2, "sell", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".sell"));
      this.set(nbtTagCompound, "Prices", nbtTagCompound2);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public double getBuyPrice(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "Prices");
      return this.getD(nbtTagCompound2, "buy");
   }

   @Override
   public double getSellPrice(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "Prices");
      return this.getD(nbtTagCompound2, "sell");
   }

   @Override
   public org.bukkit.inventory.ItemStack setItemErrorToItem(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "ItemError", true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public org.bukkit.inventory.ItemStack getItemInHand(Player player) {
      return player.getInventory().getItemInMainHand();
   }

   @Override
   public org.bukkit.inventory.ItemStack setSpawnerType(org.bukkit.inventory.ItemStack itemStack, String type) {
      return this.setSpawnerType(itemStack, type, false);
   }

   public org.bukkit.inventory.ItemStack setSpawnerType(org.bukkit.inventory.ItemStack itemStack, String type, boolean flag) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "SpawnerType", type);
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(this.setOnyxCompound(itemStack, nbtTagCompound));
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      TooltipDisplay tooltipDisplay = this.getOrNull(patchedDataComponentMap, DataComponents.q);
      tooltipDisplay.a(DataComponents.aa, true);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   public org.bukkit.inventory.ItemStack addSpawnPotential(org.bukkit.inventory.ItemStack itemStack, EntityType entityType, Integer value) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      NBTTagCompound nbtTagCompound = this.getCustom(this.getOrCreateCustom(patchedDataComponentMap, v1_21_R5.Components.block_entity_data));
      NBTTagCompound nbtTagCompound2 = this.getOrCreate(nbtTagCompound, "normal_config");
      if (!this.hasKey(nbtTagCompound, "spawn_data")) {
         NBTTagCompound nbtTagCompound3 = this.getOrCreate(nbtTagCompound, "spawn_data");
         NBTTagCompound nbtTagCompound4 = new NBTTagCompound();
         this.set(nbtTagCompound4, "id", "minecraft:" + entityType.name().toLowerCase());
         this.set(nbtTagCompound3, "entity", nbtTagCompound4);
         this.set(nbtTagCompound, "spawn_data", nbtTagCompound3);
         this.set(nbtTagCompound, "id", "minecraft:trial_spawner");
         this.set(nbtTagCompound2, "ticks_between_spawn", 20.0);
         this.set(nbtTagCompound2, "simultaneous_mobs", 1.0);
         this.set(nbtTagCompound2, "simultaneous_mobs_added_per_player", 0.5);
         this.set(nbtTagCompound2, "total_mobs_added_per_player", 1.0);
         this.set(nbtTagCompound2, "total_mobs", 1.0);
      }

      NBTTagList nbtTagList = this.getOrCreate(nbtTagCompound2, "spawn_potentials", 10);
      NBTTagCompound nbtTagCompound3 = new NBTTagCompound();
      NBTTagCompound nbtTagCompound4 = new NBTTagCompound();
      NBTTagCompound nbtTagCompound5 = new NBTTagCompound();
      this.set(nbtTagCompound5, "id", "minecraft:" + entityType.name().toLowerCase());
      this.set(nbtTagCompound4, "entity", nbtTagCompound5);
      this.set(nbtTagCompound3, "data", nbtTagCompound4);
      this.set(nbtTagCompound3, "weight", value);
      nbtTagList.add(nbtTagCompound3);
      this.set(nbtTagCompound2, "spawn_potentials", nbtTagList);
      this.set(nbtTagCompound, "normal_config", nbtTagCompound2);
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   public org.bukkit.inventory.ItemStack setOminousStrength(org.bukkit.inventory.ItemStack itemStack, Integer value) {
      ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
      this.set(this.getC(itemStack2), v1_21_R5.Components.ominous_bottle_amplifier, new OminousBottleAmplifier(value));
      return CraftItemStack.asBukkitCopy(itemStack2);
   }

   @Override
   public org.bukkit.inventory.ItemStack setPotionType(org.bukkit.inventory.ItemStack itemStack, PotionTypes potionTypes) {
      PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
      if (!potionMeta.hasBasePotionType()) {
         potionMeta.setBasePotionType(potionTypes.parsePotionType());
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
   public Optional<PotionTypes> getBasePotion(org.bukkit.inventory.ItemStack itemStack) {
      PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
      return potionMeta.hasBasePotionType() && potionMeta.getBasePotionType().getEffectType() != null
         ? Optional.of(PotionTypes.matchPotionType(potionMeta.getBasePotionType()))
         : Optional.empty();
   }

   @Override
   public org.bukkit.inventory.ItemStack setPathToItem(org.bukkit.inventory.ItemStack itemStack, String path) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "PathToItem", path);
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public org.bukkit.inventory.ItemStack getSpawnerToGive(EntityType entityType) {
      org.bukkit.inventory.ItemStack itemStack = OnyxShop.getInstance().createItem.createSpawner(entityType);
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      this.set(nbtTagCompound, "SpawnerType", entityType.toString());
      return this.setOnyxCompound(itemStack, nbtTagCompound);
   }

   @Override
   public String getPathToItem(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.getS(nbtTagCompound, "PathToItem");
   }

   @Override
   public Boolean hasItemError(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return !this.hasKey(nbtTagCompound, "ItemError") ? false : this.getB(nbtTagCompound, "ItemError");
   }

   @Override
   public String getSpawnerType(org.bukkit.inventory.ItemStack itemStack) {
      NBTTagCompound nbtTagCompound = this.getOnyxCompound(itemStack);
      return this.hasKey(nbtTagCompound, "SpawnerType") && !this.getS(nbtTagCompound, "SpawnerType").isEmpty() ? this.getS(nbtTagCompound, "SpawnerType") : null;
   }

   private NBTBase get(NBTBase nbtBase, String text) {
      switch (nbtBase.b()) {
         case 9:
            return ((NBTTagList)nbtBase).c(0);
         case 10:
            return ((NBTTagCompound)nbtBase).a(text);
         default:
            return null;
      }
   }

   private void remove(NBTTagCompound nbtTagCompound, String text) {
      nbtTagCompound.r(text);
   }

   private void remove(NBTTagCompound nbtTagCompound, String text, String text2) {
      try {
         NBTBase nbtBase = nbtTagCompound.a(text);
         if (nbtBase == null) {
            return;
         }

         switch (nbtBase.b()) {
            case 8:
               JSONObject jsonObject = JsonUtil.parseJson(this.strip(nbtBase.toString()));
               jsonObject.remove(text2);
               String text3 = jsonObject.toJSONString();
               if (!text3.equals("{}")) {
                  this.set(nbtTagCompound, text, text3);
               } else {
                  nbtTagCompound.r(text);
               }
               break;
            case 9:
               ((NBTTagList)nbtBase).d(0);
               if (((NBTTagList)nbtBase).isEmpty()) {
                  nbtTagCompound.r(text);
               }
               break;
            case 10:
               this.remove((NBTTagCompound)nbtBase, text2);
               if (((NBTTagCompound)nbtBase).j()) {
                  nbtTagCompound.r(text);
               }
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

   private boolean matchCustom(NBTTagCompound nbtTagCompound, NBTTagCompound nbtTagCompound2, List<String> values) {
      for (String text : values) {
         String[] values2 = text.split("::");
         if (values2.length >= 2) {
            NBTTagCompound nbtTagCompound3 = nbtTagCompound;

            for (int index = 0; index < values2.length - 1; index++) {
               if (index == values2.length - 2) {
                  this.remove(nbtTagCompound3, values2[index], values2[index + 1]);
                  break;
               }

               nbtTagCompound3 = this.getC(nbtTagCompound3, values2[index]);
            }

            NBTTagCompound nbtTagCompound4 = nbtTagCompound2;

            for (int index = 0; index < values2.length - 1; index++) {
               if (index == values2.length - 2) {
                  this.remove(nbtTagCompound4, values2[index], values2[index + 1]);
                  break;
               }

               nbtTagCompound4 = this.getC(nbtTagCompound4, values2[index]);
            }
         } else {
            try {
               this.remove(nbtTagCompound, values2[0]);
               this.remove(nbtTagCompound2, values2[0]);
            } catch (Exception exception) {
            }
         }
      }

      return Objects.equals(nbtTagCompound, nbtTagCompound2);
   }

   @Override
   public boolean isSimilar(org.bukkit.inventory.ItemStack itemStack, org.bukkit.inventory.ItemStack itemStack2, List<String> itemIds) {
      try {
         ItemStack itemStack3 = CraftItemStack.asNMSCopy(itemStack);
         ItemStack itemStack4 = CraftItemStack.asNMSCopy(itemStack2);
         if (this.hasTag(itemStack3) && this.hasTag(itemStack4)) {
            Item item = (Item)ITEM_ITEM.get(itemStack3);
            if (!(Boolean)ITEM_MATCHER.invoke(itemStack4, item)) {
               return false;
            }

            PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack3);
            PatchedDataComponentMap patchedDataComponentMap2 = this.getC(itemStack4);
            Reference2ObjectMap reference2ObjectMap = (Reference2ObjectMap)DATA_MAP.get(patchedDataComponentMap);
            Reference2ObjectMap reference2ObjectMap2 = (Reference2ObjectMap)DATA_MAP.get(patchedDataComponentMap2);
            HashSet keys = new HashSet(reference2ObjectMap.keySet());
            keys.addAll(reference2ObjectMap2.keySet());

            for (Object value : keys) {
               DataComponentType dataComponentType = (DataComponentType)value;
               String text = String.valueOf(dataComponentType).replace("minecraft:", "");
               if (!itemIds.contains(text)) {
                  if (text.equals("custom_data")) {
                     NBTTagCompound nbtTagCompound = this.getCustom((Optional<?>)reference2ObjectMap.get(dataComponentType));
                     NBTTagCompound nbtTagCompound2 = this.getCustom((Optional<?>)reference2ObjectMap2.get(dataComponentType));
                     if (!this.matchCustom(
                        nbtTagCompound,
                        nbtTagCompound2,
                        itemIds.stream()
                           .filter(element -> element.startsWith("custom_data::"))
                           .map(element -> element.replace("custom_data::", ""))
                           .collect(Collectors.toList())
                     )) {
                        return false;
                     }
                  } else {
                     if (dataComponentType == v1_21_R5.Components.profile
                        && reference2ObjectMap.containsKey(v1_21_R5.Components.profile)
                        && reference2ObjectMap2.containsKey(v1_21_R5.Components.profile)) {
                        return this.compareProfile(
                           (ResolvableProfile)((Optional)reference2ObjectMap.get(dataComponentType)).orElse(null), (ResolvableProfile)((Optional)reference2ObjectMap2.get(dataComponentType)).orElse(null)
                        );
                     }

                     if (!Objects.equals(reference2ObjectMap.get(dataComponentType), reference2ObjectMap2.get(dataComponentType))) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to match NMS item");
         exception.printStackTrace();
      }

      return itemStack.isSimilar(itemStack2);
   }

   private boolean compareProfile(ResolvableProfile resolvableProfile, ResolvableProfile resolvableProfile2) {
      return resolvableProfile != null && resolvableProfile2 != null && resolvableProfile.d().orElse("").equals(resolvableProfile2.d().orElse("")) && SkullUtil.isSimilarTexture(resolvableProfile.g(), resolvableProfile2.g());
   }

   static {
      try {
         ITEM_ITEM.setAccessible(true);
         ITEM_MATCHER.setAccessible(true);
         COMPONENT_MAP.setAccessible(true);
         DATA_MAP.setAccessible(true);
         CUSTOM_TAG.setAccessible(true);
      } catch (Exception exception) {
         exception.printStackTrace();
         throw new UnsupportedOperationException("Failed to initialize VersionHandler, this is a bug");
      }
   }

   static final class ComponentKey<T> {
      final DataComponentType<T> type;
      final String nbt;

      ComponentKey(DataComponentType<T> dataComponentType, String key) throws NoSuchFieldException , IllegalAccessException{
         this.type = dataComponentType;
         this.nbt = key;
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

      public static DataComponentType<?> valueOf(String type) throws NoSuchFieldException, IllegalAccessException {
         Field field = v1_21_R5.Components.class.getDeclaredField(type);
         field.setAccessible(true);
         return (DataComponentType<?>)field.get(null);
      }

      public static String getName(DataComponentType<?> dataComponentType) {
         for (Field field : v1_21_R5.Components.class.getDeclaredFields()) {
            if (field.getType().equals(dataComponentType.getClass())) {
               return field.getName();
            }
         }

         return null;
      }

      public static <T> DataComponentType<T> valueOf(String type, Class<T> type2) throws NoSuchFieldException , IllegalAccessException{
         Field field = v1_21_R5.Components.class.getDeclaredField(type);
         field.setAccessible(true);
         return (DataComponentType<T>)field.get(null);
      }

      private Set<String> getRegistered() {
         Field field = v1_21_R5.getFieldByName(BuiltInRegistries.class, "DATA_COMPONENT_TYPE");
         field.setAccessible(true);

         try {
            return ((Set<?>)ReflectUtil.invokeLegacyVersionMethod("keySet", field.get(null))).stream().<String>map(element -> ((MinecraftKey)element).a()).collect(Collectors.toSet());
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to query data component types");
            exception.printStackTrace();
            return new HashSet<>();
         }
      }
   }

   private static final class NBTSerializer {
      private static final String NBT_FORMAT = "{id:\"minecraft:%1$s\",Count:%2$d,tag:%3$s}";
      private static final String COMP_FORMAT = "{id:\"minecraft:%1$s\",count:%2$d,components:%3$s}";
      private static final Object HOLDER;

      public static String serialize(ItemStack itemStack) {
         NBTTagCompound nbtTagCompound = (NBTTagCompound)ItemStack.b.encodeStart(((a)HOLDER).a(DynamicOpsNBT.a), itemStack).getOrThrow();
         return nbtTagCompound.n("components").toString();
      }

      public static Optional<DataComponentPatch> deserialize(String command, boolean flag) throws CommandSyntaxException {
         NBTTagCompound nbtTagCompound = MojangsonParser.a(command);
         return DataComponentPatch.b
            .parse(((a)HOLDER).a(DynamicOpsNBT.a), nbtTagCompound)
            .resultOrPartial(element -> SendMessage.errorMessage("Tried to load invalid item: '{}'", element));
      }

      private String convert(org.bukkit.inventory.ItemStack itemStack, String itemId) throws ItemLoadException {
         try {
            String id = String.format("{id:\"minecraft:%1$s\",Count:%2$d,tag:%3$s}", itemStack.getType().getKey().getKey(), 1, itemId);
            NBTTagCompound nbtTagCompound = MojangsonParser.a(id);
            DataFixer dataFixer = DataConverterRegistry.a();
            TypeReference typeReference = DataConverterTypes.u;
            DynamicOpsNBT dynamicOpsNBT = DynamicOpsNBT.a;
            Dynamic dynamic = dataFixer.update(typeReference, new Dynamic(dynamicOpsNBT, nbtTagCompound), 1343, 4424);
            nbtTagCompound = (NBTTagCompound)dynamic.getValue();
            return nbtTagCompound.toString();
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to convert NBT to compound, please see error below:");
            exception.printStackTrace();
            throw new ItemLoadException("Invalid NBT data found while converting to components");
         }
      }

      private String update(NBTUtils.NBTFixer nbtFixer, org.bukkit.inventory.ItemStack itemStack, String itemId) throws ItemLoadException {
         try {
            String id = String.format("{id:\"minecraft:%1$s\",count:%2$d,components:%3$s}", itemStack.getType().getKey().getKey(), 1, itemId);
            NBTTagCompound nbtTagCompound = MojangsonParser.a(id);
            DataFixer dataFixer = DataConverterRegistry.a();
            TypeReference typeReference = DataConverterTypes.u;
            DynamicOpsNBT dynamicOpsNBT = DynamicOpsNBT.a;
            Dynamic dynamic = dataFixer.update(typeReference, new Dynamic(dynamicOpsNBT, nbtTagCompound), nbtFixer.from, nbtFixer.to);
            nbtTagCompound = (NBTTagCompound)dynamic.getValue();
            return nbtTagCompound.toString();
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to update component data of item, see error below:");
            exception.printStackTrace();
            throw new ItemLoadException("Unknown error occurred while updating component data");
         }
      }

      private void convertMeta(ItemStack itemStack) throws IllegalAccessException {
         PatchedDataComponentMap patchedDataComponentMap = (PatchedDataComponentMap)v1_21_R5.COMPONENT_MAP.get(itemStack);
         ItemLore itemLore = (ItemLore)patchedDataComponentMap.a(v1_21_R5.Components.lore);
         if (itemLore != null) {
            List<IChatBaseComponent> values = itemLore.a();
            itemLore = new ItemLore(values.stream().map(element -> CraftChatMessage.fromJSONOrString(element.getString())).collect(Collectors.toList()));
            patchedDataComponentMap.b(v1_21_R5.Components.lore, itemLore);
         }

         IChatBaseComponent iChatBaseComponent = (IChatBaseComponent)patchedDataComponentMap.a(v1_21_R5.Components.custom_name);
         if (iChatBaseComponent != null) {
            patchedDataComponentMap.b(v1_21_R5.Components.custom_name, CraftChatMessage.fromJSONOrString(iChatBaseComponent.getString()));
         }
      }

      private static ItemStack removeEmpty(ItemStack itemStack) throws IllegalAccessException {
         PatchedDataComponentMap patchedDataComponentMap = (PatchedDataComponentMap)v1_21_R5.COMPONENT_MAP.get(itemStack);
         if (patchedDataComponentMap.a(v1_21_R5.Components.stored_enchantments) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.stored_enchantments, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.enchantments) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.enchantments, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.recipes) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.recipes, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.firework_explosion) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.firework_explosion, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.fireworks) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.fireworks, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.profile) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.profile, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.instrument) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.instrument, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.trim) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.trim, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.lore) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.lore, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.custom_name) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.custom_name, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.item_name) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.item_name, null);
         }

         if (patchedDataComponentMap.a(v1_21_R5.Components.suspicious_stew_effects) == null) {
            patchedDataComponentMap.b(v1_21_R5.Components.suspicious_stew_effects, null);
         }

         v1_21_R5.COMPONENT_MAP.set(itemStack, patchedDataComponentMap);
         return itemStack;
      }

      private static DataComponentPatch parseAndApply(DataComponentPatch dataComponentPatch, String text, boolean flag) throws Throwable {
         Map map = (Map)new Gson().fromJson(text, Map.class);
         if (flag && map.containsKey("display")) {
            map.remove("display");
         }

         ItemMeta itemMeta = SerializableMeta.deserialize(map);
         net.minecraft.core.component.DataComponentPatch.a value = buildMeta(itemMeta);
         value.copy(dataComponentPatch);
         return value.a();
      }

      private static net.minecraft.core.component.DataComponentPatch.a buildMeta(ItemMeta itemMeta) throws ClassNotFoundException , InstantiationException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, InvocationTargetException{
         Class type = Class.forName("org.bukkit.craftbukkit." + ServerInfo.getVersion().name() + ".inventory.CraftMetaItem$Applicator");
         Object value = type.newInstance();
         Method method = itemMeta.getClass().getDeclaredMethod("applyToItem", type);
         method.setAccessible(true);
         method.invoke(itemMeta, value);
         Field field = type.getDeclaredField("builder");
         field.setAccessible(true);
         return (net.minecraft.core.component.DataComponentPatch.a)field.get(value);
      }

      static {
         try {
            Method method = CraftServer.class.getDeclaredMethod("getServer");
            method.setAccessible(true);
            Object value = method.invoke(Bukkit.getServer());
            Method method2 = CraftRegistry.class.getDeclaredMethod("getMinecraftRegistry");
            method2.setAccessible(true);
            HOLDER = method2.invoke(value);
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to init item serializer");
            exception.printStackTrace();
            throw new NullPointerException("Failed to init item serializer");
         }
      }
   }
}
