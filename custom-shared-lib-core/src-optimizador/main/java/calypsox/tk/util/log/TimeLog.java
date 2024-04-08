package calypsox.tk.util.log;

import com.calypso.tk.core.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static java.time.Instant.now;

public final class TimeLog {

    private TimeLog() {
        super();
    }

    private static Logger log = LoggerFactory.getLogger(TimeLog.class);

    public static <T> T timeLog(String msg, Supplier<T> supplier) {
        Instant startTime = now();

        T result = supplier.get();

        Duration d = Duration.between(startTime, now());
        long hours = d.toHours();
        long minutes = d.minusHours(hours).toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        Log.system("TIME-LOG",
                String.format("TIME-LOG: %s tardo %d msegs => %d:%d:%d", msg, d.toMillis(), hours, minutes, seconds));

        return result;
    }

    public static long timeLog(String msg, Runnable runnable) {
        Instant startTime = now();

        runnable.run();

        Duration d = Duration.between(startTime, now());
        long hours = d.toHours();
        long minutes = d.minusHours(hours).toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        long ellapsedMilliseconds = d.toMillis();
        Log.system("TIME-LOG",
                String.format("TIME-LOG: %s tardo %d msegs => %d:%d:%d", msg, ellapsedMilliseconds, hours, minutes, seconds));

        return ellapsedMilliseconds;
    }

}
