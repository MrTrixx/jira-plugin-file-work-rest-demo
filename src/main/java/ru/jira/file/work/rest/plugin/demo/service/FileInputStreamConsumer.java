package ru.jira.file.work.rest.plugin.demo.service;

import com.atlassian.jira.util.io.InputStreamConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import ru.jira.file.work.rest.plugin.demo.dto.FileData;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class FileInputStreamConsumer implements InputStreamConsumer<FileData<byte[]>> {
    private final String filename;

    @Override
    public FileData<byte[]> withInputStream(final InputStream inputStream) throws IOException {
        log.debug("Start read file {}", filename);
        final var attachmentFileBytesData = IOUtils.toByteArray(inputStream);
        final var attachmentData = new FileData<>(attachmentFileBytesData, filename);
        log.info("Successfully read file {}", filename);
        return attachmentData;
    }
}
