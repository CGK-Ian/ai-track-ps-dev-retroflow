package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * STUB — left behind by the previous vendor.
 *
 * Extends JpaRepository for basic CRUD out of the box; add derived query
 * methods here as the service layer needs them (e.g. finding the current
 * OPEN retrospective for a team).
 */
public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

	boolean existsByTeam_IdAndStatus(Long teamId, Retrospective.Status status);

	Page<Retrospective> findByTeam_Id(Long teamId, Pageable pageable);

	@Query("select r from Retrospective r where (:id is null or r.id = :id)"
		+ " and lower(r.title) like :titlePattern escape '\\'"
		+ " and (:status is null or r.status = :status)")
	Page<Retrospective> search(@Param("id") Long id,
							   @Param("titlePattern") String titlePattern,
							   @Param("status") Retrospective.Status status,
							   Pageable pageable);
}
