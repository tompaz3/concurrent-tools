# Concurrent tools
This project contains some simple concurrent tools:

1. `LockExecutionThreadFactory` - simple thread factory.
1. `LockExecution` - fluent API enabling tasks execution within the given lock.
1. `ReadWriteLock` - wrapper for `java.util.concurrent.locks.ReadWriteLock`, which uses `LockExecution` API.

Project uses [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Dependencies

Project's dependencies are:
1. [Vavr.io](https://www.vavr.io/) - library influenced by functional programming paradigms, introducing fluent, immutable 
and monadic-like API into the Java world.

Project's test dependencies are:
1. [JUnit Jupiter](https://junit.org/junit5/docs/current/user-guide/) - Java testing framework (test dependency).
1. [AssertJ](https://joel-costigliola.github.io/assertj/) - Java fluent assertions framework (test dependency).

Maven plugins used by the project are:
1. [Maven Surefire Plugin](http://maven.apache.org/surefire/maven-surefire-plugin/) - used for tests execution.
1. [Maven Shade Plugin](http://maven.apache.org/plugins/maven-shade-plugin/) - used to build application fat JAR.
1. [JGitVer Maven Plugin](https://github.com/jgitver/jgitver-maven-plugin) - used for version managing, based on Git VCS.
1. [Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/) - used for artifact deployment.

## Usage
### LockExecution

This interface allows functional style `.map()`, `.flatMap()` and `.filter()` operations, which are
supposed to be executed within a single lock context. More operations might be added to provide 
even more useful and fluent API.

After having executed provided commands, the lock is released in the `try-finally` block.

`execute()` method returns [Vavr.io](https://www.vavr.io/) `Try<T>`.

This API supports timeout when waiting for lock acquisition, 
using `java.util.concurrent.locks.Lock.tryLock(long time, TimeUnit unit)`.

**Example**
```
private final Store<CarId, Car> cars = ... // some store containing cars by carIds

Try<UpdatedCar> updateIfExists(CarUpdated event) {
  return LockExecution.<Optional<Car>>withLock(writeLock()) // get write lock
    .execute(() -> cars.get(event.getCarId()))  // let's assume, store returns Optional<Car>
    .filter(Optional::isPresent)
    .map(Optional::get)
    .supply(event::toCar)
    .map(car -> cars.store(car.getCarId(), car))
    .map(UpdatedCar::fromCar)
    .execute();
}
```
**Example with timeout**
```
private final Store<CarId, Car> cars = ... // some store containing cars by carIds

Try<UpdatedCar> updateIfExists(CarUpdated event) {
  return LockExecution.<Optional<Car>>withLock(writeLock()) // get write lock, write lock 
                                                            // might be already locking this instance
                                                            // in a different thread
    .execute(() -> cars.get(event.getCarId()))  // let's assume, store returns Optional<Car>
    .filter(Optional::isPresent)
    .map(Optional::get)
    .supply(event::toCar)
    .map(car -> cars.store(car.getCarId(), car))
    .map(UpdatedCar::fromCar)
    .withLockTimeout()
    .seconds(2L)    // wait max. 2 seconds for lock acquiring
    .execute();
}
```

#### Methods:

* `.map(Function<T,K> mapper)` - applies `mapper` function to the current execution result
and returns `LockExecution<K>` instance.
* `.flatMap(Function<T,LockExecution<K>> mapper)` - applies `mapper` function to the current execution
result and returns `mapper` result (`LockExecution<K>` instance).
* `.supply(Supplier<K> supplier)` - ignores current execution result and generates new result.
Returns `LockExecution<K>` instance with supplied result.
* `.run(Runnable runnable)` - ignores current execution result and executes passed `runnable`.
Always returns `LockExecution<Void>` type, which does not hold any result (`null`). 
_// TODO: find better return value than `null` for this case_
* `.filter(Predicate<T> predicate)` - applies `predicate` to the current execution result.
    * if result doesn't pass the test, `LockExection.none()` is returned, which cannot perform
    any operations and will return `null` value. _// TODO: find better return value than `null` 
    for this case_
    * if result passes the test, current `LockExecution` instance is returned.
* `.withLockTimeout()` - use when you want your lock to be executed with timeout, 
  using `Lock.tryLock(long ,TimeUnit)`. This returns builder for `TimeoutLockExecution` which is
  `LockExecution` implementation supporting timeout when acquiring the lock.

### ReadWriteLock
This tool uses `java.util.concurrent.locks.ReadWriteLock` to provide lock.
Exposes `LockExecution` fluent API via `read(Supplier)`, `write(Supplier)` and `write(Runnable)` 
methods.

Using `ReadWriteLock` provides lock and allows an example above to be rewritten to:
```
private final ReadWriteLock lock = ReadWriteLock.newInstance();
private final Store<CarId, Car> cars = ... // some store containing cars by carIds

Try<UpdatedCar> updateIfExists(CarUpdated event) {
  return lock.write(() -> cars.get(event.getCarId())) // let's assume, store returns Optional<Car> 
    .execute(() -> cars.get(event.getCarId()))  
    .filter(Optional::isPresent)
    .map(Optional::get)
    .supply(event::toCar)
    .map(car -> cars.store(car.getCarId(), car))
    .map(UpdatedCar::fromCar)
    .execute();
}
```

