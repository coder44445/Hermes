package com.notification.repository;

import com.notification.event.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    Optional<DeadLetterEvent> findByEventKey(String key);
}
