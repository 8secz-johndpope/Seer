 /*
  * SonarQube, open source software quality management tool.
  * Copyright (C) 2008-2013 SonarSource
  * mailto:contact AT sonarsource DOT com
  *
  * SonarQube is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * SonarQube is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package org.sonar.server.rule;
 
 import org.junit.Test;
 import org.sonar.api.rule.RuleDefinitions;
 
 import static org.fest.assertions.Assertions.assertThat;
 
 public class RuleRepositoriesTest {
   @Test
   public void should_register_repositories() {
     RuleDefinitions.Context context = new RuleDefinitions.Context();
     new SquidDefinitions().define(context);
     new FindbugsDefinitions().define(context);
 
     RuleRepositories repositories = new RuleRepositories();
     repositories.register(context);
 
     RuleRepositories.Repository findbugs = repositories.repository("findbugs");
     assertThat(findbugs).isNotNull();
     assertThat(findbugs.key()).isEqualTo("findbugs");
     assertThat(findbugs.name()).isEqualTo("Findbugs");
     assertThat(findbugs.language()).isEqualTo("java");
     assertThat(findbugs.getKey()).isEqualTo("findbugs");
    assertThat(findbugs.getName()).isEqualTo("Findbugs");
     assertThat(findbugs.getLanguage()).isEqualTo("java");
 
     RuleRepositories.Repository squid = repositories.repository("squid");
     assertThat(squid).isNotNull();
     assertThat(squid.key()).isEqualTo("squid");
     assertThat(squid.name()).isEqualTo("Squid");
     assertThat(squid.language()).isEqualTo("java");
 
     assertThat(repositories.repositories()).containsOnly(findbugs, squid);
     assertThat(repositories.repositoriesForLang("java")).containsOnly(findbugs, squid);
 
     // test equals() and hashCode()
     assertThat(findbugs).isEqualTo(findbugs).isNotEqualTo(squid).isNotEqualTo("findbugs").isNotEqualTo(null);
   }
 
   static class FindbugsDefinitions implements RuleDefinitions {
     @Override
     public void define(Context context) {
       NewRepository repo = context.newRepository("findbugs", "java");
       repo.setName("Findbugs");
       repo.newRule("ABC")
           .setName("ABC")
           .setHtmlDescription("Description of ABC");
       repo.done();
     }
   }
 
   static class SquidDefinitions implements RuleDefinitions {
     @Override
     public void define(Context context) {
       NewRepository repo = context.newRepository("squid", "java");
       repo.setName("Squid");
       repo.newRule("DEF")
           .setName("DEF")
           .setHtmlDescription("Description of DEF");
       repo.done();
     }
   }
 }
