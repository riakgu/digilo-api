package com.riakgu.digilo.notification;

import com.riakgu.digilo.common.service.EmailService;
import com.riakgu.digilo.common.service.WhatsAppService;
import com.riakgu.digilo.config.SiteProperties;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.dto.OrderCredentialResponse;
import com.riakgu.digilo.payment.Payment;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSenderService {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final TemplateEngine templateEngine;
    private final SiteProperties siteProperties;

    public void sendEmailOtp(String email, String otp) {
        Context context = new Context();
        context.setVariable("otpCode", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("companyName", siteProperties.companyName());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String html = templateEngine.process("email/otp-verification", context);
        emailService.sendEmail(email, siteProperties.name() + " - Email Verification", html);

        log.info("Email OTP sent to {}", email);
    }

    public void sendWhatsAppOtp(String phone, String otp) {
        Context context = new Context();
        context.setVariable("otpCode", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String message = templateEngine.process("whatsapp/otp-verification.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("WhatsApp OTP sent to {}", phone);
    }

    public void sendPasswordResetEmail(String email, String otp) {
        Context context = new Context();
        context.setVariable("otpCode", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("companyName", siteProperties.companyName());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String html = templateEngine.process("email/password-reset", context);
        emailService.sendEmail(email, siteProperties.name() + " - Password Reset", html);

        log.info("Password reset email sent to {}", email);
    }

    public void sendPasswordResetWhatsApp(String phone, String otp) {
        Context context = new Context();
        context.setVariable("otpCode", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String message = templateEngine.process("whatsapp/password-reset.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("Password reset WhatsApp sent to {}", phone);
    }

    public void sendPaymentSuccessEmail(Order order, Payment payment) {
        if (!order.getUser().canReceiveEmail()) {
            log.info("User email not verified, skipping payment success email for orderId={}", order.getId());
            return;
        }

        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("payment", payment);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("companyName", siteProperties.companyName());
        context.setVariable("orderUrl", siteProperties.frontendUrl() + "/account/orders/" + order.getId());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String html = templateEngine.process("email/payment-success", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, siteProperties.name() + " - Payment Successful", html);

        log.info("Payment success email sent to {}", email);
    }

    public void sendPaymentSuccessWhatsApp(Order order, Payment payment) {
        if (!order.getUser().canReceiveWhatsApp()) {
            log.info("User phone not verified or missing, skipping payment success WhatsApp for orderId={}", order.getId());
            return;
        }

        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("payment", payment);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("orderUrl", siteProperties.frontendUrl() + "/account/orders/" + order.getId());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String message = templateEngine.process("whatsapp/payment-success.txt", context);
        String phone = order.getUser().getPhone();
        whatsAppService.sendMessage(phone, message);

        log.info("Payment success WhatsApp sent to {}", phone);
    }

    public void sendCredentialsDeliveryEmail(Order order, List<OrderCredentialResponse> credentials) {
        if (!order.getUser().canReceiveEmail()) {
            log.info("User email not verified, skipping credentials delivery email for orderId={}", order.getId());
            return;
        }

        Context context = new Context();
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("credentials", credentials);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("companyName", siteProperties.companyName());
        context.setVariable("orderUrl", siteProperties.frontendUrl() + "/account/orders/" + order.getId());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String html = templateEngine.process("email/credentials-delivery", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, siteProperties.name() + " - Your Product Credentials", html);

        log.info("Credentials delivery email sent to {}", email);
    }

    public void sendCredentialsDeliveryWhatsApp(Order order, List<OrderCredentialResponse> credentials) {
        if (!order.getUser().canReceiveWhatsApp()) {
            log.info("User phone not verified or missing, skipping credentials delivery WhatsApp for orderId={}", order.getId());
            return;
        }

        Context context = new Context();
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("credentials", credentials);
        context.setVariable("siteName", siteProperties.name());
        context.setVariable("orderUrl", siteProperties.frontendUrl() + "/account/orders/" + order.getId());
        context.setVariable("supportUrl", siteProperties.supportUrl());

        String message = templateEngine.process("whatsapp/credentials-delivery.txt", context);
        String phone = order.getUser().getPhone();
        whatsAppService.sendMessage(phone, message);

        log.info("Credentials delivery WhatsApp sent to {}", phone);
    }

}
