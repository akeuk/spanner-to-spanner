package com.custom.dataflow.utils;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.Type.StructField;
import com.google.cloud.Timestamp;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Base64;
import java.util.function.BiFunction;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

public class SpannerUtils {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    //Strict to JSON
    public static JsonObject convertStructToJson(Struct struct) {
      JsonObject jsonObject = new JsonObject();
      List<Type.StructField> structFields = struct.getType().getStructFields();

      for (Type.StructField field : structFields) {
        if (struct.isNull(field.getName())) {
          continue;
        }

        switch (field.getType().getCode()) {
          case BOOL:
            jsonObject.addProperty(field.getName(), struct.getBoolean(field.getName()));
            break;
          case INT64:
            jsonObject.addProperty(field.getName(), struct.getLong(field.getName()));
            break;
          case FLOAT64:
            jsonObject.addProperty(field.getName(), struct.getDouble(field.getName()));
            break;
          case STRING:
          case PG_NUMERIC:
            jsonObject.addProperty(field.getName(), struct.getString(field.getName()));
            break;
          case BYTES:
            jsonObject.addProperty(field.getName(), struct.getBytes(field.getName()).toStringUtf8());
            break;
          case DATE:
            jsonObject.addProperty(field.getName(), struct.getDate(field.getName()).toString());
            break;
          case TIMESTAMP:
            jsonObject.addProperty(field.getName(), struct.getTimestamp(field.getName()).toString());
            break;
          case ARRAY:
            jsonObject.add(field.getName(), convertArrayToJsonArray(struct, field.getName()));
            break;
          case STRUCT:
            jsonObject.add(field.getName(), convertStructToJson(struct.getStruct(field.getName())));
            break;
          default:
            throw new RuntimeException("Unsupported type: " + field.getType());
        }
      }
      return jsonObject;
    }
    
    private static JsonArray convertArrayToJsonArray(Struct struct, String columnName) {
      Type.Code code = struct.getColumnType(columnName).getArrayElementType().getCode();
      JsonArray jsonArray = new JsonArray();
      switch (code) {
        case BOOL:
          struct.getBooleanList(columnName).forEach(jsonArray::add);
          break;
        case INT64:
          struct.getLongList(columnName).forEach(jsonArray::add);
          break;
        case FLOAT64:
          struct.getDoubleList(columnName).forEach(jsonArray::add);
          break;
        case STRING:
        case PG_NUMERIC:
          struct.getStringList(columnName).forEach(jsonArray::add);
          break;
        case BYTES:
          struct.getBytesList(columnName).stream()
              .map(ByteArray::toStringUtf8)
              .forEach(jsonArray::add);
          break;
        case DATE:
          struct.getDateList(columnName).stream().map(Date::toString).forEach(jsonArray::add);
          break;
        case TIMESTAMP:
          struct.getTimestampList(columnName).stream()
              .map(Timestamp::toString)
              .forEach(jsonArray::add);
          break;
        case STRUCT:
          struct.getStructList(columnName).stream()
              .map(SpannerUtils::convertStructToJson)
              .forEach(jsonArray::add);
          break;
        default:
          throw new RuntimeException("Unsupported type: " + code);
      }
      return jsonArray;
    }

