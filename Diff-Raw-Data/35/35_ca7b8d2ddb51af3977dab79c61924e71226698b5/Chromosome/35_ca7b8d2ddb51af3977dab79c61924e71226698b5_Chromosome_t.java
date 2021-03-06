 /******************************************************************************
  *                                                                             *
  *  Copyright: (c) Syncleus, Inc.                                              *
  *                                                                             *
  *  You may redistribute and modify this source code under the terms and       *
  *  conditions of the Open Source Community License - Type C version 1.0       *
  *  or any later version as published by Syncleus, Inc. at www.syncleus.com.   *
  *  There should be a copy of the license included with this file. If a copy   *
  *  of the license is not included you are granted no right to distribute or   *
  *  otherwise use this file except through a legal and valid license. You      *
  *  should also contact Syncleus, Inc. at the information below if you cannot  *
  *  find a license:                                                            *
  *                                                                             *
  *  Syncleus, Inc.                                                             *
  *  2604 South 12th Street                                                     *
  *  Philadelphia, PA 19148                                                     *
  *                                                                             *
  ******************************************************************************/
 package com.syncleus.dann.genetics.wavelets;
 
 import com.syncleus.dann.genetics.MutableDouble;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Random;
 import java.util.Set;
 
public class Chromosome implements Cloneable
 {
 	private WaveletChromatid leftChromatid;
 	private WaveletChromatid rightChromatid;
 	private static Random random = new Random();
 
 	private double mutability;
 
 	public Chromosome(Chromosome copy)
 	{
 		this.leftChromatid = copy.leftChromatid.clone();
 		this.rightChromatid = copy.rightChromatid.clone();
 		this.mutability = copy.mutability;
 	}
 
	public boolean bind(SignalKeyConcentration concentration, boolean isExternal)
	{
		return false;
	}

 	protected WaveletChromatid getLeftChromatid()
 	{
 		return this.leftChromatid;
 	}
 
 	protected WaveletChromatid getRightChromatid()
 	{
 		return this.rightChromatid;
 	}
 
 	public List<WaveletGene> getGenes()
 	{
 		List<WaveletGene> genes = new ArrayList<WaveletGene>(this.leftChromatid.getGenes());
 		genes.addAll(this.rightChromatid.getGenes());
 		return Collections.unmodifiableList(genes);
 	}
 
 	public List<PromoterGene> getPromoterGenes()
 	{
 		List<PromoterGene> promoters = new ArrayList<PromoterGene>(this.leftChromatid.getPromoterGenes());
 		promoters.addAll(this.rightChromatid.getPromoterGenes());
 		return Collections.unmodifiableList(promoters);
 	}
 
 	public List<SignalGene> getLocalSignalGenes()
 	{
 		List<SignalGene> localSignalGenes = new ArrayList<SignalGene>(this.leftChromatid.getLocalSignalGenes());
 		localSignalGenes.addAll(this.rightChromatid.getLocalSignalGenes());
 		return Collections.unmodifiableList(localSignalGenes);
 	}
 
 	public List<ExternalSignalGene> getExternalSignalGenes()
 	{
 		List<ExternalSignalGene> externalSignalGenes = new ArrayList<ExternalSignalGene>(this.leftChromatid.getExternalSignalGenes());
 		externalSignalGenes.addAll(this.rightChromatid.getExternalSignalGenes());
 		return Collections.unmodifiableList(externalSignalGenes);
 	}
 
 	private void crossover(double deviation)
 	{
 		//find the crossover position
 		int crossoverPosition;
 		if(random.nextBoolean())
 		{
 			int length = ( this.leftChromatid.getCentromerePosition() < this.rightChromatid.getCentromerePosition() ? this.leftChromatid.getCentromerePosition() : this.rightChromatid.getCentromerePosition());
 
 			int fromEnd = (int) Math.abs( (new MutableDouble(0d)).mutate(deviation).doubleValue() );
 			if(fromEnd > length)
 				crossoverPosition = 0;
 			else
 				crossoverPosition = -1 * (length - fromEnd);
 		}
 		else
 		{
 			int leftLength = this.leftChromatid.getGenes().size() - this.leftChromatid.getCentromerePosition();
 			int rightLength = this.rightChromatid.getGenes().size() - this.leftChromatid.getCentromerePosition();
 
 			int length = ( leftLength < rightLength ? leftLength : rightLength);
 
 			int fromEnd = (int) Math.abs( (new MutableDouble(0d)).mutate(deviation).doubleValue() );
 			if(fromEnd > length)
 				crossoverPosition = 0;
 			else
 				crossoverPosition = (length - fromEnd);
 		}
 
 		//perform the crossover.
 		List<WaveletGene> leftGenes = this.leftChromatid.crossover(crossoverPosition);
 		List<WaveletGene> rightGenes = this.rightChromatid.crossover(crossoverPosition);
 
 		this.leftChromatid.crossover(rightGenes, crossoverPosition);
 		this.rightChromatid.crossover(leftGenes, crossoverPosition);
 	}
 
 	@Override
 	public Chromosome clone()
 	{
 		return new Chromosome(this);
 	}
 
 	public void mutate(Set<Key> keyPool)
 	{
 		this.crossover(this.mutability);
 		this.leftChromatid.mutate(keyPool);
 		this.rightChromatid.mutate(keyPool);
 		this.mutability = Mutation.mutabilityMutation(this.mutability);
 	}
 }
