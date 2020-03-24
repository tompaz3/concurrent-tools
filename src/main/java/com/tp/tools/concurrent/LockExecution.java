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

import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface LockExecution<T> {

  <K> LockExecution<K> map(final Function<T, K> mapper);

  <K> LockExecution<K> flatMap(final Function<T, LockExecution<K>> mapper);

  LockExecution<T> filter(final Predicate<T> predicate);

  T execute();

  static <T> LockExecutionLockBuilder<T> withLock(final Lock lock) {
    return new LockExecutionLockBuilder<>(lock);
  }

  private static <T> LockExecution<T> of(final Lock lock, final Supplier<T> action) {
    return new LockExecutionSome<>(lock, action);
  }

  class LockExecutionLockBuilder<T> {

    private final Lock lock;

    private LockExecutionLockBuilder(final Lock lock) {
      this.lock = lock;
    }

    public LockExecution<T> execute(final Supplier<T> action) {
      return LockExecution.of(lock, action);
    }

    public LockExecution<Void> execute(final Runnable action) {
      return LockExecution.of(lock, () -> {
        action.run();
        return null;
      });
    }
  }

  interface LockExecutionWithAction<T> extends LockExecution<T> {

    Function<Void, T> action();
  }

  class LockExecutionNone<T> implements LockExecutionWithAction<T> {

    private static final LockExecution<?> NONE = new LockExecutionNone<>(() -> null);

    private final Function<Void, T> action;

    private LockExecutionNone(final Supplier<T> action) {
      this.action = ignore -> action.get();
    }

    @Override
    public Function<Void, T> action() {
      return action;
    }

    @Override
    public <K> LockExecution<K> map(final Function<T, K> mapper) {
      return none();
    }

    @Override
    public <K> LockExecution<K> flatMap(final Function<T, LockExecution<K>> mapper) {
      return none();
    }

    @Override
    public LockExecution<T> filter(final Predicate<T> predicate) {
      return this;
    }

    @Override
    public T execute() {
      return action.apply(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> LockExecution<T> none() {
      return (LockExecution<T>) NONE;
    }
  }

  class LockExecutionSome<T> implements LockExecutionWithAction<T> {

    private final Lock lock;
    private final Function<Void, T> action;

    private LockExecutionSome(final Lock lock, final Supplier<T> action) {
      this.lock = lock;
      this.action = ignore -> action.get();
    }

    @Override
    public Function<Void, T> action() {
      return action;
    }

    @Override
    public <K> LockExecution<K> map(final Function<T, K> mapper) {
      return LockExecution.of(lock, () -> this.action.andThen(mapper).apply(null));
    }

    @Override
    public <K> LockExecution<K> flatMap(final Function<T, LockExecution<K>> mapper) {
      return LockExecution.of(lock, () -> {
        final LockExecution<K> apply = this.action.andThen(mapper).apply(null);
        return ((LockExecutionWithAction<K>) apply).action().apply(null);
      });
    }

    @Override
    public LockExecution<T> filter(final Predicate<T> predicate) {
      return this.flatMap(ret -> predicate.test(ret) ? this : LockExecutionNone.none());
    }

    @Override
    public T execute() {
      lock.lock();
      try {
        return action.apply(null);
      } finally {
        lock.unlock();
      }
    }
  }
}
