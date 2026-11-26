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

   public BedrockInventory(int var1) {
      this.items = new BedrockInventory.FormItem[var1];
   }

   public BedrockInventory.FormItem[] getItems() {
      return this.items;
   }

   public abstract void openForm(OnyxShop var1, Player var2, FloodgatePlayer var3);

   public abstract void openGridForm(OnyxShop var1, Player var2, FloodgatePlayer var3, boolean var4);

   public String getTitle(Translatable var1, int var2, int var3, boolean var4) {
      String var5 = String.format("%02d", var3);
      String var6 = String.format("%02d", var2);
      String var7 = var1.getBedrockLegacy();
      String var8 = "default";
      int var9 = var7.indexOf(":texture:");
      if (var9 != -1) {
         int var10 = var9 + ":texture:".length();
         int var11 = var7.indexOf(58, var10);
         if (var11 != -1) {
            String var12 = var7.substring(var10, var11);
            if (!var12.isEmpty()) {
               var8 = var12;
            }

            var7 = var7.substring(0, var9) + var7.substring(var11 + 1);
         }
      }

      return textureField(var8)
         + "§"
         + var5.charAt(0)
         + "§"
         + var5.charAt(1)
         + "§E§S§G§U§I§"
         + var6.charAt(0)
         + "§"
         + var6.charAt(1)
         + (var4 ? "§k§r" : "§r§r")
         + "§r"
         + var7;
   }

   public static String textureField(String var0) {
      if (var0 == null || var0.isEmpty()) {
         var0 = "default";
      }

      if (var0.length() > 16) {
         var0 = var0.substring(0, 16);
      }

      StringBuilder var1 = new StringBuilder(var0);

      while (var1.length() < 16) {
         var1.append('?');
      }

      return var1.toString();
   }

   public String buildTooltip(ItemStack var1, boolean var2) {
      int var3 = Math.min(Math.max(var1.getAmount(), 1), 99);
      StringBuilder var4 = new StringBuilder(var2 ? String.format("stack#%02ddur#00§r", var3) : "");
      MetaUtils var5 = OnyxShop.getInstance().getMetaUtils();
      ItemMeta var6 = var1.hasItemMeta() ? var1.getItemMeta() : null;
      if (var6 != null && var6.hasDisplayName()) {
         var4.append(var5.getDisplayName(var6).getBedrockLegacy());
      } else {
         var4.append(OnyxShop.getInstance().getMaterialName(var1.getType().name()));
      }

      if (var2 && var6 != null && var6.hasLore()) {
         for (Translatable var8 : var5.getLore(var6)) {
            var4.append('\n').append(var8.getBedrockLegacy());
         }
      }

      return var4.toString();
   }

   public Builder addGridItem(Builder var1, ItemStack var2, boolean var3, int var4, boolean var5) {
      return this.addGridItem(var1, var2, var3, var4, var5, null);
   }

   public Builder addGridItem(Builder var1, ItemStack var2, boolean var3, int var4, boolean var5, String var6) {
      BedrockInventory.FormItem var7 = new BedrockInventory.FormItem(var2, var3, var6, true);
      if (var7.icon != null) {
         var1.button(var7.getTooltip(var5), Type.PATH, var7.icon);
      } else {
         var1.button(var7.getTooltip(var5));
      }

      this.items[var4] = var7;
      return var1;
   }

   public Builder readdGridItem(Builder var1, int var2, boolean var3) {
      if (this.items[var2].icon != null) {
         var1.button(this.items[var2].getTooltip(var3), FormImage.of(Type.PATH, this.items[var2].icon));
      } else {
         var1.button(this.items[var2].getTooltip(var3), (FormImage)null);
      }

      return var1;
   }

   public Builder addItem(Builder var1, ItemStack var2, int var3) {
      return this.addItem(var1, var2, var3, null);
   }

   public Builder addItem(Builder var1, ItemStack var2, int var3, String var4) {
      this.items[var3] = new BedrockInventory.FormItem(var2, false, var4, false);
      if (this.items[var3].icon != null) {
         var1.button(this.items[var3].tooltip, FormImage.of(Type.PATH, this.items[var3].icon));
      } else {
         var1.button(this.items[var3].tooltip);
      }

      return var1;
   }

   public class FormItem {
      private final String tooltip;
      private final String icon;
      private final boolean fillItem;

      public FormItem(ItemStack nullx, boolean nullxx, String nullxxx, boolean nullxxxx) {
         this.tooltip = BedrockInventory.this.buildTooltip(nullx, nullxxxx);
         this.icon = nullxxx != null ? nullxxx : (nullxxxx ? BedrockItemIcons.getIcon(nullx, BedrockInventory.this.p) : BedrockItemIcons.getTexturePath(nullx));
         this.fillItem = nullxx;
      }

      public String getTooltip(boolean var1) {
         return var1 ? this.tooltip + "§1§3§3§7" : this.tooltip;
      }

      public String getIcon() {
         return this.icon;
      }

      public boolean isFillItem() {
         return this.fillItem;
      }
   }
}
