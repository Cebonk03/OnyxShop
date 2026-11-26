package com.onyxshop.util;

import java.util.regex.Pattern;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.sound.BukkitSound;
import com.onyxshop.util.sound.KeyedSound;
import com.onyxshop.util.sound.Sound;
import org.bukkit.entity.Player;

public class SoundEffects {
   private final OnyxShop plugin;
   private float soundVolume = 1.0F;
   private float soundPitch = 1.0F;
   private Sound purchaseItem;
   private Sound sellItem;
   private Sound insufficientFunds;
   private Sound insufficientStock;
   private Sound openMainMenu;
   private Sound closeMainMenu;
   private Sound openShop;
   private Sound selectItem;
   private Sound changePage;
   private Sound backButton;
   private Sound selectAmount;

   public SoundEffects(OnyxShop var1) {
      this.plugin = var1;
   }

   private void loadEffects() {
      try {
         this.soundVolume = Float.parseFloat(ConfigManager.getConfig().getString("sound-effects.volume", "1"));
         this.soundPitch = Float.parseFloat(ConfigManager.getConfig().getString("sound-effects.pitch", "1"));
      } catch (NumberFormatException var2) {
         SendMessage.warnMessage("Failed to load sound volume settings in config.yml, should be a number");
      }

      this.purchaseItem = this.getSound("sound-effects.purchase-item");
      this.sellItem = this.getSound("sound-effects.sell-item");
      this.insufficientFunds = this.getSound("sound-effects.insufficient-funds");
      this.insufficientStock = this.getSound("sound-effects.insufficient-stock");
      this.openMainMenu = this.getSound("sound-effects.open-main-menu");
      this.closeMainMenu = this.getSound("sound-effects.close-main-menu");
      this.openShop = this.getSound("sound-effects.open-shop");
      this.selectItem = this.getSound("sound-effects.select-shop-item");
      this.changePage = this.getSound("sound-effects.change-shop-page");
      this.backButton = this.getSound("sound-effects.use-back-button");
      this.selectAmount = this.getSound("sound-effects.select-item-amount");
   }

   private Sound getSound(String var1) {
      String var2 = ConfigManager.getConfig().getString(var1);
      if (var2 != null && !var2.isEmpty()) {
         try {
            return this.getSoundByName(var2);
         } catch (NullPointerException | IllegalArgumentException var6) {
            try {
               SendMessage.warnMessage("Failed to find a sound effect with name '" + var2 + "', using default...");
               return this.getSoundByName(ConfigManager.getConfig().getDef().getString(var2));
            } catch (NullPointerException | IllegalArgumentException var5) {
               SendMessage.warnMessage("Failed to load default sound effect '" + var2 + "', using none...");
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private Sound getSoundByName(String var1) {
      BukkitSound var2 = BukkitSound.valueOf(var1);
      if (var2 != null) {
         return var2;
      } else if (this.isNamespacedKey(var1)) {
         return new KeyedSound(var1);
      } else if (var1.contains(".")) {
         throw new IllegalArgumentException("Failed to parse sound into valid namespaced key for " + var1);
      } else {
         throw new IllegalArgumentException();
      }
   }

   private boolean isNamespacedKey(String var1) {
      int var2 = var1.indexOf(":");
      if (var2 == -1) {
         var1 = "minecraft:" + var1;
         var2 = 9;
      }

      String var3 = var1.substring(0, var2);
      String var4 = var1.substring(var2 + 1);
      Pattern var5 = Pattern.compile("[^a-z0-9_.-]");
      return !var3.isEmpty() && !var4.isEmpty() && var3.length() + var4.length() + 1 <= 256 && !var5.matcher(var3).find() && !var5.matcher(var4).find();
   }

   public void reload() {
      this.loadEffects();
   }

   public void completeTransaction(Player var1, Transaction.Mode var2) {
      if (var2 == Transaction.Mode.BUY) {
         this.purchaseItem(var1);
      } else {
         this.sellItem(var1);
      }
   }

   public void purchaseItem(Player var1) {
      if (this.purchaseItem != null) {
         this.purchaseItem.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void sellItem(Player var1) {
      if (this.sellItem != null) {
         this.sellItem.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void insufficientFunds(Player var1) {
      if (this.insufficientFunds != null) {
         this.insufficientFunds.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void insufficientStock(Player var1) {
      if (this.insufficientStock != null) {
         this.insufficientStock.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void openMainMenu(Player var1) {
      if (this.openMainMenu != null) {
         this.openMainMenu.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void closeMainMenu(Player var1) {
      if (this.closeMainMenu != null) {
         this.closeMainMenu.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void openShop(Player var1) {
      if (this.openShop != null) {
         this.openShop.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void selectItem(Player var1) {
      if (this.selectItem != null) {
         this.selectItem.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void changePage(Player var1) {
      if (this.changePage != null) {
         this.changePage.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void useBackButton(Player var1) {
      if (this.backButton != null) {
         this.backButton.play(var1, this.soundVolume, this.soundPitch);
      }
   }

   public void changeItemAmount(Player var1) {
      if (this.selectAmount != null) {
         this.selectAmount.play(var1, this.soundVolume, this.soundPitch);
      }
   }
}
