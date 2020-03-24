package com.tp.tools.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ReadWriteLock {

  private final java.util.concurrent.locks.ReadWriteLock lock;

  public ReadWriteLock(final java.util.concurrent.locks.ReadWriteLock lock) {this.lock = lock;}

  public <T> LockExecution<T> read(final Supplier<T> read) {
    return LockExecution.<T>withLock(lock.readLock()).execute(read);
  }

  public <T> LockExecution<T> write(final Supplier<T> write) {
    return LockExecution.<T>withLock(lock.writeLock()).execute(write);
  }

  public LockExecution<Void> write(final Runnable write) {
    return LockExecution.withLock(lock.writeLock()).execute(write);
  }

  public static ReadWriteLock newInstance() {
    return new ReadWriteLock(new ReentrantReadWriteLock());
  }

  public static ReadWriteLock newInstanceFair() {
    return new ReadWriteLock(new ReentrantReadWriteLock(true));
  }
}
