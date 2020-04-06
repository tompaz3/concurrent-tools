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

import java.util.function.Consumer;
import java.util.function.Function;

// TODO: this should be in some different library, actually
public interface TryExecute<E, T> {

  <K> TryExecute<E, K> map(Function<T, K> mapper);

  <K> TryExecute<E, K> flatMap(final Function<T, TryExecute<E, K>> mapper);

  <K> TryExecute<K, T> mapError(Function<E, K> mapper);

  <K> TryExecute<K, T> flatMapError(final Function<E, TryExecute<K, T>> mapper);

  T value();

  E error();

  boolean isSuccess();

  default boolean isError() {
    return !isSuccess();
  }

  //region functions for side effects handling
  TryExecute<E, T> onSuccess(Consumer<T> action);

  TryExecute<E, T> onError(Consumer<E> action);

  TryExecute<E, T> onSuccess(Runnable action);

  TryExecute<E, T> onError(Runnable action);
  //endregion


  static <E, T> TryExecute<E, T> of(final T value) {
    return new Success<>(ignore -> value);
  }

  static <E, T> TryExecute<E, T> ofError(final E error) {
    return new Failure<>(ignore -> error);
  }


  interface TryExecuteWithValues<E, T> extends TryExecute<E, T> {

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
    default <K> TryExecute<E, K> map(final Function<T, K> mapper) {
      return new Success<>(ignore -> this.valueFunction().andThen(mapper).apply(null));
    }

    @Override
    default <K> TryExecute<E, K> flatMap(final Function<T, TryExecute<E, K>> mapper) {
      return new Success<>(ignore -> {
        final TryExecute<E, K> apply = this.valueFunction().andThen(mapper).apply(null);
        return ((TryExecuteWithValues<E, K>) apply).valueFunction().apply(null);
      });
    }

    @Override
    default <K> TryExecute<K, T> mapError(final Function<E, K> mapper) {
      return new Failure<>(ignore -> this.errorFunction().andThen(mapper).apply(null));
    }

    @Override
    default <K> TryExecute<K, T> flatMapError(final Function<E, TryExecute<K, T>> mapper) {
      return new Failure<>(ignore -> {
        final TryExecute<K, T> apply = this.errorFunction().andThen(mapper).apply(null);
        return ((TryExecuteWithValues<K, T>) apply).errorFunction().apply(null);
      });
    }

    @Override
    default TryExecute<E, T> onSuccess(final Consumer<T> action) {
      return map(value -> {
        action.accept(value);
        return value;
      });
    }

    @Override
    default TryExecute<E, T> onError(final Consumer<E> action) {
      return mapError(error -> {
        action.accept(error);
        return error;
      });
    }

    @Override
    default TryExecute<E, T> onSuccess(final Runnable action) {
      return map(value -> {
        action.run();
        return value;
      });
    }

    @Override
    default TryExecute<E, T> onError(final Runnable action) {
      return mapError(error -> {
        action.run();
        return error;
      });
    }
  }

  class Success<E, T> implements TryExecuteWithValues<E, T> {

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

    @Override
    public boolean isSuccess() {
      return true;
    }
  }

  class Failure<E, T> implements TryExecuteWithValues<E, T> {

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

    @Override
    public boolean isSuccess() {
      return false;
    }
  }
}
