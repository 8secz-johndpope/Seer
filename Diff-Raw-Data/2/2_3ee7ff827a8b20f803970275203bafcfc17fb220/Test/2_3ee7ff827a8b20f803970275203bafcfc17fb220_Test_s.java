 package main;
 
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 import java.util.Calendar;
 import java.util.List;
 
 import org.encog.ensemble.EnsembleAggregator;
 import org.encog.ensemble.EnsembleMLMethodFactory;
 import org.encog.ensemble.EnsembleTrainFactory;
 import techniques.EvaluationTechnique;
 import helpers.ArgParser;
 import helpers.ArgParser.BadArgument;
 import helpers.DataLoader;
 import helpers.Evaluator;
 import helpers.ChainParams;
 import helpers.ProblemDescription;
 
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.Properties;
 
 public class Test {
 
 	Evaluator ev;
 	static DataLoader dataLoader;
 	static ProblemDescription problem;
 	
 	private static List<Integer> sizes;
 	private static List<Integer> dataSetSizes;
 	private static List<Double> trainingErrors;
 	private static int nFolds;
 	private static double activationThreshold;
 	private static EnsembleTrainFactory etf;
 	private static List<EnsembleMLMethodFactory> mlfs;
 	private static EnsembleAggregator agg;
 	private static String etType;
 	private static boolean verbose;
 	private static double selectionError;
 	
 	private static Connection sqlConnection;
 	
 	public static void loop() throws SQLException
 	{
 		DateFormat sqlDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
 		Calendar cal = Calendar.getInstance();
 		Statement statement = sqlConnection.createStatement();
 		long chainId = cal.getTimeInMillis() * 1000 + (int) Math.random() * 1000;
 		statement.setQueryTimeout(30);
 		statement.executeUpdate("INSERT INTO chains (folds,aggregation,problem,technique,start,ensemble_training,id,invalidated) VALUES (" + nFolds + 
 				                ", '" + agg.getLabel() + "'" + 
 				                ", '" + problem.getLabel() + "'" +
 				                ", '" + etType + "'" +
 				                ", '" + sqlDateFormat.format(cal.getTime()) + "' " +
 				                ", '" + etf.getLabel() + "'" +
 				                ", " + chainId + 
 				                ", 1)" +
 				                ";");
 		for (Integer dataSetSize : dataSetSizes)
 		for (int fold=0; fold < nFolds; fold++)
 		for (EnsembleMLMethodFactory mlf: mlfs)
 		{
 
 			EvaluationTechnique et = null;
 			ChainParams fullLabel = new ChainParams(problem.getLabel(),etType,etf.getLabel(),mlf.getLabel(),agg.getLabel(),dataSetSize);
 			try
 			{
 				et = ArgParser.technique(etType,sizes,dataSetSize,fullLabel,mlf,etf,agg);
 			} catch (BadArgument e)
 			{
 				help();
 			}
 			for (double te: trainingErrors)
 			{
 				Evaluator ev = new Evaluator(et, dataLoader, te, selectionError, verbose,fold);
 				ev.getResults(fullLabel,te,fold,statement,chainId);
 			}
 		}
 		statement.executeUpdate("UPDATE chains SET invalidated = 0, end = '" + sqlDateFormat.format(cal.getTime()) + "' WHERE id = " + chainId);
 	}
 	
 	public static void main(String[] args)
 	{
 		if (args.length != 12) 
 		{
 			help();
 		} 
 		try 
 		{
 			//TODO: a lot of these should be in a properties file
 			etType = args[0];
 			problem = ArgParser.problem(args[1]);
 			sizes = ArgParser.intList(args[2]);
 			dataSetSizes = ArgParser.intList(args[3]);
 			trainingErrors = ArgParser.doubleList(args[4]);
 			nFolds = ArgParser.intSingle(args[5]);
 			dataLoader = problem.getDataLoader(activationThreshold,nFolds);
 		} catch (helpers.ProblemDescriptionLoader.BadArgument e) 
 		{
 			System.err.println("Could not create dataLoader - perhaps the mapper_type property is wrong");
 			e.printStackTrace();
 		} catch (BadArgument e) {
 			help();
 		}
 		try 
 		{
 			Class.forName("com.mysql.jdbc.Driver");
 		} catch (ClassNotFoundException e) 
 		{
			System.err.println("Could not find SQLite JDBC driver!");
 		}
 		try
 		{
             Properties prop = new Properties();
             prop.load(new FileInputStream("config.properties"));
             String dbhost = prop.getProperty("dbhost");
             String dbuser = prop.getProperty("dbuser");
             String dbpass = prop.getProperty("dbpass");
             String dbport = prop.getProperty("dbport");
             String dbname = prop.getProperty("dbname");
             String dbconn = "jdbc:mysql://" + dbhost + ":" + dbport + "/" + dbname;             
 			sqlConnection = DriverManager.getConnection(dbconn, dbuser, dbpass);
 			loop();
 		} catch(SQLException e)
 	    {
 	      System.err.println(e.getMessage());
 	      e.printStackTrace();
 	    } catch (IOException e)
         {
             e.printStackTrace();
         }
 	    finally
 	    {
 	      try
 	      {
 	        if(sqlConnection != null)
 	          sqlConnection.close();
 	      }
 	      catch(SQLException e)
 	      {
 	        System.err.println(e);
 	      }
 	    }
 		System.exit(0);
 	}
 
 	private static void help()
 	{
 		System.err.println("Usage: Test <technique> <problem> <sizes> <dataSetSizes> <trainingErrors> <nFolds> <activationThreshold> <training> <membertypes> <aggregator> <verbose> <selectionError>");
 		System.err.println("nFolds must be > 1");
 		System.exit(2);
 	}
 }
