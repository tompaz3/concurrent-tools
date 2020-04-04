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

// TODO: this should be in some different library, actually
class TryExecuteTest {

  @Test
  void testValuesLeftIdentity() {
    // given
    final Function<Integer, TryExecute<Integer, String>> flatMapper = i -> i < 10
        ? TryExecute.ofError("Lower than 10")
        : TryExecute.of(i - 10);
    final var value = 20;
    final var expectedValue = 10;

    // when
    final TryExecute<Integer, String> tryExecute = TryExecute.<Integer, String>of(value)
        .flatMap(flatMapper);
    final TryExecute<Integer, String> tryExecute2 = TryExecute.of(expectedValue);
    final var value1 = tryExecute.value();
    final var value2 = tryExecute2.value();

    // then
    assertThat(value1).isEqualTo(value2);
  }

  @Test
  void testValuesRightIdentity() {
    // given
    final TryExecute<Integer, String> value = TryExecute.of(20);

    // when
    final TryExecute<Integer, String> tryExecute = value.flatMap(TryExecute::of);
    final var value1 = value.value();
    final var value2 = tryExecute.value();

    // then
    assertThat(value1).isEqualTo(value2);
  }

  @Test
  void testValuesAssociativityIdentity() {
    // given
    final Function<Integer, TryExecute<Integer, String>> flatMapper = i -> i < 15
        ? TryExecute.ofError("Lower than 15")
        : TryExecute.of(i * 2);
    final Function<Integer, TryExecute<Integer, String>> flatMapper2 = i -> i > 30
        ? TryExecute.of(i + 1)
        : TryExecute.ofError("Lower than 30");
    final TryExecute<Integer, String> value = TryExecute.of(20);

    // when
    final TryExecute<Integer, String> tryExecute = value
        .flatMap(flatMapper)
        .flatMap(flatMapper2);
    final TryExecute<Integer, String> tryExecute2 = value
        .flatMap(i -> flatMapper.apply(i).flatMap(flatMapper2));
    final var value1 = tryExecute.value();
    final var value2 = tryExecute2.value();

    // then
    assertThat(value1).isEqualTo(value2);
  }

  @Test
  void testErrorsLeftIdentity() {
    // given
    final var errorMsg = "This is an error";
    final var expectedMsg = "An error occurred";
    final Function<String, TryExecute<Integer, String>> flatMapper = e -> e.contains("error")
        ? TryExecute.ofError(expectedMsg)
        : TryExecute.of(5);

    // when
    final TryExecute<Integer, String> tryExecute = TryExecute.<Integer, String>ofError(
        errorMsg)
        .flatMapError(flatMapper);
    final TryExecute<Integer, String> tryExecute2 = TryExecute.ofError(expectedMsg);
    final var error1 = tryExecute.error();
    final var error2 = tryExecute2.error();

    // then
    assertThat(error1).isEqualTo(error2);
  }

  @Test
  void testErrorsRightIdentity() {
    // given
    final var errorMsg = "This is an error";
    final TryExecute<Integer, String> value = TryExecute.ofError(errorMsg);

    // when
    final TryExecute<Integer, String> tryExecute = value
        .flatMapError(TryExecute::ofError);
    final var error1 = value.error();
    final var error2 = tryExecute.error();

    // then
    assertThat(error1).isEqualTo(error2);
  }

  @Test
  void testErrorsAssociativityIdentity() {
    // given
    final var errorMsg = "This is an error";
    final var expectedMsg = "An error occurred";
    final Function<String, TryExecute<Integer, String>> flatMapper = e -> e.contains("error")
        ? TryExecute.ofError(expectedMsg)
        : TryExecute.of(5);
    final Function<String, TryExecute<Integer, String>> flatMapper2 = e -> expectedMsg.equals(e)
        ? TryExecute.ofError("What an error")
        : TryExecute.of(20);
    final TryExecute<Integer, String> value = TryExecute.ofError(errorMsg);

    // when
    final TryExecute<Integer, String> tryExecute = value
        .flatMapError(flatMapper)
        .flatMapError(flatMapper2);
    final TryExecute<Integer, String> tryExecute2 = value
        .flatMapError(i -> flatMapper.apply(i).flatMapError(flatMapper2));
    final var error1 = tryExecute.error();
    final var error2 = tryExecute2.error();

    // then
    assertThat(error1).isEqualTo(error2);
  }
}