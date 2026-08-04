package com.back.global.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * float[] <-> DB TEXT 컬럼(CSV 문자열) 변환 컨버터
 * - 임베딩 벡터를 별도 벡터 타입 없이 TEXT 컬럼에 저장하기 위함 (DB 이식성 확보)
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            sb.append(attribute[i]);
        }
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new float[0];
        }

        String[] tokens = dbData.split(DELIMITER);
        float[] result = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = Float.parseFloat(tokens[i]);
        }
        return result;
    }
}
