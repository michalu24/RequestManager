package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.infrastructure.persistence.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RequestJpaRepository extends JpaRepository<RequestEntity, UUID> {
}
