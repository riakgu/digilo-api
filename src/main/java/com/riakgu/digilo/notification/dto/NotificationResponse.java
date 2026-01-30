package com.riakgu.digilo.notification.dto;

import com.riakgu.digilo.notification.Notification;
import com.riakgu.digilo.notification.NotificationType;
import com.riakgu.digilo.notification.ReferenceType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private ReferenceType referenceType;
    private Long referenceId;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
