package ru.jira.file.work.rest.plugin.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonProperty;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class FileAddResponseDto {
    @JsonProperty
    private String attachmentName;
    @JsonProperty
    private String attachmentLink;
}
