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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class TestUtils {

  private TestUtils() {
    throw new UnsupportedOperationException("Cannot instantiate utility class.");
  }

  public static void sleep(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static ExecutorService fixedThreadPoolExecutor(final int threads) {
    final ThreadFactory threadFactory = new LockExecutionThreadFactory(
        LockExecutionTest.class.getSimpleName(), 1, true);
    return Executors.newFixedThreadPool(threads, threadFactory);
  }
}
