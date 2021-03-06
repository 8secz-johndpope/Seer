 /*
  * This file is part of MyPet
  *
  * Copyright (C) 2011-2013 Keyle
  * MyPet is licensed under the GNU Lesser General Public License.
  *
  * MyPet is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * MyPet is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package de.Keyle.MyPet.entity.ai.target;
 
 import de.Keyle.MyPet.entity.types.EntityMyPet;
 import de.Keyle.MyPet.entity.types.MyPet;
 import de.Keyle.MyPet.skill.skills.Behavior;
 import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
 import de.Keyle.MyPet.util.MyPetPvP;
 import net.minecraft.server.v1_4_R1.EntityLiving;
 import net.minecraft.server.v1_4_R1.EntityPlayer;
 import net.minecraft.server.v1_4_R1.EntityTameableAnimal;
 import net.minecraft.server.v1_4_R1.PathfinderGoal;
 import org.bukkit.entity.Player;
 
 public class EntityAIOwnerHurtByTarget extends PathfinderGoal
 {
     private EntityMyPet petEntity;
     private EntityLiving lastDamager;
     private MyPet myPet;
 
     public EntityAIOwnerHurtByTarget(EntityMyPet entityMyPet)
     {
         this.petEntity = entityMyPet;
         myPet = entityMyPet.getMyPet();
     }
 
     public boolean a()
     {
         if (!petEntity.canMove())
         {
             return false;
         }
         EntityLiving localEntityLiving = this.petEntity.getOwner();
         if (localEntityLiving == null)
         {
             return false;
         }
         this.lastDamager = localEntityLiving.aC();
         if (this.lastDamager == null || !lastDamager.isAlive())
         {
             return false;
         }
         if (lastDamager instanceof EntityPlayer)
         {
             Player targetPlayer = (Player) lastDamager.getBukkitEntity();
 
             if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
             {
                 return false;
             }
         }
         else if (lastDamager instanceof EntityMyPet)
         {
             MyPet targetMyPet = ((EntityMyPet) lastDamager).getMyPet();
             if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
             {
                 return false;
             }
         }
         else if (lastDamager instanceof EntityTameableAnimal)
         {
             EntityTameableAnimal tameable = (EntityTameableAnimal) lastDamager;
            if (tameable.isTamed())
             {
                 Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                 if (myPet.getOwner().equals(tameableOwner))
                 {
                     return false;
                 }
             }
         }
         if (myPet.getSkills().isSkillActive("Behavior"))
         {
             Behavior behaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
             if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
             {
                 return false;
             }
             if (behaviorSkill.getBehavior() == BehaviorState.Raid)
             {
                 if (lastDamager instanceof EntityTameableAnimal && ((EntityTameableAnimal) lastDamager).isTamed())
                 {
                     return false;
                 }
                 if (lastDamager instanceof EntityMyPet)
                 {
                     return false;
                 }
                 if (lastDamager instanceof EntityPlayer)
                 {
                     return false;
                 }
             }
         }
         return true;
     }
 
     public boolean b()
     {
         EntityLiving entityliving = petEntity.getGoalTarget();
 
         if (!petEntity.canMove())
         {
             return false;
         }
         else if (entityliving == null)
         {
             return false;
         }
         else if (!entityliving.isAlive())
         {
             return false;
         }
         return true;
     }
 
     public void c()
     {
         petEntity.setGoalTarget(this.lastDamager);
     }
 
     public void d()
     {
         petEntity.setGoalTarget(null);
     }
 }
