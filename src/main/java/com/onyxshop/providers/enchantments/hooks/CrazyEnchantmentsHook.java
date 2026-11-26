package com.onyxshop.providers.enchantments.hooks;

import com.badbones69.crazyenchantments.paper.CrazyEnchantments;
import com.badbones69.crazyenchantments.paper.Starter;
import com.badbones69.crazyenchantments.paper.api.enums.CEnchantments;
import com.badbones69.crazyenchantments.paper.api.objects.CEBook;
import com.badbones69.crazyenchantments.paper.api.objects.CEnchantment;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class CrazyEnchantmentsHook implements EnchantProvider {
   private final Starter ce = ((CrazyEnchantments)Bukkit.getPluginManager().getPlugin("CrazyEnchantments")).getStarter();

   public CrazyEnchantmentsHook() {
   }

   @Override
   public String getName(SimpleEnchantment var1) {
      return ((CEnchantment)var1.getEnchant()).getName();
   }

   @Override
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      CEnchantment var3 = this.ce.getCrazyManager().getEnchantmentFromName(var1);
      if (var3 != null) {
         var2 = OnyxShop.getInstance().allowUnsafeEnchants ? var2 : (var2 > var3.getMaxLevel() ? var3.getMaxLevel() : var2);
         return new SimpleEnchantment(EnchantsProvider.Provider.CRAZY_ENCHANTS, var3, var1, var2);
      } else {
         throw new ItemLoadException("CrazyEnchantments could not find a enchantment with name '" + var1 + "'");
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack var1, SimpleEnchantment var2) {
      if (OnyxShop.getInstance().allowUnsafeEnchants) {
         return true;
      } else {
         if (var2.getEnchant() instanceof CEnchantment) {
            CEnchantment var3 = (CEnchantment)var2.getEnchant();
            if (!var3.canEnchantItem(var1)) {
               SendMessage.errorMessage("Could not apply enchantment '" + var2.getKey() + "' to this shop item material type");
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public ItemStack addEnchantment(ItemStack var1, SimpleEnchantment var2, @Nullable DisplayItem var3) {
      if (var2.getEnchant() instanceof CEnchantment) {
         if (XMaterial.matchXMaterial(var1) == XMaterial.BOOK) {
            CEBook var4 = new CEBook((CEnchantment)var2.getEnchant(), var2.getLvl(), var1.getAmount());
            var4.setSuccessRate(100);
            var4.setDestroyRate(0);
            ItemBuilder var5 = new ItemBuilder(var4.buildBook());
            var5.withSimpleMeta(var1);
            return var5.build();
         } else {
            return this.ce.getCrazyManager().addEnchantment(var1, (CEnchantment)var2.getEnchant(), var2.getLvl());
         }
      } else {
         return var1;
      }
   }

   @Override
   public boolean isEnchant(NamespacedKey var1) {
      return this.ce.getCrazyManager().getEnchantmentFromName(var1.getKey()) != null;
   }

   public List<String> getAppliedEnchants(ItemStack var1) {
      return this.ce
         .getEnchantmentBookSettings()
         .getEnchantments(var1)
         .entrySet()
         .stream()
         .map(var0 -> "crazyenchantments:" + ((CEnchantment)var0.getKey()).getName() + ":" + var0.getValue())
         .collect(Collectors.toList());
   }

   public List<SimpleEnchantment> getEnchantsOfItem(ItemStack var1) {
      return this.ce
         .getEnchantmentBookSettings()
         .getEnchantments(var1)
         .entrySet()
         .stream()
         .map(
            var0 -> new SimpleEnchantment(
               EnchantsProvider.Provider.CRAZY_ENCHANTS, var0.getKey(), ((CEnchantment)var0.getKey()).getName(), (Integer)var0.getValue()
            )
         )
         .collect(Collectors.toList());
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return Arrays.stream(CEnchantments.values()).map(var0 -> "crazyenchantments:" + var0.getName() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return Arrays.stream(CEnchantments.values())
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.CRAZY_ENCHANTS, var0.getEnchantment(), var0.getName(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return Arrays.stream(CEnchantments.values())
         .map(CEnchantments::getEnchantment)
         .filter(var1x -> var1x.canEnchantItem(var1))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      ItemStack var2 = new ItemStack(Material.ENCHANTED_BOOK);
      var2 = this.addEnchantment(var2, var1, null);
      ItemMeta var3 = var2.getItemMeta();
      var3.setDisplayName(((CEnchantment)var1.getEnchant()).getInfoName());
      var3.setLore(((CEnchantment)var1.getEnchant()).getInfoDescription());
      var2.setItemMeta(var3);
      return var2;
   }
}
