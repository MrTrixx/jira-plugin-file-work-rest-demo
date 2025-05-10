package ru.jira.file.work.rest.plugin.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class FilesAddResponseDto {
    private List<FileAddResponseDto> data;
}
