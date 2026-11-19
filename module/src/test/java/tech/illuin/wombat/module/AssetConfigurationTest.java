package tech.illuin.wombat.module;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusAsset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuration files are kebab-case YAML and the API speaks camelCase JSON, both derived from the same implicit
 * property names by the YAML mapper's naming strategy: these tests guard that nothing in the core modules defeats it.
 */
class AssetConfigurationTest
{
    private static final List<WombatModule> MODULES = StreamSupport.stream(ServiceLoader.load(WombatModule.class).spliterator(), false).toList();
    private static final ObjectMapper YAML = register(YAMLMapper.builder().findAndAddModules().propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE).build());
    private static final ObjectMapper JSON = register(JsonMapper.builder().findAndAddModules().build());
    private static final Path DOCS = Path.of("..", "doc", "module");
    private static final Pattern YAML_BLOCK = Pattern.compile("```yaml\\n(.*?)```", Pattern.DOTALL);
    /** Types serialized as a single value, which have no properties of their own. */
    private static final Set<Class<?>> VALUE_TYPES = Set.of(Country.class, Lifetime.class, Period.class, SecretKey.class);

    /**
     * An explicit multi-word name (e.g. {@code @JsonProperty("someName")}) is used as is by both mappers, so the YAML
     * would expect {@code someName} rather than {@code some-name}.
     */
    @Test
    void naming_isKebabCaseInYamlForEveryProperty()
    {
        List<String> mismatches = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();
        for (WombatModule module : MODULES)
            collectMismatches(JSON.constructType(module.assetClass()), visited, mismatches);

        assertTrue(visited.size() > MODULES.size(), "no nested type was walked");
        assertEquals(List.of(), mismatches);
    }

    /**
     * Every documented asset is read without losing any of its keys: unknown keys are otherwise silently ignored, at the
     * root of an asset because its identity is unwrapped, and anywhere in the app which disables failing on them.
     */
    @Test
    void docs_areReadWithoutIgnoringAnyKey() throws IOException
    {
        Map<String, List<JsonNode>> documented = documentedAssets();
        assertEquals(MODULES.size(), documented.size(), "every module documents its configuration");

        for (List<JsonNode> sources : documented.values())
        {
            for (JsonNode source : sources)
            {
                Asset asset = YAML.treeToValue(source, Asset.class);
                assertKeysRead(source, YAML.valueToTree(asset), asset.identity().id());
            }
        }
    }

    @Test
    void docs_roundTripInYamlAndJson() throws IOException
    {
        for (List<JsonNode> sources : documentedAssets().values())
        {
            for (JsonNode source : sources)
            {
                Asset asset = YAML.treeToValue(source, Asset.class);

                assertEquals(asset, YAML.readValue(YAML.writeValueAsString(asset), Asset.class));
                assertEquals(asset, JSON.readValue(JSON.writeValueAsString(asset), Asset.class));
            }
        }
    }

    @Test
    void persisted_readsKebabCaseAssetsFromBefore015() throws IOException
    {
        KubernetesAPIAsset kubernetes = assertInstanceOf(KubernetesAPIAsset.class, JSON.readValue("""
            {
              "type": "tech.illuin.wombat-module.kubernetes-api",
              "id": "k8s",
              "environment-id": "env",
              "config-path": "/etc/kubeconfig",
              "read-timeout": "PT10S",
              "profile": {"provider": "aws", "instance-type": "m5.large", "location": "FRA", "lifespan": "5y"}
            }
            """, Asset.class));

        assertEquals("env", kubernetes.identity().environmentId());
        assertEquals("/etc/kubeconfig", kubernetes.configPath());
        assertEquals(Duration.ofSeconds(10), kubernetes.readTimeout().orElseThrow());
        assertEquals("m5.large", kubernetes.profile().instanceType());

        LLMPrometheusAsset prometheus = assertInstanceOf(LLMPrometheusAsset.class, JSON.readValue("""
            {
              "type": "tech.illuin.wombat-module.llm-prometheus",
              "id": "prom",
              "environment-id": "env",
              "prometheus-url": "http://prometheus",
              "proxy-url": "http://proxy:8080",
              "username": "wombat",
              "password-key": "PROM_PASSWORD",
              "profile": {"provider": "mistralai", "model": "m", "location": "FRA", "dynamic-profile": {"query": "q"}}
            }
            """, Asset.class));

        assertEquals("http://prometheus", prometheus.prometheusUrl());
        assertEquals("http://proxy:8080", prometheus.proxyUrl());
        assertEquals(SecretKey.of("PROM_PASSWORD"), prometheus.passwordKey());
        assertEquals("q", prometheus.profile().dynamicProfile().query());
    }

    private static void collectMismatches(JavaType type, Set<Class<?>> visited, List<String> mismatches)
    {
        if (type.isContainerType() || type.isReferenceType())
        {
            collectMismatches(type.getContentType(), visited, mismatches);
            return;
        }

        Class<?> raw = type.getRawClass();
        if (raw.isPrimitive() || raw.isEnum() || raw.getName().startsWith("java.") || VALUE_TYPES.contains(raw) || !visited.add(raw))
            return;

        JsonSubTypes subTypes = raw.getAnnotation(JsonSubTypes.class);
        if (subTypes != null)
        {
            for (JsonSubTypes.Type subType : subTypes.value())
                collectMismatches(JSON.constructType(subType.value()), visited, mismatches);
        }

        Map<String, String> yamlNames = properties(YAML, type).stream()
            .collect(Collectors.toMap(BeanPropertyDefinition::getInternalName, BeanPropertyDefinition::getName));
        for (BeanPropertyDefinition property : properties(JSON, type))
        {
            String expected = PropertyNamingStrategies.KebabCaseStrategy.INSTANCE.translate(property.getName());
            String actual = yamlNames.get(property.getInternalName());
            if (!expected.equals(actual))
                mismatches.add(raw.getSimpleName() + "." + property.getInternalName() + " is '" + actual + "' in YAML, expected '" + expected + "'");

            collectMismatches(property.getPrimaryType(), visited, mismatches);
        }
    }

    private static List<BeanPropertyDefinition> properties(ObjectMapper mapper, JavaType type)
    {
        BeanDescription description = mapper.getSerializationConfig().introspect(mapper.constructType(type.getRawClass()));
        return description.findProperties();
    }

    /**
     * @return the documented assets by asset-type, from the YAML blocks of {@code doc/module/*.md} starting with an
     *         {@code id}, the asset-type being named after the file
     */
    private static Map<String, List<JsonNode>> documentedAssets() throws IOException
    {
        Map<String, String> typeByDoc = MODULES.stream()
            .map(module -> module.type().name())
            .collect(Collectors.toMap(name -> name.substring(name.lastIndexOf('.') + 1) + ".md", Function.identity()));

        try (Stream<Path> docs = Files.list(DOCS))
        {
            return docs
                .filter(doc -> typeByDoc.containsKey(doc.getFileName().toString()))
                .collect(Collectors.toMap(doc -> typeByDoc.get(doc.getFileName().toString()), doc -> readAssets(doc, typeByDoc.get(doc.getFileName().toString()))));
        }
    }

    private static List<JsonNode> readAssets(Path doc, String type)
    {
        try {
            List<JsonNode> assets = new ArrayList<>();
            Matcher matcher = YAML_BLOCK.matcher(Files.readString(doc));
            while (matcher.find())
            {
                if (!matcher.group(1).startsWith("id:"))
                    continue;
                ObjectNode asset = (ObjectNode) YAML.readTree(matcher.group(1));
                asset.put("type", type);
                assets.add(asset);
            }
            assertFalse(assets.isEmpty(), "no asset documented in " + doc);
            return assets;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to read " + doc, e);
        }
    }

    private static void assertKeysRead(JsonNode source, JsonNode read, String path)
    {
        if (source.isObject())
        {
            source.properties().forEach(entry -> {
                assertTrue(read.has(entry.getKey()), "'" + entry.getKey() + "' is ignored at " + path);
                assertKeysRead(entry.getValue(), read.get(entry.getKey()), path + "." + entry.getKey());
            });
        }
        else if (source.isArray())
        {
            for (int i = 0 ; i < source.size() ; i++)
                assertKeysRead(source.get(i), read.get(i), path + "[" + i + "]");
        }
    }

    private static <M extends ObjectMapper> M register(M mapper)
    {
        MODULES.forEach(module -> mapper.registerSubtypes(new NamedType(module.assetClass(), module.type().name())));
        return mapper;
    }
}
