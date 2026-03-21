package me.fndo.fnclient;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 测试配置类
 */
@Configuration
@Import({StandaloneFactoryBean.class})
public class TestConfig {
}
