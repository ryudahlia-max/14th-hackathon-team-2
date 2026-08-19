package com.team2.wellness.core.routine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    List<Routine> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Routine> findAllByOwnerIdInAndActiveTrue(List<UUID> ownerIds);

    List<Routine> findAllByActiveTrue();

    Optional<Routine> findByIdAndOwnerId(UUID id, UUID ownerId);
}
