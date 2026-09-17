package tech.illuin.wombat.core.source.persistence.micrometer;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.source.data.KubernetesData;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.MappedMicrometerMetricPersister;

import java.util.Collection;
import java.util.List;

import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

public class OldMicrometerMetricPersister implements WombatMetricPersister
{
    private final MeterRegistry registry;

    private static final Logger logger = LoggerFactory.getLogger(MappedMicrometerMetricPersister.class);

    public OldMicrometerMetricPersister(MeterRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public void persist(Collection<MetricData> metrics)
    {
        this.persistKubernetes(metrics.stream()
            .filter(metric -> metric instanceof KubernetesData)
            .map(metric -> (KubernetesData) metric)
            .toList()
        );

        this.persistLLM(metrics.stream()
            .filter(metric -> metric instanceof LLMData)
            .map(metric -> (LLMData) metric)
            .toList()
        );
    }

    private void persistKubernetes(Collection<KubernetesData> metrics)
    {
        logger.debug("Recording kubernetes metrics for {} pods in {}/{}", metrics.size());
        for (KubernetesData k8sData : metrics)
        {
            logger.trace("Current metrics {}", k8sData);
            List<Tag> tags = createK8STags(k8sData);
            record(METRIC_K8S_CPU, k8sData.cpuNanocores(), tags);
            record(METRIC_K8S_RAM, k8sData.ramBytes(), tags);
        }
    }

    private void persistLLM(Collection<LLMData> metrics)
    {
        logger.debug("Recording llm metrics for {} pods in {}/{}", metrics.size());
        for (LLMData llmData : metrics)
        {
            logger.trace("Current metrics {}", llmData);
            List<Tag> tags = createLLMTags(llmData);
            record(METRIC_LLM_OUTPUT_TOKENS, llmData.outputTokens(), tags);
        }
    }

    private void record(String metric, double value, Collection<Tag> tags)
    {
        DistributionSummary.builder(metric)
            .tags(tags)
            .register(this.registry)
            .record(value);
    }

    private static List<Tag> createK8STags(KubernetesData data)
    {
        return List.of(
            Tag.of(TAG_ENVIRONMENT, data.environmentId()),
            Tag.of(TAG_ASSET, data.assetId()),
            Tag.of(TAG_SERVICE, data.serviceId()),
            Tag.of(TAG_K8S_CLUSTER, data.cluster()),
            Tag.of(TAG_K8S_NAMESPACE, data.namespace()),
            Tag.of(TAG_K8S_POD, data.pod()),
            Tag.of(TAG_K8S_CONTAINER, data.serviceId())
        );
    }

    private static List<Tag> createLLMTags(LLMData data)
    {
        return List.of(
            Tag.of(TAG_ENVIRONMENT, data.environmentId()),
            Tag.of(TAG_ASSET, data.assetId()),
            Tag.of(TAG_SERVICE, data.serviceId()),
            Tag.of(TAG_LLM_MODEL, data.model())
        );
    }
}
