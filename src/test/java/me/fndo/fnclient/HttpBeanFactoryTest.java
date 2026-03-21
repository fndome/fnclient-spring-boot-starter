package me.fndo.fnclient;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpBeanFactory 测试
 */
public class HttpBeanFactoryTest {

    @Test
    public void testToBodyString() throws Exception {
        String input = "Hello, World!";
        java.io.InputStream inputStream = new java.io.ByteArrayInputStream(input.getBytes("UTF-8"));
        
        String result = HttpBeanFactory.toBodyString(inputStream);
        
        assertEquals(input, result);
    }

    @Test
    public void testToBodyStringWithChinese() throws Exception {
        String input = "你好，世界！";
        java.io.InputStream inputStream = new java.io.ByteArrayInputStream(input.getBytes("UTF-8"));
        
        String result = HttpBeanFactory.toBodyString(inputStream);
        
        assertEquals(input, result);
    }

    @Test
    public void testToBodyStringWithEmpty() throws Exception {
        String input = "";
        java.io.InputStream inputStream = new java.io.ByteArrayInputStream(input.getBytes("UTF-8"));
        
        String result = HttpBeanFactory.toBodyString(inputStream);
        
        assertEquals(input, result);
    }

    @Test
    public void testGetObject() {
        HttpBeanFactory<TestRemote> factory = new HttpBeanFactory<>(TestRemote.class);
        TestRemote remote = factory.getObject();
        assertNotNull(remote);
    }

    @Test
    public void testRequestContextHolder() {
        // 测试 RequestContextHolder 的设置和获取
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Test-Header", "test-value");
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        assertNotNull(attributes);
        assertEquals("test-value", attributes.getRequest().getHeader("X-Test-Header"));
        
        RequestContextHolder.resetRequestAttributes();
    }
}
