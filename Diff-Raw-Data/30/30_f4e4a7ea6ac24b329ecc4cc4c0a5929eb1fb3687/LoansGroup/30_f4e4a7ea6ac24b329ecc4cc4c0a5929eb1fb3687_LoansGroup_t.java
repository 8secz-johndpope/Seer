 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 package ise.gameoflife.groups;
 
 import ise.gameoflife.inputs.LeaveNotification.Reasons;
 import ise.gameoflife.models.GroupDataInitialiser;
 import ise.gameoflife.models.HuntingTeam;
 import ise.gameoflife.models.Tuple;
 import ise.gameoflife.participants.AbstractGroupAgent;
 import ise.gameoflife.participants.PublicGroupDataModel;
 import ise.gameoflife.tokens.AgentType;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.Map;
 import java.util.Random;
 import java.util.TreeSet;
 import java.util.LinkedHashMap;
 import ise.gameoflife.tokens.InteractionResult;
 import java.util.Set;
 
 /**
  *
  * @author george
  */
 public class LoansGroup extends AbstractGroupAgent {
     private static final long serialVersionUID = 1L;
 
 
     private static final double priceToPlay = 100;
     private static final double achievementThreshold = 1000;//the goal that any group is trying to achieve (it can be thought as the group's progression to a new age)
     
     private static Map<String, Double> inNeed = new HashMap<String, Double>();
     private static Map<String, HashMap<String, Tuple<Double, Double> >> loanRequestsAccepted = new HashMap<String, HashMap<String, Tuple<Double, Double> >>();
     private static Map<String, List<Tuple<String, Double> > > loanRepayments = new HashMap<String, List<Tuple<String, Double> > >();
 
     private final double greediness = new Random().nextDouble();
     private Map<String, List<Tuple<Double, Double> > > loansGiven = new HashMap<String, List<Tuple<Double, Double> > >();
     private Map<String, List<Tuple<Double, Double> > > loansTaken = new HashMap<String, List<Tuple<Double, Double> > >();
 
     private static Map<String, Map<String, List<Tuple<Double, Double> > > > publicLoansGiven = new HashMap<String, Map<String, List<Tuple<Double, Double> > > >();
 
     @Deprecated
     public LoansGroup() {
     	super();
     }
 
     public LoansGroup(GroupDataInitialiser dm) {
 	super(dm);
     }
 
     @Override
     protected void onActivate() {
         //Do nothing!
     }
 
     @Override
     protected boolean respondToJoinRequest(String playerID) {
         //To keep it simple always accept agents no matter what 
         return true;
     }
 
     private Comparator<String> c = new Comparator<String>() {
             private Random r = new Random(0);
             @Override
             public int compare(String o1, String o2)
             {
                     return (r.nextBoolean() ? -1 : 1);
             }
     };
 
     @Override
     public List<HuntingTeam> selectTeams()
     {
             ArrayList<HuntingTeam> teams = new ArrayList <HuntingTeam>();
             List<String> members = new ArrayList<String>(getDataModel().getMemberList());
             Collections.sort(members, c);
             int agents = members.size();
 
             for(int i=0; i < agents; i += 2){
                     int ubound = (i + 2 >= agents) ? agents : i + 2;
                     teams.add(new HuntingTeam(members.subList(i, ubound)));
         }
 
             return teams;
     }
 
     @Override
     protected void onMemberLeave(String playerID, Reasons reason) {
         //TODO: Reuse code from TestPoliticalGroup but it doesn't really matter because we don't care about politics
     }
     
     @Override
     protected void beforeNewRound() {
         //If a group in need has died then we have to remove it from the set.
         if(!getConn().availableGroups().containsAll(inNeed.keySet()))
         {
             Set<String> available = getConn().availableGroups();
             Iterator<String> i = inNeed.keySet().iterator();
             while(i.hasNext())
             {
                 String member = i.next();
                 if (!available.contains(member))
                 {
                     i.remove();
                 }
             }
         }
 
         theMoneyIsOK(getDataModel().getCurrentReservedFood());
         if (getDataModel().getMemberList().size() != 1)
         {
             List<String> newPanel = updatePanel();
             this.setPanel(newPanel);
         }
     }    
 
     @Override
     protected AgentType decideGroupStrategy() {
         //Check if this group has leader/leaders. If leaders have not emerge yet then no decision at all
         List<String> currentPanel = getDataModel().getPanel();
         int population = getDataModel().getMemberList().size();
 
         if (currentPanel.isEmpty()||(population == 1))  return null;
 
         List<Tuple<AgentType, Double> > followersTypeCounterList = new LinkedList<Tuple<AgentType, Double> >();
         List<Tuple<AgentType, Double> > panelTypeCounterList = new LinkedList<Tuple<AgentType, Double> >();
 
         //We get lists containing panel's and followers' preferences in strategies in descending order
         followersTypeCounterList = getStrategyPreferences(getDataModel().getMemberList());
         panelTypeCounterList = getStrategyPreferences(currentPanel);
 
         //Calculate the quotum. It is the number of supporters needed to pass a proposal. In this case proposal
         //is the strategy of the group. The quotum is a function of the social belief of the group
         double quotum = (population * getDataModel().getEstimatedSocialLocation())/population;
 
         //Start with the most prefereed strategy of the panel (the strategy that the leader/leaders wish to follow
         //If this strategy is supported by a high enough number of followers (quotum) then we pick this strategy
         //Otherwise try the next best strategy. The lower the quotum the less easy is to get your proposal accepted
         //This is the case of dictatorship.
         Iterator<Tuple<AgentType, Double> > i = panelTypeCounterList.iterator();
         while(i.hasNext())
         {
             int n = 0;
             Tuple<AgentType, Double> panelPreference = i.next();
             while (panelPreference.getKey() != followersTypeCounterList.get(n).getKey())
             {
                 n++;
             }
             double followerSupport = followersTypeCounterList.get(n).getValue();
             if (followerSupport >= quotum)
             {
                 return panelPreference.getKey();
             }
         }
         //If we have reached this statement then we have not found a well suported strategy probably because the
         //quotum is very high (bottom of y axis - anarchism), so the group does not get to play the hunting game
         //other groups
         return null;
     }
     
     private List<Tuple<AgentType, Double>> getStrategyPreferences(List<String> agents) {
         
         int population = agents.size();
 
         Tuple<AgentType, Double> tftTypes = new Tuple<AgentType, Double>(AgentType.TFT, 0.0);
         Tuple<AgentType, Double> acTypes = new Tuple<AgentType, Double>(AgentType.AC, 0.0);
         Tuple<AgentType, Double> adTypes = new Tuple<AgentType, Double>(AgentType.AD, 0.0);
         Tuple<AgentType, Double> rTypes = new Tuple<AgentType, Double>(AgentType.R, 0.0);
 
         //Count types in agents list
         for (String agentID : agents)
         {
             switch(getConn().getAgentById(agentID).getAgentType())
             {
                 case AC:
                     double oldCountAC = acTypes.getValue();
                     acTypes.setValue(oldCountAC+1);
                     break;
                 case AD:
                     double oldCountAD = adTypes.getValue();
                     adTypes.setValue(oldCountAD+1);
                     break;
                 case TFT:
                     double oldCountTFT = tftTypes.getValue();
                     tftTypes.setValue(oldCountTFT+1);
                     break;
                 case R:
                     double oldCountR = rTypes.getValue();
                     rTypes.setValue(oldCountR+1);
                     break;
             }
         }
 
         //Find the average of each type
         acTypes.setValue(acTypes.getValue()/population);
         adTypes.setValue(adTypes.getValue()/population);
         tftTypes.setValue(tftTypes.getValue()/population);
         rTypes.setValue(rTypes.getValue()/population);
 
         List< Tuple<AgentType, Double> > preferencesRatioList = new LinkedList<Tuple<AgentType, Double> >();
 
         //Add the ratios to the list
         preferencesRatioList.add(acTypes);
         preferencesRatioList.add(adTypes);
         preferencesRatioList.add(tftTypes);
         preferencesRatioList.add(rTypes);
 
         //Sort the preferred startegies in descending order
         Collections.sort(preferencesRatioList, preferencesComparator);
 
         return preferencesRatioList;
     }
     
     private Comparator< Tuple<AgentType, Double> > preferencesComparator = new Comparator< Tuple<AgentType, Double> >() {
         @Override
         public int compare(Tuple<AgentType, Double> o1, Tuple<AgentType, Double> o2)
         {
             Double v1 = o1.getValue();
             Double v2 = o2.getValue();
             return (v1>v2 ? -1 : 1);
         }
     };
 
         /**
     * This method updates the panel for this group. The panel is the set of leaders in this group
     * The size of the panel depends on the social position of the group. If it is at the very top
     * it has a single leader (dictator). If it is at the bottom then every member belongs to the panel (anarchism).
     * @param none
     * @return The new panel members.
     */
     private List<String> updatePanel(){
 
         double groupSocialPosition;
         int population, panelSize;
 
         //STEP 1:Find the size of the panel. It is the proportion of the total population that
         // can be in the panel. It is calculated using the social position of the group.
         population = getDataModel().getMemberList().size();
         groupSocialPosition = getDataModel().getEstimatedSocialLocation();
 
         //Round to the closest integer
         panelSize = (int) Math.round(population*groupSocialPosition);
         if (panelSize == 0) //The group is on the very top of the axis. Dictatorship
         {
             //Force panelSize to be at least one (dictator)
             panelSize = 1;
         }
         //STEP 1 END
 
         //STEP 2: Get the average trust of each agent in the group
         List< Tuple<String, Double> > panelCandidates = new LinkedList< Tuple<String, Double> >();
 
         List<String> groupMembers = getDataModel().getMemberList();
 
         for (String candidate: groupMembers )
         {
             double sum = 0;
             int numKnownTrustValues = 0;
             for (String member: groupMembers )
             {
                 if ((getConn().getAgentById(member).getTrust(candidate) != null)&&(!member.equals(candidate)))
                 {
                     sum += getConn().getAgentById(member).getTrust(candidate);
                     numKnownTrustValues++;
                 }
             }
 
             Tuple<String, Double> tuple;
             if (numKnownTrustValues != 0)
             {
                 tuple = new Tuple<String, Double>(candidate, sum/numKnownTrustValues);
                 panelCandidates.add(tuple);
             }
         }
         //STEP 2 END
 
         //STEP 3: Sort the agents in descending order of trust values
         Collections.sort(panelCandidates, d);
         //STEP 3 END
 
         //STEP 4: Populate the panel list with the most trusted agents in the group (i.e. the leaders)
         //Note that eventhough an agent is a candidate its trust must be above a threshold to become member of the panel.
         //The threshold is the social position. If the group is highly authoritarian then anyone with a trust value
         //above zero can become a leader. In libertarian groups panel formations are rare since a relatively high trust value
         //must be achieved! Also the threshold acts as a warning for current panel members. If their trust falls
         //below this threshold due to bad decisions they will be ousted in the next round.
         List<String> newPanel = new LinkedList<String>();
         if (!panelCandidates.isEmpty()&&(panelCandidates.size() >= panelSize))//Panel is not empty and we have enough candidates to select leaders
         {
             for (int i = 0; i < panelSize; i++)
             {
                 if (panelCandidates.get(i).getValue() >= groupSocialPosition)
                 {
                     newPanel.add(panelCandidates.get(i).getKey());
                 }
             }
         }
         //STEP 4 END
 
         return newPanel;
     }
 
     private Comparator< Tuple<String, Double> > d = new Comparator< Tuple<String, Double> >() {
         @Override
         public int compare(Tuple<String, Double> o1, Tuple<String, Double> o2)
         {
             Double v1 = o1.getValue();
             Double v2 = o2.getValue();
             return (v1>v2 ? -1 : 1);
         }
     };
 
     private Comparator< Tuple<Double, Double> > loansComparator = new Comparator< Tuple<Double, Double> >() {
         @Override
         public int compare(Tuple<Double, Double> o1, Tuple<Double, Double> o2)
         {
             Double v1 = o1.getKey();
             Double v2 = o2.getKey();
             return (v1>v2 ? -1 : 1);
         }
     };
 
     @Override
     protected Tuple<AgentType, Double> makePayments()
     {   
         double currentFoodReserve;
         AgentType strategy = getDataModel().getGroupStrategy();
         
         currentFoodReserve = getDataModel().getCurrentReservedFood();        
         if(!inNeed.containsKey(this.getId()))
         {
             //DEBUGGING ONLY
             System.out.println("------------------");
             System.out.println(this.getDataModel().getName());
             System.out.println("Current reserved food: "+this.getDataModel().getCurrentReservedFood());
             System.out.println("I have " + loansTaken.size()+ " outstanding payments!");
             for (String s: loansTaken.keySet())
             {
                 System.out.println("I owe "+getConn().getGroupById(s).getName());
                 for(Tuple<Double, Double> t: loansTaken.get(s))
                 {
                     System.out.println("    "+t.getKey()+ " units of food with interest rate "+t.getValue());
                 }
             }
 //            System.out.println("*********************");
 //            System.out.println("I have " + loansGiven.size()+ " debtors!");
 //            for (String s: loansGiven.keySet())
 //            {
 //                System.out.println("I will get:");
 //                for(Tuple<Double, Double> t: loansGiven.get(s))
 //                {
 //                    if(getConn().getGroupById(s) == null) break;
 //                    System.out.println("    "+t.getKey()+ " units of food with interest rate "+t.getValue()+" from "+getConn().getGroupById(s).getName());
 //                }
 //             }
             //DEBUGGING ONLY END
             
             //Spend money       
             if(strategy != null)
             {  
                 currentFoodReserve -= priceToPlay;//pay, in theory, to join the game among groups
                 if(currentFoodReserve < priceToPlay)//if the theory is way to risky
                 {
                     currentFoodReserve += priceToPlay;//go back
                     strategy = null;//and sit this turn out
                 }
             }
 
             if ( this.greediness > new Random().nextDouble() )
             {
                 double goalRatio = currentFoodReserve / achievementThreshold;//check how close you are to attaining achievement
                 double percentDecrease;
                 percentDecrease = ( (1-getAverageHappiness(0)) * goalRatio) * currentFoodReserve;
                 currentFoodReserve -= percentDecrease;
             }
 
             if (!loansTaken.isEmpty())
             {
                 for (String creditor: loansTaken.keySet())
                 {
                     double totalAmountPaid = 0;
                     //Find the loans taken from this creditor and sort the in descending order
                     Collections.sort(loansTaken.get(creditor), loansComparator);
                     //Iterate through the loans from this creditor
                     List<Tuple<Double, Double> > loanInfoList = loansTaken.get(creditor);
                     Iterator<Tuple<Double, Double> > i = loanInfoList.iterator();
 
                     while(i.hasNext())
                     {
                         Tuple<Double, Double> loanInfo = i.next();
 
                         //Calculate the amount to pay (amount *(1+ interest))
                         double amountToPay = loanInfo.getKey()* (1+loanInfo.getValue());
                         //If the group has the money then it pays
                         if (currentFoodReserve > amountToPay + priceToPlay)
                         {
                             currentFoodReserve -= amountToPay;
                             //We remove this loan from  since it is paid
                             i.remove();
                             totalAmountPaid += amountToPay;
                         }
                     }
                     //If that was the only loan taken from this creditor then remove the creditor form the list
                     if (loansTaken.get(creditor).isEmpty()) loansTaken.remove(creditor);
                     if (totalAmountPaid != 0)
                     {
                         System.out.println("I paid back "+totalAmountPaid+ " to "+getConn().getGroupById(creditor).getName());
                         Tuple<String, Double> paymentReceipt = new Tuple<String, Double>();
                         paymentReceipt.add(this.getId(), totalAmountPaid);
                         if (!loanRepayments.containsKey(creditor))
                         {
                             List<Tuple<String, Double> > existingPayments = new ArrayList<Tuple<String, Double> >();
                             existingPayments.add(paymentReceipt);
                             loanRepayments.put(creditor, existingPayments);
                         }
                         else
                         {
                            List<Tuple<String, Double> > existingPayments = loanRepayments.get(creditor);
                             existingPayments.add(paymentReceipt);
                         }
                     }
                 }
             }
             System.out.println("After repayment: "+currentFoodReserve);
         }
         else
         {
             strategy = null;
         }
         return new Tuple<AgentType, Double>(strategy, currentFoodReserve);            
     }
     
     private boolean theMoneyIsOK(double mostRecentReserve)
     {
         //is the reserve increasing or decreasing        
         double deltaFoodReserve;
         if (getDataModel().getReservedFoodHistory().size() > 1)
         {
             deltaFoodReserve = mostRecentReserve - getDataModel().getReservedFoodHistory().getValue(1);
         }
         else
         {
             deltaFoodReserve = 0;
         }
 
         //check how close the group is to attaining achievement
         double goalRatio = mostRecentReserve / achievementThreshold;        
 
         if(!inNeed.containsKey(this.getId()))
         {            
             if((goalRatio < 0.15)&&(deltaFoodReserve<0))
             {
                 //if group is only 15% of the way then the group needs help      
                 inNeed.put(this.getId(), 150 - mostRecentReserve);
                 return false;
             }
             else
             {
                 //everything is ok, nobody needs help
                 return true;
             }
         }
         else
         {
             //you've been in need for quite a while, it would seem
             return false;
         }
     }
     
     private double getAverageHappiness(int turnsAgo)
     {
         double average = 0;
 
         if(!getDataModel().getMemberList().isEmpty())
         {
             Double happiness;
             for(String member : getDataModel().getMemberList())
             {
                 if(turnsAgo > 0)
                 {
                     if (getConn().getAgentById(member).getHappinessHistory().size() > 1)
                     {
                         happiness = getConn().getAgentById(member).getHappinessHistory().getValue(turnsAgo);
                     }
                     else
                     {
                         happiness = 0.0;
                     }
                     if(happiness != null)//otherwise add nothing
                     {
                         average += happiness;                   
                     }
                 }
                 else
                 {
                     happiness = getConn().getAgentById(member).getCurrentHappiness();
                     if(happiness != null)//otherwise add nothing
                     {
                         average += happiness;                   
                     }                    
                 }
             }
             average = average / getDataModel().getMemberList().size();
         }
         return average;
     }
 
     @Override
     protected Tuple<Double, Double> updateTaxedPool(double sharedFood) {
         double currentFoodReserve; 
         currentFoodReserve = getDataModel().getCurrentReservedFood();
         double tax = 0;
 
         //FOR DEBUGGING ONLY
         System.out.println("------------------");
         System.out.println(this.getDataModel().getName());
         System.out.println("Current reserved food: "+this.getDataModel().getCurrentReservedFood());
         //FOR DEBUGGING ONLY END
         
         //If this groups has any debtors must check if any of them has paid back
         if(loanRepayments.containsKey(this.getId()))
         {
             List<Tuple<String, Double> > paymentsInfo = new  ArrayList<Tuple<String, Double> >();
             paymentsInfo = loanRepayments.get(this.getId());
             
             Iterator<Tuple<String, Double> > i = paymentsInfo.iterator();
             while(i.hasNext())
             {
                 Tuple<String, Double> currentPayment = i.next();
                 double amountReceived = currentPayment.getValue();
                 currentFoodReserve += amountReceived;
                 System.out.println("I have received a payment of "+amountReceived+" from "+getConn().getGroupById(currentPayment.getKey()).getName());
             }
             loanRepayments.remove(this.getId());
         }
         System.out.println("After repayments: "+currentFoodReserve);
         //check how close you are to attaining achievement
         double goalRatio = currentFoodReserve / achievementThreshold;
 
         if(inNeed.containsKey(this.getId()))
         {
             //the group is in trouble and needs to tax high
             tax = 1 - goalRatio;//since you're far away from your achievement, tax high 
         }
         else
         {
             //tax normally
             tax = getAverageHappiness(0) * (1-goalRatio);
         }
 
         //The actual taxation happens here
         currentFoodReserve = currentFoodReserve + sharedFood*tax;
         sharedFood = sharedFood - sharedFood*tax;
         
         Tuple<Double, Double> newSharedAndReserve = new Tuple<Double,Double>();
         newSharedAndReserve.add(sharedFood, currentFoodReserve);
 
         return newSharedAndReserve;
     }
 
     @Override
     protected Tuple<InteractionResult, Double> interactWithOtherGroups() {
 
         double currentFoodReserve = getDataModel().getCurrentReservedFood();
         Tuple<InteractionResult, Double> interactionResult = new Tuple<InteractionResult, Double>();
         
         //FOR DEBUGGING ONLY
         System.out.println("------------------");
         System.out.println(this.getDataModel().getName());
         System.out.println("Current reserved food: "+this.getDataModel().getCurrentReservedFood());
         //FOR DEBUGGING ONLY END
 
         //First check if you are in need
         if(inNeed.containsKey(this.getId()))
         {
             //Check if group has managed to recover their economic status to good.
             //Also even if they managed to do so if someone has already givem them a loan then they must pay
             if ((currentFoodReserve > priceToPlay+50)&&(!loanRequestsAccepted.containsKey(this.getId())))
             {
                 inNeed.remove(this.getId());
                 System.out.println("We solved our economic problem so we don't want a loan anymore!");
                 interactionResult.add(InteractionResult.NothingHappened, 0.0);
             }
             else if (loanRequestsAccepted.containsKey(this.getId()))
             {
                  //If the request for a loan was granted then store the receipt in your records to help with repayments later (Hopefully..)
                  HashMap<String, Tuple<Double, Double> > loanRecord = loanRequestsAccepted.get(this.getId());
                  Set<String> giverID = loanRecord.keySet();
 
                  Tuple<Double, Double> currentLoanInfo = loanRecord.get(giverID.iterator().next());
                  if (!loansTaken.containsKey(giverID.iterator().next()))
                  {
                      List<Tuple<Double, Double> > existingLoans = new ArrayList<Tuple<Double, Double> >();
                      existingLoans.add(currentLoanInfo);
                      this.loansTaken.put(giverID.iterator().next(), existingLoans);
                  }
                 else
                 {
                    List<Tuple<Double, Double> > existingLoans = this.loansTaken.get(giverID.iterator().next());
                     existingLoans.add(currentLoanInfo);
                 }
 
                  loanRequestsAccepted.remove(this.getId());
                  inNeed.remove(this.getId());
                  interactionResult.add(InteractionResult.LoanTaken, loanRecord.get(giverID.iterator().next()).getKey());
                  System.out.println("I have requested a loan and the result is "+ interactionResult.getKey()+ ". I have been given "+ interactionResult.getValue()+ " units of food!");
             }
             else
             {
                 //Else if no one has given you money do nothing
                 interactionResult.add(InteractionResult.NothingHappened, 0.0);
                 System.out.println("I have requested a loan and the result is "+ interactionResult.getKey()+ ". I have been given "+ interactionResult.getValue()+ " units of food!");
             }
             return interactionResult;
         }
 
         //If you are not in need you might want to help another group
         if (!inNeed.isEmpty())
         {
             Map<String, Double> inNeedSorted = new HashMap<String, Double>();
             inNeedSorted = sortHashMap(inNeed);            
             for (String groupID: inNeedSorted.keySet() )
             {
                 //if someone else accepted their requests do nothing!
                 if (!loanRequestsAccepted.containsKey(groupID))
                 {
                     double amountNeeded = inNeed.get(groupID);
                     //intrest = 0.5 of loaner's greediness and requesters situation which is neve above 0.15
                     double interestRate = 0.05*greediness + (getConn().getGroupById(groupID).getCurrentReservedFood() / achievementThreshold);
                     //TODO: Design a heuristic to decide if group will give a loan
                     //For now give a loan if u have the amount needed
                     if (currentFoodReserve - amountNeeded >  priceToPlay+50)
                     {
                         //Create a tuple containing the amount granted and the interest
                         Tuple<Double, Double> loanInfo = new Tuple<Double, Double>();
                         loanInfo.add(amountNeeded, interestRate);
 
                         //Then store the loan info along with the requester ID in your records
                         if (!loansGiven.containsKey(groupID))
                         {
                             List<Tuple<Double, Double> > existingLoans = new ArrayList<Tuple<Double, Double> >();
                             existingLoans.add(loanInfo);
                             this.loansGiven.put(groupID, existingLoans);
                         }
                         else
                         {
                            List<Tuple<Double, Double> > existingLoans = this.loansGiven.get(groupID);
                             existingLoans.add(loanInfo);
                         }
 
                         publicLoansGiven.put(this.getId(), loansGiven);
                         
                         //Use the same structure to send a receipt to the requester to store it in his records
                         HashMap<String, Tuple<Double, Double> > loanRecord = new HashMap<String, Tuple<Double, Double> >();
                         loanRecord.put(this.getId(), loanInfo);
                         loanRequestsAccepted.put(groupID, loanRecord);
                         interactionResult.add(InteractionResult.LoanGiven, amountNeeded);
                         System.out.println(getConn().getGroupById(groupID).getName() + " requested a loan and the result is "+ interactionResult.getKey()+ ". I gave them "+ interactionResult.getValue()+ " units of food!");
                         return interactionResult;
                     }
                 }
             }
         }
         interactionResult.add(InteractionResult.NothingHappened, 0.0);
         System.out.println("No interaction at all!");
         return interactionResult;
     }
     
     private HashMap<String, Double> sortHashMap(Map<String, Double> unsorted){
         Map<String, Double> tempMap = new HashMap<String, Double>();
         for (String key : unsorted.keySet())
         {
             tempMap.put(key,unsorted.get(key));
         }
 
         //make sure to get the data
         List<String> unsortedKeys = new ArrayList<String>(tempMap.keySet());
         List<Double> unsortedValues = new ArrayList<Double>(tempMap.values());
         
         //make our result ready
         HashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>();
         
         //put the values in a tree set, they are immediately orderd, then put them in an array
         TreeSet<Double> sortedSet = new TreeSet<Double>(unsortedValues);       
         Object[] sortedArray = sortedSet.toArray();
         
         //sort them in a map
         for (int i = 0; i < sortedArray.length; i++){
             sortedMap.put(unsortedKeys.get(unsortedValues.indexOf(sortedArray[i])), 
                           (Double)sortedArray[i]);
         }
         return sortedMap;
     }
 
    public static Map<String, List<Tuple<Double, Double> > > getLoansGiven(PublicGroupDataModel dm)
     {
         if (!publicLoansGiven.containsKey(dm.getId()))
             return null;
         else
            return publicLoansGiven.get(dm.getId());
     }
     
 }
 
