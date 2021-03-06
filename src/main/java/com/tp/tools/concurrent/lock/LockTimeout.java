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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class LockTimeout {

  private final long timeout;
  private final TimeUnit unit;

  private LockTimeout(final long timeout, final TimeUnit unit) {
    this.timeout = timeout;
    this.unit = unit;
  }

  long getTimeout() {
    return timeout;
  }

  TimeUnit getUnit() {
    return unit;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LockTimeout)) {
      return false;
    }
    final LockTimeout that = (LockTimeout) o;
    return timeout == that.timeout &&
        unit == that.unit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(timeout, unit);
  }

  static LockTimeout nanos(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.NANOSECONDS);
  }

  static LockTimeout micros(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.MICROSECONDS);
  }

  static LockTimeout millis(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.MILLISECONDS);
  }

  static LockTimeout seconds(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.SECONDS);
  }

  static LockTimeout minutes(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.MINUTES);
  }

  static LockTimeout hours(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.HOURS);
  }

  static LockTimeout days(final long timeout) {
    return new LockTimeout(timeout, TimeUnit.DAYS);
  }
}
