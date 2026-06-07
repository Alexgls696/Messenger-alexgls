package com.alexgls.springboot.notificationsservice.client;

import com.alexgls.springboot.notificationsservice.dto.ServiceLoginResponse;

public interface AuthServiceClient {
    ServiceLoginResponse getServiceAccessToken();
}
