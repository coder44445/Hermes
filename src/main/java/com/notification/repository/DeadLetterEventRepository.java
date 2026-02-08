package com.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.notification.event.DeadLetterEvent;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    Optional<DeadLetterEvent> findByEventKey(String key);
}
