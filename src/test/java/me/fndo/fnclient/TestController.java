package me.fndo.fnclient;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试用 Controller - 用于 Standalone 模式
 */
@RestController
@RequestMapping("/api")
@Standalone(remote = TestRemote.class)
public class TestController {

    @GetMapping("/user/{id}")
    public TestUser getUser(@PathVariable("id") Long id) {
        return new TestUser(id, "TestUser-" + id);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
