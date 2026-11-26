package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;

public class ItemRequirementLoadException extends CustomException {
   public ItemRequirementLoadException(String var1) {
      super(var1);
   }

   public ItemRequirementLoadException(Translatable var1) {
      super(var1.getLegacy());
   }
}
