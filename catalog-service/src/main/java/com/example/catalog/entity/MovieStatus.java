package com.example.catalog.entity;

/**
 * High level lifecycle for movie cards that controls whether they appear in the catalog.
 */
public enum MovieStatus {
    /**
     * Card is being prepared by editors and is not visible to end users.
     */
    DRAFT,
    /**
     * Card is reviewed and ready to be published after final checks.
     */
    READY,
    /**
     * Card is public and exposed to catalog/search.
     */
    PUBLISHED,
    /**
     * Card is archived and hidden from catalog but kept for history.
     */
    ARCHIVED
}
