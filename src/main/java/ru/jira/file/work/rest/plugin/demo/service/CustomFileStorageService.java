package ru.jira.file.work.rest.plugin.demo.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.jira.issue.attachment.AttachmentReadException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.rest.common.multipart.FilePart;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import ru.jira.file.work.rest.plugin.demo.dto.CustomStoredFileData;
import ru.jira.file.work.rest.plugin.demo.dto.CustomStoredFilesData;
import ru.jira.file.work.rest.plugin.demo.dto.FileData;
import ru.jira.file.work.rest.plugin.demo.dto.FileDataRequestByIds;
import ru.jira.file.work.rest.plugin.demo.entity.FileDataEntity;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ru.jira.file.work.rest.plugin.demo.entity.FileDataEntity.CREATOR_COLUMN;
import static ru.jira.file.work.rest.plugin.demo.entity.FileDataEntity.DATA_COLUMN;
import static ru.jira.file.work.rest.plugin.demo.entity.FileDataEntity.FILE_NAME_COLUMN;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomFileStorageService {
    private static final String DEFAULT_ZIP_NAME_TEMPLATE = "files_%s_%s.zip";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ActiveObjects activeObjects;
    private final Clock clock;

    public CustomStoredFileData saveFile(final FilePart filePart) {
        final var loggedInUserName = getLoggedInUserNameOrThrowSecurityException();
        final var fileDataEntity = mapFilePartToEntity(filePart, loggedInUserName);
        return new CustomStoredFileData(fileDataEntity.getID(), filePart.getName());
    }

    public CustomStoredFilesData saveFiles(final Collection<FilePart> fileParts) {
        final var loggedInUserName = getLoggedInUserNameOrThrowSecurityException();
        List<CustomStoredFileData> data = new ArrayList<>();
        for (FilePart filePart : fileParts) {
            final var fileDataEntity = mapFilePartToEntity(filePart, loggedInUserName);
            data.add(new CustomStoredFileData(fileDataEntity.getID(), filePart.getName()));
        }
        return new CustomStoredFilesData(data);
    }

    private FileDataEntity mapFilePartToEntity(final FilePart filePart, final String loggedInUserName) {
        final var bytes = readBytes(filePart);
        final var fileDataEntity = activeObjects.create(
                FileDataEntity.class,
                new DBParam(FILE_NAME_COLUMN, filePart.getName()),
                new DBParam(DATA_COLUMN, bytes),
                new DBParam(CREATOR_COLUMN, loggedInUserName)
        );
        return fileDataEntity;
    }

    private byte[] readBytes(final FilePart filePart) {
        try {
            return IOUtils.toByteArray(filePart.getInputStream());
        } catch (IOException e) {
            log.error("Error happened during reading file {}", filePart.getName());
            throw new AttachmentReadException(e);
        }
    }

    public FileData<byte[]> getFileById(final int id) {
        return Arrays.stream(findFileData("ID = ?", id))
                     .map(entity -> new FileData<>(entity.getData(), entity.getFileName()))
                     .findFirst()
                     .orElseThrow(() -> new NotFoundException(format("File not found by %s", id)));
    }

    public FileData<StreamingOutput> getFilesByIdsInZipFormat(
            final FileDataRequestByIds fileDataRequestByIds,
            final String zipFileName) {
        final var loggedInUserName = getLoggedInUserNameOrThrowSecurityException();
        final var ids = fileDataRequestByIds.getIds();
        if (isEmpty(ids)) {
            throw new IllegalArgumentException("Array of ids cannot be empty");
        }

        final var fileData = findFilesByIds(ids);
        if (fileData.isEmpty()) {
            throw new NotFoundException(format("Files not found by ids %s", ids));
        }

        final var resolveZipFileName = resolveZipFileName(zipFileName, loggedInUserName);
        return new FileData<>(StreamingOutputZipFileCreator.of(fileData), resolveZipFileName);
    }

    private List<FileData<byte[]>> findFilesByIds(final List<Integer> ids) {
        final var placeholderCommaList = Joiner.on(", ")
                                               .join(Iterables.transform(ids, Functions.constant("?")));
        Object[] matchValuesArray = Iterables.toArray(ids, Object.class);

        return Arrays.stream(activeObjects.find(
                             FileDataEntity.class,
                             "ID",
                             Query.select().where(format("ID IN (%s)", placeholderCommaList), matchValuesArray)))
                     .map(entity -> new FileData<>(entity.getData(), entity.getFileName()))
                     .collect(Collectors.toUnmodifiableList());
    }

    private String getLoggedInUserNameOrThrowSecurityException() {
        return Optional.ofNullable(jiraAuthenticationContext.getLoggedInUser())
                       .map(ApplicationUser::getUsername)
                       .orElseThrow(SecurityException::new);
    }

    private FileDataEntity[] findFileData(final String query, Object queryArgs) {
        return activeObjects.find(
                FileDataEntity.class,
                Query.select()
                     .where(query, queryArgs));
    }


    private String resolveZipFileName(final String zipFileName, final String loggedInUserName) {
        if (isBlank(zipFileName)) {
            return format(DEFAULT_ZIP_NAME_TEMPLATE, loggedInUserName, DATE_TIME_FORMATTER.format(LocalDate.now(clock)));
        } else {
            return zipFileName;
        }
    }
}
