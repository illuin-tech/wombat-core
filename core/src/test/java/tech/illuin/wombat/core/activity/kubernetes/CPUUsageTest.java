package tech.illuin.wombat.core.activity.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CPUUsageTest
{
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void factories_createExpectedTypes()
    {
        CPUUsage usageNano = CPUUsage.nanocores(1_000_000_000.0);
        NanocoresCPUUsage nano = assertInstanceOf(NanocoresCPUUsage.class, usageNano);
        assertEquals(1_000_000_000.0, nano.nanocores());

        CPUUsage usageRel = CPUUsage.relative(0.75);
        RelativeCPUUsage rel = assertInstanceOf(RelativeCPUUsage.class, usageRel);
        assertEquals(0.75, rel.ratio());
    }

    @Test
    void jackson_serializesAndDeserializesNanocoresCPUUsage() throws Exception
    {
        CPUUsage usage = CPUUsage.nanocores(2_500_000_000.0);
        String json = mapper.writeValueAsString(usage);

        CPUUsage read = mapper.readValue(json, CPUUsage.class);
        assertInstanceOf(NanocoresCPUUsage.class, read);
        assertEquals(2_500_000_000.0, ((NanocoresCPUUsage) read).nanocores());
    }

    @Test
    void jackson_serializesAndDeserializesRelativeCPUUsage() throws Exception
    {
        CPUUsage usage = CPUUsage.relative(0.85);
        String json = mapper.writeValueAsString(usage);

        CPUUsage read = mapper.readValue(json, CPUUsage.class);
        assertInstanceOf(RelativeCPUUsage.class, read);
        assertEquals(0.85, ((RelativeCPUUsage) read).ratio());
    }

    @Test
    void kubernetesActivityData_supportsCompatibilityConstructor()
    {
        Instant now = Instant.parse("2026-11-01T00:00:00Z");
        TimeRange range = new TimeRange(now, now.plus(Duration.ofHours(1)));

        KubernetesActivityData data = new KubernetesActivityData(
            ActivityRegime.MEASURED,
            Set.of("srv-1"),
            range,
            1_500_000_000.0,
            Map.of("cnt-1", 1.0),
            Map.of("cnt-1", new ClusterInfo("c1", "ns1"))
        );

        assertInstanceOf(NanocoresCPUUsage.class, data.cpuUsage());
        assertEquals(1_500_000_000.0, ((NanocoresCPUUsage) data.cpuUsage()).nanocores());
    }
}
