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

import com.tp.tools.concurrent.lock.LockExecution.LockExecutionNone.LockExecutionLockBuilder;
import com.tp.tools.concurrent.lock.LockExecution.LockExecutionNone.LockExecutionSome;
import io.vavr.control.Try;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>Fluent lock execution API.</p>
 * <p></p>
 * <p>Allows user to chain consequent executions within a single lock and
 * execute them in a single call.</p>
 * <p>This is not a monad (does not comply with identity or associativity rules), but is strongly
 * inspired by functional programming patterns</p>
 *
 * <p>Example usage:</p>
 * <p>
 * <code>
 * <br/>
 * <br/>private final Store<CarId, Car> cars = ... // some store containing cars by carIds
 * <br/>
 * <br/>UpdatedCar updateIfExists(CarUpdated event) {
 * <br/>&nbsp;&nbsp;return LockExecution.<Optional<Car>>withLock(writeLock()) // get write lock
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.execute(() -> cars.get(event.getCarId()))  // let's assume, store
 * returns Optional<Car>
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.filter(Optional::isPresent)
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.map(Optional::get)
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.supply(ignore -> event.toCar())
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.map(car -> cars.store(car.getCarId(), car))
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.map(UpdatedCar::fromCar)
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;.execute();
 * <br/>}
 * </code>
 * </p>
 *
 * @param <T> return type of the execution.
 */
public interface LockExecution<T> {

  <K> LockExecution<K> map(final Function<T, K> mapper);

  <K> LockExecution<K> flatMap(final Function<T, LockExecution<K>> mapper);

  LockExecution<Void> run(final Runnable runnable);

  <K> LockExecution<K> supply(final Supplier<K> supplier);

  LockExecution<T> filter(final Predicate<T> predicate);

  Try<T> execute();

  static <T> LockExecutionNone.LockExecutionLockBuilder<T> withLock(final Lock lock) {
    return new LockExecutionLockBuilder<>(lock);
  }

  @SuppressWarnings("unchecked")
  static <T> LockExecution<T> none() {
    return (LockExecution<T>) LockExecutionNone.NONE;
  }

  private static <T> LockExecution<T> of(final Lock lock, final Supplier<T> action) {
    return new LockExecutionSome<>(lock, action);
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
      return LockExecution.none();
    }

    @Override
    public <K> LockExecution<K> flatMap(final Function<T, LockExecution<K>> mapper) {
      return LockExecution.none();
    }

    @Override
    public LockExecution<Void> run(final Runnable runnable) {
      return LockExecution.none();
    }

    @Override
    public <K> LockExecution<K> supply(final Supplier<K> supplier) {
      return LockExecution.none();
    }

    @Override
    public LockExecution<T> filter(final Predicate<T> predicate) {
      return this;
    }

    @Override
    public Try<T> execute() {
      return Try.success(null);
    }

    static class LockExecutionSome<T> implements LockExecutionWithAction<T> {

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
      public LockExecution<Void> run(final Runnable runnable) {
        return LockExecution.of(lock, () ->
            this.action
                .andThen(ignore -> runRunnable(runnable))
                .apply(null)
        );
      }

      @Override
      public <K> LockExecution<K> supply(final Supplier<K> supplier) {
        return LockExecution.of(lock, () ->
            this.action
                .andThen(ignore -> supplier.get())
                .apply(null)
        );
      }

      @Override
      public LockExecution<T> filter(final Predicate<T> predicate) {
        return this.flatMap(ret -> predicate.test(ret) ? this : LockExecution.none());
      }

      @Override
      public Try<T> execute() {
        try {
          lock.lock();
        } catch (final Exception e) {
          return Try.failure(e);
        }
        try {
          return Try.ofSupplier(() -> action.apply(null));
        } catch (final Exception e) {
          return Try.failure(e);
        } finally {
          lock.unlock();
        }
      }

      private Void runRunnable(final Runnable runnable) {
        runnable.run();
        return null;
      }
    }

    //region builders
    static class LockExecutionLockBuilder<T> {

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
    //endregion
  }
}
