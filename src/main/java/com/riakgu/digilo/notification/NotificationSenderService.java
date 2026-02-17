package com.riakgu.digilo.notification;

import com.riakgu.digilo.common.service.EmailService;
import com.riakgu.digilo.common.service.WhatsAppService;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderItem;
import com.riakgu.digilo.order.dto.OrderCredentialResponse;
import com.riakgu.digilo.payment.Payment;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final Locale ID_LOCALE = Locale.of("id", "ID");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", ID_LOCALE)
                    .withZone(ZoneId.of("Asia/Jakarta"));

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final TemplateEngine templateEngine;

    // ==================== OTP ====================

    public void sendEmailOtp(String name, String email, String otp) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otpCode", otp);

        String html = templateEngine.process("email/otp-verification", context);
        emailService.sendEmail(email, "Digilo - Email Verification", html);

        log.info("Email OTP sent to {}", email);
    }

    public void sendWhatsAppOtp(String name, String phone, String otp) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otpCode", otp);

        String message = templateEngine.process("whatsapp/otp-verification.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("WhatsApp OTP sent to {}", phone);
    }

    // ==================== PASSWORD RESET ====================

    public void sendPasswordResetEmail(String name, String email, String otp) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otpCode", otp);

        String html = templateEngine.process("email/password-reset", context);
        emailService.sendEmail(email, "Digilo - Password Reset", html);

        log.info("Password reset email sent to {}", email);
    }

    public void sendPasswordResetWhatsApp(String name, String phone, String otp) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otpCode", otp);

        String message = templateEngine.process("whatsapp/password-reset.txt", context);
        whatsAppService.sendMessage(phone, message);

        log.info("Password reset WhatsApp sent to {}", phone);
    }

    // ==================== PAYMENT SUCCESS ====================

    public void sendPaymentSuccessEmail(Order order, Payment payment) {
        if (!order.getUser().canReceiveEmail()) {
            log.info("User email not verified, skipping payment success email for orderId={}", order.getId());
            return;
        }

        Context context = buildPaymentSuccessContext(order, payment);

        String html = templateEngine.process("email/payment-success", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, "Digilo - Payment Successful", html);

        log.info("Payment success email sent to {}", email);
    }

    public void sendPaymentSuccessWhatsApp(Order order, Payment payment) {
        if (!order.getUser().canReceiveWhatsApp()) {
            log.info("User phone not verified or missing, skipping payment success WhatsApp for orderId={}", order.getId());
            return;
        }

        Context context = buildPaymentSuccessContext(order, payment);

        String message = templateEngine.process("whatsapp/payment-success.txt", context);
        String phone = order.getUser().getPhone();
        whatsAppService.sendMessage(phone, message);

        log.info("Payment success WhatsApp sent to {}", phone);
    }

    // ==================== ORDER COMPLETED ====================

    public void sendOrderCompletedEmail(Order order, List<OrderCredentialResponse> credentials) {
        if (!order.getUser().canReceiveEmail()) {
            log.info("User email not verified, skipping order completed email for orderId={}", order.getId());
            return;
        }

        Context context = new Context();
        context.setVariable("name", order.getUser().getName());
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("credentials", credentials);

        String html = templateEngine.process("email/order-completed", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, "Digilo - Order Completed", html);

        log.info("Order completed email sent to {}", email);
    }

    public void sendOrderCompletedWhatsApp(Order order, List<OrderCredentialResponse> credentials) {
        if (!order.getUser().canReceiveWhatsApp()) {
            log.info("User phone not verified or missing, skipping order completed WhatsApp for orderId={}", order.getId());
            return;
        }

        Context context = new Context();
        context.setVariable("name", order.getUser().getName());
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("credentials", credentials);

        String message = templateEngine.process("whatsapp/order-completed.txt", context);
        String phone = order.getUser().getPhone();
        whatsAppService.sendMessage(phone, message);

        log.info("Order completed WhatsApp sent to {}", phone);
    }

    // ==================== ORDER CANCELLED ====================

    public void sendOrderCancelledEmail(Order order) {
        if (!order.getUser().canReceiveEmail()) {
            log.info("User email not verified, skipping order cancelled email for orderId={}", order.getId());
            return;
        }

        Context context = buildOrderContext(order);

        String html = templateEngine.process("email/order-cancelled", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, "Digilo - Order Cancelled", html);

        log.info("Order cancelled email sent to {}", email);
    }

    public void sendOrderCancelledWhatsApp(Order order) {
        if (!order.getUser().canReceiveWhatsApp()) {
            log.info("User phone not verified or missing, skipping order cancelled WhatsApp for orderId={}", order.getId());
            return;
        }

        Context context = buildOrderContext(order);

        String message = templateEngine.process("whatsapp/order-cancelled.txt", context);
        String phone = order.getUser().getPhone();
        whatsAppService.sendMessage(phone, message);

        log.info("Order cancelled WhatsApp sent to {}", phone);
    }

    // ==================== PAYMENT EXPIRED ====================

    public void sendPaymentExpiredEmail(Order order) {
        if (!order.getUser().canReceiveEmail()) {
            log.info("User email not verified, skipping payment expired email for orderId={}", order.getId());
            return;
        }

        Context context = buildOrderContext(order);

        String html = templateEngine.process("email/payment-expired", context);
        String email = order.getUser().getEmail();
        emailService.sendEmail(email, "Digilo - Payment Expired", html);

        log.info("Payment expired email sent to {}", email);
    }

    public void sendPaymentExpiredWhatsApp(Order order) {
        if (!order.getUser().canReceiveWhatsApp()) {
            log.info("User phone not verified or missing, skipping payment expired WhatsApp for orderId={}", order.getId());
            return;
        }

        Context context = buildOrderContext(order);

        String message = templateEngine.process("whatsapp/payment-expired.txt", context);
        String phone = order.getUser().getPhone();
        whatsAppService.sendMessage(phone, message);

        log.info("Payment expired WhatsApp sent to {}", phone);
    }

    // ==================== HELPERS ====================

    private Context buildPaymentSuccessContext(Order order, Payment payment) {
        Context context = new Context();
        context.setVariable("name", order.getUser().getName());
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("paymentType", payment.getPaymentType());
        context.setVariable("transactionId", payment.getProviderTransactionId());
        context.setVariable("paidAt", DATE_FORMATTER.format(payment.getPaidAt()));
        context.setVariable("items", buildOrderItemDtos(order));
        context.setVariable("subtotal", formatCurrency(order.getSubtotal()));

        if (order.getPromo() != null) {
            context.setVariable("promoCode", order.getPromo().getCode());
            context.setVariable("discountAmount", formatCurrency(order.getDiscountAmount()));
        }

        context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));
        return context;
    }

    private Context buildOrderContext(Order order) {
        Context context = new Context();
        context.setVariable("name", order.getUser().getName());
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("items", buildOrderItemDtos(order));
        context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));
        return context;
    }

    private List<Map<String, Object>> buildOrderItemDtos(Order order) {
        return order.getItems().stream()
                .map(item -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("productName", item.getProductName());
                    dto.put("variantName", item.getVariantName());
                    dto.put("quantity", item.getQuantity());
                    dto.put("subtotal", formatCurrency(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                    return dto;
                })
                .toList();
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(ID_LOCALE);
        return formatter.format(amount);
    }

}
