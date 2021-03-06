 package net.sf.anathema.character.generic.framework.reporting.template.voidstate;
 
 import java.io.InputStream;
 import java.util.List;
 
 import net.sf.anathema.character.generic.equipment.IWeaponType;
 import net.sf.anathema.character.generic.health.HealthType;
 import net.sf.anathema.character.generic.impl.equipment.MeleeWeaponType;
 import net.sf.anathema.character.generic.traits.types.AbilityType;
 import net.sf.anathema.character.generic.type.CharacterType;
 
 public class DefaultVoidstateSubreports implements IVoidstateSubreports {
 
   public static final String FILE_BASE = "net/sf/anathema/character/reportdesigns/voidstate/"; //$NON-NLS-1$
   private final CharacterType characterType;
 
   public DefaultVoidstateSubreports(CharacterType characterType) {
     this.characterType = characterType;
   }
 
   public InputStream loadAttributeSubreport() {
     String abilitySubreportResourceLocation = FILE_BASE + "VoidstateDefaultAttributeSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(abilitySubreportResourceLocation);
   }
 
   public InputStream loadAbilitySubreport() {
     String abilitySubreportResourceLocation = FILE_BASE + "VoidstateAbilitySubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(abilitySubreportResourceLocation);
   }
 
   public InputStream loadAbilitySetSubreport() {
     String abilitySubreportResourceLocation = FILE_BASE + "VoidstateFiveGroupAbilitySetSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(abilitySubreportResourceLocation);
   }
 
   public InputStream loadFiveAbilityGroupSubreport() {
     String abilitySubreportResourceLocation = FILE_BASE + "VoidstateFiveAbilityGroupSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(abilitySubreportResourceLocation);
   }
 
   public InputStream loadTenAbilityGroupSubreport() {
     String abilitySubreportResourceLocation = FILE_BASE + "VoidstateTenAbilityGroupSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(abilitySubreportResourceLocation);
   }
 
   public InputStream loadSingleAbilitySubreport() {
     String abilitySubreportResourceLocation = FILE_BASE + "VoidstateSingleAbilitySubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(abilitySubreportResourceLocation);
   }
 
   public InputStream loadCombatStatsSubreport() {
     String subreportResourceLocation = FILE_BASE + "VoidstateCombatStatsSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(subreportResourceLocation);
   }
 
   public InputStream loadMiddleColumnSubreport() {
     String middleColumnSubreportResourceLocation = FILE_BASE + "DefaultMiddleColumnSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(middleColumnSubreportResourceLocation);
   }
 
   public InputStream loadWillpowerSubreport() {
     String willpowerSubreportResourceLocation = FILE_BASE + "VoidstateDefaultWillpowerSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(willpowerSubreportResourceLocation);
   }
 
   public InputStream loadVirtuesSubreport() {
     String virtuesSubreportResourceLocation = FILE_BASE + "VoidstateVirtuesSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(virtuesSubreportResourceLocation);
   }
 
   public InputStream loadAnimaSubreport() {
     String animaSubreportResourceLocation = FILE_BASE + "VoidState" + getCharacterTypeName() + "AnimaSubreport.jasper"; //$NON-NLS-1$ //$NON-NLS-2$;
     return loadSystemResource(animaSubreportResourceLocation);
   }
 
   public InputStream loadBrawlSubreport() {
     String brawlSubreportResourceLocation = FILE_BASE + "VoidStateBrawlSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(brawlSubreportResourceLocation);
   }
 
   public InputStream loadHealthSubreport() {
     String healthSubreportResourceLocation = FILE_BASE + "VoidstateDefaultHealthSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(healthSubreportResourceLocation);
   }
 
   public InputStream loadFlawSubreport() {
     String flawSubreportResourceLocation = FILE_BASE + "VoidState" + getCharacterTypeName() + "FlawSubreport.jasper"; //$NON-NLS-1$ //$NON-NLS-2$;
     return loadSystemResource(flawSubreportResourceLocation);
   }
 
   public InputStream loadSequenceSubreport() {
     String sequenceSubreportResourceLocation = FILE_BASE + "VoidStateSequenceSubreport.jasper"; //$NON-NLS-1$
     return loadSystemResource(sequenceSubreportResourceLocation);
   }
 
   public InputStream loadDescriptionSubreport() {
    String descriptionSubreportResourceLocation = FILE_BASE + "VoidStateDefaultDescriptionSubreport.jasper"; //$NON-NLS-1$;
     return loadSystemResource(descriptionSubreportResourceLocation);
   }
 
   protected final InputStream loadSystemResource(String resourceLocation) {
     return getClass().getClassLoader().getResourceAsStream(resourceLocation);
   }
 
   protected final String getCharacterTypeName() {
     return characterType.name();
   }
 
   public void buildPowerCombatBrawlWeaponList(List<IWeaponType> weapons) {
     weapons.add(new MeleeWeaponType("Weapons.Brawl.Fist", AbilityType.Brawl, 0, 1, 0, HealthType.Bashing, 2, 5)); //$NON-NLS-1$
     weapons.add(new MeleeWeaponType("Weapons.Brawl.Clinch", AbilityType.Brawl, -6, 0, 2, HealthType.Bashing, 0, 1)); //$NON-NLS-1$
     weapons.add(new MeleeWeaponType("Weapons.Brawl.Kick", AbilityType.Brawl, -3, 1, 3, HealthType.Bashing, -3, 3)); //$NON-NLS-1$
   }
 
   public void buildCoreRulesBrawlWeaponList(List<IWeaponType> weapons) {
     weapons.add(new MeleeWeaponType("Weapons.Brawl.Fist", AbilityType.Brawl, 0, 0, 0, HealthType.Bashing, 0, null)); //$NON-NLS-1$
     weapons.add(new MeleeWeaponType("Weapons.Brawl.Clinch", AbilityType.Brawl, 0, 0, 2, HealthType.Bashing, 0, null)); //$NON-NLS-1$
     weapons.add(new MeleeWeaponType("Weapons.Brawl.Kick", AbilityType.Brawl, -3, -1, 2, HealthType.Bashing, -1, null)); //$NON-NLS-1$
   }
 
   public InputStream loadCharmpageSubreport() {
     String charmpageSubreportResourceLocation = FILE_BASE + "VoidstateCharmPageSubreport.jasper"; //$NON-NLS-1$;
     return loadSystemResource(charmpageSubreportResourceLocation);
   }
 
   public InputStream loadSpecialpageSubreport() {
     String nullSubreportResourceLocation = FILE_BASE + "VoidstateNullPage.jasper"; //$NON-NLS-1$;
     return loadSystemResource(nullSubreportResourceLocation);
   }
 }
