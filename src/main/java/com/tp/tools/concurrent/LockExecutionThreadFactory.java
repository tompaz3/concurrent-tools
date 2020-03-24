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
