 package net.sf.anathema.character.impl.model.charm;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import net.sf.anathema.character.generic.IBasicCharacterData;
 import net.sf.anathema.character.generic.caste.ICasteType;
 import net.sf.anathema.character.generic.framework.additionaltemplate.listening.ConfigurableCharacterChangeListener;
 import net.sf.anathema.character.generic.framework.additionaltemplate.model.ICharacterModelContext;
 import net.sf.anathema.character.generic.framework.additionaltemplate.model.ICharmLearnStrategy;
 import net.sf.anathema.character.generic.impl.magic.Charm;
 import net.sf.anathema.character.generic.impl.magic.MartialArtsUtilities;
 import net.sf.anathema.character.generic.impl.magic.charm.CharmTree;
 import net.sf.anathema.character.generic.impl.magic.charm.MartialArtsCharmTree;
 import net.sf.anathema.character.generic.impl.template.magic.ICharmProvider;
 import net.sf.anathema.character.generic.magic.ICharm;
 import net.sf.anathema.character.generic.magic.ICharmData;
 import net.sf.anathema.character.generic.magic.charms.ICharmAttributeRequirement;
 import net.sf.anathema.character.generic.magic.charms.ICharmGroup;
 import net.sf.anathema.character.generic.magic.charms.ICharmTree;
 import net.sf.anathema.character.generic.magic.charms.MartialArtsLevel;
 import net.sf.anathema.character.generic.magic.charms.special.IMultiLearnableCharm;
 import net.sf.anathema.character.generic.magic.charms.special.IOxBodyTechniqueCharm;
 import net.sf.anathema.character.generic.magic.charms.special.IPainToleranceCharm;
 import net.sf.anathema.character.generic.magic.charms.special.ISpecialCharm;
 import net.sf.anathema.character.generic.magic.charms.special.ISpecialCharmConfiguration;
 import net.sf.anathema.character.generic.magic.charms.special.ISpecialCharmVisitor;
 import net.sf.anathema.character.generic.rules.IExaltedRuleSet;
 import net.sf.anathema.character.generic.template.ICharacterTemplate;
 import net.sf.anathema.character.generic.template.ITemplateRegistry;
 import net.sf.anathema.character.generic.template.IUnsupportedTemplate;
 import net.sf.anathema.character.generic.template.magic.ICharmTemplate;
 import net.sf.anathema.character.generic.traits.IGenericTrait;
 import net.sf.anathema.character.generic.traits.ITraitType;
 import net.sf.anathema.character.generic.traits.types.OtherTraitType;
 import net.sf.anathema.character.generic.type.CharacterType;
 import net.sf.anathema.character.impl.model.charm.prerequisite.PrerequisiteSetBuilder;
 import net.sf.anathema.character.impl.model.charm.special.MultiLearnableCharmConfiguration;
 import net.sf.anathema.character.impl.model.charm.special.OxBodyTechniqueConfiguration;
 import net.sf.anathema.character.model.charm.CharmLearnAdapter;
 import net.sf.anathema.character.model.charm.ICharmConfiguration;
 import net.sf.anathema.character.model.charm.ICharmLearnListener;
 import net.sf.anathema.character.model.charm.ILearningCharmGroup;
 import net.sf.anathema.character.model.health.IHealthConfiguration;
 import net.sf.anathema.character.model.health.IPainToleranceProvider;
 import net.sf.anathema.lib.control.change.ChangeControl;
 import net.sf.anathema.lib.control.change.IChangeListener;
 import net.sf.anathema.lib.lang.ArrayUtilities;
 
 public class CharmConfiguration implements ICharmConfiguration {
 
  private final ICharmTree<ICharm> martialArtsCharmTree;
   private final Map<CharacterType, ICharmTree> alienTreesByType = new HashMap<CharacterType, ICharmTree>();
   private final Map<CharacterType, ILearningCharmGroup[]> nonMartialArtsGroupsByType = new HashMap<CharacterType, ILearningCharmGroup[]>();
   private final Map<CharacterType, ICharmTemplate> templatesByType = new HashMap<CharacterType, ICharmTemplate>();
   private CharacterType[] types;
   private final ILearningCharmGroupContainer learningCharmGroupContainer = new ILearningCharmGroupContainer() {
     public ILearningCharmGroup getLearningCharmGroup(ICharm charm) {
       return getGroup(charm);
     }
   };
   private ILearningCharmGroup[] martialArtsGroups;
   private final ICharacterModelContext context;
   private final IHealthConfiguration health;
   private final ChangeControl control = new ChangeControl();
   private final ICharmProvider provider;
 
   private final ICharmLearnListener learnableListener = new CharmLearnAdapter() {
     @Override
     public void charmForgotten(ICharm charm) {
       fireLearnConditionsChanged();
     }
 
     @Override
     public void charmLearned(ICharm charm) {
       fireLearnConditionsChanged();
     }
   };
 
   public CharmConfiguration(
       IHealthConfiguration health,
       ICharacterModelContext context,
       ITemplateRegistry registry,
       ICharmProvider provider) {
     this.health = health;
     this.context = context;
     this.provider = provider;
     IExaltedRuleSet rules = context.getBasicCharacterContext().getRuleSet();
     List<CharacterType> allCharacterTypes = new ArrayList<CharacterType>();
     ICharmTemplate nativeCharmTemplate = getNativeCharmTemplate(registry);
     this.martialArtsCharmTree = new MartialArtsCharmTree(nativeCharmTemplate, rules);
     this.martialArtsGroups = createGroups(martialArtsCharmTree.getAllCharmGroups());
     initCharacterType(nativeCharmTemplate, rules, getNativeCharacterType());
     allCharacterTypes.add(getNativeCharacterType());
     initAlienTypes(registry, rules, allCharacterTypes);
     types = allCharacterTypes.toArray(new CharacterType[allCharacterTypes.size()]);
   }
 
   private ICharmTemplate getNativeCharmTemplate(ITemplateRegistry registry) {
     IBasicCharacterData basicCharacterContext = context.getBasicCharacterContext();
     return registry.getTemplate(
         basicCharacterContext.getTemplateType(),
         basicCharacterContext.getRuleSet().getEdition()).getMagicTemplate().getCharmTemplate();
   }
 
   public void addCharmLearnListener(ICharmLearnListener listener) {
     for (ILearningCharmGroup group : getAllGroups()) {
       group.addCharmLearnListener(listener);
     }
   }
 
   private void addSpecialCharmConfigurations(final CharacterType characterType) {
     for (ISpecialCharm specialCharm : provider.getSpecialCharms(characterType)) {
       specialCharm.accept(new ISpecialCharmVisitor() {
         public void acceptOxBodyTechnique(IOxBodyTechniqueCharm visited) {
           final ICharm charm = getCharmTree(characterType).getCharmByID(visited.getCharmId());
           IGenericTrait relevantTrait = getModelContext().getTraitCollection().getTrait(visited.getRelevantTrait());
           ILearningCharmGroup group = getGroupById(charm.getCharacterType(), charm.getGroupId());
           registerOxBodyTechnique(visited, charm, relevantTrait, group);
         }
 
         public void visitMultiLearnableCharm(IMultiLearnableCharm visitedCharm) {
           final ICharm charm = getCharmTree(characterType).getCharmByID(visitedCharm.getCharmId());
           ILearningCharmGroup group = getGroupById(charm.getCharacterType(), charm.getGroupId());
           registerMultiLearnableCharm(visitedCharm, charm, group);
         }
 
         public void visitPainToleranceCharm(IPainToleranceCharm visitedCharm) {
           final ICharm charm = getCharmTree(characterType).getCharmByID(visitedCharm.getCharmId());
           ILearningCharmGroup group = getGroupById(charm.getCharacterType(), charm.getGroupId());
           registerPainToleranceCharm(visitedCharm, charm, group);
         }
       });
     }
   }
 
   public boolean contains(ICharm charm) {
     CharacterType characterType = charm.getCharacterType();
     ICharm[] allCharms = alienTreesByType.get(characterType).getAllCharms();
     for (ICharm alienCharm : allCharms) {
       if (alienCharm == charm) {
         return true;
       }
     }
     return false;
   }
 
   private ILearningCharmGroup[] createGroups(ICharmGroup[] charmGroups) {
     List<ILearningCharmGroup> newGroups = new ArrayList<ILearningCharmGroup>();
     for (ICharmGroup charmGroup : charmGroups) {
       newGroups.add(new LearningCharmGroup(getLearnStrategy(), charmGroup, this, learningCharmGroupContainer));
     }
     return newGroups.toArray(new LearningCharmGroup[0]);
   }
 
   private ICharm[] getAllCharms() {
     List<ICharm> charms = new ArrayList<ICharm>();
     for (ICharmTree tree : alienTreesByType.values()) {
       charms.addAll(Arrays.asList(tree.getAllCharms()));
     }
     return charms.toArray(new ICharm[charms.size()]);
   }
 
   public ILearningCharmGroup[] getAllGroups() {
     List<ILearningCharmGroup> allGroups = new ArrayList<ILearningCharmGroup>();
     for (ILearningCharmGroup[] groups : nonMartialArtsGroupsByType.values()) {
       allGroups.addAll(Arrays.asList(groups));
     }
     allGroups.addAll(Arrays.asList(martialArtsGroups));
     return allGroups.toArray(new ILearningCharmGroup[allGroups.size()]);
   }
 
   private ICharmTemplate getCharmTemplate(CharacterType type) {
     return templatesByType.get(type);
   }
 
   private CharacterType getCharacterType(String charmId) {
     String characterType = charmId.substring(0, charmId.indexOf(".")); //$NON-NLS-1$
     for (CharacterType type : types) {
       if (type.getId().equals(characterType)) {
         return type;
       }
     }
     throw new IllegalArgumentException("Charm Id did not contain a valid character type"); //$NON-NLS-1$
   }
 
   public ICharm getCharmById(String charmId) {
     ICharm martialArtsCharm = martialArtsCharmTree.getCharmByID(charmId);
     if (martialArtsCharm != null) {
       return martialArtsCharm;
     }
     CharacterType characterType = getCharacterType(charmId);
     return getCharmTree(characterType).getCharmByID(charmId);
   }
 
   public ICharmTree getCharmTree(CharacterType type) {
     return alienTreesByType.get(type);
   }
 
   public ICharm[] getCreationLearnedCharms() {
     List<ICharm> allLearnedCharms = new ArrayList<ICharm>();
     for (ILearningCharmGroup group : getAllGroups()) {
       allLearnedCharms.addAll(Arrays.asList(group.getCreationLearnedCharms()));
     }
     return allLearnedCharms.toArray(new ICharm[0]);
   }
 
   public ICharm[] getExperienceLearnedCharms() {
     List<ICharm> allLearnedCharms = new ArrayList<ICharm>();
     for (ILearningCharmGroup group : getAllGroups()) {
       Collections.addAll(allLearnedCharms, group.getExperienceLearnedCharms());
     }
     return allLearnedCharms.toArray(new ICharm[0]);
   }
 
   public ILearningCharmGroup[] getNonMartialArtsGroups(CharacterType characterType) {
     return nonMartialArtsGroupsByType.get(characterType);
   }
 
   public ICharm[] getLearnedCharms(boolean experienced) {
     List<ICharm> allLearnedCharms = new ArrayList<ICharm>();
     for (ILearningCharmGroup group : getAllGroups()) {
       Collections.addAll(allLearnedCharms, group.getCreationLearnedCharms());
       if (experienced) {
         Collections.addAll(allLearnedCharms, group.getExperienceLearnedCharms());
       }
     }
     return allLearnedCharms.toArray(new ICharm[0]);
   }
 
   public ILearningCharmGroup[] getMartialArtsGroups() {
     return martialArtsGroups;
   }
 
   public ISpecialCharmConfiguration getSpecialCharmConfiguration(ICharm charm) {
     ILearningCharmGroup group = getGroupById(charm.getCharacterType(), charm.getGroupId());
     return group.getSpecialCharmConfiguration(charm);
   }
 
   public String[] getUncompletedCelestialMartialArtsGroups() {
     ICharm[] learnedCharms = getLearnedCharms(true);
     Set<String> uncompletedGroups = new HashSet<String>();
     for (ICharm charm : learnedCharms) {
       if (MartialArtsUtilities.isMartialArtsCharm(charm)) {
         boolean groupIsStyle = !charm.hasAttribute(ICharmData.NO_STYLE_ATTRIBUTE);
         boolean isCelestialLevel = MartialArtsUtilities.hasLevel(MartialArtsLevel.Celestial, charm);
         boolean groupIsIncomplete = !getGroup(charm).isCompleted();
         if (groupIsStyle && isCelestialLevel && groupIsIncomplete) {
           uncompletedGroups.add(charm.getGroupId());
         }
       }
     }
     return uncompletedGroups.toArray(new String[uncompletedGroups.size()]);
   }
 
   private void initCharacterType(ICharmTemplate charmTemplate, IExaltedRuleSet rules, CharacterType type) {
     CharmTree charmTree = new CharmTree(charmTemplate, rules);
     alienTreesByType.put(type, charmTree);
     ILearningCharmGroup[] groups = createGroups(charmTree.getAllCharmGroups());
     nonMartialArtsGroupsByType.put(type, groups);
     templatesByType.put(type, charmTemplate);
     addSpecialCharmConfigurations(type);
   }
 
   private ICharmTemplate getCharmTemplate(ITemplateRegistry registry, CharacterType type) {
     ICharacterTemplate defaultTemplate = registry.getDefaultTemplate(type, context.getBasicCharacterContext()
         .getRuleSet()
         .getEdition());
     if (defaultTemplate == null || defaultTemplate instanceof IUnsupportedTemplate) {
       return null;
     }
     return defaultTemplate.getMagicTemplate().getCharmTemplate();
   }
 
   private void initAlienTypes(ITemplateRegistry registry, IExaltedRuleSet rules, List<CharacterType> characterTypes) {
     for (CharacterType type : CharacterType.values()) {
       if (characterTypes.contains(type)) {
         continue;
       }
       ICharmTemplate charmTemplate = getCharmTemplate(registry, type);
       if (charmTemplate != null && charmTemplate.knowsCharms()) {
         initCharacterType(charmTemplate, rules, type);
         characterTypes.add(type);
       }
     }
   }
 
   private CharacterType getNativeCharacterType() {
     return getModelContext().getBasicCharacterContext().getCharacterType();
   }
 
   public void unlearnAllAlienCharms() {
     for (ILearningCharmGroup[] groups : nonMartialArtsGroupsByType.values()) {
       for (ILearningCharmGroup group : groups) {
         if (group.getCharacterType() != getNativeCharacterType()) {
           group.forgetAll();
         }
       }
     }
   }
 
   public ILearningCharmGroup getGroup(String characterType, String groupName) {
     return getGroupById(CharacterType.getById(characterType), groupName);
   }
 
   public CharacterType[] getCharacterTypes(boolean includeAlienTypes) {
     if (!includeAlienTypes) {
       return new CharacterType[] { getNativeCharacterType() };
     }
     return types;
 
   }
 
   public void initListening() {
     ConfigurableCharacterChangeListener changeListener = new ConfigurableCharacterChangeListener() {
       @Override
       public void configuredChangeOccured() {
         fireLearnConditionsChanged();
       }
     };
     changeListener.addTraitTypes(createPrerequisiteSet());
     getModelContext().getCharacterListening().addChangeListener(changeListener);
     addCharmLearnListener(learnableListener);
   }
 
   private Collection<ITraitType> createPrerequisiteSet() {
     PrerequisiteSetBuilder prerequisiteSetBuilder = new PrerequisiteSetBuilder();
     prerequisiteSetBuilder.addCharms(getAllCharms());
     return prerequisiteSetBuilder.getAllPrerequisites();
   }
 
   public void addLearnableListener(IChangeListener listener) {
     control.addChangeListener(listener);
   }
 
   private void addSpecialCharmConfiguration(
       ICharm charm,
       ILearningCharmGroup group,
       ISpecialCharmConfiguration configuration) {
     group.addSpecialCharmConfiguration(charm, configuration);
   }
 
   private void fireLearnConditionsChanged() {
     control.fireChangedEvent();
   }
 
   private final ICharmLearnStrategy getLearnStrategy() {
     return context.getCharmContext().getCharmLearnStrategy();
   }
 
   private final ICharacterModelContext getModelContext() {
     return context;
   }
 
   public boolean isFullfilled(ICharmAttributeRequirement requirement) {
     int count = 0;
     for (ICharm charm : getLearnedCharms(true)) {
       if (charm.hasAttribute(requirement.getAttribute())) {
         count++;
       }
       if (count >= requirement.getCount()) {
         return true;
       }
     }
     return false;
   }
 
   public final boolean isLearnable(ICharm charm) {
     if (isAlienCharm(charm)) {
       ICasteType casteType = getModelContext().getBasicCharacterContext().getCasteType();
       if (!getCharmTemplate(getNativeCharacterType()).isAllowedAlienCharms(casteType)) {
         return false;
       }
       if (charm.hasAttribute(Charm.NOT_ALIEN_LEARNABLE)) {
         return false;
       }
     }
     if (charm.isBlockedByAlternative(context.getMagicCollection())) {
       return false;
     }
     if (MartialArtsUtilities.isMartialArtsCharm(charm)) {
       boolean isSiderealFormCharm = MartialArtsUtilities.isFormCharm(charm)
           && MartialArtsUtilities.hasLevel(MartialArtsLevel.Sidereal, charm);
       if (isSiderealFormCharm && !isCelestialMartialArtsGroupCompleted()) {
         return false;
       }
       if (!getCharmTemplate(getNativeCharacterType()).isMartialArtsCharmAllowed(
           charm,
           getModelContext().getCharmContext(),
           getModelContext().getBasicCharacterContext().isExperienced())) {
         return false;
       }
     }
     ICharmAttributeRequirement[] attributeRequirements = charm.getAttributeRequirements();
     for (ICharmAttributeRequirement requirement : attributeRequirements) {
       if (!isFullfilled(requirement)) {
         return false;
       }
     }
     IGenericTrait[] prerequisites = charm.getPrerequisites();
     for (IGenericTrait prerequisite : prerequisites) {
       IGenericTrait prerequisiteTrait = context.getTraitCollection().getTrait(prerequisite.getType());
       if (prerequisite.getCurrentValue() > prerequisiteTrait.getCurrentValue()) {
         return false;
       }
       IGenericTrait essenceTrait = context.getTraitCollection().getTrait(OtherTraitType.Essence);
       if (charm.getEssence().getCurrentValue() > essenceTrait.getCurrentValue()) {
         return false;
       }
     }
     for (ICharm parentCharm : charm.getLearnPrerequisitesCharms(this)) {
       if (!isLearnable(parentCharm)) {
         return false;
       }
     }
     return true;
   }
 
   public boolean isLearnable(String charmId) {
     ICharm charm = getCharmById(charmId);
     if (charm == null) {
       return false;
     }
     return isLearnable(charm);
   }
 
   public boolean isLearned(String charmId) {
     ICharm charm = getCharmById(charmId);
     if (charm == null) {
       return false;
     }
     return isLearned(charm);
   }
 
   public boolean isUnlearnable(String charmId) {
     ICharm charm = getCharmById(charmId);
     final ILearningCharmGroup group = getGroup(charm);
     return group.isUnlearnable(charm);
   }
 
   private void registerMultiLearnableCharm(
       IMultiLearnableCharm visitedCharm,
       final ICharm charm,
       ILearningCharmGroup group) {
     addSpecialCharmConfiguration(charm, group, new MultiLearnableCharmConfiguration(context, charm, visitedCharm, this));
   }
 
   private void registerOxBodyTechnique(
       IOxBodyTechniqueCharm visited,
       final ICharm charm,
       IGenericTrait relevantTrait,
       ILearningCharmGroup group) {
     OxBodyTechniqueConfiguration oxBodyTechniqueConfiguration = new OxBodyTechniqueConfiguration(
         context.getTraitContext(),
         charm,
         relevantTrait,
         health.getOxBodyLearnArbitrator(),
         visited);
     addSpecialCharmConfiguration(charm, group, oxBodyTechniqueConfiguration);
     health.getOxBodyLearnArbitrator().addOxBodyTechniqueConfiguration(oxBodyTechniqueConfiguration);
     health.addHealthLevelProvider(oxBodyTechniqueConfiguration.getHealthLevelProvider());
   }
 
   private void registerPainToleranceCharm(
       final IPainToleranceCharm visitedCharm,
       ICharm charm,
       ILearningCharmGroup group) {
     final ISpecialCharmConfiguration specialCharmConfiguration = group.getSpecialCharmConfiguration(charm);
     IPainToleranceProvider painToleranceProvider = new IPainToleranceProvider() {
       public int getPainToleranceLevel() {
         int learnCount = specialCharmConfiguration.getCurrentLearnCount();
         return visitedCharm.getPainToleranceLevel(learnCount);
       }
     };
     health.addPainToleranceProvider(painToleranceProvider);
   }
 
   public boolean isAlienCharm(ICharm charm) {
     return !MartialArtsUtilities.isMartialArtsCharm(charm) && isAlienType(charm.getCharacterType());
   }
 
   private boolean isAlienType(CharacterType characterType) {
     return characterType != getNativeCharacterType();
   }
 
   public ISpecialCharmConfiguration getSpecialCharmConfiguration(String charmId) {
     ICharm charm = getCharmById(charmId);
     return getSpecialCharmConfiguration(charm);
   }
 
   public final boolean isCelestialMartialArtsGroupCompleted() {
     for (ILearningCharmGroup group : getMartialArtsGroups()) {
       ICharm martialArtsCharm = group.getAllCharms()[0];
       if (!martialArtsCharm.hasAttribute(ICharmData.NO_STYLE_ATTRIBUTE)
           && MartialArtsUtilities.hasLevel(MartialArtsLevel.Celestial, martialArtsCharm)
           && group.isCompleted()) {
         return true;
       }
     }
     return false;
   }
 
   public final boolean isCompulsiveCharm(ICharm charm) {
     String[] compulsiveCharmIDs = getModelContext().getAdditionalRules().getCompulsiveCharmIDs();
     return ArrayUtilities.contains(compulsiveCharmIDs, charm.getId());
   }
 
   public final boolean isLearned(ICharm charm) {
     ILearningCharmGroup group = getGroup(charm);
     return group != null & group.isLearned(charm);
   }
 
   private final ILearningCharmGroup getGroupById(CharacterType characterType, String groupId) {
     List<ILearningCharmGroup> candidateGroups = new ArrayList<ILearningCharmGroup>();
     Collections.addAll(candidateGroups, getNonMartialArtsGroups(characterType));
     Collections.addAll(candidateGroups, getMartialArtsGroups());
     for (ILearningCharmGroup group : candidateGroups) {
       if (group.getId().equals(groupId)) {
         return group;
       }
     }
     throw new IllegalArgumentException("No charm group defined for Id: " + groupId + "," + characterType); //$NON-NLS-1$ //$NON-NLS-2$
   }
 
   public final ILearningCharmGroup getGroup(ICharm charm) {
     return getGroupById(charm.getCharacterType(), charm.getGroupId());
   }
 
   public ICharmProvider getCharmProvider() {
     return provider;
   }
 }
