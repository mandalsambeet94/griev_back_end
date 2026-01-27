package com.grievance.repository;


import com.grievance.dto.GrievanceFilter;
import com.grievance.entity.Grievance;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class GrievanceSpecification {

    public static Specification<Grievance> filter(GrievanceFilter f) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (f.getBlock() != null)
                predicates.add(cb.equal(root.get("block"), f.getBlock()));

            if (f.getGp() != null)
                predicates.add(cb.equal(root.get("gp"), f.getGp()));

            if (f.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), f.getStatus()));

            if (f.getName() != null)
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + f.getName().toLowerCase() + "%"
                ));

            if (f.getStartDate() != null && f.getEndDate() != null) {
                predicates.add(cb.between(
                        root.get("createdAt"),
                        f.getStartDate().atStartOfDay(),
                        f.getEndDate().plusDays(1).atStartOfDay()
                ));
            }

            if (f.getTopic() != null && !f.getTopic().isBlank()) {

                String pattern = "%" + f.getTopic().toLowerCase() + "%";

                Predicate topic1 =
                        cb.like(cb.lower(root.get("topic1")), pattern);

                Predicate topic2 =
                        cb.like(cb.lower(root.get("topic2")), pattern);

                Predicate topic3 =
                        cb.like(cb.lower(root.get("topic3")), pattern);

                predicates.add(cb.or(topic1, topic2, topic3));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

