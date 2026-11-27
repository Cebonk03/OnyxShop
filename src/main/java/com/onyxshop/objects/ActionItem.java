package com.onyxshop.objects;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.requirements.ItemRequirement;
import com.onyxshop.providers.requirements.RequirementType;
import com.onyxshop.providers.requirements.Requirements;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.Placeholder;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class ActionItem implements DisplayItem {
   private static final String[] actionPlaceholders = new String[]{
      "%player_name%", "%player_displayname%", "%player_level%", "%player_balance%", "%page%", "%pages%"
   };
   private ItemStack actionItem;
   private ActionItem.Action action;
   private boolean hideIfUnmet;
   private LocalizedItemStack ifUnmet;
   private Requirements requirements;
   private boolean error = false;
   private boolean clickCommand;
   private final boolean rotates;
   private final boolean closeMenu;
   private final Map<Integer, Placeholder> placeholders = new HashMap<>();
   private DisplayItem.DynamicLore lore;
   private String displayname;
   private final String itemPath;
   public final String section;
   public final String itemLoc;

   public ActionItem(OnyxShop plugin, ShopSection section, String itemId) {
      this.rotates = section instanceof RotatingShop;
      this.section = section.getSection();
      this.itemLoc = itemId;
      this.itemPath = this.section + "." + this.itemLoc;
      ConfigurationSection section2 = ConfigManager.getShop(this.section).getConfigurationSection((this.rotates ? "" : "pages.") + this.itemLoc);
      this.closeMenu = section2.getBoolean("close-menu");

      try {
         this.requirements = plugin.getRequirementManager().load(this.section, itemId);
         this.hideIfUnmet = this.requirements != null && section2.getBoolean("if-not-met.hide");
         if (this.hideIfUnmet && section2.contains("if-not-met.placeholder") && section2.contains("if-not-met.placeholder.material")) {
            this.ifUnmet = plugin.createItem.createFillItem(section2.getConfigurationSection("if-not-met.placeholder"), this.section);
         }

         try {
            this.action = ActionItem.Action.getFromString(section2.getString("action", "NONE"));
         } catch (IllegalArgumentException exception) {
            throw new ItemLoadException(Lang.INVALID_ITEM_ACTION.get().replace("%action%", section2.getString("action")));
         }

         this.actionItem = plugin.createItem.loadActionItem(this);
         if (this.actionItem == null) {
            this.actionItem = plugin.createItem.getInvalidShopItem(this.section, this.itemLoc);
            this.error = true;
         } else if (this.actionItem.getType() != Material.AIR) {
            this.displayname = plugin.useItemName
               ? (
                  this.actionItem.getItemMeta().hasDisplayName()
                     ? plugin.getMetaUtils().getRawDisplayName(this.actionItem.getItemMeta())
                     : plugin.getMaterialName(this.actionItem.getType().name())
               )
               : plugin.getMaterialName(this.actionItem.getType().name());
            List lores = this.actionItem.hasItemMeta() ? plugin.getMetaUtils().getRawLore(this.actionItem.getItemMeta()) : null;
            this.lore = this.getDynamicLore(plugin, lores);
            List nbtCommands = plugin.versionHandler.getNBTCommands(this.actionItem, CommandType.CLICK);
            if (nbtCommands.size() >= 1) {
               this.clickCommand = true;
            }

            this.placeholders.putAll(PlaceholderAPIHook.getPlaceholders(this.getShopItem(), actionPlaceholders));
         }
      } catch (ItemLoadException exception) {
         this.setError(plugin, exception);
         this.error = true;
         SendMessage.warnMessage(exception.getMessage());
         SendMessage.errorShops(this.section, this.itemLoc);
      } catch (Exception exception) {
         if (exception.getMessage() != null) {
            this.setError(plugin, exception);
         }

         this.error = true;
         SendMessage.warnMessage("A unknown error occurred while loading item: " + this.itemPath);
         exception.printStackTrace();
      }
   }

   public void setError(OnyxShop plugin, Exception exception) {
      List messages = CalculateAmount.splitLongString(exception.getMessage()).stream().map(element -> ChatUtil.formatColors("&c" + element)).collect(Collectors.toList());
      this.actionItem = plugin.createItem.getInvalidShopItem(this.section, this.itemLoc, (String)messages.remove(0));
      this.lore = this.getDynamicLore(plugin, messages.isEmpty() ? null : messages);
   }

   public ActionItem(ActionItem actionItem) {
      this.itemPath = actionItem.itemPath;
      this.section = actionItem.section;
      this.itemLoc = actionItem.itemLoc;
      this.closeMenu = actionItem.closeMenu;
      this.placeholders.putAll(actionItem.placeholders);
      this.actionItem = actionItem.actionItem;
      this.action = actionItem.action;
      this.displayname = actionItem.displayname;
      this.clickCommand = actionItem.clickCommand;
      this.error = actionItem.error;
      this.lore = actionItem.lore;
      this.requirements = actionItem.requirements;
      this.hideIfUnmet = actionItem.hideIfUnmet;
      this.ifUnmet = actionItem.ifUnmet;
      this.rotates = actionItem.rotates;
   }

   @Override
   public DisplayItem.DynamicLore getLore() {
      return this.lore;
   }

   @Override
   public String section() {
      return this.section;
   }

   @Override
   public String itemLoc() {
      return this.itemLoc;
   }

   @Override
   public String getItemPath() {
      return this.itemPath;
   }

   @Override
   public ItemStack getShopItem() {
      return this.actionItem;
   }

   public ActionItem.Action getAction() {
      return this.action;
   }

   @Override
   public boolean meetsRequirements(Player player, boolean flag) {
      if (this.requirements == null) {
         return true;
      } else {
         for (ItemRequirement itemRequirement : this.requirements) {
            if (!itemRequirement.isMet(player)) {
               if (!flag) {
                  itemRequirement.sendNotMetMessage(player);
               }

               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void addItemRequirement(ItemRequirement itemRequirement) {
      this.requirements.add(itemRequirement);
   }

   @Override
   public void addItemRequirements(Collection<? extends ItemRequirement> values) {
      this.requirements.addAll(values);
   }

   @Override
   public Requirements getRequirements() {
      return this.requirements;
   }

   public LocalizedItemStack getIfUnmet() {
      return this.ifUnmet;
   }

   @Override
   public boolean hasItemRequirement(RequirementType requirementType) {
      return this.requirements.hasType(requirementType);
   }

   @Override
   public void removeItemRequirement(ItemRequirement itemRequirement) {
      this.requirements.remove(itemRequirement);
   }

   @Override
   public void removeItemRequirements(Collection<? extends ItemRequirement> values) {
      this.requirements.removeAll(values);
   }

   @Override
   public boolean rotates() {
      return this.rotates;
   }

   @Override
   public String getDisplayname() {
      return this.displayname;
   }

   @Override
   public boolean hasItemError() {
      return this.error;
   }

   @Override
   public boolean isCloseMenu() {
      return this.closeMenu;
   }

   @Override
   public boolean isDisplayItem() {
      return false;
   }

   @Override
   public boolean isClickCommand() {
      return this.clickCommand;
   }

   public boolean isHideIfUnmet() {
      return this.hideIfUnmet;
   }

   @Override
   public boolean canPurchase(Player player, String playerName, boolean flag) {
      return true;
   }

   @Override
   public boolean hasPlaceholders() {
      return !this.placeholders.isEmpty();
   }

   @Override
   public Map<Integer, Placeholder> getPlaceholders() {
      return this.placeholders;
   }

   @Override
   public void updateSkullTexture(Object value, boolean flag) {
      SkullMeta skullMeta = (SkullMeta)this.actionItem.getItemMeta();
      SkullUtil.applySkullProfile(value, skullMeta);
      this.actionItem.setItemMeta(skullMeta);
   }

   private DisplayItem.DynamicLore getDynamicLore(OnyxShop plugin, List<String> lores) {
      return (DisplayItem.DynamicLore)(plugin.paperMeta ? new ActionItem.DynamicCompLore(lores) : new ActionItem.DynamicBukkitLore(plugin, lores));
   }

   public static enum Action {
      BACK,
      CLOSE,
      PAGE_BACK,
      PAGE_NEXT,
      SEARCH;

      private Action() {
      }

      public static ActionItem.Action getFromString(String itemId) {
         if (itemId == null) {
            throw new IllegalArgumentException();
         } else {
            return valueOf(itemId.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
         }
      }
   }

   private final class DynamicBukkitLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] fastLore;

      public DynamicBukkitLore(OnyxShop plugin, List<String> lores) {
         this.lore = lores != null && !lores.isEmpty()
            ? lores.stream().map(element -> ChatUtil.formatColors(this.removeUnknownFormats(element))).toArray(String[]::new)
            : new String[0];
         this.fastLore = Arrays.stream(this.lore).map(element -> plugin.versionHandler.toNBT(this.removeUnknownFormats(element))).toArray(String[]::new);
      }

      private String removeUnknownFormats(String text) {
         return text.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean flag, boolean flag2) {
         return (String[])(flag && !flag2 ? this.fastLore : this.lore).clone();
      }
   }

   public final class DynamicCompLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] compLore;

      public DynamicCompLore(List<String> lores) {
         this.lore = lores != null && !lores.isEmpty() ? lores.stream().map(this::removeUnknownFormats).toArray(String[]::new) : new String[0];
         this.compLore = Arrays.stream(this.lore).map(ChatUtil::getGsonComponent).toArray(String[]::new);
      }

      private String removeUnknownFormats(String text) {
         return text.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean flag, boolean flag2) {
         return (String[])(flag ? this.compLore : this.lore).clone();
      }
   }
}
