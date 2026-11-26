package com.onyxshop.versions.paper_v1_21_R6;

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

   public static Component[] chatMessageFromString(String var0, boolean var1) {
      try {
         return (Component[])chatMessageFromString.invoke(null, var0, var1);
      } catch (InvocationTargetException | IllegalAccessException var3) {
         var3.printStackTrace();
         return null;
      }
   }

   public static Component chatMessageFromStringOrNull(String var0) {
      try {
         return (Component)chatMessageFromStringOrNull.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static String chatMessageFromComponent(Component var0) {
      try {
         return (String)chatMessageFromComponent.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static String chatMessageToJSON(Component var0) {
      try {
         return (String)chatMessageToJSON.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static Component chatMessageFromJSON(String var0) {
      try {
         return (Component)chatMessageFromJSON.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static Component chatMessageFromJSONOrString(String var0) {
      try {
         return (Component)chatMessageFromJSONOrString.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static ItemStack craftStackAsNMSCopy(org.bukkit.inventory.ItemStack var0) {
      try {
         return (ItemStack)craftStackAsNMSCopy.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static org.bukkit.inventory.ItemStack craftStackAsBukkitCopy(ItemStack var0) {
      try {
         return (org.bukkit.inventory.ItemStack)craftStackAsBukkitCopy.invoke(null, var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static ItemEntity getItemHandle(Object var0) {
      try {
         return (ItemEntity)getItemHandle.invoke(var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static ServerLevel getWorldHandle(Object var0) {
      try {
         return (ServerLevel)getWorldHandle.invoke(var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static ServerPlayer getPlayerHandle(Object var0) {
      try {
         return (ServerPlayer)getPlayerHandle.invoke(var0);
      } catch (InvocationTargetException | IllegalAccessException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static void eventFactoryHandleInventoryClose(Player var0) {
      try {
         eventFactoryHandleInventoryClose.invoke(null, var0, inventoryCloseEventReason.getField("PLUGIN").get(null));
      } catch (InvocationTargetException | NoSuchFieldException | IllegalAccessException var2) {
         var2.printStackTrace();
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
      } catch (NoSuchMethodException | ClassNotFoundException var1) {
         SendMessage.errorMessage("Failed to initialize CraftBukkit class wrapper, this is a bug!");
         var1.printStackTrace();
      }
   }
}
