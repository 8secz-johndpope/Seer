 package rescuecore2.standard.kernel;
 
 import java.util.Set;
 import java.util.HashSet;
 
 import java.util.regex.PatternSyntaxException;
 
 import kernel.AgentRegistrar;
 import kernel.ComponentManager;
 import kernel.KernelException;
 
 import rescuecore2.config.Config;
 import rescuecore2.worldmodel.WorldModel;
 import rescuecore2.worldmodel.Entity;
 import rescuecore2.worldmodel.Property;
 import rescuecore2.Constants;
 
 import rescuecore2.standard.entities.FireBrigade;
 import rescuecore2.standard.entities.FireStation;
 import rescuecore2.standard.entities.AmbulanceTeam;
 import rescuecore2.standard.entities.AmbulanceCentre;
 import rescuecore2.standard.entities.PoliceForce;
 import rescuecore2.standard.entities.PoliceOffice;
 import rescuecore2.standard.entities.Civilian;
 import rescuecore2.standard.entities.Human;
 import rescuecore2.standard.entities.Road;
 import rescuecore2.standard.entities.Area;
 import rescuecore2.standard.entities.Building;
 import rescuecore2.standard.entities.StandardPropertyURN;
 import rescuecore2.standard.entities.StandardEntityURN;
 import rescuecore2.standard.entities.StandardWorldModel;
 
 import rescuecore2.standard.StandardConstants;
 
 /**
    Class that registers standard agents.
  */
 public class StandardAgentRegistrar implements AgentRegistrar {
     private static final Set<String> VISIBLE_CONFIG_OPTIONS = new HashSet<String>();
 
     static {
         VISIBLE_CONFIG_OPTIONS.add("kernel\\.agents\\.think-time");
         VISIBLE_CONFIG_OPTIONS.add("kernel\\.agents\\.ignoreuntil");
         VISIBLE_CONFIG_OPTIONS.add("kernel\\.startup\\.connect-time");
         VISIBLE_CONFIG_OPTIONS.add(Constants.COMMUNICATION_MODEL_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add(Constants.PERCEPTION_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add("fire\\.tank\\.maximum");
         VISIBLE_CONFIG_OPTIONS.add("fire\\.tank\\.refill-rate");
         VISIBLE_CONFIG_OPTIONS.add("fire\\.extinguish\\.max-sum");
         VISIBLE_CONFIG_OPTIONS.add("fire\\.extinguish\\.max-distance");
         VISIBLE_CONFIG_OPTIONS.add(StandardConstants.FIRE_BRIGADE_COUNT_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add(StandardConstants.FIRE_STATION_COUNT_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add(StandardConstants.AMBULANCE_TEAM_COUNT_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add(StandardConstants.AMBULANCE_CENTRE_COUNT_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add(StandardConstants.POLICE_FORCE_COUNT_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add(StandardConstants.POLICE_OFFICE_COUNT_KEY.replace(".", "\\."));
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.count");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.max\\.platoon");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.max\\.centre");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.\\d+\\.type");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.\\d+\\.range");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.\\d+\\.messages\\.size");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.\\d+\\.messages\\.max");
         VISIBLE_CONFIG_OPTIONS.add("comms\\.channels\\.\\d+\\.bandwidth");
         VISIBLE_CONFIG_OPTIONS.add("clear\\.repair\\.rate");
         VISIBLE_CONFIG_OPTIONS.add("clear\\.repair\\.distance");
     }
 
     @Override
     public void registerAgents(WorldModel<? extends Entity> world, Config config, ComponentManager manager) throws KernelException {
         StandardWorldModel model = StandardWorldModel.createStandardWorldModel(world);
         Config agentConfig = new Config(config);
         try {
             agentConfig.removeExceptRegex(VISIBLE_CONFIG_OPTIONS);
         }
         catch (PatternSyntaxException e) {
             throw new KernelException(e);
         }
         agentConfig.setIntValue(StandardConstants.FIRE_BRIGADE_COUNT_KEY, model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size());
         agentConfig.setIntValue(StandardConstants.FIRE_STATION_COUNT_KEY, model.getEntitiesOfType(StandardEntityURN.FIRE_STATION).size());
         agentConfig.setIntValue(StandardConstants.AMBULANCE_TEAM_COUNT_KEY, model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).size());
         agentConfig.setIntValue(StandardConstants.AMBULANCE_CENTRE_COUNT_KEY, model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE).size());
         agentConfig.setIntValue(StandardConstants.POLICE_FORCE_COUNT_KEY, model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).size());
         agentConfig.setIntValue(StandardConstants.POLICE_OFFICE_COUNT_KEY, model.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE).size());
         Set<Entity> initialEntities = new HashSet<Entity>();
         for (Entity e : world) {
             maybeAddInitialEntity(e, initialEntities);
         }
         for (Entity e : world) {
             if (e instanceof FireBrigade
                     || e instanceof FireStation
                     || e instanceof AmbulanceTeam
                     || e instanceof AmbulanceCentre
                     || e instanceof PoliceForce
                     || e instanceof PoliceOffice
                     || e instanceof Civilian) {
                 Set<Entity> s = new HashSet<Entity>(initialEntities);
                 s.add(e);
                 manager.registerAgentControlledEntity(e, s, agentConfig);
             }
         }
     }
 
     private void maybeAddInitialEntity(Entity e, Set<Entity> initialEntities) {
         if (e instanceof Road) {
             Road r = (Road)e.copy();
             filterAreaProperties(r);
             initialEntities.add(r);
         }
         if (e instanceof Building) {
             Building b = (Building)e.copy();
             filterBuildingProperties(b);
             initialEntities.add(b);
         }
         if (e instanceof Human) {
             if (!(e instanceof Civilian)) {
                 Human h = (Human)e.copy();
                 filterHumanProperties(h);
                 initialEntities.add(h);
             }
         }
     }
 
     private void filterAreaProperties(Area a) {
         for (Property next : a.getProperties()) {
             // Hide blockades
             StandardPropertyURN urn = StandardPropertyURN.fromString(next.getURN());
             switch (urn) {
             case BLOCKADES:
                 next.undefine();
                 break;
             default:
                 break;
             }
         }
     }
 
     private void filterBuildingProperties(Building b) {
         filterAreaProperties(b);
         for (Property next : b.getProperties()) {
             // Hide ignition, fieryness, brokenness, temperature
             StandardPropertyURN urn = StandardPropertyURN.fromString(next.getURN());
             switch (urn) {
             case IGNITION:
             case FIERYNESS:
             case BROKENNESS:
             case TEMPERATURE:
                 next.undefine();
                 break;
             default:
                 // Ignore
             }
         }
     }
 
     private void filterHumanProperties(Human h) {
         for (Property next : h.getProperties()) {
            // Human properties: POSITION, X, Y
             // Everything else should be undefined
             StandardPropertyURN urn = StandardPropertyURN.fromString(next.getURN());
             switch (urn) {
             case X:
             case Y:
             case POSITION:
                 break;
             default:
                 next.undefine();
             }
         }
     }
 }
