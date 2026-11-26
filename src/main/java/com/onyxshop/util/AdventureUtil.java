package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.Builder;

public class AdventureUtil extends AdventureUtils {
   private static final Pattern tagPattern;
   private static final Pattern rgbSerializer = Pattern.compile("(?<![:/<&{])(#([0-9a-fA-F]{6}))");
   private static final String BASE = "§";
   private static final String RESET = "§r";
   private static final GsonComponentSerializer legacyGsonSerializer = GsonComponentSerializer.colorDownsamplingGson();
   private static final GsonComponentSerializer gsonSerializer = GsonComponentSerializer.gson();
   private static final LegacyComponentSerializer legacyDeserializer = LegacyComponentSerializer.legacySection();
   private static final LegacyComponentSerializer legacySerializer;
   private static final LegacyComponentSerializer bedrockLegacySerializer;
   private static final String BEDROCK_COLORS;
   private static final String BEDROCK_DECORATIONS;
   private static final MiniMessage miniMessage;

   public AdventureUtil() {
   }

   @Override
   public String formatMiniToLegacy(String var1) {
      return legacyDeserializer.serialize(this.formatMini(var1));
   }

   @Override
   public Component formatMini(String var1) {
      return miniMessage.deserialize(var1.replace('§', '&'));
   }

   public static String serialize(Component var0) {
      return ((String)miniMessage.serialize(var0)).replace("\\<", "<");
   }

   @Override
   public String formatLegacyToMini(String var1) {
      return ((String)miniMessage.serialize(legacySerializer.deserialize(rgbSerializer.matcher(var1.replace('§', '&')).replaceAll("<$1>"))))
         .replace("\\<", "<");
   }

   public static String toLegacy(Component var0) {
      return legacyDeserializer.serialize(var0);
   }

   public static String toBedrockLegacy(Component var0) {
      String var1 = bedrockLegacySerializer.serialize(var0);
      int var2 = var1.length();
      StringBuilder var3 = new StringBuilder(var2);
      int var4 = 0;
      boolean var5 = false;

      for (int var6 = 0; var6 < var2; var6++) {
         char var7 = var1.charAt(var6);
         if (var7 == 167 && var6 != var2 - 1) {
            char var8 = var1.charAt(++var6);
            if (!var5 || var8 != 'r') {
               if (!var5 && BEDROCK_COLORS.indexOf(var8) != -1 && var4 != 0) {
                  var3.append("§r");
               }

               var3.append("§").append(var8);
               var5 = var8 == 'r';
               var4 = setBedrockFormattingFlag(var4, var8);
            }
         } else {
            var3.append(var7);
            var5 = false;
            if (var7 == '\n' && var4 != 0) {
               applyBedrockFormattingFlags(var4, var3);
            }
         }
      }

      return var3.toString();
   }

   private static int setBedrockFormattingFlag(int var0, char var1) {
      if (var1 == 'r') {
         return 0;
      } else {
         int var2 = BEDROCK_COLORS.indexOf(var1);
         if (var2 == -1) {
            var2 = BEDROCK_DECORATIONS.indexOf(var1);
            return var2 == -1 ? var0 : var0 | 1 << BEDROCK_COLORS.length() + var2;
         } else {
            return 1 << var2;
         }
      }
   }

   private static void applyBedrockFormattingFlags(int var0, StringBuilder var1) {
      int var2 = BEDROCK_COLORS.length();

      for (int var3 = 0; var3 < var2; var3++) {
         if ((var0 >> var3 & 1) == 1) {
            var1.append("§").append(BEDROCK_COLORS.charAt(var3));
         }
      }

      for (int var4 = 0; var4 < BEDROCK_DECORATIONS.length(); var4++) {
         if ((var0 >> var4 + var2 & 1) == 1) {
            var1.append("§").append(BEDROCK_DECORATIONS.charAt(var4));
         }
      }
   }

   @Override
   public Component formatLegacyAndMini(String var1) {
      return this.formatMini(this.formatLegacyToMini(var1));
   }

   @Override
   public String formatJsonString(String var1) {
      return (String)gsonSerializer.serialize(this.formatLegacyAndMini(var1));
   }

   @Override
   public String getAsJson(String var1) {
      return (String)gsonSerializer.serialize(legacySerializer.deserialize(var1));
   }

   public static Component fromGson(String var0) {
      return gsonSerializer.deserialize(var0);
   }

   @Override
   public String getGsonComponent(String var1) {
      return (String)gsonSerializer.serialize(this.formatMini(this.formatLegacyToMini(var1)));
   }

   public static String asGson(Component var0) {
      return (String)gsonSerializer.serialize(var0);
   }

