package com.team2.wellness.engagement.recap.domain;
import jakarta.persistence.AttributeConverter; import jakarta.persistence.Converter; import java.time.YearMonth;
@Converter public class YearMonthAttributeConverter implements AttributeConverter<YearMonth,String>{public String convertToDatabaseColumn(YearMonth value){return value==null?null:value.toString();}public YearMonth convertToEntityAttribute(String value){return value==null?null:YearMonth.parse(value);}}
