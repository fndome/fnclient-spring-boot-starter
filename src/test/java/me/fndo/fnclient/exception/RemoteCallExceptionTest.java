package me.fndo.fnclient.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * RemoteCallException 测试
 */
public class RemoteCallExceptionTest {

    @Test
    public void testRemoteCallException() {
        int statusCode = 404;
        String uri = "http://test/api/user/1";
        String body = "{\"error\": \"Not Found\"}";

        RemoteCallException exception = new RemoteCallException(statusCode, uri, body);

        assertEquals(statusCode, exception.getStatusCode());
        assertEquals(uri, exception.getUri());
        assertEquals(body, exception.getBody());
        assertTrue(exception.getMessage().contains(uri));
        assertTrue(exception.getMessage().contains(String.valueOf(statusCode)));
    }

    @Test
    public void testRemoteCallExceptionWith500() {
        RemoteCallException exception = new RemoteCallException(500, "http://test/api/error", "Internal Server Error");

        assertEquals(500, exception.getStatusCode());
        assertEquals("http://test/api/error", exception.getUri());
        assertEquals("Internal Server Error", exception.getBody());
    }

    @Test
    public void testRemoteCallExceptionWith400() {
        RemoteCallException exception = new RemoteCallException(400, "http://test/api/bad", "Bad Request");

        assertEquals(400, exception.getStatusCode());
    }

    @Test
    public void testRemoteCallExceptionIsRuntimeException() {
        RemoteCallException exception = new RemoteCallException(500, "http://test/api/error", "Error");
        
        // 验证是 RuntimeException 的子类
        assertTrue(exception instanceof RuntimeException);
    }
}