   public static String toMinecraftComponentRaw(String var0) {
      return (String)MinecraftComponentSerializer.get().serialize(miniMessage.deserialize(var0));
   }

   public static Object toMinecraftComponent(String var0) {
      return MinecraftComponentSerializer.get().deserialize(toMinecraftComponentRaw(var0));
   }

   public static String fromComponent(Object var0) {
      return (String)MinecraftComponentSerializer.get().serialize((Component)var0);
   }

   public static Object asVanilla(Translatable var0) {
      try {
         return ReflectUtil.invokeStaticLegacyMethod("asVanilla", "io.papermc.paper.adventure.PaperAdventure", new Class[]{Component.class}, var0.get());
      } catch (Exception var2) {
         return MinecraftComponentSerializer.get().serialize((Component)var0.get());
      }
   }

   @Override
   public String stripColor(String var1) {
      return tagPattern.matcher(var1).replaceAll("");
   }

   @Override
   public String getLastColors(String var1) {
      int var2 = var1.length();
      LinkedHashMap<String, Boolean> var3 = new LinkedHashMap<>();
      String var4 = null;
      boolean var5 = false;

      while (var2 > 0) {
         int var6 = var1.lastIndexOf(60, var2 - 1);
         if (var6 == -1) {
            break;
         }

         int var7 = var1.indexOf(62, var6);
         if (var7 == -1) {
            var2 = var6;
         } else {
            String var8 = var1.substring(var6 + 1, var7);
            if (var8.startsWith("/")) {
               String var14 = var8.substring(1);
               if (var14.isEmpty() || AdventureUtil.Color.isValid(var14) || var8.substring(1).matches("#[0-9a-fA-F]{6}")) {
                  var5 = true;
               } else if (AdventureUtil.Format.isValid(var14)) {
                  var3.put(var14.toLowerCase(), false);
               }
            } else if (var4 == null && AdventureUtil.Format.isValid(var8)) {
               String var9 = var8.toLowerCase();
               if (!var3.containsKey(var9)) {
                  var3.put(var9, true);
               }
            } else if (var8.startsWith("#") && var8.matches("#[0-9a-fA-F]{6}") || AdventureUtil.Color.isValid(var8)) {
               var4 = var8;
               break;
            }

            var2 = var6;
         }
      }

      if (var4 == null) {
         return "";
      } else if (var5) {
         return "";
      } else {
         StringBuilder var10 = new StringBuilder("<").append(var4).append(">");
         ArrayList<String> var11 = new ArrayList<>();

         for (Entry<String, Boolean> var15 : var3.entrySet()) {
            if ((Boolean)var15.getValue()) {
               var11.add((String)var15.getKey());
            }
         }

         Collections.reverse(var11);

         for (String var16 : var11) {
            var10.append("<").append(var16).append(">");
         }

         return var10.toString();
      }
   }

   static {
      Builder var0 = null;

      try {
         var0 = (Builder)ReflectUtil.invokeLegacyVersionMethod("toBuilder", LegacyComponentSerializer.legacyAmpersand());
         var0 = var0.flattener(ComponentFlattener.basic());
      } catch (Exception var8) {
         SendMessage.errorMessage("Failed to initialize Adventure Utils");
         var8.printStackTrace();
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_16_R1)) {
         var0.hexColors();
         var0.useUnusualXRepeatedCharacterHexFormat();
      }

      legacySerializer = var0.build();
      ArrayList<CharacterAndFormat> var1 = new ArrayList<>(CharacterAndFormat.defaults());
      var1.remove(CharacterAndFormat.STRIKETHROUGH);
      var1.remove(CharacterAndFormat.UNDERLINED);
      var1.add(CharacterAndFormat.characterAndFormat('g', TextColor.color(221, 214, 5)));
      var1.add(CharacterAndFormat.characterAndFormat('h', TextColor.color(227, 212, 209)));
      var1.add(CharacterAndFormat.characterAndFormat('i', TextColor.color(206, 202, 202)));
      var1.add(CharacterAndFormat.characterAndFormat('j', TextColor.color(68, 58, 59)));
      var1.add(CharacterAndFormat.characterAndFormat('m', TextColor.color(151, 22, 7)));
      var1.add(CharacterAndFormat.characterAndFormat('n', TextColor.color(180, 104, 77)));
      var1.add(CharacterAndFormat.characterAndFormat('p', TextColor.color(222, 177, 45)));
      var1.add(CharacterAndFormat.characterAndFormat('q', TextColor.color(17, 160, 54)));
      var1.add(CharacterAndFormat.characterAndFormat('s', TextColor.color(44, 186, 168)));
      var1.add(CharacterAndFormat.characterAndFormat('t', TextColor.color(33, 73, 123)));
      var1.add(CharacterAndFormat.characterAndFormat('u', TextColor.color(154, 92, 198)));
      var1.add(CharacterAndFormat.characterAndFormat('v', TextColor.color(235, 114, 20)));

