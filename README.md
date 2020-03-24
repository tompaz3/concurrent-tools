# Concurrent tools
This project contains some simple concurrent tools:

1. `LockExecutionThreadFactory` - simple thread factory.
1. `LockExecution` - fluent API enabling tasks execution within the given lock.
1. `ReadWriteLock` - wrapper for `java.util.concurrent.locks.ReadWriteLock`, which uses `LockExecution` API.

Project uses [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Usage
### LockExecution

This interface allows functional style `.map()`, `.flatMap()` and `.filter()` operations, which are
supposed to be executed within a single lock context. More operations might be added to provide 
even more useful and fluent API.

Releases the lock using `try-finally` block.
This might be improved using some persistence in case error occurred resulting in impossibility to
call the `try-finally` block.

```java
private final Store<CarId, Car> cars = ... // some store containing cars by carIds

UpdatedCar updateIfExists(CarUpdated event) {
  return LockExecution.<Optional<Car>>withLock(writeLock()) // get write lock
    .execute(() -> cars.get(event.getCarId()))  // let's assume, store returns Optional<Car>
    .filter(Optional::isPresent)
    .map(Optional::get)
    .map(ignore -> event.toCar())   // this should be replaced in the future with more fitting API
    .map(car -> cars.store(car.getCarId(), car))
    .map(UpdatedCar::fromCar)
    .execute();
}
```


### ReadWriteLock
This tool uses `java.util.concurrent.locks.ReadWriteLock` to provide lock.
Exposes `LockExecution` fluent API via `read(Supplier)`, `write(Supplier)` and `write(Runnable)` 
methods.

Using `ReadWriteLock` provides lock and allows an example above to be rewritten to:
```java
private final ReadWriteLock lock = ReadWriteLock.newInstance();
private final Store<CarId, Car> cars = ... // some store containing cars by carIds

UpdatedCar updateIfExists(CarUpdated event) {
  return lock.write(() -> cars.get(event.getCarId())) // let's assume, store returns Optional<Car> 
    .execute(() -> cars.get(event.getCarId()))  
    .filter(Optional::isPresent)
    .map(Optional::get)
    .map(ignore -> event.toCar())   // this should be replaced in the future with more fitting API
    .map(car -> cars.store(car.getCarId(), car))
    .map(UpdatedCar::fromCar)
    .execute();
}
```

