package com.msas.scheduler.configuration;

import com.msas.scheduler.listener.JobsListener;
import com.msas.scheduler.listener.TriggersListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzConfiguration {

    private final ApplicationContext applicationContext;

    private final TriggersListener triggersListener;

    private final JobsListener jobsListener;

    private final QuartzProperties quartzProperties;

    @PostConstruct
    public void init() {
        log.info("(!)QuartzConfiguration initialized.");
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * Quartz 관련 설정
     *
     * @param applicationContext the applicationContext
     * @return SchedulerFactoryBean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(ApplicationContext applicationContext) {

        //SchedulerFactoryBean -> Spring Framework에서 Quartz를 bean으로 관리하기 위해 필요한 클래스
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        schedulerFactoryBean.setJobFactory(jobFactory);

        schedulerFactoryBean.setApplicationContext(applicationContext);

        Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());

        schedulerFactoryBean.setGlobalTriggerListeners(triggersListener);
        schedulerFactoryBean.setGlobalJobListeners(jobsListener);
        schedulerFactoryBean.setOverwriteExistingJobs(true);
        schedulerFactoryBean.setQuartzProperties(properties);
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
        return schedulerFactoryBean;
    }

    @Bean
    public SmartLifecycle gracefulShutdownHookForQuartz(@Qualifier("schedulerFactoryBean") SchedulerFactoryBean schedulerFactoryBean) {
        return new SmartLifecycle() {
            private boolean isRunning = false;

            @Override
            public boolean isAutoStartup() {
                return true;
            }

            @Override
            public void stop(Runnable callback) {
                stop();
                log.info("Spring container is shutting down.");
                callback.run();
            }

            @Override
            public void start() {
                log.info("Quartz Graceful Shutdown Hook started.");
                isRunning = true;
            }

            @Override
            public void stop() {
                isRunning = false;

                try {
                    log.info("Quartz Graceful Shutdown...");
                    interruptJobs(schedulerFactoryBean);
                    schedulerFactoryBean.destroy();
                } catch (SchedulerException e) {
                    try {
                        log.info("Error shutting down Quartz: ", e);
                        schedulerFactoryBean.getScheduler().shutdown(false);
                    } catch (SchedulerException ex) {
                        log.error("Unable to shutdown the Quartz scheduler.", ex);
                    }
                }
            }

            private void interruptJobs(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
                Scheduler scheduler = schedulerFactoryBean.getScheduler();
                for (JobExecutionContext jobExecutionContext : scheduler.getCurrentlyExecutingJobs()) {
                    final JobDetail jobDetail = jobExecutionContext.getJobDetail();
                    log.info("interrupting job :: jobKey : {}", jobDetail.getKey());
                    scheduler.interrupt(jobDetail.getKey());
                }
            }

            @Override
            public boolean isRunning() {
                return isRunning;
            }

            @Override
            public int getPhase() {
                return Integer.MAX_VALUE;
            }
        };
    }
}