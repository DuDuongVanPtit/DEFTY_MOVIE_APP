package com.example.defty_movie_app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.defty_movie_app.shared.UserManager;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class FeedbackRepository {
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private static final String SENDER_EMAIL = "nguyenvietvan223@gmail.com";
    private static final String SENDER_PASSWORD = "fpna rnqz fbjr kgza";
    private static final String RECEIVER_EMAIL = "nguyenvietvan223@gmail.com";

    public interface FeedbackCallback {
        void onSuccess();
        void onError(String error);
    }

    public FeedbackRepository(Context context) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.context = context.getApplicationContext();
    }

    public void sendFeedback(String message, FeedbackCallback callback) {
        executorService.execute(() -> {
            try {
                sendEmail(message);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gửi feedback thất bại: " + e.getMessage()));
            }
        });
    }

    private void sendEmail(String message) throws MessagingException {
        String userEmail = UserManager.getEmail(context);
        System.out.println(userEmail);
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(userEmail));
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(RECEIVER_EMAIL));
        mimeMessage.setSubject("Defty Movie App - Feedback");

        String fullName = UserManager.getFullName(context);

        String detailedMessage = "Feedback from Defty Movie App\n\n" +
                "Full Name: " + fullName + "\n" +
                "Email: " + userEmail + "\n\n" +
                "Feedback:\n" + message + "\n\n" +
                "Sent at: " + new Date();


        mimeMessage.setText(detailedMessage);

        Transport.send(mimeMessage);
    }
}