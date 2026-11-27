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
   public boolean isStatic(int value, int value2) {
      return this.staticItem && (this.paginatedItem == null || value2 == 1 || value != 1 && value != 2 && value != value2 - 1 && value != value2);
   }

   @Override
   public NavBar.ItemAction getAction() {
      return this.action;
   }

   @Override
   public NavBar.ItemAction getAction(int value, int value2) {
      return this.paginatedItem == null
            || value2 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || value != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || value != value2)
         ? this.action
         : this.paginatedItem.getA();
   }

   @Override
   public List<String> getCommands() {
      return this.commands;
   }

   @Override
   public List<String> getCommands(int value) {
      return value == 1 && this.paginatedItem != null && !this.paginatedItem.isDisabled() ? this.paginatedItem.getCommands() : this.commands;
   }

   @Override
   public String getBedrockTexture() {
      return this.bedrockTexture;
   }

   @Override
   public String getBedrockTexture(int value, int value2) {
      return this.paginatedItem == null
            || value2 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || value != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || value != value2)
         ? this.bedrockTexture
         : this.paginatedItem.getBedrockTexture();
   }

   public NavBarItem(ItemStack itemStack, NavBar.ItemAction itemAction) {
      this.navBarItem = new LocalizedItemStack(itemStack);
      this.action = itemAction;
      this.staticItem = false;
   }

   public NavBarItem(ConfigurationSection section, ItemStack itemStack) {
      LocalizedItemStack localizedItemStack = OnyxShop.getInstance().createItem.createFillItem(section, null);
      if (localizedItemStack != null && !localizedItemStack.getItem().getType().equals(Material.AIR)) {
         this.placeholders = this.getPlaceholders(localizedItemStack);
         if (this.placeholders.values().stream().anyMatch(element -> !element.isEmpty())) {
            this.staticItem = false;
         }

         if (section.getBoolean("paginated.enabled")) {
            ConfigurationSection section2 = section.getConfigurationSection("paginated.orElse");
            if (section2 != null && !section2.getKeys(false).isEmpty()) {
               this.paginatedItem = new NavBarItem.Paginated(section2);
            } else {
               this.paginatedItem = new NavBarItem.Paginated(itemStack);
            }

            this.staticItem = false;
         }

         this.action = this.getAction(section, itemStack);
         this.navBarItem = localizedItemStack;
         this.bedrockTexture = section.getString("bedrock-texture");
      } else {
         this.disabled = true;
      }
   }

   private NavBar.ItemAction getAction(ConfigurationSection section, ItemStack itemStack) {
      if (!section.contains("action")) {
         return NavBar.ItemAction.NONE;
      } else {
         NavBar.ItemAction itemAction = NavBar.ItemAction.getFromString(section.getString("action"));
         switch (itemAction) {
            case COMMAND:
               this.commands = section.getStringList("commands");
               this.closeMenu = section.getBoolean("close-menu");
               break;
            case CYCLE_CURRENCY:
               this.onlyMultiplePrices = section.getBoolean("when-multiple-prices", true);
               break;
            case TOGGLE_MODE:
               this.onlyGeyser = section.getBoolean("onlyEnableForGeyserPlayers", true);
            case PAGE_BACK:
            case PAGE_NEXT:
               if (this.paginatedItem == null && section.getBoolean("paginated.enabled", true)) {
                  this.paginatedItem = new NavBarItem.Paginated(itemStack);
                  this.staticItem = false;
               }
         }

         return itemAction;
      }
   }

   @Override
   public ItemStack getItem(Player player) {
      if (!this.staticItem && this.navBarItem != null) {
         ItemStack itemStack = this.navBarItem.getItem(player);
         if (!itemStack.hasItemMeta()) {
            return itemStack;
         } else {
            ItemMeta itemMeta = itemStack.getItemMeta();
            Map placeholdersFor = getPlaceholdersFor(this.placeholders, player);
            if (!placeholdersFor.isEmpty()) {
               this.translateLore(player, null, itemMeta, placeholdersFor);
            }

            itemStack.setItemMeta(itemMeta);
            return itemStack;
         }
      } else {
         return this.navBarItem == null ? null : this.navBarItem.getItem(player);
      }
   }

   @Override
   public ItemStack getItem(Player player, EcoType ecoType, ShopSearch.QueryMethod queryMethod, int value, int value2) {
      if (!this.staticItem && this.navBarItem != null) {
         if (this.paginatedItem == null
            || value2 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || value != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || value != value2)) {
            ItemStack itemStack = this.navBarItem.getItem(player);
            if (!itemStack.hasItemMeta()) {
               return itemStack;
            } else {
               ItemMeta itemMeta = itemStack.getItemMeta();
               Map placeholdersFor = getPlaceholdersFor(this.placeholders, player);
               if (!placeholdersFor.isEmpty()) {
                  this.translateLore(player, ecoType, itemMeta, placeholdersFor, queryMethod, value, value2);
               }

               itemStack.setItemMeta(itemMeta);
               return itemStack;
            }
         } else {
            return this.paginatedItem.getItem(player, ecoType, queryMethod, value, value2);
         }
      } else {
         return this.navBarItem == null ? null : this.navBarItem.getItem(player);
      }
   }

   @Override
   public ItemStack getItem(Player player, EcoType ecoType, int value, int value2) {
      if (!this.staticItem && this.navBarItem != null) {
         if (this.paginatedItem == null
            || value2 != 1 && (this.action != NavBar.ItemAction.PAGE_BACK || value != 1) && (this.action != NavBar.ItemAction.PAGE_NEXT || value != value2)) {
            ItemStack itemStack = this.navBarItem.getItem(player);
            if (!itemStack.hasItemMeta()) {
               return itemStack;
            } else {
               ItemMeta itemMeta = itemStack.getItemMeta();
               Map placeholdersFor = getPlaceholdersFor(this.placeholders, player);
               if (!placeholdersFor.isEmpty()) {
                  this.translateLore(player, ecoType, itemMeta, placeholdersFor, value, value2);
               }

               itemStack.setItemMeta(itemMeta);
               return itemStack;
            }
         } else {
            return this.paginatedItem.getItem(player, ecoType, value, value2);
         }
      } else {
         return this.navBarItem == null ? null : this.navBarItem.getItem(player);
      }
   }

   @Override
   public ItemStack getItem(Player player, Transaction.Mode mode, ShopItem shopItem, EcoType ecoType) {
      if ((!this.onlyGeyser || UserManager.getUser(player).isBedrock())
         && (!this.onlyMultiplePrices || (mode != Transaction.Mode.BUY || shopItem.isABuyPricing()) && (mode != Transaction.Mode.SELL || shopItem.isASellPricing()))) {
         if (!this.staticItem && this.navBarItem != null) {
            ItemStack itemStack = this.navBarItem.getItem(player);
            if (!itemStack.hasItemMeta()) {
               return itemStack;
            } else {
               ItemMeta itemMeta = itemStack.getItemMeta();
               Map placeholdersFor = getPlaceholdersFor(this.placeholders, player);
               if (!placeholdersFor.isEmpty()) {
                  this.translateLore(player, ecoType, itemMeta, placeholdersFor);
               }

               itemStack.setItemMeta(itemMeta);
               return itemStack;
            }
         } else {
            return this.navBarItem == null ? null : this.navBarItem.getItem(player);
         }
      } else {
         return this.paginatedItem != null ? this.paginatedItem.getItem(player, ecoType, 0, 0) : new ItemStack(Material.AIR);
      }
   }

   private Map<Locale, Map<Integer, Placeholder>> getPlaceholders(LocalizedItemStack localizedItemStack) {
      HashMap map = new HashMap();

      for (Entry entry : localizedItemStack.getVariants().entrySet()) {
         map.put((Locale)entry.getKey(), this.getPlaceholders((ItemStack)entry.getValue()));
      }

      return map;
   }

   private Map<Integer, Placeholder> getPlaceholders(ItemStack itemStack) {
      if (itemStack.hasItemMeta() && (itemStack.getItemMeta().hasDisplayName() || itemStack.getItemMeta().hasLore())) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         HashMap map = new HashMap();
         if (itemMeta.hasDisplayName()) {
            String rawDisplayName = OnyxShop.getInstance().getMetaUtils().getRawDisplayName(itemMeta);
            if (Placeholder.isPlaceholder(rawDisplayName, localPlaceholders)) {
               map.put(-1, new Placeholder(rawDisplayName, localPlaceholders));
            }
         }

         if (itemMeta.hasLore()) {
            int value = 0;

            for (String shopId : OnyxShop.getInstance().getMetaUtils().getRawLore(itemMeta)) {
               if (Placeholder.isPlaceholder(shopId, localPlaceholders)) {
                  map.put(value, new Placeholder(shopId, localPlaceholders));
               }

               value++;
            }
         }

         return map;
      } else {
         return Collections.EMPTY_MAP;
      }
   }

   private static Map<Integer, Placeholder> getPlaceholdersFor(Map<Locale, Map<Integer, Placeholder>> map, Player player) {
      if (map == null) {
         return Collections.EMPTY_MAP;
      } else {
         Map map2 = (Map)map.get(UserManager.getUser(player).getLocale());
         return map2 != null ? map2 : map.getOrDefault(Lang.getDefaultLocale(), Collections.EMPTY_MAP);
      }
   }

   private void translateLore(Player player, EcoType ecoType, ItemMeta itemMeta, Map<Integer, Placeholder> map, ShopSearch.QueryMethod queryMethod, int value, int value2) {
      if (map.containsKey(-1)) {
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(itemMeta, ((Placeholder)map.get(-1)).translate(player, this.getPlaceholders(player, ecoType, value, value2, queryMethod)));
      }

      if (itemMeta.hasLore()) {
         List rawLore = OnyxShop.getInstance().getMetaUtils().getRawLore(itemMeta);

         for (Integer value3 : map.keySet()) {
            if (value3 != -1) {
               rawLore.set(value3, ((Placeholder)map.get(value3)).translate(player, this.getPlaceholders(player, ecoType, value, value2, queryMethod)));
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(itemMeta, rawLore);
      }
   }

   private void translateLore(Player player, EcoType ecoType, ItemMeta itemMeta, Map<Integer, Placeholder> map, int value, int value2) {
      if (map.containsKey(-1)) {
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(itemMeta, ((Placeholder)map.get(-1)).translate(player, this.getPlaceholders(player, ecoType, value, value2)));
      }

      if (itemMeta.hasLore()) {
         List rawLore = OnyxShop.getInstance().getMetaUtils().getRawLore(itemMeta);

         for (Integer value3 : map.keySet()) {
            if (value3 != -1) {
               rawLore.set(value3, ((Placeholder)map.get(value3)).translate(player, this.getPlaceholders(player, ecoType, value, value2)));
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(itemMeta, rawLore);
      }
   }

   private void translateLore(Player player, EcoType ecoType, ItemMeta itemMeta, Map<Integer, Placeholder> map) {
      if (map.containsKey(-1)) {
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(itemMeta, ((Placeholder)map.get(-1)).translate(player, this.getPlaceholders(player, ecoType, 0, 0)));
      }

      if (itemMeta.hasLore()) {
         List rawLore = OnyxShop.getInstance().getMetaUtils().getRawLore(itemMeta);

         for (Integer value : map.keySet()) {
            if (value != -1) {
               rawLore.set(value, ((Placeholder)map.get(value)).translate(player, this.getPlaceholders(player, ecoType, 0, 0)));
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(itemMeta, rawLore);
      }
   }

   private Replacement[] getPlaceholders(Player player, EcoType ecoType, int value, int value2) {
      return new Replacement[]{
         new Replacement("%player_name%", player::getName),
         new Replacement("%player_displayname%", () -> OnyxShop.getInstance().getDisplayName(player)),
         new Replacement("%player_level%", () -> String.valueOf(player.getLevel())),
         new Replacement(
            "%player_balance%",
            () -> OnyxShop.getInstance().formatPrice(ecoType, OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).getBalance(player))
         ),
         new Replacement("%page%", () -> String.valueOf(value)),
         new Replacement("%pages%", () -> String.valueOf(value2))
      };
   }

   private Replacement[] getPlaceholders(Player player, EcoType ecoType, int value, int value2, ShopSearch.QueryMethod queryMethod) {
      return new Replacement[]{
         new Replacement("%player_name%", player::getName),
         new Replacement("%player_displayname%", () -> OnyxShop.getInstance().getDisplayName(player)),
         new Replacement("%player_level%", () -> String.valueOf(player.getLevel())),
         new Replacement(
            "%player_balance%",
            () -> OnyxShop.getInstance().formatPrice(ecoType, OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).getBalance(player))
         ),
         new Replacement("%page%", () -> String.valueOf(value)),
         new Replacement("%pages%", () -> String.valueOf(value2)),
         new Replacement("%search-method%", queryMethod::getName)
      };
   }

   private Translatable replaceTranslations(String text) {
      return SendMessage.fromConfig(
         text.replace("%translations-shopsearch-change-search-method%", Lang.SHOPSEARCH_CHANGE_SEARCH_METHOD.get().toString())
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

      public Paginated(ItemStack itemStack) {
         if (itemStack == null) {
            this.disabled = true;
            this.orElse = new LocalizedItemStack(new ItemStack(Material.AIR));
         } else {
            this.orElse = new LocalizedItemStack(itemStack);
         }

         this.action = NavBar.ItemAction.NONE;
      }

      public Paginated(ConfigurationSection section) {
         LocalizedItemStack localizedItemStack = OnyxShop.getInstance().createItem.createFillItem(section, null);
         if (localizedItemStack != null && !localizedItemStack.getItem().getType().equals(Material.AIR)) {
            this.placeholders = NavBarItem.this.getPlaceholders(localizedItemStack);
            if (this.placeholders.values().stream().anyMatch(element -> !element.isEmpty())) {
               this.staticItem = false;
            }

            this.action = NavBarItem.this.getAction(section, null);
            this.orElse = localizedItemStack;
            this.bedrockTexture = section.getString("bedrock-texture");
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

      public ItemStack getItem(Player player, EcoType ecoType, ShopSearch.QueryMethod queryMethod, int value, int value2) {
         if (!this.staticItem && this.orElse != null) {
            ItemStack itemStack = this.orElse.getItem(player);
            if (!itemStack.hasItemMeta()) {
               return itemStack;
            } else {
               if (itemStack.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()) && this.skullOwner) {
                  SkullUtil.setSkullTexture(itemStack, null, player.getName(), true);
               }

               ItemMeta itemMeta = itemStack.getItemMeta();
               Map placeholdersFor = NavBarItem.getPlaceholdersFor(this.placeholders, player);
               if (!placeholdersFor.isEmpty()) {
                  NavBarItem.this.translateLore(player, ecoType, itemMeta, placeholdersFor, queryMethod, value, value2);
               }

               itemStack.setItemMeta(itemMeta);
               return itemStack;
            }
         } else {
            return this.orElse == null ? null : this.orElse.getItem(player);
         }
      }

      public ItemStack getItem(Player player, EcoType ecoType, int value, int value2) {
         if (!this.staticItem && this.orElse != null) {
            ItemStack itemStack = this.orElse.getItem(player);
            if (!itemStack.hasItemMeta()) {
               return itemStack;
            } else {
               if (itemStack.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()) && this.skullOwner) {
                  SkullUtil.setSkullTexture(itemStack, null, player.getName(), true);
               }

               ItemMeta itemMeta = itemStack.getItemMeta();
               Map placeholdersFor = NavBarItem.getPlaceholdersFor(this.placeholders, player);
               if (!placeholdersFor.isEmpty()) {
                  NavBarItem.this.translateLore(player, ecoType, itemMeta, placeholdersFor, value, value2);
               }

               itemStack.setItemMeta(itemMeta);
               return itemStack;
            }
         } else {
            return this.orElse == null ? null : this.orElse.getItem(player);
         }
      }
   }
}
