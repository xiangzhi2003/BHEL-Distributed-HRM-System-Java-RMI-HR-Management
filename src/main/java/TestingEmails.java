import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * TestingEmails - Email Testing Utility
 * 
 * This class demonstrates sending emails using Gmail SMTP.
 * Perfect for academic testing with zero installation required.
 * 
 * Setup Instructions:
 * 1. Enable 2-Step Verification in your Google Account
 * 2. Generate an App Password: https://myaccount.google.com/security
 * 3. Replace "YOUR_16_DIGIT_APP_PASSWORD" below with your actual app password
 * 4. Run this file to test email sending
 */
public class TestingEmails {
    
    // Gmail SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "comeonbro1233@gmail.com";
    private static final String APP_PASSWORD = "kech abej gdul ouaz"; // Replace this!
    
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("Email Testing Utility");
        System.out.println("=================================\n");
        
        // Send a test email
        sendTestEmail();
    }
    
    /**
     * Sends a simple test email using Gmail SMTP
     */
    public static void sendTestEmail() {
        try {
            // Step 1: Configure SMTP Properties
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");  // Enable TLS encryption
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");  // Use TLS 1.2 for security
            
            System.out.println("Connecting to Gmail SMTP server...");
            System.out.println("Host: " + SMTP_HOST + ":" + SMTP_PORT);
            System.out.println("Username: " + USERNAME);
            
            // Step 2: Create Session with Authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, APP_PASSWORD);
                }
            });
            
            // Enable debug mode (optional - shows detailed SMTP communication)
            // session.setDebug(true);
            
            // Step 3: Create Email Message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(USERNAME)  // Sending to yourself for testing
            );
            message.setSubject("Test Email from Java RMI HR Management System");
            
            // Email body
            String emailBody = "Hello!\n\n" +
                             "This is a test email sent from the BHEL Distributed HRM System.\n\n" +
                             "If you're reading this, your email configuration is working correctly!\n\n" +
                             "Gmail SMTP Configuration:\n" +
                             "- Host: smtp.gmail.com\n" +
                             "- Port: 587 (TLS)\n" +
                             "- Protocol: SMTP with StartTLS\n\n" +
                             "Sent on: " + new java.util.Date() + "\n\n" +
                             "Best regards,\n" +
                             "HRM System";
            
            message.setText(emailBody);
            
            // Step 4: Send the Email
            System.out.println("\nSending test email...");
            Transport.send(message);
            
            System.out.println("\n✓ SUCCESS! Email sent successfully!");
            System.out.println("Check your inbox at: " + USERNAME);
            System.out.println("\nEmail Details:");
            System.out.println("  From: " + USERNAME);
            System.out.println("  To: " + USERNAME);
            System.out.println("  Subject: Test Email from Java RMI HR Management System");
            
        } catch (AuthenticationFailedException e) {
            System.err.println("\n✗ AUTHENTICATION FAILED!");
            System.err.println("Possible reasons:");
            System.err.println("  1. Invalid App Password (not your regular Gmail password)");
            System.err.println("  2. 2-Step Verification not enabled");
            System.err.println("  3. App Password expired or revoked");
            System.err.println("\nTo fix:");
            System.err.println("  1. Go to: https://myaccount.google.com/security");
            System.err.println("  2. Enable 2-Step Verification");
            System.err.println("  3. Generate a new App Password");
            System.err.println("  4. Update APP_PASSWORD in this file");
            e.printStackTrace();
            
        } catch (MessagingException e) {
            System.err.println("\n✗ ERROR: Failed to send email");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            
        } catch (Exception e) {
            System.err.println("\n✗ UNEXPECTED ERROR");
            e.printStackTrace();
        }
    }
    
    /**
     * Utility method to send a custom email
     * 
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param body Email body/message
     * @return true if email sent successfully, false otherwise
     */
    public static boolean sendEmail(String toEmail, String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, APP_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        }
    }
}
