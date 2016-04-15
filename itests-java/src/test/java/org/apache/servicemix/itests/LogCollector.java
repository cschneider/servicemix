package org.apache.servicemix.itests;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.internal.runners.statements.Fail;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.BundleContext;

public class LogCollector implements PaxAppender {

    List<PaxLoggingEvent> log = new ArrayList<>();

    public LogCollector(BundleContext context) {
        Dictionary<String, String> props = new Hashtable<>();
        props.put("org.ops4j.pax.logging.appender.name", "ITestLogAppender");
        context.registerService(PaxAppender.class, this, props);
    }

    @Override
    public synchronized void doAppend(PaxLoggingEvent event) {
        log.add(event);
        this.notify();
    }

    public synchronized void expectContains(String message) throws InterruptedException, TimeoutException {
        for (PaxLoggingEvent event : log) {
            if (event.getMessage().contains(message)) {
                return;
            }
        }
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 10 * 1000) {
            this.wait(100);
            PaxLoggingEvent event = log.get(log.size()-1);
            if (event.getMessage().contains(message)) {
                return;
            }
        }
        throw new TimeoutException("Timeout waiting for log message containing " + message);
    }
}
