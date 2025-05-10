package ru.jira.file.work.rest.plugin.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileData<T> {
    private T fileData;
    private String fileName;
}
