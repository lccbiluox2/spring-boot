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

package org.springframework.boot.web.servlet.support;

import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * An opinionated {@link WebApplicationInitializer} to run a {@link SpringApplication}
 * from a traditional WAR deployment. Binds {@link Servlet}, {@link Filter} and
 * {@link ServletContextInitializer} beans from the application context to the server.
 * <p>
 * To configure the application either override the
 * {@link #configure(SpringApplicationBuilder)} method (calling
 * {@link SpringApplicationBuilder#sources(Class...)}) or make the initializer itself a
 * {@code @Configuration}. If you are using {@link SpringBootServletInitializer} in
 * combination with other {@link WebApplicationInitializer WebApplicationInitializers} you
 * might also want to add an {@code @Ordered} annotation to configure a specific startup
 * order.
 * <p>
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded web server then you won't need this at
 * all.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see #configure(SpringApplicationBuilder)
 */
public abstract class SpringBootServletInitializer implements WebApplicationInitializer {

	protected Log logger; // Don't initialize early

	private boolean registerErrorPageFilter = true;

	/**
	 * Set if the {@link ErrorPageFilter} should be registered. Set to {@code false} if
	 * error page mappings should be handled via the server and not Spring Boot.
	 * @param registerErrorPageFilter if the {@link ErrorPageFilter} should be registered.
	 */
	protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter) {
		this.registerErrorPageFilter = registerErrorPageFilter;
	}

	/***
	 * todo: 九师兄  2023/8/12 22:34
	 *
	 * 【Spring】Spring 外置 tomcat 启动原理
	 * https://blog.csdn.net/qq_21383435/article/details/132254559
	 *
	 */
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		// Logger initialization is deferred in case an ordered
		// LogServletContextInitializer is being used
		this.logger = LogFactory.getLog(getClass());
		WebApplicationContext rootAppContext = createRootApplicationContext(servletContext);
		if (rootAppContext != null) {
			servletContext.addListener(new ContextLoaderListener(rootAppContext) {

				@Override
				public void contextInitialized(ServletContextEvent event) {
					// no-op because the application context is already initialized
				}

			});
		}
		else {
			this.logger.debug("No ContextLoaderListener registered, as createRootApplicationContext() did not "
					+ "return an application context");
		}
	}

	/***
	 * todo: 九师兄  2023/8/12 22:41
	 *
	 * createRootApplicationContext 方法是 Spring Boot Servlet 初始化器的一个关键方法。它是在使用 Servlet
	 * 容器启动 Spring Boot 应用程序时，初始化根应用程序上下文的入口点。
	 *
	 */
	protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
		// 这里创建了 SpringApplication
		SpringApplicationBuilder builder = createSpringApplicationBuilder();
		// 设置spring boot要运行的主方法
		builder.main(getClass());
		// 获取已存在的根Web应用程序上下文（如果有）。
		ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
		if (parent != null) {
			// 如果已存在根上下文，将其设置为父上下文，并清除ServletContext中的已存在上下文属性。
			this.logger.info("Root context already created (using as parent).");
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
			builder.initializers(new ParentContextApplicationContextInitializer(parent));
		}
		// 添加ServletContext的ApplicationContext初始化器
		builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
		// 设置使用注解配置的ServletWebServer应用程序上下文。
		builder.contextClass(AnnotationConfigServletWebServerApplicationContext.class);
		// 调用可能由子类重写的配置方法。
		builder = configure(builder);
		// 添加处理Web环境属性的初始化监听器。
		builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
		// 使用构建器创建SpringApplication实例。
		SpringApplication application = builder.build();
		// 检查是否没有定义任何SpringApplication的来源（sources）。如果没有任何来源，并且当前类上存在
		// @Configuration注解，则将当前类设置为主要来源。
		if (application.getAllSources().isEmpty()
				&& MergedAnnotations.from(getClass(), SearchStrategy.TYPE_HIERARCHY).isPresent(Configuration.class)) {
			application.addPrimarySources(Collections.singleton(getClass()));
		}
		Assert.state(!application.getAllSources().isEmpty(),
				"No SpringApplication sources have been defined. Either override the "
						+ "configure method or add an @Configuration annotation");
		// Ensure error pages are registered
		// 如果没有任何来源被定义，则抛出异常
		if (this.registerErrorPageFilter) {
			application.addPrimarySources(Collections.singleton(ErrorPageFilterConfiguration.class));
		}
		// 运行SpringApplication并返回WebApplicationContext对象。
		return run(application);
	}

	/**
	 * Returns the {@code SpringApplicationBuilder} that is used to configure and create
	 * the {@link SpringApplication}. The default implementation returns a new
	 * {@code SpringApplicationBuilder} in its default state.
	 * @return the {@code SpringApplicationBuilder}.
	 * @since 1.3.0
	 */
	protected SpringApplicationBuilder createSpringApplicationBuilder() {
		// 这里创建了 SpringApplication
		return new SpringApplicationBuilder();
	}

	/**
	 * Called to run a fully configured {@link SpringApplication}.
	 * @param application the application to run
	 * @return the {@link WebApplicationContext}
	 */
	protected WebApplicationContext run(SpringApplication application) {
		return (WebApplicationContext) application.run();
	}

	/***
	 * todo: 九师兄  2023/8/12 22:47
	 *
	 * getExistingRootWebApplicationContext的作用是获取已存在的根Web应用程序上下文。
	 *
	 * 在Spring Boot中，通过SpringBootServletInitializer类来支持将Spring Boot应用程序部署到Servlet容器中
	 * （如Tomcat、Jetty等）。当部署到Servlet容器中时，Servlet容器会初始化应用程序，并创建一个Web应用程序上下文
	 * （WebApplicationContext）。
	 *
	 * getExistingRootWebApplicationContext方法用于检查是否已经存在根Web应用程序上下文。如果存在，则返回已存在
	 * 的上下文对象。这通常发生在应用程序启动后通过其他方式（如XML配置文件、Java配置类等）创建了根上下文。
	 *
	 * 如果存在根上下文，SpringBootServletInitializer会将其设置为新创建的SpringApplicationBuilder实例的父上
	 * 下文。这样可以实现Spring Boot应用程序与已有的上下文进行集成。
	 *
	 * 总结而言，getExistingRootWebApplicationContext方法的作用是获取已存在的根Web应用程序上下文，以便在Servlet
	 * 容器中部署Spring Boot应用程序时能够与已有的上下文进行集成。
	 */
	private ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
		// ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE是用于将根WebApplicationContext绑定到属性上的常量，在成功
		// 启动时使用，并提供了方便查找根上下文的方法。
		Object context = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (context instanceof ApplicationContext) {
			return (ApplicationContext) context;
		}
		return null;
	}

	/**
	 * Configure the application. Normally all you would need to do is to add sources
	 * (e.g. config classes) because other settings have sensible defaults. You might
	 * choose (for instance) to add default command line arguments, or set an active
	 * Spring profile.
	 * @param builder a builder for the application context
	 * @return the application builder
	 * @see SpringApplicationBuilder
	 */
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder;
	}

	private static final class WebEnvironmentPropertySourceInitializer
			implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

		private final ServletContext servletContext;

		private WebEnvironmentPropertySourceInitializer(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
			ConfigurableEnvironment environment = event.getEnvironment();
			if (environment instanceof ConfigurableWebEnvironment) {
				((ConfigurableWebEnvironment) environment).initPropertySources(this.servletContext, null);
			}
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

}
