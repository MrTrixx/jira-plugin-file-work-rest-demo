package ru.jira.file.work.rest.plugin.demo.entity;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

@Table(value = "file_data")
public interface FileDataEntity extends Entity {
    String FILE_NAME_COLUMN = "FILE_NAME";
    String DATA_COLUMN = "DATA";

    String CREATOR_COLUMN = "CREATOR";

    @NotNull
    String getFileName();
    void setFileName(String fileName);

    @NotNull
    byte[] getData();
    void setData(byte[] data);

    @NotNull
    String getCreator();
    void setCreator(String creator);
}
