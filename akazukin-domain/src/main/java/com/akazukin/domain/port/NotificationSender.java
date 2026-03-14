package com.akazukin.domain.port;

import com.akazukin.domain.model.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
