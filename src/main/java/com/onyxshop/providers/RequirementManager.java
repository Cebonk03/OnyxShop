package com.onyxshop.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.providers.requirements.ItemRequirement;
import com.onyxshop.providers.requirements.RequirementType;
import com.onyxshop.providers.requirements.Requirements;
import com.onyxshop.providers.requirements.items.LevelRequirement;
import com.onyxshop.providers.requirements.items.PermissionRequirement;
import com.onyxshop.providers.requirements.items.PlaceholderRequirement;
import com.onyxshop.providers.requirements.items.PlaytimeRequirement;
import com.onyxshop.providers.requirements.items.TimeRequirement;
import com.onyxshop.providers.requirements.items.quest.QuestManager;
import com.onyxshop.providers.requirements.items.quest.QuestRequirement;
import com.onyxshop.providers.requirements.items.region.RegionManager;
import com.onyxshop.providers.requirements.items.region.RegionRequirement;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.Placeholder;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.entity.Player;

public class RequirementManager {
   private final OnyxShop plugin;
   private QuestManager questManager;
   private RegionManager regionManager;

   public RequirementManager(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public Requirements load(String text, String text2) {
      Requirements requirements = new Requirements();
      int value = 0;

      for (String shopId : ConfigManager.getShop(text).getStringList(this.getItemLoc(text, text2) + ".requirements")) {
         RequirementType requirementType;
         try {
            requirementType = RequirementType.valueOf(shopId.split("::")[0]);
         } catch (IllegalArgumentException exception) {
            SendMessage.warnMessage(Lang.INVALID_ITEM_REQUIREMENT_TYPE.get().replace("%type%", shopId.split("::")[0]));
            SendMessage.errorShops(text, text2);
            continue;
         }

         try {
            String text3 = shopId.split("::")[1];
            requirements.add(this.createRequirement(text + "." + text2, requirementType, text3, this.getLore(requirementType, shopId, text3), this.getCustomMSG(shopId), value));
         } catch (ItemRequirementLoadException exception) {
            SendMessage.warnMessage(Lang.INVALID_ITEM_REQUIREMENT.get().replace("%requirement%", shopId + ":\n " + exception.getMessage()));
            SendMessage.errorShops(text, text2);
         }

         value++;
      }

      return !requirements.isEmpty() && this.plugin.requirements ? requirements : null;
   }

   private ItemRequirement createRequirement(String itemId, RequirementType requirementType, String type, List<String> values, String text, int value) throws ItemRequirementLoadException {
      switch (requirementType) {
         case TIME:
            return new TimeRequirement(type, values);
         case LEVEL:
            return new LevelRequirement(type, values, text);
         case PLAYTIME:
            return new PlaytimeRequirement(this.plugin.version, type, values, text);
         case PLACEHOLDER:
         case PLACEHOLDER_DIFFERS:
         case PLACEHOLDER_EQUALS:
         case PLACEHOLDER_LARGER:
         case PLACEHOLDER_SMALLER:
            return PlaceholderRequirement.initPlaceholderComparison(requirementType, type, values, text);
         case QUEST:
            if (this.questManager == null) {
               this.questManager = new QuestManager(this.plugin);
            }

            QuestRequirement questRequirement = new QuestRequirement(this.questManager.getProvider(), type, values, text);
            return this.questManager.addRequirement(questRequirement, itemId);
         case REGION:
            if (this.regionManager == null) {
               this.regionManager = new RegionManager(this.plugin);
            }

            RegionRequirement regionRequirement = new RegionRequirement(this.regionManager.getProvider(), type, values, text);
            return this.regionManager.addRequirement(regionRequirement, itemId);
         case PERMISSION:
            return new PermissionRequirement(type, values, text);
         case ITEM:
            return new com.onyxshop.providers.requirements.items.ItemRequirement(type, values, text, itemId, value);
         default:
            return null;
      }
   }

   private List<String> getLore(RequirementType requirementType, String lore, String type) {
      return lore.split("::").length >= 3
         ? Arrays.stream(lore.split("::")[2].split("\n")).map(ChatUtil::formatColors).collect(Collectors.toList())
         : ConfigManager.getConfig().getStringList(requirementType.getLorePath(type));
   }

   private String getCustomMSG(String text) {
      return text.split("::").length >= 4 ? ChatUtil.formatColors(text.split("::")[3]) : null;
   }

   public static RequirementManager.RequirementLore getLore(List<String> lores, String text, String... values) {
      return (RequirementManager.RequirementLore)(OnyxShop.getInstance().paperMeta
         ? new RequirementManager.CompRequirementLore(lores, text, values)
         : new RequirementManager.BukkitRequirementLore(lores, text, values));
   }

   private static String translateLocal(String text, String... values) {
      if (values.length == 1) {
         if (text.contains("%requirement%")) {
            text = text.replace("%requirement%", values[0]);
         }
      } else if (values.length == 2) {
         if (text.contains("%minTime%")) {
            text = text.replace("%minTime%", values[0]);
         }

         if (text.contains("%maxTime%")) {
            text = text.replace("%maxTime%", values[1]);
         }
      }

      return text;
   }

   private static RequirementManager.LocalizedLore getLocalized(Map<Locale, RequirementManager.LocalizedLore> map, Player player) {
      RequirementManager.LocalizedLore localizedLore = (RequirementManager.LocalizedLore)map.get(UserManager.getUser(player).getLocale());
      return localizedLore != null ? localizedLore : (RequirementManager.LocalizedLore)map.get(Lang.getDefaultLocale());
   }

   private static List<String> translate(List<String> values, Locale locale) {
      if (values.isEmpty()) {
         return values;
      } else {
         ArrayList values2 = new ArrayList();

         for (String text : values) {
            if (text.contains("%translations-")) {
               try {
                  Lang lang = Lang.valueOf(text.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
                  text = text.replace("%translations-" + lang.getKey() + "%", lang.getRaw(locale).replace("\\n", "\n"));
               } catch (IllegalArgumentException exception) {
               }
            }

            for (String text2 : text.split("\n")) {
               values2.add(OnyxShop.getInstance().paperMeta ? ChatUtil.getAdventureUtils().formatLegacyToMini(text2) : ChatUtil.formatColors(text2));
            }
         }

         return values2;
      }
   }

   private static String translate(String text, Locale locale) {
      if (text == null) {
         return "";
      } else {
         if (text.contains("%translations-")) {
            try {
               Lang lang = Lang.valueOf(text.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               text = text.replace("%translations-" + lang.getKey() + "%", lang.getRaw(locale));
            } catch (IllegalArgumentException exception) {
            }
         }

         return OnyxShop.getInstance().paperMeta ? ChatUtil.getAdventureUtils().formatLegacyToMini(text) : ChatUtil.formatColors(text);
      }
   }

   private static boolean isEmpty(List<String> values) {
      return values.stream().allMatch(element -> element.trim().isEmpty());
   }

   private String getItemLoc(String itemId, String text) {
      return (this.plugin.getShopType(itemId) == ShopType.CATEGORY ? "pages." : "") + text;
   }

   private static final class BukkitRequirementLore implements RequirementManager.RequirementLore {
      private final RequirementManager.LocalizedLore lore;
      private final Map<Locale, RequirementManager.LocalizedLore> localizedLore;
      private boolean replaced = false;

      public BukkitRequirementLore(List<String> lores, String text, String... values) {
         String[] values2 = lores != null
            ? (
               !RequirementManager.isEmpty(lores)
                  ? RequirementManager.translate(lores, Lang.getDefaultLocale())
                     .stream()
                     .map(element -> RequirementManager.translateLocal(element, values))
                     .collect(Collectors.toList())
                     .toArray(new String[lores.size()])
                  : new String[0]
            )
            : RequirementManager.translate(RequirementManager.translateLocal(text, values), Lang.getDefaultLocale()).split("\n");
         String[] shopIds = Arrays.stream(values2).map(element -> OnyxShop.getInstance().versionHandler.toNBT(element)).toArray(String[]::new);
         this.lore = !OnyxShop.getInstance().perPlayerLanguages ? new RequirementManager.LocalizedLore(values2, shopIds) : null;
         this.localizedLore = OnyxShop.getInstance().perPlayerLanguages ? this.getLocalizedLore(lores, text, values) : null;
      }

      private Map<Locale, RequirementManager.LocalizedLore> getLocalizedLore(List<String> lores, String text, String... values) {
         HashMap map = new HashMap();

         for (Locale locale : Lang.getLocales()) {
            String[] values2 = lores != null
               ? (
                  !RequirementManager.isEmpty(lores)
                     ? RequirementManager.translate(lores, locale).stream().map(element -> RequirementManager.translateLocal(element, values)).toArray(String[]::new)
                     : new String[0]
               )
               : RequirementManager.translate(RequirementManager.translateLocal(text, values), locale).split("\n");
            String[] shopIds = Arrays.stream(values2).map(element -> OnyxShop.getInstance().versionHandler.toNBT(element)).toArray(String[]::new);
            map.put(locale, new RequirementManager.LocalizedLore(values2, shopIds));
         }

         return map;
      }

      @Override
      public boolean isReplaced() {
         return this.replaced;
      }

      @Override
      public void replace(String text, String text2) {
         if (this.lore != null) {
            for (int index = 0; index < this.lore.lore.length; index++) {
               if (this.lore.lore[index].contains(text)) {
                  this.lore.lore[index] = this.lore.lore[index].replace(text, text2);
                  this.lore.fastLore[index] = this.lore.fastLore[index].replace(text, text2);
                  this.replaced = true;
               }
            }
         } else {
            for (RequirementManager.LocalizedLore localizedLore : this.localizedLore.values()) {
               for (int index = 0; index < localizedLore.lore.length; index++) {
                  if (localizedLore.lore[index].contains(text)) {
                     localizedLore.lore[index] = localizedLore.lore[index].replace(text, text2);
                     localizedLore.fastLore[index] = localizedLore.fastLore[index].replace(text, text2);
                     this.replaced = true;
                  }
               }
            }
         }
      }

      @Override
      public String[] get(Player player, boolean flag, boolean flag2, String... playerNames) {
         RequirementManager.LocalizedLore localizedLore = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, player) : this.lore;
         String[] text = (String[])(flag && !flag2 ? localizedLore.fastLore : localizedLore.lore).clone();

         for (Entry<Integer, Placeholder> entry : localizedLore.placeholders.entrySet()) {
            text[entry.getKey()] = ((Placeholder)entry.getValue()).translate(text[entry.getKey()], player, playerNames);
         }

         return text;
      }

      @Override
      public String[] get(Player player, boolean flag, boolean flag2) {
         RequirementManager.LocalizedLore localizedLore = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, player) : this.lore;
         if (localizedLore.placeholders.isEmpty()) {
            return flag && !flag2 ? localizedLore.fastLore : localizedLore.lore;
         } else {
            String[] text = (String[])(flag && !flag2 ? localizedLore.fastLore : localizedLore.lore).clone();

            for (Entry<Integer, Placeholder> entry : localizedLore.placeholders.entrySet()) {
               text[entry.getKey()] = ((Placeholder)entry.getValue()).translate(player, new String[0]);
            }

            return text;
         }
      }
   }

   private static final class CompRequirementLore implements RequirementManager.RequirementLore {
      private final RequirementManager.LocalizedLore lore;
      private final Map<Locale, RequirementManager.LocalizedLore> localizedLore;
      private boolean replaced = false;

      public CompRequirementLore(List<String> lores, String text, String... values) {
         String[] values2 = lores != null
            ? (
               !RequirementManager.isEmpty(lores)
                  ? RequirementManager.translate(lores, Lang.getDefaultLocale())
                     .stream()
                     .map(element -> RequirementManager.translateLocal(element, values))
                     .collect(Collectors.toList())
                     .toArray(new String[lores.size()])
                  : new String[0]
            )
            : RequirementManager.translate(RequirementManager.translateLocal(text, values), Lang.getDefaultLocale()).split("\n");
         String[] values3 = Arrays.stream(values2).map(ChatUtil::getGsonComponent).toArray(String[]::new);
         this.lore = !OnyxShop.getInstance().perPlayerLanguages ? new RequirementManager.LocalizedLore(values2, values3) : null;
         this.localizedLore = OnyxShop.getInstance().perPlayerLanguages ? this.getLocalizedLore(lores, text, values) : null;
      }

      private Map<Locale, RequirementManager.LocalizedLore> getLocalizedLore(List<String> lores, String text, String... values) {
         HashMap map = new HashMap();

         for (Locale locale : Lang.getLocales()) {
            String[] values2 = lores != null
               ? (
                  !RequirementManager.isEmpty(lores)
                     ? RequirementManager.translate(lores, locale).stream().map(element -> RequirementManager.translateLocal(element, values)).toArray(String[]::new)
                     : new String[0]
               )
               : RequirementManager.translate(RequirementManager.translateLocal(text, values), locale).split("\n");
            String[] values3 = Arrays.stream(values2).map(ChatUtil::getGsonComponent).toArray(String[]::new);
            map.put(locale, new RequirementManager.LocalizedLore(values2, values3));
         }

         return map;
      }

      @Override
      public boolean isReplaced() {
         return this.replaced;
      }

      @Override
      public void replace(String text, String text2) {
         if (this.lore != null) {
            for (int index = 0; index < this.lore.lore.length; index++) {
               if (this.lore.lore[index].contains(text)) {
                  this.lore.lore[index] = this.lore.lore[index].replace(text, text2);
                  this.lore.fastLore[index] = this.lore.fastLore[index].replace(text, text2);
                  this.replaced = true;
               }
            }
         } else {
            for (RequirementManager.LocalizedLore localizedLore : this.localizedLore.values()) {
               for (int index = 0; index < localizedLore.lore.length; index++) {
                  if (localizedLore.lore[index].contains(text)) {
                     localizedLore.lore[index] = localizedLore.lore[index].replace(text, text2);
                     localizedLore.fastLore[index] = localizedLore.fastLore[index].replace(text, text2);
                     this.replaced = true;
                  }
               }
            }
         }
      }

      @Override
      public String[] get(Player player, boolean flag, boolean flag2, String... playerNames) {
         RequirementManager.LocalizedLore localizedLore = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, player) : this.lore;
         String[] text = (String[])(flag && !flag2 ? localizedLore.fastLore : localizedLore.lore).clone();

         for (Entry<Integer, Placeholder> entry : localizedLore.placeholders.entrySet()) {
            text[entry.getKey()] = ((Placeholder)entry.getValue()).translate(text[entry.getKey()], player, playerNames);
         }

         return text;
      }

      @Override
      public String[] get(Player player, boolean flag, boolean flag2) {
         RequirementManager.LocalizedLore localizedLore = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, player) : this.lore;
         return flag ? localizedLore.fastLore : localizedLore.lore;
      }
   }

   private static final class LocalizedLore {
      private final Map<Integer, Placeholder> placeholders = new HashMap<>();
      private String[] lore;
      private String[] fastLore;

      private LocalizedLore(String[] lores, String[] values) {
         this.lore = lores;
         this.fastLore = values;
         int value = 0;

         for (String text : lores) {
            if (Placeholder.isPlaceholder(text, "%requirement%", "%progress%")) {
               this.placeholders.put(value, new Placeholder(text, "%requirement%", "%progress%"));
            }

            value++;
         }
      }
   }

   public interface RequirementLore {
      boolean isReplaced();

      void replace(String text, String text2);

      String[] get(Player player, boolean flag, boolean flag2, String... playerNames);

      String[] get(Player player, boolean flag, boolean flag2);
   }
}
