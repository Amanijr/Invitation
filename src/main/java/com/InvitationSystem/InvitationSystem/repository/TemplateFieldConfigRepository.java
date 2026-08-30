package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.FieldType;
import com.InvitationSystem.InvitationSystem.entity.TemplateFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateFieldConfigRepository extends JpaRepository<TemplateFieldConfig, UUID> {

    List<TemplateFieldConfig> findByTemplateId(UUID templateId);

    Optional<TemplateFieldConfig> findByTemplateIdAndFieldType(UUID templateId, FieldType fieldType);

    void deleteByTemplateId(UUID templateId);

    void deleteByTemplateIdAndId(UUID templateId, UUID id);
}
