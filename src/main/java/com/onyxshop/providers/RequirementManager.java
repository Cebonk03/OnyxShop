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

   public RequirementManager(OnyxShop var1) {
      this.plugin = var1;
   }

   public Requirements load(String var1, String var2) {
      Requirements var3 = new Requirements();
      int var4 = 0;

      for (String var6 : ConfigManager.getShop(var1).getStringList(this.getItemLoc(var1, var2) + ".requirements")) {
         RequirementType var7;
         try {
            var7 = RequirementType.valueOf(var6.split("::")[0]);
         } catch (IllegalArgumentException var10) {
            SendMessage.warnMessage(Lang.INVALID_ITEM_REQUIREMENT_TYPE.get().replace("%type%", var6.split("::")[0]));
            SendMessage.errorShops(var1, var2);
            continue;
         }

         try {
            String var8 = var6.split("::")[1];
            var3.add(this.createRequirement(var1 + "." + var2, var7, var8, this.getLore(var7, var6, var8), this.getCustomMSG(var6), var4));
         } catch (ItemRequirementLoadException var9) {
            SendMessage.warnMessage(Lang.INVALID_ITEM_REQUIREMENT.get().replace("%requirement%", var6 + ":\n " + var9.getMessage()));
            SendMessage.errorShops(var1, var2);
         }

         var4++;
      }

      return !var3.isEmpty() && this.plugin.requirements ? var3 : null;
   }

   private ItemRequirement createRequirement(String var1, RequirementType var2, String var3, List<String> var4, String var5, int var6) throws ItemRequirementLoadException {
      switch (var2) {
         case TIME:
            return new TimeRequirement(var3, var4);
         case LEVEL:
            return new LevelRequirement(var3, var4, var5);
         case PLAYTIME:
            return new PlaytimeRequirement(this.plugin.version, var3, var4, var5);
         case PLACEHOLDER:
         case PLACEHOLDER_DIFFERS:
         case PLACEHOLDER_EQUALS:
         case PLACEHOLDER_LARGER:
         case PLACEHOLDER_SMALLER:
            return PlaceholderRequirement.initPlaceholderComparison(var2, var3, var4, var5);
         case QUEST:
            if (this.questManager == null) {
               this.questManager = new QuestManager(this.plugin);
            }

            QuestRequirement var7 = new QuestRequirement(this.questManager.getProvider(), var3, var4, var5);
            return this.questManager.addRequirement(var7, var1);
         case REGION:
            if (this.regionManager == null) {
               this.regionManager = new RegionManager(this.plugin);
            }

            RegionRequirement var8 = new RegionRequirement(this.regionManager.getProvider(), var3, var4, var5);
            return this.regionManager.addRequirement(var8, var1);
         case PERMISSION:
            return new PermissionRequirement(var3, var4, var5);
         case ITEM:
            return new com.onyxshop.providers.requirements.items.ItemRequirement(var3, var4, var5, var1, var6);
         default:
            return null;
      }
   }

   private List<String> getLore(RequirementType var1, String var2, String var3) {
      return var2.split("::").length >= 3
         ? Arrays.stream(var2.split("::")[2].split("\n")).map(ChatUtil::formatColors).collect(Collectors.toList())
         : ConfigManager.getConfig().getStringList(var1.getLorePath(var3));
   }

   private String getCustomMSG(String var1) {
      return var1.split("::").length >= 4 ? ChatUtil.formatColors(var1.split("::")[3]) : null;
   }

   public static RequirementManager.RequirementLore getLore(List<String> var0, String var1, String... var2) {
      return (RequirementManager.RequirementLore)(OnyxShop.getInstance().paperMeta
         ? new RequirementManager.CompRequirementLore(var0, var1, var2)
         : new RequirementManager.BukkitRequirementLore(var0, var1, var2));
   }

   private static String translateLocal(String var0, String... var1) {
      if (var1.length == 1) {
         if (var0.contains("%requirement%")) {
            var0 = var0.replace("%requirement%", var1[0]);
         }
      } else if (var1.length == 2) {
         if (var0.contains("%minTime%")) {
            var0 = var0.replace("%minTime%", var1[0]);
         }

         if (var0.contains("%maxTime%")) {
            var0 = var0.replace("%maxTime%", var1[1]);
         }
      }

      return var0;
   }

   private static RequirementManager.LocalizedLore getLocalized(Map<Locale, RequirementManager.LocalizedLore> var0, Player var1) {
      RequirementManager.LocalizedLore var2 = (RequirementManager.LocalizedLore)var0.get(UserManager.getUser(var1).getLocale());
      return var2 != null ? var2 : (RequirementManager.LocalizedLore)var0.get(Lang.getDefaultLocale());
   }

   private static List<String> translate(List<String> var0, Locale var1) {
      if (var0.isEmpty()) {
         return var0;
      } else {
         ArrayList var2 = new ArrayList();

         for (String var4 : var0) {
            if (var4.contains("%translations-")) {
               try {
                  Lang var5 = Lang.valueOf(var4.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
                  var4 = var4.replace("%translations-" + var5.getKey() + "%", var5.getRaw(var1).replace("\\n", "\n"));
               } catch (IllegalArgumentException var9) {
               }
            }

            for (String var8 : var4.split("\n")) {
               var2.add(OnyxShop.getInstance().paperMeta ? ChatUtil.getAdventureUtils().formatLegacyToMini(var8) : ChatUtil.formatColors(var8));
            }
         }

         return var2;
      }
   }

   private static String translate(String var0, Locale var1) {
      if (var0 == null) {
         return "";
      } else {
         if (var0.contains("%translations-")) {
            try {
               Lang var2 = Lang.valueOf(var0.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               var0 = var0.replace("%translations-" + var2.getKey() + "%", var2.getRaw(var1));
            } catch (IllegalArgumentException var3) {
            }
         }

         return OnyxShop.getInstance().paperMeta ? ChatUtil.getAdventureUtils().formatLegacyToMini(var0) : ChatUtil.formatColors(var0);
      }
   }

   private static boolean isEmpty(List<String> var0) {
      return var0.stream().allMatch(var0x -> var0x.trim().isEmpty());
   }

   private String getItemLoc(String var1, String var2) {
      return (this.plugin.getShopType(var1) == ShopType.CATEGORY ? "pages." : "") + var2;
   }

   private static final class BukkitRequirementLore implements RequirementManager.RequirementLore {
      private final RequirementManager.LocalizedLore lore;
      private final Map<Locale, RequirementManager.LocalizedLore> localizedLore;
      private boolean replaced = false;

      public BukkitRequirementLore(List<String> var1, String var2, String... var3) {
         String[] var4 = var1 != null
            ? (
               !RequirementManager.isEmpty(var1)
                  ? RequirementManager.translate(var1, Lang.getDefaultLocale())
                     .stream()
                     .map(var1x -> RequirementManager.translateLocal(var1x, var3))
                     .collect(Collectors.toList())
                     .toArray(new String[var1.size()])
                  : new String[0]
            )
            : RequirementManager.translate(RequirementManager.translateLocal(var2, var3), Lang.getDefaultLocale()).split("\n");
         String[] var5 = Arrays.stream(var4).map(var0 -> OnyxShop.getInstance().versionHandler.toNBT(var0)).toArray(String[]::new);
         this.lore = !OnyxShop.getInstance().perPlayerLanguages ? new RequirementManager.LocalizedLore(var4, var5) : null;
         this.localizedLore = OnyxShop.getInstance().perPlayerLanguages ? this.getLocalizedLore(var1, var2, var3) : null;
      }

      private Map<Locale, RequirementManager.LocalizedLore> getLocalizedLore(List<String> var1, String var2, String... var3) {
         HashMap var4 = new HashMap();

         for (Locale var6 : Lang.getLocales()) {
            String[] var7 = var1 != null
               ? (
                  !RequirementManager.isEmpty(var1)
                     ? RequirementManager.translate(var1, var6).stream().map(var1x -> RequirementManager.translateLocal(var1x, var3)).toArray(String[]::new)
                     : new String[0]
               )
               : RequirementManager.translate(RequirementManager.translateLocal(var2, var3), var6).split("\n");
            String[] var8 = Arrays.stream(var7).map(var0 -> OnyxShop.getInstance().versionHandler.toNBT(var0)).toArray(String[]::new);
            var4.put(var6, new RequirementManager.LocalizedLore(var7, var8));
         }

         return var4;
      }

      @Override
      public boolean isReplaced() {
         return this.replaced;
      }

      @Override
      public void replace(String var1, String var2) {
         if (this.lore != null) {
            for (int var3 = 0; var3 < this.lore.lore.length; var3++) {
               if (this.lore.lore[var3].contains(var1)) {
                  this.lore.lore[var3] = this.lore.lore[var3].replace(var1, var2);
                  this.lore.fastLore[var3] = this.lore.fastLore[var3].replace(var1, var2);
                  this.replaced = true;
               }
            }
         } else {
            for (RequirementManager.LocalizedLore var4 : this.localizedLore.values()) {
               for (int var5 = 0; var5 < var4.lore.length; var5++) {
                  if (var4.lore[var5].contains(var1)) {
                     var4.lore[var5] = var4.lore[var5].replace(var1, var2);
                     var4.fastLore[var5] = var4.fastLore[var5].replace(var1, var2);
                     this.replaced = true;
                  }
               }
            }
         }
      }

      @Override
      public String[] get(Player var1, boolean var2, boolean var3, String... var4) {
         RequirementManager.LocalizedLore var5 = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, var1) : this.lore;
         String[] var6 = (String[])(var2 && !var3 ? var5.fastLore : var5.lore).clone();

         for (Entry<Integer, Placeholder> var8 : var5.placeholders.entrySet()) {
            var6[var8.getKey()] = ((Placeholder)var8.getValue()).translate(var6[var8.getKey()], var1, var4);
         }

         return var6;
      }

      @Override
      public String[] get(Player var1, boolean var2, boolean var3) {
         RequirementManager.LocalizedLore var4 = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, var1) : this.lore;
         if (var4.placeholders.isEmpty()) {
            return var2 && !var3 ? var4.fastLore : var4.lore;
         } else {
            String[] var5 = (String[])(var2 && !var3 ? var4.fastLore : var4.lore).clone();

            for (Entry<Integer, Placeholder> var7 : var4.placeholders.entrySet()) {
               var5[var7.getKey()] = ((Placeholder)var7.getValue()).translate(var1, new String[0]);
            }

            return var5;
         }
      }
   }

   private static final class CompRequirementLore implements RequirementManager.RequirementLore {
      private final RequirementManager.LocalizedLore lore;
      private final Map<Locale, RequirementManager.LocalizedLore> localizedLore;
      private boolean replaced = false;

      public CompRequirementLore(List<String> var1, String var2, String... var3) {
         String[] var4 = var1 != null
            ? (
               !RequirementManager.isEmpty(var1)
                  ? RequirementManager.translate(var1, Lang.getDefaultLocale())
                     .stream()
                     .map(var1x -> RequirementManager.translateLocal(var1x, var3))
                     .collect(Collectors.toList())
                     .toArray(new String[var1.size()])
                  : new String[0]
            )
            : RequirementManager.translate(RequirementManager.translateLocal(var2, var3), Lang.getDefaultLocale()).split("\n");
         String[] var5 = Arrays.stream(var4).map(ChatUtil::getGsonComponent).toArray(String[]::new);
         this.lore = !OnyxShop.getInstance().perPlayerLanguages ? new RequirementManager.LocalizedLore(var4, var5) : null;
         this.localizedLore = OnyxShop.getInstance().perPlayerLanguages ? this.getLocalizedLore(var1, var2, var3) : null;
      }

      private Map<Locale, RequirementManager.LocalizedLore> getLocalizedLore(List<String> var1, String var2, String... var3) {
         HashMap var4 = new HashMap();

         for (Locale var6 : Lang.getLocales()) {
            String[] var7 = var1 != null
               ? (
                  !RequirementManager.isEmpty(var1)
                     ? RequirementManager.translate(var1, var6).stream().map(var1x -> RequirementManager.translateLocal(var1x, var3)).toArray(String[]::new)
                     : new String[0]
               )
               : RequirementManager.translate(RequirementManager.translateLocal(var2, var3), var6).split("\n");
            String[] var8 = Arrays.stream(var7).map(ChatUtil::getGsonComponent).toArray(String[]::new);
            var4.put(var6, new RequirementManager.LocalizedLore(var7, var8));
         }

         return var4;
      }

      @Override
      public boolean isReplaced() {
         return this.replaced;
      }

      @Override
      public void replace(String var1, String var2) {
         if (this.lore != null) {
            for (int var3 = 0; var3 < this.lore.lore.length; var3++) {
               if (this.lore.lore[var3].contains(var1)) {
                  this.lore.lore[var3] = this.lore.lore[var3].replace(var1, var2);
                  this.lore.fastLore[var3] = this.lore.fastLore[var3].replace(var1, var2);
                  this.replaced = true;
               }
            }
         } else {
            for (RequirementManager.LocalizedLore var4 : this.localizedLore.values()) {
               for (int var5 = 0; var5 < var4.lore.length; var5++) {
                  if (var4.lore[var5].contains(var1)) {
                     var4.lore[var5] = var4.lore[var5].replace(var1, var2);
                     var4.fastLore[var5] = var4.fastLore[var5].replace(var1, var2);
                     this.replaced = true;
                  }
               }
            }
         }
      }

      @Override
      public String[] get(Player var1, boolean var2, boolean var3, String... var4) {
         RequirementManager.LocalizedLore var5 = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, var1) : this.lore;
         String[] var6 = (String[])(var2 && !var3 ? var5.fastLore : var5.lore).clone();

         for (Entry<Integer, Placeholder> var8 : var5.placeholders.entrySet()) {
            var6[var8.getKey()] = ((Placeholder)var8.getValue()).translate(var6[var8.getKey()], var1, var4);
         }

         return var6;
      }

      @Override
      public String[] get(Player var1, boolean var2, boolean var3) {
         RequirementManager.LocalizedLore var4 = this.localizedLore != null ? RequirementManager.getLocalized(this.localizedLore, var1) : this.lore;
         return var2 ? var4.fastLore : var4.lore;
      }
   }

   private static final class LocalizedLore {
      private final Map<Integer, Placeholder> placeholders = new HashMap<>();
      private String[] lore;
      private String[] fastLore;

      private LocalizedLore(String[] var1, String[] var2) {
         this.lore = var1;
         this.fastLore = var2;
         int var3 = 0;

         for (String var7 : var1) {
            if (Placeholder.isPlaceholder(var7, "%requirement%", "%progress%")) {
               this.placeholders.put(var3, new Placeholder(var7, "%requirement%", "%progress%"));
            }

            var3++;
         }
      }
   }

   public interface RequirementLore {
      boolean isReplaced();

      void replace(String var1, String var2);

      String[] get(Player var1, boolean var2, boolean var3, String... var4);

      String[] get(Player var1, boolean var2, boolean var3);
   }
}
