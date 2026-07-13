package tech.illuin.wombat.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import tech.illuin.wombat.persistence.model.ContainerLocation;
import tech.illuin.wombat.persistence.model.ServerMetricEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@ApplicationScoped
public class ServerMetricRepository implements PanacheRepositoryBase<ServerMetricEntity, Long>
{

    @Transactional
    public void save(ServerMetricEntity entity)
    {
        persist(entity);
    }

    public List<ServerMetricEntity> findByRange(long startMs, long endMs)
    {
        return find("instantMs >= ?1 AND instantMs <= ?2", startMs, endMs).list();
    }

    public List<ServerMetricEntity> findByRangeAndClusters(long startMs, long endMs, List<String> clusterIds)
    {
        if (clusterIds.isEmpty()) return findByRange(startMs, endMs);
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM server_metrics WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);

        Query query = bind(getEntityManager().createNativeQuery(sql.toString(), ServerMetricEntity.class), startMs, endMs, clusterIds);
        @SuppressWarnings("unchecked")
        List<ServerMetricEntity> rows = query.getResultList();
        return rows;
    }

    public OptionalDouble averageCpuPerInstant(long startMs, long endMs, List<String> clusterIds)
    {
        StringBuilder sql = new StringBuilder(
            "SELECT AVG(perInstant) FROM ("
            + " SELECT SUM(cpu_nanocores) AS perInstant FROM server_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);
        sql.append(" GROUP BY instantMs)");

        Query query = bind(getEntityManager().createNativeQuery(sql.toString()), startMs, endMs, clusterIds);
        Object result = query.getSingleResult();
        return result == null ? OptionalDouble.empty() : OptionalDouble.of(((Number) result).doubleValue());
    }

    public Map<String, Double> containerShares(long startMs, long endMs, List<String> clusterIds)
    {
        StringBuilder sql = new StringBuilder(
            "SELECT json_extract(data, '$.container') AS container,"
            + " SUM(cpu_nanocores) * 1.0 / SUM(SUM(cpu_nanocores)) OVER () AS share FROM server_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);
        sql.append(" GROUP BY json_extract(data, '$.container')");

        Query query = bind(getEntityManager().createNativeQuery(sql.toString()), startMs, endMs, clusterIds);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<String, Double> shares = new LinkedHashMap<>();
        for (Object[] row : rows)
        {
            // share is NULL only when the total CPU is zero (division by zero in SQL)
            if (row[1] == null) return Map.of();
            shares.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        return shares;
    }

    public Map<String, List<ContainerLocation>> containerLocations(long startMs, long endMs, List<String> clusterIds)
    {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT json_extract(data, '$.container') AS container,"
            + " json_extract(data, '$.cluster') AS cluster,"
            + " json_extract(data, '$.namespace') AS namespace FROM server_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);
        sql.append(" ORDER BY container, cluster, namespace");

        Query query = bind(getEntityManager().createNativeQuery(sql.toString()), startMs, endMs, clusterIds);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<String, List<ContainerLocation>> locations = new LinkedHashMap<>();
        for (Object[] row : rows)
        {
            locations.computeIfAbsent((String) row[0], k -> new ArrayList<>())
                .add(new ContainerLocation((String) row[1], (String) row[2]));
        }
        return locations;
    }

    private static void appendClusterFilter(StringBuilder sql, List<String> clusterIds)
    {
        if (!clusterIds.isEmpty()) sql.append(" AND json_extract(data, '$.cluster') IN (:clusters)");
    }

    private static Query bind(Query query, long startMs, long endMs, List<String> clusterIds)
    {
        query.setParameter("start", startMs);
        query.setParameter("end", endMs);
        if (!clusterIds.isEmpty()) query.setParameter("clusters", clusterIds);
        return query;
    }
}
