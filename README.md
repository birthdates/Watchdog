# How does it work?

There is a main instance (`Watchdog`) which has a few useful functions

* `startWatching(long minTPS)` - Start watching the current thread and log if it goes under a certain TPS

* `stopWatching()` - Stop watching the current thread

* `tick()` - Tick the current thread **(IMPORTANT)**

You can initialize the Watchdog instance with `Watchdog.init(long checkDelay, boolean newThread)` and access it
with `Watchdog.getInstance()`

To stop Watchdog, simply use it's `stop` function

# Example

```java
import com.birthdates.watchdog.Watchdog;

public class WatchdogExample {
    public WatchdogExample() {
        //start watching the current thread for tps < 10
        Watchdog.init(5000L, true);
        Watchdog.getInstance().startWatching(10);
        try {
            //this will alert the watchdog
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        //tick the current thread (basically a heartbeat, saying you're alive)
        Watchdog.getInstance().tick();
    }
}
```