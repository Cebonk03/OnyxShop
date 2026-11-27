package com.onyxshop.providers;

import com.onyxshop.OnyxShop;
import com.onyxshop.util.exceptions.PermissionsHookException;

public interface PermissionsProvider {
   void setup(OnyxShop plugin) throws PermissionsHookException;

   String getName();

   String getGroupName(String name);
}
