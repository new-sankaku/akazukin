package com.akazukin.web.i18n;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message
    String nav_dashboard();

    @Message
    String nav_analytics();

    @Message
    String nav_posts();

    @Message
    String nav_compose();

    @Message
    String nav_accounts();

    @Message
    String nav_theme();

    @Message
    String nav_logout();

    @Message
    String app_title();

    @Message
    String app_subtitle();

    @Message
    String login_title();

    @Message
    String login_username();

    @Message
    String login_username_placeholder();

    @Message
    String login_password();

    @Message
    String login_password_placeholder();

    @Message
    String login_submit();

    @Message
    String login_no_account();

    @Message
    String login_register_link();

    @Message
    String register_title();

    @Message
    String register_subtitle();

    @Message
    String register_email();

    @Message
    String register_email_placeholder();

    @Message
    String register_confirm_password();

    @Message
    String register_confirm_password_placeholder();

    @Message
    String register_submit();

    @Message
    String register_has_account();

    @Message
    String register_login_link();

    @Message
    String error_network();

    @Message
    String error_login_failed();

    @Message
    String error_registration_failed();

    @Message
    String error_passwords_mismatch();

    @Message
    String error_not_found();

    @Message
    String error_load_failed();
}
