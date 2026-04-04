/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code reflection utilities
 */
package com.anthropic.claudecode.utils;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * Reflection utilities.
 */
public final class ReflectionUtils {
    private ReflectionUtils() {}

    /**
     * Get all fields of a class.
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Get all methods of a class.
     */
    public static List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }
        return methods;
    }

    /**
     * Get field value by name.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field == null) {
                throw new NoSuchFieldException(fieldName);
            }
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    /**
     * Set field value by name.
     */
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field == null) {
                throw new NoSuchFieldException(fieldName);
            }
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value: " + fieldName, e);
        }
    }

    /**
     * Find field in class hierarchy.
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Invoke method by name.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object obj, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);

            Method method = findMethod(obj.getClass(), methodName, paramTypes);
            if (method == null) {
                throw new NoSuchMethodException(methodName);
            }
            method.setAccessible(true);
            return (T) method.invoke(obj, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + methodName, e);
        }
    }

    /**
     * Find method in class hierarchy.
     */
    private static Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) &&
                    parametersMatch(method.getParameterTypes(), paramTypes)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * Check if parameter types match.
     */
    private static boolean parametersMatch(Class<?>[] declared, Class<?>[] provided) {
        if (declared.length != provided.length) return false;
        for (int i = 0; i < declared.length; i++) {
            if (!declared[i].isAssignableFrom(provided[i]) &&
                !isPrimitiveMatch(declared[i], provided[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check primitive type match.
     */
    private static boolean isPrimitiveMatch(Class<?> declared, Class<?> provided) {
        if (declared.isPrimitive()) {
            if (declared == int.class) return provided == Integer.class;
            if (declared == long.class) return provided == Long.class;
            if (declared == double.class) return provided == Double.class;
            if (declared == float.class) return provided == Float.class;
            if (declared == boolean.class) return provided == Boolean.class;
            if (declared == byte.class) return provided == Byte.class;
            if (declared == short.class) return provided == Short.class;
            if (declared == char.class) return provided == Character.class;
        }
        return false;
    }

    /**
     * Create new instance.
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    /**
     * Create new instance with arguments.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz, Object... args) {
        try {
            Class<?>[] paramTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);

            Constructor<?> constructor = findConstructor(clazz, paramTypes);
            if (constructor == null) {
                throw new NoSuchMethodException("No matching constructor found");
            }
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    /**
     * Find constructor.
     */
    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] paramTypes) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (parametersMatch(constructor.getParameterTypes(), paramTypes)) {
                return constructor;
            }
        }
        return null;
    }

    /**
     * Get class for name.
     */
    public static Class<?> forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    /**
     * Get class for name with default.
     */
    public static Class<?> forName(String className, Class<?> defaultClass) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return defaultClass;
        }
    }

    /**
     * Check if class exists.
     */
    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get simple name of class.
     */
    public static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    /**
     * Get package name of class.
     */
    public static String getPackageName(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        return pkg != null ? pkg.getName() : "";
    }

    /**
     * Get all implemented interfaces.
     */
    public static List<Class<?>> getAllInterfaces(Class<?> clazz) {
        List<Class<?>> interfaces = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Class<?> iface : current.getInterfaces()) {
                if (!interfaces.contains(iface)) {
                    interfaces.add(iface);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

    /**
     * Check if class implements interface.
     */
    public static boolean implementsInterface(Class<?> clazz, Class<?> iface) {
        return iface.isAssignableFrom(clazz);
    }

    /**
     * Check if class is primitive wrapper.
     */
    public static boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class ||
               clazz == Double.class || clazz == Float.class ||
               clazz == Boolean.class || clazz == Byte.class ||
               clazz == Short.class || clazz == Character.class;
    }

    /**
     * Get primitive type for wrapper.
     */
    public static Class<?> getPrimitiveType(Class<?> wrapper) {
        if (wrapper == Integer.class) return int.class;
        if (wrapper == Long.class) return long.class;
        if (wrapper == Double.class) return double.class;
        if (wrapper == Float.class) return float.class;
        if (wrapper == Boolean.class) return boolean.class;
        if (wrapper == Byte.class) return byte.class;
        if (wrapper == Short.class) return short.class;
        if (wrapper == Character.class) return char.class;
        return null;
    }

    /**
     * Get wrapper type for primitive.
     */
    public static Class<?> getWrapperType(Class<?> primitive) {
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == double.class) return Double.class;
        if (primitive == float.class) return Float.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == char.class) return Character.class;
        return null;
    }

    /**
     * Get enum constants.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> List<T> getEnumConstants(Class<?> enumClass) {
        if (enumClass.isEnum()) {
            return Arrays.asList((T[]) enumClass.getEnumConstants());
        }
        return List.of();
    }

    /**
     * Get enum constant by name.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> Optional<T> getEnumConstant(Class<?> enumClass, String name) {
        if (!enumClass.isEnum()) return Optional.empty();
        try {
            return Optional.of(Enum.valueOf((Class<T>) enumClass, name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Get annotation.
     */
    public static <A extends java.lang.annotation.Annotation> Optional<A> getAnnotation(Class<?> clazz, Class<A> annotationClass) {
        return Optional.ofNullable(clazz.getAnnotation(annotationClass));
    }

    /**
     * Get annotation from method.
     */
    public static <A extends java.lang.annotation.Annotation> Optional<A> getAnnotation(Method method, Class<A> annotationClass) {
        return Optional.ofNullable(method.getAnnotation(annotationClass));
    }

    /**
     * Get annotation from field.
     */
    public static <A extends java.lang.annotation.Annotation> Optional<A> getAnnotation(Field field, Class<A> annotationClass) {
        return Optional.ofNullable(field.getAnnotation(annotationClass));
    }

    /**
     * Get methods with annotation.
     */
    public static <A extends java.lang.annotation.Annotation> List<Method> getMethodsWithAnnotation(Class<?> clazz, Class<A> annotationClass) {
        return getAllMethods(clazz).stream()
            .filter(m -> m.isAnnotationPresent(annotationClass))
            .toList();
    }

    /**
     * Get fields with annotation.
     */
    public static <A extends java.lang.annotation.Annotation> List<Field> getFieldsWithAnnotation(Class<?> clazz, Class<A> annotationClass) {
        return getAllFields(clazz).stream()
            .filter(f -> f.isAnnotationPresent(annotationClass))
            .toList();
    }

    /**
     * Copy properties from source to target.
     */
    public static void copyProperties(Object source, Object target) {
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        for (Field sourceField : getAllFields(sourceClass)) {
            Field targetField = findField(targetClass, sourceField.getName());
            if (targetField != null && targetField.getType().isAssignableFrom(sourceField.getType())) {
                try {
                    sourceField.setAccessible(true);
                    targetField.setAccessible(true);
                    targetField.set(target, sourceField.get(source));
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Create a proxy instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            handler
        );
    }

    /**
     * Get type arguments of generic interface.
     */
    @SuppressWarnings("unchecked")
    public static List<Class<?>> getTypeArguments(Class<?> clazz, Class<?> genericInterface) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt) {
                if (pt.getRawType() == genericInterface) {
                    return Arrays.stream(pt.getActualTypeArguments())
                        .filter(t -> t instanceof Class)
                        .map(t -> (Class<?>) (Class<?>) t)
                        .collect(java.util.stream.Collectors.toList());
                }
            }
        }
        return List.of();
    }

    /**
     * Check if object is instance of class by name.
     */
    public static boolean isInstanceOf(Object obj, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}