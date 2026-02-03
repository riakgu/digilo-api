package com.riakgu.digilo.notification;

import com.riakgu.digilo.common.service.EmailService;
import com.riakgu.digilo.common.service.WhatsAppService;
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

    public void sendEmailOtp(String email, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);

        String html = templateEngine.process("email/otp-verification", context);
        emailService.sendEmail(email, "Digilo - Email Verification", html);

        log.info("Email OTP sent to {}", email);
    }

    public void sendWhatsAppOtp(String phone, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("expiryMinutes", OTP_EXPIRY_MINUTES);

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

    public void sendOrderConfirmationEmail(String email, String orderNumber, String totalAmount) {
        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("totalAmount", totalAmount);

        String html = templateEngine.process("email/order-confirmation", context);
        emailService.sendEmail(email, "Digilo - Order Confirmation", html);

        log.info("Order confirmation email sent to {}", email);
    }

    public void sendOrderConfirmationWhatsApp(String phone, String orderNumber, String totalAmount) {
        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("totalAmount", totalAmount);

        String message = templateEngine.process("whatsapp/order-confirmation.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("Order confirmation WhatsApp sent to {}", phone);
    }

    public void sendPaymentSuccessEmail(String email, String orderNumber) {
        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);

        String html = templateEngine.process("email/payment-success", context);
        emailService.sendEmail(email, "Digilo - Payment Successful", html);

        log.info("Payment success email sent to {}", email);
    }

    public void sendPaymentSuccessWhatsApp(String phone, String orderNumber) {
        Context context = new Context();
        context.setVariable("orderNumber", orderNumber);

        String message = templateEngine.process("whatsapp/payment-success.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("Payment success WhatsApp sent to {}", phone);
    }
}
