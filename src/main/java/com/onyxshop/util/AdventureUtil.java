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
   public String formatMiniToLegacy(String text) {
      return legacyDeserializer.serialize(this.formatMini(text));
   }

   @Override
   public Component formatMini(String text) {
      return miniMessage.deserialize(text.replace('§', '&'));
   }

   public static String serialize(Component component) {
      return ((String)miniMessage.serialize(component)).replace("\\<", "<");
   }

   @Override
   public String formatLegacyToMini(String text) {
      return ((String)miniMessage.serialize(legacySerializer.deserialize(rgbSerializer.matcher(text.replace('§', '&')).replaceAll("<$1>"))))
         .replace("\\<", "<");
   }

   public static String toLegacy(Component component) {
      return legacyDeserializer.serialize(component);
   }

   public static String toBedrockLegacy(Component component) {
      String text = bedrockLegacySerializer.serialize(component);
      int value = text.length();
      StringBuilder builder = new StringBuilder(value);
      int value2 = 0;
      boolean flag = false;

      for (int index = 0; index < value; index++) {
         char character = text.charAt(index);
         if (character == 167 && index != value - 1) {
            char character2 = text.charAt(++index);
            if (!flag || character2 != 'r') {
               if (!flag && BEDROCK_COLORS.indexOf(character2) != -1 && value2 != 0) {
                  builder.append("§r");
               }

               builder.append("§").append(character2);
               flag = character2 == 'r';
               value2 = setBedrockFormattingFlag(value2, character2);
            }
         } else {
            builder.append(character);
            flag = false;
            if (character == '\n' && value2 != 0) {
               applyBedrockFormattingFlags(value2, builder);
            }
         }
      }

      return builder.toString();
   }

   private static int setBedrockFormattingFlag(int value, char character) {
      if (character == 'r') {
         return 0;
      } else {
         int index = BEDROCK_COLORS.indexOf(character);
         if (index == -1) {
            index = BEDROCK_DECORATIONS.indexOf(character);
            return index == -1 ? value : value | 1 << BEDROCK_COLORS.length() + index;
         } else {
            return 1 << index;
         }
      }
   }

   private static void applyBedrockFormattingFlags(int value, StringBuilder builder) {
      int value2 = BEDROCK_COLORS.length();

      for (int index = 0; index < value2; index++) {
         if ((value >> index & 1) == 1) {
            builder.append("§").append(BEDROCK_COLORS.charAt(index));
         }
      }

      for (int index = 0; index < BEDROCK_DECORATIONS.length(); index++) {
         if ((value >> index + value2 & 1) == 1) {
            builder.append("§").append(BEDROCK_DECORATIONS.charAt(index));
         }
      }
   }

   @Override
   public Component formatLegacyAndMini(String text) {
      return this.formatMini(this.formatLegacyToMini(text));
   }

   @Override
   public String formatJsonString(String text) {
      return (String)gsonSerializer.serialize(this.formatLegacyAndMini(text));
   }

   @Override
   public String getAsJson(String text) {
      return (String)gsonSerializer.serialize(legacySerializer.deserialize(text));
   }

   public static Component fromGson(String text) {
      return gsonSerializer.deserialize(text);
   }

   @Override
   public String getGsonComponent(String text) {
      return (String)gsonSerializer.serialize(this.formatMini(this.formatLegacyToMini(text)));
   }

   public static String asGson(Component component) {
      return (String)gsonSerializer.serialize(component);
   }

   public static String toMinecraftComponentRaw(String text) {
      return (String)MinecraftComponentSerializer.get().serialize(miniMessage.deserialize(text));
   }

   public static Object toMinecraftComponent(String text) {
      return MinecraftComponentSerializer.get().deserialize(toMinecraftComponentRaw(text));
   }

   public static String fromComponent(Object value) {
      return (String)MinecraftComponentSerializer.get().serialize((Component)value);
   }

   public static Object asVanilla(Translatable text) {
      try {
         return ReflectUtil.invokeStaticLegacyMethod("asVanilla", "io.papermc.paper.adventure.PaperAdventure", new Class[]{Component.class}, text.get());
      } catch (Exception exception) {
         return MinecraftComponentSerializer.get().serialize((Component)text.get());
      }
   }

   @Override
   public String stripColor(String text) {
      return tagPattern.matcher(text).replaceAll("");
   }

   @Override
   public String getLastColors(String text) {
      int value = text.length();
      LinkedHashMap<String, Boolean> map = new LinkedHashMap<>();
      String text2 = null;
      boolean flag = false;

      while (value > 0) {
         int index = text.lastIndexOf(60, value - 1);
         if (index == -1) {
            break;
         }

         int value2 = text.indexOf(62, index);
         if (value2 == -1) {
            value = index;
         } else {
            String text3 = text.substring(index + 1, value2);
            if (text3.startsWith("/")) {
               String text4 = text3.substring(1);
               if (text4.isEmpty() || AdventureUtil.Color.isValid(text4) || text3.substring(1).matches("#[0-9a-fA-F]{6}")) {
                  flag = true;
               } else if (AdventureUtil.Format.isValid(text4)) {
                  map.put(text4.toLowerCase(), false);
               }
            } else if (text2 == null && AdventureUtil.Format.isValid(text3)) {
               String text4 = text3.toLowerCase();
               if (!map.containsKey(text4)) {
                  map.put(text4, true);
               }
            } else if (text3.startsWith("#") && text3.matches("#[0-9a-fA-F]{6}") || AdventureUtil.Color.isValid(text3)) {
               text2 = text3;
               break;
            }

            value = index;
         }
      }

      if (text2 == null) {
         return "";
      } else if (flag) {
         return "";
      } else {
         StringBuilder builder = new StringBuilder("<").append(text2).append(">");
         ArrayList<String> values = new ArrayList<>();

         for (Entry<String, Boolean> entry : map.entrySet()) {
            if ((Boolean)entry.getValue()) {
               values.add((String)entry.getKey());
            }
         }

         Collections.reverse(values);

         for (String text3 : values) {
            builder.append("<").append(text3).append(">");
         }

         return builder.toString();
      }
   }

   static {
      Builder builder = null;

      try {
         builder = (Builder)ReflectUtil.invokeLegacyVersionMethod("toBuilder", LegacyComponentSerializer.legacyAmpersand());
         builder = builder.flattener(ComponentFlattener.basic());
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to initialize Adventure Utils");
         exception.printStackTrace();
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_16_R1)) {
         builder.hexColors();
         builder.useUnusualXRepeatedCharacterHexFormat();
      }

      legacySerializer = builder.build();
      ArrayList<CharacterAndFormat> values = new ArrayList<>(CharacterAndFormat.defaults());
      values.remove(CharacterAndFormat.STRIKETHROUGH);
      values.remove(CharacterAndFormat.UNDERLINED);
      values.add(CharacterAndFormat.characterAndFormat('g', TextColor.color(221, 214, 5)));
      values.add(CharacterAndFormat.characterAndFormat('h', TextColor.color(227, 212, 209)));
      values.add(CharacterAndFormat.characterAndFormat('i', TextColor.color(206, 202, 202)));
      values.add(CharacterAndFormat.characterAndFormat('j', TextColor.color(68, 58, 59)));
      values.add(CharacterAndFormat.characterAndFormat('m', TextColor.color(151, 22, 7)));
      values.add(CharacterAndFormat.characterAndFormat('n', TextColor.color(180, 104, 77)));
      values.add(CharacterAndFormat.characterAndFormat('p', TextColor.color(222, 177, 45)));
      values.add(CharacterAndFormat.characterAndFormat('q', TextColor.color(17, 160, 54)));
      values.add(CharacterAndFormat.characterAndFormat('s', TextColor.color(44, 186, 168)));
      values.add(CharacterAndFormat.characterAndFormat('t', TextColor.color(33, 73, 123)));
      values.add(CharacterAndFormat.characterAndFormat('u', TextColor.color(154, 92, 198)));
      values.add(CharacterAndFormat.characterAndFormat('v', TextColor.color(235, 114, 20)));

      LegacyComponentSerializer legacyComponentSerializer;
      try {
         legacyComponentSerializer = LegacyComponentSerializer.builder().character('§').formats(values).build();
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to initialize Bedrock Adventure Utils, falling back to standard legacy colors");
         exception.printStackTrace();
         legacyComponentSerializer = legacyDeserializer;
      }

      bedrockLegacySerializer = legacyComponentSerializer;
      StringBuilder builder2 = new StringBuilder();
      StringBuilder builder3 = new StringBuilder();

      for (CharacterAndFormat characterAndFormat : values) {
         if (characterAndFormat.format() instanceof TextColor) {
            builder2.append(characterAndFormat.character());
         }

         if (characterAndFormat.format() instanceof TextDecoration) {
            builder3.append(characterAndFormat.character());
         }
      }

      BEDROCK_COLORS = builder2.toString();
      BEDROCK_DECORATIONS = builder3.toString();
      net.kyori.adventure.text.minimessage.MiniMessage.Builder builder4 = MiniMessage.builder();
      builder4.tags(TagResolver.standard());
      builder4.strict(false);
      miniMessage = builder4.build();
      HashSet values2 = new HashSet();
      values2.addAll(AdventureUtil.Color.names());
      values2.addAll(AdventureUtil.Format.names());
      values2.addAll(AdventureUtil.Special.names());
      tagPattern = Pattern.compile("</?!?(?:(" + String.join("|", values2) + ")(?::[^>]+)?|#[0-9a-fA-F]{6})>");
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

      private Color(String text) {
         this.name = text;
      }

      public static List<String> names() {
         return Arrays.stream(values()).map(AdventureUtil.Color::getName).collect(Collectors.toList());
      }

      public String getName() {
         return this.name;
      }

      private static boolean isValid(String text) {
         for (AdventureUtil.Color color : values()) {
            if (color.name.equals(text.toLowerCase(Locale.ENGLISH))) {
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

      private Format(String... values) {
         this.names = values;
      }

      public static List<String> names() {
         return Arrays.stream(values()).flatMap(element -> Arrays.stream(element.getNames())).collect(Collectors.toList());
      }

      public String[] getNames() {
         return this.names;
      }

      private static boolean isValid(String text) {
         text = text.toLowerCase(Locale.ROOT);

         for (AdventureUtil.Format format : values()) {
            for (String text2 : format.names) {
               if (text2.equals(text)) {
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

      private Special(String... values) {
         this.names = values;
      }

      public static List<String> names() {
         return Arrays.stream(values()).flatMap(element -> Arrays.stream(element.getNames())).collect(Collectors.toList());
      }

      public String[] getNames() {
         return this.names;
      }

      private static boolean isValid(String text) {
         text = text.toLowerCase(Locale.ROOT);

         for (AdventureUtil.Special special : values()) {
            for (String text2 : special.names) {
               if (text2.equals(text)) {
                  return true;
               }
            }
         }

         return false;
      }
   }
}
