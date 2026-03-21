package me.fndo.fnclient;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 测试用 Remote 接口
 */
@HttpExchange("http://test/api")
public interface TestRemote {

    @GetExchange("/user/{id}")
    TestUser getUser(@PathVariable("id") Long id);

    @GetExchange("/health")
    String health();
}
