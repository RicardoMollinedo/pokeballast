package com.ballastlane.pokeproject.repository.jpa;

import com.ballastlane.pokeproject.bl.model.TaskStatus;
import com.ballastlane.pokeproject.repository.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Every query includes the owner: ownership is enforced by the database query, not in memory. */
public interface TaskJpaRepository extends JpaRepository<TaskEntity, Long> {

    @Query("select t from TaskEntity t where t.id = :id and t.ownerId = :ownerId")
    Optional<TaskEntity> findByIdAndOwner(@Param("id") long id, @Param("ownerId") long ownerId);

    @Query(value = """
            select t from TaskEntity t where t.ownerId = :ownerId
            order by case when t.dueDate is null then 1 else 0 end, t.dueDate, t.id""",
            countQuery = "select count(t) from TaskEntity t where t.ownerId = :ownerId")
    Page<TaskEntity> findByOwner(@Param("ownerId") long ownerId, Pageable pageable);

    // two queries instead of ":status is null or ...": that pattern fails on PostgreSQL with null parameters
    @Query(value = """
            select t from TaskEntity t where t.ownerId = :ownerId and t.status = :status
            order by case when t.dueDate is null then 1 else 0 end, t.dueDate, t.id""",
            countQuery = "select count(t) from TaskEntity t where t.ownerId = :ownerId and t.status = :status")
    Page<TaskEntity> findByOwnerAndStatus(@Param("ownerId") long ownerId, @Param("status") TaskStatus status,
                                          Pageable pageable);

    @Modifying
    @Query("delete from TaskEntity t where t.id = :id and t.ownerId = :ownerId")
    int deleteByIdAndOwner(@Param("id") long id, @Param("ownerId") long ownerId);
}
