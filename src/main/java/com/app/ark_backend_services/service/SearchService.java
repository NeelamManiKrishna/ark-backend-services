package com.app.ark_backend_services.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Shared "q" search for list endpoints: a case-insensitive prefix match on a resource's name/email/id fields,
 * always combined (AND) with the caller's tenant and branch criteria so search can never widen access.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final MongoTemplate mongoTemplate;

    public <E, R> Page<R> search(Class<E> type, Criteria scope, String q, List<String> fields,
                                 Pageable pageable, Function<E, R> mapper) {
        String term = q == null ? "" : q.trim();
        Criteria criteria = scope;
        if (!term.isEmpty()) {
            Pattern prefix = Pattern.compile("^" + Pattern.quote(term), Pattern.CASE_INSENSITIVE);
            Criteria[] alternatives = fields.stream()
                    .map(f -> Criteria.where(f).regex(prefix))
                    .toArray(Criteria[]::new);
            criteria = new Criteria().andOperator(scope, new Criteria().orOperator(alternatives));
        }
        Query query = Query.query(criteria);
        long total = mongoTemplate.count(query, type);
        List<R> rows = mongoTemplate.find(query.with(pageable), type).stream().map(mapper).toList();
        return new PageImpl<>(rows, pageable, total);
    }
}
