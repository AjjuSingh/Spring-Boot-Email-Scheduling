package com.softai.email.scheduler.controllers;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import javax.validation.Valid;

import com.softai.email.scheduler.payload.EmailRequest;
import com.softai.email.scheduler.payload.EmailResponse;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailSchedulerController{
    @Autowired
    private Scheduler scheduler;

    private static final Logger logger = LoggerFactory.getLogger(EmailSchedulerController.class);


    /**
     * Sample request body
     * {
            "email":"iamcloud.dev@gmail.com",
            "subject":"This is automated email",
            "body":"This is testing of scheduling email APIs",
            "dateTime":"2021-07-10T14:09:23",
            "timeZoneId":"Asia/Kolkata"
        }
     * @param request
     * @return
     */

    @PostMapping("/schedulerEmail")
    public ResponseEntity<EmailResponse> scheduleEmail(@Valid @RequestBody EmailRequest request){
        try {
            ZonedDateTime dateTime = ZonedDateTime.of(request.getDateTime(), request.getTimeZoneId());
            if(dateTime.isBefore(ZonedDateTime.now())){
                EmailResponse response = new EmailResponse(false, "DateTime must be after current time");
                return ResponseEntity.badRequest().body(response);
            }
            JobDetail jobDetail = buildJobDetail(request);
            Trigger trigger = buildTrigger(jobDetail, dateTime);
            scheduler.scheduleJob(jobDetail, trigger);
            EmailResponse response = new EmailResponse(true, jobDetail.getKey().getName(), jobDetail.getKey().getGroup(), "Email scheduled successfully!!!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error scheduling email", e);
            EmailResponse response = new EmailResponse(false, "Error scheduling email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private JobDetail buildJobDetail(EmailRequest sEmailRequest){
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email", sEmailRequest.getEmail());
        jobDataMap.put("subject", sEmailRequest.getSubject());
        jobDataMap.put("body", sEmailRequest.getBody());

        return JobBuilder
            .newJob(com.softai.email.scheduler.jobs.EmailJob.class)
            .withIdentity(UUID.randomUUID().toString(), "email-jobs")
            .withDescription("Send email jobs")
            .usingJobData(jobDataMap)
            .storeDurably()
            .build();
    }

    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt){
        return TriggerBuilder.newTrigger().forJob(jobDetail)
            .withIdentity(jobDetail.getKey().getName(), "email-triggers")
            .withDescription("Send email trigger")
            .startAt(Date.from(startAt.toInstant()))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
            .build();
    }
}
