package com.unibazaar.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.unibazaar.services.ICloudinaryService;

import java.io.File;
import java.util.Map;

public class CloudinaryServiceImpl implements ICloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl() {
        this.cloudinary = new Cloudinary(com.unibazaar.db.EnvConfig.get("CLOUDINARY_URL", ""));
    }

    @Override
    public String uploadImage(File file) {
        try {
            @SuppressWarnings("rawtypes")
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            return (String) uploadResult.get("url");
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }
}
