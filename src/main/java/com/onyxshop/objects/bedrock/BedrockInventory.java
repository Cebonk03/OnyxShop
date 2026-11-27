package com.onyxshop.objects.bedrock;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Translatable;
import com.onyxshop.util.MetaUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.SimpleForm.Builder;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public abstract class BedrockInventory {
   public final BedrockInventory.FormItem[] items;
   public Player p;
   private static final String TEXTURE_PREFIX = ":texture:";

   public BedrockInventory(int value) {
      this.items = new BedrockInventory.FormItem[value];
   }

   public BedrockInventory.FormItem[] getItems() {
      return this.items;
   }

   public abstract void openForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer);

   public abstract void openGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag);

   public String getTitle(Translatable text, int value, int value2, boolean flag) {
      String title = String.format("%02d", value2);
      String text2 = String.format("%02d", value);
      String bedrockLegacy = text.getBedrockLegacy();
      String text3 = "default";
      int index = bedrockLegacy.indexOf(":texture:");
      if (index != -1) {
         int value3 = index + ":texture:".length();
         int value4 = bedrockLegacy.indexOf(58, value3);
         if (value4 != -1) {
            String text4 = bedrockLegacy.substring(value3, value4);
            if (!text4.isEmpty()) {
               text3 = text4;
            }

            bedrockLegacy = bedrockLegacy.substring(0, index) + bedrockLegacy.substring(value4 + 1);
         }
      }

      return textureField(text3)
         + "§"
         + title.charAt(0)
         + "§"
         + title.charAt(1)
         + "§E§S§G§U§I§"
         + text2.charAt(0)
         + "§"
         + text2.charAt(1)
         + (flag ? "§k§r" : "§r§r")
         + "§r"
         + bedrockLegacy;
   }

   public static String textureField(String text) {
      if (text == null || text.isEmpty()) {
         text = "default";
      }

      if (text.length() > 16) {
         text = text.substring(0, 16);
      }

      StringBuilder builder = new StringBuilder(text);

      while (builder.length() < 16) {
         builder.append('?');
      }

      return builder.toString();
   }

   public String buildTooltip(ItemStack itemStack, boolean flag) {
      int amount = Math.min(Math.max(itemStack.getAmount(), 1), 99);
      StringBuilder builder = new StringBuilder(flag ? String.format("stack#%02ddur#00§r", amount) : "");
      MetaUtils metaUtils = OnyxShop.getInstance().getMetaUtils();
      ItemMeta itemMeta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : null;
      if (itemMeta != null && itemMeta.hasDisplayName()) {
         builder.append(metaUtils.getDisplayName(itemMeta).getBedrockLegacy());
      } else {
         builder.append(OnyxShop.getInstance().getMaterialName(itemStack.getType().name()));
      }

      if (flag && itemMeta != null && itemMeta.hasLore()) {
         for (Translatable text : metaUtils.getLore(itemMeta)) {
            builder.append('\n').append(text.getBedrockLegacy());
         }
      }

      return builder.toString();
   }

   public Builder addGridItem(Builder builder, ItemStack itemStack, boolean flag, int value, boolean flag2) {
      return this.addGridItem(builder, itemStack, flag, value, flag2, null);
   }

   public Builder addGridItem(Builder builder, ItemStack itemStack, boolean flag, int value, boolean flag2, String itemId) {
      BedrockInventory.FormItem formItem = new BedrockInventory.FormItem(itemStack, flag, itemId, true);
      if (formItem.icon != null) {
         builder.button(formItem.getTooltip(flag2), Type.PATH, formItem.icon);
      } else {
         builder.button(formItem.getTooltip(flag2));
      }

      this.items[value] = formItem;
      return builder;
   }

   public Builder readdGridItem(Builder builder, int value, boolean flag) {
      if (this.items[value].icon != null) {
         builder.button(this.items[value].getTooltip(flag), FormImage.of(Type.PATH, this.items[value].icon));
      } else {
         builder.button(this.items[value].getTooltip(flag), (FormImage)null);
      }

      return builder;
   }

   public Builder addItem(Builder builder, ItemStack itemStack, int value) {
      return this.addItem(builder, itemStack, value, null);
   }

   public Builder addItem(Builder builder, ItemStack itemStack, int value, String itemId) {
      this.items[value] = new BedrockInventory.FormItem(itemStack, false, itemId, false);
      if (this.items[value].icon != null) {
         builder.button(this.items[value].tooltip, FormImage.of(Type.PATH, this.items[value].icon));
      } else {
         builder.button(this.items[value].tooltip);
      }

      return builder;
   }

   public class FormItem {
      private final String tooltip;
      private final String icon;
      private final boolean fillItem;

      public FormItem(ItemStack itemStack, boolean flag, String itemId, boolean flag2) {
         this.tooltip = BedrockInventory.this.buildTooltip(itemStack, flag2);
         this.icon = itemId != null ? itemId : (flag2 ? BedrockItemIcons.getIcon(itemStack, BedrockInventory.this.p) : BedrockItemIcons.getTexturePath(itemStack));
         this.fillItem = flag;
      }

      public String getTooltip(boolean flag) {
         return flag ? this.tooltip + "§1§3§3§7" : this.tooltip;
      }

      public String getIcon() {
         return this.icon;
      }

      public boolean isFillItem() {
         return this.fillItem;
      }
   }
}
