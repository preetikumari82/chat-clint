package com.example.chatbot.model;

/** Document ki zindagi (SRS 4.4): UPLOADED -> PROCESSING -> INDEXED ya FAILED */
public enum DocumentStatus {
    UPLOADED,    // file save ho gayi, abhi job queue me nahi gayi
    PROCESSING,  // ingestion chal raha hai
    INDEXED,     // chunks + vectors ready, bot use kar sakta hai
    FAILED       // kuch toot gaya, reason IngestionJob me hai
}