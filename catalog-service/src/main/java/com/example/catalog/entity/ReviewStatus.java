package com.example.catalog.entity;

public enum ReviewStatus {
    /**
     * Review was submitted by a user and is waiting for moderation.
     */
    PENDING,
    /**
     * Review is visible to everyone.
     */
    PUBLISHED,
    /**
     * Review is hidden because it was marked as spam or abusive.
     */
    SPAM,
    /**
     * Review is removed by moderators.
     */
    DELETED
}
