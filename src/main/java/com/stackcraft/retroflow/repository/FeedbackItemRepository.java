package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * STUB — left behind by the previous vendor.
 *
 * Extends JpaRepository for basic CRUD out of the box; add derived query
 * methods here as the service layer needs them (e.g. finding items by
 * retrospective, or filtering action items by priority/completed status).
 */
public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, Long> {
    @Query("select f from FeedbackItem f where f.retrospective.id = :retrospectiveId"
        + " and (:actionItemsOnly = false or f.type = 'ACTION_ITEM')"
        + " and (:priority is null or f.priority = :priority)"
        + " and (:completed is null or f.completed = :completed)")
    Page<FeedbackItem> search(@Param("retrospectiveId") Long retrospectiveId,
                              @Param("actionItemsOnly") boolean actionItemsOnly,
                              @Param("priority") ActionPriority priority,
                              @Param("completed") Boolean completed,
                              Pageable pageable);
}
