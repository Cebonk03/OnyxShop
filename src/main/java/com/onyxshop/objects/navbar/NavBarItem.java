package com.onyxshop.objects.navbar;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.Placeholder;
import com.onyxshop.util.Replacement;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.XMaterial;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NavBarItem implements NavItem {
   private static final String[] localPlaceholders = new String[]{
      "%player_name%", "%player_displayname%", "%player_level%", "%player_balance%", "%page%", "%pages%", "%search-method%"
   };
   private LocalizedItemStack navBarItem;
   private NavBar.ItemAction action;
   private boolean staticItem = true;
   private boolean onlyGeyser;
   private boolean onlyMultiplePrices;
   private boolean disabled;
   private boolean closeMenu;
   private NavBarItem.Paginated paginatedItem;
   private List<String> commands;
   private Map<Locale, Map<Integer, Placeholder>> placeholders;
   private String bedrockTexture;

   @Override
   public boolean isDisabled() {
      return this.disabled;
   }

   @Override
   public boolean isCloseMenu() {
      return this.closeMenu;
   }

   @Override
   public boolean isOnlyGeyser() {
      return this.onlyGeyser;
   }

   @Override
   public boolean isStatic(int var1, int var2) {
      return this.staticItem && (this.paginatedItem == null || var2 == 1 || var1 != 1 && var1 != 2 && var1 != var2 - 1 && var1 != var2);
   }

   @Override
   public NavBar.ItemAction getAction() {
      return this.action;
   }

   @Override
   public NavBar.ItemAction getAction(int var1, int var2) {
      return this.paginatedItem == null
            || var2 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || var1 != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || var1 != var2)
         ? this.action
         : this.paginatedItem.getA();
   }

   @Override
   public List<String> getCommands() {
      return this.commands;
   }

   @Override
   public List<String> getCommands(int var1) {
      return var1 == 1 && this.paginatedItem != null && !this.paginatedItem.isDisabled() ? this.paginatedItem.getCommands() : this.commands;
   }

   @Override
   public String getBedrockTexture() {
      return this.bedrockTexture;
   }

   @Override
   public String getBedrockTexture(int var1, int var2) {
      return this.paginatedItem == null
            || var2 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || var1 != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || var1 != var2)
         ? this.bedrockTexture
         : this.paginatedItem.getBedrockTexture();
   }

   public NavBarItem(ItemStack var1, NavBar.ItemAction var2) {
      this.navBarItem = new LocalizedItemStack(var1);
      this.action = var2;
      this.staticItem = false;
   }

   public NavBarItem(ConfigurationSection var1, ItemStack var2) {
      LocalizedItemStack var3 = OnyxShop.getInstance().createItem.createFillItem(var1, null);
      if (var3 != null && !var3.getItem().getType().equals(Material.AIR)) {
         this.placeholders = this.getPlaceholders(var3);
         if (this.placeholders.values().stream().anyMatch(var0 -> !var0.isEmpty())) {
            this.staticItem = false;
         }

         if (var1.getBoolean("paginated.enabled")) {
            ConfigurationSection var4 = var1.getConfigurationSection("paginated.orElse");
            if (var4 != null && !var4.getKeys(false).isEmpty()) {
               this.paginatedItem = new NavBarItem.Paginated(var4);
            } else {
               this.paginatedItem = new NavBarItem.Paginated(var2);
            }

            this.staticItem = false;
         }

         this.action = this.getAction(var1, var2);
         this.navBarItem = var3;
         this.bedrockTexture = var1.getString("bedrock-texture");
      } else {
         this.disabled = true;
      }
   }

   private NavBar.ItemAction getAction(ConfigurationSection var1, ItemStack var2) {
      if (!var1.contains("action")) {
         return NavBar.ItemAction.NONE;
      } else {
         NavBar.ItemAction var3 = NavBar.ItemAction.getFromString(var1.getString("action"));
         switch (var3) {
            case COMMAND:
               this.commands = var1.getStringList("commands");
               this.closeMenu = var1.getBoolean("close-menu");
               break;
            case CYCLE_CURRENCY:
               this.onlyMultiplePrices = var1.getBoolean("when-multiple-prices", true);
               break;
            case TOGGLE_MODE:
               this.onlyGeyser = var1.getBoolean("onlyEnableForGeyserPlayers", true);
            case PAGE_BACK:
            case PAGE_NEXT:
               if (this.paginatedItem == null && var1.getBoolean("paginated.enabled", true)) {
                  this.paginatedItem = new NavBarItem.Paginated(var2);
                  this.staticItem = false;
               }
         }

         return var3;
      }
   }

   @Override
   public ItemStack getItem(Player var1) {
      if (!this.staticItem && this.navBarItem != null) {
         ItemStack var2 = this.navBarItem.getItem(var1);
         if (!var2.hasItemMeta()) {
            return var2;
         } else {
            ItemMeta var3 = var2.getItemMeta();
            Map var4 = getPlaceholdersFor(this.placeholders, var1);
            if (!var4.isEmpty()) {
               this.translateLore(var1, null, var3, var4);
            }

            var2.setItemMeta(var3);
            return var2;
         }
      } else {
         return this.navBarItem == null ? null : this.navBarItem.getItem(var1);
      }
   }

   @Override
   public ItemStack getItem(Player var1, EcoType var2, ShopSearch.QueryMethod var3, int var4, int var5) {
      if (!this.staticItem && this.navBarItem != null) {
         if (this.paginatedItem == null
            || var5 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || var4 != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || var4 != var5)) {
            ItemStack var6 = this.navBarItem.getItem(var1);
            if (!var6.hasItemMeta()) {
               return var6;
            } else {
               ItemMeta var7 = var6.getItemMeta();
               Map var8 = getPlaceholdersFor(this.placeholders, var1);
               if (!var8.isEmpty()) {
                  this.translateLore(var1, var2, var7, var8, var3, var4, var5);
               }

               var6.setItemMeta(var7);
               return var6;
            }
         } else {
            return this.paginatedItem.getItem(var1, var2, var3, var4, var5);
         }
      } else {
         return this.navBarItem == null ? null : this.navBarItem.getItem(var1);
      }
   }

   @Override
   public ItemStack getItem(Player var1, EcoType var2, int var3, int var4) {
      if (!this.staticItem && this.navBarItem != null) {
         if (this.paginatedItem == null
            || var4 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || var3 != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || var3 != var4)) {
            ItemStack var5 = this.navBarItem.getItem(var1);
            if (!var5.hasItemMeta()) {
               return var5;
            } else {
               ItemMeta var6 = var5.getItemMeta();
               Map var7 = getPlaceholdersFor(this.placeholders, var1);
               if (!var7.isEmpty()) {
                  this.translateLore(var1, var2, var6, var7, var3, var4);
               }

               var5.setItemMeta(var6);
               return var5;
            }
         } else {
            return this.paginatedItem.getItem(var1, var2, var3, var4);
         }
      } else {
         return this.navBarItem == null ? null : this.navBarItem.getItem(var1);
      }
   }

   @Override
   public ItemStack getItem(Player var1, Transaction.Mode var2, ShopItem var3, EcoType var4) {
      if ((!this.onlyGeyser || UserManager.getUser(var1).isBedrock())
         && (!this.onlyMultiplePrices || (var2 != Transaction.Mode.BUY || var3.isABuyPricing()) && (var2 != Transaction.Mode.SELL || var3.isASellPricing()))) {
         if (!this.staticItem && this.navBarItem != null) {
            ItemStack var5 = this.navBarItem.getItem(var1);
            if (!var5.hasItemMeta()) {
               return var5;
            } else {
               ItemMeta var6 = var5.getItemMeta();
               Map var7 = getPlaceholdersFor(this.placeholders, var1);
               if (!var7.isEmpty()) {
                  this.translateLore(var1, var4, var6, var7);
               }

               var5.setItemMeta(var6);
               return var5;
            }
         } else {
            return this.navBarItem == null ? null : this.navBarItem.getItem(var1);
         }
      } else {
         return this.paginatedItem != null ? this.paginatedItem.getItem(var1, var4, 0, 0) : new ItemStack(Material.AIR);
      }
   }

   private Map<Locale, Map<Integer, Placeholder>> getPlaceholders(LocalizedItemStack var1) {
      HashMap var2 = new HashMap();

      for (Entry var4 : var1.getVariants().entrySet()) {
         var2.put((Locale)var4.getKey(), this.getPlaceholders((ItemStack)var4.getValue()));
      }

      return var2;
   }

   private Map<Integer, Placeholder> getPlaceholders(ItemStack var1) {
      if (var1.hasItemMeta() && (var1.getItemMeta().hasDisplayName() || var1.getItemMeta().hasLore())) {
         ItemMeta var2 = var1.getItemMeta();
         HashMap var3 = new HashMap();
         if (var2.hasDisplayName()) {
            String var4 = OnyxShop.getInstance().getMetaUtils().getRawDisplayName(var2);
            if (Placeholder.isPlaceholder(var4, localPlaceholders)) {
               var3.put(-1, new Placeholder(var4, localPlaceholders));
            }
         }

         if (var2.hasLore()) {
            int var7 = 0;

            for (String var6 : OnyxShop.getInstance().getMetaUtils().getRawLore(var2)) {
               if (Placeholder.isPlaceholder(var6, localPlaceholders)) {
                  var3.put(var7, new Placeholder(var6, localPlaceholders));
               }

               var7++;
            }
         }

         return var3;
      } else {
         return Collections.EMPTY_MAP;
      }
   }

   private static Map<Integer, Placeholder> getPlaceholdersFor(Map<Locale, Map<Integer, Placeholder>> var0, Player var1) {
      if (var0 == null) {
         return Collections.EMPTY_MAP;
      } else {
         Map var2 = (Map)var0.get(UserManager.getUser(var1).getLocale());
         return var2 != null ? var2 : var0.getOrDefault(Lang.getDefaultLocale(), Collections.EMPTY_MAP);
      }
   }

   private void translateLore(Player var1, EcoType var2, ItemMeta var3, Map<Integer, Placeholder> var4, ShopSearch.QueryMethod var5, int var6, int var7) {
      if (var4.containsKey(-1)) {
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(var3, ((Placeholder)var4.get(-1)).translate(var1, this.getPlaceholders(var1, var2, var6, var7, var5)));
      }

      if (var3.hasLore()) {
         List var8 = OnyxShop.getInstance().getMetaUtils().getRawLore(var3);

         for (Integer var10 : var4.keySet()) {
            if (var10 != -1) {
               var8.set(var10, ((Placeholder)var4.get(var10)).translate(var1, this.getPlaceholders(var1, var2, var6, var7, var5)));
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(var3, var8);
      }
   }

   private void translateLore(Player var1, EcoType var2, ItemMeta var3, Map<Integer, Placeholder> var4, int var5, int var6) {
      if (var4.containsKey(-1)) {
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(var3, ((Placeholder)var4.get(-1)).translate(var1, this.getPlaceholders(var1, var2, var5, var6)));
      }

      if (var3.hasLore()) {
         List var7 = OnyxShop.getInstance().getMetaUtils().getRawLore(var3);

         for (Integer var9 : var4.keySet()) {
            if (var9 != -1) {
               var7.set(var9, ((Placeholder)var4.get(var9)).translate(var1, this.getPlaceholders(var1, var2, var5, var6)));
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(var3, var7);
      }
   }

   private void translateLore(Player var1, EcoType var2, ItemMeta var3, Map<Integer, Placeholder> var4) {
      if (var4.containsKey(-1)) {
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(var3, ((Placeholder)var4.get(-1)).translate(var1, this.getPlaceholders(var1, var2, 0, 0)));
      }

      if (var3.hasLore()) {
         List var5 = OnyxShop.getInstance().getMetaUtils().getRawLore(var3);

         for (Integer var7 : var4.keySet()) {
            if (var7 != -1) {
               var5.set(var7, ((Placeholder)var4.get(var7)).translate(var1, this.getPlaceholders(var1, var2, 0, 0)));
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(var3, var5);
      }
   }

   private Replacement[] getPlaceholders(Player var1, EcoType var2, int var3, int var4) {
      return new Replacement[]{
         new Replacement("%player_name%", var1::getName),
         new Replacement("%player_displayname%", () -> OnyxShop.getInstance().getDisplayName(var1)),
         new Replacement("%player_level%", () -> String.valueOf(var1.getLevel())),
         new Replacement(
            "%player_balance%",
            () -> OnyxShop.getInstance().formatPrice(var2, OnyxShop.getInstance().getEcoHandler().getEcon(var2).getBalance(var1))
         ),
         new Replacement("%page%", () -> String.valueOf(var3)),
         new Replacement("%pages%", () -> String.valueOf(var4))
      };
   }

   private Replacement[] getPlaceholders(Player var1, EcoType var2, int var3, int var4, ShopSearch.QueryMethod var5) {
      return new Replacement[]{
         new Replacement("%player_name%", var1::getName),
         new Replacement("%player_displayname%", () -> OnyxShop.getInstance().getDisplayName(var1)),
         new Replacement("%player_level%", () -> String.valueOf(var1.getLevel())),
         new Replacement(
            "%player_balance%",
            () -> OnyxShop.getInstance().formatPrice(var2, OnyxShop.getInstance().getEcoHandler().getEcon(var2).getBalance(var1))
         ),
         new Replacement("%page%", () -> String.valueOf(var3)),
         new Replacement("%pages%", () -> String.valueOf(var4)),
         new Replacement("%search-method%", var5::getName)
      };
   }

   private Translatable replaceTranslations(String var1) {
      return SendMessage.fromConfig(
         var1.replace("%translations-shopsearch-change-search-method%", Lang.SHOPSEARCH_CHANGE_SEARCH_METHOD.get().toString())
            .replace("%translations-previous-page%", Lang.PREVIOUS_PAGE.get().toString())
            .replace(
               "%translations-current-page%",
               Lang.CURRENT_PAGE.get().getLegacy().contains("%page%")
                  ? Lang.CURRENT_PAGE.get().toString()
                  : Lang.CURRENT_PAGE.get().builder().append(" %page%/%pages%").build().toString()
            )
            .replace("%translations-next-page%", Lang.NEXT_PAGE.get().toString())
            .replace("%translations-money%", Lang.MONEY.get().toString())
            .replace("%translations-cancel%", Lang.CANCEL.get().toString())
            .replace("%translations-back%", Lang.BACK.get().toString())
      );
   }

   public class Paginated {
      private LocalizedItemStack orElse;
      private NavBar.ItemAction action;
      private boolean skullOwner = false;
      private boolean staticItem = true;
      private boolean disabled = false;
      private List<String> commands;
      private Map<Locale, Map<Integer, Placeholder>> placeholders;
      private String bedrockTexture;

      public Paginated(ItemStack nullx) {
         if (nullx == null) {
            this.disabled = true;
            this.orElse = new LocalizedItemStack(new ItemStack(Material.AIR));
         } else {
            this.orElse = new LocalizedItemStack(nullx);
         }

         this.action = NavBar.ItemAction.NONE;
      }

      public Paginated(ConfigurationSection nullx) {
         LocalizedItemStack var3 = OnyxShop.getInstance().createItem.createFillItem(nullx, null);
         if (var3 != null && !var3.getItem().getType().equals(Material.AIR)) {
            this.placeholders = NavBarItem.this.getPlaceholders(var3);
            if (this.placeholders.values().stream().anyMatch(var0 -> !var0.isEmpty())) {
               this.staticItem = false;
            }

            this.action = NavBarItem.this.getAction(nullx, null);
            this.orElse = var3;
            this.bedrockTexture = nullx.getString("bedrock-texture");
         } else {
            this.disabled = true;
         }
      }

      public String getBedrockTexture() {
         return this.bedrockTexture;
      }

      public boolean isDisabled() {
         return this.disabled;
      }

      public NavBar.ItemAction getA() {
         return this.action;
      }

      public List<String> getCommands() {
         return this.commands;
      }

      public ItemStack getItem(Player var1, EcoType var2, ShopSearch.QueryMethod var3, int var4, int var5) {
         if (!this.staticItem && this.orElse != null) {
            ItemStack var6 = this.orElse.getItem(var1);
            if (!var6.hasItemMeta()) {
               return var6;
            } else {
               if (var6.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()) && this.skullOwner) {
                  SkullUtil.setSkullTexture(var6, null, var1.getName(), true);
               }

               ItemMeta var7 = var6.getItemMeta();
               Map var8 = NavBarItem.getPlaceholdersFor(this.placeholders, var1);
               if (!var8.isEmpty()) {
                  NavBarItem.this.translateLore(var1, var2, var7, var8, var3, var4, var5);
               }

               var6.setItemMeta(var7);
               return var6;
            }
         } else {
            return this.orElse == null ? null : this.orElse.getItem(var1);
         }
      }

      public ItemStack getItem(Player var1, EcoType var2, int var3, int var4) {
         if (!this.staticItem && this.orElse != null) {
            ItemStack var5 = this.orElse.getItem(var1);
            if (!var5.hasItemMeta()) {
               return var5;
            } else {
               if (var5.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()) && this.skullOwner) {
                  SkullUtil.setSkullTexture(var5, null, var1.getName(), true);
               }

               ItemMeta var6 = var5.getItemMeta();
               Map var7 = NavBarItem.getPlaceholdersFor(this.placeholders, var1);
               if (!var7.isEmpty()) {
                  NavBarItem.this.translateLore(var1, var2, var6, var7, var3, var4);
               }

               var5.setItemMeta(var6);
               return var5;
            }
         } else {
            return this.orElse == null ? null : this.orElse.getItem(var1);
         }
      }
   }
}
