package com.example.chatbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Background kaam ke liye thread pool. (Purani galat-spelling wali AsyncConfirg.java DELETE karo.)
 * Pool chhota (2) rakha hai: ek saath bahut saare documents embed karne se Gemini rate limit (429) lagta hai.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor(@Value("${app.ingestion.threads:2}") int threads) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(threads);
        ex.setMaxPoolSize(threads);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("ingest-");
        return ex;
    }
}