 
 package com.redditandroiddevelopers.tamagotchi;
 
 import android.content.Context;
 import android.os.Bundle;
 
 import com.badlogic.gdx.backends.android.AndroidApplication;
 import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
 import com.redditandroiddevelopers.tamagotchi.dao.CreatureDao;
 import com.redditandroiddevelopers.tamagotchi.dao.CreatureDatabase;
 import com.redditandroiddevelopers.tamagotchi.dao.CreatureEvolutionDao;
 import com.redditandroiddevelopers.tamagotchi.dao.CreatureRaiseTypeDao;
 import com.redditandroiddevelopers.tamagotchi.dao.CreatureStateDao;
 import com.redditandroiddevelopers.tamagotchi.dao.ExperienceDao;
 import com.redditandroiddevelopers.tamagotchi.dao.MedicineDao;
 import com.redditandroiddevelopers.tamagotchi.dao.SicknessDao;
 import com.redditandroiddevelopers.tamagotchi.mappers.CreatureEvolutionMapper;
 import com.redditandroiddevelopers.tamagotchi.mappers.CreatureMapper;
 import com.redditandroiddevelopers.tamagotchi.mappers.CreatureRaiseTypeMapper;
 import com.redditandroiddevelopers.tamagotchi.mappers.CreatureStateMapper;
 import com.redditandroiddevelopers.tamagotchi.mappers.ExperienceMapper;
 import com.redditandroiddevelopers.tamagotchi.mappers.MedicineMapper;
 import com.redditandroiddevelopers.tamagotchi.mappers.SicknessMapper;
 import com.redditandroiddevelopers.tamagotchi.model.Creature;
 import com.redditandroiddevelopers.tamagotchi.model.CreatureEvolution;
 import com.redditandroiddevelopers.tamagotchi.model.CreatureRaiseType;
 import com.redditandroiddevelopers.tamagotchi.model.CreatureState;
 import com.redditandroiddevelopers.tamagotchi.model.Experience;
 import com.redditandroiddevelopers.tamagotchi.model.Medicine;
 import com.redditandroiddevelopers.tamagotchi.model.Sickness;
 
 public class MainActivity extends AndroidApplication {
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
         cfg.useGL20 = false;
 
         TamagotchiConfiguration tcfg = new TamagotchiConfiguration();
         initializeDatabase(tcfg);
         initialize(new TamagotchiGame(tcfg), cfg);
 
     }
 
     private void initializeDatabase(TamagotchiConfiguration config) {
         final Context context = getBaseContext();
         config.creatureDao = new CreatureDao(new CreatureDatabase<Creature>(
                 context), new CreatureMapper());
         config.creatureEvolutionDao = new CreatureEvolutionDao(
                 new CreatureDatabase<CreatureEvolution>(
                         context), new CreatureEvolutionMapper());
         config.creatureRaiseTypeDao = new CreatureRaiseTypeDao(
                 new CreatureDatabase<CreatureRaiseType>(
                         context), new CreatureRaiseTypeMapper());
         config.creatureStateDao = new CreatureStateDao(new CreatureDatabase<CreatureState>(
                 context), new CreatureStateMapper());
         config.experienceDao = new ExperienceDao(new CreatureDatabase<Experience>(
                 context), new ExperienceMapper());
         config.medicineDao = new MedicineDao(new CreatureDatabase<Medicine>(
                 context), new MedicineMapper());
        config.sicknessDay = new SicknessDao(new CreatureDatabase<Sickness>(
                 context), new SicknessMapper());
     }
 }
