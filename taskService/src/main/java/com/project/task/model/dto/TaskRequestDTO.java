package com.project.task.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    @JsonProperty("name")
    @NotBlank(message = "Task name is required")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

}