    // Struct to CSV
    public static String convertStructToCsv(Struct struct) {
        StringWriter stringWriter = new StringWriter();
        try {
          CSVPrinter printer =
              new CSVPrinter(
                  stringWriter,
                  CSVFormat.DEFAULT.withRecordSeparator("").withQuoteMode(QuoteMode.ALL_NON_NULL));
          LinkedHashMap<String, BiFunction<Struct, String, String>> parsers = Maps.newLinkedHashMap();
          parsers.putAll(mapColumnParsers(struct.getType().getStructFields()));
          List<String> values = parseResultSet(struct, parsers);
          printer.printRecord(values);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return stringWriter.toString();
    }

    private static List<String> parseResultSet(
        Struct struct, LinkedHashMap<String, BiFunction<Struct, String, String>> parsers) {
          List<String> result = Lists.newArrayList();
    
          for (String columnName : parsers.keySet()) {
            if (!parsers.containsKey(columnName)) {
              throw new RuntimeException("No parser for column: " + columnName);
            }
            result.add(parsers.get(columnName).apply(struct, columnName));
          }
          return result;
    }

    private static LinkedHashMap<String, BiFunction<Struct, String, String>> mapColumnParsers(
            List<StructField> fields) {
          LinkedHashMap<String, BiFunction<Struct, String, String>> columnParsers =
              Maps.newLinkedHashMap();
          for (StructField field : fields) {
            columnParsers.put(field.getName(), getColumnParser(field.getType().getCode()));
          }
          return columnParsers;
    }

    private static BiFunction<Struct, String, String> getColumnParser(Type.Code columnType) {
        switch (columnType) {
          case BOOL:
            return nullSafeColumnParser(
                (currentRow, columnName) -> Boolean.toString(currentRow.getBoolean(columnName)));
          case INT64:
          case ENUM:
            return nullSafeColumnParser(
                (currentRow, columnName) -> Long.toString(currentRow.getLong(columnName)));
          case FLOAT32:
            return nullSafeColumnParser(
                ((currentRow, columnName) -> Float.toString(currentRow.getFloat(columnName))));
          case FLOAT64:
            return nullSafeColumnParser(
                ((currentRow, columnName) -> Double.toString(currentRow.getDouble(columnName))));
          case STRING:
          case PG_NUMERIC:
            return nullSafeColumnParser(Struct::getString);
          case JSON:
            return nullSafeColumnParser(Struct::getJson);
          case PG_JSONB:
            return nullSafeColumnParser(Struct::getPgJsonb);
          case BYTES:
          case PROTO:
            return nullSafeColumnParser(
                (currentRow, columnName) ->
                    Base64.getEncoder().encodeToString(currentRow.getBytes(columnName).toByteArray()));
          case DATE:
            return nullSafeColumnParser(
                (currentRow, columnName) -> currentRow.getDate(columnName).toString());
          case TIMESTAMP:
            return nullSafeColumnParser(
                (currentRow, columnName) -> currentRow.getTimestamp(columnName).toString());
          case ARRAY:
            return nullSafeColumnParser(SpannerUtils::parseArrayValue);
          default:
            throw new RuntimeException("Unsupported type: " + columnType);
        }
      }
    
    private static BiFunction<Struct, String, String> nullSafeColumnParser(
        BiFunction<Struct, String, String> columnParser) {
      return (currentRow, columnName) ->
          currentRow.isNull(columnName) ? null : columnParser.apply(currentRow, columnName);
    }
  
    private static String parseArrayValue(Struct currentRow, String columnName) {
        Code code = currentRow.getColumnType(columnName).getArrayElementType().getCode();
        switch (code) {
          case BOOL:
            return GSON.toJson(currentRow.getBooleanArray(columnName));
          case INT64:
          case ENUM:
            return GSON.toJson(currentRow.getLongArray(columnName));
          case FLOAT32:
            return GSON.toJson(currentRow.getFloatArray(columnName));
          case FLOAT64:
            return GSON.toJson(currentRow.getDoubleArray(columnName));
          case STRING:
          case PG_NUMERIC:
            return GSON.toJson(currentRow.getStringList(columnName));
          case BYTES:
          case PROTO:
            return GSON.toJson(
                currentRow.getBytesList(columnName).stream()
                    .map(byteArray -> Base64.getEncoder().encodeToString(byteArray.toByteArray()))
                    .collect(Collectors.toList()));
          case DATE:
            return GSON.toJson(
                currentRow.getDateList(columnName).stream()
                    .map(Date::toString)
                    .collect(Collectors.toList()));
          case TIMESTAMP:
            return GSON.toJson(
                currentRow.getTimestampList(columnName).stream()
                    .map(Timestamp::toString)
                    .collect(Collectors.toList()));
          default:
            throw new RuntimeException("Unsupported type: " + code);
        }
    }
}
