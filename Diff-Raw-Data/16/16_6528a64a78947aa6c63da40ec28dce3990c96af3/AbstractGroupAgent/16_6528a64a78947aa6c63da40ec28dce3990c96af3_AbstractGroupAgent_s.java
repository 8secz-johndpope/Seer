 package ise.gameoflife.participants;
 
 import ise.gameoflife.models.GroupDataModel;
 import ise.gameoflife.actions.RespondToApplication;
 import ise.gameoflife.enviroment.EnvConnector;
 import ise.gameoflife.enviroment.PublicEnvironmentConnection;
 import ise.gameoflife.inputs.HuntResult;
 import ise.gameoflife.inputs.JoinRequest;
 import ise.gameoflife.inputs.LeaveNotification;
 import ise.gameoflife.models.Food;
 import ise.gameoflife.models.HuntingTeam;
 import ise.gameoflife.tokens.GroupRegistration;
 import ise.gameoflife.tokens.RegistrationResponse;
 import ise.gameoflife.tokens.TurnType;
 import ise.gameoflife.tokens.UnregisterRequest;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.UUID;
 import org.simpleframework.xml.Element;
 import presage.EnvironmentConnector;
 import presage.Input;
 import presage.Participant;
 import presage.environment.messages.ENVRegistrationResponse;
 // TODO: Make it clear that the contract calls for a public consturctor with one argument that takes in the datamodel.
 /**
  *
  * @author Benedict
  */
 public abstract class AbstractGroupAgent implements Participant
 {
 	private static final long serialVersionUID = 1L;
 
 	/**
 	 * The DataModel used by this agent.
 	 */
 	@Element
 	private GroupDataModel dm;
 	/**
 	 * The authorisation code for use with sexy things like the environment
 	 */
 	private UUID authCode;
 	/**
 	 * Reference to the environment connector, that allows the agent to interact
 	 * with the environment
 	 */
 	protected PublicEnvironmentConnection conn;
 	private EnvConnector ec;
 	private EnvironmentConnector tmp_ec;
 	
 	private Map<String, Double> huntResult;
 
 	/**
 	 * 
 	 * @deprecated 
 	 */
 	@Deprecated
 	public AbstractGroupAgent()
 	{
 		super();
 	}
 
 	/**
 	 * 
 	 * @param dm
 	 */
 	public AbstractGroupAgent(GroupDataModel dm)
 	{
 		this.dm = dm;
 	}
 
 	@Override
 	public String getId()
 	{
 		return dm.getId();
 	}
 
 	@Override
 	public ArrayList<String> getRoles()
 	{
 		return new ArrayList<String>(Arrays.asList(new String[]{"group"}));
 	}
 
 @Override
 	public void initialise(EnvironmentConnector environmentConnector)
 	{
 		tmp_ec = environmentConnector;
 		dm.initialise(environmentConnector);

		// TODO: Add input handlers here
		
		conn = PublicEnvironmentConnection.getInstance();
 		onInit();
 	}
 
 	@Override
 	public final void onActivation()
 	{
 		GroupRegistration request = new GroupRegistration(dm.getId());
 		ENVRegistrationResponse r = tmp_ec.register(request);
 		this.authCode = r.getAuthCode();
 		this.ec = ((RegistrationResponse)r).getEc();
 		onActivate();
 	}
 
 	@Override
 	public final void onDeActivation()
 	{
 		ec.deregister(new UnregisterRequest(dm.getId(), authCode));
 	}
 
 	@Override
 	public void execute()
 	{
 		TurnType turn = ec.getCurrentTurnType();
 
 		if (TurnType.firstTurn.equals(turn))
 		{
 			beforeNewRound();
 			clearRoundData();
 		}
 
 		switch (turn)
 		{
 			case GroupSelect:
 				// Nothing to do here - this is handled in enQueueMessage
 				break;
 			case TeamSelect:
 				doTeamSelect();
 				break;
 			case GoHunt:
 				// Nothing to do here - agents are off hunting!
 				break;
 			case HuntResults:
 				doHandleHuntResults();
 				break;
 		}
 	}
 
 	private void clearRoundData()
 	{
 		// TODO: Clear any data that is per round that is stored
 		huntResult = new HashMap<String, Double>();
 	}
 	
 	private void doTeamSelect()
 	{
 		Map<HuntingTeam, Food> teams = selectTeams();
 		// TODO: Inform each member of each team of their team and order with a
 		// Group order action
 	}
 	
 	private void doHandleHuntResults()
 	{
 		huntResult = distributeFood(Collections.unmodifiableMap(huntResult));
 		// TODO: Inform all agents of their share
 		// TODO: Check that all agents get a value
 		// TODO: Inform agents of 0 food if value was not set (for their records)
 	}
 	
 	/**
 	 * 
 	 * @param cycle
 	 */
 	@Override
 	public final void setTime(long cycle)
 	{
 		dm.setTime(cycle);
 	}
 
 	/**
 	 * Returns the DataModel of this object
 	 * @return The DataModel of this object
 	 */
 	@Override
 	public final GroupDataModel getInternalDataModel()
 	{
 		return dm;
 	}
 
 	@Override
 	public final void enqueueInput(Input input)
 	{
 		if (input.getClass().equals(JoinRequest.class))
 		{
 			boolean response = this.respondToJoinRequest(((JoinRequest)input).getAgent());
 			ec.act(new RespondToApplication(this.getId(), response), this.getId(), authCode);
 			if (response)	this.dm.memberList.add(((JoinRequest)input).getAgent());
 			return;
 		}
 
 		if (input.getClass().equals(LeaveNotification.class))
 		{
 			final LeaveNotification in = (LeaveNotification)input;
 			dm.memberList.remove(in.getAgent());
 			this.onMemberLeave(in.getAgent(), in.getReason());
 			return;
 		}
 
 		if (input.getClass().equals(HuntResult.class))
 		{
 			final HuntResult in = (HuntResult)input;
 			huntResult.put(in.getAgent(), in.getNutritionValue());
 			return;
 		}
 
 		ec.logToErrorLog("Group Unable to handle Input of type " + input.getClass().getCanonicalName());
 	}
 
 	/**
 	 * 
 	 * @param input
 	 */
 	@Override
 	public final void enqueueInput(ArrayList<Input> input)
 	{
 		for (Input in : input) enqueueInput(in);
 	}
 
 	@Override
 	public void onSimulationComplete()
 	{
 		// TODO: Need anything here?
 	}
 
 	/**
 	 * TODO: Document
 	 */
 	abstract protected void onInit();
 	/**
 	 * TODO: Document
 	 */
 	abstract protected void onActivate();
 	
 	/**
 	 * TODO: Document
 	 * @param playerID
 	 * @return
 	 */
 	abstract protected boolean respondToJoinRequest(String playerID);
 	/**
 	 * TODO: Document
 	 * @return
 	 */
 	abstract protected Map<HuntingTeam, Food> selectTeams();
 	/**
 	 * Function used to distribute the food around after the brave
 	 * hunters have returned with their winnings
 	 * @param gains
 	 * @return
 	 */
 	abstract protected Map<String, Double> distributeFood(Map<String, Double> gains);
 	/**
 	 * 
 	 * @param playerID
 	 * @param reason
 	 */
 	abstract protected void onMemberLeave(String playerID, LeaveNotification.Reasons reason);
 	/**
 	 * Here you implement any code concerning data storage about the events
 	 * of this round before it is all deleted for a new round to begin.
 	 * N.B: a "round" occurs after all turn types have been iterated through. This
 	 * is to avoid confusion between "cycles", "turn" and "time". Alternatively, use
 	 * of the unit "Harcourt" may also be used. 1 Round = 1 Harcourt
 	 */
 	abstract protected void beforeNewRound();
 }
