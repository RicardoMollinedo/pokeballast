package com.ballastlane.pokeproject.repository.adapter;

import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.model.Task;
import com.ballastlane.pokeproject.bl.model.TaskStatus;
import com.ballastlane.pokeproject.bl.port.TaskPort;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.TaskMapper;
import com.ballastlane.pokeproject.repository.entity.TaskEntity;
import com.ballastlane.pokeproject.repository.jpa.TaskJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAdapter implements TaskPort {

    private final TaskJpaRepository jpa;
    private final TaskMapper mapper;

    @Override
    public Optional<Task> findByIdAndOwner(long id, long ownerId) {
        log.info("[DB] Finding task - id={}, ownerId={}", id, ownerId);
        Optional<Task> task = jpa.findByIdAndOwner(id, ownerId).map(mapper::toModel);
        log.info("[DB] Find task returned - id={}, found={}", id, task.isPresent());
        return task;
    }

    @Override
    public PageResult<Task> findByOwner(long ownerId, TaskStatus status, PageQuery pageQuery) {
        log.info("[DB] Listing tasks - ownerId={}, status={}, page={}, size={}", ownerId, status, pageQuery.page(), pageQuery.size());
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size());
        Page<TaskEntity> page = status == null ? jpa.findByOwner(ownerId, pageable)
                : jpa.findByOwnerAndStatus(ownerId, status, pageable);
        log.info("[DB] List tasks returned - total={}, ids={}", page.getTotalElements(),
                page.getContent().stream().map(TaskEntity::getId).toList());
        return new PageResult<>(page.getContent().stream().map(mapper::toModel).toList(),
                pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    @Override
    @Transactional
    public Task save(Task task) {
        log.info("[DB] Saving task - id={}, ownerId={}, version={}", task.id(), task.ownerId(), task.version());
        TaskEntity entity;
        if (task.id() == null) {
            entity = TaskEntity.create();
        } else {
            entity = jpa.findByIdAndOwner(task.id(), task.ownerId())
                    .orElseThrow(() -> new BallastException(BallastErrorCode.TASK_NOT_FOUND));
            if (!Objects.equals(entity.getVersion(), task.version())) {
                throw new BallastException(BallastErrorCode.STALE_VERSION);
            }
        }
        mapper.copyToEntity(task, entity);
        try {
            TaskEntity saved = jpa.saveAndFlush(entity);
            log.info("[DB] Save task returned - id={}, version={}", saved.getId(), saved.getVersion());
            return mapper.toModel(saved);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BallastException(BallastErrorCode.STALE_VERSION);
        }
    }

    @Override
    @Transactional
    public boolean deleteByIdAndOwner(long id, long ownerId) {
        log.info("[DB] Deleting task - id={}, ownerId={}", id, ownerId);
        int deleted = jpa.deleteByIdAndOwner(id, ownerId);
        log.info("[DB] Delete task returned - id={}, deleted={}", id, deleted);
        return deleted == 1;
    }
}
