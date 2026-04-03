package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.BulkUploadSession;
import com.InvitationSystem.InvitationSystem.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkUploadSessionRepository extends JpaRepository<BulkUploadSession, UUID> {
    
    List<BulkUploadSession> findByEventId(UUID eventId);
    
    List<BulkUploadSession> findByUploadedBy(UUID uploadedBy);
    
    List<BulkUploadSession> findByStatus(UploadStatus status);
    
    List<BulkUploadSession> findByEventIdOrderByUploadedAtDesc(UUID eventId);
    
    List<BulkUploadSession> findByUploadedByOrderByUploadedAtDesc(UUID uploadedBy);
}
