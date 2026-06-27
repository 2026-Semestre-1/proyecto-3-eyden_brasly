package gui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadDispatchPrintStream extends PrintStream {
    private final DispatcherOutputStream dispatcher;

    private ThreadDispatchPrintStream(DispatcherOutputStream dispatcher) {
        super(dispatcher, true, StandardCharsets.UTF_8);
        this.dispatcher = dispatcher;
    }

    public static ThreadDispatchPrintStream install() {
        PrintStream fallback = System.out;
        ThreadDispatchPrintStream stream = new ThreadDispatchPrintStream(new DispatcherOutputStream(fallback));
        System.setOut(stream);
        System.setErr(stream);
        return stream;
    }

    public void register(Thread thread, OutputStream outputStream) {
        dispatcher.register(thread, outputStream);
    }

    public void unregister(Thread thread) {
        dispatcher.unregister(thread);
    }

    private static class DispatcherOutputStream extends OutputStream {
        private final PrintStream fallback;
        private final Map<Long, OutputStream> outputsByThread;

        DispatcherOutputStream(PrintStream fallback) {
            this.fallback = fallback;
            this.outputsByThread = new ConcurrentHashMap<>();
        }

        void register(Thread thread, OutputStream outputStream) {
            outputsByThread.put(thread.threadId(), outputStream);
        }

        void unregister(Thread thread) {
            outputsByThread.remove(thread.threadId());
        }

        @Override
        public void write(int value) throws IOException {
            OutputStream target = outputsByThread.get(Thread.currentThread().threadId());
            if (target == null) {
                fallback.write(value);
                fallback.flush();
                return;
            }

            target.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            OutputStream target = outputsByThread.get(Thread.currentThread().threadId());
            if (target == null) {
                fallback.write(bytes, offset, length);
                fallback.flush();
                return;
            }

            target.write(bytes, offset, length);
        }

        @Override
        public void flush() throws IOException {
            OutputStream target = outputsByThread.get(Thread.currentThread().threadId());
            if (target == null) {
                fallback.flush();
                return;
            }

            target.flush();
        }
    }
}
