package com.example.websocketchatbacked.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

    // CPU核心数，用于动态计算线程池参数
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    @Bean(name = "loggingExecutor")
    public Executor loggingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("logging-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 分块任务线程池（CPU密集型）
     */
    @Bean("chunkExecutor")
    public Executor chunkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES + 1); // 核心线程数：CPU核心数+1
        executor.setMaxPoolSize(CPU_CORES * 2); // 最大线程数：CPU核心数*2
        executor.setQueueCapacity(100); // 队列容量
        executor.setThreadNamePrefix("chunk-task-"); // 线程名前缀
        // 拒绝策略：当队列满且线程数达最大时，由调用线程执行任务（减缓提交速度）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待所有任务完成后再关闭，最多等60秒
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize(); // 初始化线程池
        return executor;
    }

    /**
     * 上传任务线程池（IO密集型）
     */
    @Bean("uploadExecutor")
    public Executor uploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES * 2); // 核心线程数：CPU核心数*2
        executor.setMaxPoolSize(CPU_CORES * 4); // 最大线程数：CPU核心数*4
        executor.setQueueCapacity(200); // 队列容量，比CPU密集型大
        executor.setThreadNamePrefix("upload-task-"); // 线程名前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 向量化任务线程池（CPU/IO混合）
     */
    @Bean("vectorExecutor")
    public Executor vectorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES * 2); // 核心线程数：CPU核心数*2
        executor.setMaxPoolSize(CPU_CORES * 3); // 最大线程数：CPU核心数*3
        executor.setQueueCapacity(150); // 队列容量，介于前两者之间
        executor.setThreadNamePrefix("vector-task-"); // 线程名前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
