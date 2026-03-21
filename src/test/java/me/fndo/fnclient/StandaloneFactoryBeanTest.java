package me.fndo.fnclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StandaloneFactoryBean 测试
 */
@SpringBootTest
@ContextConfiguration(classes = {TestConfig.class, TestController.class})
public class StandaloneFactoryBeanTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testStandaloneFactoryBean() throws Exception {
        // 创建 StandaloneFactoryBean
        StandaloneFactoryBean<TestRemote> factoryBean = new StandaloneFactoryBean<>(
            TestRemote.class, 
            new String[]{"me.fndo.fnclient"}
        );

        // 设置 ApplicationContext
        factoryBean.setApplicationContext(applicationContext);

        // 验证 getObjectType
        assertEquals(TestRemote.class, factoryBean.getObjectType());

        // 获取代理对象
        TestRemote remote = factoryBean.getObject();
        assertNotNull(remote);
    }

    @Test
    public void testStandaloneFactoryBeanWithEmptyPackages() throws Exception {
        StandaloneFactoryBean<TestRemote> factoryBean = new StandaloneFactoryBean<>(
            TestRemote.class, 
            new String[]{}
        );

        factoryBean.setApplicationContext(applicationContext);

        // 空 packages 应该也能创建代理
        TestRemote remote = factoryBean.getObject();
        assertNotNull(remote);
    }

    @Test
    public void testStandaloneFactoryBeanWithNullPackages() throws Exception {
        StandaloneFactoryBean<TestRemote> factoryBean = new StandaloneFactoryBean<>(
            TestRemote.class, 
            null
        );

        factoryBean.setApplicationContext(applicationContext);

        TestRemote remote = factoryBean.getObject();
        assertNotNull(remote);
    }
}
