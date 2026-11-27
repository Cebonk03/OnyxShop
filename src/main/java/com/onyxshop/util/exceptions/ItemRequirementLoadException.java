package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;

public class ItemRequirementLoadException extends CustomException {
   public ItemRequirementLoadException(String itemId) {
      super(itemId);
   }

   public ItemRequirementLoadException(Translatable text) {
      super(text.getLegacy());
   }
}
