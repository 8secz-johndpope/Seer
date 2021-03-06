 /**
  * 
  */
 package com.rathesh.codejam2012.server;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.PrintWriter;
 import java.net.Socket;
 import java.util.List;
 
 import com.google.common.collect.Lists;
 import com.rathesh.codejam2012.server.strategies.EMAStrategy;
 import com.rathesh.codejam2012.server.strategies.LWMAStrategy;
 import com.rathesh.codejam2012.server.strategies.SMAStrategy;
 import com.rathesh.codejam2012.server.strategies.Strategy;
 import com.rathesh.codejam2012.server.strategies.TMAStrategy;
 
 /**
  * @author Alex Bourgeois
  * 
  */
 public class StockExchange implements Runnable {
 
   @Override
   public void run() {
     startStockExchange();
   }
 
   private void startStockExchange() {
     Socket priceSocket = null;
     PrintWriter out = null;
     BufferedReader in = null;
    DataDump dataDump = new DataDump();
 
     // 0. Create strategies and managers
     int slowN = 20, fastN = 5;
     Strategy SMASlow = new SMAStrategy(slowN, MSETServlet.WINDOW_SIZE, false);
     Strategy SMAFast = new SMAStrategy(fastN, MSETServlet.WINDOW_SIZE, true);
     Strategy LWMASlow = new LWMAStrategy(slowN, MSETServlet.WINDOW_SIZE, false);
     Strategy LWMAFast = new LWMAStrategy(fastN, MSETServlet.WINDOW_SIZE, true);
     Strategy EMASlow = new EMAStrategy(slowN, MSETServlet.WINDOW_SIZE, false);
     Strategy EMAFast = new EMAStrategy(fastN, MSETServlet.WINDOW_SIZE, true);
     Strategy TMASlow = new TMAStrategy(slowN, MSETServlet.WINDOW_SIZE, false);
     Strategy TMAFast = new TMAStrategy(fastN, MSETServlet.WINDOW_SIZE, true);
 
     try {
       // Set sockets
       priceSocket = new Socket("localhost", MSETServlet.priceFeedPort);
       MSETServlet.tradeBookingSocket = new Socket("localhost", MSETServlet.tradeBookingPort);
 
       // Get streams
       out = new PrintWriter(priceSocket.getOutputStream(), true);
       in = new BufferedReader(new InputStreamReader(priceSocket.getInputStream()));
       MSETServlet.outTradeBooking = new PrintWriter(
           MSETServlet.tradeBookingSocket.getOutputStream(), true);
       MSETServlet.inTradeBooking = new BufferedReader(new InputStreamReader(
           MSETServlet.tradeBookingSocket.getInputStream()));
 
       // 2. Start price feed with 'H'
       out.println('H');
       out.flush();
       // 3. While still receiving prices (not receiving 'C')
       String token = "";
       char c;
       MSETServlet.dataDump = new DataDump();
       List<Double> prices = Lists.newArrayList();
 
       while ((c = (char) in.read()) != 'C') {
         while (c != '|') {
           token += c;
           c = (char) in.read();
         }
         double price = Double.parseDouble(token);
 
         // TODO take care of WINDOW_SIZE
         prices.add(price);
         // 4. Update strategies which will update managers, Managers will call
         // sendBuy or Sell
         SMASlow.update(price);
         SMAFast.update(price);
         LWMASlow.update(price);
         LWMAFast.update(price);
         EMASlow.update(price);
         EMAFast.update(price);
         TMASlow.update(price);
         TMAFast.update(price);
         // 5. Update clock
         token = "";
         // Ignore the delimiter
         in.read();
 
        dataDump.setPrices(prices);
        dataDump.setSmaSlow(SMASlow.getAverages());
        dataDump.setSmaFast(SMAFast.getAverages());
        dataDump.setLwmaSlow(LWMASlow.getAverages());
        dataDump.setLwmaFast(LWMAFast.getAverages());
        dataDump.setEmaSlow(EMASlow.getAverages());
        dataDump.setEmaFast(EMAFast.getAverages());
        dataDump.setTmaSlow(TMASlow.getAverages());
        dataDump.setTmaFast(TMAFast.getAverages());
         MSETServlet.time++;
       }
     }
     catch (IOException e) {
       // TODO Auto-generated catch block
       e.printStackTrace();
     }
   }
 }
