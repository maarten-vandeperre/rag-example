package com.rag.app.infrastructure.document;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class PlainTextExtractor {

    public String extract(byte[] fileContent) {
        Charset charset = detectCharset(fileContent);
        int offset = bomOffset(fileContent, charset);

        try {
            CharBuffer charBuffer = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(fileContent, offset, fileContent.length - offset));
            return charBuffer.toString().strip();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Failed to decode plain text document", exception);
        }
    }

    private Charset detectCharset(byte[] fileContent) {
        if (fileContent.length >= 2 && fileContent[0] == (byte) 0xFE && fileContent[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (fileContent.length >= 2 && fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (fileContent.length >= 3 && fileContent[0] == (byte) 0xEF && fileContent[1] == (byte) 0xBB && fileContent[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        return StandardCharsets.UTF_8;
    }

    private int bomOffset(byte[] fileContent, Charset charset) {
        if (charset.equals(StandardCharsets.UTF_8) && fileContent.length >= 3
            && fileContent[0] == (byte) 0xEF && fileContent[1] == (byte) 0xBB && fileContent[2] == (byte) 0xBF) {
            return 3;
        }
        if ((charset.equals(StandardCharsets.UTF_16BE) || charset.equals(StandardCharsets.UTF_16LE)) && fileContent.length >= 2) {
            return 2;
        }
        return 0;
    }
}
