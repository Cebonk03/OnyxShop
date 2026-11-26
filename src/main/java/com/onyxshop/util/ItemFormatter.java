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

   public static Map<String, String> serialize(String var0) {
      return getData(var0);
   }

   private static Map<String, String> getData(String var0) {
      HashMap var1 = new HashMap();

      for (String var3 : allKeys) {
         int var4 = var0.indexOf(var3 + ":");
         if (var4 != -1 && (var4 == 0 || var0.charAt(var4 - 1) == ' ')) {
            var1.put(var3, getValue(var0, var3));
         }
      }

      return var1;
   }

   private static String getValue(String var0, String var1) {
      String var2 = var0.split(var1 + ":", 2)[1];
      char[] var3 = var2.toCharArray();

      for (int var4 = 0; var4 < var3.length; var4++) {
         if (var3[var4] == ':') {
            String var5 = var2.substring(0, var4);
            if (!allKeys.stream().noneMatch(var1x -> var5.endsWith(' ' + var1x))) {
               char[] var6 = var5.toCharArray();

               for (int var7 = var6.length; var7 > 0; var7--) {
                  if (var6[var7 - 1] == ' ') {
                     return var5.substring(0, var7 - 1);
                  }
               }
            }
         }
      }

      return var2;
   }

   public static ItemStack build(Map<String, String> var0, String var1, int var2) throws ItemLoadException {
      ItemStack var3 = CreateItemMethodes.createItemMaterialFromString(null, (String)var0.get("material"));
      boolean var4 = false;
      ItemBuilder var5 = new ItemBuilder(var3);
      if (var0.containsKey("amount")) {
         var4 = true;
         if (true) {
            var5.setAmount(Integer.parseInt((String)var0.get("amount")));
         }
      }

      if (var0.containsKey("name")) {
         var4 = true;
         if (true) {
            var5.setDisplayName(SendMessage.fromConfig((String)var0.get("name")));
         }
      }

      if (var0.containsKey("lore")) {
         var4 = true;
         if (true) {
            var5.setCLore(Arrays.stream(((String)var0.get("lore")).split("\\\\n")).map(SendMessage::fromConfig).collect(Collectors.toList()));
         }
      }

      if (var0.containsKey("skullowner")) {
         var4 = true;
         if (true) {
            var5.setOwner((String)var0.get("skullowner"));
         }
      }

      if (var0.containsKey("skull-texture")) {
         var4 = true;
         if (true) {
            var5.setTexture((String)var0.get("skull-texture"));
         }
      }

      if (var0.containsKey("item-flags")) {
         var4 = true;
         if (true) {
            var5.withItemFlags(Arrays.asList(((String)var0.get("item-flags")).split(",")));
         }
      }

      try {
         if (var0.containsKey("CustomModelData")) {
            var4 = true;
            if (true) {
               var5.withCustomModelData(Integer.parseInt((String)var0.get("CustomModelData")));
            }
         }
      } catch (NumberFormatException var9) {
      }

      if (var4) {
         var3 = var5.build();
      }

      if (var0.containsKey("instrument")) {
         var3 = CreateItemMethodes.setInstrument(var3, (String)var0.get("instrument"));
      }

      if (var0.containsKey("stew-effect")) {
         var3 = CreateItemMethodes.setStewEffect(var3, (String)var0.get("stew-effect"));
      }

      if (var0.containsKey("potiontypes")) {
         var3 = CreateItemMethodes.setPotionTypes(
            var3, Arrays.asList(((String)var0.get("potiontypes")).split(",")), null, var1, var3.getType().equals(XMaterial.SPLASH_POTION.parseMaterial())
         );
      }

      if (var0.containsKey("enchantments")) {
         var3 = CreateItemMethodes.setEnchantments(var3, Arrays.asList(((String)var0.get("enchantments")).split(",")), null, var1, null);
      }

      if (var0.containsKey("item-flags")) {
         var3 = new ItemBuilder(var3).withItemFlags(Arrays.asList(((String)var0.get("item-flags")).split(","))).build();
      }

      try {
         String var6 = NBTUtils.getNBT(new ItemStack(var3), var0, var1, var2, true);
         if (var6 != null) {
            var3 = CreateItemMethodes.setNBTData(var3, var6, false);
         }

         if (!ServerInfo.supportsComponents() && var0.containsKey("CustomModelData")) {
            var3 = plugin.versionHandler.setNBTInt(var3, "CustomModelData", Integer.parseInt((String)var0.get("CustomModelData")));
         }

         return var3;
      } catch (JsonParseException var7) {
         throw new ItemLoadException("Failed to parse NBTData '" + (String)var0.get("NBTData") + "' with reason: " + var7.getMessage());
      } catch (NumberFormatException var8) {
         throw new ItemLoadException("Failed to parse CustomModelData with value: '" + (String)var0.get("CustomModelData") + "'");
      }
   }

   private static String getMaterial(ItemStack var0) {
      try {
         return XMaterial.matchXMaterial(var0).name();
      } catch (IllegalArgumentException var2) {
         return var0.getType().name();
      }
   }

   public static String deserialize(ItemStack var0, boolean var1) {
      HashMap<String, String> var2 = new HashMap<>();
      if (!var1) {
         String var3 = plugin.getItemManager().getItem(var0);
         if (var3 != null) {
            var2.put("material", var3);
            return var2.entrySet().stream().map(var0x -> (String)var0x.getKey() + ":" + (String)var0x.getValue()).collect(Collectors.joining(" "));
         }
      }

      var2.put("material", var0.getType().name());
      if (var0.hasItemMeta() && !var1) {
         ItemMeta var7 = var0.getItemMeta();
         if (var7.hasDisplayName()) {
            var2.put("name", var7.getDisplayName());
         }

         if (var7.hasLore()) {
            StringBuilder var4 = new StringBuilder();

            for (int var5 = 0; var5 < var7.getLore().size(); var5++) {
               if (var5 != 0) {
                  var4.append("\\n");
               }

               var4.append((String)var7.getLore().get(var5));
            }

            var2.put("lore", var4.toString());
         }

         if (var7 instanceof SkullMeta) {
            SkullMeta var9 = (SkullMeta)var7;
            if (SkullUtil.hasSkullOwner(var9)) {
               var2.put("skullowner", var9.getOwner());
            } else {
               String var14 = SkullUtil.getSkullTexture(var9);
               if (var14 != null) {
                  var2.put("skull-texture", var14);
               }

               if (var9.hasOwner()) {
                  var2.put("skullowner", var9.getOwner());
               }
            }
         }

         if (plugin.version >= 114 && var7 instanceof SuspiciousStewMeta) {
            List<org.bukkit.potion.PotionEffect> var10 = ((SuspiciousStewMeta)var7).getCustomEffects();
            if (!var10.isEmpty()) {
               var2.put("stew-effect", var10.stream().map(var0x -> var0x.getType().getName()).collect(Collectors.joining(",")));
            }
         }

         if (PotionTypes.canHaveEffects(var0)) {
            List<com.onyxshop.util.PotionTypes> var11 = PotionTypes.getPotionTypes(var0);
            if (!var11.isEmpty()) {
               var2.put("potiontypes", var11.stream().map(Enum::name).collect(Collectors.joining(",")));
            }
         }

         List var12 = plugin.enchantsProvider.getEnchantKeys(var0);
         if (!var12.isEmpty()) {
            var2.put("enchantments", String.join(",", var12));
         }

         if (plugin.version >= 119 && var7 instanceof MusicInstrumentMeta) {
            var2.put("instrument", ((MusicInstrumentMeta)var7).getInstrument().getKey().getKey());
         }

         if (!var7.getItemFlags().isEmpty()) {
            var2.put("item-flags", var7.getItemFlags().stream().map(Enum::name).collect(Collectors.joining(",")));
         }

         if (var7.hasCustomModelData()) {
            var2.put("CustomModelData", String.valueOf(var7.getCustomModelData()));
         }
      }

      try {
         String var8 = plugin.versionHandler.getNBTDataAsString(var0, var1);
         if (var8 != null) {
            String var13 = !var1 ? getValidKeys(var8) : var8;
            if (var13 == null) {
               throw new NullPointerException();
            }

            var2.put(ServerInfo.supportsComponents() ? "components" : "NBTData", var13);
         }
      } catch (Exception var6) {
         SendMessage.warnMessage("Failed to parse nbt from item, skipping...");
         var6.printStackTrace();
      }

      return var2.entrySet().stream().map(var0x -> (String)var0x.getKey() + ":" + (String)var0x.getValue()).collect(Collectors.joining(" "));
   }

   private static String getValidKeys(String var0) throws ParseException {
      try {
         Object var1 = parseTag(var0);

         for (String var8 : getKeys(var1)) {
            if (var8.equalsIgnoreCase("display")
               || var8.equalsIgnoreCase("Potion")
               || var8.equalsIgnoreCase("SkullOwner")
               || var8.equalsIgnoreCase("Effects")
               || allKeys.contains(var8)) {
               remove(var1, var8);
            }
         }

         return var1.toString();
      } catch (Exception var6) {
         JSONObject var2 = JsonUtil.parseJson(var0);
         ArrayList var3 = new ArrayList();

         for (Object var5 : var2.keySet()) {
            if (((String)var5).equalsIgnoreCase("display")
               || ((String)var5).equalsIgnoreCase("Potion")
               || ((String)var5).equalsIgnoreCase("SkullOwner")
               || ((String)var5).equalsIgnoreCase("Effects")
               || allKeys.contains(var5)) {
               var3.add((String)var5);
            }
         }

         var3.forEach(var2::remove);
         return var2.toString();
      }
   }

   private static Object parseTag(String var0) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      Class var1 = Class.forName(
         plugin.version > 116 ? "net.minecraft.nbt.MojangsonParser" : "net.minecraft.server." + ServerInfo.getVersion().name() + ".MojangsonParser"
      );
      Method var2 = var1.getDeclaredMethod(plugin.version > 117 ? "a" : "parse", String.class);
      var2.setAccessible(true);
      return var2.invoke(var1, var0);
   }

   private static Set<String> getKeys(Object var0) throws NoSuchFieldException , IllegalAccessException{
      Field var1 = var0.getClass().getDeclaredField(plugin.version > 116 ? "x" : "map");
      var1.setAccessible(true);
      return ((Map)var1.get(var0)).keySet();
   }

   private static void remove(Object var0, String var1) throws NoSuchMethodException , IllegalAccessException, InvocationTargetException{
      Method var2 = var0.getClass().getDeclaredMethod(plugin.version > 117 ? "r" : "remove", String.class);
      var2.setAccessible(true);
      var2.invoke(var0, var1);
   }
}
