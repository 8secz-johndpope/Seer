 /*
  * Copyright (c) 2012 Amndeep Singh Mann <Amndeep.dev@gmail.com> Please read License.txt for full license information.
  */
 
 package randomartassignment.picture;
 
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.Calendar;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 
 import randomartassignment.expression.ExpressionGeneratorParallel;
 
 /**
  * DriverParallel creates images based off of randomly generated mathematical expressions. It has been designed to work in a parallel fashion.
  * 
  * Do not attempt to run this program without testing if switch statements that work on Strings are allowed (such as Oracle Java SE 7).
  * 
  * The idea for this comes from the Nifty Assignments project hosted by Stanford University, in particular the Random Art project idea of Christopher A. Stone <stone@cs.hmc.edu>. The
  * original conception of this idea belongs to Andrej Bauer <Andrej.Bauer@andrej.com>.
  * 
  * @author Amndeep Singh Mann
  * @version 1.0 20 July 2012
  * @see <a href="http://nifty.stanford.edu/2009/stone-random-art/">Stone's Random Art</a>
  * @see <a href="http://www.random-art.org/">Bauer's Random Art</a>
  */
 
 public final class DriverParallel
 {
 	/**
 	 * An array of expression generators in order to create expressions to represent the color values for the picture.
 	 */
 	private static ExpressionGeneratorParallel[] e;
 
 	/**
 	 * The raw values generated by the expressions - its values are between [-1.0, 1.0].
 	 */
 	private static double[][][] raw;
 
 	/**
 	 * The actual color values when converted from the raw values - its values are between [0, 255].
 	 */
 	private static int[][][] colorvals;
 
 	/**
 	 * Used for showing how much work is left.
 	 */
 	private static volatile int counter;
 
 	/**
 	 * A value used to help determine how long the program has been running.
 	 */
 	private static long start, now;
 
 	/**
 	 * A value used to print out a formatted time.
 	 */
 	private static int milli, sec, min, hr;
 
 	/**
 	 * Used to tell when the program is fully finished.
 	 */
 	private static boolean notFinished;
 
 	/**
 	 * Increments the counter value to tell when a thread has finished its assigned work.
 	 */
 	private static synchronized void increaseCounter()
 	{
 		counter++;
 	}
 
 	/**
 	 * Based on the time difference between the start time of the program and the current time of the program, values are assigned so as to properly display the time.
 	 * 
 	 * @param timeDifference
 	 *             A long consisting of the time difference between current time and the start time of the program. It should not be less than 0.
 	 */
 	private static void setTime(long timeDifference)
 	{
 		milli = (int) (timeDifference / 1000000);
 		hr = milli / 3600000;
 		milli %= 3600000;
 		min = milli / 60000;
 		milli %= 60000;
 		sec = milli / 1000;
 		milli %= 1000;
 	}
 
 	/**
 	 * A method that states which portion of the image is currently being worked on for use in "debugging" output.
 	 * 
 	 * @param matrixNum
 	 *             A number that states which matrix is currently being worked on, since each matrix represents a color, it can be used to determine what is being worked on.
 	 * @param grayscale
 	 *             A boolean stating if the grayscale flag is on.
 	 * @return A string representing which portion of the image is currently being worked on by a thread.
 	 */
 	private static String getColor(int matrixNum, boolean grayscale)
 	{
 		if(grayscale)
 			return "gray";
 		else
 			switch(matrixNum)
 			{
 				case 0:
 					return "red";
 				case 1:
 					return "green";
 				case 2:
 					return "blue";
 				default:
 					throw new Error(
 					          "Unexpected number of matrices - implies either some sort of unconceivable error or the code that applies to matrixNum/numMatrices has been changed without updating this method.");
 			}
 	}
 
 	/**
 	 * Creates an image based off of randomly generated mathematical statements.
 	 * 
 	 * @param args
 	 *             "-d 'int'": Changes the depth. "-w 'int'": Changes the width of the image. "-h 'int'": Changes the height of the image. "-l 'String'": Changes the location where the
 	 *             image is saved (make sure that the directory ends with a /). "-n 'String'": Changes the name of the image (make sure that the name starts with a /). "-c": Makes a color
 	 *             image as opposed to a grayscale one. "-v": Prints out a lot of debugging statements, particularly ones that determine the percentage of the work completed and the total
 	 *             time that the application has been running.
 	 */
 	public static void main(String[] args)
 	{
 		// Stuff for checking out how long it takes for the program to run
 		start = now = System.nanoTime();
 		milli = sec = min = hr = 0;
 
 		// Check for the end
 		notFinished = true;
 
 		// Values that change depending on user preference (determined through commandline args) - pregiven default values
 		int depth = (int) (Math.random() * 10);// maximum depth of recursion
 		int width = 301;// width and height for picture size
 		int height = 301;
 		boolean doingGrayscale = true;// grayscale vs. color
 		int numMatrices = 1;
 		boolean verbose = false;// whether or not to print anything out
 		Calendar calendar = Calendar.getInstance();// helps determine default folder and file naming
 		String pictureLocation = null;
 		boolean hasLocation = false;// hasLocation (given by user)
 		String filename = null;
 		boolean hasFilename = false;// hasFilename (given by user)
 
 		for(int x = 0; x < args.length; x++)
 		{
 			switch(args[x])
 			// This is a (minimum) Java SE 7 feature only (using String in a switch method)
 			{
 				case "-d":
 				{
 					int d;
 					try
 					{
 						d = Integer.parseInt(args[x + 1]);
 					}
 					catch(NumberFormatException ee)
 					{
 						d = depth;
 					}
 					catch(IndexOutOfBoundsException ee)
 					{
 						d = depth;
 					}
 					depth = d;
 					break;
 				}
 				case "-w":
 				{
 					int w;
 					try
 					{
 						w = Integer.parseInt(args[x + 1]);
 					}
 					catch(NumberFormatException ee)
 					{
 						w = width;
 					}
 					catch(IndexOutOfBoundsException ee)
 					{
 						w = width;
 					}
 					width = w;
 					break;
 				}
 				case "-h":
 				{
 					int h;
 					try
 					{
 						h = Integer.parseInt(args[x + 1]);
 					}
 					catch(NumberFormatException ee)
 					{
 						h = height;
 					}
 					catch(IndexOutOfBoundsException ee)
 					{
 						h = height;
 					}
 					height = h;
 					break;
 				}
 				case "-c":
 				{
 					doingGrayscale = false;
 					numMatrices = 3;
 					break;
 				}
 				case "-v":
 				{
 					verbose = true;
 					break;
 				}
 				case "-l":
 				{
 					try
 					{
 						pictureLocation = args[x + 1];
 						hasLocation = true;
 					}
 					catch(IndexOutOfBoundsException ee)
 					{
 						pictureLocation = null;
 						hasLocation = false;
 					}
 					break;
 				}
 				case "-n":
 				{
 					try
 					{
 						filename = args[x + 1];
 						hasFilename = true;
 					}
 					catch(IndexOutOfBoundsException ee)
 					{
 						filename = null;
 						hasFilename = false;
 					}
 					break;
 				}
 				default:
 				{
 					break;
 				}
 			}
 		}
 
 		final double xinterval = 2.0 / (width - 1);// the minus one is so that the end result is [-1,1] and not [-1,1)
 		final double yinterval = 2.0 / (height - 1);
 
 		final int timeinterval = (int) (0.00172 * (width * height) + 0.89912 * Math.sqrt(width * height) + 191.84353);
 		final boolean verbose_ = verbose;// for use in anonymous inner classes without making verbose static
 
 		/* An anonymous inner class that prints out how long the program has been executing. */
 		Thread timer = new Thread(new Runnable()
 			{
 				public void run()
 				{
 					while(notFinished)
 					{
 						try
 						{
 							Thread.sleep(timeinterval);
 						}
 						catch(InterruptedException e)
 						{
 						}
 
 						if(verbose_)
 						{
 							now = System.nanoTime();
 							setTime(now - start);
 
 							System.out.format("TIME: %d:%02d:%02d:%03d\n", hr, min, sec, milli);
 						}
 					}
 				}
 			});
 		timer.start();
 
 		counter = 0;// counter for displaying percentage complete in output, maximum is what it is out of
 		int maximum = height * ((doingGrayscale) ? 1 : 3);
 
 		if(verbose)
 		{
 			System.out.println("Started program and accepted parameters...");
 			System.out.println((doingGrayscale) ? "Making equation..." : "Making equations...");
 		}
 
 		e = new ExpressionGeneratorParallel[numMatrices];
 		for(int a = 0; a < numMatrices; a++)
 			e[a] = new ExpressionGeneratorParallel(depth);
 
 		String[] equation = new String[numMatrices];
 		for(int a = 0; a < numMatrices; a++)
 		{
 			equation[a] = e[a].toString();
 			// commented out because this often takes a really long time to print if they are too large and so they cause everything to crash
 			// if(verbose)
 			// {
 			// System.out.println("This is equation " + a + ": " + equation[a]);
 			// }
 		}
 
 		// upper-level manager for threads
		ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
 
 		// contains the values from [-1.0, 1.0]
 		raw = new double[numMatrices][height][width];
 
 		if(verbose)
 		{
 			System.out.println("Created equations...\nSetting up raw matrix...");
 		}
 
 		for(int k = 0; k < numMatrices; k++)
 		{
 			// stuff for printing out where we are
 			String color = getColor(k, doingGrayscale);
 
 			for(int j = 0; j < height; j++)
 			{
 				if(verbose)
 				{
 					if(j % 100 == 0)
 					{
 						System.out.println("Setting up making of row " + j + " out of " + height + " in the " + color + " raw matrix... | Finished: "
 						          + (int) ((counter / ((double) maximum)) * 100) + "%");
 					}
 				}
 
 				/* An anonymous inner class that finishes working on a row of the image by itself. This one works on evaluating the raw expression. It takes the longest time to finish. */
 				final int w = width, jj = j, kk = k;
 				pool.execute(new Runnable()
 					{
 						public void run()
 						{
 							// converts the expression into a value between [-1.0, 1.0], the intervals represent the "distance" between pixels
 							for(int i = 0; i < w; i++)
 							{
 								raw[kk][jj][i] = e[kk].evaluateExpression(i * xinterval - 1.0, jj * yinterval - 1.0);
 							}
 							increaseCounter();
 						}
 
 					});
 			}
 		}
 
 		if(verbose)
 		{
 			System.out.println("Making raw matrix...");
 		}
 
 		pool.shutdown();// reminds it to send everything off to be executed and then close down
 		while(!pool.isTerminated())// while jobs aren't finished - this is the part of the program that takes the longest
 		{
 			try
 			{
 				Thread.sleep(timeinterval);
 				if(verbose)
 				{
 					now = System.nanoTime();
 					setTime(now - start);
 
 					System.out.println("Still making raw matrix... " + (int) ((counter / ((double) maximum)) * 100) + "%");
 				}
 			}
 			catch(InterruptedException ie)
 			{
 			}
 		}
 
 		counter = 0;// reset for the next set of work
 
 		if(verbose)
 		{
 			System.out.println("Setting up picture matrix...");
 		}
 
 		pool = Executors.newCachedThreadPool();// if problem with too many threads, make it a newFixedThreadPool(int num) - for TJ, num should be 150
 
 		colorvals = new int[numMatrices][height][width];// contains the actual color values
 
 		for(int k = 0; k < numMatrices; k++)
 		{
 			String color = getColor(k, doingGrayscale);
 
 			if(verbose)
 			{
 				System.out.println("Working on the " + color + " matrix...");
 			}
 
 			for(int j = 0; j < height; j++)
 			{
 				if(verbose)
 				{
 					if(j % 100 == 0)
 					{
 						System.out.println("Setting up making row " + j + " out of " + height + " in the " + color + " picture matrix... | Finished: "
 						          + (int) ((counter / ((double) maximum)) * 100) + "%");
 					}
 				}
 
 				/* An anonymous inner class that finishes working on a row of the image by itself. This one works on converting the raw values into actual color values. */
 				final int jj = j;
 				final int kk = k;
 				final int ww = width;
 				pool.execute(new Runnable()
 					{
 						public void run()
 						{
 							for(int i = 0; i < ww; i++)
 							{
 								colorvals[kk][jj][i] = (int) ((raw[kk][jj][i] + 1.0) * 255 / 2.0);
 							}
 							increaseCounter();
 						}
 					});
 			}
 		}
 
 		if(verbose)
 		{
 			System.out.println("Making picture matrix...");
 		}
 
 		pool.shutdown();
 		while(!pool.isTerminated())
 		{
 			try
 			{
 				Thread.sleep(timeinterval);
 				if(verbose)
 				{
 					now = System.nanoTime();
 					setTime(now - start);
 
 					System.out.println("Still making picture matrix... " + (int) ((counter / ((double) maximum)) * 100) + "%");
 				}
 			}
 			catch(InterruptedException ie)
 			{
 			}
 		}
 
 		if(verbose)
 		{
 			System.out.println("Making picture...");
 		}
 
 		/*
 		 * PGM is grayscale, PPM is color - They are filled with human readable values. These are used because of that ease of creation and reading, also, I did not wish to implement a way
 		 * to make a more compact image. Since these files are very large, it is advised that you convert them into something like a JP(E)G.
 		 */
 
 		if(!hasLocation)
 		{
 			pictureLocation = String.format(System.getProperty("user.home") + "/RandomArtAssignmentPictures/%d/%02d/%02d/", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
 			          calendar.get(Calendar.DATE));
 		}
 
 		String filetype = (doingGrayscale) ? ".pgm" : ".ppm";
 		File file = null;
 		try
 		{
 			file = new File(pictureLocation);
 		}
 		catch(NullPointerException ee)
 		{
 			file = new File("~");
 		}
 
 		if(!hasFilename)
 		{
 			filename = String.format("/%02d-%02d-%02d-%03d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
 			          calendar.get(Calendar.MILLISECOND));
 		}
 		file.mkdirs();// make the file location and any folders that might be necessary
 		PrintWriter printer = null;
 		try
 		{
 			printer = new PrintWriter(new BufferedWriter(new FileWriter(file.getPath() + filename + filetype)));
 		}
 		catch(IOException e1)
 		{
 			e1.printStackTrace();
 			System.exit(1);
 		}
 		printer.println((doingGrayscale) ? "P2" : "P3");
 		printer.println(width + " " + height);
 		printer.println(255);
 
 		for(int j = 0; j < height; j++)
 		{
 			if(verbose)
 			{
 				if(j % 100 == 0)
 				{
 					System.out.println("Making row " + j + " out of " + height + " of the picture...");
 				}
 			}
 
 			for(int i = 0; i < width; i++)
 			{
 				for(int k = 0; k < numMatrices; k++)
 				{
 					printer.print(colorvals[k][j][i] + " ");
 				}
 			}
 			printer.println();
 		}
 
 		for(String s : equation)
 		{
 			if(verbose)
 			{
 				System.out.println("Printing an equation at the end of the file...");
 			}
 
 			printer.println("# " + s);
 		}
 
 		printer.flush();
 		printer.close();
 
 		/* Makes sure that the final verbose statement, if verbose is true, is the final amount of code executed. */
 		notFinished = false;
 		try
 		{
 			timer.join();
 		}
 		catch(InterruptedException e1)
 		{
 			e1.printStackTrace();
 		}
 
 		if(verbose)
 		{
 			now = System.nanoTime();
 			setTime(now - start);
 
 			System.out.format("Finished... %d:%02d:%02d:%03d\n", hr, min, sec, milli);
 		}
 	}
 }
