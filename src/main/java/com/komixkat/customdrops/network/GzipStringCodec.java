package com.komixkat.customdrops.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class GzipStringCodec implements StreamCodec<RegistryFriendlyByteBuf, String> {

    public static final GzipStringCodec INSTANCE = new GzipStringCodec();

    private static final int MAX_DECOMPRESSED_BYTES = 16 * 1024 * 1024;

    private GzipStringCodec() {}

    @Override
    public String decode(RegistryFriendlyByteBuf buf) {
        int length = buf.readInt();
        if (length < 0 || length > buf.readableBytes()) {
            throw new IllegalStateException("Invalid gzip payload length " + length);
        }
        byte[] compressed = new byte[length];
        buf.readBytes(compressed);
        try {
            String result = inflate(compressed);
            if (result == null) {
                throw new IllegalStateException("Failed to decompress customdrops payload");
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decompress customdrops payload", e);
        }
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf, String value) {
        byte[] compressed;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(value.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            compressed = baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress customdrops payload", e);
        }
        buf.writeInt(compressed.length);
        buf.writeBytes(compressed);
    }

    private static String inflate(byte[] compressed) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = gzip.read(chunk)) >= 0) {
                if (result.size() + read > MAX_DECOMPRESSED_BYTES) {
                    throw new IOException("Decompressed payload exceeds " + MAX_DECOMPRESSED_BYTES + " bytes");
                }
                result.write(chunk, 0, read);
            }
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}