 /* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
    modeler, Finit element mesher, Plugin architecture.
 
     Copyright (C) 2005
                   Jerome Robert <jeromerobert@users.sourceforge.net>
 
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
 
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
 
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */
 
 package org.jcae.mesh.amibe.validation;
 
 import gnu.trove.TFloatArrayList;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.BufferedOutputStream;
 import java.io.PrintStream;
 import org.apache.log4j.Logger;
 
 /**
  * Class to handle quality results.
  *
  * This class allows easy computation of mesh quality.  A criterion
  * factor can be selected, then quality is computed and results are
  * printed on screen or in files.
  *
  * Example:
  * <pre>
  *    QualityFloat data = new QualityFloat();
  *    data.setQualityProcedure(new MinAngleFace());
  *    for (Iterator itf = mesh3D.getFacesIterator(); itf.hasNext(); )
  *        data.compute(itf.next());
  *    //  Print all results in the BB mesh format.
  *    data.printMeshBB("foo.bb");
  *    //  Gather results into 10 blocks...
  *    data.split(10);
  *    //  ... and display them on screen.
  *    data.printLayers();
  * </pre>
  */
 
 public class QualityFloat
 {
 	private static Logger logger=Logger.getLogger(QualityFloat.class);
 	private TFloatArrayList data;
 	private QualityProcedure qproc;
 	private int [] sorted;
 	private int layers = 0;
 	private float vmin, vmax;
 	private float qmin, qmax, qavg;
 
 	public QualityFloat()
 	{
 		data = new TFloatArrayList();
 	}
 	
 	public QualityFloat(int n)
 	{
 		data = new TFloatArrayList(n);
 	}
 	
 	public void setQualityProcedure(QualityProcedure q)
 	{
 		qproc = q;
 		qproc.bindResult(data);
 	}
 	
 	public void compute(Object x)
 	{
 		assert qproc != null;
 		data.add(qproc.quality(x));
 	}
 	
 	public void add(float x)
 	{
 		data.add(x);
 	}
 	
 	public void finish()
 	{
 		qproc.finish();
 	}
 	
 	public int size()
 	{
 		return data.size();
 	}
 	
 	public void setTarget(float factor)
 	{
 		int nrTotal = data.size();
 		if (factor == 0.0f)
 			return;
 		else
 			factor = 1.0f/factor;
 		for (int i = 0; i < nrTotal; i++)
 		{
 			float val = data.get(i);
 			val *= factor;
 			data.set(i, val);
 		}
 	}
 	
 	public void split(int n)
 	{
 		layers = n;
 		if (layers <= 0)
 			return;
 		int nrTotal = data.size();
 		//  min() and max() methods are buggy in trove 1.0.2
 		vmin = Float.MAX_VALUE;
 		vmax = Float.MIN_VALUE;
 		qavg = 0.0f;
 		for (int i = 0; i < nrTotal; i++)
 		{
 			float val = data.get(i);
 			qavg += val / nrTotal;
 			if (vmin > val)
 				vmin = val;
 			if (vmax < val)
 				vmax = val;
 		}
 		qmin = vmin;
 		qmax = vmax;
 		float delta = (vmax - vmin) / ((float) layers);
 		// sorted[0]: number of points with value < vmin
 		// sorted[layers+1]: number of points with value > vmax
 		sorted = new int[layers+2];
 		for (int i = 0; i < layers + 2; i++)
 			sorted[i] = 0;
 		for (int i = 0; i < nrTotal; i++)
 		{
 			float val = data.get(i);
			int cell = (int) ((val - vmin) / delta + 1.001f);
 			if (cell < 0)
 				cell = 0;
 			else if (cell >= layers + 1)
 			{
 				if (val > vmax)
 					cell = layers + 1;
 				else
 					cell = layers;
 			}
 			sorted[cell]++;
 		}
 	}
 	
 	public void split(float v1, float v2, int n)
 	{
 		layers = n;
 		vmin = v1;
 		vmax = v2;
 		qavg = 0.0f;
 		qmin = Float.MAX_VALUE;
 		qmax = Float.MIN_VALUE;
 		if (layers <= 0)
 			return;
 		int nrTotal = data.size();
 		//  The last cell is for v >= vmax
		float delta = (vmax - vmin) / ((float) layers);
 		sorted = new int[layers+2];
 		for (int i = 0; i < layers+2; i++)
 			sorted[i] = 0;
 		for (int i = 0; i < nrTotal; i++)
 		{
 			float val = data.get(i);
 			if (qmin > val)
 				qmin = val;
 			if (qmax < val)
 				qmax = val;
 			qavg += data.get(i) / nrTotal;
			int cell = (int) ((val - vmin) / delta + 1.001f);
 			if (cell < 0)
 				cell = 0;
 			else if (cell >= layers + 1)
 			{
 				if (val > vmax)
 					cell = layers + 1;
 				else
 					cell = layers;
 			}
 			sorted[cell]++;
 		}
 	}
 	
 	public void printLayers()
 	{
 		if (layers <= 0)
 		{
 			logger.error("split() method must be called before printLayers()");
 			return;
 		}
 		int nrTotal = data.size();
 		float delta = (vmax - vmin) / ((float) layers);
 		if (sorted[0] > 0)
 			System.out.println(" < "+vmin+" "+sorted[0]+" ("+(((float) 100.0 * sorted[0])/((float) nrTotal))+"%)");
 		for (int i = 0; i < layers; i++)
 		{
 			System.out.println(""+(vmin+i*delta)+" ; "+(vmin+(i+1)*delta)+" "+sorted[i+1]+" ("+(((float) 100.0 * sorted[i+1])/((float) nrTotal))+"%)");
 		}
 		if (sorted[layers+1] > 0)
 			System.out.println(" > "+vmax+" "+sorted[layers+1]+" ("+(((float) 100.0 * sorted[layers+1])/((float) nrTotal))+"%)");
 		System.out.println("total: "+nrTotal);
 		System.out.println("qmin: "+qmin);
 		System.out.println("qmax: "+qmax);
 		System.out.println("qavg: "+qavg);
 	}
 	
 	public void printMeshBB(String file)
 	{
 		int nrTotal = data.size();
 		try
 		{
 			PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
 			out.println("3 1 "+nrTotal+" "+qproc.getType());
 			for (int i = 0; i < nrTotal; i++)
 				out.println(""+data.get(i));
 			out.close();
 		}
 		catch (FileNotFoundException ex)
 		{
 			logger.error("Cannot write into: "+file);
 		}
 	}
 }
