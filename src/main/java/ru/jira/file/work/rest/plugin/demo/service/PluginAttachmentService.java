package ru.jira.file.work.rest.plugin.demo.service;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.exception.AttachmentNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.AttachmentReadException;
import com.atlassian.jira.issue.attachment.AttachmentRuntimeException;
import com.atlassian.jira.issue.attachment.AttachmentWriteException;
import com.atlassian.jira.issue.attachment.ConvertTemporaryAttachmentParams;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.rest.common.multipart.FilePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import ru.jira.file.work.rest.plugin.demo.dto.FileData;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginAttachmentService {
    private static final String ZIP_FILE_NAME_FROM_ISSUE_TEMPLATE = "zip_file_%s.zip";

    private final AttachmentManager attachmentManager;
    private final AttachmentService attachmentService;

    public long createAttachment(final FilePart filePart, final ApplicationUser loggedInUser, final Issue issue) {
        validateCanCreateAttachment(issue, loggedInUser);
        return internalCreateAttachment(filePart, loggedInUser, issue);
    }

    private void validateCanCreateAttachment(final Issue issue, final ApplicationUser loggedInUser) {
        if (!attachmentService.canCreateTemporaryAttachments(new JiraServiceContextImpl(loggedInUser), issue)) {
            throw new IssuePermissionException(
                    format(
                            "User %s cannot create attachment in issue key: %s id: %s",
                            loggedInUser.getName(),
                            issue.getKey(),
                            issue.getId()
                    )
            );
        }
    }

    private long internalCreateAttachment(
            final FilePart filePart,
            final ApplicationUser loggedInUser,
            final Issue issue
    ) {
        try {
            final var temporaryAttachmentId = attachmentManager.createTemporaryAttachment(
                    filePart.getInputStream(),
                    filePart.getSize()
            );
            final var convertTemporaryAttachmentParams =
                    ConvertTemporaryAttachmentParams.builder()
                                                    .setTemporaryAttachmentId(temporaryAttachmentId)
                                                    .setAuthor(loggedInUser)
                                                    .setIssue(issue)
                                                    .setFilename(filePart.getName())
                                                    .setContentType(filePart.getContentType())
                                                    .setCreatedTime(DateTime.now())
                                                    .setFileSize(filePart.getSize())
                                                    .build();
            return attachmentManager.convertTemporaryAttachment(convertTemporaryAttachmentParams)
                                    .map(changeItemBean -> Long.parseLong(changeItemBean.getTo()))
                                    .getOrThrow(() -> new AttachmentWriteException("Unexpected behavior"));
        } catch (Exception e) {
            throw new AttachmentRuntimeException(e);
        }
    }

    // need improve method and use bucket of attachment to get link on file with full name
    // attachment in issue can be image or thumbnailable and not have extension for that type of file
    public FileData<byte[]> getAttachmentFromIssue(
            final long attachmentId,
            final Issue issue,
            final ApplicationUser loggedInUser
    ) {
        validateCanWorkWithAttachments(issue, loggedInUser);
        return attachmentManager.getAttachments(issue)
                                .stream()
                                .filter(attachment -> attachment.getId().equals(attachmentId))
                                .filter(this::notSupportedInThisPluginFiles)
                                .findFirst()
                                .map(attachment -> convertAttachmentToFileData(issue, attachment))
                                .orElseThrow(() ->
                                        new AttachmentNotFoundException(
                                                format(
                                                        "Attachment %s not found in issue %s",
                                                        attachmentId,
                                                        issue.getKey()
                                                )
                                        )
                                );
    }

    // check all permission on work with attachments
    public void validateCanWorkWithAttachments(final Issue issue, final ApplicationUser loggedInUser) {
        if (!attachmentService.canManageAttachments(new JiraServiceContextImpl(loggedInUser), issue)) {
            throw new IssuePermissionException(
                    format("User %s has not permission on work with attachment in issue key: %s id: %s",
                            loggedInUser.getName(),
                            issue.getKey(),
                            issue.getId()
                    )
            );
        }
    }

    private FileData<byte[]> convertAttachmentToFileData(final Issue issue, final Attachment attachment) {
        try {
            return attachmentManager.streamAttachmentContent(
                    attachment,
                    new FileInputStreamConsumer(attachment.getFilename())
            );
        } catch (IOException e) {
            log.error(
                    "Error happened during reading file {} from issue {}",
                    attachment.getFilename(),
                    issue.getKey()
            );
            throw new AttachmentReadException(e);
        }
    }

    public FileData<StreamingOutput> getAttachmentsFromIssueAsZip(
            final Issue issue,
            final ApplicationUser loggedInUser,
            final String zipFileName) {
        validateCanWorkWithAttachments(issue, loggedInUser);
        final var attachmentsAsBytes = attachmentManager.getAttachments(issue)
                                                        .stream()
                                                        .filter(this::notSupportedInThisPluginFiles)
                                                        .map(attachment ->
                                                                getAttachmentAsBytes(
                                                                        attachment,
                                                                        issue
                                                                )
                                                        )
                                                        .filter(Objects::nonNull)
                                                        .collect(toList());
        final var resolvedZipFileName = isBlank(zipFileName) ? format(ZIP_FILE_NAME_FROM_ISSUE_TEMPLATE, issue.getKey()) : zipFileName;
        return new FileData<>(
                StreamingOutputZipFileCreator.of(attachmentsAsBytes),
                resolvedZipFileName
        );
    }

    private boolean notSupportedInThisPluginFiles(final Attachment attachment) {
        return !attachment.isImage() && !Boolean.TRUE.equals(attachment.isThumbnailable());
    }

    private FileData<byte[]> getAttachmentAsBytes(final Attachment attachment, final Issue issue) {
        try {
            return attachmentManager.streamAttachmentContent(
                    attachment,
                    inputStream -> new FileData<>(
                            IOUtils.toByteArray(inputStream),
                            attachment.getFilename()
                    )
            );
        } catch (IOException e) {
            log.error(
                    "Error happened during reading file {} from issue {}",
                    attachment.getFilename(),
                    issue.getKey(),
                    e
            );
            return null;
        }
    }
}