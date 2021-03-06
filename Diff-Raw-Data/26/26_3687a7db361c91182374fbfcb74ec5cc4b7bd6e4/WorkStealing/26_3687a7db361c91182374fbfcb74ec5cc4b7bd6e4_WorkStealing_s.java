 package ibis.satin.impl;
 
 import ibis.ipl.IbisError;
 import ibis.ipl.IbisIdentifier;
 import ibis.ipl.SendPort;
 import ibis.ipl.SendPortIdentifier;
 import ibis.ipl.WriteMessage;
 
 import java.io.IOException;
 
 public abstract class WorkStealing extends Stats {
 
 	protected void sendResult(InvocationRecord r, ReturnRecord rr) {
 		if (/* exiting || */r.alreadySentExceptionResult)
 			return;
 
 		if (ASSERTS && r.owner == null) {
 			System.err.println("SATIN '" + ident.name()
 					+ "': owner is null in sendResult");
 			System.exit(1);
 		}
 
 		if (STEAL_DEBUG) {
 			out.println("SATIN '" + ident.name() + "': sending job result to "
 					+ r.owner.name() + ", exception = "
 					+ (r.eek == null ? "null" : ("" + r.eek)));
 		}
 
 		SendPort s = null;
 
 		/*
 		 * boolean noUpdate = false; if (NUM_CRASHES > 0) { for (int i=1; i
 		 * <NUM_CRASHES+1 && i <allIbises.size(); i++) { IbisIdentifier id =
 		 * (IbisIdentifier) allIbises.get(i); if (id.equals(ident)) { //
 		 * System.out.println(ident.name() + " - i'm not sending the result");
 		 * noUpdate = true; } } }
 		 */
 
 		synchronized (this) {
 
 			/*
 			 * if (FAULT_TOLERANCE) { if (GRT_TIMING) { updateTimer.start(); }
 			 * globalResultTable.updateInvocationRecord(r);
 			 * 
 			 * if (GRT_TIMING) { updateTimer.stop(); } }
 			 */
 			s = getReplyPortNoWait(r.owner);
 		}
 
 		if (s == null) {
 			//probably crashed..
 			return;
 		}
 
 		try {
 			WriteMessage writeMessage = s.newMessage();
 			if (r.eek == null) {
 				writeMessage.writeByte(Protocol.JOB_RESULT_NORMAL);
 				writeMessage.writeObject(r.owner);
 				writeMessage.writeObject(rr);
 			} else {
 				if (rr == null)
 					r.alreadySentExceptionResult = true;
 				writeMessage.writeByte(Protocol.JOB_RESULT_EXCEPTION);
 				writeMessage.writeObject(r.owner);
 				writeMessage.writeObject(r.eek);
 				writeMessage.writeInt(r.stamp);
 			}
 			long cnt = writeMessage.finish();
 
 			if (STEAL_STATS) {
 				if (inDifferentCluster(r.owner)) {
 					interClusterMessages++;
 					interClusterBytes += cnt;
 				} else {
 					intraClusterMessages++;
 					intraClusterBytes += cnt;
 				}
 			}
 		} catch (IOException e) {
 			System.err.println("SATIN '" + ident.name()
 					+ "': Got Exception while sending steal request: " + e);
 			System.exit(1);
 		}
 	}
 
 	/*
 	 * does a synchronous steal. If blockOnServer is true, it blocks on server
 	 * side until work is available, or we must exit. This is used in
 	 * MasterWorker algorithms.
 	 */
 	protected InvocationRecord stealJob(Victim v, boolean blockOnServer) {
 
 		if (ASSERTS && stolenJob != null) {
 			throw new IbisError(
 					"EEEK, trying to steal while an unhandled stolen job is available.");
 		}
 		if (STEAL_TIMING) {
 			stealTimer.start();
 		}
 
 		if (STEAL_STATS) {
 			stealAttempts++;
 		}
 
		sendStealRequest(v, true, blockOnServer);
		return waitForStealReply();
 	}
 
 	protected void sendStealRequest(Victim v, boolean synchronous,
 			boolean blocking) {
		if (exiting)
			return;
 
 		if (STEAL_DEBUG && synchronous) {
 			System.err.println("SATIN '" + ident.name()
 					+ "': sending steal message to " + v.ident.name());
 		}
 		if (STEAL_DEBUG && !synchronous) {
 			System.err.println("SATIN '" + ident.name()
 					+ "': sending ASYNC steal message to " + v.ident.name());
 		}
 
 		try {
 			SendPort s = v.s;
 			WriteMessage writeMessage = s.newMessage();
 			byte opcode = -1;
 
 			if (synchronous) {
 				if (blocking) {
 					opcode = Protocol.BLOCKING_STEAL_REQUEST;
 				} else {
 					if (FAULT_TOLERANCE) {
 						synchronized (this) {
 							if (getTable) {
 								opcode = Protocol.STEAL_AND_TABLE_REQUEST;
 							} else {
 								opcode = Protocol.STEAL_REQUEST;
 							}
 						}
 					} else {
 						opcode = Protocol.STEAL_REQUEST;
 					}
 				}
 			} else {
 				if (FAULT_TOLERANCE) {
 					synchronized (this) {
 						if (clusterCoordinator && getTable) {
 							opcode = Protocol.ASYNC_STEAL_AND_TABLE_REQUEST;
 						} else {
 							if (getTable) {
 								System.err
 										.println("SATIN '"
 												+ ident.name()
 												+ ": EEEK sending async steal message while waiting for table!!");
 							}
 							opcode = Protocol.ASYNC_STEAL_REQUEST;
 						}
 					}
 				} else {
 					opcode = Protocol.ASYNC_STEAL_REQUEST;
 				}
 			}
 
 			writeMessage.writeByte(opcode);
 			long cnt = writeMessage.finish();
 			if (STEAL_STATS) {
 				if (inDifferentCluster(v.ident)) {
 					interClusterMessages++;
 					interClusterBytes += cnt;
 				} else {
 					intraClusterMessages++;
 					intraClusterBytes += cnt;
 				}
 			}
 		} catch (IOException e) {
 			System.err.println("SATIN '" + ident.name()
 					+ "': Got Exception while sending "
 					+ (synchronous ? "" : "a") + "synchronous"
 					+ " steal request: " + e);
 			System.exit(1);
 		}
 	}
 
 	protected InvocationRecord waitForStealReply() {
 		//		if(exiting) return false;
 
 		if (IDLE_TIMING) {
 			idleTimer.start();
 		}
 
 		// Replaced this wait call, do something useful instead:
 		// handleExceptions and aborts.
 		if (upcalls) {
 			if (HANDLE_MESSAGES_IN_LATENCY) {
 				while (true) {
 					satinPoll();
 
 					if (ABORTS || FAULT_TOLERANCE) {
 						handleDelayedMessages();
 					}
 
 					synchronized (this) {
 
 						if (gotStealReply) {
 							/*
 							 * Immediately reset gotStealReply, we know that a
 							 * reply has arrived.
 							 */
 							gotStealReply = false;
 							currentVictimCrashed = false;
 							break;
 						}
 
 						if (FAULT_TOLERANCE) {
 							if (currentVictimCrashed) {
 								currentVictimCrashed = false;
 								if (gotStealReply == false)
 									return null;
 							}
 						}
 					}
 					//					Thread.yield();
 				}
 			} else {
 				synchronized (this) {
 					while (!gotStealReply) {
 
 						if (FAULT_TOLERANCE) {
 							if (currentVictimCrashed) {
 								currentVictimCrashed = false;
 								//							System.err.println("SATIN '" + ident.name() +
 								// "': current victim crashed");
 								if (gotStealReply == false)
 									return null;
 								break;
 							}
 						}
 
 						try {
 							wait();
 						} catch (InterruptedException e) {
 							throw new IbisError(e);
 						}
 
 					}
 					/*
 					 * Immediately reset gotStealReply, we know that a reply has
 					 * arrived.
 					 */
 					gotStealReply = false;
 				}
 			}
 		} else { // poll for reply
 			while (!gotStealReply) {
 				satinPoll();
 				if (FAULT_TOLERANCE) {
 					if (currentVictimCrashed) {
 						currentVictimCrashed = false;
 						if (gotStealReply == false)
 							return null;
 					}
 				}
 
 			}
 			gotStealReply = false;
 		}
 
 		if (IDLE_TIMING) {
 			idleTimer.stop();
 		}
 
 		if (STEAL_TIMING) {
 			stealTimer.stop();
 		}
 
 		/*
 		 * if(STEAL_DEBUG) { out.println("SATIN '" + ident.name() + "': got
 		 * synchronous steal reply: " + (stolenJob == null ? "FAILED" :
 		 * "SUCCESS")); }
 		 */
 
 		/* If successfull, we now have a job in stolenJob. */
 		if (stolenJob == null) {
 			return null;
 		}
 
 		/* I love it when a plan comes together! */
 
 		if (STEAL_STATS) {
 			stealSuccess++;
 		}
 
 		InvocationRecord myJob = stolenJob;
 		stolenJob = null;
 
 		//		stolenFrom = myJob.owner;
 
 		// if we have ordered communication, we have to wait until
 		// our sequence number equals the one in the job
 		if (Satin.use_seq) {
 			if (TUPLE_TIMING) {
 				tupleOrderingWaitTimer.start();
 			}
 			if (TUPLE_DEBUG) {
 				System.err.println("steal reply seq nr = " + stealReplySeqNr
 						+ ", my seq nr = " + expected_seqno);
 			}
 			while (stealReplySeqNr > expected_seqno) {
 				handleDelayedMessages();
 			}
 			if (TUPLE_TIMING) {
 				tupleOrderingWaitTimer.stop();
 			}
 		}
 
 		return myJob;
 	}
 
 	// hold the lock when calling this
 	protected void addToJobResultList(InvocationRecord r) {
 		if (ASSERTS) {
 			assertLocked(this);
 		}
 		resultList.add(r);
 	}
 
 	// hold the lock when calling this
 	protected InvocationRecord getStolenInvocationRecord(int stamp,
 			SendPortIdentifier sender, IbisIdentifier owner) {
 		if (ASSERTS) {
 			assertLocked(this);
 			if (owner == null) {
 				System.err.println("SATIN '" + ident.name()
 						+ "': owner is null in getStolenInvocationRecord");
 				System.exit(1);
 			}
 			if (!owner.equals(ident)) {
 				System.err.println("SATIN '" + ident.name()
 						+ "': Removing wrong stamp!");
 				System.exit(1);
 			}
 		}
 		return outstandingJobs.remove(stamp, owner);
 	}
 
 	synchronized void addJobResult(ReturnRecord rr, SendPortIdentifier sender,
 			IbisIdentifier i, Throwable eek, int stamp) {
 		receivedResults = true;
 		InvocationRecord r = null;
 
 		if (rr != null) {
 			r = getStolenInvocationRecord(rr.stamp, sender, i);
 		} else {
 			r = getStolenInvocationRecord(stamp, sender, i);
 		}
 
 		if (r != null) {
 			if (rr != null) {
 				rr.assignTo(r);
 			} else {
 				r.eek = eek;
 			}
 			if (r.eek != null) { // we have an exception, add it to the list.
 				// the list will be read during the sync
 				if (ABORTS) {
 					addToExceptionList(r);
 				} else {
 					throw new IbisError("Got exception result", r.eek);
 				}
 			} else {
 				addToJobResultList(r);
 			}
 		} else {
 			if (ABORTS || FAULT_TOLERANCE) {
 				if (ABORT_DEBUG) {
 					out.println("SATIN '" + ident.name()
 							+ "': got result for aborted job, ignoring.");
 				}
 			} else {
 				out.println("SATIN '" + ident.name()
 						+ "': got result for unknown job!");
 				System.exit(1);
 			}
 		}
 	}
 
 	synchronized void handleResults() {
 		while (true) {
 			InvocationRecord r = resultList.removeIndex(0);
 			if (r == null)
 				break;
 
 			if (r.eek != null) {
 				handleInlet(r);
 			}
 
 			r.spawnCounter.value--;
 
 			if (FAULT_TOLERANCE) {
 				//add the finished job to children list
 				if (r.parent != null) {
 					r.sibling = r.parent.child;
 					r.parent.child = r;
 				}
 			}
 			if (ASSERTS && r.spawnCounter.value < 0) {
 				out.println("Just made spawncounter < 0");
 				new Exception().printStackTrace();
 				System.exit(1);
 			}
 		}
 
 		receivedResults = false;
 	}
 
 	// hold the lock when calling this
 	protected void addToOutstandingJobList(InvocationRecord r) {
 		if (ASSERTS) {
 			assertLocked(this);
 		}
 		outstandingJobs.add(r);
 	}
 
 	protected synchronized void gotJobResult(InvocationRecord ir) {
 		gotStealReply = true;
 		stolenJob = ir;
 		currentVictim = null;
 		notifyAll();
 	}
 }
