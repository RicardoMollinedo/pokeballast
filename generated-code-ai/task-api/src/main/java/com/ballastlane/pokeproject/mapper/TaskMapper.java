package com.ballastlane.pokeproject.mapper;

import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.model.Task;
import com.ballastlane.pokeproject.bl.model.TaskContent;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.TaskResponse;
import com.ballastlane.pokeproject.repository.entity.TaskEntity;
import org.springframework.stereotype.Component;

/** Task: entity ↔ model ↔ response DTO. */
@Component
public class TaskMapper {

    public Task toModel(TaskEntity entity) {
        return new Task(entity.getId(), entity.getOwnerId(),
                new TaskContent(entity.getTitle(), entity.getDescription(), entity.getDueDate()),
                entity.getStatus(), entity.getVersion() == null ? 0 : entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /** Copies everything except id and version, which JPA manages. */
    public void copyToEntity(Task task, TaskEntity entity) {
        entity.setOwnerId(task.ownerId());
        entity.setTitle(task.content().title());
        entity.setDescription(task.content().description());
        entity.setDueDate(task.content().dueDate());
        entity.setStatus(task.status());
        entity.setCreatedAt(task.createdAt());
        entity.setUpdatedAt(task.updatedAt());
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(task.id(), task.content().title(), task.content().description(), task.status().name(),
                task.content().dueDate(), task.version(), task.createdAt(), task.updatedAt());
    }

    public PageResponse<TaskResponse> toPage(PageResult<Task> page) {
        return new PageResponse<>(page.items().stream().map(this::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
