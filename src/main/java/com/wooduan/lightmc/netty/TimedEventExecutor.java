package com.wooduan.lightmc.netty;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default {@link SingleThreadEventExecutor} implementation which just execute all submitted task in a
 * serial fashion.
 */
public final class TimedEventExecutor extends SingleThreadEventExecutor {
	volatile long totalTakeTime;
	volatile long totalExecTime;
    public TimedEventExecutor() {
        this((EventExecutorGroup) null);
    }

    public TimedEventExecutor(ThreadFactory threadFactory) {
        this(null, threadFactory);
    }

    public TimedEventExecutor(Executor executor) {
        this(null, executor);
    }

    public TimedEventExecutor(EventExecutorGroup parent) {
        this(parent, new DefaultThreadFactory(TimedEventExecutor.class));
    }

    public TimedEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory) {
        super(parent, threadFactory, true);
    }

    public TimedEventExecutor(EventExecutorGroup parent, Executor executor) {
        super(parent, executor, true);
    }

    @Override
    protected void run() {
        for (;;) {
        	long takeStart = System.nanoTime();
            Runnable task = takeTask();
            long takeEnd = System.nanoTime();
            totalTakeTime += takeEnd-takeStart;
            
            if (task != null) {
            	
                task.run();
                long runEnd = System.nanoTime();
                totalExecTime += runEnd - takeEnd;
                
                updateLastExecutionTime();
            }

            if (confirmShutdown()) {
                break;
            }
        }
    }
    
    public void resetTime()
    {
    	totalExecTime = 0;
    	totalTakeTime = 0;
    }
    
    public long getTotalExecTime() {
		return totalExecTime;
	}
    
    public long getTotalTakeTime() {
		return totalTakeTime;
	}
}

