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

   public FileUploader(Player player, Consumer<ArrayList<String>> task) {
      this.inv = Bukkit.createInventory(this, 45, "§8§lSelect files to upload");
      this.onComplete = task;
      this.player = player;
      this.buildPane();
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      player.openInventory(this.inv);
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
         for (int size = 0; size < this.inv.getSize(); size++) {
            if (this.inv.getItem(size) == null) {
               this.inv.setItem(size, this.plugin.createItem.fillItem);
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
      Set shops = ConfigManager.getShops();
      ArrayList shopIds = new ArrayList();
      File file = new File(this.plugin.getDataFolder() + File.separator + "shops");
      if (file.exists() && file.isDirectory()) {
         Arrays.stream(file.listFiles()).forEach(entry -> {
            if (entry.isDirectory()) {
               for (File file2 : entry.listFiles()) {
                  if (shops.contains(file2.getName().split("\\.")[0])) {
                     shopIds.add(entry.getName() + "/" + file2.getName().split("\\.")[0]);
                  }
               }
            } else if (shops.contains(entry.getName().split("\\.")[0])) {
               shopIds.add(entry.getName().split("\\.")[0]);
            }
         });
      }

      return shopIds;
   }

   private class Listen implements Listener {
      private Listen() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent event) {
         if (event.getClickedInventory() != null
            && event.getClickedInventory().getHolder() instanceof ShopInventory
            && event.getCurrentItem() != null
            && event.getCurrentItem().getType() != Material.AIR) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (event.getClickedInventory() == FileUploader.this.inv) {
               if (slot == 14) {
                  ItemBuilder itemBuilder;
                  if (!FileUploader.this.files.contains("config.yml")) {
                     itemBuilder = new ItemBuilder(XMaterial.FILLED_MAP.parseItem());
                     itemBuilder.hideFlags();
                     itemBuilder.withDisplayName("§a§lconfig.yml").withEnchantGlint();
                     FileUploader.this.files.add("config.yml");
                  } else {
                     itemBuilder = new ItemBuilder(XMaterial.PAPER.parseItem());
                     itemBuilder.withDisplayName("§c§lconfig.yml").removeEnchants();
                     FileUploader.this.files.remove("config.yml");
                  }

                  itemBuilder.withLore(
                     CalculateAmount.splitLongString(
                        "§6This file won't be uploaded by default, though, if anything related to §ftransaction screens§6, §foption items§6, §fnavigation bars§6, §finventory sizes§6, §fetc... §6is customized in your shop layout, you may opt-in to also include this file in the upload process."
                     )
                  );
                  FileUploader.this.inv.setItem(14, itemBuilder.build());
               } else if (slot == 12) {
                  if (event.getClick() == ClickType.LEFT) {
                     FileUploader.this.current = FileUploader.ShopSelect.class;
                     FileUploader.this.shopSelect.open(FileUploader.this.player);
                  } else if (event.getClick() == ClickType.RIGHT) {
                     if (!FileUploader.this.shops.isEmpty()) {
                        FileUploader.this.shops.clear();
                     } else {
                        FileUploader.this.shops.addAll(ConfigManager.getShops());
                     }

                     FileUploader.this.updateShops();
                  }
               } else if (slot == 30) {
                  FileUploader.this.quit();
                  event.getWhoClicked().closeInventory();
                  FileUploader.this.onComplete.accept(null);
               } else if (slot == 32) {
                  if (FileUploader.this.files.isEmpty() && FileUploader.this.shops.isEmpty()) {
                     return;
                  }

                  FileUploader.this.quit();

                  for (String text : FileUploader.this.shops) {
                     FileUploader.this.files.add("shops/" + text + ".yml");
                     FileUploader.this.files.add("sections/" + text + ".yml");
                  }

                  FileUploader.this.onComplete.accept(FileUploader.this.files);
               }
            } else if (event.getClickedInventory().getHolder() instanceof FileUploader.ShopSelect) {
               if (slot == 53) {
                  FileUploader.this.updateShops();
                  FileUploader.this.current = FileUploader.class;
                  FileUploader.this.player.openInventory(FileUploader.this.inv);
               } else {
                  switch (XMaterial.matchXMaterial(event.getCurrentItem())) {
                     case PAPER:
                     case FILLED_MAP:
                        FileUploader.this.shopSelect.toggle(slot);
                  }
               }
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent event) {
         if (event.getInventory() == FileUploader.this.inv && FileUploader.this.current == FileUploader.class) {
            FileUploader.this.quit();
         } else if (event.getInventory().getHolder() instanceof FileUploader.ShopSelect && FileUploader.this.current == FileUploader.ShopSelect.class) {
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

      public void open(Player player) {
         if (!this.inv.isEmpty()) {
            this.inv.clear();
         }

         this.update();
         this.buildPane(player);
         player.openInventory(this.inv);
      }

      private void update() {
         for (String text : FileUploader.this.originalShops) {
            ItemBuilder itemBuilder;
            if (FileUploader.this.shops.contains(text)) {
               itemBuilder = new ItemBuilder(XMaterial.FILLED_MAP.parseMaterial());
               itemBuilder.withEnchantGlint();
               itemBuilder.hideFlags();
               itemBuilder.withDisplayName("§a§l" + text);
               itemBuilder.withLore("§6Click to deselect");
            } else {
               itemBuilder = new ItemBuilder(XMaterial.PAPER.parseMaterial());
               itemBuilder.hideFlags();
               itemBuilder.withDisplayName("§c§l" + text);
               itemBuilder.withLore("§6Click to select");
            }

            this.inv.addItem(new ItemStack[]{itemBuilder.build()});
         }
      }

      private void buildPane(Player player) {
         this.inv.setItem(45, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
         this.inv.setItem(53, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      }

      public void toggle(int value) {
         ItemStack itemStack = this.inv.getItem(value);
         String itemId = ChatColor.stripColor(itemStack.getItemMeta().getDisplayName());
         if (FileUploader.this.shops.contains(itemId)) {
            ItemBuilder itemBuilder = new ItemBuilder(XMaterial.PAPER.parseMaterial());
            itemBuilder.withDisplayName("§c§l" + itemId);
            itemBuilder.withLore("§6Click to select");
            this.inv.setItem(value, itemBuilder.build());
            FileUploader.this.shops.remove(itemId);
         } else {
            ItemBuilder itemBuilder = new ItemBuilder(XMaterial.FILLED_MAP.parseMaterial());
            itemBuilder.withEnchantGlint();
            itemBuilder.hideFlags();
            itemBuilder.withDisplayName("§a§l" + itemId);
            itemBuilder.withLore("§6Click to deselect");
            this.inv.setItem(value, itemBuilder.build());
            FileUploader.this.shops.add(itemId);
         }
      }
   }
}
