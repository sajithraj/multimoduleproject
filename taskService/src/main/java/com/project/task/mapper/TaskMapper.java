package com.project.task.mapper;

import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import com.project.task.model.dto.TaskResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "default",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "createdAt", expression = "java(System.currentTimeMillis())")
    @Mapping(target = "updatedAt", expression = "java(System.currentTimeMillis())")
    Task toEntity(TaskRequestDTO dto);


    @Mapping(target = "status", expression = "java(task.getStatus() != null ? task.getStatus().name() : null)")
    TaskResponseDTO toResponseDTO(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(TaskRequestDTO dto, @MappingTarget Task task);

}
