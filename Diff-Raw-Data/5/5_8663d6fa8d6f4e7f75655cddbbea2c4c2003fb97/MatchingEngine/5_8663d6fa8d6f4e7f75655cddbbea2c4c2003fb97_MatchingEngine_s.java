 package edu.virginia.jinsup;
 
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.Random;
 
 /**
  * Class that handles order creation, modification, and cancellation. Also deals
  * with trades and provides agents with appropriate trade data from the last
  * millisecond of trading.
  */
 
 public class MatchingEngine {
 
   /**
    * Maximum size the log buffer will reach before writing
    */
   private static final int LOG_BUFFER_SIZE = 524288;
 
   /**
    * Number of milliseconds to calculate moving average
    */
   private static final int MOVING_AVERAGE_LENGTH = 500;
 
   /**
    * All the orders in the simulation, grouped by agent ID.
    */
   private final HashMap<Long, ArrayList<Order>> orderMap;
 
   /**
    * All the agents in the simulation, indexed by agent ID.
    */
   private final HashMap<Long, Agent> agentMap;
 
   /**
    * All the buy orders in the simulation, unsorted.
    */
   private final ArrayList<Order> buyOrders;
 
   /**
    * All the sell orders in the simulation, unsorted.
    */
   private final ArrayList<Order> sellOrders;
 
   /**
    * The volume of shares sold that were initiated by an aggressive buying agent
    * in the last millisecond of trading.
    * 
    */
   private int lastAgVolumeBuySide;
 
   /**
    * The volumes of shares sold that were initiated by an aggressive selling
    * agent in the last millisecond of trading.
    */
   private int lastAgVolumeSellSide;
 
   /**
    * The current (per ms) volume of shares that were initiated by an aggressive
    * buying agent in the last millisecond of trading.
    */
   private int currAgVolumeBuySide;
 
   /**
    * The current (per ms) volume of shares that were initiated by an aggressive
    * selling agent in the last millisecond of trading.
    */
   private int currAgVolumeSellSide;
 
   /**
    * The price (CENTS) that the share was last traded at. This should be plotted
    * every time it is updated (i.e. whenever a trade occurs).
    */
   private int lastTradePrice;
 
   /**
    * Remains true while the simulator is still in the starting period. This
    * means that all orders that may result in a trade will be cancelled.
    */
   private boolean startingPeriod;
 
   /**
    * The buy price (CENTS) for a share, specified by the user.
    */
   private final int buyPrice;
 
   /**
    * A temporary buffer in memory used to keep logging information so that the
    * program does not have write to the log file every time an order is made,
    * traded, etc. Writes are made to the log file either when the simulator is
    * done or when the buffer is full.
    */
   private final ArrayList<String> logBuffer;
 
   /**
    * Queue containing the midpoints of best bid and best ask prices. The length
    * of the list is determined by the get moving average method.
    */
   private final LinkedList<Integer> midpoints;
 
   /**
    * Stores the moving sum, which is used to efficiently calculate moving the
    * average of the best bid and best ask prices.
    */
   private int movingSum;
 
   private final Random randomElementGenerator;
 
   /**
    * Creates a matching engine with empty fields. Everything is initialized to
    * zero. Also initializes the log file.
    * 
    * @param buyPrice
    *          The price (CENTS) that orders should be centered around.
    */
   public MatchingEngine(int buyPrice) {
     orderMap = new HashMap<Long, ArrayList<Order>>();
     buyOrders = new ArrayList<Order>();
     sellOrders = new ArrayList<Order>();
     agentMap = new HashMap<Long, Agent>();
     midpoints = new LinkedList<Integer>();
     lastAgVolumeBuySide = 0;
     lastAgVolumeSellSide = 0;
     lastTradePrice = 0;
     startingPeriod = true;
     movingSum = 0;
     randomElementGenerator = new Random();
     this.buyPrice = buyPrice;
 
     // 2^19 lines before writing to file
     logBuffer = new ArrayList<String>(LOG_BUFFER_SIZE);
 
     // create the CSV file
     try {
       FileWriter writer = new FileWriter(Controller.graphFrame.getDest());
       writer
         .append("Agent ID, Message, Buy/Sell, Order ID, Original Quantity, Price, Type, Leaves Quantity, Trade Price, Aggressor, Trade Match ID\n");
       writer.flush();
       writer.close();
     } catch (IOException e) {
       // TODO Auto-generated catch block
       e.printStackTrace();
     }
   }
 
   /**
    * Deletes an order from the simulation (orderMap and allOrders).
    * 
    * @param order
    *          The order to be removed.
    * @param agentID
    *          ID of the agent whose order is to be removed.
    */
   public void cancelOrder(Order order, boolean market) {
     buyOrders.remove(order);
     sellOrders.remove(order);
     orderMap.get(order.getCreatorID()).remove(order);
     logOrder(order, 3, market, -order.getCurrentQuant(), 0);
     order.setQuant(0);
   }
 
   /**
    * Inserts the agent into the MatchingEngine's agentMap so that it can keep
    * track of it. This is called every time a new agent is constructed so it
    * should not have to be explicitly called.
    * 
    * @param id
    *          ID of the agent.
    * @param agent
    *          Agent object to be added.
    */
   public void addNewAgent(long id, Agent agent) {
     agentMap.put(id, agent);
   }
 
   /**
    * Takes a newly created order and stores it in the MatchingEngine's allOrders
    * and orderMap so that it can keep track of it. Also checks if any trade
    * occurs from this newly created order.
    * 
    * @param order
    *          New order to be inserted into the MatchingEngine.
    * @param agentID
    *          ID of the agent that initiated the order.
    */
   public boolean createOrder(Order order, boolean market) {
     if (order.isBuyOrder()) {
       buyOrders.add(order);
     } else {
       sellOrders.add(order);
     }
     if (orderMap.containsKey(order.getCreatorID())) {
       orderMap.get(order.getCreatorID()).add(order);
     } else {
       ArrayList<Order> orderList = new ArrayList<Order>();
       orderList.add(order);
       orderMap.put(order.getCreatorID(), orderList);
     }
     // log the action.
     // must then check if a trade can occur
 
     if (!market) {
       logOrder(order, 1, false, 0, 0);
       return trade(order, willTrade(order));
     }
     logOrder(order, 1, true, 0, 0);
     return false;
   }
 
   /**
    * Trades market orders only. If there are not orders to satisfy the market
    * order, then the order is cancelled.
    * 
    * @param order
    *          The market order to be traded.
    */
   public void tradeMarketOrder(Order order) {
     // have to add the order to the orderMap and all orders, otherwise the
     // trade method will not work.
     createOrder(order, true);
     if (startingPeriod) {
       cancelOrder(order, true);
       return;
     }
     // save price for logging at the end.
     int price = 0;
 
     long aggressorID = order.getCreatorID();
     int totalVolumeTraded = 0;
     int quantityToRid = order.getCurrentQuant();
     if (order.isBuyOrder()) {
       ArrayList<Order> topSells = topSellOrders();
       if (topSells.isEmpty()) {
         // nothing was traded. order will be cancelled.
         cancelOrder(order, true);
         return;
       }
       while (!topSells.isEmpty()) {
         price = topSells.get(0).getPrice();
         int currentVolumeTraded = trade(order, topSells.get(0));
         totalVolumeTraded += currentVolumeTraded;
 
         // notify the non aggressor.
         agentMap.get(topSells.get(0).getCreatorID()).setLastOrderTraded(true,
          currentVolumeTraded);
 
         if (totalVolumeTraded < quantityToRid) {
           topSells.remove(0);
         } else {
           break;
         }
       }
       // notify the aggressor
       agentMap.get(aggressorID).setLastOrderTraded(true, totalVolumeTraded);
       lastAgVolumeBuySide += totalVolumeTraded;
     } else {
       ArrayList<Order> topBuys = topBuyOrders();
 
       if (topBuys.isEmpty()) {
         // nothing was traded. order will be cancelled.
         cancelOrder(order, true);
         return;
       }
       while (!topBuys.isEmpty()) {
         price = topBuys.get(0).getPrice();
         int currentVolumeTraded = trade(order, topBuys.get(0));
         totalVolumeTraded += currentVolumeTraded;
 
         // notify the non aggressor.
         agentMap.get(topBuys.get(0).getCreatorID()).setLastOrderTraded(true,
           currentVolumeTraded);
 
         if (totalVolumeTraded < quantityToRid) {
           topBuys.remove(0);
         } else {
           break;
         }
       }
       // notify the aggressor
      agentMap.get(aggressorID).setLastOrderTraded(true, totalVolumeTraded);
       lastAgVolumeSellSide += totalVolumeTraded;
     }
     // System.out.print("Market ORDER ");
     lastTradePrice = price;
     logTrade(order, price, totalVolumeTraded);
     logAggressiveTrader(order, true, price, totalVolumeTraded);
 
   }
 
   /**
    * Method that performs the actual trading for the tradeMarketOrder and
    * trade(Order, ArrayList) method. Takes care of filling the correct
    * quantities during a trade.
    * 
    * @param order1
    *          Order of the aggressive agent.
    * @param order2
    *          Order of the passive agent.
    * @return The volume that was traded between the two orders.
    */
   private int trade(Order order1, Order order2) {
     // save price for logging at the end.
     int price = order2.getPrice();
     lastTradePrice = price;
 
     int volumeTraded;
     if (order1.getCurrentQuant() == order2.getCurrentQuant()) {
       volumeTraded = order1.getCurrentQuant();
       buyOrders.remove(order1);
       sellOrders.remove(order1);
       buyOrders.remove(order2);
       sellOrders.remove(order2);
       order1.setQuant(0);
       order2.setQuant(0);
       orderMap.get(order1.getCreatorID()).remove(order1);
       orderMap.get(order2.getCreatorID()).remove(order2);
     } else if (order1.getCurrentQuant() > order2.getCurrentQuant()) {
       // delete orderToTrade, decrease quantity of o
       volumeTraded = order1.getCurrentQuant() - order2.getCurrentQuant();
       order1.setQuant(volumeTraded);
       buyOrders.remove(order2);
       sellOrders.remove(order2);
       order2.setQuant(0);
       orderMap.get(order2.getCreatorID()).remove(order2);
     } else {
       volumeTraded = order2.getCurrentQuant() - order1.getCurrentQuant();
       order2.setQuant(volumeTraded);
       buyOrders.remove(order1);
       sellOrders.remove(order1);
       order1.setQuant(0);
       orderMap.get(order1.getCreatorID()).remove(order1);
     }
 
     // NO LOGGING HERE! LOGGING IS DONE IN THE OTHER TWO TRADE METHODS!
     // except for logging extras....
 
     logExtraTrades(order2, price, volumeTraded);
 
     return volumeTraded;
   }
 
   /**
    * Modifies the order in the MatchingEngine's allOrders and orderMap. Also
    * checks if any trade occurs from this modification.
    * 
    * @param order
    *          Order being modified.
    * @param newPrice
    *          The new price the order should have.
    * @param newQuant
    *          The new quantity the order should have.
    */
   public boolean modifyOrder(Order order, int newPrice, int newQuant) {
     int priceDiff = newPrice - order.getPrice();
     int quantDiff = newQuant - order.getCurrentQuant();
     order.setPrice(newPrice);
     order.setQuant(newQuant);
     // log the action
     logOrder(order, 2, false, quantDiff, priceDiff);
     // must then check if a trade can occur
     return trade(order, willTrade(order));
   }
 
   /**
    * @return The last price that a share was traded for. Used primarily as data
    *         for the bar chart.
    */
   public int getLastTradePrice() {
     return lastTradePrice;
   }
 
   /**
    * 
    * @return The top ten pending buy orders sorted by price (highest) and time
    *         of order creation (most recent).
    */
   public ArrayList<Order> topBuyOrders() {
     ArrayList<Order> topBuyOrders = new ArrayList<Order>();
     for (Order o : buyOrders) {
       if (topBuyOrders.size() < 10) {
         topBuyOrders.add(o);
         Collections.sort(topBuyOrders, Order.highestFirstComparator);
       } else if (o.compare(o, topBuyOrders.get(9)) < 0) {
         topBuyOrders.set(9, o);
         Collections.sort(topBuyOrders, Order.highestFirstComparator);
       }
     }
     return topBuyOrders;
   }
 
   /**
    * @return The top ten sell orders sorted by price (lowest) and time of order
    *         creation (most recent).
    */
   public ArrayList<Order> topSellOrders() {
     ArrayList<Order> topSellOrders = new ArrayList<Order>();
     for (Order a : sellOrders) {
       if (topSellOrders.size() < 10) {
         topSellOrders.add(a);
         Collections.sort(topSellOrders, Order.lowestFirstComparator);
       } else if (Order.lowestFirstComparator.compare(a, topSellOrders.get(9)) < 0) {
         topSellOrders.set(9, a);
         Collections.sort(topSellOrders, Order.lowestFirstComparator);
       }
     }
     return topSellOrders;
   }
 
   /**
    * Checks if an order will make cause a trade.
    * 
    * @param order
    *          The order to check for a trade
    * @return Orders that have the same price, if a trade can be made. Otherwise,
    *         null.
    */
   public ArrayList<Order> willTrade(Order order) {
     ArrayList<Order> samePrice = new ArrayList<Order>();
     if (order.isBuyOrder()) {
       // check for sell orders at the same sell price
       // must be sure to pick orders that were placed first.
       for (Order o : sellOrders) {
         if (o.getPrice() == order.getPrice()) {
           samePrice.add(o);
         }
       }
     } else {
       for (Order o : buyOrders) {
         if (o.getPrice() == order.getPrice()) {
           samePrice.add(o);
         }
       }
     }
     return (samePrice.isEmpty()) ? null : samePrice;
   }
 
   /**
    * Initializes trading for limit orders. If samePricedOrders is null, i.e. the
    * order will not make a trade, then the function simply exits and no logging
    * is done.
    * 
    * @param order
    *          The order to be traded.
    * @param samePricedOrders
    *          Orders having the same price as the order to be traded.
    * 
    * @return True if the trade was made.
    */
   public boolean trade(Order order, ArrayList<Order> samePricedOrders) {
     if (samePricedOrders == null) {
       return false;
     }
 
     // save price for logging below
     int price = order.getPrice();
 
     if (startingPeriod) {
       cancelOrder(order, true);
       return false;
     }
 
     lastTradePrice = samePricedOrders.get(0).getPrice();
 
     // now select the orders that were made first.
     Collections.sort(samePricedOrders, Order.highestFirstComparator);
 
     Order orderToTrade = samePricedOrders.get(0);
 
     int volumeTraded = trade(order, orderToTrade);
 
     // log the action and ID of trade, with System.currentTimeMillis()
     // and volume traded.
     // notify both agents that a trade has occurred.
 
     if (order.isBuyOrder()) {
       lastAgVolumeBuySide += volumeTraded;
     } else {
       lastAgVolumeSellSide += volumeTraded;
     }
     // now get the agents (aggressor and non-aggressor) and notify them.
     int aggressorTrades;
     int passiveTrades;
     if (order.isBuyOrder()) {
       lastAgVolumeBuySide += volumeTraded;
       aggressorTrades = volumeTraded;
       passiveTrades = -volumeTraded;
     } else {
       lastAgVolumeSellSide += volumeTraded;
       aggressorTrades = -volumeTraded;
       passiveTrades = volumeTraded;
     }
     // now get the agents (aggressor and non-aggressor) and notify them.
     agentMap.get(order.getCreatorID())
       .setLastOrderTraded(true, aggressorTrades);
     agentMap.get(orderToTrade.getCreatorID()).setLastOrderTraded(true,
       passiveTrades);
 
     // System.out.print("LIMIT ORDER ");
     logTrade(order, price, volumeTraded);
     logAggressiveTrader(order, false, price, volumeTraded);
     return true;
   }
 
   /**
    * @return The order with the highest bid price.
    */
   public Order getBestBid() {
     if (buyOrders.isEmpty()) {
       return null;
     } else {
       return Collections.min(buyOrders, Order.highestFirstComparator);
     }
   }
 
   /**
    * @return The order with the lowest ask price.
    */
   public Order getBestAsk() {
     if (sellOrders.isEmpty()) {
       return null;
     } else {
       return Collections.max(sellOrders, Order.highestFirstComparator);
     }
   }
 
   /**
    * @return All orders in the simulation.
    */
   // public ArrayList<Order> getAllOrders() {
   // return allOrders;
   // }
 
   /**
    * Sets the matching to allow or disallow trades to occur based on whether or
    * not the simulation is still running during the startup period.
    * 
    * @param isStartingPeriod
    *          If true, then the simulation will remain under startup mode and
    *          trades will not be allowed. Otherwise, trades will be enabled.
    */
   public void setStartingPeriod(boolean isStartingPeriod) {
     startingPeriod = isStartingPeriod;
   }
 
   /**
    * @return The buy price for a share.
    */
   public int getBuyPrice() {
     return buyPrice;
   }
 
   /**
    * Stores lastAgVolumeBuySide and lastAgVolumeSellSideResets for the previous
    * millisecond of trading and resets currAgVolumeBuySide and
    * currAgVolumeSellSide.
    */
   public void reset() {
     lastAgVolumeBuySide = currAgVolumeBuySide;
     lastAgVolumeSellSide = currAgVolumeSellSide;
     currAgVolumeBuySide = 0;
     currAgVolumeSellSide = 0;
   }
 
   /**
    * A special method for opportunistic traders that returns the moving average
    * for the last MOVING_AVERAGE_LENGTH number of milliseconds.
    */
   public void storeMovingAverage() {
     int midpoint =
       (getBestBid() == null || getBestAsk() == null) ? buyPrice + 12
         : ((getBestBid().getPrice() + getBestAsk().getPrice()) / 2);
     if (midpoints.size() > MOVING_AVERAGE_LENGTH) {
       movingSum -= midpoints.poll();
     }
     movingSum += midpoint;
     midpoints.add(midpoint);
   }
 
   /**
    * Provides agents with the moving average with length of time specified by
    * the calculateMovingAverage() method.
    */
   public int getMovingAverage() {
     return movingSum / midpoints.size();
   }
 
   /**
    * Logs all the required information into the order book and updates graph
    * when an order is created, modified, or deleted. The CSV to be logged will
    * have the following fields: Agent ID, Message Type (1 = new order, 2 =
    * modification , 3 = cancel, 105 = trade), Buy/Sell (1/2), Order ID, Original
    * Order Quantity, Price, Order Type (limit/market), Leaves Quantity.
    * 
    * @param order
    *          Order to log
    * @param messageType
    *          Message type
    * @param market
    *          True if logging a market order. False if logging a limit order
    */
   public void logOrder(Order order, int messageType, boolean market,
     int quantChanged, int priceChanged) {
 
     if (!market) {
       switch (messageType) {
         case 1:
           Controller.graphFrame.addOrder(order.isBuyOrder(),
             order.getCurrentQuant(), order.getPrice());
           break;
         case 2:
           if (priceChanged != 0) {
             // delete all orders from the old price point
             Controller.graphFrame.addOrder(order.isBuyOrder(),
               -order.getCurrentQuant(), order.getPrice() - priceChanged);
             // re-add the orders to the new price point
           } else {
             Controller.graphFrame.addOrder(order.isBuyOrder(), quantChanged,
               order.getPrice());
           }
           break;
         case 3:
           Controller.graphFrame.addOrder(order.isBuyOrder(), quantChanged,
             order.getPrice());
           break;
         default:
           System.out.println("Message type " + messageType + " is invalid.");
           System.exit(1);
           break;
       }
     }
 
     if (logBuffer.size() == LOG_BUFFER_SIZE) {
       // write the stuff to the file.
       writeToLog();
     }
     logBuffer.add(order.getCreatorID() + "," + messageType + ","
       + (order.isBuyOrder() ? "1" : "2") + "," + order.getId() + ","
       + order.getOriginalQuant() + "," + order.getPrice() / 100.0 + ","
       + (market ? "Market" : "Limit") + "," + order.getCurrentQuant() + "\n");
   }
 
   /**
    * Logs all the required information into the orderbook and updates graph if
    * trade occurs. In addition to the fields above, this method will also add
    * fields for Price of Trade, Quantity Filled, Aggressor Indicator (Y/N), and
    * Trade ID. Calls updateGraph() if needed.
    * 
    * @param agOrder
    *          The order of the aggressive agent.
    * @param tradePrice
    *          The price that the trade occurred at.
    * @param volume
    *          The volume that was traded.
    */
   public void logTrade(Order agOrder, int tradePrice, int volume) {
     // TODO when a market order occurs, what is the last trade price that we
     // log? There is a case when a market order depletes a price point
     // TODO (optimization) if logTrade only updates the graph now, we can just
     // call addTrade in the trade methods every time we want to update the
     // trade price
 
     Controller.graphFrame.addTrade(Controller.time * 0.001, tradePrice);
   }
 
   /**
    * A logging method that is called to log aggressive orders when they trade
    * only.
    * 
    * @param agOrder
    *          The aggressive order to be logged.
    * @param market
    *          True if the aggressive order is a market order.
    * @param tradePrice
    *          The price that the trade occurred at.
    * @param volume
    *          The volume that was traded on this order.
    */
   public void logAggressiveTrader(Order agOrder, boolean market,
     int tradePrice, int volume) {
 
     if (agOrder.isBuyOrder()) {
       currAgVolumeBuySide += volume;
     } else {
       currAgVolumeSellSide += volume;
     }
 
     Controller.graphFrame.addOrder(agOrder.isBuyOrder(), -volume, tradePrice);
     if (logBuffer.size() == LOG_BUFFER_SIZE) {
       // write the stuff to the file.
       // logging for the passive order
       writeToLog();
     }
     logBuffer.add(agOrder.getCreatorID() + ",105,"
       + (agOrder.isBuyOrder() ? "1" : "2") + "," + agOrder.getId() + ","
       + agOrder.getOriginalQuant() + "," + agOrder.getPrice() * 0.01 + ","
       + (market ? "Market," : "Limit,") + agOrder.getCurrentQuant() + ","
       + tradePrice * 0.01 + "," + volume + ",Y," + System.currentTimeMillis()
       + "\n");
 
   }
 
   /**
    * A logging method that is called to log passive orders when they are traded
    * only.
    * 
    * @param passOrder
    *          The passive order to be logged.
    * @param tradePrice
    *          The price (cents) that the trade occurred at.
    * @param volume
    *          The volume that was traded on this order.
    */
   public void logExtraTrades(Order passOrder, int tradePrice, int volume) {
     if (passOrder.isBuyOrder()) {
       currAgVolumeBuySide += volume;
     } else {
       currAgVolumeSellSide += volume;
     }
 
     Controller.graphFrame.addOrder(passOrder.isBuyOrder(), -volume, tradePrice);
     if (logBuffer.size() == LOG_BUFFER_SIZE) {
       // write the stuff to the file.
       // logging for the passive order
       writeToLog();
 
     }
     logBuffer.add(passOrder.getCreatorID() + ",105,"
       + (passOrder.isBuyOrder() ? "1" : "2") + "," + passOrder.getId() + ","
       + passOrder.getOriginalQuant() + "," + passOrder.getPrice() * 0.01
       + ",Limit," + passOrder.getCurrentQuant() + "," + tradePrice * 0.01 + ","
       + volume + ",N," + System.currentTimeMillis() + "\n");
 
   }
 
   /**
    * Writes the logBuffer contents to the file and then clears the log buffer.
    * This is called when the buffer is full or when the simulation has ended.
    */
   public void writeToLog() {
     try {
       FileWriter writer = new FileWriter(Controller.graphFrame.getDest(), true);
       for (int i = 0; i < logBuffer.size(); i++) {
         writer.append(logBuffer.get(i));
       }
       writer.flush();
       writer.close();
 
     } catch (IOException e) {
       // TODO Auto-generated catch block
       e.printStackTrace();
     }
 
     logBuffer.clear();
   }
 
   /**
    * @return The volume of aggressive shares bought in the last millisecond of
    *         trading.
    */
   public int getLastAgVolumeBuySide() {
     return lastAgVolumeBuySide;
   }
 
   /**
    * @return The volume of aggressive shares sold in the last millisecond of
    *         trading.
    */
   public int getLastAgVolumeSellSide() {
     return lastAgVolumeSellSide;
   }
 
   /**
    * @param agentID
    *          Agent to select orders from
    * @return A random order that the agent made that has not been traded yet
    */
   public Order getRandomOrder(long agentID) {
     return orderMap.get(agentID).get(
       randomElementGenerator.nextInt(orderMap.get(agentID).size()));
   }
 
   public void cancelAllSellOrders(long agentID) {
     for (Order o : orderMap.get(agentMap)) {
       if (!o.isBuyOrder()) {
         cancelOrder(o, false);
       }
     }
   }
 
   public void cancelAllBuyOrders(long agentID) {
     for (Order o : orderMap.get(agentMap)) {
       if (o.isBuyOrder()) {
         cancelOrder(o, false);
       }
     }
   }
 }
