 
 class TypeConversion
 {
     public static final int test_1__float_to_int = 1;
     
     public static int float_to_int(float a)
     {
 	return (int)a;
     }
     
     public static final int test_1__double_to_int = 1;
     
     public static int double_to_int(double a)
     {
 	return (int)a;
     }
     
     public static final float test_1__int_to_float = 1.0F;
     public static final float test_255__int_to_float = 255.0F;
     public static final float test_50225__int_to_float = 50225.0F;
     
     public static float int_to_float(int a)
     {
 	return (float)a;
     }
     
     public static final double test_1__int_to_double = 1.0;
     public static final double test_255__int_to_double = 255.0;
     public static final double test_50225__int_to_double = 50225.0;
     
     public static double int_to_double(int a)
     {
 	return (double)a;
     }
     
     private TypeConversion()
     {
     }
 }
