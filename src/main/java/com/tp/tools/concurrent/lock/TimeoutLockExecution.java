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

package com.tp.tools.concurrent.lock;

import io.vavr.control.Try;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TimeoutLockExecution<T> implements LockExecution<T> {

  private final LockTimeout timeout;

  private final LockExecution<T> lockExecution;

  private TimeoutLockExecution(final LockTimeout timeout, final LockExecution<T> lockExecution) {
    this.timeout = timeout;
    this.lockExecution = lockExecution;
  }

  @Override
  public <K> LockExecution<K> map(final Function<T, K> mapper) {
    return new TimeoutLockExecution<>(timeout, lockExecution.map(mapper));
  }

  @Override
  public <K> LockExecution<K> flatMap(final Function<T, LockExecution<K>> mapper) {
    return new TimeoutLockExecution<>(timeout, lockExecution.flatMap(mapper));
  }

  @Override
  public LockExecution<Void> run(final Runnable runnable) {
    return new TimeoutLockExecution<>(timeout, lockExecution.run(runnable));
  }

  @Override
  public <K> LockExecution<K> supply(final Supplier<K> supplier) {
    return new TimeoutLockExecution<>(timeout, lockExecution.supply(supplier));
  }

  @Override
  public LockExecution<T> filter(final Predicate<T> predicate) {
    return new TimeoutLockExecution<>(timeout, lockExecution.filter(predicate));
  }

  @Override
  public Try<T> execute() {
    // normally, there should be only one instance of LockExecutionNone
    if (lockExecution == LockExecution.none() || lockExecution instanceof LockExecutionNone) {
      return lockExecution.execute();
    } else if (lockExecution instanceof LockExecutionSome) {
      final LockExecutionSome<T> some = (LockExecutionSome<T>) lockExecution;
      final Lock lock = some.lock();
      return Try.of(() -> lock.tryLock(timeout.getTimeout(), timeout.getUnit()))
          .filter(locked -> locked)
          .map(ignore -> some.action().apply(null))
          .andFinally(lock::unlock);
    } else {
      return Try.failure(new IllegalArgumentException("Unsupported LockExecution type"));
    }
  }

  static <T> TimeoutLockExecutionBuilder<T> builder(final LockExecution<T> lockExecution) {
    return new TimeoutLockExecutionBuilder<>(lockExecution);
  }

  public static class TimeoutLockExecutionBuilder<T> {

    private final LockExecution<T> lockExecution;

    private TimeoutLockExecutionBuilder(final LockExecution<T> lockExecution) {
      this.lockExecution = lockExecution;
    }

    public TimeoutLockExecution<T> nanos(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.nanos(timeout), lockExecution);
    }

    public TimeoutLockExecution<T> micros(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.micros(timeout), lockExecution);
    }

    public TimeoutLockExecution<T> millis(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.millis(timeout), lockExecution);
    }

    public TimeoutLockExecution<T> seconds(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.seconds(timeout), lockExecution);
    }

    public TimeoutLockExecution<T> minutes(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.minutes(timeout), lockExecution);
    }

    public TimeoutLockExecution<T> hours(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.hours(timeout), lockExecution);
    }

    public TimeoutLockExecution<T> days(final long timeout) {
      return new TimeoutLockExecution<>(LockTimeout.days(timeout), lockExecution);
    }
  }
}