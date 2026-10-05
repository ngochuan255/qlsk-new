package com.qlsk.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrCodeService {

    @Value("${app.qr.width:300}")
    private int width;

    @Value("${app.qr.height:300}")
    private int height;

    /**
     * Generates a PNG image of QR Code for given content
     */
    public byte[] generateQrCodeImage(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh mã QR: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a Base64 data URL for direct embedding in HTML (data:image/png;base64,...)
     */
    public String generateQrCodeBase64(String text) {
        byte[] imageBytes = generateQrCodeImage(text);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}
