package com.onyxshop.util;

import com.google.gson.JsonParseException;
import java.lang.reflect.InvocationTargetException;
import org.json.simple.parser.ParseException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.CreateItemMethodes;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.json.simple.JSONObject;

public class ItemFormatter {
   private static final List<String> allKeys = Arrays.asList(
      "material",
      "name",
      "lore",
      "skullowner",
      "skull-texture",
      "instrument",
      "stew-effect",
      "enchantments",
      "NBTData",
      "components",
      "potiontypes",
      "CustomModelData",
      "ignored-tags",
      "id",
      "item-flags",
      "amount"
   );
   private static final OnyxShop plugin = OnyxShop.getInstance();

   public ItemFormatter() {
   }

   public static Map<String, String> serialize(String text) {
      return getData(text);
   }

   private static Map<String, String> getData(String text) {
      HashMap map = new HashMap();

      for (String text2 : allKeys) {
         int index = text.indexOf(text2 + ":");
         if (index != -1 && (index == 0 || text.charAt(index - 1) == ' ')) {
            map.put(text2, getValue(text, text2));
         }
      }

      return map;
   }

   private static String getValue(String text, String text2) {
      String text3 = text.split(text2 + ":", 2)[1];
      char[] values = text3.toCharArray();

      for (int index = 0; index < values.length; index++) {
         if (values[index] == ':') {
            String text4 = text3.substring(0, index);
            if (!allKeys.stream().noneMatch(element -> text4.endsWith(' ' + element))) {
               char[] values2 = text4.toCharArray();

               for (int index2 = values2.length; index2 > 0; index2--) {
                  if (values2[index2 - 1] == ' ') {
                     return text4.substring(0, index2 - 1);
                  }
               }
            }
         }
      }

      return text3;
   }

   public static ItemStack build(Map<String, String> itemIds, String text, int value) throws ItemLoadException {
      ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, (String)itemIds.get("material"));
      boolean flag = false;
      ItemBuilder itemBuilder = new ItemBuilder(itemStack);
      if (itemIds.containsKey("amount")) {
         flag = true;
         if (true) {
            itemBuilder.setAmount(Integer.parseInt((String)itemIds.get("amount")));
         }
      }

      if (itemIds.containsKey("name")) {
         flag = true;
         if (true) {
            itemBuilder.setDisplayName(SendMessage.fromConfig((String)itemIds.get("name")));
         }
      }

      if (itemIds.containsKey("lore")) {
         flag = true;
         if (true) {
            itemBuilder.setCLore(Arrays.stream(((String)itemIds.get("lore")).split("\\\\n")).map(SendMessage::fromConfig).collect(Collectors.toList()));
         }
      }

      if (itemIds.containsKey("skullowner")) {
         flag = true;
         if (true) {
            itemBuilder.setOwner((String)itemIds.get("skullowner"));
         }
      }

      if (itemIds.containsKey("skull-texture")) {
         flag = true;
         if (true) {
            itemBuilder.setTexture((String)itemIds.get("skull-texture"));
         }
      }

      if (itemIds.containsKey("item-flags")) {
         flag = true;
         if (true) {
            itemBuilder.withItemFlags(Arrays.asList(((String)itemIds.get("item-flags")).split(",")));
         }
      }

      try {
         if (itemIds.containsKey("CustomModelData")) {
            flag = true;
            if (true) {
               itemBuilder.withCustomModelData(Integer.parseInt((String)itemIds.get("CustomModelData")));
            }
         }
      } catch (NumberFormatException exception) {
      }

      if (flag) {
         itemStack = itemBuilder.build();
      }

      if (itemIds.containsKey("instrument")) {
         itemStack = CreateItemMethodes.setInstrument(itemStack, (String)itemIds.get("instrument"));
      }

      if (itemIds.containsKey("stew-effect")) {
         itemStack = CreateItemMethodes.setStewEffect(itemStack, (String)itemIds.get("stew-effect"));
      }

      if (itemIds.containsKey("potiontypes")) {
         itemStack = CreateItemMethodes.setPotionTypes(
            itemStack, Arrays.asList(((String)itemIds.get("potiontypes")).split(",")), null, text, itemStack.getType().equals(XMaterial.SPLASH_POTION.parseMaterial())
         );
      }

      if (itemIds.containsKey("enchantments")) {
         itemStack = CreateItemMethodes.setEnchantments(itemStack, Arrays.asList(((String)itemIds.get("enchantments")).split(",")), null, text, null);
      }

      if (itemIds.containsKey("item-flags")) {
         itemStack = new ItemBuilder(itemStack).withItemFlags(Arrays.asList(((String)itemIds.get("item-flags")).split(","))).build();
      }

