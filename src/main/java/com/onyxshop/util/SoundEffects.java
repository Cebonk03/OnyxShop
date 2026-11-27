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

   public SoundEffects(OnyxShop plugin) {
      this.plugin = plugin;
   }

   private void loadEffects() {
      try {
         this.soundVolume = Float.parseFloat(ConfigManager.getConfig().getString("sound-effects.volume", "1"));
         this.soundPitch = Float.parseFloat(ConfigManager.getConfig().getString("sound-effects.pitch", "1"));
      } catch (NumberFormatException exception) {
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

   private Sound getSound(String text) {
      String text2 = ConfigManager.getConfig().getString(text);
      if (text2 != null && !text2.isEmpty()) {
         try {
            return this.getSoundByName(text2);
         } catch (NullPointerException | IllegalArgumentException exception) {
            try {
               SendMessage.warnMessage("Failed to find a sound effect with name '" + text2 + "', using default...");
               return this.getSoundByName(ConfigManager.getConfig().getDef().getString(text2));
            } catch (NullPointerException | IllegalArgumentException exception2) {
               SendMessage.warnMessage("Failed to load default sound effect '" + text2 + "', using none...");
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private Sound getSoundByName(String name) {
      BukkitSound bukkitSound = BukkitSound.valueOf(name);
      if (bukkitSound != null) {
         return bukkitSound;
      } else if (this.isNamespacedKey(name)) {
         return new KeyedSound(name);
      } else if (name.contains(".")) {
         throw new IllegalArgumentException("Failed to parse sound into valid namespaced key for " + name);
      } else {
         throw new IllegalArgumentException();
      }
   }

   private boolean isNamespacedKey(String key) {
      int index = key.indexOf(":");
      if (index == -1) {
         key = "minecraft:" + key;
         index = 9;
      }

      String text = key.substring(0, index);
      String text2 = key.substring(index + 1);
      Pattern pattern = Pattern.compile("[^a-z0-9_.-]");
      return !text.isEmpty() && !text2.isEmpty() && text.length() + text2.length() + 1 <= 256 && !pattern.matcher(text).find() && !pattern.matcher(text2).find();
   }

   public void reload() {
      this.loadEffects();
   }

   public void completeTransaction(Player player, Transaction.Mode mode) {
      if (mode == Transaction.Mode.BUY) {
         this.purchaseItem(player);
      } else {
         this.sellItem(player);
      }
   }

   public void purchaseItem(Player player) {
      if (this.purchaseItem != null) {
         this.purchaseItem.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void sellItem(Player player) {
      if (this.sellItem != null) {
         this.sellItem.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void insufficientFunds(Player player) {
      if (this.insufficientFunds != null) {
         this.insufficientFunds.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void insufficientStock(Player player) {
      if (this.insufficientStock != null) {
         this.insufficientStock.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void openMainMenu(Player player) {
      if (this.openMainMenu != null) {
         this.openMainMenu.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void closeMainMenu(Player player) {
      if (this.closeMainMenu != null) {
         this.closeMainMenu.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void openShop(Player player) {
      if (this.openShop != null) {
         this.openShop.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void selectItem(Player player) {
      if (this.selectItem != null) {
         this.selectItem.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void changePage(Player player) {
      if (this.changePage != null) {
         this.changePage.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void useBackButton(Player player) {
      if (this.backButton != null) {
         this.backButton.play(player, this.soundVolume, this.soundPitch);
      }
   }

   public void changeItemAmount(Player player) {
      if (this.selectAmount != null) {
         this.selectAmount.play(player, this.soundVolume, this.soundPitch);
      }
   }
}
