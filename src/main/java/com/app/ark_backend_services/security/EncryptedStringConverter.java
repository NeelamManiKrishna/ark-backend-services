package com.app.ark_backend_services.security;

import org.springframework.data.mongodb.core.convert.MongoConversionContext;
import org.springframework.data.mongodb.core.convert.MongoValueConverter;

/** Encrypts a String property on write and decrypts it on read (legacy plaintext is passed through). */
public class EncryptedStringConverter implements MongoValueConverter<String, String> {

    @Override
    public String read(String value, MongoConversionContext context) {
        return FieldEncryption.decrypt(value);
    }

    @Override
    public String write(String value, MongoConversionContext context) {
        return FieldEncryption.encrypt(value);
    }
}
