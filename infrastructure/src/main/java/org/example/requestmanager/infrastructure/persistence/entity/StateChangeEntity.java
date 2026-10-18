package org.example.requestmanager.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestState;

import java.time.Instant;

@Entity
@Table(name = "state_change")
public class StateChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 20)
    private RequestState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 20)
    private RequestState toState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestAction action;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StateChangeEntity() {
    }

    public StateChangeEntity(RequestState fromState,
                             RequestState toState,
                             RequestAction action,
                             String reason,
                             Instant occurredAt) {
        this.fromState = fromState;
        this.toState = toState;
        this.action = action;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public RequestState getFromState() {
        return fromState;
    }

    public RequestState getToState() {
        return toState;
    }

    public RequestAction getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
