 /*
  * Copyright (C) 2003 - 2008
  * Computational Intelligence Research Group (CIRG@UP)
  * Department of Computer Science
  * University of Pretoria
  * South Africa
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 package net.sourceforge.cilib.ec.iterationstrategies;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.ListIterator;
 
 import net.sourceforge.cilib.algorithm.population.IterationStrategy;
 import net.sourceforge.cilib.ec.EC;
 import net.sourceforge.cilib.entity.Entity;
 import net.sourceforge.cilib.entity.Topology;
 import net.sourceforge.cilib.entity.comparator.AscendingFitnessComparator;
 import net.sourceforge.cilib.entity.operators.Operator;
 import net.sourceforge.cilib.entity.operators.crossover.CrossoverStrategy;
 import net.sourceforge.cilib.entity.operators.crossover.UniformCrossoverStrategy;
 import net.sourceforge.cilib.entity.operators.general.TopologyLoopingOperator;
 import net.sourceforge.cilib.entity.operators.mutation.GaussianMutationStrategy;
 import net.sourceforge.cilib.entity.operators.mutation.MutationStrategy;
 import net.sourceforge.cilib.entity.topologies.GBestTopology;
 
 /**
  * 
  * @author Gary Pampara
  */
 public class GeneticAlgorithmIterationStrategy extends IterationStrategy<EC> {
 	private static final long serialVersionUID = -2429984051022079804L;
 
 	private CrossoverStrategy crossoverStrategy;
 	private MutationStrategy mutationStrategy;
 
 	/**
 	 * Create an instance of the {@linkplain IterationStrategy}. Default cross-over
 	 * and mutation operators are {@linkplain UniformCrossoverStrategy} and
 	 * {@linkplain GaussianMutationStrategy} respectively.
 	 */
 	public GeneticAlgorithmIterationStrategy() {
 		this.crossoverStrategy = new UniformCrossoverStrategy();
 		this.mutationStrategy = new GaussianMutationStrategy();
 		
 		initialiseOperatorPipeline();
 	}
 
 	/**
 	 * Copy constructor. Create an instance that is a copy of the provided instance.
 	 * @param copy the instance to copy.
 	 */
 	public GeneticAlgorithmIterationStrategy(GeneticAlgorithmIterationStrategy copy) {
 		this.crossoverStrategy = copy.crossoverStrategy.getClone();
 		this.mutationStrategy = copy.mutationStrategy.getClone();
 		
 		this.operatorPipeline = new ArrayList<Operator>();
 		for (Operator operator : copy.operatorPipeline) {
 			this.operatorPipeline.add(operator.getClone());
 		}
 	}
 	
 	/**
 	 * {@inheritDoc}
 	 */
 	public GeneticAlgorithmIterationStrategy getClone() {
 		return new GeneticAlgorithmIterationStrategy(this);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@SuppressWarnings("unchecked")
 	public void performIteration(EC ec) {
 		Topology<Entity> offspring = new GBestTopology<Entity>();
 		
 		for (Operator operator : operatorPipeline) {
 			operator.performOperation(ec.getTopology(), offspring);
 		}
 		
 		// Perform crossover
 		//List<Entity> crossedOver = this.crossoverStrategy.crossover(ec.getTopology());
 				
 		// Perform mutation on offspring
 		//this.mutationStrategy.mutate(crossedOver);
 		
 		// Evaluate the fitness values of the generated offspring
 		for (Entity entity : offspring) {
 			entity.calculateFitness();
 		}
 		
 		// Perform new population selection
 		Topology<Entity> topology = (Topology<Entity>) ec.getTopology();
 		for (Entity entity : offspring) {
 			topology.add(entity);
 		}
 		
 		//this.boundaryConstraint.enforce(entity);
 		
 		Collections.sort(ec.getTopology(), new AscendingFitnessComparator());
 		ListIterator<? extends Entity> i = ec.getTopology().listIterator(ec.getPopulationSize());
 		
 		while (i.hasNext()) {
 			i.next();
 			i.remove();
 		}
 
 		offspring.clear();
 		offspring = null;
 	}
 
 	/**
 	 * Get the currently specified {@linkplain CrossoverStrategy}.
 	 * @return The current {@linkplain CrossoverStrategy}.
 	 */
 	public CrossoverStrategy getCrossoverStrategy() {
 		return crossoverStrategy;
 	}
 
 	/**
 	 * Set the current {@linkplain CrossoverStrategy} and reinitialise the operator pipeline.
 	 * @param crossoverStrategy The {@linkplain CrossoverStrategy} to use.
 	 */
 	public void setCrossoverStrategy(CrossoverStrategy crossoverStrategy) {
 		this.crossoverStrategy = crossoverStrategy;
 		initialiseOperatorPipeline();
 	}
 
 	/**
 	 * Get the currently specified {@linkplain MutationStrategy}.
 	 * @return The current {@linkplain MutationStrategy}.
 	 */
 	public MutationStrategy getMutationStrategy() {
 		return mutationStrategy;
 	}
 
 	/**
 	 * Set the current {@linkplain MutationStrategy} and reinitialise the operator pipeline.
 	 * @param mutationStrategy The {@linkplain MutationStrategy} to use.
 	 */
 	public void setMutationStrategy(MutationStrategy mutationStrategy) {
 		this.mutationStrategy = mutationStrategy;
 		initialiseOperatorPipeline();
 	}
 	
 	/**
 	 * Setup the operator pipeline based on the currently specified {@linkplain CrossoverStrategy}
 	 * and {@linkplain MutationStrategy}.
 	 * 
 	 * The operator is firstly cleared of all elements and then recreated using the current
 	 * {@linkplain Operator} objects.
 	 */
 	private void initialiseOperatorPipeline() {
 		this.operatorPipeline.clear();
 		
 		TopologyLoopingOperator loopingOperator = new TopologyLoopingOperator();
 		loopingOperator.setOperator(this.crossoverStrategy);
 		
 		this.operatorPipeline.add(loopingOperator);
 		this.operatorPipeline.add(this.mutationStrategy);
 	}
 	
 }
