package OOP.Solution;

import java.lang.reflect.*;
import java.util.*;
import OOP.Provided.*;
import OOP.Provided.OOPResult.OOPTestResult;
import OOP.Solution.OOPTestClass.OOPTestClassType;

public class OOPUnitCore {
    private static void backupObj(Object obj, Object other) {
        Arrays.stream(obj.getClass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            Object fieldObject;
            Class<?> fieldClass;
            try {
                fieldObject = field.get(obj);
                fieldClass = fieldObject.getClass();
                if (Modifier.isFinal(field.getModifiers()))
                {
                    return;
                }
                if (fieldObject instanceof Cloneable) {
                    Method FieldClone = fieldClass.getDeclaredMethod("clone");
                    FieldClone.setAccessible(true);
                    field.set(other, FieldClone.invoke(fieldObject));
                } else if (Arrays.stream(fieldClass.getConstructors())
                        .anyMatch(constructor -> (constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0] == fieldClass))) {
                    Constructor<?> cons = fieldClass.getConstructor(fieldClass);
                    field.set(other, cons.newInstance(fieldObject));
                } else {
                    field.set(other, field.get(obj));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void resetExpectedException(Class<?> testClass, Object obj) {
        // Get OOPExceptionRule
        for (Field F : testClass.getDeclaredFields()) {
            if (F.isAnnotationPresent(OOPExceptionRule.class)) {
                F.setAccessible(true);
                try {
                    F.set(obj, OOPExpectedExceptionImpl.none());
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static OOPExpectedException getExpectedException(Class<?> testClass, Object obj) {
        // Get OOPExceptionRule
        OOPExpectedException expectedException = OOPExpectedExceptionImpl.none();
        for (Field F : testClass.getDeclaredFields()) {
            if (F.isAnnotationPresent(OOPExceptionRule.class)) {
                F.setAccessible(true);
                try {
                    expectedException = (OOPExpectedException) F.get(obj);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return expectedException;
    }

    private static OOPTestResult invokeTestMethod(Class<?> testClass, Method method, Object obj, String[] msg) {
        method.setAccessible(true);
        try {
            resetExpectedException(testClass, obj);
            method.invoke(obj);
            OOPExpectedException expectedException = getExpectedException(testClass, obj);
            if (expectedException.getExpectedException() == null) {
                return OOPTestResult.SUCCESS;
            }
            msg[0] = expectedException.getExpectedException().getName();
            return OOPTestResult.ERROR;

        } catch (Exception e) {
            if (e.getCause() instanceof OOPAssertionFailure) {
                msg[0] = e.getCause().getMessage();
                return OOPTestResult.FAILURE;
            } else {
                OOPExpectedException expectedException = getExpectedException(testClass, obj);
                if (expectedException.getExpectedException() == null) {
                    msg[0] = e.getCause().getClass().getName();
                    return OOPTestResult.ERROR;
                } else if (expectedException.assertExpected((Exception) e.getCause())) {
                    return OOPTestResult.SUCCESS;
                } else {
                    OOPExceptionMismatchError mismatchError = new OOPExceptionMismatchError(
                            expectedException.getExpectedException(), ((Exception) e.getCause()).getClass());
                    msg[0] = mismatchError.getMessage();
                    return OOPTestResult.EXPECTED_EXCEPTION_MISMATCH;
                }
            }
        }
    }

    private static OOPTestResult invokeMethod(Method method, Object obj, Object backup, String[] msg) {
        method.setAccessible(true);
        try {
            backupObj(obj, backup);
            method.invoke(obj);
        } catch (Exception e) {
            backupObj(backup, obj);
            msg[0] = e.getCause().getClass().getName();
            return OOPTestResult.ERROR;
        }

        return OOPTestResult.SUCCESS;
    }

    private static OOPTestResult invokeBeforeMethod(List<Method> Methods, Object obj, Object backup, String[] msg) {
        ListIterator<?> it = Methods.listIterator(Methods.size());
        while (it.hasPrevious()) {
            Method method = (Method) it.previous();
            if (invokeMethod(method, obj, backup, msg) == OOPTestResult.ERROR) {
                return OOPTestResult.ERROR;
            }
        }
        return OOPTestResult.SUCCESS;
    }

    private static OOPTestResult invokeAfterMethod(List<Method> Methods, Object obj, Object backup, String[] msg) {
        for (Method method : Methods) {
            if (invokeMethod(method, obj, backup, msg) == OOPTestResult.ERROR) {
                return OOPTestResult.ERROR;
            }
        }
        return OOPTestResult.SUCCESS;
    }

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }

        throw new OOPAssertionFailure(expected, actual);
    }

    static public void fail() {
        throw new OOPAssertionFailure();
    }

    static public OOPTestSummary runClass(Class<?> testClass) {

        return runClass(testClass, "");
    }

    static public OOPTestSummary runClass(Class<?> testClass, String tag) {
        if (testClass == null || tag == null || !testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        Object obj, backup;
        try {
            Constructor<?> constructor = testClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            obj = constructor.newInstance();
            backup = constructor.newInstance();
        } catch (Exception e) {
            return null;
        }

        List<String> allMethodsNames = new LinkedList<>();
        List<Method> allMethods = new LinkedList<>();

        // Gather all methods in inheritance tree
        Arrays.stream(testClass.getDeclaredMethods()).forEach(method -> {
            allMethodsNames.add(method.getName());
            allMethods.add(method);
        });

        for (Class<?> c = testClass.getSuperclass(); c != null; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredMethods())
                    .forEach(method -> {
                        if (!allMethodsNames.contains(method.getName()) && !Modifier.isPrivate(method.getModifiers())) {
                            allMethods.add(method);
                            allMethodsNames.add(method.getName());
                        }
                    });
        }

        // Create map for results of test functions
        Map<String, OOPResult> results = new HashMap<>();

        // Gather all setUp functions from all the classes in inheritance tree
        List<Method> methSetup = allMethods.stream().filter(method -> method.isAnnotationPresent(OOPSetup.class))
                .toList();
        ListIterator<?> it = methSetup.listIterator(methSetup.size());
        while (it.hasPrevious()) {
            Method method = (Method) it.previous();
            method.setAccessible(true);
            try {
                method.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Gather all Test functions from all the classes in inheritance tree
        List<Method> methTest = allMethods.stream()
                .filter(method -> (method.isAnnotationPresent(OOPTest.class)
                        && method.getAnnotation(OOPTest.class).tag().contains(tag)))
                .toList();

        if (testClass.getAnnotation(OOPTestClass.class).value() == OOPTestClassType.ORDERED) {
            methTest = methTest.stream()
                    .sorted(Comparator.comparingInt((Method m) -> m.getAnnotation(OOPTest.class).order())).toList();
        }

        String[] message = new String[1];
        OOPTestResult result;
        for (Method method : methTest) {
            List<Method> before = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPBefore.class) &&
                    Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(method.getName()))).toList();
            List<Method> after = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPAfter.class) &&
                    Arrays.asList(m.getAnnotation(OOPAfter.class).value()).contains(method.getName()))).toList();

            result = invokeBeforeMethod(before, obj, backup, message);
            if (result != OOPTestResult.SUCCESS) {
                results.put(method.getName(), new OOPResultImpl(result, message[0]));
                continue;
            }

            message[0] = null;
            result = invokeTestMethod(testClass, method, obj, message);
            results.put(method.getName(), new OOPResultImpl(result, message[0]));

            result = invokeAfterMethod(after, obj, backup, message);
            if (result != OOPTestResult.SUCCESS) {
                results.put(method.getName(), new OOPResultImpl(result, message[0]));
                continue;
            }
        }

        return new OOPTestSummary(results);
    }
}
