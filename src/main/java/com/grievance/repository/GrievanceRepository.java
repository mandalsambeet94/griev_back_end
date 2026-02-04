package com.grievance.repository;

import com.grievance.entity.Grievance;
import com.grievance.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long>, JpaSpecificationExecutor<Grievance> {

    Page<Grievance> findByCollectedBy(User collectedBy, Pageable pageable);

    @EntityGraph(attributePaths = "attachments")
    List<Grievance> findAll(Specification<Grievance> spec);

    List<Grievance> findByBlock(String block);

    List<Grievance> findByGp(String gp);

    List<Grievance> findByVillageSahi(String villageSahi);

    List<Grievance> findByNameContainingIgnoreCase(String name);

    /*@Query("SELECT g FROM Grievance g WHERE " +
            "(:block IS NULL OR g.block = :block) AND " +
            "(:gp IS NULL OR g.gp = :gp) AND " +
            "(:villageSahi IS NULL OR g.villageSahi = :villageSahi) AND " +
            "(:name IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Grievance> findByFilters(@Param("block") String block,
                                  @Param("gp") String gp,
                                  @Param("villageSahi") String villageSahi,
                                  @Param("name") String name);*/

    @Query("""
SELECT g FROM Grievance g
WHERE g.block = COALESCE(:block, g.block)
AND g.gp = COALESCE(:gp, g.gp)
AND g.villageSahi = COALESCE(:villageSahi, g.villageSahi)
AND g.wardNo = COALESCE(:wardNo, g.wardNo)
AND g.createdAt >= COALESCE(:startOfDay, g.createdAt)
  AND g.createdAt < COALESCE(:endOfDay, g.createdAt)
  AND g.contact = COALESCE(:contact, g.contact)
  AND g.fatherSpouseName ILIKE CONCAT('%', COALESCE(:fatherSpouseName, ''), '%')
AND g.status = :status
AND g.name ILIKE CONCAT('%', COALESCE(:name, ''), '%')
""")
    List<Grievance> findWithStatus(
            @Param("block") String block,
            @Param("gp") String gp,
            @Param("villageSahi") String villageSahi,
            @Param("wardNo") String wardNo,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("contact") String contact,
            @Param("fatherSpouseName") String fatherSpouseName,
            @Param("status") Grievance.GrievanceStatus status,
            @Param("name") String name
    );

    @Query("""
SELECT g FROM Grievance g
WHERE g.block = COALESCE(:block, g.block)
AND g.gp = COALESCE(:gp, g.gp)
AND g.villageSahi = COALESCE(:villageSahi, g.villageSahi)
AND g.wardNo = COALESCE(:wardNo, g.wardNo)
AND g.createdAt >= COALESCE(:startOfDay, g.createdAt)
AND g.createdAt < COALESCE(:endOfDay, g.createdAt)
AND g.contact = COALESCE(:contact, g.contact)
AND g.fatherSpouseName ILIKE CONCAT('%', COALESCE(:fatherSpouseName, ''), '%')
AND g.name ILIKE CONCAT('%', COALESCE(:name, ''), '%')
""")
    List<Grievance> findWithoutStatus(
            @Param("block") String block,
            @Param("gp") String gp,
            @Param("villageSahi") String villageSahi,
            @Param("wardNo") String wardNo,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("contact") String contact,
            @Param("fatherSpouseName") String fatherSpouseName,
            @Param("name") String name
    );

    List<Grievance> findTop10ByOrderByUpdatedAtDesc();

    @Query("SELECT COUNT(g) FROM Grievance g WHERE g.collectedBy = :agent")
    Long countByAgent(User agent);

    @Query("SELECT COUNT(g) FROM Grievance g WHERE g.status = :status AND g.collectedBy = :agent")
    Long countByStatusAndAgent(Grievance.GrievanceStatus status, User agent);

    @Query("""
SELECT g
FROM Grievance g
WHERE g.createdAt >= :startOfDay
  AND g.createdAt < :startOfNextDay
  AND g.collectedBy = :agent
""")
    List<Grievance> countTodayByAgent(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay,
            @Param("agent") User agent
    );


    @Query("SELECT g.block, COUNT(g) FROM Grievance g GROUP BY g.block")
    List<Object[]> countByBlock();

    @Query("SELECT g.status, COUNT(g) FROM Grievance g GROUP BY g.status")
    List<Object[]> countByStatus();

    @Query("SELECT DATE(g.createdAt), COUNT(g) FROM Grievance g " +
            "WHERE g.createdAt >= :startDate " +
            "GROUP BY DATE(g.createdAt) " +
            "ORDER BY DATE(g.createdAt)")
    List<Object[]> countByDate(@Param("startDate") LocalDateTime startDate);

    @Query("""
    SELECT DISTINCT g
    FROM Grievance g
    LEFT JOIN FETCH g.attachments a
    WHERE g.id IN :ids
""")
    List<Grievance> findAllWithAttachmentsByIdIn(@Param("ids") List<Long> ids);

}