package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "ai_task_provider_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "task_type"}))
public class AiTaskProviderSettingEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "task_type", nullable = false, length = 30)
    public String taskType;

    @Column(nullable = false, length = 30)
    public String provider;
}
