package database;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * EmailService - Simple Email Service
 * 
 * A clean, reusable email service for the HRM system.
 * Sends emails with dynamically provided subject and body content.
 * 
 * Fetches SMTP configuration from Firebase Firestore for security.
 */
public class EmailService {
    
    // Firebase Configuration
    private static final String PROJECT_ID = "distributed-system-data";
    private static final String FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
            + "/databases/(default)/documents";
    private static final String JAVAMAIL_CONFIG_ID = "NwT7uYZ7gOe0jDUNPqG1";
    
    // SMTP Configuration (fetched from Firebase)
    private String smtpHost;
    private String smtpPort;
    private String username;
    private String appPassword;
    
    /**
     * Constructor - Fetches SMTP configuration from Firebase
     */
    public EmailService() {
        fetchSmtpConfig();
    }
    
    /**
     * Fetch SMTP configuration from Firebase Firestore
     * Reads from JavaMail collection with document ID: NwT7uYZ7gOe0jDUNPqG1
     */
    private void fetchSmtpConfig() {
        try {
            URL url = URI.create(FIRESTORE_URL + "/JavaMail/" + JAVAMAIL_CONFIG_ID).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                
                // Debug: Print raw response to see what Firebase is returning
                System.out.println("DEBUG - Raw Firebase response:");
                System.out.println(response);
                System.out.println();
                
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject fields = json.getAsJsonObject("fields");
                
                if (fields != null) {
                    // Debug: Print available field names
                    System.out.println("DEBUG - Available fields in Firebase:");
                    fields.keySet().forEach(key -> System.out.println("  - " + key));
                    System.out.println();
                    
                    // Extract SMTP configuration from Firebase (matching Firebase field names)
                    this.smtpHost = getStringField(fields, "Host");
                    this.smtpPort = getStringField(fields, "Port");
                    this.username = getStringField(fields, "Username");
                    this.appPassword = getStringField(fields, "App Passowrd");  // Note: Matches Firebase field name (typo in Firebase)
                    
                    System.out.println("✓ SMTP configuration loaded from Firebase");
                    System.out.println("  Host: " + this.smtpHost);
                    System.out.println("  Port: " + this.smtpPort);
                    System.out.println("  Username: " + this.username);
                    System.out.println("  Password: " + (this.appPassword != null ? "***" : "NOT FOUND"));
                } else {
                    System.err.println("✗ Failed to load SMTP configuration: fields not found");
                }
            } else {
                System.err.println("✗ Failed to fetch SMTP config from Firebase (HTTP " + conn.getResponseCode() + ")");
            }

        } catch (Exception e) {
            System.err.println("✗ Error fetching SMTP config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to extract string value from Firestore field
     */
    private String getStringField(JsonObject fields, String fieldName) {
        if (fields.has(fieldName)) {
            return fields.getAsJsonObject(fieldName).get("stringValue").getAsString();
        }
        return null;
    }
    
    /**
     * Helper method to read HTTP response
     */
    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
    
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
            // SMTP Configuration (using values from Firebase)
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, appPassword);
                }
            });
            
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "BHEL HRM System"));
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
