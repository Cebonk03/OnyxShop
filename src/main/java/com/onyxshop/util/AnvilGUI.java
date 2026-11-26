package com.onyxshop.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import com.onyxshop.OnyxShop;
import com.onyxshop.VersionHandler;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class AnvilGUI {
   public static VersionHandler.AnvilGUI WRAPPER;
   private final Plugin plugin;
   private final Player player;
   private String inventoryTitle;
   private ItemStack insert;
   private final boolean preventClose;
   private final Consumer<Player> closeListener;
   private final BiFunction<Player, String, AnvilGUI.Response> completeFunction;
   private int containerId;
   private Inventory inventory;
   private final AnvilGUI.ListenUp listener = new AnvilGUI.ListenUp();
   private boolean open;

   @Deprecated
   public AnvilGUI(Plugin var1, Player var2, String var3, String var4, BiFunction<Player, String, String> var5) {
      this(var1, var2, var3, var4, null, false, null, (var1x, var2x) -> {
         String var3x = (String)var5.apply(var1x, var2x);
         return var3x != null ? AnvilGUI.Response.text(var3x) : AnvilGUI.Response.close();
      });
   }

   private AnvilGUI(
      Plugin var1,
      Player var2,
      String var3,
      String var4,
      ItemStack var5,
      boolean var6,
      Consumer<Player> var7,
      BiFunction<Player, String, AnvilGUI.Response> var8
   ) {
      this.plugin = var1;
      this.player = var2;
      this.inventoryTitle = var3;
      this.insert = var5;
      this.preventClose = var6;
      this.closeListener = var7;
      this.completeFunction = var8;
      if (var4 != null) {
         if (var5 == null) {
            this.insert = new ItemStack(Material.PAPER);
         }

         ItemMeta var9 = this.insert.getItemMeta();
         var9.setDisplayName(var4);
         this.insert.setItemMeta(var9);
      }

      this.openInventory();
   }

   private void openInventory() {
      WRAPPER.handleInventoryCloseEvent(this.player);
      WRAPPER.setActiveContainerDefault(this.player);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      VersionHandler.AnvilGUI.AnvilContainer var1 = WRAPPER.newAnvilContainer(this.player, this.inventoryTitle);
      this.inventory = var1.getBukkitInventory();
      this.inventory.setItem(0, this.insert);
      this.containerId = WRAPPER.getNextContainerId(this.player, var1);
      WRAPPER.sendPacketOpenWindow(this.player, this.containerId, this.inventoryTitle);
      WRAPPER.setActiveContainer(this.player, var1);
      WRAPPER.setActiveContainerId(var1, this.containerId);
      WRAPPER.addActiveContainerSlotListener(var1, this.player);
      this.open = true;
   }

   public void closeInventory() {
      if (this.open) {
         this.open = false;
         WRAPPER.handleInventoryCloseEvent(this.player);
         WRAPPER.setActiveContainerDefault(this.player);
         WRAPPER.sendPacketCloseWindow(this.player, this.containerId);
         HandlerList.unregisterAll(this.listener);
         if (this.closeListener != null) {
            this.closeListener.accept(this.player);
         }
      }
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public static class Builder {
      private Consumer<Player> closeListener;
      private boolean preventClose = false;
      private Consumer<Player> inputLeftClickListener;
      private Consumer<Player> inputRightClickListener;
      private BiFunction<Player, String, AnvilGUI.Response> completeFunction;
      private Plugin plugin;
      private String title = "Repair & Name";
      private String itemText;
      private ItemStack itemLeft;
      private ItemStack itemRight;

      public Builder() {
      }

      public AnvilGUI.Builder preventClose() {
         this.preventClose = true;
         return this;
      }

      public AnvilGUI.Builder onClose(Consumer<Player> var1) {
         Validate.notNull(var1, "closeListener cannot be null");
         this.closeListener = var1;
         return this;
      }

      public AnvilGUI.Builder onLeftInputClick(Consumer<Player> var1) {
         this.inputLeftClickListener = var1;
         return this;
      }

      public AnvilGUI.Builder onRightInputClick(Consumer<Player> var1) {
         this.inputRightClickListener = var1;
         return this;
      }

      public AnvilGUI.Builder onComplete(BiFunction<Player, String, AnvilGUI.Response> var1) {
         Validate.notNull(var1, "Complete function cannot be null");
         this.completeFunction = var1;
         return this;
      }

      public AnvilGUI.Builder plugin(Plugin var1) {
         Validate.notNull(var1, "Plugin cannot be null");
         this.plugin = var1;
         return this;
      }

      public AnvilGUI.Builder text(String var1) {
         Validate.notNull(var1, "Text cannot be null");
         this.itemText = var1;
         return this;
      }

      public AnvilGUI.Builder title(String var1) {
         Validate.notNull(var1, "title cannot be null");
         this.title = var1;
         return this;
      }

      @Deprecated
      public AnvilGUI.Builder item(ItemStack var1) {
         return this.itemLeft(var1);
      }

      public AnvilGUI.Builder itemLeft(ItemStack var1) {
         Validate.notNull(var1, "item cannot be null");
         this.itemLeft = var1;
         return this;
      }

      public AnvilGUI.Builder itemRight(ItemStack var1) {
         this.itemRight = var1;
         return this;
      }

      public AnvilGUI open(Player var1) {
         Validate.notNull(this.plugin, "Plugin cannot be null");
         Validate.notNull(this.completeFunction, "Complete function cannot be null");
         Validate.notNull(var1, "Player cannot be null");
         return new AnvilGUI(this.plugin, var1, this.title, this.itemText, this.itemLeft, this.preventClose, this.closeListener, this.completeFunction);
      }
   }

   private class ListenUp implements Listener {
      private ListenUp() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent var1) {
         if (var1.getInventory().equals(AnvilGUI.this.inventory)
            && (
               var1.getRawSlot() < 3
                  || var1.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                  || var1.getRawSlot() < 3
                     && (
                        var1.getAction().equals(InventoryAction.PLACE_ALL)
                           || var1.getAction().equals(InventoryAction.PLACE_ONE)
                           || var1.getAction().equals(InventoryAction.PLACE_SOME)
                           || var1.getCursor() != null
                     )
            )) {
            var1.setCancelled(true);
            Player var2 = (Player)var1.getWhoClicked();
            if (var1.getRawSlot() == 2) {
               ItemStack var3 = AnvilGUI.this.inventory.getItem(2);
               if (var3 == null || var3.getType() == Material.AIR) {
                  return;
               }

               AnvilGUI.Response var4 = AnvilGUI.this.completeFunction.apply(var2, var3.hasItemMeta() ? var3.getItemMeta().getDisplayName() : "");
               if (var4.getText() != null) {
                  ItemMeta var5 = var3.getItemMeta();
                  var5.setDisplayName(var4.getText());
                  var3.setItemMeta(var5);
                  AnvilGUI.this.inventory.setItem(0, var3);
               } else if (AnvilGUI.this.open) {
                  AnvilGUI.this.open = false;
                  AnvilGUI.WRAPPER.handleInventoryCloseEvent(AnvilGUI.this.player);
                  AnvilGUI.WRAPPER.setActiveContainerDefault(AnvilGUI.this.player);
                  AnvilGUI.WRAPPER.sendPacketCloseWindow(AnvilGUI.this.player, AnvilGUI.this.containerId);
                  HandlerList.unregisterAll(AnvilGUI.this.listener);
               }
            }
         }
      }

      @EventHandler
      public void onInventoryDrag(InventoryDragEvent var1) {
         if (var1.getInventory().equals(AnvilGUI.this.inventory)) {
            for (int var5 : AnvilGUI.Slot.values()) {
               if (var1.getRawSlots().contains(var5)) {
                  var1.setCancelled(true);
                  break;
               }
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent var1) {
         if (AnvilGUI.this.open && var1.getInventory().equals(AnvilGUI.this.inventory)) {
            AnvilGUI.this.closeInventory();
            if (AnvilGUI.this.preventClose) {
               OnyxShop var10000 = OnyxShop.getInstance();
               AnvilGUI var2 = AnvilGUI.this;
               var10000.runTask(() -> var2.openInventory());
            }
         }
      }
   }

   public static class Response {
      private final String text;

      private Response(String var1) {
         this.text = var1;
      }

      public String getText() {
         return this.text;
      }

      public static AnvilGUI.Response close() {
         return new AnvilGUI.Response(null);
      }

      public static AnvilGUI.Response text(String var0) {
         return new AnvilGUI.Response(var0);
      }
   }

   public static class Slot {
      private static final int[] values = new int[]{0, 1, 2};
      public static final int INPUT_LEFT = 0;
      public static final int INPUT_RIGHT = 1;
      public static final int OUTPUT = 2;

      public Slot() {
      }

      public static int[] values() {
         return values;
      }
   }
}
