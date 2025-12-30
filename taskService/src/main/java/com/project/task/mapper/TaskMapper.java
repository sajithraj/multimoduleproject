package com.project.task.mapper;

import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import com.project.task.model.dto.TaskResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct Mapper for Task entity and DTOs
 * Production-grade mapper with proper configuration
 */
@Mapper(
        componentModel = "default",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);

    /**
     * Convert TaskRequestDTO to Task entity
     * Ignores id, createdAt, updatedAt (auto-generated)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(TaskRequestDTO dto);

    /**
     * Convert Task entity to TaskResponseDTO
     * Maps status enum to string
     */
    @Mapping(target = "status", expression = "java(task.getStatus() != null ? task.getStatus().name() : null)")
    TaskResponseDTO toResponseDTO(Task task);

    /**
     * Update existing Task entity from TaskRequestDTO
     * Used for PUT operations
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(TaskRequestDTO dto, @MappingTarget Task task);
}

