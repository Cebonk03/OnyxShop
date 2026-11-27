package com.onyxshop.versions.paper_v1_21_R7;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import com.onyxshop.util.exceptions.ItemLoadException;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
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
import net.minecraft.world.item.Item;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class v1_21_R7 implements VersionHandler {
   private static final Field CUSTOM_TAG = getFieldByType(CustomData.class, CompoundTag.class);
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
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);

      try {
         DataComponentType dataComponentType = v1_21_R7.Components.valueOf(itemId);
         return patchedDataComponentMap.hasNonDefault(dataComponentType);
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
      Component component = CBClassWrapper.chatMessageFromString(text, true)[0];
      return CBClassWrapper.chatMessageToJSON(component);
   }

   public void setLived(org.bukkit.entity.Item item) {
      CBClassWrapper.getItemHandle(item).pickupDelay = -32768;
   }

   public org.bukkit.inventory.ItemStack setComponent(org.bukkit.inventory.ItemStack itemStack, String itemId, Object value) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);

         try {
            patchedDataComponentMap.set(v1_21_R7.Components.valueOf(itemId, Object.class), value);
         } catch (Exception exception) {
            SendMessage.warnMessage("Unsupported component tag found for " + itemId + " while injecting item data, this is a bug, please report");
         }
      }

      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisplayname(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
         this.set(patchedDataComponentMap, v1_21_R7.Components.custom_name, CBClassWrapper.chatMessageFromStringOrNull(this.toNBT(itemId)));
      }

      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTDataAsString(org.bukkit.inventory.ItemStack itemStack, boolean flag) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
         this.setAccessible(patchedDataComponentMap);
         if (!flag) {
            this.remove(patchedDataComponentMap, v1_21_R7.Components.enchantments);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.stored_enchantments);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.firework_explosion);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.fireworks);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.instrument);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.trim);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.lore);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.custom_name);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.suspicious_stew_effects);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.recipes);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.dyed_color);
            this.removeCustom(patchedDataComponentMap, "ONYX");
            this.remove(patchedDataComponentMap, v1_21_R7.Components.ominous_bottle_amplifier);
            this.remove(patchedDataComponentMap, v1_21_R7.Components.profile);
         }

         try {
            String text = v1_21_R7.NBTSerializer.serialize(itemStack2);
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
      PatchedDataComponentMap patchedDataComponentMap = this.getC(CBClassWrapper.craftStackAsNMSCopy(new org.bukkit.inventory.ItemStack(itemStack)));
      String lore = CBClassWrapper.chatMessageFromComponent((Component)this.<ItemLore>getOrNull(patchedDataComponentMap, v1_21_R7.Components.lore).lines().get(0));
      return lore.substring(1).substring(0, lore.length() - 1);
   }

   @Override
   public Object getNMSLore(org.bukkit.inventory.ItemStack itemStack) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      List lores = this.<ItemLore>getOrNull(patchedDataComponentMap, v1_21_R7.Components.lore).lines();
      return lores != null ? lores : new ArrayList();
   }

   @Override
   public org.bukkit.inventory.ItemStack applyNMSLore(org.bukkit.inventory.ItemStack itemStack, Object value) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(new org.bukkit.inventory.ItemStack(itemStack));
      if (this.hasTag(itemStack2)) {
         PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
         this.set(patchedDataComponentMap, v1_21_R7.Components.lore, new ItemLore((List)value));
      }

      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
   }

   @Override
   public void setItemLore(Object value, String itemId, int value2) {
      ((List)value).set(value2, CBClassWrapper.chatMessageFromJSON(itemId));
   }

   @Override
   public void addItemLore(Object value, String itemId, int value2) {
      ((List)value).add(value2, CBClassWrapper.chatMessageFromJSON(itemId));
   }

   @Override
   public void addItemLore(Object value, String itemId) {
      ((List)value).add(CBClassWrapper.chatMessageFromJSON(itemId));
   }

   @Override
   public void updateInventoryTitle(Player player, Translatable text) {
      ServerPlayer serverPlayer = CBClassWrapper.getPlayerHandle(player);
      serverPlayer.connection
         .send(
            new ClientboundOpenScreenPacket(
               serverPlayer.containerMenu.containerId,
               serverPlayer.containerMenu.getType(),
               ServerInfo.supportsPaper() ? (Component)AdventureUtil.asVanilla(text) : CBClassWrapper.chatMessageFromStringOrNull(text.getLegacy())
            )
         );
      player.updateInventory();
   }

   @Override
   public org.bukkit.inventory.ItemStack setNBTString(org.bukkit.inventory.ItemStack itemStack, String itemId, String text) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      CompoundTag compoundTag = this.getCustom(patchedDataComponentMap);
      String[] values = itemId.split("::");
      CompoundTag compoundTag2 = compoundTag;

      for (int index = 0; index < values.length - 1; index++) {
         String text2 = values[index];
         CompoundTag compoundTag3;
         if (this.hasKey(compoundTag2, text2)) {
            compoundTag3 = (CompoundTag)compoundTag2.getCompound(text2).get();
         } else {
            compoundTag3 = new CompoundTag();
            this.set(compoundTag2, text2, compoundTag3);
         }

         compoundTag2 = compoundTag3;
      }

      this.set(compoundTag2, values[values.length - 1], text);
      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
   }

   @Override
   public String getNBTString(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      CompoundTag compoundTag = this.getCustom(patchedDataComponentMap);
      String[] values = itemId.split("::");

      for (int index = 0; index < values.length - 1; index++) {
         if (!this.hasKey(compoundTag, values[index])) {
            return null;
         }

         compoundTag = this.getC(compoundTag, values[index]);
      }

      return this.hasKey(compoundTag, values[values.length - 1]) ? this.getS(compoundTag, values[values.length - 1]) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setNBTInt(org.bukkit.inventory.ItemStack itemStack, String itemId, Integer value) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      if (itemId.contains("::")) {
         CompoundTag compoundTag = this.getCustom(patchedDataComponentMap);
         CompoundTag compoundTag2 = this.getOrCreate(compoundTag, itemId.split("::")[0]);
         this.set(compoundTag2, itemId.split("::")[1], value);
         this.set(compoundTag, itemId.split("::")[0], compoundTag2);
      } else {
         CompoundTag compoundTag = this.getCustom(patchedDataComponentMap);
         this.set(compoundTag, itemId, value);
      }

      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
   }

   @Override
   public Integer getNBTInt(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      String[] values = itemId.split("::");
      if (values[0].equals("custom_data")) {
         CompoundTag compoundTag = this.getCustom(patchedDataComponentMap);

         for (int index = 1; index < values.length - 1; index++) {
            if (!this.hasKey(compoundTag, values[index])) {
               return null;
            }

            compoundTag = this.getC(compoundTag, values[index]);
         }

         return this.hasKey(compoundTag, values[values.length - 1]) ? this.getI(compoundTag, values[values.length - 1]) : null;
      } else {
         Tag tag = this.getC(patchedDataComponentMap, itemId);
         return tag != null ? (Integer)tag.asInt().orElse(null) : null;
      }
   }

   @Override
   public org.bukkit.inventory.ItemStack setPages(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "Pages", itemId);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public String getPages(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.getS(compoundTag, "Pages");
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtString(org.bukkit.inventory.ItemStack itemStack, String itemId, String text) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, itemId, text);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public String getOnyxNbtString(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, itemId) ? this.getS(compoundTag, itemId) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setOnyxNbtInt(org.bukkit.inventory.ItemStack itemStack, String itemId, Integer value) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, itemId, value);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public Integer getOnyxNbtInt(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, itemId) ? this.getI(compoundTag, itemId) : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setIndex(org.bukkit.inventory.ItemStack itemStack, Integer index) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "Index", index);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public Integer getIndex(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, "Index") ? this.getI(compoundTag, "Index") : -1;
   }

   @Override
   public org.bukkit.inventory.ItemStack setAction(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "Action", itemId);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public String getAction(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, "Action") ? this.getS(compoundTag, "Action") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack setEditOption(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "EditOption", itemId);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public String getEditOption(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, "EditOption") ? this.getS(compoundTag, "EditOption") : null;
   }

   @Override
   public org.bukkit.inventory.ItemStack getShopAdminItem(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "AdminItem", true);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public boolean isShopAdminItem(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.getB(compoundTag, "AdminItem");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTtag(org.bukkit.inventory.ItemStack itemStack, String itemId, boolean flag) throws ItemLoadException {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);

      try {
         Optional<DataComponentPatch> optional = v1_21_R7.NBTSerializer.deserialize(itemId, flag);
         return optional.<org.bukkit.inventory.ItemStack>map(element -> {
            itemStack2.applyComponents(element);
            return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
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
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      ListTag listTag = this.getOrCreate(compoundTag, commandType.getCompound(), 8);
      itemIds.forEach(entry -> listTag.add(StringTag.valueOf(entry.startsWith("/") ? entry.substring(1) : entry)));
      this.set(compoundTag, commandType.getCompound(), listTag);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public boolean hasNBTCommand(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return !this.getOrCreate(compoundTag, "buy-commands", 8).isEmpty() || !this.getOrCreate(compoundTag, "sell-commands", 8).isEmpty();
   }

   @Override
   public List<String> getNBTCommands(org.bukkit.inventory.ItemStack itemStack, CommandType commandType) {
      ListTag listTag = this.getOrCreate(this.getOnyxCompound(itemStack), commandType.getCompound(), 8);
      return listTag.stream().<String>map(Tag::toString).collect(Collectors.toList());
   }

   private CompoundTag getC(CompoundTag compoundTag, String text) {
      return compoundTag.getCompoundOrEmpty(text);
   }

   private Tag getC(PatchedDataComponentMap patchedDataComponentMap, String text) {
      try {
         DataComponentType dataComponentType = v1_21_R7.Components.valueOf(text);
         return this.encodeC(dataComponentType, patchedDataComponentMap);
      } catch (Exception exception) {
         return null;
      }
   }

   private <T> Tag encodeC(DataComponentType<T> dataComponentType, PatchedDataComponentMap patchedDataComponentMap) {
      T value = (T)patchedDataComponentMap.get(dataComponentType);
      return (Tag)dataComponentType.codecOrThrow().encodeStart(NbtOps.INSTANCE, value).getOrThrow();
   }

   private CompoundTag getC(ListTag listTag, int value) {
      return listTag.getCompoundOrEmpty(value);
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

   private CompoundTag getCustom(PatchedDataComponentMap patchedDataComponentMap) {
      return this.getCustom(patchedDataComponentMap, false);
   }

   private CompoundTag getCustom(PatchedDataComponentMap patchedDataComponentMap, boolean flag) {
      try {
         CustomData customData = this.getOrNull(patchedDataComponentMap, DataComponents.CUSTOM_DATA);
         if (customData == null) {
            if (flag) {
               return null;
            }

            CompoundTag compoundTag = new CompoundTag();

            try {
               this.addCustom(patchedDataComponentMap, DataComponents.CUSTOM_DATA, compoundTag);
            } catch (Exception exception) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               exception.printStackTrace();
            }

            customData = this.getOrNull(patchedDataComponentMap, DataComponents.CUSTOM_DATA);
         }

         return this.getCustom(customData);
      } catch (IllegalAccessException exception) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         exception.printStackTrace();
         return null;
      }
   }

   private CompoundTag getCustom(CustomData customData) throws IllegalAccessException {
      return (CompoundTag)CUSTOM_TAG.get(customData);
   }

   private CompoundTag getCustom(Optional<?> optional) throws IllegalAccessException {
      return optional != null && optional.isPresent() ? this.getCustom((CustomData)optional.get()).copy() : new CompoundTag();
   }

   private CompoundTag getClonedCustom(PatchedDataComponentMap patchedDataComponentMap) {
      return this.getClonedCustom(patchedDataComponentMap, false);
   }

   private CompoundTag getClonedCustom(PatchedDataComponentMap patchedDataComponentMap, boolean flag) {
      try {
         CustomData customData = this.getOrNull(patchedDataComponentMap, DataComponents.CUSTOM_DATA);
         if (customData == null) {
            if (flag) {
               return null;
            }

            CompoundTag compoundTag = new CompoundTag();

            try {
               this.addCustom(patchedDataComponentMap, DataComponents.CUSTOM_DATA, compoundTag);
            } catch (Exception exception) {
               SendMessage.errorMessage("Failed to create custom data NBT for item");
               exception.printStackTrace();
            }

            customData = this.getOrNull(patchedDataComponentMap, DataComponents.CUSTOM_DATA);
         }

         return this.getClonedCustom(patchedDataComponentMap, customData);
      } catch (Exception exception) {
         SendMessage.logDebugMessage("Failed to retrieve custom NBT from item");
         exception.printStackTrace();
         return null;
      }
   }

   private CompoundTag getClonedCustom(PatchedDataComponentMap patchedDataComponentMap, CustomData customData) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException {
      Field field = CustomData.class.getDeclaredField("tag");
      field.setAccessible(true);
      CompoundTag compoundTag = (CompoundTag)field.get(customData);
      Method method = CompoundTag.class.getDeclaredMethod("copy");
      method.setAccessible(true);
      CompoundTag compoundTag2 = (CompoundTag)method.invoke(compoundTag);
      Constructor constructor = Class.forName("net.minecraft.world.item.component.CustomData").getDeclaredConstructor(CompoundTag.class);
      constructor.setAccessible(true);
      CustomData customData2 = (CustomData)constructor.newInstance(compoundTag2);
      this.set(patchedDataComponentMap, DataComponents.CUSTOM_DATA, customData2);
      return compoundTag2;
   }

   private void addCustom(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<CustomData> dataComponentType, CompoundTag compoundTag) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      Constructor constructor = CustomData.class.getDeclaredConstructor(compoundTag.getClass());
      constructor.setAccessible(true);
      CustomData customData = (CustomData)constructor.newInstance(compoundTag);
      this.set(patchedDataComponentMap, dataComponentType, customData);
   }

   private void removeCustom(PatchedDataComponentMap patchedDataComponentMap, String text) {
      CompoundTag compoundTag = this.getCustom(patchedDataComponentMap, true);
      if (compoundTag != null) {
         compoundTag.remove(text);
      }
   }

   private String getS(CompoundTag compoundTag, String text) {
      return compoundTag.getStringOr(text, "");
   }

   private double getD(CompoundTag compoundTag, String text) {
      return compoundTag.getDoubleOr(text, 0.0);
   }

   private boolean getB(CompoundTag compoundTag, String text) {
      return compoundTag.getBooleanOr(text, false);
   }

   private int getI(CompoundTag compoundTag, String text) {
      return compoundTag.getIntOr(text, 0);
   }

   private ListTag getOrCreate(CompoundTag compoundTag, String text, int value) {
      return compoundTag.getListOrEmpty(text);
   }

   private CompoundTag getOrCreate(CompoundTag compoundTag, String text) {
      return compoundTag.getCompoundOrEmpty(text);
   }

   private boolean hasKey(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key);
   }

   private <T> T getOrNull(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<? extends T> dataComponentType) {
      return (T)patchedDataComponentMap.get(dataComponentType);
   }

   private CustomData getOrCreateCustom(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<CustomData> dataComponentType) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
      CustomData customData = this.getOrNull(patchedDataComponentMap, dataComponentType);
      if (customData == null) {
         this.addCustom(patchedDataComponentMap, dataComponentType, new CompoundTag());
         return this.getOrNull(patchedDataComponentMap, dataComponentType);
      } else {
         return customData;
      }
   }

   private boolean hasTag(ItemStack itemStack) {
      return !itemStack.isEmpty();
   }

   private String getString(CompoundTag compoundTag, String text) {
      return compoundTag.getStringOr(text, "");
   }

   private void setString(CompoundTag compoundTag, String text, String text2) {
      compoundTag.putString(text, text2);
   }

   private void set(CompoundTag compoundTag, String text, ListTag listTag) {
      compoundTag.put(text, listTag);
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
         Method method = patchedDataComponentMap.getClass().getDeclaredMethod("ensureMapOwnership");
         method.setAccessible(true);
         method.invoke(patchedDataComponentMap);
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to make data map accessible");
         exception.printStackTrace();
      }
   }

   private void set(CompoundTag compoundTag, String text, boolean flag) {
      compoundTag.putBoolean(text, flag);
   }

   private void set(CompoundTag compoundTag, String text, double amount) {
      compoundTag.putDouble(text, amount);
   }

   private void set(CompoundTag compoundTag, String text, CompoundTag compoundTag2) {
      compoundTag.put(text, compoundTag2);
   }

   private <T> T set(PatchedDataComponentMap patchedDataComponentMap, DataComponentType<T> dataComponentType, T value) {
      return (T)patchedDataComponentMap.set(dataComponentType, value);
   }

   private void set(CompoundTag compoundTag, String text, String text2) {
      compoundTag.putString(text, text2);
   }

   private void set(CompoundTag compoundTag, String text, Integer value) {
      compoundTag.putInt(text, value);
   }

   private void merge(CompoundTag compoundTag, CompoundTag compoundTag2) {
      compoundTag.merge(compoundTag2);
   }

   private void set(ItemStack itemStack, PatchedDataComponentMap patchedDataComponentMap) {
      try {
         COMPONENT_MAP.set(itemStack, patchedDataComponentMap);
      } catch (IllegalAccessException exception) {
         SendMessage.logDebugMessage("Failed to retrieve compounds from item");
         exception.printStackTrace();
      }
   }

   private CompoundTag getOnyxCompound(org.bukkit.inventory.ItemStack itemStack) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      CompoundTag compoundTag = this.getCustom(this.getC(itemStack2));
      return this.getOrCreate(compoundTag, "ONYX");
   }

   private org.bukkit.inventory.ItemStack setOnyxCompound(org.bukkit.inventory.ItemStack itemStack, CompoundTag compoundTag) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      CompoundTag compoundTag2 = this.getCustom(patchedDataComponentMap);
      this.set(compoundTag2, "ONYX", compoundTag);
      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
   }

   @Override
   public String getTitle(Inventory inventory) {
      return "";
   }

   @Override
   public org.bukkit.inventory.ItemStack setDisabledBackButton(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "disabledBackButton", true);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public boolean isDisabledBackButton(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, "disabledBackButton") && this.getB(compoundTag, "disabledBackButton");
   }

   @Override
   public org.bukkit.inventory.ItemStack addNBTPrices(org.bukkit.inventory.ItemStack itemStack, String itemId) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      CompoundTag compoundTag2 = this.getOrCreate(compoundTag, "Prices");
      this.set(compoundTag2, "buy", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".buy"));
      this.set(compoundTag2, "sell", ConfigManager.getShop(itemId.split("\\.")[0]).getDouble(itemId + ".sell"));
      this.set(compoundTag, "Prices", compoundTag2);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public double getBuyPrice(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      CompoundTag compoundTag2 = this.getOrCreate(compoundTag, "Prices");
      return this.getD(compoundTag2, "buy");
   }

   @Override
   public double getSellPrice(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      CompoundTag compoundTag2 = this.getOrCreate(compoundTag, "Prices");
      return this.getD(compoundTag2, "sell");
   }

   @Override
   public org.bukkit.inventory.ItemStack setItemErrorToItem(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "ItemError", true);
      return this.setOnyxCompound(itemStack, compoundTag);
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
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "SpawnerType", type);
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(this.setOnyxCompound(itemStack, compoundTag));
      PatchedDataComponentMap patchedDataComponentMap = this.getC(itemStack2);
      TooltipDisplay tooltipDisplay = this.getOrNull(patchedDataComponentMap, DataComponents.TOOLTIP_DISPLAY);
      tooltipDisplay.withHidden(DataComponents.BLOCK_ENTITY_DATA, true);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   public org.bukkit.inventory.ItemStack setOminousStrength(org.bukkit.inventory.ItemStack itemStack, Integer value) {
      ItemStack itemStack2 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
      this.set(this.getC(itemStack2), v1_21_R7.Components.ominous_bottle_amplifier, new OminousBottleAmplifier(value));
      return CBClassWrapper.craftStackAsBukkitCopy(itemStack2);
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
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "PathToItem", path);
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public org.bukkit.inventory.ItemStack getSpawnerToGive(EntityType entityType) {
      org.bukkit.inventory.ItemStack itemStack = OnyxShop.getInstance().createItem.createSpawner(entityType);
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      this.set(compoundTag, "SpawnerType", entityType.toString());
      return this.setOnyxCompound(itemStack, compoundTag);
   }

   @Override
   public String getPathToItem(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.getS(compoundTag, "PathToItem");
   }

   @Override
   public Boolean hasItemError(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return !this.hasKey(compoundTag, "ItemError") ? false : this.getB(compoundTag, "ItemError");
   }

   @Override
   public String getSpawnerType(org.bukkit.inventory.ItemStack itemStack) {
      CompoundTag compoundTag = this.getOnyxCompound(itemStack);
      return this.hasKey(compoundTag, "SpawnerType") && !this.getS(compoundTag, "SpawnerType").isEmpty() ? this.getS(compoundTag, "SpawnerType") : null;
   }

   private Tag get(Tag tag, String text) {
      switch (tag.getId()) {
         case 9:
            return ((ListTag)tag).get(0);
         case 10:
            return ((CompoundTag)tag).get(text);
         default:
            return null;
      }
   }

   private void remove(CompoundTag compoundTag, String text) {
      compoundTag.remove(text);
   }

   private void remove(CompoundTag compoundTag, String text, String text2) {
      try {
         Tag tag = compoundTag.get(text);
         if (tag == null) {
            return;
         }

         switch (tag.getId()) {
            case 8:
               JSONObject jsonObject = JsonUtil.parseJson(this.strip(tag.toString()));
               jsonObject.remove(text2);
               String text3 = jsonObject.toJSONString();
               if (!text3.equals("{}")) {
                  this.set(compoundTag, text, text3);
               } else {
                  compoundTag.remove(text);
               }
               break;
            case 9:
               ((ListTag)tag).remove(0);
               if (((ListTag)tag).isEmpty()) {
                  compoundTag.remove(text);
               }
               break;
            case 10:
               this.remove((CompoundTag)tag, text2);
               if (((CompoundTag)tag).isEmpty()) {
                  compoundTag.remove(text);
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

   private boolean matchCustom(CompoundTag compoundTag, CompoundTag compoundTag2, List<String> values) {
      for (String text : values) {
         String[] values2 = text.split("::");
         if (values2.length >= 2) {
            CompoundTag compoundTag3 = compoundTag;

            for (int index = 0; index < values2.length - 1; index++) {
               if (index == values2.length - 2) {
                  this.remove(compoundTag3, values2[index], values2[index + 1]);
                  break;
               }

               compoundTag3 = this.getC(compoundTag3, values2[index]);
            }

            CompoundTag compoundTag4 = compoundTag2;

            for (int index = 0; index < values2.length - 1; index++) {
               if (index == values2.length - 2) {
                  this.remove(compoundTag4, values2[index], values2[index + 1]);
                  break;
               }

               compoundTag4 = this.getC(compoundTag4, values2[index]);
            }
         } else {
            try {
               this.remove(compoundTag, values2[0]);
               this.remove(compoundTag2, values2[0]);
            } catch (Exception exception) {
            }
         }
      }

      return Objects.equals(compoundTag, compoundTag2);
   }

   @Override
   public boolean isSimilar(org.bukkit.inventory.ItemStack itemStack, org.bukkit.inventory.ItemStack itemStack2, List<String> itemIds) {
      try {
         ItemStack itemStack3 = CBClassWrapper.craftStackAsNMSCopy(itemStack);
         ItemStack itemStack4 = CBClassWrapper.craftStackAsNMSCopy(itemStack2);
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
                     CompoundTag compoundTag = this.getCustom((Optional<?>)reference2ObjectMap.get(dataComponentType));
                     CompoundTag compoundTag2 = this.getCustom((Optional<?>)reference2ObjectMap2.get(dataComponentType));
                     if (!this.matchCustom(
                        compoundTag,
                        compoundTag2,
                        itemIds.stream()
                           .filter(element -> element.startsWith("custom_data::"))
                           .map(element -> element.replace("custom_data::", ""))
                           .collect(Collectors.toList())
                     )) {
                        return false;
                     }
                  } else {
                     if (dataComponentType == v1_21_R7.Components.profile) {
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
      return resolvableProfile != null
         && resolvableProfile2 != null
         && resolvableProfile.name().orElse("").equals(resolvableProfile2.name().orElse(""))
         && resolvableProfile.partialProfile().properties().equals(resolvableProfile2.partialProfile().properties());
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

      ComponentKey(DataComponentType<T> dataComponentType, String key) {
         this.type = dataComponentType;
         this.nbt = key;
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

      public static DataComponentType<?> valueOf(String type) throws NoSuchFieldException, IllegalAccessException {
         Field field = v1_21_R7.Components.class.getDeclaredField(type);
         field.setAccessible(true);
         return (DataComponentType<?>)field.get(null);
      }

      public static String getName(DataComponentType<?> dataComponentType) {
         for (Field field : v1_21_R7.Components.class.getDeclaredFields()) {
            if (field.getType().equals(dataComponentType.getClass())) {
               return field.getName();
            }
         }

         return null;
      }

      public static <T> DataComponentType<T> valueOf(String type, Class<T> type2) throws NoSuchFieldException, IllegalAccessException {
         Field field = v1_21_R7.Components.class.getDeclaredField(type);
         field.setAccessible(true);
         return (DataComponentType<T>)field.get(null);
      }

      private Set<String> getRegistered() {
         Field field = v1_21_R7.getFieldByName(BuiltInRegistries.class, "DATA_COMPONENT_TYPE");
         field.setAccessible(true);

         try {
            return ((Set<?>)ReflectUtil.invokeLegacyVersionMethod("keySet", field.get(null))).stream().<String>map(element -> ((Identifier)element).getPath()).collect(Collectors.toSet());
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
         CompoundTag compoundTag = (CompoundTag)ItemStack.CODEC.encodeStart(((Provider)HOLDER).createSerializationContext(NbtOps.INSTANCE), itemStack).getOrThrow();
         return compoundTag.getCompoundOrEmpty("components").toString();
      }

      public static Optional<DataComponentPatch> deserialize(String command, boolean flag) throws CommandSyntaxException {
         CompoundTag compoundTag = TagParser.parseCompoundFully(command);
         return DataComponentPatch.CODEC
            .parse(((Provider)HOLDER).createSerializationContext(NbtOps.INSTANCE), compoundTag)
            .resultOrPartial(element -> SendMessage.errorMessage("Tried to load invalid item: '{}'", element));
      }

      private String convert(org.bukkit.inventory.ItemStack itemStack, String itemId) throws ItemLoadException {
         try {
            String id = String.format("{id:\"minecraft:%1$s\",Count:%2$d,tag:%3$s}", itemStack.getType().getKey().getKey(), 1, itemId);
            CompoundTag compoundTag = TagParser.parseCompoundFully(id);
            DataFixer dataFixer = DataFixers.getDataFixer();
            TypeReference typeReference = References.ITEM_STACK;
            NbtOps nbtOps = NbtOps.INSTANCE;
            Dynamic dynamic = dataFixer.update(typeReference, new Dynamic(nbtOps, compoundTag), 1343, 4424);
            compoundTag = (CompoundTag)dynamic.getValue();
            return compoundTag.toString();
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to convert NBT to compound, please see error below:");
            exception.printStackTrace();
            throw new ItemLoadException("Invalid NBT data found while converting to components");
         }
      }

      private String update(NBTUtils.NBTFixer nbtFixer, org.bukkit.inventory.ItemStack itemStack, String itemId) throws ItemLoadException {
         try {
            String id = String.format("{id:\"minecraft:%1$s\",count:%2$d,components:%3$s}", itemStack.getType().getKey().getKey(), 1, itemId);
            CompoundTag compoundTag = TagParser.parseCompoundFully(id);
            DataFixer dataFixer = DataFixers.getDataFixer();
            TypeReference typeReference = References.ITEM_STACK;
            NbtOps nbtOps = NbtOps.INSTANCE;
            Dynamic dynamic = dataFixer.update(typeReference, new Dynamic(nbtOps, compoundTag), nbtFixer.from, nbtFixer.to);
            compoundTag = (CompoundTag)dynamic.getValue();
            return compoundTag.toString();
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to update component data of item, see error below:");
            exception.printStackTrace();
            throw new ItemLoadException("Unknown error occurred while updating component data");
         }
      }

      private void convertMeta(ItemStack itemStack) throws IllegalAccessException {
         PatchedDataComponentMap patchedDataComponentMap = (PatchedDataComponentMap)v1_21_R7.COMPONENT_MAP.get(itemStack);
         ItemLore itemLore = (ItemLore)patchedDataComponentMap.get(v1_21_R7.Components.lore);
         if (itemLore != null) {
            List<Component> values = itemLore.lines();
            itemLore = new ItemLore(values.stream().map(element -> CBClassWrapper.chatMessageFromJSONOrString(element.getString())).collect(Collectors.toList()));
            patchedDataComponentMap.set(v1_21_R7.Components.lore, itemLore);
         }

         Component component = (Component)patchedDataComponentMap.get(v1_21_R7.Components.custom_name);
         if (component != null) {
            patchedDataComponentMap.set(v1_21_R7.Components.custom_name, CBClassWrapper.chatMessageFromJSONOrString(component.getString()));
         }
      }

      private static ItemStack removeEmpty(ItemStack itemStack) throws IllegalAccessException {
         PatchedDataComponentMap patchedDataComponentMap = (PatchedDataComponentMap)v1_21_R7.COMPONENT_MAP.get(itemStack);
         if (patchedDataComponentMap.get(v1_21_R7.Components.stored_enchantments) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.stored_enchantments, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.enchantments) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.enchantments, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.recipes) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.recipes, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.firework_explosion) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.firework_explosion, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.fireworks) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.fireworks, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.profile) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.profile, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.instrument) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.instrument, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.trim) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.trim, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.lore) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.lore, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.custom_name) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.custom_name, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.item_name) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.item_name, null);
         }

         if (patchedDataComponentMap.get(v1_21_R7.Components.suspicious_stew_effects) == null) {
            patchedDataComponentMap.set(v1_21_R7.Components.suspicious_stew_effects, null);
         }

         v1_21_R7.COMPONENT_MAP.set(itemStack, patchedDataComponentMap);
         return itemStack;
      }

      static {
         try {
            Method method = Class.forName("org.bukkit.craftbukkit.CraftServer").getDeclaredMethod("getServer");
            method.setAccessible(true);
            Object value = method.invoke(Bukkit.getServer());
            Method method2 = Class.forName("org.bukkit.craftbukkit.CraftRegistry").getDeclaredMethod("getMinecraftRegistry");
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
