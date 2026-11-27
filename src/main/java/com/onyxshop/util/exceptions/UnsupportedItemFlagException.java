package com.onyxshop.util.exceptions;

public class UnsupportedItemFlagException extends CustomException {
   public UnsupportedItemFlagException(String itemId) {
      super(itemId);
   }
}
