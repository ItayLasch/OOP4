package OOP.Solution;

import java.lang.reflect.*;
import java.util.*;

import javax.lang.model.util.ElementScanner14;

import OOP.Provided.*;
import OOP.Provided.OOPResult.OOPTestResult;
import OOP.Solution.OOPTestClass.OOPTestClassType;

public class OOPUnitCore {
    static private void invokeTestMethod(Map<String, OOPResult> results, List<Method> Methods, Object obj, OOPExpectedException expectedExeption)
    {
        for (Method method : Methods) {
            method.setAccessible(true);
            try {
                method.invoke(obj);
                results.put(method.getName(), new OOPResultImpl(OOPTestResult.SUCCESS, null));
            } catch (OOPAssertionFailure e) {
                results.put(method.getName(), new OOPResultImpl(OOPTestResult.FAILURE, e.getMessage()));
            } catch (Exception e) {
                if (expectedExeption == null) {
                    results.put(method.getName(), new OOPResultImpl(OOPTestResult.ERROR, e.getClass().getName()));
                } else if (expectedExeption.assertExpected(e)) {
                    results.put(method.getName(), new OOPResultImpl(OOPTestResult.SUCCESS, null));
                } else {
                    OOPExceptionMismatchError error = new OOPExceptionMismatchError(
                            expectedExeption.getExpectedException(), e.getClass());
                    results.put(method.getName(),
                            new OOPResultImpl(OOPTestResult.EXPECTED_EXCEPTION_MISMATCH, error.getMessage()));
                }
            }
        }
    }

    static private void invokeBeforeAfterMethod(Map<String, OOPResult> results, List<Method> Methods, Object obj) {
        for (Method method : Methods) {
            method.setAccessible(true);
            try {
                method.invoke(obj);
                results.put(method.getName(), new OOPResultImpl(OOPTestResult.SUCCESS, null));
            } catch (Exception e) {
                results.put(method.getName(), new OOPResultImpl(OOPTestResult.ERROR, e.getClass().getName()));
            }
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }

        throw new OOPAssertionFailure();
    }

    static public void fail() {
        throw new OOPAssertionFailure();
    }

    static public OOPTestSummary runClass(Class<?> testClass)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        if (testClass == null || testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        // Gather all setUp functions from all the Classes in tree
        Object obj = testClass.getConstructor().newInstance();

        // Get OOPExceptionRule
        OOPExpectedException expectedExeption = null;
        for (Field F : testClass.getDeclaredFields()) {
            if (F.isAnnotationPresent(OOPExceptionRule.class)) {
                expectedExeption = (OOPExpectedException) F.get(obj);
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

        Map<String, OOPResult> results = new HashMap<>();
        List<Method> methSetup = allMethods.stream().filter(method -> method.isAnnotationPresent(OOPSetup.class))
                .sorted(Collections.reverseOrder()).toList();

        List<Method> methTest = allMethods.stream().filter(method -> method.isAnnotationPresent(OOPTest.class))
                .toList();

        if (testClass.getAnnotation(OOPTestClass.class).value() == OOPTestClassType.ORDERED) {
            methTest = methTest.stream().sorted((Method m1, Method m2) -> m1.getAnnotation(OOPTest.class).order()
                    - m2.getAnnotation(OOPTest.class).order()).toList();
        }
       
        for (Method method : methTest) {
            List<Method> before = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPBefore.class) &&
                    Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(method.getName())))
                    .sorted(Collections.reverseOrder()).toList();
            List<Method> after = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPAfter.class) &&
                    Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(method.getName()))).toList();

            invokeBeforeAfterMethod(results, before, obj);
            invokeTestMethod(results, methTest, obj,expectedExeption);
            invokeBeforeAfterMethod(results, after, obj);
        }

        return new OOPTestSummary(results);
    }

    static public OOPTestSummary runClass(Class<?> testClass, String tag)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        if (testClass == null || testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        return new OOPTestSummary(new HashMap<>());
    }
}