      LegacyComponentSerializer var2;
      try {
         var2 = LegacyComponentSerializer.builder().character('§').formats(var1).build();
      } catch (Exception var7) {
         SendMessage.errorMessage("Failed to initialize Bedrock Adventure Utils, falling back to standard legacy colors");
         var7.printStackTrace();
         var2 = legacyDeserializer;
      }

      bedrockLegacySerializer = var2;
      StringBuilder var3 = new StringBuilder();
      StringBuilder var4 = new StringBuilder();

      for (CharacterAndFormat var6 : var1) {
         if (var6.format() instanceof TextColor) {
            var3.append(var6.character());
         }

         if (var6.format() instanceof TextDecoration) {
            var4.append(var6.character());
         }
      }

      BEDROCK_COLORS = var3.toString();
      BEDROCK_DECORATIONS = var4.toString();
      net.kyori.adventure.text.minimessage.MiniMessage.Builder var9 = MiniMessage.builder();
      var9.tags(TagResolver.standard());
      var9.strict(false);
      miniMessage = var9.build();
      HashSet var10 = new HashSet();
      var10.addAll(AdventureUtil.Color.names());
      var10.addAll(AdventureUtil.Format.names());
      var10.addAll(AdventureUtil.Special.names());
      tagPattern = Pattern.compile("</?!?(?:(" + String.join("|", var10) + ")(?::[^>]+)?|#[0-9a-fA-F]{6})>");
   }

   private static enum Color {
      BLACK("black"),
      DARK_BLUE("dark_blue"),
      DARK_GREEN("dark_green"),
      DARK_AQUA("dark_aqua"),
      DARK_RED("dark_red"),
      DARK_PURPLE("dark_purple"),
      GOLD("gold"),
      GRAY("gray"),
      DARK_GRAY("dark_gray"),
      BLUE("blue"),
      GREEN("green"),
      AQUA("aqua"),
      RED("red"),
      LIGHT_PURPLE("light_purple"),
      YELLOW("yellow"),
      WHITE("white");

      private final String name;

      private Color(String nullxx) {
         this.name = nullxx;
      }

      public static List<String> names() {
         return Arrays.stream(values()).map(AdventureUtil.Color::getName).collect(Collectors.toList());
      }

      public String getName() {
         return this.name;
      }

      private static boolean isValid(String var0) {
         for (AdventureUtil.Color var4 : values()) {
            if (var4.name.equals(var0.toLowerCase(Locale.ENGLISH))) {
               return true;
            }
         }

         return false;
      }
   }

   private static enum Format {
      BOLD("bold", "b"),
      ITALIC("italic", "i", "em"),
      UNDERLINED("underlined", "u"),
      STRIKETHROUGH("strikethrough", "st"),
      OBFUSCATED("obfuscated", "obf");

      private final String[] names;

      private Format(String... nullxx) {
         this.names = nullxx;
      }

      public static List<String> names() {
         return Arrays.stream(values()).flatMap(var0 -> Arrays.stream(var0.getNames())).collect(Collectors.toList());
      }

      public String[] getNames() {
         return this.names;
      }

      private static boolean isValid(String var0) {
         var0 = var0.toLowerCase(Locale.ROOT);

         for (AdventureUtil.Format var4 : values()) {
            for (String var8 : var4.names) {
               if (var8.equals(var0)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static enum Special {
      COLOR("color", "colour", "c"),
      HOVER("hover"),
      CLICK("click"),
      KEY("key"),
      TRANSLATABLE("lang", "translatable", "tr"),
      TRANSLATABLE_FALLBACK("lang_or", "translate_or", "tr_or"),
      INSERTION("insertion"),
      FONT("font"),
      GRADIENT("gradient"),
      RAINBOW("rainbow"),
      TRANSITION("transition"),
      RESET("reset"),
      NEWLINE("newLine", "br"),
      SELECTOR("selector", "sel"),
      SCORE("score"),
      NBT("nbt", "data");

      private final String[] names;

      private Special(String... nullxx) {
         this.names = nullxx;
      }

      public static List<String> names() {
         return Arrays.stream(values()).flatMap(var0 -> Arrays.stream(var0.getNames())).collect(Collectors.toList());
      }

      public String[] getNames() {
         return this.names;
      }

      private static boolean isValid(String var0) {
         var0 = var0.toLowerCase(Locale.ROOT);

         for (AdventureUtil.Special var4 : values()) {
            for (String var8 : var4.names) {
               if (var8.equals(var0)) {
                  return true;
               }
            }
         }

         return false;
      }
   }
}
