 package org.jskat.ai.nn.util;
 
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.encog.engine.network.activation.ActivationSigmoid;
 import org.encog.ml.data.MLData;
 import org.encog.ml.data.MLDataPair;
 import org.encog.ml.data.MLDataSet;
 import org.encog.ml.data.basic.BasicMLData;
 import org.encog.ml.data.basic.BasicMLDataPair;
 import org.encog.ml.data.basic.BasicMLDataSet;
 import org.encog.neural.networks.BasicNetwork;
 import org.encog.neural.networks.PersistBasicNetwork;
 import org.encog.neural.networks.layers.BasicLayer;
 import org.encog.neural.networks.training.propagation.Propagation;
 import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
 
 /**
  * Wraps the Encog network to fulfill the interface {@link INeuralNetwork}
  */
 public class EncogNetworkWrapper implements INeuralNetwork {
 
 	private BasicNetwork network;
 	private final PersistBasicNetwork networkPersister;
 	private final Propagation trainer;
 	private MLDataSet trainingSet;
 
 	private final List<MLDataPair> dataPairList = new ArrayList<MLDataPair>();
	private final int MAX_SIZE = 10;
 	private int currentIndex = -1;
 
 	/**
 	 * Constructor
 	 * 
 	 * @param newTopo
 	 *            Network topology
 	 */
 	public EncogNetworkWrapper(final NetworkTopology newTopo) {
 		network = new BasicNetwork();
 		network.addLayer(new BasicLayer(null, true, newTopo.getInputNeuronCount()));
 		for (int i = 0; i < newTopo.getHiddenLayerCount(); i++) {
 			network.addLayer(new BasicLayer(new ActivationSigmoid(), true, newTopo.getHiddenNeuronCount(i)));
 		}
 		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1));
 		network.getStructure().finalizeStructure();
 		network.reset();
 
 		networkPersister = new PersistBasicNetwork();
 
 		trainingSet = new BasicMLDataSet();
 		MLData inputs = new BasicMLData(newTopo.getInputNeuronCount());
 		MLData outputs = new BasicMLData(1);
 		// trainingSet.add(inputs, outputs);
 		trainingSet = new BasicMLDataSet();
 		trainingSet.add(new BasicMLDataPair(inputs, outputs));
 		trainer = new ResilientPropagation(network, trainingSet);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public double getAvgDiff() {
 		return 0.0;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public double adjustWeights(final double[] inputValues, final double[] outputValues) {
 
 		if (dataPairList.size() < MAX_SIZE) {
 			dataPairList.add(new BasicMLDataPair(new BasicMLData(inputValues), new BasicMLData(outputValues)));
 			currentIndex++;
 		} else {
 			currentIndex = (currentIndex + 1) % MAX_SIZE;
 			dataPairList.set(currentIndex, new BasicMLDataPair(new BasicMLData(inputValues), new BasicMLData(
 					outputValues)));
 		}
 
 		trainingSet.close();
 		trainingSet = new BasicMLDataSet(dataPairList);
 
 		trainer.iteration();
 		return trainer.getError();
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void resetNetwork() {
 		network.reset();
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public double getPredictedOutcome(final double[] inputValues) {
 		MLData output = network.compute(new BasicMLData(inputValues));
 		return output.getData(0);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public long getIterations() {
 		return 0;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public boolean saveNetwork(final String fileName) {
 		try {
 			networkPersister.save(new FileOutputStream(fileName), network);
 		} catch (FileNotFoundException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		return true;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void loadNetwork(final String fileName, final int inputNeurons, final int hiddenNeurons,
 			final int outputNeurons) {
 		network = (BasicNetwork) networkPersister.read(getClass().getResourceAsStream(fileName));
 	}
 }
