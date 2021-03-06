 package com.malhartech.demos.performance;
 
 /*
  * To change this template, choose Tools | Templates and open the template in the editor.
  */
import com.malhartech.api.Context.OperatorContext;
 import com.malhartech.api.DefaultInputPort;
 import com.malhartech.api.Operator;
 import java.util.ArrayList;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  *
  * @author Chetan Narsude <chetan@malhar-inc.com>
  */
 public class WordCountModule<T> implements Operator
 {
   public final transient DefaultInputPort<T> input = new DefaultInputPort<T>(this)
   {
     @Override
     public void process(T tuple)
     {
       count++;
     }
   };
   transient ArrayList<Integer> counts;
   transient int count;
 
   @Override
   public void endWindow()
   {
     counts.add(count);
     count = 0;
 
     if (counts.size() % 10 == 0) {
       logger.info("counts = {}", counts);
       counts.clear();
     }
   }
 
   @Override
   public void teardown()
   {
     logger.info("counts = {}", counts);
   }
 
   @Override
   public void beginWindow()
   {
   }
 
   @Override
  public void setup(OperatorContext context)
   {
     counts = new ArrayList<Integer>();
   }
 
   private static final long serialVersionUID = 201208061820L;
   private static final Logger logger = LoggerFactory.getLogger(WordCountModule.class);
 }
