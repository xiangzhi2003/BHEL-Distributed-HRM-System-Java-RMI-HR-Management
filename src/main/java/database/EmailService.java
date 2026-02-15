package database;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * EmailService - Simple Email Service
 * 
 * A clean, reusable email service for the HRM system.
 * Sends emails with dynamically provided subject and body content.
 * 
 * Uses Gmail SMTP with App Password authentication.
 */
public class EmailService {
    
    // Gmail SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "comeonbro1233@gmail.com";
    private static final String APP_PASSWORD = "kech abej gdul ouaz";
    
    /**
     * Send an email with custom subject and body
     * 
     * @param toEmail Recipient email address
     * @param subject Email subject line
     * @param body Email body content
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEmail(String toEmail, String subject, String body) {
        try {
            // SMTP Configuration
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, APP_PASSWORD);
                }
            });
            
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME, "BHEL HRM System"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            
            // Send email
            Transport.send(message);
            System.out.println("✓ Email sent successfully to: " + toEmail);
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Failed to send email to: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }
}
