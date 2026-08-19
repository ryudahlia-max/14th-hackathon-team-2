package com.team2.wellness.core.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    List<GroupMember> findAllByUserId(UUID userId);

    List<GroupMember> findAllByGroupId(UUID groupId);

    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByGroupId(UUID groupId);

    @Query("""
            select count(firstMember) > 0 from GroupMember firstMember
            where firstMember.userId = :firstUserId
              and exists (
                select secondMember.id from GroupMember secondMember
                where secondMember.groupId = firstMember.groupId
                  and secondMember.userId = :secondUserId
              )
            """)
    boolean existsSharedGroup(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );
}
