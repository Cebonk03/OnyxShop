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
   public AnvilGUI(Plugin plugin, Player player, String playerName, String text, BiFunction<Player, String, String> task) {
      this(plugin, player, playerName, text, null, false, null, (element, arg2) -> {
         String text2 = (String)task.apply(element, arg2);
         return text2 != null ? AnvilGUI.Response.text(text2) : AnvilGUI.Response.close();
      });
   }

   private AnvilGUI(
      Plugin plugin,
      Player player,
      String text,
      String text2,
      ItemStack itemStack,
      boolean flag,
      Consumer<Player> task,
      BiFunction<Player, String, AnvilGUI.Response> task2
   ) {
      this.plugin = plugin;
      this.player = player;
      this.inventoryTitle = text;
      this.insert = itemStack;
      this.preventClose = flag;
      this.closeListener = task;
      this.completeFunction = task2;
      if (text2 != null) {
         if (itemStack == null) {
            this.insert = new ItemStack(Material.PAPER);
         }

         ItemMeta itemMeta = this.insert.getItemMeta();
         itemMeta.setDisplayName(text2);
         this.insert.setItemMeta(itemMeta);
      }

      this.openInventory();
   }

   private void openInventory() {
      WRAPPER.handleInventoryCloseEvent(this.player);
      WRAPPER.setActiveContainerDefault(this.player);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      VersionHandler.AnvilGUI.AnvilContainer anvilContainer = WRAPPER.newAnvilContainer(this.player, this.inventoryTitle);
      this.inventory = anvilContainer.getBukkitInventory();
      this.inventory.setItem(0, this.insert);
      this.containerId = WRAPPER.getNextContainerId(this.player, anvilContainer);
      WRAPPER.sendPacketOpenWindow(this.player, this.containerId, this.inventoryTitle);
      WRAPPER.setActiveContainer(this.player, anvilContainer);
      WRAPPER.setActiveContainerId(anvilContainer, this.containerId);
      WRAPPER.addActiveContainerSlotListener(anvilContainer, this.player);
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

      public AnvilGUI.Builder onClose(Consumer<Player> task) {
         Validate.notNull(task, "closeListener cannot be null");
         this.closeListener = task;
         return this;
      }

      public AnvilGUI.Builder onLeftInputClick(Consumer<Player> task) {
         this.inputLeftClickListener = task;
         return this;
      }

      public AnvilGUI.Builder onRightInputClick(Consumer<Player> task) {
         this.inputRightClickListener = task;
         return this;
      }

      public AnvilGUI.Builder onComplete(BiFunction<Player, String, AnvilGUI.Response> task) {
         Validate.notNull(task, "Complete function cannot be null");
         this.completeFunction = task;
         return this;
      }

      public AnvilGUI.Builder plugin(Plugin plugin) {
         Validate.notNull(plugin, "Plugin cannot be null");
         this.plugin = plugin;
         return this;
      }

      public AnvilGUI.Builder text(String text) {
         Validate.notNull(text, "Text cannot be null");
         this.itemText = text;
         return this;
      }

      public AnvilGUI.Builder title(String title) {
         Validate.notNull(title, "title cannot be null");
         this.title = title;
         return this;
      }

      @Deprecated
      public AnvilGUI.Builder item(ItemStack itemStack) {
         return this.itemLeft(itemStack);
      }

      public AnvilGUI.Builder itemLeft(ItemStack itemStack) {
         Validate.notNull(itemStack, "item cannot be null");
         this.itemLeft = itemStack;
         return this;
      }

      public AnvilGUI.Builder itemRight(ItemStack itemStack) {
         this.itemRight = itemStack;
         return this;
      }

      public AnvilGUI open(Player player) {
         Validate.notNull(this.plugin, "Plugin cannot be null");
         Validate.notNull(this.completeFunction, "Complete function cannot be null");
         Validate.notNull(player, "Player cannot be null");
         return new AnvilGUI(this.plugin, player, this.title, this.itemText, this.itemLeft, this.preventClose, this.closeListener, this.completeFunction);
      }
   }

   private class ListenUp implements Listener {
      private ListenUp() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent event) {
         if (event.getInventory().equals(AnvilGUI.this.inventory)
            && (
               event.getRawSlot() < 3
                  || event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                  || event.getRawSlot() < 3
                     && (
                        event.getAction().equals(InventoryAction.PLACE_ALL)
                           || event.getAction().equals(InventoryAction.PLACE_ONE)
                           || event.getAction().equals(InventoryAction.PLACE_SOME)
                           || event.getCursor() != null
                     )
            )) {
            event.setCancelled(true);
            Player player = (Player)event.getWhoClicked();
            if (event.getRawSlot() == 2) {
               ItemStack itemStack = AnvilGUI.this.inventory.getItem(2);
               if (itemStack == null || itemStack.getType() == Material.AIR) {
                  return;
               }

               AnvilGUI.Response response = AnvilGUI.this.completeFunction.apply(player, itemStack.hasItemMeta() ? itemStack.getItemMeta().getDisplayName() : "");
               if (response.getText() != null) {
                  ItemMeta itemMeta = itemStack.getItemMeta();
                  itemMeta.setDisplayName(response.getText());
                  itemStack.setItemMeta(itemMeta);
                  AnvilGUI.this.inventory.setItem(0, itemStack);
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
      public void onInventoryDrag(InventoryDragEvent event) {
         if (event.getInventory().equals(AnvilGUI.this.inventory)) {
            for (int slot : AnvilGUI.Slot.values()) {
               if (event.getRawSlots().contains(slot)) {
                  event.setCancelled(true);
                  break;
               }
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent event) {
         if (AnvilGUI.this.open && event.getInventory().equals(AnvilGUI.this.inventory)) {
            AnvilGUI.this.closeInventory();
            if (AnvilGUI.this.preventClose) {
               OnyxShop plugin = OnyxShop.getInstance();
               AnvilGUI anvilGUI = AnvilGUI.this;
               plugin.runTaskFor(() -> anvilGUI.openInventory(), AnvilGUI.this.player);
            }
         }
      }
   }

   public static class Response {
      private final String text;

      private Response(String text) {
         this.text = text;
      }

      public String getText() {
         return this.text;
      }

      public static AnvilGUI.Response close() {
         return new AnvilGUI.Response(null);
      }

      public static AnvilGUI.Response text(String text) {
         return new AnvilGUI.Response(text);
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
