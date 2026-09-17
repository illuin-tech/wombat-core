package tech.illuin.wombat.core.source.persistence.micrometer;

public final class MicrometerTags
{
    public static final String TAG_ENVIRONMENT = "wombat.environment";
    public static final String TAG_ASSET = "wombat.asset";
    public static final String TAG_SERVICE = "wombat.service";

    public static final String METRIC_K8S_CPU = "wombat.k8s.container.cpu";
    public static final String METRIC_K8S_RAM = "wombat.k8s.container.ram";
    public static final String TAG_K8S_CLUSTER = "wombat.k8s.cluster";
    public static final String TAG_K8S_NAMESPACE = "wombat.k8s.namespace";
    public static final String TAG_K8S_POD = "wombat.k8s.pod";
    public static final String TAG_K8S_CONTAINER = "wombat.k8s.container";

    public static final String METRIC_LLM_OUTPUT_TOKENS = "wombat.llm.output-tokens";
    public static final String TAG_LLM_MODEL = "wombat.llm.model";

    private MicrometerTags() {}
}
