package ru.jira.file.work.rest.plugin.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonProperty;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomStoredFileData {
    @JsonProperty
    private int id;
    @JsonProperty
    private String name;
}
