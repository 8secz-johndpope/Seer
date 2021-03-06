 /*
  * Copyright (c) 1999-2012, Ecole des Mines de Nantes
  * All rights reserved.
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  *     * Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *     * Redistributions in binary form must reproduce the above copyright
  *       notice, this list of conditions and the following disclaimer in the
  *       documentation and/or other materials provided with the distribution.
  *     * Neither the name of the Ecole des Mines de Nantes nor the
  *       names of its contributors may be used to endorse or promote products
  *       derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package samples;
 
 import choco.kernel.common.util.tools.ArrayUtils;
 import solver.Solver;
 import solver.constraints.binary.Element;
 import solver.constraints.nary.Sum;
 import solver.constraints.nary.alldifferent.AllDifferent;
 import solver.constraints.real.RealConstraint;
 import solver.search.loop.monitors.VoidSearchMonitor;
 import solver.search.strategy.StrategyFactory;
 import solver.variables.IntVar;
 import solver.variables.RealVar;
 import solver.variables.VariableFactory;
 import solver.variables.view.Views;
 
 import java.util.Random;
 
 /**
  * <br/>
  *
  * @author Charles Prud'homme
  * @since 20/07/12
  */
 public class SantaClaude extends AbstractProblem {
 
     int n_gifts = 20;
     int n_kids = 10;
     int max_price = 25;
     int[] gift_price;
 
     IntVar[] kid_gift;
     IntVar[] kid_price;
     IntVar total_cost;
     RealVar average;
 
     @Override
     public void createSolver() {
         solver = new Solver("Santa Claude");
     }
 
     @Override
     public void buildModel() {
         Random rand = new Random(29091981);
         double precision = 1.e-6;
 
         kid_gift = VariableFactory.enumeratedArray("g2k", n_kids, 0, n_gifts, solver);
         kid_price = VariableFactory.enumeratedArray("p2k", n_kids, 0, max_price, solver);
         total_cost = VariableFactory.bounded("total cost", 0, max_price * n_kids, solver);
        average = VariableFactory.real("average", 8.0, 9.0, precision, solver);
 
 
         gift_price = new int[n_gifts];
         for (int i = 0; i < n_gifts; i++) {
             gift_price[i] = rand.nextInt(max_price) + 1;
         }
         solver.post(new AllDifferent(kid_gift, solver));
         for (int i = 0; i < n_kids; i++) {
             solver.post(new Element(kid_price[i], gift_price, kid_gift[i], 0, solver));
         }
         solver.post(Sum.eq(kid_price, total_cost, solver));
 
         RealConstraint ave_cons = new RealConstraint(solver);
         StringBuilder function = new StringBuilder("(");
         function.append('{').append(0).append('}');
         for (int i = 1; i < n_kids; i++) {
             function.append("+{").append(i).append('}');
         }
         function.append(")/").append(n_kids).append("=").append('{').append(n_kids).append('}');
//        function.append(";{").append(n_kids).append("} in [120,129]");
 
         RealVar[] all_vars = ArrayUtils.append(Views.real(kid_price, precision), new RealVar[]{average});
 
         ave_cons.addFunction(function.toString(), all_vars);
//        ave_cons.addFunction("{0} in [120.5,129.5]", average);
//        ave_cons.addFunction("{0} >= 120.5; {0} <= 129.5]", average);
         ave_cons.discretize(kid_price);
         solver.post(ave_cons);
     }
 
     @Override
     public void configureSearch() {
         solver.set(StrategyFactory.random(kid_gift, solver.getEnvironment(), 29091981));
     }
 
     @Override
     public void configureEngine() {
     }
 
     @Override
     public void solve() {
         solver.getSearchLoop().plugSearchMonitor(new VoidSearchMonitor() {
             @Override
             public void onSolution() {
                 if (LOGGER.isInfoEnabled()) {
                     LOGGER.info("*******************");
                     for (int i = 0; i < n_kids; i++) {
                         LOGGER.info("Kids #{} has received the gift #{} at a cost of {} euros",
                                 new Object[]{i, kid_gift[i].getValue(), kid_price[i].getValue()});
                     }
                     LOGGER.info("Total cost: {} euros", total_cost.getValue());
                     LOGGER.info("Average: [{},{}] euros", average.getLB(), average.getUB());
                 }
             }
         });
         solver.findAllSolutions();
     }
 
     @Override
     public void prettyOut() {
     }
 
     public static void main(String[] args) {
         new SantaClaude().execute(args);
     }
 }
