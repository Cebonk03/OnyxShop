package org.quartz;

public class ObjectAlreadyExistsException extends JobPersistenceException {
   private static final long serialVersionUID = -558301282071659896L;

   public ObjectAlreadyExistsException(String var1) {
      super(var1);
   }

   public ObjectAlreadyExistsException(JobDetail var1) {
      super("Unable to store Job : '" + var1.getKey() + "', because one already exists with this identification.");
   }

   public ObjectAlreadyExistsException(Trigger var1) {
      super(
         "Unable to store Trigger with name: '"
            + var1.getKey().getName()
            + "' and group: '"
            + var1.getKey().getGroup()
            + "', because one already exists with this identification."
      );
   }
}
