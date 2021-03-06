 package javachallenge.server;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Random;
 
 import javachallenge.IllegalAgentException;
 import javachallenge.NotImplementedException;
 import javachallenge.common.Action;
 import javachallenge.common.ActionType;
 import javachallenge.common.AgentMessage;
 import javachallenge.common.BlockType;
 import javachallenge.common.Direction;
 import javachallenge.common.InitMessage;
 import javachallenge.common.Point;
 import javachallenge.common.ServerMessage;
 import javachallenge.common.VisionPoint;
 import javachallenge.graphics.GraphicClient;
 import javachallenge.graphics.GraphicClient.DuplicateMemberException;
 import javachallenge.graphics.GraphicClient.OutOfMapException;
 import javachallenge.graphics.util.Position;
 
 public class Engine {
 	private static final int FLAG_POINT = 100;
 	private static final int GAME_CYCLES = 725;
 	private static final int SPAWN_MARGIN = 6 ;
 	private static final int SPAWN_LOW_PERIOD = 0;
 	private static final int SPAWN_NORM_PERIOD = 5;
 
 	
 	private Map map;
 	private int cycle, teamCount;
 	private ArrayList<Team> teams = new ArrayList<Team>();
 	private boolean gameEnded = false;
 	private ArrayList<Agent> deadAgents;
 	private ArrayList<Agent> spawnedAgents;
 	private GraphicClient graphicClient;
 	private Game game;
 	
 	
 	public boolean gameIsOver() {
 		return gameEnded;
 	}
 	
 	public Engine(Game game, GraphicClient graphicClient) {
 		this.game = game;
 		this.map = game.getMap();
 		this.cycle = 0;
 		teamCount = map.getTeamCount();
 		ArrayList<Point> spawnLoc = map.getSpawnLocations();
 		for(int i = 0 ; i < teamCount ; i++){
 			teams.add(new Team(spawnLoc.get(i), i));
 		}
 		
 		this.graphicClient = graphicClient ;
 	}
 	
 	//methods for running the game
 	
 	public void beginStep(){
 		deadAgents = new ArrayList<Agent>();
 		spawnedAgents = new ArrayList<Agent>();
 	}
 	
 	
 	public void teamStep(ArrayList<Action> actions){
 		if(gameEnded){
 			return;
 		}
 		if(cycle % 2 == 0){
 			handleMoves(actions);
 		}
 		else{
 			//handle attacks
 			HashMap<Integer, ArrayList<Integer>> attackNum = new HashMap<Integer, ArrayList<Integer>>();
 			int[][] spawnAttacks = new int[teamCount][teamCount];
 			for(Action action : actions){
 				if(action.getType() == ActionType.NONE || action.getType() != ActionType.ATTACK){
 					continue;
 				}
 				try{
 					Agent agent = getAgent(action.getTeamId(), action.getId());
 					Point dest = agent.getLocation().applyDirection(action.getDir());
 					Agent opAgent = game.getAgent(dest);
 					if(opAgent != null){
 						int opId = opAgent.getId();
 						if(attackNum.get(opId) == null){
 							attackNum.put(opId, new ArrayList<Integer>());
 						}
 						ArrayList<Integer> tmp = attackNum.get(opId);
 						tmp.add(agent.getTeamId());
 						attackNum.put(opId, tmp);
 					}
 					
 					//Handle spawn point attacks
 					if(getSpawnLocationTeam(dest) != -1){
 						spawnAttacks[getSpawnLocationTeam(dest)][agent.getTeamId()]++;
 					}
 				}
 				catch(Exception e){
 					System.out.println("Bad Agent : Team " + action.getTeamId());
 					continue;
 				}
 				
 			}
 			for(Action action : actions){
 				if(action.getType() == ActionType.NONE || action.getType() != ActionType.ATTACK){
 					continue;
 				}
 				try{
 					Agent agent = getAgent(action.getTeamId(), action.getId());
 					Point dest = agent.getLocation().applyDirection(action.getDir());
 					Agent opAgent = game.getAgent(dest);
 					int firstTeamAttacks = Collections.frequency(attackNum.get(opAgent.getId()), agent.getTeamId());
 					int secondTeamAttacks = Collections.frequency(attackNum.get(agent.getId()), opAgent.getTeamId());
 					if(firstTeamAttacks >= secondTeamAttacks){
 						if(opAgent.isAlive()){
 							deadAgents.add(opAgent);
 						}
 						opAgent.setAlive(false);
 				
 					}
 				}
 				catch (Exception e) {
 					System.out.println("Bad Agent : Team " + action.getTeamId());
 					continue;
 				}
 			}
 		}
 		
 		for(Flag flag : game.getFlags()){
 			Agent agent = game.getAgent(flag.getLocation());
 			Team team = (agent == null ? null : teams.get(agent.getTeamId()));
 			flag.step(team);
 		}
 		
 	}
 	
 	public int getCycle() {
 		return cycle;
 	}
 
 	public void endStep(){
 		respawn();
 		updateScores();
 		cycle++;
 		//----------------------------------------------
 		if(cycle >= GAME_CYCLES){
 			gameEnded = true;
 		}
 	}
 	
 	//method to move the agents and check for the destination to be empty
 	
 	private boolean moveAgent(Action action){
 		ActionType actionType = action.getType();
 		if(actionType == ActionType.NONE){
 			return false;
 		}
 		
 		Direction dir = action.getDir() ;
 		Agent agent = null ;
 		
 		try {
 			agent = getAgent(action.getTeamId(), action.getId());
 		} catch (IllegalAgentException e) {
 			e.printStackTrace();
 			//TODO ask mina :D
 			return false;
 		}
 		
 		Point dest = agent.getLocation().applyDirection(dir);
 		if(actionType == ActionType.MOVE){
 			//System.err.println("Dest is : " + dest.x + " " + dest.y + " - " + map.isInsideMap(dest));
 			if(map.isInsideMap(dest) && map.getBlockType(dest) == BlockType.GROUND && !occupied(dest)){
 				game.moveAgent(agent, agent.getLocation(), dest) ;
 				agent.setLocation(dest);
 				Integer id = new Integer(agent.getId()) ;
 				graphicClient.move(id, dir) ;
 				
 				// TODO phase2 implement
 				//--------------------------- Move On The Flag  
 //				if (game.hasFlag(dest)){
 //					Flag flag = game.getFlagByLocation(dest); 
 //					if (flag.isAlive()){
 //						flag.obtain() ;
 //						//--------------------------- Update graphic
 //						graphicClient.obtainFlag(flag.getId() + 1);
 //						//--------------------------- Update score
 //						int teamId = action.getTeamId(); 
 //						getTeam(teamId).updaetScore(FLAG_POINT) ;
 //					}
 //				}
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	//returns the specified agent, checking for it to exist and to be alive
 	
 	private Agent getAgent(int teamId, int id) throws IllegalAgentException{
 		Agent agent = teams.get(teamId).getAgent(id);
 		if(agent == null || !agent.isAlive()){
 			throw new IllegalAgentException();
 		}
 		return agent;
 	}
 	
 	//checks if the given location is occupied by an agent, a flag or a spawned point
 	private boolean occupied(Point p){
 		return (game.getAgent(p) !=  null /*|| getSpawnLocationTeam(p) != -1*/);
 	}
 	
 	//returns the id of the team for which the given location is the spawn point, -1 if it is not.
 	private int getSpawnLocationTeam(Point p){
 		for(Team t : teams){
 			if(p.equals(t.getSpawnLocation())){
 				return t.getId();
 			}
 		}
 		return -1;
 	}
 	 
 	private void respawn(){
 		for(Team team : teams){
 			int spawnRate = (team.getAgents().size() >= SPAWN_MARGIN ? SPAWN_LOW_PERIOD : SPAWN_NORM_PERIOD);
 			int lastSpawned = team.getLastSpawned();
 			if(spawnRate > 0 && (lastSpawned < 0 || cycle - lastSpawned > spawnRate || (cycle - lastSpawned) % spawnRate == 0) 
 					&& game.getAgent(team.getSpawnLocation()) == null && team.isActiveSpawnPoint()){
 				Agent newAgent = team.addAgent();
 				game.spawnAgent(newAgent) ;
 				spawnedAgents.add(newAgent);
 				team.setLastSpawned(cycle);
 				
 				
 				Position p = new Position(newAgent.getLocation().x, newAgent.getLocation().y);
 				Integer id = new Integer(newAgent.getId()) ;
  				try {
 					graphicClient.spawn(id, p);
 				} catch (OutOfMapException e) {
 					e.printStackTrace();
 				} catch (DuplicateMemberException e) {
 					e.printStackTrace();
 				}
 			}
 			else{
 				spawnedAgents.add(null);
 			}
 		}
 	}
 	
 	//updates the scores of teams
 	private void updateScores(){
 		for(Flag flag : game.getFlags()){
 			Team owner = flag.getOwner();
 			if(owner != null){
 				owner.updaetScore(FLAG_POINT);
 			}
 		}
 		//-----------------------
 		for (Team t : teams) {
			//TODO fill the ratio :D
			graphicClient.setScore(t.getId(), t.getScore(), 0.25) ;
 		}
 		graphicClient.setTime(cycle) ;
 	}
 	
 	private ArrayList<Integer> getScores(){
 		ArrayList<Integer> result = new ArrayList<Integer>();
 		for(int i = 0 ; i < teamCount ; i++){
 			result.add(teams.get(i).getScore());
 		}
 		return result;
 	}
 	
 	private ArrayList<AgentMessage> getAgentMessages(int teamId){
 		ArrayList<AgentMessage> result = new ArrayList<AgentMessage>();
 		for(Agent agent : teams.get(teamId).getAgents()){
 			if(!agent.isAlive()){
 				continue;
 			}
 			//------------------------------------------------------------------
 			Point loc = agent.getLocation();
 			VisionPoint[] visions = new VisionPoint[Direction.values().length] ;
 			for (Direction dir : Direction.values()) {
 				visions[dir.ordinal()] = game.getVision(loc.applyDirection(dir)) ; 
 			}
 			result.add(new AgentMessage(agent.getId(), loc, visions));
 		}
 		return result;
 	}
 	
 	private ArrayList<Integer> getDeadAgents(int teamId){
 		ArrayList<Integer> deads = new ArrayList<Integer>();
 		for(Agent agent : deadAgents){
 			if(agent.getTeamId() == teamId){
 				deads.add(agent.getId());
 			}
 		}
 		return deads;
 	}
 	
 	public ArrayList<InitMessage> getInitialMessage() {
 		ArrayList<InitMessage> msgs = new ArrayList<InitMessage>();
 
 		for (Team t: teams) {
 			InitMessage msg = new InitMessage(t.getId(), this.map);
 			msgs.add(msg);
 		}
 		return msgs;
 	}
 	
 	public ArrayList<ServerMessage> getStepMessage() {
 		ArrayList<ServerMessage> msgs = new ArrayList<ServerMessage>();
 
 		ArrayList<Integer> scores = getScores();
 		
 		for (Team t: teams) {
 			ServerMessage msg = new ServerMessage();
 			
 			msg.setSpawnedId(spawnedAgents.get(t.getId()) == null ? Agent.noAgent : spawnedAgents.get(t.getId()).getId());
 			msg.setScores(scores);
 			msg.setDeadAgents(getDeadAgents(t.getId()));
 			msg.setAgentMsg(getAgentMessages(t.getId()));
 			msg.setGameEnded(this.gameIsOver()) ;
 			msg.setFlagOwners(game.getFlagOwners()) ;
 			msgs.add(msg);
 		}
 		
 		return msgs;
 	}
 
 	public Team getTeam(int i) {
 		return teams.get(i);
 	}
 	
 	
 	private void handleMoves(ArrayList<Action> actions){
 		ArrayList<Integer> out = new ArrayList<Integer>();
 		ArrayList<Action> outAct = new ArrayList<Action>();
 		ArrayList<ArrayList<Integer>> firstIn = new ArrayList<ArrayList<Integer>>();
 		
 		ArrayList<Integer> in = new ArrayList<Integer>();
 		
 		int nodeNum = map.getWid() * map.getHei();
 		boolean[] seen = new boolean[nodeNum];
 		
 		for(int i = 0 ; i < nodeNum ; i++){
 			out.add(-1);
 			firstIn.add(new ArrayList<Integer>());
 			outAct.add(null);
 			in.add(-1);
 		}
 		for(Action act : actions){
 			Agent agent = null;
 			try {
 				agent = getAgent(act.getTeamId(), act.getId());
 			} catch (IllegalAgentException e) {
 				// TODO ask mina
 				e.printStackTrace();
 				continue ;
 			}
 			//--------------------------------------------------
 			Point loc = agent.getLocation();
 			Point dest = loc.applyDirection(act.getDir());
 			if(map.isInsideMap(dest)){
 				int locNum = getNodeNum(loc);
 				int destNum = getNodeNum(dest);
 //				System.out.println("Action");
 //				System.out.println(loc);
 //				System.out.println(locNum);
 //				System.out.println(dest);
 //				System.out.println(destNum);
 				out.set(locNum, destNum);
 				outAct.set(locNum, act);
 				firstIn.get(destNum).add(locNum);
 			}
 		}
 		
 		for(int i = 0 ; i < firstIn.size() ; i++){
 			ArrayList<Integer> list = firstIn.get(i);
 			if(list.size() > 1){
 				Random r = new Random();
 				int randNei = list.get(r.nextInt(list.size()));
 				in.set(i, randNei);
 				for(Integer integ : list){
 					if(integ != randNei){
 						out.set(integ, -1);
 						outAct.set(integ, null);
 					}
 				}
 			}
 			else if(list.size() == 1){
 				in.set(i, list.get(0));
 			}
 		}
 		/*System.out.println("---------------Graph-------------");
 		for(int i = 0 ; i < firstIn.size() ; i++){
 			System.out.print(firstIn.get(i) + " ");
 		}
 		System.out.println();
 		System.out.println("-----------------out--------------");
 		for(int i = 0 ; i < out.size() ; i++){
 			System.out.print(out.get(i) + " ");
 		}
 		System.out.println("\n----------------in-----------");
 		for(int i = 0 ; i < in.size() ; i++){
 			System.out.print(in.get(i) + " ");
 		}
 		System.out.println("\n--------------------------------");
 		*/
 		ArrayList<Integer> routeStart = new ArrayList<Integer>();
 		for(int i = 0 ; i < in.size() ; i++){
 			if(in.get(i) == -1){
 				routeStart.add(i);
 			}
 		}
 //		System.out.println("---------Routes--------");
 		
 		for(int i = 0 ; i < routeStart.size() ; i++){
 			//find route
 			ArrayList<Action> routeActs = new ArrayList<Action>();
 			int node = routeStart.get(i);
 		//	System.out.println("Start node ");
 		//	System.out.println(node);
 			while(out.get(node) >= 0){
 				routeActs.add(outAct.get(node));
 				seen[node] = true;
 				node = out.get(node);
 			//	System.out.print(node + " ");
 			}
 	//		System.out.println();
 	//		System.out.println(routeActs);
 			for(int j = routeActs.size() - 1 ; j >= 0 ; j--){
 				moveAgent(routeActs.get(j));
 			}
 		}
 		
 		for(int i = 0 ; i < nodeNum ; i++){
 			ArrayList<Action> cycleActs = new ArrayList<Action>();
 			if(!seen[i]){
 				int node = i;
 				while(!seen[node] && out.get(node) >= 0){
 					seen[node] = true;
 					cycleActs.add(outAct.get(node));
 					node = out.get(node);
 				}
 			}
 			if(cycleActs.size() > 2){
 				Action firstAct = cycleActs.get(0);
 			//	System.out.println("REMOVE " + getAgent(firstAct.getTeamId(), firstAct.getId()).getLocation());
 				Agent agent = null ;
 				try {
 					agent = getAgent(firstAct.getTeamId(), firstAct.getId()) ;
 				} catch (IllegalAgentException e) {
 					// TODO Auto-generated catch block what should I do?! 
 					e.printStackTrace();
 				}
 				game.setAgent(agent.getLocation(), null);
 				for(int j = cycleActs.size() - 1; j > 0; j--){
 					System.out.println(cycleActs.get(j));
 					moveAgent(cycleActs.get(j));
 				}
 			//	System.out.println("MOVES DONE");
 				
 				Agent a = null ;
 				try {
 					a = getAgent(firstAct.getTeamId(), firstAct.getId());
 				} catch (IllegalAgentException e) {
 					// TODO Auto-generated catch block what should I do?! 
 					e.printStackTrace();
 				}
 				Point dest = a.getLocation().applyDirection(firstAct.getDir());
 			//	System.out.println("Destionation " + dest);
 			//	System.out.println(a.getLocation());
 			//	System.out.println("DONE");
 				a.setLocation(dest);
 				game.setAgent(dest, a);
 				Integer id = new Integer(a.getId()) ;
 				graphicClient.move(id, firstAct.getDir()) ;
 			//	System.out.println("DONE");
 			}
 		}
 	}
 	
 	private int getNodeNum(Point p){
 		return p.y * map.getWid() + p.x;
 	}
 	
 	public int getTeamScore(){
 		return teams.get(0).getScore();
 	}
 
 }
 
