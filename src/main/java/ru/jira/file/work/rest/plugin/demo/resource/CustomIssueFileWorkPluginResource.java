package ru.jira.file.work.rest.plugin.demo.resource;

import com.atlassian.plugins.rest.common.multipart.FilePart;
import com.atlassian.plugins.rest.common.multipart.MultipartConfig;
import com.atlassian.plugins.rest.common.multipart.MultipartConfigClass;
import com.atlassian.plugins.rest.common.multipart.MultipartFormParam;
import lombok.RequiredArgsConstructor;
import ru.jira.file.work.rest.plugin.demo.dto.FileAddResponseDto;
import ru.jira.file.work.rest.plugin.demo.service.IssueAttachmentService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static ru.jira.file.work.rest.plugin.demo.util.Const.ATTACHMENT_CONTENT_DISPOSITION_HEADER_VALUE_TEMPLATE;
import static ru.jira.file.work.rest.plugin.demo.util.Const.CONTENT_DISPOSITION_HEADER_NAME;
import static ru.jira.file.work.rest.plugin.demo.util.Const.ZIP_FILE_NAME_QUERY_PARAM_NAME;

@Path("issue_file_resource")
@RequiredArgsConstructor
public class CustomIssueFileWorkPluginResource {
    public static final String ISSUE_ID_PATH_PARAM_NAME = "issueId";
    public static final String ISSUE_ID_PATH_PARAM = "/{" + ISSUE_ID_PATH_PARAM_NAME + "}";
    public static final String ATTACHMENT_ID_PATH_PARAM = "attachmentId";
    public static final String FILE_NAME_PATH_PARAM = "/{" + ATTACHMENT_ID_PATH_PARAM + "}";

    private final IssueAttachmentService issueAttachmentService;

    @GET
    @Path("unload_file_from_issue" + ISSUE_ID_PATH_PARAM + FILE_NAME_PATH_PARAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public Response unloadFileFromIssue(
            @PathParam(ISSUE_ID_PATH_PARAM_NAME) final long issueId,
            @PathParam(ATTACHMENT_ID_PATH_PARAM) final long attachmentId
    ) {
        final var fileData = issueAttachmentService.getFileFromIssue(attachmentId, issueId);
        return Response.ok(fileData.getFileData())
                       .header(
                               CONTENT_DISPOSITION_HEADER_NAME,
                               format(
                                       ATTACHMENT_CONTENT_DISPOSITION_HEADER_VALUE_TEMPLATE,
                                       fileData.getFileName()
                               )
                       ).build();
    }

    @GET
    @Path("unload_files_from_issue_zipped" + ISSUE_ID_PATH_PARAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public Response unloadFileFromIssueZipped(
            @PathParam(ISSUE_ID_PATH_PARAM_NAME) final long issueId,
            @QueryParam(ZIP_FILE_NAME_QUERY_PARAM_NAME) final String zipFileName
    ) {
        final var streamingOutputDto = issueAttachmentService.getAttachmentsFromIssueAsZip(issueId, zipFileName);
        return Response.ok(streamingOutputDto.getFileData())
                       .header(
                               CONTENT_DISPOSITION_HEADER_NAME,
                               format(
                                       ATTACHMENT_CONTENT_DISPOSITION_HEADER_VALUE_TEMPLATE,
                                       streamingOutputDto.getFileName()
                               )
                       ).build();
    }


    @POST
    @Path("add_file_to_issue" + ISSUE_ID_PATH_PARAM)
    @Produces(APPLICATION_JSON)
    @MultipartConfigClass(CustomIssueFileMultipartConfig.class)
    public FileAddResponseDto addFileToIssue(
            @PathParam(ISSUE_ID_PATH_PARAM_NAME) final long issueId,
            @MultipartFormParam("file") final FilePart filePart
    ) {
        return issueAttachmentService.addFileToIssue(issueId, filePart);
    }

    public static class CustomIssueFileMultipartConfig implements MultipartConfig {

        @Override
        public long getMaxFileSize() {
            return Long.MAX_VALUE; // use your max file size
        }

        @Override
        public long getMaxSize() {
            return Long.MAX_VALUE; // use your max file size
        }
    }
}
