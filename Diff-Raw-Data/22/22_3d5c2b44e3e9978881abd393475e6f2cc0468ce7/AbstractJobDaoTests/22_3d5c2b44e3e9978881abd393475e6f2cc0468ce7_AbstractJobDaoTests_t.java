 /*
  * Copyright 2006-2007 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.springframework.batch.execution.repository.dao;
 
import java.sql.Timestamp;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;
 
 import org.springframework.batch.core.domain.BatchStatus;
 import org.springframework.batch.core.domain.JobExecution;
 import org.springframework.batch.core.domain.JobInstance;
 import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
 import org.springframework.batch.core.runtime.SimpleJobIdentifier;
 import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
 import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
 import org.springframework.batch.repeat.ExitStatus;
 import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
 import org.springframework.util.ClassUtils;
 
 /**
  * @author Dave Syer
  *
  */
 public abstract class AbstractJobDaoTests extends
 		AbstractTransactionalDataSourceSpringContextTests {
 
 	protected JobDao jobDao;
 
 	protected ScheduledJobIdentifier jobRuntimeInformation;
 
 	protected JobInstance job;
 
 	protected JobExecution jobExecution;
 
 	protected Date jobExecutionStartTime = new Date(System
 			.currentTimeMillis());
 
 	protected String[] getConfigLocations() {
 		return new String[] { ClassUtils.addResourcePathToPackagePath(
 				getClass(), "sql-dao-test.xml") };
 	}
 
 	/*
 	 * Because AbstractTransactionalSpringContextTests is used, this method will
 	 * be called by Spring to set the JobRepository.
 	 */
 	public void setJobRepositoryDao(JobDao jobRepositoryDao) {
 		this.jobDao = jobRepositoryDao;
 	}
 
 	protected void onSetUpInTransaction() throws Exception {
 		jobRuntimeInformation = new ScheduledJobIdentifier("Job1", "TestStream",
 				new SimpleDateFormat("yyyyMMdd").parse("20070505"));
 
 		// Create job.
 		job = jobDao.createJob(jobRuntimeInformation);
 
 		// Create an execution
 		jobExecutionStartTime = new Date(System.currentTimeMillis());
 		jobExecution = new JobExecution(job);
 		jobExecution.setStartTime(jobExecutionStartTime);
 		jobExecution.setStatus(BatchStatus.STARTED);
 		jobDao.save(jobExecution);
 	}
 
 	public void testVersionIsNotNullForJob() throws Exception {
 		int version = jdbcTemplate
 				.queryForInt("select version from BATCH_JOB_INSTANCE where ID="
 						+ job.getId());
 		assertEquals(0, version);
 	}
 
 	public void testVersionIsNotNullForJobExecution() throws Exception {
 		int version = jdbcTemplate
 				.queryForInt("select version from BATCH_JOB_EXECUTION where ID="
 						+ jobExecution.getId());
 		assertEquals(0, version);
 	}
 
 	public void testFindNonExistentJob() {
 		// No job should be found since it hasn't been created.
 		List jobs = jobDao.findJobs(new ScheduledJobIdentifier("Job2", "TestStream", new Date()));
 		assertTrue(jobs.size() == 0);
 	}
 
 	public void testFindJob() {
 
 		List jobs = jobDao.findJobs(jobRuntimeInformation);
 		assertTrue(jobs.size() == 1);
 		JobInstance tempJob = (JobInstance) jobs.get(0);
 		assertTrue(job.equals(tempJob));
 		assertEquals(jobRuntimeInformation, tempJob.getIdentifier());
 	}
 
 	public void testFindJobWithNullRuntime() {
 
 		ScheduledJobIdentifier runtimeInformation = null;
 
 		try {
 			jobDao.findJobs(runtimeInformation);
 			fail();
 		} catch (IllegalArgumentException ex) {
 			// expected
 		}
 	}
 
 	/**
 	 * Test that ensures that if you create a job with a given name, then find a
 	 * job with the same name, but other pieces of the identifier different, you
 	 * get no result, not the existing one.
 	 */
 	public void testCreateJobWithExistingName() {
 		ScheduledJobIdentifier scheduledIdentifier = new ScheduledJobIdentifier(
 				"ScheduledJob", "key", new Date());
 
 		jobDao.createJob(scheduledIdentifier);
 
 		// Modifying the key should bring back a completely different
 		// JobInstance
 		ScheduledJobIdentifier newIdentifier = new ScheduledJobIdentifier(
 				"ScheduledJob", "different key", new Date());
 
 		List jobs;
 		jobs = jobDao.findJobs(scheduledIdentifier);
 		assertEquals(1, jobs.size());
 		JobInstance job = (JobInstance) jobs.get(0);
 		assertEquals(scheduledIdentifier, job.getIdentifier());
 
 		jobs = jobDao.findJobs(newIdentifier);
 		assertEquals(0, jobs.size());
 
 	}
 
 	public void testUpdateJob() {
 		// Update the returned job with a new status
 		job.setStatus(BatchStatus.COMPLETED);
 		jobDao.update(job);
 
 		// The job just updated should be found, with the saved status.
 		List jobs = jobDao.findJobs(jobRuntimeInformation);
 		assertTrue(jobs.size() == 1);
 		JobInstance tempJob = (JobInstance) jobs.get(0);
 		assertTrue(job.equals(tempJob));
 		assertEquals(tempJob.getStatus(), BatchStatus.COMPLETED);
 	}
 
 	public void testUpdateJobWithNullId() {
 
 		JobInstance testJob = new JobInstance(null);
 		try {
 			jobDao.update(testJob);
 			fail();
 		} catch (IllegalArgumentException ex) {
 			// expected
 		}
 	}
 
 	public void testUpdateNullJob() {
 
 		JobInstance testJob = null;
 		try {
 			jobDao.update(testJob);
 		} catch (IllegalArgumentException ex) {
 			// expected
 		}
 	}
 
 	public void testUpdateJobExecution() {
 
 		jobExecution.setStatus(BatchStatus.COMPLETED);
 		jobExecution.setExitStatus(ExitStatus.FINISHED);
 		jobExecution.setEndTime(new Date(System.currentTimeMillis()));
 		jobDao.update(jobExecution);
 
 		List executions = jobDao.findJobExecutions(job);
 		assertEquals(executions.size(), 1);
 		validateJobExecution(jobExecution, (JobExecution) executions.get(0));
 
 	}
 
 	public void testSaveJobExecution(){
 
 		List executions = jobDao.findJobExecutions(job);
 		assertEquals(executions.size(), 1);
 		validateJobExecution(jobExecution, (JobExecution) executions.get(0));
 	}
 
 	public void testUpdateInvalidJobExecution() {
 
 		// id is invalid
 		JobExecution execution = new JobExecution(job, new Long(29432));
 		try {
 			jobDao.update(execution);
 			fail("Expected NoSuchBatchDomainObjectException");
 		} catch (NoSuchBatchDomainObjectException ex) {
 			// expected
 		}
 	}
 
 	public void testUpdateNullIdJobExection() {
 
 		JobExecution execution = new JobExecution(job);
 		try {
 			jobDao.update(execution);
 			fail();
 		} catch (IllegalArgumentException ex) {
 			// expected
 		}
 	}
 
 	public void testIncrementExecutionCount() {
 
 		// 1 JobExection already added in setup
 		assertEquals(jobDao.getJobExecutionCount(job.getId()), 1);
 
 		// Save new JobExecution for same job
 		JobExecution testJobExecution = new JobExecution(job);
 		jobDao.save(testJobExecution);
 		// JobExecutionCount should be incremented by 1
 		assertEquals(jobDao.getJobExecutionCount(job.getId()), 2);
 	}
 
 	public void testZeroExecutionCount() {
 
 		JobInstance testJob = jobDao.createJob(new ScheduledJobIdentifier(
 				"TestJob", "key", new Date()));
 		// no jobExecutions saved for new job, count should be 0
 		assertEquals(jobDao.getJobExecutionCount(testJob.getId()), 0);
 	}
 
 	public void testJobWithSimpleJobIdentifier() throws Exception {
 		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("Job1");
 
 		// Create job.
 		job = jobDao.createJob(jobIdentifier);
 
 		List jobs = jdbcTemplate.queryForList(
 				"SELECT * FROM BATCH_JOB_INSTANCE where ID=?", new Object[] { job
 						.getId() });
 		assertEquals(1, jobs.size());
 		assertEquals(job.getName(), ((Map) jobs.get(0)).get("JOB_NAME"));
 
 	}
 
 	public void testJobWithDefaultJobIdentifier() throws Exception {
 		DefaultJobIdentifier jobIdentifier = new DefaultJobIdentifier("Job1", "testKey");
 
 		// Create job.
 		job = jobDao.createJob(jobIdentifier);
 
 		List jobs = jobDao.findJobs(jobIdentifier); 
 			
 		assertEquals(1, jobs.size());
 		assertEquals(job.getName(), ((JobInstance) jobs.get(0)).getName());
 		assertEquals(jobIdentifier.getJobKey(), ((JobInstance) jobs.get(0)).
 				getIdentifier().getJobInstanceProperties().getString(DefaultJobIdentifier.JOB_KEY));
 
 	}
 
 	public void testJobWithScheduledJobIdentifier() throws Exception {
 		Date date = new Date();
 		ScheduledJobIdentifier jobIdentifier = new ScheduledJobIdentifier("Job1", "testKey", date);
 
 		// Create job.
 		job = jobDao.createJob(jobIdentifier);
 
 		List jobs = jobDao.findJobs(jobIdentifier); 
 			
 		assertEquals(1, jobs.size());
 		assertEquals(job.getName(), ((JobInstance) jobs.get(0)).getName());
 		assertEquals(jobIdentifier.getJobKey(), ((JobInstance) jobs.get(0)).
 				getIdentifier().getJobInstanceProperties().getString(DefaultJobIdentifier.JOB_KEY));
 
 	}
 
 	public void testJobWithScheduledJobIdentifierAndDifferentTime() throws Exception {
 		Date date = new Date();
 		ScheduledJobIdentifier jobIdentifier = new ScheduledJobIdentifier("Job1", "testKey", date);
 
 		// Create job.
 		job = jobDao.createJob(jobIdentifier);
 		
 		Date later = new Date(date.getTime()+3600000);
 		ScheduledJobIdentifier laterIdentifier = new ScheduledJobIdentifier("Job1", "testKey", later);
 
 		List jobs = jobDao.findJobs(laterIdentifier); 
 
 		// Different timestamp is different identifier...
 		assertEquals(0, jobs.size());
 
 	}
 
	public void testJobWithScheduledJobIdentifierAndDifferentDateImplementation() throws Exception {
		Date date = new Date();
		ScheduledJobIdentifier jobIdentifier = new ScheduledJobIdentifier("Job1", "testKey", date);

		// Create job.
		job = jobDao.createJob(jobIdentifier);
		
		Timestamp later = new Timestamp(date.getTime());
		ScheduledJobIdentifier laterIdentifier = new ScheduledJobIdentifier("Job1", "testKey", later);

		List jobs = jobDao.findJobs(laterIdentifier); 

		// Different timestamp is different identifier...
		assertEquals(1, jobs.size());
		assertEquals(job.getName(), ((JobInstance) jobs.get(0)).getName());
		assertEquals(jobIdentifier.getJobKey(), ((JobInstance) jobs.get(0)).
				getIdentifier().getJobInstanceProperties().getString(DefaultJobIdentifier.JOB_KEY));

	}

 	public void testFindJobExecutions(){
 
 		List results = jobDao.findJobExecutions(job);
 		assertEquals(results.size(), 1);
 		validateJobExecution(jobExecution, (JobExecution)results.get(0));
 	}
 	
 	public void testFindJobsWithProperties() throws Exception{
 		
 		
 	}
 
 	private void validateJobExecution(JobExecution lhs, JobExecution rhs){
 
 		//equals operator only checks id
 		assertEquals(lhs, rhs);
 		assertEquals(lhs.getStartTime(), rhs.getStartTime());
 		assertEquals(lhs.getEndTime(), rhs.getEndTime());
 		assertEquals(lhs.getStatus(), rhs.getStatus());
 		assertEquals(lhs.getExitStatus(), rhs.getExitStatus());
 	}
 
 }
