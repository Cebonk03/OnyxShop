package com.onyxshop.providers.items;

import com.dre.brewery.Brew;
import com.dre.brewery.api.BreweryApi;
import com.dre.brewery.recipe.BRecipe;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BreweryHook implements ItemProvider {
   private static final ItemType TYPE = new ItemType(ItemProviderType.BREWERY);
   private final OnyxShop plugin;
   private final boolean breweryX;
   private boolean isReady = false;

   public BreweryHook(OnyxShop plugin, boolean flag) {
      this.plugin = plugin;
      this.breweryX = flag;
      if (plugin.getServer().getPluginManager().isPluginEnabled(this.getRealName())) {
         this.enable();
      } else {
         plugin.runTaskLater(() -> {
            if (!plugin.getServer().getPluginManager().isPluginEnabled(this.getRealName())) {
               SendMessage.errorMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getRealName()));
            } else {
               this.enable();
            }
         }, 20L);
      }
   }

   private void enable() {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getRealName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
   }

   @Override
   public ItemType getType() {
      return TYPE;
   }

   @Override
   public boolean isReady() {
      return this.isReady;
   }

   @Override
   public ItemStack getItem(String itemId) throws ItemLoadException {
      if (!this.isReady) {
         return null;
      } else {
         String[] values = itemId.split("\\.");

         try {
            BRecipe bRecipe = BreweryApi.getRecipe(values[0]);
            if (bRecipe == null) {
               throw new ItemLoadException("Cannot find brewery recipe for '" + values[0] + "." + values[1] + "'");
            } else {
               int value;
               try {
                  value = Integer.parseInt(values[1]);
               } catch (NumberFormatException exception) {
                  throw new ItemLoadException("Invalid brew quality for " + values[1] + ", should be a number!");
               }

               ItemStack itemStack = BreweryApi.createBrewItem(values[0], value);
               Brew brew = BreweryApi.getBrew(itemStack);
               this.seal(brew, itemStack);
               return itemStack;
            }
         } catch (Exception exception) {
            String text;
            if (exception instanceof ArrayIndexOutOfBoundsException) {
               text = "Invalid syntax for Brewery item, should be '<recipeName>.<quality>'. But got '" + itemId + "'";
            } else if (!(exception instanceof ItemLoadException)) {
               text = "Error while creating Brew item from Brewery";
               exception.printStackTrace();
            } else {
               text = exception.getMessage();
            }

            throw new ItemLoadException("Failed to load Brew with reason: " + text);
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      Brew brew = Brew.get(itemStack);
      if (brew == null) {
         return null;
      } else {
         String recipeName = BreweryApi.getRecipeName(brew);
         if (recipeName == null) {
            return null;
         } else {
            int quality = brew.getQuality();
            return recipeName + "." + quality;
         }
      }
   }

   @Override
   public String getID(String text) {
      return text.split(":")[0];
   }

   private String getRealName() {
      return this.breweryX ? "BreweryX" : "Brewery";
   }

   private void seal(Brew brew, ItemStack itemStack) throws ItemLoadException {
      if (!this.breweryX) {
         brew.seal(itemStack, (org.bukkit.entity.Player)null);
      } else {
         try {
            Method method = brew.getClass().getDeclaredMethod("seal", ItemStack.class, Player.class);
            method.setAccessible(true);
            method.invoke(brew, itemStack, null);
         } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException exception) {
            exception.printStackTrace();
            throw new ItemLoadException("Failed to seal brew");
         }
      }
   }
}
