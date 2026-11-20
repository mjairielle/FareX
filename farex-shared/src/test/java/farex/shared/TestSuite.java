package farex.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestSuite {
    public static void main(String[] args) {
        Class<?>[] testClasses = {
                farex.shared.data.StoreTest.class,
                farex.shared.data.OperatorTest.class,
                farex.shared.plan.TripPlannerTest.class,
                farex.shared.plan.TripPlannerNetworkTest.class,
                farex.shared.reward.RewardsTest.class,
                farex.shared.fare.FareRuleTest.class,
                farex.shared.fare.RoadKmTest.class,
                farex.shared.txn.TxnTest.class,
                farex.shared.card.CardPayloadTest.class,
                farex.shared.user.UserTest.class
        };

        int total = 0;
        int passed = 0;
        List<String> failures = new ArrayList<>();

        System.out.println("==================================================");
        System.out.println("            FAREX AUTOMATED TEST SUITE            ");
        System.out.println("==================================================");

        for (Class<?> cls : testClasses) {
            System.out.println("Running " + cls.getSimpleName() + "...");
            for (Method m : cls.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Test.class)) {
                    total++;
                    Path tempDir = null;
                    try {
                        Object instance = cls.getDeclaredConstructor().newInstance();
                        tempDir = Files.createTempDirectory("farex-test-");

                        // Inject @TempDir if any
                        for (Field f : cls.getDeclaredFields()) {
                            if (f.isAnnotationPresent(org.junit.jupiter.api.io.TempDir.class)) {
                                f.setAccessible(true);
                                f.set(instance, tempDir);
                            }
                        }

                        // Run @BeforeEach
                        for (Method bm : cls.getDeclaredMethods()) {
                            if (bm.isAnnotationPresent(BeforeEach.class)) {
                                bm.setAccessible(true);
                                bm.invoke(instance);
                            }
                        }

                        m.setAccessible(true);
                        m.invoke(instance);
                        passed++;
                        System.out.println("  PASS: " + m.getName());
                    } catch (Throwable t) {
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        String err = cls.getSimpleName() + "." + m.getName() + " -> " + cause.getMessage();
                        failures.add(err);
                        System.err.println("  FAIL: " + m.getName() + ": " + cause.getMessage());
                    }
                }
            }
        }

        System.out.println("==================================================");
        System.out.printf("Results: %d/%d tests passed.%n", passed, total);
        System.out.println("==================================================");

        if (!failures.isEmpty()) {
            System.err.println("Failures:");
            for (String f : failures) System.err.println("  - " + f);
            System.exit(1);
        }
    }
}
