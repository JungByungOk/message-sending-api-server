package com.msas.scheduler.configuration;

import com.msas.scheduler.service.JobsListener;
import com.msas.scheduler.service.TriggersListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Properties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzConfiguration {
    private final TriggersListener triggersListener;

    private final JobsListener jobsListener;

    private final QuartzProperties quartzProperties;

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
}