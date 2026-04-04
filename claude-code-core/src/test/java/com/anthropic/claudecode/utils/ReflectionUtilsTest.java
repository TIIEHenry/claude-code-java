/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReflectionUtils.
 */
class ReflectionUtilsTest {

    static class TestClass {
        private String privateField;
        public int publicField;

        private String privateMethod() {
            return "private";
        }

        public String publicMethod() {
            return "public";
        }

        public String methodWithArgs(String a, int b) {
            return a + b;
        }
    }

    static class ChildClass extends TestClass {
        private double childField;
    }

    @Test
    @DisplayName("ReflectionUtils getAllFields returns all fields")
    void getAllFields() {
        List<Field> fields = ReflectionUtils.getAllFields(TestClass.class);

        assertTrue(fields.size() >= 2);
    }

    @Test
    @DisplayName("ReflectionUtils getAllFields includes inherited")
    void getAllFieldsInherited() {
        List<Field> fields = ReflectionUtils.getAllFields(ChildClass.class);

        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("childField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateField")));
    }

    @Test
    @DisplayName("ReflectionUtils getAllMethods returns all methods")
    void getAllMethods() {
        List<Method> methods = ReflectionUtils.getAllMethods(TestClass.class);

        assertTrue(methods.stream().anyMatch(m -> m.getName().equals("privateMethod")));
        assertTrue(methods.stream().anyMatch(m -> m.getName().equals("publicMethod")));
    }

    @Test
    @DisplayName("ReflectionUtils getFieldValue gets private field")
    void getFieldValue() {
        TestClass obj = new TestClass();
        obj.privateField = "test";

        String value = ReflectionUtils.getFieldValue(obj, "privateField");
        assertEquals("test", value);
    }

    @Test
    @DisplayName("ReflectionUtils setFieldValue sets private field")
    void setFieldValue() {
        TestClass obj = new TestClass();

        ReflectionUtils.setFieldValue(obj, "privateField", "new value");

        assertEquals("new value", obj.privateField);
    }

    @Test
    @DisplayName("ReflectionUtils invokeMethod invokes private method")
    void invokeMethod() {
        TestClass obj = new TestClass();

        String result = ReflectionUtils.invokeMethod(obj, "privateMethod");
        assertEquals("private", result);
    }

    @Test
    @DisplayName("ReflectionUtils invokeMethod with args")
    void invokeMethodWithArgs() {
        TestClass obj = new TestClass();

        String result = ReflectionUtils.invokeMethod(obj, "methodWithArgs", "value", 42);
        assertEquals("value42", result);
    }

    @Test
    @DisplayName("ReflectionUtils newInstance creates instance")
    void newInstance() {
        TestClass obj = ReflectionUtils.newInstance(TestClass.class);

        assertNotNull(obj);
        assertTrue(obj instanceof TestClass);
    }

    @Test
    @DisplayName("ReflectionUtils forName loads class")
    void forName() {
        Class<?> clazz = ReflectionUtils.forName("java.lang.String");

        assertEquals(String.class, clazz);
    }

    @Test
    @DisplayName("ReflectionUtils forName with default")
    void forNameWithDefault() {
        Class<?> clazz = ReflectionUtils.forName("nonexistent.Class", String.class);

        assertEquals(String.class, clazz);
    }

    @Test
    @DisplayName("ReflectionUtils classExists true for existing")
    void classExistsTrue() {
        assertTrue(ReflectionUtils.classExists("java.lang.String"));
    }

    @Test
    @DisplayName("ReflectionUtils classExists false for missing")
    void classExistsFalse() {
        assertFalse(ReflectionUtils.classExists("nonexistent.Class"));
    }

    @Test
    @DisplayName("ReflectionUtils getSimpleName")
    void getSimpleName() {
        String name = ReflectionUtils.getSimpleName(TestClass.class);
        assertEquals("TestClass", name);
    }

    @Test
    @DisplayName("ReflectionUtils getPackageName")
    void getPackageName() {
        String pkg = ReflectionUtils.getPackageName(ReflectionUtilsTest.class);
        assertTrue(pkg.contains("claudecode"));
    }

    @Test
    @DisplayName("ReflectionUtils getAllInterfaces")
    void getAllInterfaces() {
        List<Class<?>> interfaces = ReflectionUtils.getAllInterfaces(String.class);

        assertTrue(interfaces.contains(Comparable.class));
        assertTrue(interfaces.contains(CharSequence.class));
    }

    @Test
    @DisplayName("ReflectionUtils implementsInterface true")
    void implementsInterfaceTrue() {
        assertTrue(ReflectionUtils.implementsInterface(String.class, CharSequence.class));
    }

    @Test
    @DisplayName("ReflectionUtils implementsInterface false")
    void implementsInterfaceFalse() {
        assertFalse(ReflectionUtils.implementsInterface(String.class, List.class));
    }

    @Test
    @DisplayName("ReflectionUtils isWrapperType true")
    void isWrapperTypeTrue() {
        assertTrue(ReflectionUtils.isWrapperType(Integer.class));
        assertTrue(ReflectionUtils.isWrapperType(Boolean.class));
        assertTrue(ReflectionUtils.isWrapperType(Double.class));
    }

    @Test
    @DisplayName("ReflectionUtils isWrapperType false")
    void isWrapperTypeFalse() {
        assertFalse(ReflectionUtils.isWrapperType(String.class));
    }

    @Test
    @DisplayName("ReflectionUtils getPrimitiveType")
    void getPrimitiveType() {
        assertEquals(int.class, ReflectionUtils.getPrimitiveType(Integer.class));
        assertEquals(boolean.class, ReflectionUtils.getPrimitiveType(Boolean.class));
    }

    @Test
    @DisplayName("ReflectionUtils getWrapperType")
    void getWrapperType() {
        assertEquals(Integer.class, ReflectionUtils.getWrapperType(int.class));
        assertEquals(Boolean.class, ReflectionUtils.getWrapperType(boolean.class));
    }

    @Test
    @DisplayName("ReflectionUtils getEnumConstants")
    void getEnumConstants() {
        List<TestEnum> constants = ReflectionUtils.getEnumConstants(TestEnum.class);

        assertEquals(2, constants.size());
        assertTrue(constants.contains(TestEnum.A));
        assertTrue(constants.contains(TestEnum.B));
    }

    @Test
    @DisplayName("ReflectionUtils getEnumConstant by name")
    void getEnumConstant() {
        Optional<TestEnum> result = ReflectionUtils.getEnumConstant(TestEnum.class, "A");

        assertTrue(result.isPresent());
        assertEquals(TestEnum.A, result.get());
    }

    @Test
    @DisplayName("ReflectionUtils getEnumConstant missing")
    void getEnumConstantMissing() {
        Optional<TestEnum> result = ReflectionUtils.getEnumConstant(TestEnum.class, "C");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("ReflectionUtils isInstanceOf true")
    void isInstanceOfTrue() {
        assertTrue(ReflectionUtils.isInstanceOf("test", "java.lang.String"));
    }

    @Test
    @DisplayName("ReflectionUtils isInstanceOf false")
    void isInstanceOfFalse() {
        assertFalse(ReflectionUtils.isInstanceOf("test", "java.lang.Integer"));
    }

    enum TestEnum { A, B }
}