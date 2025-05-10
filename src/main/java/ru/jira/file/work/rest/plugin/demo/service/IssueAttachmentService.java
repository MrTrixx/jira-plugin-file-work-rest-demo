package ru.jira.file.work.rest.plugin.demo.service;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.rest.common.multipart.FilePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.jira.file.work.rest.plugin.demo.dto.FileAddResponseDto;
import ru.jira.file.work.rest.plugin.demo.dto.FileData;

import javax.ws.rs.core.StreamingOutput;
import java.util.Optional;

import static com.atlassian.jira.util.ErrorCollection.Reason.FORBIDDEN;
import static com.atlassian.jira.util.ErrorCollection.Reason.NOT_FOUND;
import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueAttachmentService {
    private static final String ATTACHMENT_LINK_TEMPLATE = "%s/jira/rest/api/latest/attachments/%s";

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final IssueService issueService;
    private final ApplicationProperties applicationProperties;
    private final PluginAttachmentService pluginAttachmentService;

    public FileAddResponseDto addFileToIssue(final long issueId, final FilePart filePart) {
        final var loggedInUser = getLoggedInUserOrThrowSecurityException();
        final var issue = getIssueById(issueId, loggedInUser);
        final var attachmentId = pluginAttachmentService.createAttachment(filePart, loggedInUser, issue);
        return FileAddResponseDto.builder()
                                 .attachmentLink(format(
                                         ATTACHMENT_LINK_TEMPLATE,
                                         applicationProperties.getString(APKeys.JIRA_BASEURL), attachmentId
                                 ))
                                 .attachmentName(filePart.getName())
                                 .build();
    }

    public FileData<byte[]> getFileFromIssue(final long attachmentId, final long issueId) {
        final var loggedInUser = getLoggedInUserOrThrowSecurityException();
        final var issue = getIssueById(issueId, loggedInUser);
        return pluginAttachmentService.getAttachmentFromIssue(attachmentId, issue, loggedInUser);
    }

    public FileData<StreamingOutput> getAttachmentsFromIssueAsZip(final long issueId, final String zipFileName) {
        final var loggedInUser = getLoggedInUserOrThrowSecurityException();
        final var issue = getIssueById(issueId, loggedInUser);
        return pluginAttachmentService.getAttachmentsFromIssueAsZip(issue, loggedInUser, zipFileName);
    }

    private ApplicationUser getLoggedInUserOrThrowSecurityException() {
        return Optional.ofNullable(jiraAuthenticationContext.getLoggedInUser())
                       .orElseThrow(SecurityException::new);
    }

    private Issue getIssueById(final long issueId, final ApplicationUser loggedInUser) {
        final var issueValidationResult = issueService.getIssue(loggedInUser, issueId);
        if (issueValidationResult.getErrorCollection()
                                 .getReasons()
                                 .contains(FORBIDDEN)) {
            throw new IssuePermissionException(format("User %s has not permission", loggedInUser.getName()));
        } else if (issueValidationResult.getErrorCollection()
                                        .getReasons()
                                        .contains(NOT_FOUND)) {
            throw new IssueNotFoundException(format("Issue with id %s not found", issueId));
        } else {
            return issueValidationResult.getIssue();
        }
    }
}
