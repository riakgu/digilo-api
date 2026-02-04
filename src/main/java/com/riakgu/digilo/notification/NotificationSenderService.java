package com.riakgu.digilo.notification;

import com.riakgu.digilo.common.service.EmailService;
import com.riakgu.digilo.common.service.WhatsAppService;
import com.riakgu.digilo.config.SiteProperties;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.payment.Payment;
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
        context.setVariable("siteName", siteProperties.getName());
        context.setVariable("companyName", siteProperties.getCompanyName());
        context.setVariable("supportUrl", siteProperties.getSupportUrl());

        String html = templateEngine.process("email/otp-verification", context);
        emailService.sendEmail(email, siteProperties.getName() + " - Email Verification", html);

        log.info("Email OTP sent to {}", email);
    }

    public void sendWhatsAppOtp(String phone, String otp) {
        Context context = new Context();
        context.setVariable("otpCode", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);
        context.setVariable("siteName", siteProperties.getName());
        context.setVariable("supportUrl", siteProperties.getSupportUrl());

        String message = templateEngine.process("whatsapp/otp-verification.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("WhatsApp OTP sent to {}", phone);
    }

    public void sendPasswordResetEmail(String email, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);

        String html = templateEngine.process("email/password-reset", context);
        emailService.sendEmail(email, "Digilo - Password Reset", html);

        log.info("Password reset email sent to {}", email);
    }

    public void sendPasswordResetWhatsApp(String phone, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);

        String message = templateEngine.process("whatsapp/password-reset.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("Password reset WhatsApp sent to {}", phone);
    }

    public void sendPaymentSuccessEmail(Order order, Payment payment) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("payment", payment);
        context.setVariable("siteName", siteProperties.getName());
        context.setVariable("companyName", siteProperties.getCompanyName());
        context.setVariable("orderUrl", siteProperties.getFrontendUrl() + "/account/orders/" + order.getId());
        context.setVariable("supportUrl", siteProperties.getSupportUrl());

        String html = templateEngine.process("email/payment-success", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, siteProperties.getName() + " - Payment Successful", html);

        log.info("Payment success email sent to {}", email);
    }

    public void sendPaymentSuccessWhatsApp(Order order, Payment payment) {
        String phone = order.getUser().getPhone();
        if (phone == null || phone.isBlank()) {
            log.info("User has no phone number, skipping WhatsApp notification");
            return;
        }

        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("payment", payment);
        context.setVariable("siteName", siteProperties.getName());
        context.setVariable("orderUrl", siteProperties.getFrontendUrl() + "/account/orders/" + order.getId());
        context.setVariable("supportUrl", siteProperties.getSupportUrl());

        String message = templateEngine.process("whatsapp/payment-success.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("Payment success WhatsApp sent to {}", phone);
    }

}
