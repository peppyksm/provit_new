package com.provit.persistence;

import jakarta.persistence.*;

import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Shared JPA operations; callers choose explicit entity types and ownership predicates. */
@Repository
@Transactional(readOnly = true)
public class JpaStore {
    @PersistenceContext private EntityManager entityManager;

    public static Map<String, Object> params(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private <Q extends Query> Q bind(Q query, Map<String, ?> values) {
        for (var parameter : query.getParameters()) {
            String name = parameter.getName();
            if (name != null) {
                if (!values.containsKey(name))
                    throw new IllegalArgumentException("Missing query parameter " + name);
                Object value = values.get(name);
                Class<?> type = parameter.getParameterType();
                if (value instanceof Number number) {
                    if (type == Integer.class) value = Math.toIntExact(number.longValue());
                    if (type == Long.class) value = number.longValue();
                    if (type == Short.class) value = number.shortValue();
                }
                query.setParameter(
                        name, value instanceof String text && text.isEmpty() ? null : value);
            }
        }
        return query;
    }

    public <E> List<E> entities(
            Class<E> type, String predicate, Map<String, ?> params, int offset, int limit) {
        var query =
                bind(
                        entityManager.createQuery(
                                "select e from " + type.getSimpleName() + " e " + predicate, type),
                        params);
        query.setFirstResult(Math.max(0, offset));
        query.setMaxResults(Math.max(1, limit));
        return query.getResultList();
    }

    public <E, T> List<T> list(
            Class<E> entity, Class<T> dto, String predicate, Map<String, ?> params) {
        return list(entity, dto, predicate, params, 0, Integer.MAX_VALUE, Map.of());
    }

    public <E, T> List<T> list(
            Class<E> entity,
            Class<T> dto,
            String predicate,
            Map<String, ?> params,
            int offset,
            int limit,
            Map<String, String> aliases) {
        return entities(entity, predicate, params, offset, limit).stream()
                .map(value -> DtoMapper.copy(value, DtoMapper.create(dto), aliases))
                .toList();
    }

    public <E, T> T one(Class<E> entity, Class<T> dto, String predicate, Map<String, ?> params) {
        var results = list(entity, dto, predicate, params);
        return results.isEmpty() ? null : results.get(0);
    }

    public long count(Class<?> entity, String predicate, Map<String, ?> params) {
        return bind(
                        entityManager.createQuery(
                                "select count(e) from "
                                        + entity.getSimpleName()
                                        + " e "
                                        + predicate,
                                Long.class),
                        params)
                .getSingleResult();
    }

    @Transactional
    public <E> int insert(Class<E> entityType, Object dto, Map<String, String> dtoToEntity) {
        return insert(entityType, dto, dtoToEntity, Set.of());
    }

    @Transactional
    public <E> int insert(
            Class<E> entityType,
            Object dto,
            Map<String, String> dtoToEntity,
            Set<String> serverFields) {
        E entity = DtoMapper.create(entityType);
        DtoMapper.values(dto)
                .forEach(
                        (name, value) -> {
                            String field = dtoToEntity.getOrDefault(name, name);
                            if (!serverFields.contains(field)) DtoMapper.set(entity, field, value);
                        });
        for (var field : entityType.getDeclaredFields()) {
            if (field.isAnnotationPresent(GeneratedValue.class)) {
                DtoMapper.set(entity, field.getName(), null);
            }
        }
        entityManager.persist(entity);
        entityManager.flush();
        Map<String, String> reverse = new HashMap<>();
        dtoToEntity.forEach((key, value) -> reverse.put(value, key));
        DtoMapper.copy(entity, dto, reverse);
        return 1;
    }

    @Transactional
    public <E> int patch(
            Class<E> entity, String predicate, Map<String, ?> params, Map<String, ?> changes) {
        var found = entities(entity, predicate, params, 0, 1);
        if (found.isEmpty()) return 0;
        changes.forEach((name, value) -> DtoMapper.set(found.get(0), name, value));
        if (PropertyAccessorFactory.forBeanPropertyAccess(found.get(0))
                .isWritableProperty("updatedAt")) {
            DtoMapper.set(found.get(0), "updatedAt", java.time.LocalDateTime.now(DtoMapper.ZONE));
        }
        entityManager.flush();
        return 1;
    }

    @Transactional
    public int modify(String jpql, Map<String, ?> params) {
        entityManager.flush();
        int count = bind(entityManager.createQuery(jpql), params).executeUpdate();
        entityManager.clear();
        return count;
    }

    @Transactional
    public int nativeModify(String sql, Map<String, ?> params) {
        entityManager.flush();
        int count = bind(entityManager.createNativeQuery(sql), params).executeUpdate();
        entityManager.clear();
        return count;
    }

    public <T> T scalar(String sql, Class<T> type, Map<String, ?> params) {
        Object value = bind(entityManager.createNativeQuery(sql), params).getSingleResult();
        if (value == null) return null;
        if (type == Integer.class) return type.cast(Math.toIntExact(((Number) value).longValue()));
        if (type == Long.class) return type.cast(((Number) value).longValue());
        return type.cast(value);
    }

    public <T> List<T> nativeList(String sql, Class<T> dto, Map<String, ?> params) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows =
                bind(entityManager.createNativeQuery(sql, Tuple.class), params).getResultList();
        return mapRows(rows, dto);
    }

    public <T> List<T> project(String jpql, Class<T> dto, Map<String, ?> params) {
        return mapRows(
                bind(entityManager.createQuery(jpql, Tuple.class), params).getResultList(), dto);
    }

    private <T> List<T> mapRows(List<Tuple> rows, Class<T> dto) {
        return rows.stream()
                .map(
                        row -> {
                            T value = DtoMapper.create(dto);
                            var bean = PropertyAccessorFactory.forBeanPropertyAccess(value);
                            for (var element : row.getElements()) {
                                String alias = element.getAlias();
                                String name = DtoMapper.camel(alias);
                                if (!bean.isWritableProperty(name)) {
                                    name =
                                            Arrays.stream(bean.getPropertyDescriptors())
                                                    .map(java.beans.PropertyDescriptor::getName)
                                                    .filter(
                                                            candidate ->
                                                                    candidate.equalsIgnoreCase(
                                                                            alias))
                                                    .findFirst()
                                                    .orElse(name);
                                }
                                DtoMapper.set(value, name, row.get(alias));
                            }
                            return value;
                        })
                .toList();
    }

    public <T> T nativeOne(String sql, Class<T> dto, Map<String, ?> params) {
        var rows = nativeList(sql, dto, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public <E> E lock(Class<E> type, Object id) {
        return entityManager.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    }
}
