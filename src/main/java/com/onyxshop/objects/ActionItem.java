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

   public ActionItem(OnyxShop var1, ShopSection var2, String var3) {
      this.rotates = var2 instanceof RotatingShop;
      this.section = var2.getSection();
      this.itemLoc = var3;
      this.itemPath = this.section + "." + this.itemLoc;
      ConfigurationSection var4 = ConfigManager.getShop(this.section).getConfigurationSection((this.rotates ? "" : "pages.") + this.itemLoc);
      this.closeMenu = var4.getBoolean("close-menu");

      try {
         this.requirements = var1.getRequirementManager().load(this.section, var3);
         this.hideIfUnmet = this.requirements != null && var4.getBoolean("if-not-met.hide");
         if (this.hideIfUnmet && var4.contains("if-not-met.placeholder") && var4.contains("if-not-met.placeholder.material")) {
            this.ifUnmet = var1.createItem.createFillItem(var4.getConfigurationSection("if-not-met.placeholder"), this.section);
         }

         try {
            this.action = ActionItem.Action.getFromString(var4.getString("action", "NONE"));
         } catch (IllegalArgumentException var7) {
            throw new ItemLoadException(Lang.INVALID_ITEM_ACTION.get().replace("%action%", var4.getString("action")));
         }

         this.actionItem = var1.createItem.loadActionItem(this);
         if (this.actionItem == null) {
            this.actionItem = var1.createItem.getInvalidShopItem(this.section, this.itemLoc);
            this.error = true;
         } else if (this.actionItem.getType() != Material.AIR) {
            this.displayname = var1.useItemName
               ? (
                  this.actionItem.getItemMeta().hasDisplayName()
                     ? var1.getMetaUtils().getRawDisplayName(this.actionItem.getItemMeta())
                     : var1.getMaterialName(this.actionItem.getType().name())
               )
               : var1.getMaterialName(this.actionItem.getType().name());
            List var5 = this.actionItem.hasItemMeta() ? var1.getMetaUtils().getRawLore(this.actionItem.getItemMeta()) : null;
            this.lore = this.getDynamicLore(var1, var5);
            List var6 = var1.versionHandler.getNBTCommands(this.actionItem, CommandType.CLICK);
            if (var6.size() >= 1) {
               this.clickCommand = true;
            }

            this.placeholders.putAll(PlaceholderAPIHook.getPlaceholders(this.getShopItem(), actionPlaceholders));
         }
      } catch (ItemLoadException var8) {
         this.setError(var1, var8);
         this.error = true;
         SendMessage.warnMessage(var8.getMessage());
         SendMessage.errorShops(this.section, this.itemLoc);
      } catch (Exception var9) {
         if (var9.getMessage() != null) {
            this.setError(var1, var9);
         }

         this.error = true;
         SendMessage.warnMessage("A unknown error occurred while loading item: " + this.itemPath);
         var9.printStackTrace();
      }
   }

   public void setError(OnyxShop var1, Exception var2) {
      List var3 = CalculateAmount.splitLongString(var2.getMessage()).stream().map(var0 -> ChatUtil.formatColors("&c" + var0)).collect(Collectors.toList());
      this.actionItem = var1.createItem.getInvalidShopItem(this.section, this.itemLoc, (String)var3.remove(0));
      this.lore = this.getDynamicLore(var1, var3.isEmpty() ? null : var3);
   }

   public ActionItem(ActionItem var1) {
      this.itemPath = var1.itemPath;
      this.section = var1.section;
      this.itemLoc = var1.itemLoc;
      this.closeMenu = var1.closeMenu;
      this.placeholders.putAll(var1.placeholders);
      this.actionItem = var1.actionItem;
      this.action = var1.action;
      this.displayname = var1.displayname;
      this.clickCommand = var1.clickCommand;
      this.error = var1.error;
      this.lore = var1.lore;
      this.requirements = var1.requirements;
      this.hideIfUnmet = var1.hideIfUnmet;
      this.ifUnmet = var1.ifUnmet;
      this.rotates = var1.rotates;
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
   public boolean meetsRequirements(Player var1, boolean var2) {
      if (this.requirements == null) {
         return true;
      } else {
         for (ItemRequirement var4 : this.requirements) {
            if (!var4.isMet(var1)) {
               if (!var2) {
                  var4.sendNotMetMessage(var1);
               }

               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void addItemRequirement(ItemRequirement var1) {
      this.requirements.add(var1);
   }

   @Override
   public void addItemRequirements(Collection<? extends ItemRequirement> var1) {
      this.requirements.addAll(var1);
   }

   @Override
   public Requirements getRequirements() {
      return this.requirements;
   }

   public LocalizedItemStack getIfUnmet() {
      return this.ifUnmet;
   }

   @Override
   public boolean hasItemRequirement(RequirementType var1) {
      return this.requirements.hasType(var1);
   }

   @Override
   public void removeItemRequirement(ItemRequirement var1) {
      this.requirements.remove(var1);
   }

   @Override
   public void removeItemRequirements(Collection<? extends ItemRequirement> var1) {
      this.requirements.removeAll(var1);
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
   public boolean canPurchase(Player var1, String var2, boolean var3) {
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
   public void updateSkullTexture(Object var1, boolean var2) {
      SkullMeta var3 = (SkullMeta)this.actionItem.getItemMeta();
      SkullUtil.applySkullProfile(var1, var3);
      this.actionItem.setItemMeta(var3);
   }

   private DisplayItem.DynamicLore getDynamicLore(OnyxShop var1, List<String> var2) {
      return (DisplayItem.DynamicLore)(var1.paperMeta ? new ActionItem.DynamicCompLore(var2) : new ActionItem.DynamicBukkitLore(var1, var2));
   }

   public static enum Action {
      BACK,
      CLOSE,
      PAGE_BACK,
      PAGE_NEXT,
      SEARCH;

      private Action() {
      }

      public static ActionItem.Action getFromString(String var0) {
         if (var0 == null) {
            throw new IllegalArgumentException();
         } else {
            return valueOf(var0.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
         }
      }
   }

   private final class DynamicBukkitLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] fastLore;

      public DynamicBukkitLore(OnyxShop nullx, List<String> nullxx) {
         this.lore = nullxx != null && !nullxx.isEmpty()
            ? nullxx.stream().map(var1 -> ChatUtil.formatColors(this.removeUnknownFormats(var1))).toArray(String[]::new)
            : new String[0];
         this.fastLore = Arrays.stream(this.lore).map(var2 -> nullx.versionHandler.toNBT(this.removeUnknownFormats(var2))).toArray(String[]::new);
      }

      private String removeUnknownFormats(String var1) {
         return var1.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean var1, boolean var2) {
         return (String[])(var1 && !var2 ? this.fastLore : this.lore).clone();
      }
   }

   public final class DynamicCompLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] compLore;

      public DynamicCompLore(List<String> nullx) {
         this.lore = nullx != null && !nullx.isEmpty() ? nullx.stream().map(this::removeUnknownFormats).toArray(String[]::new) : new String[0];
         this.compLore = Arrays.stream(this.lore).map(ChatUtil::getGsonComponent).toArray(String[]::new);
      }

      private String removeUnknownFormats(String var1) {
         return var1.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean var1, boolean var2) {
         return (String[])(var1 ? this.compLore : this.lore).clone();
      }
   }
}
