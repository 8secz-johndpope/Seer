 import aterm.*;
 import aterm.pure.*;
 
 import java.util.*;
 
 public class TestFib {
 
   private ATermFactory factory;
   
   private AFun zero, suc, plus,fib;
   private ATermAppl tzero;
 
 
   public TestFib(ATermFactory factory) {
     this.factory = factory;
 
     zero = factory.makeAFun("zero", 0, false);
     suc  = factory.makeAFun("suc" , 1, false);
     plus = factory.makeAFun("plus", 2, false);
     fib  = factory.makeAFun("fib" , 1, false);
     tzero = factory.makeAppl(zero);
   }
   
   public void test1() {
    System.out.println("test 1");
 
     ATermAppl res = normalizePlus(
       factory.makeAppl(plus,
       factory.makeAppl(suc,factory.makeAppl(suc,tzero)),
       factory.makeAppl(suc,factory.makeAppl(suc,tzero))));
     
    System.out.println("res = 2+2 = " + res);
   }
 
   public void test2() {
    System.out.println("test 2");
     ATermAppl res = normalizeFib(
       factory.makeAppl(fib,
       factory.makeAppl(suc,
       factory.makeAppl(suc,
       factory.makeAppl(suc,
       factory.makeAppl(suc,tzero))))));
 
    System.out.println("res = fib(4) = " + res);
   }
 
   public void test3(int n) {
    System.out.println("test 3");
         
     ATermAppl N = tzero;
     for(int i=0 ; i<n ; i++) {
       N = factory.makeAppl(suc,N);
     }
       //System.out.println("N = " + N);
     
     long start   = System.currentTimeMillis();
     ATermAppl res = normalizeFib(factory.makeAppl(fib,N));
     long end   = System.currentTimeMillis();
       //System.out.println("fib(" + n + ") = " + res);
    System.out.println("fib(" + n + ") in " + (end-start) + " ms");
 
       //System.out.println(factory);
   }
     
   public ATermAppl normalizePlus(ATermAppl t) {
     boolean memo = false;
     ATermAppl res = t;
     while(true) {
       ATermAppl v0 = (ATermAppl)res.getArgument(0);
 
         // plus(s(s(s(s(s(x))))),y) => plus(x,s(s(s(s(s(y))))))
       if(v0.getAFun() == suc) {
         ATermAppl v1 = (ATermAppl)v0.getArgument(0);
         if(v1.getAFun() == suc) {
           ATermAppl v2 = (ATermAppl)v1.getArgument(0);
           if(v2.getAFun() == suc) {
             ATermAppl v3 = (ATermAppl)v2.getArgument(0);
             if(v3.getAFun() == suc) {
               ATermAppl v4 = (ATermAppl)v3.getArgument(0);
               if(v4.getAFun() == suc) {
                 res = factory.makeAppl(plus,
                                       v4.getArgument(0),
                                       factory.makeAppl(suc,
                                       factory.makeAppl(suc,
                                       factory.makeAppl(suc,
                                       factory.makeAppl(suc,
                                       factory.makeAppl(suc,
                                       res.getArgument(1)))))));
                 continue;
               }
             }
           }
         }
       }
 
         // plus(0,x) = x
       if(v0.getAFun() == zero) {
         res = (ATermAppl)res.getArgument(1);
         break;
       }
 
         // plus(s(x),y) => plus(x,s(y))
       if(v0.getAFun() == suc) {
         res = factory.makeAppl(plus,
                               v0.getArgument(0),
                               factory.makeAppl(suc,res.getArgument(1)));
         continue;
       }
       break;
     }
     return res;
    }
     
   public ATermAppl normalizeFib(ATermAppl t) {
     boolean memo = false;
     
     ATermAppl res = t;
     while(true) {
         // fib(0) = suc(0)
       ATermAppl v0 = (ATermAppl)res.getArgument(0);
       if(v0.getAFun() == zero) {
         res = factory.makeAppl(suc,v0);
         break;
       }
         // fib(suc(0)) => suc(0)
       if(v0.getAFun() == suc) {
         ATermAppl v1 = (ATermAppl)v0.getArgument(0);
         if(v1.getAFun() == zero) {
           res = v0;
           break;
         }
       }
         //  fib(s(s(x))) => plus(fib(x),fib(s(x)))
         //     v0 v1
       if(v0.getAFun() == suc) {
         ATermAppl v1 = (ATermAppl)v0.getArgument(0);
         if(v1.getAFun() == suc) {
           ATermAppl v2 = (ATermAppl)v1.getArgument(0);
           ATermAppl fib1 = normalizeFib(factory.makeAppl(fib,v2));
           ATermAppl fib2 = normalizeFib(factory.makeAppl(fib,v1));
             //System.out.println("adding");
           res = normalizePlus(factory.makeAppl(plus,fib1,fib2));
           break;
         }
       }
       break;
     }
     return res;
   }
    
  
 }
