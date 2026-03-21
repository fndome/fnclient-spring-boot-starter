/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fndo.fnclient;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import me.fndo.fnclient.core.Standalone;
import me.fndo.fnclient.core.StandaloneMethodInterceptor;
import me.fndo.fnclient.core.util.ClassFileReader;

/**
 * Standalone 模式的 FactoryBean
 * <p>
 * 用于创建基于本地 Controller 的远程接口代理。
 * 通过扫描指定包中的 Controller，建立 Remote 接口方法与 Controller 方法的映射关系，
 * 实现对 Remote 接口调用的本地路由。
 *
 * @param <T> 远程接口类型
 * @author sim
 */
public class StandaloneFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneFactoryBean.class);

    private final Class<T> remoteInterface;
    private final String[] standalonePackages;

    private final Map<Method, Object> methodControllerMap = new ConcurrentHashMap<>();
    private final Map<Method, Method> methodMapping = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    public StandaloneFactoryBean(Class<T> remoteInterface, String[] standalonePackages) {
        this.remoteInterface = remoteInterface;
        this.standalonePackages = standalonePackages;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        initializeMethodMappings();
    }

    /**
     * 初始化方法映射关系
     * <p>
     * 扫描 standalonePackages 配置中的 Controller，
     * 建立 Remote 接口方法与 Controller 方法的双向映射
     */
    private void initializeMethodMappings() {
        if (applicationContext == null) {
            return;
        }

        if (!isStandaloneModeEnabled()) {
            return;
        }

        scanAndMapControllers();
    }

    /**
     * 判断是否启用了 Standalone 模式
     */
    private boolean isStandaloneModeEnabled() {
        return standalonePackages != null && standalonePackages.length > 0;
    }

    /**
     * 扫描 Controller 并建立方法映射
     */
    private void scanAndMapControllers() {
        for (String packageName : standalonePackages) {
            if (!StringUtils.hasText(packageName)) {
                continue;
            }

            Set<Class<?>> classes = ClassFileReader.getClasses(packageName);
            for (Class<?> clazz : classes) {
                processControllerClass(clazz);
            }
        }
    }

    /**
     * 处理单个 Controller 类
     */
    private void processControllerClass(Class<?> clazz) {
        Standalone standalone = AnnotationUtils.findAnnotation(clazz, Standalone.class);
        if (standalone == null) {
            return;
        }

        Class<?> targetRemoteInterface = standalone.remote();
        if (!isValidRemoteInterface(targetRemoteInterface)) {
            return;
        }

        if (!remoteInterface.equals(targetRemoteInterface)) {
            return;
        }

        Object controllerBean = getControllerBean(clazz);
        if (controllerBean == null) {
            return;
        }

        mapControllerMethods(clazz, controllerBean);
    }

    /**
     * 验证 Remote 接口是否有效
     */
    private boolean isValidRemoteInterface(Class<?> remoteInterface) {
        return remoteInterface != null && remoteInterface != void.class;
    }

    /**
     * 从 ApplicationContext 获取 Controller Bean
     */
    private Object getControllerBean(Class<?> clazz) {
        try {
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            LOGGER.debug("Failed to get bean for class: {}", clazz.getName(), e);
            return null;
        }
    }

    /**
     * 映射 Controller 方法到 Remote 接口方法
     */
    private void mapControllerMethods(Class<?> controllerClass, Object controllerBean) {
        Method[] controllerMethods = controllerClass.getDeclaredMethods();
        for (Method controllerMethod : controllerMethods) {
            try {
                Method remoteMethod = remoteInterface.getMethod(
                        controllerMethod.getName(),
                        controllerMethod.getParameterTypes()
                );
                methodControllerMap.put(remoteMethod, controllerBean);
                methodMapping.put(remoteMethod, controllerMethod);
            } catch (NoSuchMethodException e) {
                // Controller 方法在 Remote 接口中没有对应，忽略
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        Assert.notNull(applicationContext, "ApplicationContext must be set before creating proxy");

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(remoteInterface);
        proxyFactory.addAdvice(new StandaloneMethodInterceptor(remoteInterface, methodControllerMap, methodMapping));

        return (T) proxyFactory.getProxy();
    }

    @Override
    public Class<?> getObjectType() {
        return remoteInterface;
    }
}
