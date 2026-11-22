package com.playtomic.tests.wallet.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class Entity {

    @Id
    protected String id = UUID.randomUUID().toString();

    protected Instant createdAt;

    protected Instant updatedAt;

    protected String createdBy;

    protected String updatedBy;

    @Version
    protected Long version;
}
