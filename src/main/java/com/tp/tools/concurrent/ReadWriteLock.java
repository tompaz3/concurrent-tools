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
