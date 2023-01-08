package OOP.Solution;

import java.lang.reflect.*;
import java.util.*;
import OOP.Provided.*;
import OOP.Provided.OOPResult.OOPTestResult;
import OOP.Solution.OOPTestClass.OOPTestClassType;

public class OOPUnitCore {

    static private void backupObj(Object obj, Object other) throws Exception
    {
        Arrays.stream(obj.getClass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            Object fieldObject;
            Class<?> fieldClass;
            try {
                fieldObject = field.get(obj);
                fieldClass = field.getClass();
                if(fieldObject instanceof Cloneable)
                {
                    Method FieldClone = fieldClass.getDeclaredMethod("clone");
                    FieldClone.setAccessible(true);
                    field.set(other, FieldClone.invoke(fieldObject));
                }
                else if(Arrays.stream(fieldClass.getConstructors()).
                    anyMatch(constructor -> (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0] == fieldClass)))
                {
                    Constructor<?> cons = fieldClass.getConstructor(fieldClass);
                    field.set(other, cons.newInstance(fieldObject));
                }
                else {
                    field.set(other, field.get(obj));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    static private OOPExpectedException getExpectedException(Class<?> testClass, Object obj)
    {
        // Get OOPExceptionRule
        OOPExpectedException expectedExeption = OOPExpectedExceptionImpl.none();
        for (Field F : testClass.getDeclaredFields()) {
            if (F.isAnnotationPresent(OOPExceptionRule.class)) {
                F.setAccessible(true);
                try {
                    expectedExeption = (OOPExpectedException) F.get(obj);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return expectedExeption;
    }

    static private OOPTestResult invokeTestMethod(Class<?> testClass, Method method, Object obj, String msg)
    {
        method.setAccessible(true);
        try {
            method.invoke(obj);
            return OOPTestResult.SUCCESS;
        } catch (Exception e) {
            if (e.getCause() instanceof OOPAssertionFailure) {
                msg = e.getCause().getMessage();
                return OOPTestResult.FAILURE;
            } else {
                OOPExpectedException expectedExeption = getExpectedException(testClass, obj);
                if (expectedExeption.getExpectedException() == null) {
                    msg = e.getClass().getName();
                    return OOPTestResult.ERROR;
                } else if (expectedExeption.assertExpected((Exception) e.getCause())) {
                    return OOPTestResult.SUCCESS;
                } else {
                    OOPExceptionMismatchError mismatchError = new OOPExceptionMismatchError(expectedExeption.getExpectedException(), e.getClass());
                    msg = mismatchError.getMessage();
                    return OOPTestResult.EXPECTED_EXCEPTION_MISMATCH;
                }
            }
        }
    }

    static private OOPTestResult invokeBeforeAfterMethod(List<Method> Methods,Object obj, String msg) {
        for (Method method : Methods) {
            method.setAccessible(true);
            try {
                method.invoke(obj);
            } catch (Exception e) {
                msg = e.getClass().getName();
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

        throw new OOPAssertionFailure();
    }

    static public void fail() {
        throw new OOPAssertionFailure();
    }

    static public OOPTestSummary runClass(Class<?> testClass) throws Exception {
        if (testClass == null || testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        Object obj, backup;
        try{
            obj = testClass.getConstructor().newInstance();
            backup = testClass.getConstructor().newInstance();
        }catch(Exception e)
        {
            return null;
        }

        List<String> allMethodsNames = new LinkedList<>();
        List<Method> allMethods = new LinkedList<>();

        // Gather all methods in inheritance tree
        Arrays.stream(testClass.getDeclaredMethods()).forEach(method ->{
                    allMethodsNames.add(method.getName());
                    allMethods.add(method);
                }
        );
            
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
                .sorted(Collections.reverseOrder()).toList();

        // Invoke all setup methods
        methSetup.forEach(method -> {
            method.setAccessible(true);
            try {
                method.invoke(obj);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        // Gather all Test functions from all the classes in inheritance tree
        List<Method> methTest = allMethods.stream().filter(method -> method.isAnnotationPresent(OOPTest.class))
                .toList();

        if (testClass.getAnnotation(OOPTestClass.class).value() == OOPTestClassType.ORDERED) {
            methTest = methTest.stream().sorted((Method m1, Method m2) -> m1.getAnnotation(OOPTest.class).order()
                    - m2.getAnnotation(OOPTest.class).order()).toList();
        }
        
        String message = "";
        OOPTestResult result;
        for (Method method : methTest) {
            List<Method> before = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPBefore.class) &&
                    Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(method.getName())))
                    .sorted(Collections.reverseOrder()).toList();
            List<Method> after = allMethods.stream().filter(m -> (m.isAnnotationPresent(OOPAfter.class) &&
                    Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(method.getName()))).toList();

            backupObj(obj, backup);
            result = invokeBeforeAfterMethod(before, obj, message);
            if (result != OOPTestResult.SUCCESS)
            {
                backupObj(backup, obj);
                results.put(method.getName(), new OOPResultImpl(result, message));
                continue;
            }
            result = invokeTestMethod(testClass, method, obj, message);
            if (result != OOPTestResult.SUCCESS) {
                results.put(method.getName(), new OOPResultImpl(result, message));
                continue;
            }
            backupObj(obj, backup);
            result = invokeBeforeAfterMethod(after, obj, message);
            if (result != OOPTestResult.SUCCESS) {
                backupObj(backup, obj);
                results.put(method.getName(), new OOPResultImpl(result, message));
                continue;
            }

            results.put(method.getName(), new OOPResultImpl(OOPTestResult.SUCCESS, null));
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
