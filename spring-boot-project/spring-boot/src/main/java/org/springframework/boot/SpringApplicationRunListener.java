/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Listener for the {@link SpringApplication} {@code run} method.
 * {@link SpringApplicationRunListener}s are loaded via the {@link SpringFactoriesLoader}
 * and should declare a public constructor that accepts a {@link SpringApplication}
 * instance and a {@code String[]} of arguments. A new
 * {@link SpringApplicationRunListener} instance will be created for each run.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 *
 * 目前，SpringBoot 中自带的 SpringApplicationRunListener 接口只有一个实现类：
 * EventPublishingRunListener，该实现类作用：通过观察者模式的事件机制，在 run 方法
 * 的不同阶段触发 Event 事件，ApplicationListener 的实现类们通过监听不同的 Event
 * 事件对象触发不同的业务处理逻辑。
 *
 * 通过自定义实现 ApplicationListener 实现类，可以在 SpringBoot 启动的不同阶段，实现
 * 一定的处理，可见SpringApplicationRunListener 接口给 SpringBoot 带来了扩展性
 *
 * 「添加一个自定义的实现类，在不同阶段结束时打印下当前时间，通过计算不同阶段的运行时间，就能大体定位哪些阶段耗时比较高」
 * ，然后重点排查这些阶段的代码。
 */
public interface SpringApplicationRunListener {

	/**
	 * Called immediately when the run method has first started. Can be used for very
	 * early initialization.
	 *
	 * run 方法第一次被执行时调用，早期初始化工作
	 */
	default void starting() {
	}

	/**
	 * Called once the environment has been prepared, but before the
	 * {@link ApplicationContext} has been created.
	 * @param environment the environment
	 *
	 * nvironment 创建后，ApplicationContext 创建前
	 */
	default void environmentPrepared(ConfigurableEnvironment environment) {
	}

	/**
	 * Called once the {@link ApplicationContext} has been created and prepared, but
	 * before sources have been loaded.
	 * @param context the application context
	 *
	 * ApplicationContext 实例创建，部分属性设置了
	 */
	default void contextPrepared(ConfigurableApplicationContext context) {
	}

	/**
	 * Called once the application context has been loaded but before it has been
	 * refreshed.
	 * @param context the application context
	 *
	 *  ApplicationContext 加载后，refresh 前
	 */
	default void contextLoaded(ConfigurableApplicationContext context) {
	}

	/**
	 * The context has been refreshed and the application has started but
	 * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
	 * ApplicationRunners} have not been called.
	 * @param context the application context.
	 * @since 2.0.0
	 *
	 * refresh 后
	 */
	default void started(ConfigurableApplicationContext context) {
	}

	/**
	 * Called immediately before the run method finishes, when the application context has
	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
	 * {@link ApplicationRunner ApplicationRunners} have been called.
	 * @param context the application context.
	 * @since 2.0.0
	 *
	 * 所有初始化完成后，run 结束前
	 */
	default void running(ConfigurableApplicationContext context) {
	}

	/**
	 * Called when a failure occurs when running the application.
	 * @param context the application context or {@code null} if a failure occurred before
	 * the context was created
	 * @param exception the failure
	 * @since 2.0.0
	 *
	 * 初始化失败后
	 */
	default void failed(ConfigurableApplicationContext context, Throwable exception) {
	}

}
