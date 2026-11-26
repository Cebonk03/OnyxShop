package com.onyxshop.util.exceptions;

public class CustomException extends Exception {
   public CustomException(String var1) {
      super(var1);
   }

   @Override
   public String toString() {
      return this.getMessage();
   }
}
