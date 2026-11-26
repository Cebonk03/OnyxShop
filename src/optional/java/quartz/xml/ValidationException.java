package org.quartz.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ValidationException extends Exception {
   private static final long serialVersionUID = -1697832087051681357L;
   private Collection<Exception> validationExceptions = new ArrayList<>();

   public ValidationException() {
   }

   public ValidationException(String var1) {
      super(var1);
   }

   public ValidationException(Collection<Exception> var1) {
      this();
      this.validationExceptions = Collections.unmodifiableCollection(this.validationExceptions);
      this.initCause((Throwable)var1.iterator().next());
   }

   public ValidationException(String var1, Collection<Exception> var2) {
      this(var1);
      this.validationExceptions = Collections.unmodifiableCollection(this.validationExceptions);
      this.initCause((Throwable)var2.iterator().next());
   }

   public Collection<Exception> getValidationExceptions() {
      return this.validationExceptions;
   }

   @Override
   public String getMessage() {
      if (this.getValidationExceptions().size() == 0) {
         return super.getMessage();
      } else {
         StringBuffer var1 = new StringBuffer();
         boolean var2 = true;

         for (Exception var4 : this.getValidationExceptions()) {
            if (!var2) {
               var1.append('\n');
               var2 = false;
            }

            var1.append(var4.getMessage());
         }

         return var1.toString();
      }
   }
}
