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

import java.util.function.Function;

public interface TryLockExecute<T, E> {

  <K> TryLockExecute<K, E> map(Function<T, K> mapper);

  <K> TryLockExecute<K, E> flatMap(final Function<T, TryLockExecute<K, E>> mapper);

  <K> TryLockExecute<T, K> mapError(Function<E, K> mapper);

  <K> TryLockExecute<T, K> flatMapError(final Function<E, TryLockExecute<T, K>> mapper);

  T value();

  E error();

  static <T, E> TryLockExecute<T, E> of(final T value) {
    return new Success<>(ignore -> value);
  }

  static <T, E> TryLockExecute<T, E> ofError(final E error) {
    return new Failure<>(ignore -> error);
  }


  interface TryLockExecuteWithValues<T, E> extends TryLockExecute<T, E> {

    Function<Void, T> valueFunction();

    Function<Void, E> errorFunction();

    @Override
    default T value() {
      return valueFunction().apply(null);
    }

    @Override
    default E error() {
      return errorFunction().apply(null);
    }

    @Override
    default <K> TryLockExecute<K, E> map(final Function<T, K> mapper) {
      return new Success<>(ignore -> this.valueFunction().andThen(mapper).apply(null));
    }

    @Override
    default <K> TryLockExecute<K, E> flatMap(final Function<T, TryLockExecute<K, E>> mapper) {
      return new Success<>(ignore -> {
        final TryLockExecute<K, E> apply = this.valueFunction().andThen(mapper).apply(null);
        return ((TryLockExecuteWithValues<K, E>) apply).valueFunction().apply(null);
      });
    }

    @Override
    default <K> TryLockExecute<T, K> mapError(final Function<E, K> mapper) {
      return new Failure<>(ignore -> this.errorFunction().andThen(mapper).apply(null));
    }

    @Override
    default <K> TryLockExecute<T, K> flatMapError(final Function<E, TryLockExecute<T, K>> mapper) {
      return new Failure<>(ignore -> {
        final TryLockExecute<T, K> apply = this.errorFunction().andThen(mapper).apply(null);
        return ((TryLockExecuteWithValues<T, K>) apply).errorFunction().apply(null);
      });
    }
  }

  class Success<T, E> implements TryLockExecuteWithValues<T, E> {

    private static final Function<Void, ?> NO_ERROR_FUNCTION = ignore -> {
      throw new UnsupportedOperationException();
    };

    private final Function<Void, T> valueFunction;

    private Success(final Function<Void, T> valueFunction) {
      this.valueFunction = valueFunction;
    }

    @Override
    public Function<Void, T> valueFunction() {
      return valueFunction;
    }

    @Override
    public Function<Void, E> errorFunction() {
      return noErrorFunction();
    }

    @SuppressWarnings("unchecked")
    private static <E> Function<Void, E> noErrorFunction() {
      return (Function<Void, E>) NO_ERROR_FUNCTION;
    }
  }

  class Failure<T, E> implements TryLockExecuteWithValues<T, E> {

    private static final Function<Void, ?> NO_VALUE_FUNCTION = ignore -> {
      throw new UnsupportedOperationException();
    };

    private final Function<Void, E> errorFunction;

    private Failure(final Function<Void, E> errorFunction) {
      this.errorFunction = errorFunction;
    }

    @Override
    public Function<Void, T> valueFunction() {
      return noValueFunction();
    }

    @Override
    public Function<Void, E> errorFunction() {
      return errorFunction;
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<Void, T> noValueFunction() {
      return (Function<Void, T>) NO_VALUE_FUNCTION;
    }
  }
}
