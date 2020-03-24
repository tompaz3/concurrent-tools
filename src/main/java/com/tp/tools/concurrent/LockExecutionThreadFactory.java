/*
 * Copyright 2020 Tomasz Paździurek <t.pazdziurek@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tp.tools.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class LockExecutionThreadFactory extends AtomicLong implements ThreadFactory {

  private static final long serialVersionUID = 6513315664631794722L;
  private static final long INITIAL_VALUE = 0L;
  private final String prefix;
  private final int priority;
  private final boolean nonBlocking;

  public LockExecutionThreadFactory(final String prefix, final int priority,
      final boolean nonBlocking) {
    super(INITIAL_VALUE);
    this.prefix = prefix;
    this.priority = priority;
    this.nonBlocking = nonBlocking;
  }

  @Override
  public Thread newThread(final Runnable runnable) {
    final String name = this.prefix + '-' + this.incrementAndGet();
    final Thread thread =
        this.nonBlocking ? new LockExecutionThread(runnable, name) : new Thread(runnable, name);
    thread.setPriority(this.priority);
    thread.setDaemon(true);
    return thread;
  }

  @Override
  public String toString() {
    return LockExecutionThreadFactory.class.getSimpleName() + "[" + this.prefix + "]";
  }

  static final class LockExecutionThread extends Thread {

    LockExecutionThread(final Runnable run, final String name) {
      super(run, name);
    }
  }
}
