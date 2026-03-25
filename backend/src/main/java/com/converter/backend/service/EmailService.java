package com.converter.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @Async
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            log.info("Envoi d'email de vérification à: {}", toEmail);
            
            String subject = "Vérification de votre compte ZenithConvert";
            String body = "Bonjour,\n\n" +
                    "Merci de vous être inscrit sur ZenithConvert. " +
                    "Votre code de vérification est : " + verificationCode + "\n\n" +
                    "Ce code expirera dans 15 minutes.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe ZenithConvert";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email de vérification envoyé avec succès à: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de vérification", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            log.info("Envoi d'email de réinitialisation de mot de passe à: {}", toEmail);
            
            String subject = "Réinitialisation de votre mot de passe ZenithConvert";
            String resetLink = "https://app.zenithconvert.com/reset-password?token=" + resetToken;
            String body = "Bonjour,\n\n" +
                    "Vous avez demandé une réinitialisation de votre mot de passe.\n" +
                    "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe :\n\n" +
                    resetLink + "\n\n" +
                    "Ce lien expirera dans 1 heure.\n\n" +
                    "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe ZenithConvert";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email de réinitialisation envoyé avec succès à: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }
}
