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


        //Get OOPExceptionRule
        Object expectedExeption = null;
        for(Field F : testClass.getDeclaredFields()){
            if(F.isAnnotationPresent(OOPExceptionRule.class)) {
                expectedExeption = F.get(obj);
            }
        }

        List<String> allMethodsNames = new LinkedList<>();
        List<Method> allMethods = new LinkedList<>();
        
        for (Class<?> c = testClass; c != null; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredMethods())
                    .forEach(method -> {
                        if (!allMethodsNames.contains(method.getName()) || Modifier.isStatic(method.getModifiers())) {
                            allMethods.add(method);
                            allMethodsNames.add(method.getName());
                        }
                    });
        }

        List<Method> methSetup = allMethods.stream().filter(method -> method.isAnnotationPresent(OOPSetup.class)).toList();
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
