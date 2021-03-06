 /**
  * 
  */
 package de.gebit.integrity.runner.forking.processes;
 
 import de.gebit.integrity.runner.forking.Fork;
 
 /**
  * This is a service responsible for abnormal termination of forked processes (and generic processes which have been
  * started during test execution, for example by a fixture, which are encouraged to have an instance of this interface
  * injected for usage). It is being called to terminate all "child" processes in the shutdown hook of the main test
  * runner VM.
  * 
  * @author Slartibartfast
  * 
  */
 public interface ProcessTerminator {
 
 	/**
 	 * Registers a fork with the terminator.
 	 * 
 	 * @param aFork
 	 *            the fork to register
 	 */
 	void registerFork(Fork aFork);
 
 	/**
 	 * Unregisters a fork from the terminator.
 	 * 
 	 * @param aFork
 	 *            the fork to unregister
 	 */
 	void unregisterFork(Fork aFork);
 
 	/**
 	 * Registers a {@link Process} with the terminator.
 	 * 
 	 * @param aProcess
 	 *            the process to register
 	 */
 	void registerProcess(Process aProcess);
 
 	/**
 	 * Unregisters a {@link Process} from the terminator.
 	 * 
 	 * @param aProcess
 	 *            the process to unregister
 	 */
 	void unregisterProcess(Process aProcess);
 
 	/**
	 * Kill all known forks and processes,
 	 * 
 	 * @param aTimeout
 	 * @return
 	 */
 	boolean killAndWait(int aTimeout);
 
 }
