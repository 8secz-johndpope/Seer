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
 package org.sonar.plugins.core.issue;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Strings;
 import com.google.common.collect.Lists;
 import org.sonar.api.batch.*;
 import org.sonar.api.component.ResourcePerspectives;
 import org.sonar.api.issue.Issuable;
 import org.sonar.api.issue.Issue;
 import org.sonar.api.profiles.RulesProfile;
 import org.sonar.api.resources.Project;
 import org.sonar.api.resources.Resource;
 import org.sonar.api.resources.ResourceUtils;
 import org.sonar.api.rule.RuleKey;
 import org.sonar.api.rules.ActiveRule;
 import org.sonar.api.rules.Rule;
 import org.sonar.api.rules.RuleFinder;
 import org.sonar.api.utils.KeyValueFormat;
 import org.sonar.batch.issue.IssueCache;
 import org.sonar.core.issue.DefaultIssue;
 import org.sonar.core.issue.IssueChangeContext;
 import org.sonar.core.issue.IssueUpdater;
 import org.sonar.core.issue.db.IssueDto;
 import org.sonar.core.issue.workflow.IssueWorkflow;
 
 import java.util.Collection;
 
 @DependsUpon({DecoratorBarriers.END_OF_VIOLATIONS_GENERATION, DecoratorBarriers.START_VIOLATION_TRACKING})
 @DependedUpon({DecoratorBarriers.END_OF_VIOLATION_TRACKING, DecoratorBarriers.END_OF_ISSUES_UPDATES})
 public class IssueTrackingDecorator implements Decorator {
 
   private final IssueCache issueCache;
   private final InitialOpenIssuesStack initialOpenIssues;
   private final IssueTracking tracking;
   private final IssueHandlers handlers;
   private final IssueWorkflow workflow;
   private final IssueUpdater updater;
   private final IssueChangeContext changeContext;
   private final ResourcePerspectives perspectives;
   private final RulesProfile rulesProfile;
   private final RuleFinder ruleFinder;
 
   public IssueTrackingDecorator(IssueCache issueCache, InitialOpenIssuesStack initialOpenIssues, IssueTracking tracking,
                                 IssueHandlers handlers, IssueWorkflow workflow,
                                 IssueUpdater updater,
                                 Project project,
                                 ResourcePerspectives perspectives,
                                 RulesProfile rulesProfile,
                                 RuleFinder ruleFinder) {
     this.issueCache = issueCache;
     this.initialOpenIssues = initialOpenIssues;
     this.tracking = tracking;
     this.handlers = handlers;
     this.workflow = workflow;
     this.updater = updater;
     this.changeContext = IssueChangeContext.createScan(project.getAnalysisDate());
     this.perspectives = perspectives;
     this.rulesProfile = rulesProfile;
     this.ruleFinder = ruleFinder;
   }
 
   public boolean shouldExecuteOnProject(Project project) {
     return project.isLatestAnalysis();
   }
 
   public void decorate(Resource resource, DecoratorContext context) {
     Issuable issuable = perspectives.as(Issuable.class, resource);
     if (issuable != null) {
       doDecorate(resource);
 
     }
   }
 
   @VisibleForTesting
   void doDecorate(Resource resource) {
     Collection<DefaultIssue> issues = Lists.newArrayList();
     for (Issue issue : issueCache.byComponent(resource.getEffectiveKey())) {
       issueCache.remove(issue);
       issues.add((DefaultIssue) issue);
     }
     // issues = all the issues created by rule engines during this module scan and not excluded by filters
 
     // all the issues that are not closed in db before starting this module scan, including manual issues
     Collection<IssueDto> dbOpenIssues = initialOpenIssues.selectAndRemove(resource.getId());
 
     IssueTrackingResult trackingResult = tracking.track(resource, dbOpenIssues, issues);
 
     // unmatched = issues that have been resolved + issues on disabled/removed rules + manual issues
     addUnmatched(trackingResult.unmatched(), issues);
 
     mergeMatched(trackingResult);
 
     if (ResourceUtils.isProject(resource)) {
       // issues that relate to deleted components
       addDead(issues);
     }
 
     for (DefaultIssue issue : issues) {
       workflow.doAutomaticTransition(issue, changeContext);
       handlers.execute(issue, changeContext);
       issueCache.put(issue);
     }
   }
 
   private void mergeMatched(IssueTrackingResult result) {
     for (DefaultIssue issue : result.matched()) {
       IssueDto ref = result.matching(issue);
 
       // invariant fields
       issue.setKey(ref.getKee());
       issue.setCreationDate(ref.getIssueCreationDate());
       issue.setUpdateDate(ref.getIssueUpdateDate());
       issue.setCloseDate(ref.getIssueCloseDate());
 
       // non-persisted fields
       issue.setNew(false);
       issue.setEndOfLife(false);
       issue.setOnDisabledRule(false);
 
       // fields to update with old values
       issue.setActionPlanKey(ref.getActionPlanKey());
       issue.setResolution(ref.getResolution());
       issue.setStatus(ref.getStatus());
       issue.setAssignee(ref.getAssignee());
       issue.setAuthorLogin(ref.getAuthorLogin());
       if (ref.getIssueAttributes() != null) {
         issue.setAttributes(KeyValueFormat.parse(ref.getIssueAttributes()));
       }
 
       // fields to update with current values
       if (ref.isManualSeverity()) {
         issue.setManualSeverity(true);
         issue.setSeverity(ref.getSeverity());
       } else {
         // Emulate change of severity in the current scan.
         String severity = issue.severity();
         issue.setSeverity(ref.getSeverity());
         updater.setSeverity(issue, severity, changeContext);
       }
       Integer line = issue.line();
       issue.setLine(ref.getLine());
       updater.setLine(issue, line);
     }
   }
 
   private void addUnmatched(Collection<IssueDto> unmatchedIssues, Collection<DefaultIssue> issues) {
     for (IssueDto unmatchedDto : unmatchedIssues) {
       DefaultIssue unmatched = unmatchedDto.toDefaultIssue();
      unmatched.setNew(false);
 
      boolean manualIssue = !Strings.isNullOrEmpty(unmatched.reporter());
      Rule rule = ruleFinder.findByKey(unmatched.ruleKey());
       if (manualIssue) {
         // Manual rules are not declared in Quality profiles, so no need to check ActiveRule
        boolean onDisabledRule = (rule == null || Rule.STATUS_REMOVED.equals(rule.getStatus()));
         unmatched.setEndOfLife(onDisabledRule);
        unmatched.setOnDisabledRule(onDisabledRule);
       } else {
        ActiveRule activeRule = rulesProfile.getActiveRule(unmatched.ruleKey().repository(), unmatched.ruleKey().rule());
         unmatched.setEndOfLife(true);
        unmatched.setOnDisabledRule(activeRule == null || rule == null || Rule.STATUS_REMOVED.equals(rule.getStatus()));
       }

       issues.add(unmatched);
     }
   }
 
   private void addDead(Collection<DefaultIssue> issues) {
     for (IssueDto deadDto : initialOpenIssues.getAllIssues()) {
       DefaultIssue dead = deadDto.toDefaultIssue();
       ActiveRule activeRule = rulesProfile.getActiveRule(deadDto.getRuleRepo(), deadDto.getRule());
       dead.setNew(false);
       dead.setEndOfLife(true);
       dead.setOnDisabledRule(activeRule == null);
       issues.add(dead);
     }
   }
 }
