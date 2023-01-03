package OOP.Solution;

import java.lang.reflect.*;
import java.util.*;
import OOP.Provided.*;

public class OOPUnitCore {
    public static void assertEquals(Object expected, Object actual)
    {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }

        throw new OOPAssertionFailure();
    }

    static public void fail()
    {
        throw new OOPAssertionFailure();
    }

    static public OOPTestSummary runClass(Class<?> testClass)
        throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        if (testClass == null || testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        
        // Gather all setUp functions from all the Classes in tree
        Object obj = testClass.getConstructor().newInstance();
        List<String> methSetupNames = new LinkedList<>();
        List<Method> methSetup = new LinkedList<>();
        
        for (Class<?> c = testClass; c != null; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(OOPSetup.class))
                    .forEach(method -> {
                        if (!methSetupNames.contains(method.getName()) || Modifier.isStatic(method.getModifiers())) {
                            methSetup.add(method);
                            methSetupNames.add(method.getName());
                        }
                    });
        }

        // Invoke each method on the Object
        methSetup.stream().sorted(Collections.reverseOrder()).forEach(method-> {
            method.setAccessible(true);
            try {
                method.invoke(obj);
            } catch (Exception e) {
            }
        }
        );

        return new OOPTestSummary(new HashMap<>());
    }

    static public OOPTestSummary runClass(Class<?> testClass, String tag)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        if (testClass == null || testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        return new OOPTestSummary(new HashMap<>());
    }
}
