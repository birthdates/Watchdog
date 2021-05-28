# How does it work?

There is a main instance (`Watchdog`) which has a few useful functions

* `startWatching(long minTPS)` - Start watching the current thread and log if it goes under a certain TPS

* `stopWatching()` - Stop watching the current thread

* `tick()` - Tick the current thread **(IMPORTANT)**

You can initialize the Watchdog instance with `Watchdog.init(long checkDelay)` and access it
with `Watchdog.getInstance()`

To stop Watchdog, simply use it's `stop` function