package com.softai.email.scheduler.jobs;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class EmailJob extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(EmailJob.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailProperties mailProperties;

    @Override
    protected void executeInternal(JobExecutionContext exec) throws JobExecutionException {
        logger.info("Excecuting job with key {}", exec.getJobDetail().getKey());

        JobDataMap jobDataMap = exec.getMergedJobDataMap();
        String subject = jobDataMap.getString("subject");
        String body = jobDataMap.getString("body");
        String recipentEmail = jobDataMap.getString("email");

        sendMail(mailProperties.getUsername(), recipentEmail, subject, body);
    }
    

    private void sendMail(String fromEmail, String toEmail, String subject, String body) {
        try {
            logger.info("Sending email to {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");
            messageHelper.setFrom(fromEmail);
            messageHelper.setTo(toEmail);
            messageHelper.setSubject(subject);
            messageHelper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}", toEmail);
        }
    }
}
