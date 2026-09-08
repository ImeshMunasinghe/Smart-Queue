package com.smartqueue.repository;

import com.smartqueue.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, UUID> {
    List<ServiceType> findByOfficeIdAndActiveTrue(UUID officeId);
    Optional<ServiceType> findByOfficeIdAndCode(UUID officeId, String code);
}
