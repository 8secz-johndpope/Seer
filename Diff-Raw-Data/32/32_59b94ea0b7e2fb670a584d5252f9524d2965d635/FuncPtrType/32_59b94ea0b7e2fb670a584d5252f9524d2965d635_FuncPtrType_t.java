 //---------------------------------------------------------------------
 //
 //---------------------------------------------------------------------
 class FuncPtrType extends PtrGrpType
 {
     //---------------------------------------------------------------------
     //      Constants
     //---------------------------------------------------------------------
     private static final String FUNCPTR_NAME = "funcptr";
     private static final int FUNCPTR_SIZE    = 4;
 
     //---------------------------------------------------------------------
     //      Constructors
     //---------------------------------------------------------------------
     public FuncPtrType()
     {
        super(FUNCPTR_NAME, FUNCPTR_SIZE, null);
     }
 
     public FuncPtrType(String strName, int size)
     {
        super(strName, size, null);
     }
 
     //---------------------------------------------------------------------
     //      Methods
     //---------------------------------------------------------------------
     public boolean isFuncPtr()
     {
         return true;
     }
 
 }
