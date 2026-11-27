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

   public BedrockInventorys(OnyxShop plugin) {
      this.plugin = plugin;
      this.api = FloodgateApi.getInstance();
      this.reload();
   }

   public void reload() {
      this.gridForms = ConfigManager.getConfig().getBoolean("bedrock-forms.use-grid-forms", false);
      this.onlyForToutch = ConfigManager.getConfig().getBoolean("bedrock-forms.only-enable-for-touch-devices", false);
      this.doubleClickForToutch = ConfigManager.getConfig().getBoolean("bedrock-forms.use-double-click-for-touch-devices", true);
      BedrockItemIcons.init(this.plugin);
   }

   public boolean isEnabledFor(Player player) {
      if (!this.onlyForToutch) {
         return true;
      } else {
         FloodgatePlayer floodgatePlayer = this.api.getPlayer(player.getUniqueId());
         return floodgatePlayer != null && this.isTouch(floodgatePlayer);
      }
   }

   public void openMainMenu(Player player) {
      FloodgatePlayer floodgatePlayer = this.api.getPlayer(player.getUniqueId());
      if (floodgatePlayer != null) {
         if (this.gridForms) {
            new MainMenu(this.plugin, true).openGridForm(this.plugin, player, floodgatePlayer, this.usesDoubleClick(floodgatePlayer));
         } else {
            new MainMenu(this.plugin, false).openForm(this.plugin, player, floodgatePlayer);
         }
      }
   }

   public void openShopSection(
      Player player,
      ShopSection section,
      int value,
      boolean flag,
      String shopId,
      Map<Integer, ItemStack> map,
      List<String> sectionIds,
      ItemStack itemStack,
      Translatable text,
      int value2,
      boolean flag2
   ) {
      FloodgatePlayer floodgatePlayer = this.api.getPlayer(player.getUniqueId());
      if (floodgatePlayer != null) {
         if (this.gridForms) {
            new com.onyxshop.objects.bedrock.inventorys.ShopSection(true, section, value, flag, shopId, map, sectionIds, itemStack, text, value2, flag2)
               .openGridForm(this.plugin, player, floodgatePlayer, this.usesDoubleClick(floodgatePlayer));
         } else {
            new com.onyxshop.objects.bedrock.inventorys.ShopSection(false, section, value, flag, shopId, map, sectionIds, itemStack, text, value2, flag2)
               .openForm(this.plugin, player, floodgatePlayer);
         }
      }
   }

   public void openTransactionMenu(TransactionMenu transactionMenu) {
      Player player = transactionMenu.getPlayer();
      FloodgatePlayer floodgatePlayer = this.api.getPlayer(player.getUniqueId());
      if (floodgatePlayer != null) {
         if (this.gridForms) {
            new com.onyxshop.objects.bedrock.inventorys.TransactionMenu(true, transactionMenu)
               .openGridForm(this.plugin, player, floodgatePlayer, this.usesDoubleClick(floodgatePlayer));
         } else {
            new com.onyxshop.objects.bedrock.inventorys.TransactionMenu(false, transactionMenu).openForm(this.plugin, player, floodgatePlayer);
         }
      }
   }

   private boolean usesDoubleClick(FloodgatePlayer floodgatePlayer) {
      return this.doubleClickForToutch && this.isTouch(floodgatePlayer);
   }

   private boolean isTouch(FloodgatePlayer floodgatePlayer) {
      DeviceOs deviceOs = floodgatePlayer.getDeviceOs();
      return deviceOs != null && this.TOUCH_DEVICES.contains(deviceOs.name()) ? true : floodgatePlayer.getInputMode() == InputMode.TOUCH;
   }
}
