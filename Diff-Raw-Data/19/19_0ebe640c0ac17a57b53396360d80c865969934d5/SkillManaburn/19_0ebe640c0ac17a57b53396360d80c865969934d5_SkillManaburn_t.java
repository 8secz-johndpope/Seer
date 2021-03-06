 package com.herocraftonline.dev.heroes.command.skill.skills;
 
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.entity.Player;
 import org.bukkit.util.config.ConfigurationNode;
 
 import com.herocraftonline.dev.heroes.Heroes;
 import com.herocraftonline.dev.heroes.command.skill.TargettedSkill;
 import com.herocraftonline.dev.heroes.persistence.Hero;
 
 public class SkillManaburn extends TargettedSkill {
 
     public SkillManaburn(Heroes plugin) {
         super(plugin);
         name = "Manaburn";
         description = "Burns the targets mana";
         usage = "/skill manaburn";
         minArgs = 0;
         maxArgs = 1;
         identifiers.add("skill manaburn");
     }
 
     @Override
     public ConfigurationNode getDefaultConfig() {
         ConfigurationNode node = super.getDefaultConfig();
         node.setProperty("transfer-amount", 20);
         return node;
     }
 
     @Override
     public boolean use(Hero hero, LivingEntity target, String[] args) {
         if (!(target instanceof Player)) {
             return false;
         }
         Hero tHero = plugin.getHeroManager().getHero((Player) target);
         if (tHero == null) {
             return false;
         }
         int transferamount = getSetting(hero.getHeroClass(), "transfer-amount", 20);
         if (tHero.getMana() > transferamount) {
             if ((hero.getMana() + transferamount) > 100) {
                 transferamount = (100 - hero.getMana());
             }
             tHero.setMana(tHero.getMana() - transferamount);
            notifyNearbyPlayers(hero.getPlayer().getLocation(), useText, hero.getPlayer().getName(), name);
             return true;
         } else {
             return false;
         }
     }
 
 }
