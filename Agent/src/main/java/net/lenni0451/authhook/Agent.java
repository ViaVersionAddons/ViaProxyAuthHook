package net.lenni0451.authhook;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public class Agent {

    public static void agentmain(final String args, final Instrumentation instrumentation) {
        hook(instrumentation);
    }

    public static void premain(final String args, final Instrumentation instrumentation) {
        hook(instrumentation);
    }

    private static void hook(final Instrumentation instrumentation) {
        try {
            Map<String, String> config = Config.load();
            instrumentation.addTransformer(new URLRedirector(config), true);
        } catch (Throwable t) {
            System.err.println("An error occurred while starting AuthHook");
            t.printStackTrace();
            System.exit(-1);
        }
    }

}
