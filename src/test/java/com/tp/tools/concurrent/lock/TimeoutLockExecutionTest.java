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

import static com.tp.tools.concurrent.lock.TestUtils.sleep;
import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.control.Try;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.Test;

class TimeoutLockExecutionTest {

  @Test
  void shouldWaitTillLockReleasedWhenTimeoutExceedsWaitDuration() {
    // given lock
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    // and executor service
    final ExecutorService executorService = TestUtils.fixedThreadPoolExecutor(2);
    // and countdown latch
    final CountDownLatch latch = new CountDownLatch(2);
    // and timeout millis
    final long timeoutMillis = 500L;

    // and task with write lock decreasing latch
    final LockExecution<Void> lockingTask = LockExecution.<Void>withLock(lock.writeLock())
        .execute(() -> {
          if (latch.getCount() != 2L) {
            throw new IllegalStateException();
          }
          latch.countDown();
        })
        .run(() -> sleep(300L));
    // and task with lock timeout
    final TimeoutLockExecution<Void> awaitingTask = LockExecution.<Void>withLock(lock.readLock())
        .execute(() -> {
          if (latch.getCount() != 1L) {
            throw new IllegalStateException();
          }
          latch.countDown();
        })
        .withLockTimeout()
        .millis(timeoutMillis);

    // when run locking task future
    final CompletableFuture<Try<Void>> lockingFuture = CompletableFuture
        .supplyAsync(lockingTask::execute, executorService);
    // and 50 ms later run awaiting task future
    sleep(50L);
    final CompletableFuture<Try<Void>> awaitingFuture = CompletableFuture
        .supplyAsync(awaitingTask::execute, executorService);

    // then locking task succeeded
    assertThat(lockingFuture.join().isSuccess()).isTrue();
    // and awaiting task succeeded
    assertThat(awaitingFuture.join().isSuccess()).isTrue();
    // and countdown latch is 0
    assertThat(latch.getCount()).isEqualTo(0L);
  }

  @Test
  void shouldFailWhenTimeoutIsLessThanWaitingDuration() {
    // given lock
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    // and executor service
    final ExecutorService executorService = TestUtils.fixedThreadPoolExecutor(2);
    // and countdown latch
    final CountDownLatch latch = new CountDownLatch(2);
    // and timeout millis
    final long timeoutMillis = 350L;
    // and waiting duration millis
    final long waitingDurationMillis = 500L;

    // and task with write lock decreasing latch
    final LockExecution<Void> lockingTask = LockExecution.<Void>withLock(lock.writeLock())
        .execute(() -> {
          if (latch.getCount() != 2L) {
            throw new IllegalStateException();
          }
          latch.countDown();
        })
        .run(() -> sleep(waitingDurationMillis));
    // and task with lock timeout
    final TimeoutLockExecution<Void> awaitingTask = LockExecution.<Void>withLock(lock.readLock())
        .execute(() -> {
          if (latch.getCount() != 1L) {
            throw new IllegalStateException();
          }
          latch.countDown();
        })
        .withLockTimeout()
        .millis(timeoutMillis);

    // when run locking task future
    final CompletableFuture<Try<Void>> lockingFuture = CompletableFuture
        .supplyAsync(lockingTask::execute, executorService);
    // and 50 ms later run awaiting task future
    sleep(50L);
    final CompletableFuture<Try<Void>> awaitingFuture = CompletableFuture
        .supplyAsync(awaitingTask::execute, executorService);

    // then locking task succeeded
    assertThat(lockingFuture.join().isSuccess()).isTrue();
    // and awaiting task failed
    assertThat(awaitingFuture.join().isFailure()).isTrue();
    // and countdown latch is 1
    assertThat(latch.getCount()).isEqualTo(1L);
  }
}