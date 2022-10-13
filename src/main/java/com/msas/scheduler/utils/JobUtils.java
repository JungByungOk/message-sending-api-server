package com.msas.scheduler.utils;

import com.msas.scheduler.dto.RequestJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public final class JobUtils {

    private JobUtils() {
    }

    public static JobDetail createJob(RequestJob requestJob, Class<? extends Job> jobClass, ApplicationContext context) {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(jobClass);
        factoryBean.setDurability(false);
        factoryBean.setApplicationContext(context);
        factoryBean.setName(requestJob.getJobName());
        factoryBean.setGroup(requestJob.getJobGroup());

        if (requestJob.getJobDataMap() != null) {
            factoryBean.setJobDataMap(requestJob.getJobDataMap());
        }

        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    public static Trigger createTrigger(RequestJob requestJob) {
        String cronExpression = requestJob.getCronExpression();
        LocalDateTime startDateAt = requestJob.getStartDateAt();

        if (!StringUtils.isEmpty(cronExpression)) {
            if (!CronExpression.isValidExpression(cronExpression)) {
                throw new IllegalArgumentException("Provided expression " + cronExpression + " is not a valid cron expression");
            }
            return createCronTrigger(requestJob);
        } else if (startDateAt != null) {
            return createSimpleTrigger(requestJob);
        }
        throw new IllegalStateException("unsupported trigger descriptor");
    }

    private static Trigger createCronTrigger(RequestJob requestJob) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setName(requestJob.getJobName());
        factoryBean.setGroup(requestJob.getJobGroup());
        factoryBean.setCronExpression(requestJob.getCronExpression());
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        try {
            factoryBean.afterPropertiesSet();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return factoryBean.getObject();
    }

    private static Trigger createSimpleTrigger(RequestJob requestJob) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setName(requestJob.getJobName());
        factoryBean.setGroup(requestJob.getJobGroup());
        factoryBean.setStartTime(Date.from(requestJob.getStartDateAt().atZone(ZoneId.systemDefault()).toInstant()));
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        factoryBean.setRepeatInterval(requestJob.getRepeatIntervalInSeconds() * 1000); //ms 단위임
        factoryBean.setRepeatCount(requestJob.getRepeatCount());

        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}