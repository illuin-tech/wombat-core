package tech.illuin.wombat.core.asset.type;

/**
 * What a module serves, and the name it is registered under.
 * <p>
 * An {@link AssetType} is defined by its module, which serves the assets of that type; a {@link FamilyType} serves every
 * asset of its family, whatever its asset-type. Both are named {@code namespace.localName}, in a single namespace.
 */
public sealed interface ModuleType permits AssetType, FamilyType
{
    String namespace();

    String localName();

    ServiceFamily family();

    default String name()
    {
        return this.namespace() + "." + this.localName();
    }
}
