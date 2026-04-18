package lessons.inner_classes;

/**
 * LESSON 13A — INNER CLASSES IN JAVA
 *
 * Inner class categories:
 * 1) Member inner class (non-static)
 * 2) Static nested class
 * 3) Local inner class (inside a method)
 * 4) Anonymous inner class
 *
 * SDE2 interview focus:
 * - Prefer static nested class unless outer instance state is required.
 * - Member inner classes keep an implicit reference to outer instance.
 * - Local/anonymous classes can capture effectively final local variables.
 */
public class InnerClassesDemo {

    private final String appName = "InterviewPrepApp";
    private static final String APP_VERSION = "1.0.0";

    // 1) Member inner class: has access to all outer members, including private ones.
    class UserSession {
        private final String user;

        UserSession(String user) {
            this.user = user;
        }

        void printSessionInfo() {
            System.out.println("UserSession -> user=" + user + ", app=" + appName);
            System.out.println("Outer this -> " + InnerClassesDemo.this);
            System.out.println("Inner this -> " + this);
        }

        @Override
        public String toString() {
            return "UserSession{user='" + user + "'}";
        }
    }

    // 2) Static nested class: grouped logically with outer class but no outer instance reference.
    static class AppLogger {
        void logStartup() {
            System.out.println("AppLogger -> starting version " + APP_VERSION);
        }

        // Can only access outer static members directly.
        static String loggerType() {
            return "STATIC_NESTED_LOGGER";
        }
    }

    // 3) Local inner class: class declared inside a method.
    void processRequest(String endpoint) {
        int retries = 2; // effectively final

        class RequestTracker {
            void track() {
                System.out.println("Tracking endpoint " + endpoint + " with retries=" + retries);
            }
        }

        new RequestTracker().track();
    }

    // 4) Anonymous inner class: one-off implementation.
    void runWithCallback() {
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                System.out.println("Anonymous callback for app " + appName);
            }
        };

        callback.run();
    }

    public static void main(String[] args) {
        System.out.println("=== Member Inner Class ===");
        InnerClassesDemo demo = new InnerClassesDemo();
        InnerClassesDemo.UserSession session = demo.new UserSession("alice");
        session.printSessionInfo();

        System.out.println("\n=== Static Nested Class ===");
        AppLogger logger = new AppLogger();
        logger.logStartup();
        System.out.println("Logger type: " + AppLogger.loggerType());

        System.out.println("\n=== Local Inner Class ===");
        demo.processRequest("/api/users");

        System.out.println("\n=== Anonymous Inner Class ===");
        demo.runWithCallback();
    }

    @Override
    public String toString() {
        return "InnerClassesDemo{appName='" + appName + "'}";
    }
}
