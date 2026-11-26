package com.onyxshop.events.delayed;

import com.onyxshop.util.exceptions.EconomyLoadException;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;

public interface DelayedItemProvider {
   void load() throws EconomyLoadException, ItemRequirementLoadException;

   String getPath();
}
