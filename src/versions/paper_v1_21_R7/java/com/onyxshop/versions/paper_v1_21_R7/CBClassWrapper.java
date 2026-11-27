package com.onyxshop.versions.paper_v1_21_R7;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.onyxshop.methodes.SendMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CBClassWrapper {
   private static Class<?> craftChatMessage;
   private static Method chatMessageFromString;
   private static Method chatMessageFromStringOrNull;
   private static Method chatMessageFromComponent;
   private static Method chatMessageToJSON;
   private static Method chatMessageFromJSON;
   private static Method chatMessageFromJSONOrString;
   private static Class<?> craftItemStack;
   private static Method craftStackAsNMSCopy;
   private static Method craftStackAsBukkitCopy;
   private static Class<?> craftItem;
   private static Method getItemHandle;
   private static Class<?> craftWorld;
   private static Method getWorldHandle;
   private static Class<?> craftPlayer;
   private static Method getPlayerHandle;
   private static Class<?> craftEventFactory;
   private static Method eventFactoryHandleInventoryClose;
   public static Class<?> inventoryCloseEventReason;

   public static Component[] chatMessageFromString(String message, boolean flag) {
      try {
         return (Component[])chatMessageFromString.invoke(null, message, flag);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static Component chatMessageFromStringOrNull(String message) {
      try {
         return (Component)chatMessageFromStringOrNull.invoke(null, message);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static String chatMessageFromComponent(Component component) {
      try {
         return (String)chatMessageFromComponent.invoke(null, component);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static String chatMessageToJSON(Component component) {
      try {
         return (String)chatMessageToJSON.invoke(null, component);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static Component chatMessageFromJSON(String message) {
      try {
         return (Component)chatMessageFromJSON.invoke(null, message);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static Component chatMessageFromJSONOrString(String message) {
      try {
         return (Component)chatMessageFromJSONOrString.invoke(null, message);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static ItemStack craftStackAsNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
      try {
         return (ItemStack)craftStackAsNMSCopy.invoke(null, itemStack);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static org.bukkit.inventory.ItemStack craftStackAsBukkitCopy(ItemStack itemStack) {
      try {
         return (org.bukkit.inventory.ItemStack)craftStackAsBukkitCopy.invoke(null, itemStack);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static ItemEntity getItemHandle(Object value) {
      try {
         return (ItemEntity)getItemHandle.invoke(value);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static ServerLevel getWorldHandle(Object value) {
      try {
         return (ServerLevel)getWorldHandle.invoke(value);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static ServerPlayer getPlayerHandle(Object value) {
      try {
         return (ServerPlayer)getPlayerHandle.invoke(value);
      } catch (InvocationTargetException | IllegalAccessException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public static void eventFactoryHandleInventoryClose(Player player) {
      try {
         eventFactoryHandleInventoryClose.invoke(null, player, inventoryCloseEventReason.getField("PLUGIN").get(null));
      } catch (InvocationTargetException | NoSuchFieldException | IllegalAccessException exception) {
         exception.printStackTrace();
      }
   }

   static {
      try {
         craftChatMessage = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
         chatMessageFromString = craftChatMessage.getDeclaredMethod("fromString", String.class, boolean.class);
         chatMessageFromString.setAccessible(true);
         chatMessageFromStringOrNull = craftChatMessage.getDeclaredMethod("fromStringOrNull", String.class);
         chatMessageFromStringOrNull.setAccessible(true);
         chatMessageFromComponent = craftChatMessage.getDeclaredMethod("fromComponent", Component.class);
         chatMessageFromComponent.setAccessible(true);
         chatMessageToJSON = craftChatMessage.getDeclaredMethod("toJSON", Component.class);
         chatMessageToJSON.setAccessible(true);
         chatMessageFromJSON = craftChatMessage.getDeclaredMethod("fromJSON", String.class);
         chatMessageFromJSON.setAccessible(true);
         chatMessageFromJSONOrString = craftChatMessage.getDeclaredMethod("fromJSONOrString", String.class);
         chatMessageFromJSONOrString.setAccessible(true);
         craftItemStack = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
         craftStackAsNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
         craftStackAsNMSCopy.setAccessible(true);
         craftStackAsBukkitCopy = craftItemStack.getDeclaredMethod("asBukkitCopy", ItemStack.class);
         craftStackAsBukkitCopy.setAccessible(true);
         craftItem = Class.forName("org.bukkit.craftbukkit.entity.CraftItem");
         getItemHandle = craftItem.getDeclaredMethod("getHandle");
         getItemHandle.setAccessible(true);
         craftWorld = Class.forName("org.bukkit.craftbukkit.CraftWorld");
         getWorldHandle = craftWorld.getDeclaredMethod("getHandle");
         getWorldHandle.setAccessible(true);
         craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
         getPlayerHandle = craftPlayer.getDeclaredMethod("getHandle");
         getPlayerHandle.setAccessible(true);
         craftEventFactory = Class.forName("org.bukkit.craftbukkit.event.CraftEventFactory");
         eventFactoryHandleInventoryClose = craftEventFactory.getDeclaredMethod(
            "handleInventoryCloseEvent", Player.class, Class.forName("org.bukkit.event.inventory.InventoryCloseEvent$Reason")
         );
         eventFactoryHandleInventoryClose.setAccessible(true);
         inventoryCloseEventReason = Class.forName("org.bukkit.event.inventory.InventoryCloseEvent$Reason");
      } catch (NoSuchMethodException | ClassNotFoundException exception) {
         SendMessage.errorMessage("Failed to initialize CraftBukkit class wrapper, this is a bug!");
         exception.printStackTrace();
      }
   }
}
