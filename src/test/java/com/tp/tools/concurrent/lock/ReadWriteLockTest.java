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

import com.tp.tools.concurrent.TryExecute;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ReadWriteLockTest {

  @Test
  void shouldBeLockedWhenTryingToExecuteConcurrentOperation() {
    // given lock
    final ReadWriteLock lock = ReadWriteLock.newInstance();
    // and list with 2 items
    final List<String> list = new ArrayList<>();
    list.add("A1");
    list.add("A2");
    // and expected size
    final int expectedSize = 3;

    // and executor service
    final ExecutorService executorService = givenExecutorService();

    // and countdown latch
    final CountDownLatch latch = new CountDownLatch(3);

    // and task with write lock increasing list number and sleeping 250 ms before and 250 ms after change

    final LockExecution<Void> addListElementTask = lock.write(() -> list.get(0))
        .map(ignore -> {
          sleep(250);
          return null;
        })
        .map(ignore -> {
          list.add("A3");
          return null;
        })
        .map(ignore -> {
          sleep(250);
          if (latch.getCount() != 3L) {
            throw new IllegalStateException();
          }
          latch.countDown();
          return null;
        });
    // and another task with read lock which gets list size
    final LockExecution<Integer> getListSizeTaskWithLock = lock.read(() -> list.get(1))
        .map(ignore -> {
          if (latch.getCount() > 2L) {
            throw new IllegalStateException();
          }
          latch.countDown();
          return null;
        })
        .map(ignore -> list.size());
    // and another task without lock which gets list size
    final Supplier<Integer> getListTaskWithoutLock = () -> {
      if (latch.getCount() > 2L) {
        throw new IllegalStateException();
      }
      latch.countDown();
      return list.size();
    };

    // when run first future
    final CompletableFuture<TryExecute<Throwable, Void>> firstFuture = CompletableFuture
        .supplyAsync(addListElementTask::execute, executorService);
    // and 100 ms later run second future
    sleep(100);
    final CompletableFuture<TryExecute<Throwable, Integer>> secondFuture = CompletableFuture
        .supplyAsync(getListSizeTaskWithLock::execute, executorService);
    // and run third future
    final CompletableFuture<Integer> thirdFuture = CompletableFuture
        .supplyAsync(getListTaskWithoutLock, executorService);

    // then second future returns expected size
    Assertions.assertThat(secondFuture.join().value()).isEqualTo(expectedSize);
    // and third future returns expected size
    Assertions.assertThat(thirdFuture.join()).isEqualTo(expectedSize);
    // and latch is already 0
    Assertions.assertThat(latch.getCount()).isEqualTo(0L);
  }

  @Test
  void shouldIgnoreReadLock() {
    // given lock
    final ReadWriteLock lock = ReadWriteLock.newInstance();
    // and list with 2 items
    final List<String> list = new ArrayList<>();
    list.add("A1");
    list.add("A2");
    // and initial size
    final int initialSize = list.size();

    // and executor service
    final ExecutorService executorService = givenExecutorService();

    // and countdown latch
    final CountDownLatch latch = new CountDownLatch(3);

    // and task read write lock increasing list number and sleeping 500 ms before and 250 ms after change
    final LockExecution<Void> addListElementTask = lock.read(() -> list.get(0))
        .map(ignore -> {
          sleep(500);
          return null;
        })
        .map(ignore -> {
          list.add("A3");
          return null;
        })
        .map(ignore -> {
          sleep(250);
          latch.countDown();
          return null;
        });
    // and another task with read lock which gets list size
    final LockExecution<Integer> getListSizeTaskWithLock = lock.read(() -> list.get(1))
        .map(ignore -> {
          latch.countDown();
          return null;
        })
        .map(ignore -> list.size());
    // and another task without lock which gets list size
    final Supplier<Integer> getListTaskWithoutLock = () -> {
      latch.countDown();
      return list.size();
    };

    // when run first future
    final CompletableFuture<TryExecute<Throwable, Void>> firstFuture = CompletableFuture
        .supplyAsync(addListElementTask::execute, executorService);
    // and 100 ms later run second future
    sleep(100);
    final CompletableFuture<TryExecute<Throwable, Integer>> secondFuture = CompletableFuture
        .supplyAsync(getListSizeTaskWithLock::execute, executorService);
    // and run third future
    final CompletableFuture<Integer> thirdFuture = CompletableFuture
        .supplyAsync(getListTaskWithoutLock, executorService);

    // then second future returns expected size
    Assertions.assertThat(secondFuture.join().value()).isEqualTo(initialSize);
    // and third future returns expected size
    Assertions.assertThat(thirdFuture.join()).isEqualTo(initialSize);
    // and latch not yet finished counting down
    Assertions.assertThat(latch.getCount()).isGreaterThan(0L);
  }

  private ExecutorService givenExecutorService() {
    final ThreadFactory threadFactory = new LockExecutionThreadFactory(
        LockExecutionTest.class.getSimpleName(), 1, true);
    return Executors.newFixedThreadPool(2, threadFactory);
  }

  private static void sleep(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
