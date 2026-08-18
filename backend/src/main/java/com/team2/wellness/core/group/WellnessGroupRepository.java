package com.team2.wellness.core.group;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WellnessGroupRepository extends JpaRepository<WellnessGroup, UUID> {
}
