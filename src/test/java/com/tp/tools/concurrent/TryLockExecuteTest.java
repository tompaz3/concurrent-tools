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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TryLockExecuteTest {

  @Test
  void testValuesLeftIdentity() {
    // given
    final Function<Integer, TryLockExecute<Integer, String>> flatMapper = i -> i < 10
        ? TryLockExecute.ofError("Lower than 10")
        : TryLockExecute.of(i - 10);
    final var value = 20;
    final var expectedValue = 10;

    // when
    final TryLockExecute<Integer, String> tryLockExecute = TryLockExecute.<Integer, String>of(value)
        .flatMap(flatMapper);
    final TryLockExecute<Integer, String> tryLockExecute2 = TryLockExecute.of(expectedValue);
    final var value1 = tryLockExecute.value();
    final var value2 = tryLockExecute2.value();

    // then
    assertThat(value1).isEqualTo(value2);
  }

  @Test
  void testValuesRightIdentity() {
    // given
    final TryLockExecute<Integer, String> value = TryLockExecute.of(20);

    // when
    final TryLockExecute<Integer, String> tryLockExecute = value.flatMap(TryLockExecute::of);
    final var value1 = value.value();
    final var value2 = tryLockExecute.value();

    // then
    assertThat(value1).isEqualTo(value2);
  }

  @Test
  void testValuesAssociativityIdentity() {
    // given
    final Function<Integer, TryLockExecute<Integer, String>> flatMapper = i -> i < 15
        ? TryLockExecute.ofError("Lower than 15")
        : TryLockExecute.of(i * 2);
    final Function<Integer, TryLockExecute<Integer, String>> flatMapper2 = i -> i > 30
        ? TryLockExecute.of(i + 1)
        : TryLockExecute.ofError("Lower than 30");
    final TryLockExecute<Integer, String> value = TryLockExecute.of(20);

    // when
    final TryLockExecute<Integer, String> tryLockExecute = value
        .flatMap(flatMapper)
        .flatMap(flatMapper2);
    final TryLockExecute<Integer, String> tryLockExecute2 = value
        .flatMap(i -> flatMapper.apply(i).flatMap(flatMapper2));
    final var value1 = tryLockExecute.value();
    final var value2 = tryLockExecute2.value();

    // then
    assertThat(value1).isEqualTo(value2);
  }

  @Test
  void testErrorsLeftIdentity() {
    // given
    final var errorMsg = "This is an error";
    final var expectedMsg = "An error occurred";
    final Function<String, TryLockExecute<Integer, String>> flatMapper = e -> e.contains("error")
        ? TryLockExecute.ofError(expectedMsg)
        : TryLockExecute.of(5);

    // when
    final TryLockExecute<Integer, String> tryLockExecute = TryLockExecute.<Integer, String>ofError(
        errorMsg)
        .flatMapError(flatMapper);
    final TryLockExecute<Integer, String> tryLockExecute2 = TryLockExecute.ofError(expectedMsg);
    final var error1 = tryLockExecute.error();
    final var error2 = tryLockExecute2.error();

    // then
    assertThat(error1).isEqualTo(error2);
  }

  @Test
  void testErrorsRightIdentity() {
    // given
    final var errorMsg = "This is an error";
    final TryLockExecute<Integer, String> value = TryLockExecute.ofError(errorMsg);

    // when
    final TryLockExecute<Integer, String> tryLockExecute = value
        .flatMapError(TryLockExecute::ofError);
    final var error1 = value.error();
    final var error2 = tryLockExecute.error();

    // then
    assertThat(error1).isEqualTo(error2);
  }

  @Test
  void testErrorsAssociativityIdentity() {
    // given
    final var errorMsg = "This is an error";
    final var expectedMsg = "An error occurred";
    final Function<String, TryLockExecute<Integer, String>> flatMapper = e -> e.contains("error")
        ? TryLockExecute.ofError(expectedMsg)
        : TryLockExecute.of(5);
    final Function<String, TryLockExecute<Integer, String>> flatMapper2 = e -> expectedMsg.equals(e)
        ? TryLockExecute.ofError("What an error")
        : TryLockExecute.of(20);
    final TryLockExecute<Integer, String> value = TryLockExecute.ofError(errorMsg);

    // when
    final TryLockExecute<Integer, String> tryLockExecute = value
        .flatMapError(flatMapper)
        .flatMapError(flatMapper2);
    final TryLockExecute<Integer, String> tryLockExecute2 = value
        .flatMapError(i -> flatMapper.apply(i).flatMapError(flatMapper2));
    final var error1 = tryLockExecute.error();
    final var error2 = tryLockExecute2.error();

    // then
    assertThat(error1).isEqualTo(error2);
  }
}