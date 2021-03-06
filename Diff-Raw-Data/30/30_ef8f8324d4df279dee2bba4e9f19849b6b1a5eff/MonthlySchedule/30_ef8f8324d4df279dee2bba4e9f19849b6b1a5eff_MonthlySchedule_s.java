 /*
  * Nexus: Maven Repository Manager
  * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
  * 
  * This file is part of Nexus.                                                                                                                                  
  * 
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see http://www.gnu.org/licenses/.
  *
  */
 package org.sonatype.scheduling.schedules;
 
 import java.util.Date;
 import java.util.Set;
 
 import org.sonatype.scheduling.iterators.MonthlySchedulerIterator;
 import org.sonatype.scheduling.iterators.SchedulerIterator;
 
 public class MonthlySchedule
     extends AbstractSchedule
 {
     private final Set<Integer> daysToRun;
 
     public MonthlySchedule( Date startDate, Date endDate, Set<Integer> daysToRun )
     {
         super( startDate, endDate );
 
         this.daysToRun = daysToRun;
     }
 
     public Set<Integer> getDaysToRun()
     {
         return daysToRun;
     }
 
     protected SchedulerIterator createIterator()
     {
         return new MonthlySchedulerIterator( getStartDate(), getEndDate(), daysToRun );
     }
 
 }
