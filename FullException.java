class FullException extends Exception
{
      public FullException() {}
      public FullException (Throwable cause){
    	  super (cause);
      }
      public FullException (String message, Throwable cause){
    	  super (message, cause);
      }
      public FullException(String message){
    	  super (message);
      }
      
 }

class EmptyException extends Exception
{
      public EmptyException() {}
      public EmptyException (Throwable cause){
    	  super (cause);
      }
      public EmptyException (String message, Throwable cause){
    	  super (message, cause);
      }
      public EmptyException(String message){
    	  super (message);
      }
      
 }