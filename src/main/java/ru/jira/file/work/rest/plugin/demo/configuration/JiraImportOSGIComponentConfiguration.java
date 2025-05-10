package ru.jira.file.work.rest.plugin.demo.configuration;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

@Configuration
public class JiraImportOSGIComponentConfiguration {

    @Bean
    IssueService issueService() {
        return importOsgiService(IssueService.class);
    }

    @Bean
    AttachmentService attachmentService() {
        return importOsgiService(AttachmentService.class);
    }

    @Bean
    AttachmentManager attachmentManager() {
        return importOsgiService(AttachmentManager.class);
    }

    @Bean
    JiraAuthenticationContext jiraAuthenticationContext() {
        return importOsgiService(JiraAuthenticationContext.class);
    }

    @Bean
    ApplicationProperties applicationProperties() {
        return importOsgiService(ApplicationProperties.class);
    }

    @Bean
    ActiveObjects activeObjects() {
        return importOsgiService(ActiveObjects.class);
    }
}
