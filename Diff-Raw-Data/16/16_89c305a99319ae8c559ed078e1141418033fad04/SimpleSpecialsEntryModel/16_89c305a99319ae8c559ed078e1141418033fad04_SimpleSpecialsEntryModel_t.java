 package net.sf.anathema.charmentry.model;
 
 import net.sf.anathema.character.generic.magic.charms.type.TurnType;
 import net.sf.anathema.charmentry.presenter.ISimpleSpecialsEntryModel;
 import net.sf.anathema.lib.control.change.ChangeControl;
 import net.sf.anathema.lib.control.change.IChangeListener;
 
 public class SimpleSpecialsEntryModel implements ISimpleSpecialsEntryModel {
   private int speed = DEFAULT_SPEED;
   private int defenseModifier = DEFAULT_DEFENSE_MODIFIER;
   private final ISimpleSpecialsArbitrator arbitrator;
   private final ChangeControl control = new ChangeControl();
  private final TurnType turnType = TurnType.Tick;
 
   public SimpleSpecialsEntryModel(ISimpleSpecialsArbitrator arbitrator) {
     this.arbitrator = arbitrator;
     arbitrator.addModelChangeListener(new IChangeListener() {
       public void changeOccured() {
         control.fireChangedEvent();
       }
     });
   }
 
   public void reset() {
     this.speed = DEFAULT_SPEED;
     this.defenseModifier = DEFAULT_DEFENSE_MODIFIER;
     control.fireChangedEvent();
   }
 
   public void setSpeed(int newValue) {
     if (arbitrator.isSimpleSpecialsAvailable()) {
       this.speed = newValue;
       control.fireChangedEvent();
     }
   }
 
   public void setDefenseValue(int newValue) {
     if (arbitrator.isSimpleSpecialsAvailable()) {
       this.defenseModifier = newValue;
       control.fireChangedEvent();
     }
   }
 
   public TurnType getTurnType() {
    return turnType;
   }
 
   public int getDefenseModifier() {
     return defenseModifier;
   }
 
   public int getSpeed() {
     return speed;
   }
 
   public void addChangeListener(IChangeListener listener) {
     control.addChangeListener(listener);
   }
 }
