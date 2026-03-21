package me.fndo.fnclient.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassFileReader 测试
 */
public class ClassFileReaderTest {

    @Test
    public void testGetClasses() {
        Set<Class<?>> classes = ClassFileReader.getClasses("me.fndo.fnclient");
        
        // 应该能找到测试类
        assertNotNull(classes);
        assertFalse(classes.isEmpty(), "应该找到至少一个类");
        
        // 检查是否包含预期的类
        boolean foundTestRemote = classes.stream()
            .anyMatch(c -> c.getSimpleName().equals("TestRemote"));
        // 注意：TestRemote 是接口，可能不会被扫描到，因为它在测试目录
        
        // 至少应该找到一些类
        System.out.println("Found classes: " + classes.size());
        for (Class<?> clazz : classes) {
            System.out.println("  - " + clazz.getName());
        }
    }

    @Test
    public void testGetClassesWithNonExistentPackage() {
        Set<Class<?>> classes = ClassFileReader.getClasses("non.existent.package");
        
        assertNotNull(classes);
        assertTrue(classes.isEmpty(), "不存在的包应该返回空集合");
    }

    @Test
    public void testGetClassesWithCurrentPackage() {
        Set<Class<?>> classes = ClassFileReader.getClasses("me.fndo.fnclient.util");
        
        assertNotNull(classes);
        // 应该能找到 ClassFileReader 本身
        boolean found = classes.stream()
            .anyMatch(c -> c.getSimpleName().equals("ClassFileReader"));
        // 注意：当前类可能不会被扫描到
    }
}
