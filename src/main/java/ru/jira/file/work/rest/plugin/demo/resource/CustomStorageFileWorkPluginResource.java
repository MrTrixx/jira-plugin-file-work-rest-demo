package ru.jira.file.work.rest.plugin.demo.resource;

import com.atlassian.plugins.rest.common.multipart.FilePart;
import com.atlassian.plugins.rest.common.multipart.MultipartConfig;
import com.atlassian.plugins.rest.common.multipart.MultipartConfigClass;
import com.atlassian.plugins.rest.common.multipart.MultipartFormParam;
import lombok.RequiredArgsConstructor;
import ru.jira.file.work.rest.plugin.demo.dto.CustomStoredFileData;
import ru.jira.file.work.rest.plugin.demo.dto.FileDataRequestByIds;
import ru.jira.file.work.rest.plugin.demo.service.CustomFileStorageService;

import javax.ws.rs.Consumes;
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
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static ru.jira.file.work.rest.plugin.demo.util.Const.ATTACHMENT_CONTENT_DISPOSITION_HEADER_VALUE_TEMPLATE;
import static ru.jira.file.work.rest.plugin.demo.util.Const.CONTENT_DISPOSITION_HEADER_NAME;
import static ru.jira.file.work.rest.plugin.demo.util.Const.ZIP_FILE_NAME_QUERY_PARAM_NAME;

@Path("custom_storage_file_resource")
@RequiredArgsConstructor
public class CustomStorageFileWorkPluginResource {
    public static final String ID_PATH_PARAM_NAME = "id";
    public static final String ID_PATH_PARAM = "/{" + ID_PATH_PARAM_NAME + "}";

    private final CustomFileStorageService customFileStorageService;

    @POST
    @Path("load_file")
    @Produces(APPLICATION_JSON)
    @Consumes(MULTIPART_FORM_DATA)
    @MultipartConfigClass(CustomFileMultipartConfig.class)
    public CustomStoredFileData customStoredFileData(@MultipartFormParam("file") final FilePart filePart) {
        return customFileStorageService.saveFile(filePart);
    }

    @GET
    @Path("get_file_by_id" + ID_PATH_PARAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public Response getFileByIdFromCustomStorage(@PathParam(ID_PATH_PARAM_NAME) final int id) {
        final var fileData = customFileStorageService.getFileById(id);
        return Response.ok(fileData.getFileData()).header(
                CONTENT_DISPOSITION_HEADER_NAME,
                format(
                        ATTACHMENT_CONTENT_DISPOSITION_HEADER_VALUE_TEMPLATE,
                        fileData.getFileName()
                )
        ).build();
    }

    @GET
    @Path("get_files_by_ids")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_OCTET_STREAM)
    public Response getFileByIdsFromCustomStorageAsZip(
            final FileDataRequestByIds fileDataRequestByIds,
            @QueryParam(ZIP_FILE_NAME_QUERY_PARAM_NAME) final String zipFileName) {
        final var fileData = customFileStorageService.getFilesByIdsInZipFormat(fileDataRequestByIds, zipFileName);
        return Response.ok(fileData.getFileData()).header(
                CONTENT_DISPOSITION_HEADER_NAME,
                format(
                        ATTACHMENT_CONTENT_DISPOSITION_HEADER_VALUE_TEMPLATE,
                        fileData.getFileName()
                )
        ).build();
    }

    public static class CustomFileMultipartConfig implements MultipartConfig {

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