      try {
         String nbt = NBTUtils.getNBT(new ItemStack(itemStack), itemIds, text, value, true);
         if (nbt != null) {
            itemStack = CreateItemMethodes.setNBTData(itemStack, nbt, false);
         }

         if (!ServerInfo.supportsComponents() && itemIds.containsKey("CustomModelData")) {
            itemStack = plugin.versionHandler.setNBTInt(itemStack, "CustomModelData", Integer.parseInt((String)itemIds.get("CustomModelData")));
         }

         return itemStack;
      } catch (JsonParseException exception) {
         throw new ItemLoadException("Failed to parse NBTData '" + (String)itemIds.get("NBTData") + "' with reason: " + exception.getMessage());
      } catch (NumberFormatException exception) {
         throw new ItemLoadException("Failed to parse CustomModelData with value: '" + (String)itemIds.get("CustomModelData") + "'");
      }
   }

   private static String getMaterial(ItemStack itemStack) {
      try {
         return XMaterial.matchXMaterial(itemStack).name();
      } catch (IllegalArgumentException exception) {
         return itemStack.getType().name();
      }
   }

   public static String deserialize(ItemStack itemStack, boolean flag) {
      HashMap<String, String> map = new HashMap<>();
      if (!flag) {
         String item = plugin.getItemManager().getItem(itemStack);
         if (item != null) {
            map.put("material", item);
            return map.entrySet().stream().map(entry -> (String)entry.getKey() + ":" + (String)entry.getValue()).collect(Collectors.joining(" "));
         }
      }

      map.put("material", itemStack.getType().name());
      if (itemStack.hasItemMeta() && !flag) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            map.put("name", itemMeta.getDisplayName());
         }

         if (itemMeta.hasLore()) {
            StringBuilder builder = new StringBuilder();

            for (int size = 0; size < itemMeta.getLore().size(); size++) {
               if (size != 0) {
                  builder.append("\\n");
               }

               builder.append((String)itemMeta.getLore().get(size));
            }

            map.put("lore", builder.toString());
         }

         if (itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta)itemMeta;
            if (SkullUtil.hasSkullOwner(skullMeta)) {
               map.put("skullowner", skullMeta.getOwner());
            } else {
               String skullTexture = SkullUtil.getSkullTexture(skullMeta);
               if (skullTexture != null) {
                  map.put("skull-texture", skullTexture);
               }

               if (skullMeta.hasOwner()) {
                  map.put("skullowner", skullMeta.getOwner());
               }
            }
         }

         if (plugin.version >= 114 && itemMeta instanceof SuspiciousStewMeta) {
            List<org.bukkit.potion.PotionEffect> customEffects = ((SuspiciousStewMeta)itemMeta).getCustomEffects();
            if (!customEffects.isEmpty()) {
               map.put("stew-effect", customEffects.stream().map(element -> element.getType().getName()).collect(Collectors.joining(",")));
            }
         }

         if (PotionTypes.canHaveEffects(itemStack)) {
            List<com.onyxshop.util.PotionTypes> potionTypes = PotionTypes.getPotionTypes(itemStack);
            if (!potionTypes.isEmpty()) {
               map.put("potiontypes", potionTypes.stream().map(Enum::name).collect(Collectors.joining(",")));
            }
         }

         List enchantKeys = plugin.enchantsProvider.getEnchantKeys(itemStack);
         if (!enchantKeys.isEmpty()) {
            map.put("enchantments", String.join(",", enchantKeys));
         }

         if (plugin.version >= 119 && itemMeta instanceof MusicInstrumentMeta) {
            map.put("instrument", ((MusicInstrumentMeta)itemMeta).getInstrument().getKey().getKey());
         }

         if (!itemMeta.getItemFlags().isEmpty()) {
            map.put("item-flags", itemMeta.getItemFlags().stream().map(Enum::name).collect(Collectors.joining(",")));
         }

         if (itemMeta.hasCustomModelData()) {
            map.put("CustomModelData", String.valueOf(itemMeta.getCustomModelData()));
         }
      }

      try {
         String nbtDataAsString = plugin.versionHandler.getNBTDataAsString(itemStack, flag);
         if (nbtDataAsString != null) {
            String text = !flag ? getValidKeys(nbtDataAsString) : nbtDataAsString;
            if (text == null) {
               throw new NullPointerException();
            }

            map.put(ServerInfo.supportsComponents() ? "components" : "NBTData", text);
         }
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to parse nbt from item, skipping...");
         exception.printStackTrace();
      }

      return map.entrySet().stream().map(entry -> (String)entry.getKey() + ":" + (String)entry.getValue()).collect(Collectors.joining(" "));
   }

   private static String getValidKeys(String text) throws ParseException {
      try {
         Object value = parseTag(text);

         for (String text2 : getKeys(value)) {
            if (text2.equalsIgnoreCase("display")
               || text2.equalsIgnoreCase("Potion")
               || text2.equalsIgnoreCase("SkullOwner")
               || text2.equalsIgnoreCase("Effects")
               || allKeys.contains(text2)) {
               remove(value, text2);
            }
         }

         return value.toString();
      } catch (Exception exception) {
         JSONObject jsonObject = JsonUtil.parseJson(text);
         ArrayList values = new ArrayList();

         for (Object value : jsonObject.keySet()) {
            if (((String)value).equalsIgnoreCase("display")
               || ((String)value).equalsIgnoreCase("Potion")
               || ((String)value).equalsIgnoreCase("SkullOwner")
               || ((String)value).equalsIgnoreCase("Effects")
               || allKeys.contains(value)) {
               values.add((String)value);
            }
         }

         values.forEach(jsonObject::remove);
         return jsonObject.toString();
      }
   }

   private static Object parseTag(String text) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      Class type = Class.forName(
         plugin.version > 116 ? "net.minecraft.nbt.MojangsonParser" : "net.minecraft.server." + ServerInfo.getVersion().name() + ".MojangsonParser"
      );
      Method method = type.getDeclaredMethod(plugin.version > 117 ? "a" : "parse", String.class);
      method.setAccessible(true);
      return method.invoke(type, text);
   }

   private static Set<String> getKeys(Object value) throws NoSuchFieldException , IllegalAccessException{
      Field field = value.getClass().getDeclaredField(plugin.version > 116 ? "x" : "map");
      field.setAccessible(true);
      return ((Map)field.get(value)).keySet();
   }

   private static void remove(Object value, String text) throws NoSuchMethodException , IllegalAccessException, InvocationTargetException{
      Method method = value.getClass().getDeclaredMethod(plugin.version > 117 ? "r" : "remove", String.class);
      method.setAccessible(true);
      method.invoke(value, text);
   }
}
