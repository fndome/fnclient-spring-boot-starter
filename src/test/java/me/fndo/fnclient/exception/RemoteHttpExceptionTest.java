package me.fndo.fnclient.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 远程调用异常测试
 */
public class RemoteHttpExceptionTest {

    @Test
    public void testRemote1xxException() {
        Remote1xxException exception = new Remote1xxException(100, "http://test/api", "Continue");
        
        assertEquals(100, exception.getStatusCode());
        assertEquals("http://test/api", exception.getUri());
        assertEquals("Continue", exception.getBody());
        assertTrue(exception.getMessage().contains("1xx Informational"));
        assertTrue(exception instanceof RemoteCallException);
    }

    @Test
    public void testRemote3xxException() {
        Remote3xxException exception = new Remote3xxException(302, "http://test/api", "Found");
        
        assertEquals(302, exception.getStatusCode());
        assertEquals("http://test/api", exception.getUri());
        assertEquals("Found", exception.getBody());
        assertTrue(exception.getMessage().contains("3xx Redirection"));
        assertTrue(exception instanceof RemoteCallException);
    }

    @Test
    public void testRemote4xxException() {
        Remote4xxException exception = new Remote4xxException(404, "http://test/api/1", "Not Found");
        
        assertEquals(404, exception.getStatusCode());
        assertEquals("http://test/api/1", exception.getUri());
        assertEquals("Not Found", exception.getBody());
        assertTrue(exception.getMessage().contains("4xx Client Error"));
        assertTrue(exception instanceof RemoteCallException);
    }

    @Test
    public void testRemote5xxException() {
        Remote5xxException exception = new Remote5xxException(503, "http://test/api/service", "Service Unavailable");
        
        assertEquals(503, exception.getStatusCode());
        assertEquals("http://test/api/service", exception.getUri());
        assertEquals("Service Unavailable", exception.getBody());
        assertTrue(exception.getMessage().contains("5xx Server Error"));
        assertTrue(exception instanceof RemoteCallException);
    }

    @Test
    public void testExceptionHierarchy() {
        // 验证所有异常都是 RuntimeException 的子类
        assertTrue(new Remote1xxException(100, "http://test", "") instanceof RuntimeException);
        assertTrue(new Remote3xxException(302, "http://test", "") instanceof RuntimeException);
        assertTrue(new Remote4xxException(404, "http://test", "") instanceof RuntimeException);
        assertTrue(new Remote5xxException(500, "http://test", "") instanceof RuntimeException);
    }
}
