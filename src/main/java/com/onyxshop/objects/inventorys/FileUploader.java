package com.onyxshop.objects.inventorys;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.objects.ShopInventory;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class FileUploader extends ShopInventory {
   private final Inventory inv;
   private final FileUploader.Listen listener = new FileUploader.Listen();
   private final OnyxShop plugin = OnyxShop.getInstance();
   private final ArrayList<String> originalShops = this.initShopFiles();
   private final ArrayList<String> shops = new ArrayList<>(this.originalShops);
   private FileUploader.ShopSelect shopSelect = new FileUploader.ShopSelect();
   private final ArrayList<String> files = new ArrayList<>();
   private final Consumer<ArrayList<String>> onComplete;
   private final Player player;
   private Class<? extends ShopInventory> current = FileUploader.class;

   public FileUploader(Player var1, Consumer<ArrayList<String>> var2) {
      this.inv = Bukkit.createInventory(this, 45, "§8§lSelect files to upload");
      this.onComplete = var2;
      this.player = var1;
      this.buildPane();
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      var1.openInventory(this.inv);
   }

   private void buildPane() {
      this.inv
         .setItem(
            12,
            new ItemBuilder(XMaterial.FILLED_MAP.parseItem())
               .withDisplayName("§a§lshops")
               .hideFlags()
               .withLore(
                  CalculateAmount.splitLongString(
                     "§6§lShops selected for upload: §r§c"
                        + this.shops.size()
                        + " \n \n §aLeft click to select/deselect shop files \n §aRight click to select/deselect every shop file"
                  )
               )
               .build()
         );
      this.inv
         .setItem(
            14,
            new ItemBuilder(XMaterial.PAPER.parseItem())
               .withDisplayName("§c§lconfig.yml")
               .withLore(
                  CalculateAmount.splitLongString(
                     "§6This file won't be uploaded by default, though, if anything related to §ftransaction screens§6, §foption items§6, §fnavigation bars§6, §finventory sizes§6, §fetc... §6is customized in your shop layout, you may opt-in to also include this file in the upload process."
                  )
               )
               .build()
         );
      this.inv.setItem(32, new ItemBuilder(XMaterial.GREEN_WOOL.parseItem()).withDisplayName("§a§lComplete upload process").build());
      this.inv.setItem(30, new ItemBuilder(XMaterial.RED_WOOL.parseItem()).withDisplayName("§c§lCancel upload process").build());
      this.fillEmpty();
   }

   private void fillEmpty() {
      if (this.plugin.createItem.fillItem != null) {
         for (int var1 = 0; var1 < this.inv.getSize(); var1++) {
            if (this.inv.getItem(var1) == null) {
               this.inv.setItem(var1, this.plugin.createItem.fillItem);
            }
         }
      }
   }

   private void updateShops() {
      this.inv
         .setItem(
            12,
            new ItemBuilder(XMaterial.FILLED_MAP.parseItem())
               .withDisplayName("§a§lshops")
               .hideFlags()
               .withLore(
                  CalculateAmount.splitLongString(
                     "§6§lShops selected for upload: §r§c"
                        + this.shops.size()
                        + " \n \n §aLeft click to select/deselect shop files \n §aRight click to select/deselect every shop file"
                  )
               )
               .build()
         );
   }

   private void quit() {
      InventoryClickEvent.getHandlerList().unregister(this.listener);
   }

   private ArrayList<String> initShopFiles() {
      Set var1 = ConfigManager.getShops();
      ArrayList var2 = new ArrayList();
      File var3 = new File(this.plugin.getDataFolder() + File.separator + "shops");
      if (var3.exists() && var3.isDirectory()) {
         Arrays.stream(var3.listFiles()).forEach(var2x -> {
            if (var2x.isDirectory()) {
               for (File var6 : var2x.listFiles()) {
                  if (var1.contains(var6.getName().split("\\.")[0])) {
                     var2.add(var2x.getName() + "/" + var6.getName().split("\\.")[0]);
                  }
               }
            } else if (var1.contains(var2x.getName().split("\\.")[0])) {
               var2.add(var2x.getName().split("\\.")[0]);
            }
         });
      }

      return var2;
   }

   private class Listen implements Listener {
      private Listen() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent var1) {
         if (var1.getClickedInventory() != null
            && var1.getClickedInventory().getHolder() instanceof ShopInventory
            && var1.getCurrentItem() != null
            && var1.getCurrentItem().getType() != Material.AIR) {
            var1.setCancelled(true);
            int var2 = var1.getRawSlot();
            if (var1.getClickedInventory() == FileUploader.this.inv) {
               if (var2 == 14) {
                  ItemBuilder var3;
                  if (!FileUploader.this.files.contains("config.yml")) {
                     var3 = new ItemBuilder(XMaterial.FILLED_MAP.parseItem());
                     var3.hideFlags();
                     var3.withDisplayName("§a§lconfig.yml").withEnchantGlint();
                     FileUploader.this.files.add("config.yml");
                  } else {
                     var3 = new ItemBuilder(XMaterial.PAPER.parseItem());
                     var3.withDisplayName("§c§lconfig.yml").removeEnchants();
                     FileUploader.this.files.remove("config.yml");
                  }

                  var3.withLore(
                     CalculateAmount.splitLongString(
                        "§6This file won't be uploaded by default, though, if anything related to §ftransaction screens§6, §foption items§6, §fnavigation bars§6, §finventory sizes§6, §fetc... §6is customized in your shop layout, you may opt-in to also include this file in the upload process."
                     )
                  );
                  FileUploader.this.inv.setItem(14, var3.build());
               } else if (var2 == 12) {
                  if (var1.getClick() == ClickType.LEFT) {
                     FileUploader.this.current = FileUploader.ShopSelect.class;
                     FileUploader.this.shopSelect.open(FileUploader.this.player);
                  } else if (var1.getClick() == ClickType.RIGHT) {
                     if (!FileUploader.this.shops.isEmpty()) {
                        FileUploader.this.shops.clear();
                     } else {
                        FileUploader.this.shops.addAll(ConfigManager.getShops());
                     }

                     FileUploader.this.updateShops();
                  }
               } else if (var2 == 30) {
                  FileUploader.this.quit();
                  var1.getWhoClicked().closeInventory();
                  FileUploader.this.onComplete.accept(null);
               } else if (var2 == 32) {
                  if (FileUploader.this.files.isEmpty() && FileUploader.this.shops.isEmpty()) {
                     return;
                  }

                  FileUploader.this.quit();

                  for (String var4 : FileUploader.this.shops) {
                     FileUploader.this.files.add("shops/" + var4 + ".yml");
                     FileUploader.this.files.add("sections/" + var4 + ".yml");
                  }

                  FileUploader.this.onComplete.accept(FileUploader.this.files);
               }
            } else if (var1.getClickedInventory().getHolder() instanceof FileUploader.ShopSelect) {
               if (var2 == 53) {
                  FileUploader.this.updateShops();
                  FileUploader.this.current = FileUploader.class;
                  FileUploader.this.player.openInventory(FileUploader.this.inv);
               } else {
                  switch (XMaterial.matchXMaterial(var1.getCurrentItem())) {
                     case PAPER:
                     case FILLED_MAP:
                        FileUploader.this.shopSelect.toggle(var2);
                  }
               }
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent var1) {
         if (var1.getInventory() == FileUploader.this.inv && FileUploader.this.current == FileUploader.class) {
            FileUploader.this.quit();
         } else if (var1.getInventory().getHolder() instanceof FileUploader.ShopSelect && FileUploader.this.current == FileUploader.ShopSelect.class) {
            FileUploader.this.updateShops();
            FileUploader.this.current = FileUploader.class;
            FileUploader.this.player.openInventory(FileUploader.this.inv);
         }
      }
   }

   private class ShopSelect extends ShopInventory {
      public final Inventory inv = Bukkit.createInventory(this, 54, "§8§lSelect shops to upload");

      public ShopSelect() {
      }

      public void open(Player var1) {
         if (!this.inv.isEmpty()) {
            this.inv.clear();
         }

         this.update();
         this.buildPane(var1);
         var1.openInventory(this.inv);
      }

      private void update() {
         for (String var2 : FileUploader.this.originalShops) {
            ItemBuilder var3;
            if (FileUploader.this.shops.contains(var2)) {
               var3 = new ItemBuilder(XMaterial.FILLED_MAP.parseMaterial());
               var3.withEnchantGlint();
               var3.hideFlags();
               var3.withDisplayName("§a§l" + var2);
               var3.withLore("§6Click to deselect");
            } else {
               var3 = new ItemBuilder(XMaterial.PAPER.parseMaterial());
               var3.hideFlags();
               var3.withDisplayName("§c§l" + var2);
               var3.withLore("§6Click to select");
            }

            this.inv.addItem(new ItemStack[]{var3.build()});
         }
      }

      private void buildPane(Player var1) {
         this.inv.setItem(45, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
         this.inv.setItem(53, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      }

      public void toggle(int var1) {
         ItemStack var2 = this.inv.getItem(var1);
         String var3 = ChatColor.stripColor(var2.getItemMeta().getDisplayName());
         if (FileUploader.this.shops.contains(var3)) {
            ItemBuilder var4 = new ItemBuilder(XMaterial.PAPER.parseMaterial());
            var4.withDisplayName("§c§l" + var3);
            var4.withLore("§6Click to select");
            this.inv.setItem(var1, var4.build());
            FileUploader.this.shops.remove(var3);
         } else {
            ItemBuilder var5 = new ItemBuilder(XMaterial.FILLED_MAP.parseMaterial());
            var5.withEnchantGlint();
            var5.hideFlags();
            var5.withDisplayName("§a§l" + var3);
            var5.withLore("§6Click to deselect");
            this.inv.setItem(var1, var5.build());
            FileUploader.this.shops.add(var3);
         }
      }
   }
}
