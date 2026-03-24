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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;
import org.springframework.web.service.annotation.HttpExchange;

import me.fndo.fnclient.core.Standalone;
import me.fndo.fnclient.core.config.HttpBeanFactory;
import me.fndo.fnclient.core.util.ClassFileReader;

/**
 * HTTP Exchange Bean 定义注册器
 * <p>
 * 负责扫描和注册 HTTP 远程服务接口的 Bean 定义。
 * 支持混合模式：优先使用 Standalone 本地调用，未覆盖的接口使用 HTTP 远程代理。
 *
 * @author sim
 */
public class HttpExchangeRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        String basePackage = resolveBasePackage(annotationMetadata);
        Set<Class<?>> allClasses = scanClasses(basePackage, annotationMetadata);
        List<Class<?>> remoteInterfaces = findRemoteInterfaces(allClasses);

        Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(EnableHttpExchange.class.getName());
        String[] standalonePackages = resolveStandalonePackages(attributes);
        String httpClientClassName = resolveHttpClientClassName(attributes);

        Set<Class<?>> standaloneCoveredRemotes = findStandaloneCoveredRemotes(remoteInterfaces, standalonePackages);

        registerBeanDefinitions(remoteInterfaces, standaloneCoveredRemotes, standalonePackages, httpClientClassName, registry);
    }

    /**
     * 解析基础包路径
     */
    private String resolveBasePackage(AnnotationMetadata annotationMetadata) {
        String startClassName = annotationMetadata.getClassName();
        int lastDotIndex = startClassName.lastIndexOf('.');
        return lastDotIndex > 0 ? startClassName.substring(0, lastDotIndex) : "";
    }

    /**
     * 扫描所有相关类
     */
    private Set<Class<?>> scanClasses(String basePackage, AnnotationMetadata annotationMetadata) {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(ClassFileReader.getClasses(basePackage));

        Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(EnableHttpExchange.class.getName());
        if (attributes != null) {
            Object basePackagesObj = attributes.get("basePackages");
            if (basePackagesObj instanceof String[]) {
                String[] basePackages = (String[]) basePackagesObj;
                for (String pkg : basePackages) {
                    classes.addAll(ClassFileReader.getClasses(pkg));
                }
            }
        }

        return classes;
    }

    /**
     * 查找所有 HttpExchange 接口
     */
    private List<Class<?>> findRemoteInterfaces(Set<Class<?>> classes) {
        List<Class<?>> remoteInterfaces = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (isHttpExchangeInterface(clazz)) {
                remoteInterfaces.add(clazz);
            }
        }
        return remoteInterfaces;
    }

    /**
     * 判断是否为 HttpExchange 接口
     */
    private boolean isHttpExchangeInterface(Class<?> clazz) {
        return clazz.isInterface() && clazz.isAnnotationPresent(HttpExchange.class);
    }

    /**
     * 解析 Standalone 包配置
     */
    private String[] resolveStandalonePackages(Map<String, Object> attributes) {
        if (attributes == null) {
            return new String[0];
        }
        Object standalonePackagesObj = attributes.get("standalonePackages");
        if (standalonePackagesObj instanceof String[]) {
            return (String[]) standalonePackagesObj;
        }
        return new String[0];
    }

    /**
     * 解析 HTTP 客户端配置类名
     */
    private String resolveHttpClientClassName(Map<String, Object> attributes) {
        if (attributes == null) {
            return "org.springframework.web.client.RestTemplate";
        }
        Object value = attributes.get("httpClientClass");
        if (value instanceof Class<?>) {
            return ((Class<?>) value).getName();
        }
        return "org.springframework.web.client.RestTemplate";
    }

    /**
     * 查找被 Standalone 覆盖的 Remote 接口
     */
    private Set<Class<?>> findStandaloneCoveredRemotes(List<Class<?>> remoteInterfaces, String[] standalonePackages) {
        Set<Class<?>> coveredRemotes = new HashSet<>();

        if (standalonePackages == null || standalonePackages.length == 0) {
            return coveredRemotes;
        }

        for (String packageName : standalonePackages) {
            Set<Class<?>> classes = ClassFileReader.getClasses(packageName);
            for (Class<?> clazz : classes) {
                Standalone standalone = clazz.getAnnotation(Standalone.class);
                if (standalone != null) {
                    Class<?> remoteClass = standalone.remote();
                    if (isValidRemoteClass(remoteClass) && remoteInterfaces.contains(remoteClass)) {
                        coveredRemotes.add(remoteClass);
                    }
                }
            }
        }

        return coveredRemotes;
    }

    /**
     * 验证 Remote 类是否有效
     */
    private boolean isValidRemoteClass(Class<?> remoteClass) {
        return remoteClass != null && remoteClass != void.class;
    }

    /**
     * 注册 Bean 定义
     */
    private void registerBeanDefinitions(List<Class<?>> remoteInterfaces,
                                         Set<Class<?>> standaloneCoveredRemotes,
                                         String[] standalonePackages,
                                         String httpClientClassName,
                                         BeanDefinitionRegistry registry) {
        // 如果没有配置 standalonePackages 且没有 remote 需要覆盖，说明这是从子模块应用触发的注册
        // 跳过注册，等待主应用（standalone-starter）来注册
        if (standalonePackages.length == 0 && standaloneCoveredRemotes.isEmpty()) {
            return;
        }

        for (Class<?> clazz : remoteInterfaces) {
            String beanName = StringUtils.uncapitalize(clazz.getSimpleName());

            // 如果 Bean 已经存在，跳过注册（避免重复注册导致冲突）
            if (registry.containsBeanDefinition(beanName)) {
                continue;
            }

            if (standaloneCoveredRemotes.contains(clazz)) {
                registerStandaloneBean(clazz, standalonePackages, registry, beanName);
            } else {
                registerRemoteProxyBean(clazz, httpClientClassName, registry, beanName);
            }
        }
    }

    /**
     * 注册 Standalone Bean
     */
    private void registerStandaloneBean(Class<?> clazz, String[] standalonePackages,
                                        BeanDefinitionRegistry registry, String beanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StandaloneFactoryBean.class);
        builder.addConstructorArgValue(clazz);
        builder.addConstructorArgValue(standalonePackages);

        GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);

        registry.registerBeanDefinition(beanName, definition);
    }

    /**
     * 注册远程代理 Bean
     */
    private void registerRemoteProxyBean(Class<?> clazz, String httpClientClassName,
                                         BeanDefinitionRegistry registry, String beanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HttpBeanFactory.class);
        builder.addConstructorArgValue(clazz);
        builder.addConstructorArgValue(httpClientClassName);

        GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);

        registry.registerBeanDefinition(beanName, definition);
    }
}
