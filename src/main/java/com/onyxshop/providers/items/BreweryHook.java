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

   public BreweryHook(OnyxShop var1, boolean var2) {
      this.plugin = var1;
      this.breweryX = var2;
      if (var1.getServer().getPluginManager().isPluginEnabled(this.getRealName())) {
         this.enable();
      } else {
         var1.runTaskLater(() -> {
            if (!var1.getServer().getPluginManager().isPluginEnabled(this.getRealName())) {
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
   public ItemStack getItem(String var1) throws ItemLoadException {
      if (!this.isReady) {
         return null;
      } else {
         String[] var2 = var1.split("\\.");

         try {
            BRecipe var3 = BreweryApi.getRecipe(var2[0]);
            if (var3 == null) {
               throw new ItemLoadException("Cannot find brewery recipe for '" + var2[0] + "." + var2[1] + "'");
            } else {
               int var9;
               try {
                  var9 = Integer.parseInt(var2[1]);
               } catch (NumberFormatException var7) {
                  throw new ItemLoadException("Invalid brew quality for " + var2[1] + ", should be a number!");
               }

               ItemStack var5 = BreweryApi.createBrewItem(var2[0], var9);
               Brew var6 = BreweryApi.getBrew(var5);
               this.seal(var6, var5);
               return var5;
            }
         } catch (Exception var8) {
            String var4;
            if (var8 instanceof ArrayIndexOutOfBoundsException) {
               var4 = "Invalid syntax for Brewery item, should be '<recipeName>.<quality>'. But got '" + var1 + "'";
            } else if (!(var8 instanceof ItemLoadException)) {
               var4 = "Error while creating Brew item from Brewery";
               var8.printStackTrace();
            } else {
               var4 = var8.getMessage();
            }

            throw new ItemLoadException("Failed to load Brew with reason: " + var4);
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      Brew var2 = Brew.get(var1);
      if (var2 == null) {
         return null;
      } else {
         String var3 = BreweryApi.getRecipeName(var2);
         if (var3 == null) {
            return null;
         } else {
            int var4 = var2.getQuality();
            return var3 + "." + var4;
         }
      }
   }

   @Override
   public String getID(String var1) {
      return var1.split(":")[0];
   }

   private String getRealName() {
      return this.breweryX ? "BreweryX" : "Brewery";
   }

   private void seal(Brew var1, ItemStack var2) throws ItemLoadException {
      if (!this.breweryX) {
         var1.seal(var2, (org.bukkit.entity.Player)null);
      } else {
         try {
            Method var3 = var1.getClass().getDeclaredMethod("seal", ItemStack.class, Player.class);
            var3.setAccessible(true);
            var3.invoke(var1, var2, null);
         } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException var4) {
            var4.printStackTrace();
            throw new ItemLoadException("Failed to seal brew");
         }
      }
   }
}
