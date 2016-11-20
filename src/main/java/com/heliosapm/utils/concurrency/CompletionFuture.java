/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.utils.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Title: CompletionFuture</p>
 * <p>Description: A simple task completion future</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.CompletionFuture</code></p>
 */

public class CompletionFuture implements Future<Boolean> {
	/** The task completion latch */
	protected final CountDownLatch latch;
	/** The exception thrown by the task */
	protected Exception thrown = null;
	
	/**
	 * Creates a new CompletionFuture
	 * @param count The countdown required to drop the latch
	 */
	public CompletionFuture(final int count) {
		latch = new CountDownLatch(count);
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone() {
		return latch.getCount() < 1;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public Boolean get() throws InterruptedException, ExecutionException {
		latch.await();
		if(thrown!=null) throw new ExecutionException(thrown);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		final boolean complete = latch.await(timeout, unit);
		if(thrown!=null) throw new ExecutionException(thrown);
		return complete;
	}
	
	/**
	 * Marks the task as complete
	 */
	public void complete() {
		while(!isDone()) latch.countDown();
	}
	
	/**
	 * Fails the task
	 * @param ex The failure cause
	 */
	public void fail(final Exception ex) {
		thrown = ex;
		complete();
	}

}
