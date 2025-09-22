package com.mageddo.dnsproxyserver.config.provider.dataformatv3.converter;

import com.mageddo.dnsproxyserver.config.provider.dataformatv3.ConfigV3;
import com.mageddo.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EnvConverter implements Converter {

  static final String PREFIX = "DPS_";

  @Override
  public ConfigV3 parse() {
    return parse(System.getenv());
  }

  ConfigV3 parse(Map<String, String> envs) {
    final var tree = buildTree(envs);
    return JsonUtils.instance().convertValue(tree, ConfigV3.class);
  }

  @Override
  public String serialize(ConfigV3 config) {
    return "";
  }

  @Override
  public int priority() {
    return 0;
  }

  Map<String, Object> buildTree(Map<String, String> envs) {
    final Map<String, Object> root = new LinkedHashMap<>();
    envs.entrySet()
      .stream()
      .filter(entry -> entry.getKey().startsWith(PREFIX))
      .forEach(entry -> insert(root, ConfigV3.class, entry.getKey().substring(PREFIX.length()), entry.getValue()));
    return root;
  }

  private void insert(Map<String, Object> current, Class<?> currentClass, String key, String value) {
    final var tokens = key.split("_");
    assign(current, currentClass, tokens, 0, value);
  }

  private void assign(Map<String, Object> current, Class<?> currentClass, String[] tokens, int index, String value) {
    final var match = findField(currentClass, tokens, index);
    final var field = match.field();
    final var nextIndex = index + match.consumedTokens();

    if (List.class.isAssignableFrom(field.getType())) {
      final int listIndex = parseIndex(tokens, nextIndex);
      final var list = (List<Object>) current.computeIfAbsent(field.getName(), k -> new ArrayList<>());
      ensureSize(list, listIndex + 1);

      final var elementType = match.elementType();
      if (isSimple(elementType)) {
        list.set(listIndex, convert(value, elementType));
        return;
      }

      Map<String, Object> nested = asMap(list.get(listIndex));
      if (nested == null) {
        nested = new LinkedHashMap<>();
        list.set(listIndex, nested);
      }
      assign(nested, elementType, tokens, nextIndex + 1, value);
      return;
    }

    if (isSimple(field.getType())) {
      current.put(field.getName(), convert(value, field.getType()));
      return;
    }

    final Map<String, Object> nested = (Map<String, Object>) current.computeIfAbsent(field.getName(), k -> new LinkedHashMap<>());
    assign(nested, field.getType(), tokens, nextIndex, value);
  }

  private FieldMatch findField(Class<?> currentClass, String[] tokens, int startIndex) {
    for (int len = tokens.length - startIndex; len >= 1; len--) {
      final var property = toProperty(tokens, startIndex, len);
      final Field field = findField(currentClass, property);
      if (field != null) {
        final Class<?> elementType;
        if (List.class.isAssignableFrom(field.getType())) {
          if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
            elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
          } else {
            elementType = Object.class;
          }
        } else {
          elementType = null;
        }
        return new FieldMatch(field, len, elementType);
      }
    }
    throw new IllegalArgumentException("Unknown path for " + String.join("_", tokens));
  }

  private Field findField(Class<?> clazz, String name) {
    try {
      return clazz.getField(name);
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private String toProperty(String[] tokens, int startIndex, int len) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      if (i > 0) {
        sb.append('_');
      }
      sb.append(tokens[startIndex + i]);
    }
    return toCamelCase(sb.toString());
  }

  private String toCamelCase(String value) {
    final var parts = value.toLowerCase(Locale.US).split("_");
    final var sb = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      sb.append(StringUtils.capitalize(parts[i]));
    }
    return sb.toString();
  }

  private boolean isSimple(Class<?> type) {
    if (type == null) {
      return false;
    }
    return type.isPrimitive()
      || Number.class.isAssignableFrom(type)
      || Boolean.class.isAssignableFrom(type)
      || CharSequence.class.isAssignableFrom(type);
  }

  private Object convert(String value, Class<?> type) {
    if (StringUtils.isBlank(value) && type != String.class) {
      return null;
    }
    if (type == String.class) {
      return value;
    }
    if (type == Integer.class || type == int.class) {
      return Integer.valueOf(value);
    }
    if (type == Boolean.class || type == boolean.class) {
      return Boolean.valueOf(value);
    }
    if (type == Long.class || type == long.class) {
      return Long.valueOf(value);
    }
    if (type == Double.class || type == double.class) {
      return Double.valueOf(value);
    }
    if (type == Float.class || type == float.class) {
      return Float.valueOf(value);
    }
    return value;
  }

  private int parseIndex(String[] tokens, int index) {
    if (index >= tokens.length) {
      throw new IllegalArgumentException("Missing list index for " + String.join("_", tokens));
    }
    return Integer.parseInt(tokens[index]);
  }

  private void ensureSize(List<Object> list, int size) {
    while (list.size() < size) {
      list.add(null);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return null;
  }

  record FieldMatch(Field field, int consumedTokens, Class<?> elementType) {
  }
}
