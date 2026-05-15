package com.example.catalog.service.asset;

public class MovieAssetStorageException extends RuntimeException {
    public MovieAssetStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MovieAssetStorageException(String message) {
        super(message);
    }
}
