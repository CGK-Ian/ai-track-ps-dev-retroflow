package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.ActionItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    List<ActionItem> findAllByRetrospective_Id(Long retrospectiveId);

    Optional<ActionItem> findByFeedbackItemId(Long feedbackItemId);
}