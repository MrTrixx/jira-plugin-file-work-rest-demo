package ru.jira.file.work.rest.plugin.demo.service;

import lombok.RequiredArgsConstructor;
import ru.jira.file.work.rest.plugin.demo.dto.FileData;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor(staticName = "of")
public class StreamingOutputZipFileCreator implements StreamingOutput {
    private final List<FileData<byte[]>> attachmentsAsBytes;

    @Override
    public void write(final OutputStream outputStream) throws IOException, WebApplicationException {
        Map<String, Integer> duplicateFileNames = new HashMap<>();
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final var zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (final var data : attachmentsAsBytes) {
                Integer count = duplicateFileNames.get(data.getFileName());
                String fileNameInZip;
                // dummy resolve duplication
                if (count != null) {
                    count = count + 1;
                    duplicateFileNames.put(data.getFileName(), count);
                    String[] splitName = data.getFileName().split("\\.");
                    if (splitName.length == 1) {
                        fileNameInZip = data.getFileName();
                    } else if (splitName.length == 2) {
                        fileNameInZip = splitName[0] + "_" + count + "." + splitName[1];
                    } else {
                        int lengthOfLastElementInSplitFileName = splitName[splitName.length - 1].length();
                        String fileNameWithoutExtension = data.getFileName().substring(0, data.getFileName().length() - lengthOfLastElementInSplitFileName - 1);
                        fileNameInZip = fileNameWithoutExtension + "_" + count + "." + splitName[splitName.length - 1];
                    }
                } else {
                    duplicateFileNames.put(data.getFileName(), 0);
                    fileNameInZip = data.getFileName();
                }
                ZipEntry zipEntry = new ZipEntry(fileNameInZip);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(data.getFileData());
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            zipOutputStream.flush();
            outputStream.write(byteArrayOutputStream.toByteArray());
        }
    }
}
