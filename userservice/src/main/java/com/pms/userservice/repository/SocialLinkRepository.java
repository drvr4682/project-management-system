package com.pms.userservice.repository;

import com.pms.userservice.entity.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SocialLinkRepository extends JpaRepository<SocialLink, UUID> {

    List<SocialLink> findByProfileId(UUID profileId);

    void deleteByProfileId(UUID profileId);
}
