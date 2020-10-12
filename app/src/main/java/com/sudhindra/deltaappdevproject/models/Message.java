package com.sudhindra.deltaappdevproject.models;

import com.google.firebase.firestore.Exclude;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {

    public static final String SENDER_ID = "senderId", SENT_TIME_IN_MILLIS = "sentTimeInMillis";
    public static final int TEXT = 9023, IMAGE = 1289, FILE = 4590;
    private String text;
    private String senderId;
    private String recipientId;
    private String senderName;
    private int type;
    // File Related Stuff
    private String fileName;
    private String fileMIMEType;
    private int fileProgress = -1;
    private boolean uploading = false;
    /**
     * sentTimeInMillis is also the name of the Message File (if exists) in the "messages" folder of Firebase Storage
     */
    private long sentTimeInMillis;

    public Message() {

    }

    public Message(long sentTimeInMillis) {
        this.sentTimeInMillis = sentTimeInMillis;
    }

    public Message(String text, String senderId, String recipientId, String senderName, int type, long sentTimeInMillis) {
        this.text = text;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.senderName = senderName;
        this.type = type;
        this.sentTimeInMillis = sentTimeInMillis;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileMIMEType() {
        return fileMIMEType;
    }

    public void setFileMIMEType(String fileMIMEType) {
        this.fileMIMEType = fileMIMEType;
    }

    @Exclude
    public int getFileProgress() {
        return fileProgress;
    }

    public void setFileProgress(int fileProgress) {
        this.fileProgress = fileProgress;
    }

    @Exclude
    public boolean isUploading() {
        return uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    public long getSentTimeInMillis() {
        return sentTimeInMillis;
    }

    public void setSentTimeInMillis(long sentTimeInMillis) {
        this.sentTimeInMillis = sentTimeInMillis;
    }

    @Exclude
    public String getTimeText() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        Date currentDate = new Date(System.currentTimeMillis());
        Date sentDate = new Date(sentTimeInMillis);
        if (fmt.format(sentDate).equals(fmt.format(currentDate))) {
            fmt = new SimpleDateFormat("hh:mm a");
            return fmt.format(sentDate);
        }
        fmt = new SimpleDateFormat("dd/MM/yy  hh:mm a");
        return fmt.format(sentDate);
    }

    @Exclude
    public TextMessage getTextMessageForUser(String currentUid) {
        if (senderId.equals(currentUid))
            return TextMessage.createForLocalUser(text, sentTimeInMillis);
        return TextMessage.createForRemoteUser(text, sentTimeInMillis, senderId);
    }
}
