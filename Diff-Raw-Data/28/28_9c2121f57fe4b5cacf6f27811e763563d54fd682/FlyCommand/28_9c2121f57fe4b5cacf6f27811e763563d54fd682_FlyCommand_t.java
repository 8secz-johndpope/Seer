 package net.aufdemrand.denizen.scripts.commands.entity;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import net.aufdemrand.denizen.exceptions.CommandExecutionException;
 import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
 import net.aufdemrand.denizen.objects.Element;
 import net.aufdemrand.denizen.objects.aH;
 import net.aufdemrand.denizen.objects.dEntity;
 import net.aufdemrand.denizen.objects.dList;
 import net.aufdemrand.denizen.objects.dLocation;
 import net.aufdemrand.denizen.objects.dPlayer;
 import net.aufdemrand.denizen.scripts.ScriptEntry;
 import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
 import net.aufdemrand.denizen.utilities.Conversion;
 import net.aufdemrand.denizen.utilities.debugging.dB;
 import net.aufdemrand.denizen.utilities.debugging.dB.Messages;
 import net.aufdemrand.denizen.utilities.entity.Position;
 import net.aufdemrand.denizen.utilities.entity.Rotation;
 
 import org.bukkit.Location;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.scheduler.BukkitRunnable;
 import org.bukkit.util.Vector;
 
 /**
  * Make an entity fly where its controller is looking,
  * or - alternatively - make it fly between different destinations.
  *
  * @author David Cernat
  */
 
 public class FlyCommand extends AbstractCommand {
 
     @Override
     public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
 
         // Initialize necessary fields
 
         for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {
 
             if (!scriptEntry.hasObject("cancel")
                 && arg.matches("cancel")) {
 
                 scriptEntry.addObject("cancel", "");
             }
 
             else if (!scriptEntry.hasObject("destinations")
                      && arg.matchesPrefix("destination, destinations, d")) {
 
                 scriptEntry.addObject("destinations", ((dList) arg.asType(dList.class)).filter(dLocation.class));
             }
             
             else if (!scriptEntry.hasObject("controller")
                      && arg.matchesArgumentType(dPlayer.class)
                      && arg.matchesPrefix("controller, c")) {
                 
                scriptEntry.addObject("controller", (arg.asType(dPlayer.class)));
             }
 
             else if (!scriptEntry.hasObject("origin")
                      && arg.matchesArgumentType(dLocation.class)) {
 
                 scriptEntry.addObject("origin", arg.asType(dLocation.class));
             }
 
             else if (!scriptEntry.hasObject("entities")
                      && arg.matchesArgumentList(dEntity.class)) {
 
                 scriptEntry.addObject("entities", ((dList) arg.asType(dList.class)).filter(dEntity.class));
             }
 
             else if (!scriptEntry.hasObject("speed")
                      && arg.matchesPrimitive(aH.PrimitiveType.Double)) {
 
                 scriptEntry.addObject("speed", arg.asElement());
             }
         }
 
         // Use the NPC or player's locations as the location if one is not specified
 
         scriptEntry.defaultObject("origin",
                 scriptEntry.hasPlayer() ? scriptEntry.getPlayer().getLocation() : null,
                 scriptEntry.hasNPC() ? scriptEntry.getNPC().getLocation() : null);
         
         // Use a default speed of 1.2 if one is not specified
 
         scriptEntry.defaultObject("speed", new Element(1.2));
 
         // Check to make sure required arguments have been filled
 
         if (!scriptEntry.hasObject("entities"))
             throw new InvalidArgumentsException(Messages.ERROR_MISSING_OTHER, "ENTITIES");
         if (!scriptEntry.hasObject("origin"))
             throw new InvalidArgumentsException(Messages.ERROR_MISSING_OTHER, "ORIGIN");
     }
 
     @SuppressWarnings("unchecked")
     @Override
     public void execute(final ScriptEntry scriptEntry) throws CommandExecutionException {
         // Get objects
 
         dLocation origin = (dLocation) scriptEntry.getObject("origin");
         List<dEntity> entities = (List<dEntity>) scriptEntry.getObject("entities");
         final List<dLocation> destinations = scriptEntry.hasObject("destinations") ?
                                 (List<dLocation>) scriptEntry.getObject("destinations") :
                                 new ArrayList<dLocation>();
         
         // Set freeflight to true only if there are no destinations
        final boolean freeflight = destinations.size() < 1;
                                 
         dEntity controller = (dEntity) scriptEntry.getObject("controller");
         
         // If freeflight is on, we need to do some checks
         if (freeflight) {
             
             // If no controller was set, we need someone to control the
             // flying entities, so try to find a player in the entity list
             if (controller == null) {
                 for (dEntity entity : entities) {
                     if (entity.isPlayer()) {
                        // If this player will be a rider on something, and will not
                        // be at the bottom ridden by the other entities, set it as
                        // the controller
                        if (entities.get(entities.size() - 1) != entity) {
                            controller = entity;
                            dB.report(getName(), "Flight control defaulting to " + controller);
                            break;
                        }
                     }
                 }
             
                 // If the controller is still null, we cannot continue
                 if (controller == null) {
                     dB.report(getName(), "There is no one to control the flight's path!");
                     return;
                 }
             }
             
             // Else, if the controller was set, we need to make sure
             // it is among the flying entities, and add it if it is not
             else {
                 boolean found = false;
             
                 for (dEntity entity : entities) {
                     if (entity.identify().equals(controller.identify())) {
                         found = true;
                         break;
                     }
                 }
                 
                 // Add the controller to the entity list
                 if (!found) {
                     dB.report(getName(), "Adding controller " + controller + " to flying entities.");
                     entities.add(0, controller);
                 }
             }
         }
 
         final Element speed = (Element) scriptEntry.getObject("speed");
         Boolean cancel = scriptEntry.hasObject("cancel");
 
         // Report to dB
         dB.report(getName(), (cancel == true ? aH.debugObj("cancel", cancel) : "") +
                              aH.debugObj("origin", origin) +
                              aH.debugObj("entities", entities.toString()) +
                              aH.debugObj("speed", speed) +
                              (freeflight ? aH.debugObj("controller", controller)
                                          : aH.debugObj("destinations", destinations.toString())));
 
         // Mount or dismount all of the entities
         if (cancel.equals(false)) {
 
             // Go through all the entities, spawning/teleporting them
             for (dEntity entity : entities) {
 
                 if (!entity.isSpawned()) {
                     entity.spawnAt(origin);
                 }
                 else {
                     entity.teleport(origin);
                 }
             }
 
             Position.mount(Conversion.convert(entities));
         }
         else {
             Position.dismount(Conversion.convert(entities));
 
             // Go no further if we are dismounting entities
             return;
         }
 
         // Get the last entity on the list
         final Entity entity = entities.get(entities.size() - 1).getBukkitEntity();
        final LivingEntity finalController = controller != null ? controller.getLivingEntity() : null;
         
         BukkitRunnable task = new BukkitRunnable() {
 
             Location location = null;
             Boolean flying = true;
 
             public void run() {
 
                 if (freeflight) {
 
                     // If freeflight is on, and the flying entity
                     // is ridden by another entity, let it keep
                     // flying where the controller is looking
                     
                     if (!entity.isEmpty() && finalController.isInsideVehicle()) {
                         location = finalController.getEyeLocation()
                                      .add(finalController.getEyeLocation().getDirection()
                                      .multiply(30));
                     }
                     else {
                         flying = false;
                     }
                 }
                 else {
 
                     // If freelight is not on, keep flying only as long
                     // as there are destinations left
 
                     if (destinations.size() > 0) {
                         location = destinations.get(0);
                     }
                     else {
                         flying = false;
                     }
                 }
 
                 if (flying && entity.isValid()) {
 
                     // To avoid excessive turbulence, only have the entity rotate
                     // when it really needs to
                     if (!Rotation.isFacingLocation(entity, location, 50)) {
 
                         Rotation.faceLocation(entity, location);
                     }
 
                     Vector v1 = entity.getLocation().toVector();
                     Vector v2 = location.toVector();
                     Vector v3 = v2.clone().subtract(v1).normalize().multiply(speed.asDouble());
 
                     entity.setVelocity(v3);
 
                     // If freeflight is off, check if the entity has reached its
                     // destination, and remove the destination if that happens
                     // to be the case
 
                     if (!freeflight) {
 
                         if (Math.abs(v2.getX() - v1.getX()) < 2 && Math.abs(v2.getY() - v1.getY()) < 2
                             && Math.abs(v2.getZ() - v1.getZ()) < 2) {
 
                             destinations.remove(0);
                         }
                     }
                 }
                 else {
 
                     flying = false;
                     this.cancel();
                 }
             }
         };
 
            task.runTaskTimer(denizen, 0, 3);
     }
 
 }
