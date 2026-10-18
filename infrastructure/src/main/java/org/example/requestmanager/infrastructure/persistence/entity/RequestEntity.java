package org.example.requestmanager.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.example.requestmanager.domain.model.RequestState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "request")
public class RequestEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestState state;

    @Column(name = "publication_number", unique = true)
    private Long publicationNumber;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "request_id", nullable = false)
    @OrderBy("id")
    private List<StateChangeEntity> history = new ArrayList<>();

    protected RequestEntity() {
    }

    public RequestEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public Long getPublicationNumber() {
        return publicationNumber;
    }

    public void setPublicationNumber(Long publicationNumber) {
        this.publicationNumber = publicationNumber;
    }

    public List<StateChangeEntity> getHistory() {
        return history;
    }
}
