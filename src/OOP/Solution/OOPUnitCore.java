package OOP.Solution;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import OOP.Provided.*;
import OOP.Provided.OOPResult.OOPTestResult;
import OOP.Solution.OOPTestClass.OOPTestClassType;

public class OOPUnitCore {
    private static Map<Field, Object> backupObj(Object obj) {
        Map<Field, Object> backup_dict = new HashMap<>();
        Arrays.stream(obj.getClass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            Object fieldObject;
            Class<?> fieldClass;
            try {
                fieldObject = field.get(obj);
                fieldClass = fieldObject.getClass();
                if (Modifier.isFinal(field.getModifiers())) {
                    return;
                }
                if (fieldObject instanceof Cloneable) {
                    try {
                        Method FieldClone = fieldClass.getDeclaredMethod("clone");
                        FieldClone.setAccessible(true);
                        backup_dict.put(field, FieldClone.invoke(fieldObject));
                    } catch (NoSuchMethodException e) {
                        Method FieldClone = fieldClass.getMethod("clone");
                        FieldClone.setAccessible(true);
                        backup_dict.put(field, FieldClone.invoke(fieldObject));
                    }
                } else if (Arrays.stream(fieldClass.getConstructors())
                        .anyMatch(constructor -> (constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0] == fieldClass))) {
                    try {
                        Constructor<?> cons = fieldClass.getDeclaredConstructor(fieldClass);
                        cons.setAccessible(true);
                        backup_dict.put(field, cons.newInstance(fieldObject));
                    } catch (NoSuchMethodException e) {
                        Constructor<?> cons = fieldClass.getConstructor(fieldClass);
                        cons.setAccessible(true);
                        backup_dict.put(field, cons.newInstance(fieldObject));
                    }

                } else {
                    backup_dict.put(field, field.get(obj));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return backup_dict;
    }

    private static void restoreObject(Object obj, Map<Field, Object> backup_dict) {
        backup_dict.forEach((f, o) -> {
            try {
                f.set(obj, o);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
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

    private static OOPTestResult invokeBeforeAfterMethod(List<Method> Methods, Object obj, String[] msg) {
        for (Method method : Methods) {
            method.setAccessible(true);
            Map<Field, Object> backup_dict = backupObj(obj);
            try {
                method.invoke(obj);
            } catch (Exception e) {
                restoreObject(obj, backup_dict);
                msg[0] = e.getCause().getClass().getName();
                return OOPTestResult.ERROR;
            }
        }

        return OOPTestResult.SUCCESS;
    }

    public static void assertEquals(Object expected, Object actual) throws OOPAssertionFailure {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }

        throw new OOPAssertionFailure(expected, actual);
    }

    static public void fail() throws OOPAssertionFailure {
        throw new OOPAssertionFailure();
    }

    static public OOPTestSummary runClass(Class<?> testClass) throws IllegalArgumentException {

        return runClass(testClass, null);
    }

    static public OOPTestSummary runClass(Class<?> testClass, String tag) throws IllegalArgumentException {
        if (testClass == null || !testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        // Create Object and backup using a Constructor
        Object obj;
        try {
            Constructor<?> constructor = testClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            obj = constructor.newInstance();
        } catch (Exception e) {
            return null;
        }

        List<String> allMethodsNames = new LinkedList<>();
        List<Method> allMethods = new LinkedList<>();

        for (Class<?> c = testClass; c != null; c = c.getSuperclass()) {
            final Class<?> type = c;
            Arrays.stream(c.getDeclaredMethods())
                    .forEach(method -> {
                        if (!allMethodsNames.contains(method.getName()) &&
                                ((type == testClass) || !Modifier.isPrivate(method.getModifiers()))) {
                            allMethods.add(method);
                            allMethodsNames.add(method.getName());
                        }
                    });
        }

        // Create map for results of test functions
        Map<String, OOPResult> results = new HashMap<>();

        // Gather all setUp functions from all the classes in inheritance tree
        List<Method> methSetup = allMethods.stream().filter(method -> method.isAnnotationPresent(OOPSetup.class))
                .collect(Collectors.toList());
        Collections.reverse(methSetup);

        methSetup.forEach(method -> {
            method.setAccessible(true);
            try {
                method.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Gather all Test functions from all the classes in inheritance tree
        List<Method> methTest_t = allMethods.stream()
                .filter(method -> (method.isAnnotationPresent(OOPTest.class)))
                .collect(Collectors.toList());
        List<Method> methTest = methTest_t;
        if (tag != null) {
            methTest = methTest_t.stream().filter(method -> method.getAnnotation(OOPTest.class).tag().equals(tag))
                    .collect(Collectors.toList());
        }
        if (testClass.getAnnotation(OOPTestClass.class).value() == OOPTestClassType.ORDERED) {
            methTest = methTest.stream()
                    .sorted(Comparator.comparingInt((Method m) -> m.getAnnotation(OOPTest.class).order())).toList();
        }

        String[] message = new String[1];
        OOPTestResult result;
        for (Method method : methTest) {
            List<Method> before = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPBefore.class) &&
                    Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(method.getName())))
                    .collect(Collectors.toList());
            Collections.reverse(before);
            List<Method> after = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPAfter.class) &&
                    Arrays.asList(m.getAnnotation(OOPAfter.class).value()).contains(method.getName()))).toList();

            result = invokeBeforeAfterMethod(before, obj, message);
            if (result != OOPTestResult.SUCCESS) {
                results.put(method.getName(), new OOPResultImpl(result, message[0]));
                continue;
            }

            message[0] = null;
            result = invokeTestMethod(testClass, method, obj, message);
            results.put(method.getName(), new OOPResultImpl(result, message[0]));

            result = invokeBeforeAfterMethod(after, obj, message);
            if (result != OOPTestResult.SUCCESS) {
                results.put(method.getName(), new OOPResultImpl(result, message[0]));
            }
        }

        return new OOPTestSummary(results);
    }
}
