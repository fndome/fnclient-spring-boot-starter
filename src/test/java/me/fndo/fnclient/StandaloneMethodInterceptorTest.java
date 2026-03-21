package me.fndo.fnclient;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StandaloneMethodInterceptor 测试
 */
public class StandaloneMethodInterceptorTest {

    @Test
    public void testInvoke() throws Throwable {
        // 创建模拟 Controller
        TestController controller = new TestController();

        // 创建方法映射
        Map<Method, Object> methodControllerMap = new HashMap<>();
        Method getUserMethod = TestRemote.class.getMethod("getUser", Long.class);
        methodControllerMap.put(getUserMethod, controller);

        // 创建拦截器
        StandaloneMethodInterceptor interceptor = new StandaloneMethodInterceptor(
            TestRemote.class, 
            methodControllerMap
        );

        // 创建代理
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(TestRemote.class);
        proxyFactory.addAdvice(interceptor);
        TestRemote remote = (TestRemote) proxyFactory.getProxy();

        // 测试调用
        TestUser user = remote.getUser(1L);
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("TestUser-1", user.getName());
    }

    @Test
    public void testInvokeHealth() throws Throwable {
        TestController controller = new TestController();

        Map<Method, Object> methodControllerMap = new HashMap<>();
        Method healthMethod = TestRemote.class.getMethod("health");
        methodControllerMap.put(healthMethod, controller);

        StandaloneMethodInterceptor interceptor = new StandaloneMethodInterceptor(
            TestRemote.class, 
            methodControllerMap
        );

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(TestRemote.class);
        proxyFactory.addAdvice(interceptor);
        TestRemote remote = (TestRemote) proxyFactory.getProxy();

        String result = remote.health();
        assertEquals("OK", result);
    }

    @Test
    public void testInvokeNotFound() {
        // 空映射，应该抛出异常
        StandaloneMethodInterceptor interceptor = new StandaloneMethodInterceptor(
            TestRemote.class, 
            new HashMap<>()
        );

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(TestRemote.class);
        proxyFactory.addAdvice(interceptor);
        TestRemote remote = (TestRemote) proxyFactory.getProxy();

        // 调用未映射的方法应该抛出 IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            remote.getUser(1L);
        });
    }
}
