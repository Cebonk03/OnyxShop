package com.onyxshop.util.exceptions;

public class ItemProviderLoadException extends CustomException {
   public ItemProviderLoadException(String itemId) {
      super(itemId);
   }
}
