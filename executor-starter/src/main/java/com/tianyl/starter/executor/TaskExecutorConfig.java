package com.tianyl.starter.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskExecutorConfig {

	@Value("${core.pool.size:10}")
	private int corePoolSize;

	@Value("${max.pool.size:10}")
	private int maxPoolSize;

	@Value("${queue.capacity:0}")
	private int queueCapacity;

	@Bean
	public Executor getAsyncExecutor() {
		BlockingQueue<Runnable> queue = null;
		if (queueCapacity > 0) {
			queue = new LinkedBlockingQueue<Runnable>(queueCapacity);
		} else {
			queue = new SynchronousQueue<Runnable>();
		}
		final Logger logger = LoggerFactory.getLogger("AsyncExecutor-task");
		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS, queue) {
			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				if (t != null) {
					// 记录日志
					logger.error("异步操作出现异常", t);
				}
			}
		};

		return executor;
	}

}