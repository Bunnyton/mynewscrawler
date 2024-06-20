package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
public final class Logger {

    public static void info(final String string, final Object... args) {
        if (string == null) {
            return;
        }
        System.out.printf((string) + "%n", args);
    }

    public static void err(final String string, final Object... args) {
        if (string == null) {
            return;
        }
        System.err.printf((string) + "%n", args);
    }

    public static void logException(Throwable throwable) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();
        System.err.println("Exception happened!\n" + throwable + "\n" + stackTrace);
    }

}
