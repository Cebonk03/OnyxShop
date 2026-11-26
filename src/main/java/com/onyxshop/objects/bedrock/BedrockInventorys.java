package com.onyxshop.objects.bedrock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Translatable;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.bedrock.inventorys.MainMenu;
import com.onyxshop.objects.shops.ShopSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;

public class BedrockInventorys {
   private final Set<String> TOUCH_DEVICES = new HashSet<>(Arrays.asList("GOOGLE", "IOS", "AMAZON", "WINDOWS_PHONE"));
   private final OnyxShop plugin;
   private final FloodgateApi api;
   private boolean gridForms;
   private boolean onlyForToutch;
   private boolean doubleClickForToutch;

   public BedrockInventorys(OnyxShop var1) {
      this.plugin = var1;
      this.api = FloodgateApi.getInstance();
      this.reload();
   }

   public void reload() {
      this.gridForms = ConfigManager.getConfig().getBoolean("bedrock-forms.use-grid-forms", false);
      this.onlyForToutch = ConfigManager.getConfig().getBoolean("bedrock-forms.only-enable-for-touch-devices", false);
      this.doubleClickForToutch = ConfigManager.getConfig().getBoolean("bedrock-forms.use-double-click-for-touch-devices", true);
      BedrockItemIcons.init(this.plugin);
   }

   public boolean isEnabledFor(Player var1) {
      if (!this.onlyForToutch) {
         return true;
      } else {
         FloodgatePlayer var2 = this.api.getPlayer(var1.getUniqueId());
         return var2 != null && this.isTouch(var2);
      }
   }

   public void openMainMenu(Player var1) {
      FloodgatePlayer var2 = this.api.getPlayer(var1.getUniqueId());
      if (var2 != null) {
         if (this.gridForms) {
            new MainMenu(this.plugin, true).openGridForm(this.plugin, var1, var2, this.usesDoubleClick(var2));
         } else {
            new MainMenu(this.plugin, false).openForm(this.plugin, var1, var2);
         }
      }
   }

   public void openShopSection(
      Player var1,
      ShopSection var2,
      int var3,
      boolean var4,
      String var5,
      Map<Integer, ItemStack> var6,
      List<String> var7,
      ItemStack var8,
      Translatable var9,
      int var10,
      boolean var11
   ) {
      FloodgatePlayer var12 = this.api.getPlayer(var1.getUniqueId());
      if (var12 != null) {
         if (this.gridForms) {
            new com.onyxshop.objects.bedrock.inventorys.ShopSection(true, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11)
               .openGridForm(this.plugin, var1, var12, this.usesDoubleClick(var12));
         } else {
            new com.onyxshop.objects.bedrock.inventorys.ShopSection(false, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11)
               .openForm(this.plugin, var1, var12);
         }
      }
   }

   public void openTransactionMenu(TransactionMenu var1) {
      Player var2 = var1.getPlayer();
      FloodgatePlayer var3 = this.api.getPlayer(var2.getUniqueId());
      if (var3 != null) {
         if (this.gridForms) {
            new com.onyxshop.objects.bedrock.inventorys.TransactionMenu(true, var1)
               .openGridForm(this.plugin, var2, var3, this.usesDoubleClick(var3));
         } else {
            new com.onyxshop.objects.bedrock.inventorys.TransactionMenu(false, var1).openForm(this.plugin, var2, var3);
         }
      }
   }

   private boolean usesDoubleClick(FloodgatePlayer var1) {
      return this.doubleClickForToutch && this.isTouch(var1);
   }

   private boolean isTouch(FloodgatePlayer var1) {
      DeviceOs var2 = var1.getDeviceOs();
      return var2 != null && this.TOUCH_DEVICES.contains(var2.name()) ? true : var1.getInputMode() == InputMode.TOUCH;
   }
}
