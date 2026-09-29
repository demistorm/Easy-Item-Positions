package win.demistorm.mcpositioneditor.editor;

import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// Central store for override/load-time data keyed by model id (parents propagate to children)
public final class TransformOverrideManager {

    private TransformOverrideManager() {}

    public static final ItemDisplayContext[] EDITOR_CONTEXTS = {
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
        ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
        ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
        ItemDisplayContext.HEAD,
        ItemDisplayContext.GUI,
        ItemDisplayContext.GROUND,
        ItemDisplayContext.FIXED,
        ItemDisplayContext.ON_SHELF
    };

    private static final Map<Identifier, Map<ItemDisplayContext, ItemTransform>> OVERRIDES = new ConcurrentHashMap<>();

    private static final Map<Identifier, Map<ItemDisplayContext, Identifier>> TRANSFORM_PROVIDER = new ConcurrentHashMap<>();

    private static final Map<Identifier, List<Identifier>> VARIANT_FAMILY = new ConcurrentHashMap<>();

    private static volatile Map<Identifier, UnbakedModel> RAW_MODELS = Map.of();

    private static final Map<CuboidItemModelWrapper, Identifier> WRAPPER_IDS = new ConcurrentHashMap<>();

    private static final Map<SpecialModelWrapper<?>, Identifier> SPECIAL_WRAPPER_IDS = new ConcurrentHashMap<>();

    public static void captureMetadata(Map<Identifier, UnbakedModel> rawModels,
                                       ClientItemInfoLoader.LoadedClientInfos loadedClientInfos) {
        WRAPPER_IDS.clear();
        SPECIAL_WRAPPER_IDS.clear();
        TRANSFORM_PROVIDER.clear();
        VARIANT_FAMILY.clear();

        Map<Identifier, UnbakedModel> snapshot = new HashMap<>(rawModels);
        RAW_MODELS = Collections.unmodifiableMap(snapshot);

        for (Identifier id : snapshot.keySet()) {
            Map<ItemDisplayContext, Identifier> providers = new EnumMap<>(ItemDisplayContext.class);
            for (ItemDisplayContext ctx : EDITOR_CONTEXTS) {
                Identifier provider = findProvider(snapshot, id, ctx);
                if (provider != null) {
                    providers.put(ctx, provider);
                }
            }
            if (!providers.isEmpty()) {
                TRANSFORM_PROVIDER.put(id, Collections.unmodifiableMap(providers));
            }
        }

        loadedClientInfos.contents().forEach((itemId, clientItem) -> {
            Set<Identifier> family = new HashSet<>();
            collectFamily(clientItem.model(), family);
            if (!family.isEmpty()) {
                List<Identifier> sorted = new ArrayList<>(family);
                sorted.sort(Identifier::compareTo);
                VARIANT_FAMILY.put(itemId, Collections.unmodifiableList(sorted));
            }
        });

        log.debug("Captured model metadata: {} raw models, {} item families, {} providers",
            snapshot.size(), VARIANT_FAMILY.size(), TRANSFORM_PROVIDER.size());
    }

    private static void collectFamily(ItemModel.Unbaked node, Set<Identifier> out) {
        switch (node) {
            case CuboidItemModelWrapper.Unbaked block -> out.add(block.model());
            case SelectItemModel.Unbaked select -> {
                select.unbakedSwitch().cases().forEach(c -> collectFamily(c.model(), out));
                select.fallback().ifPresent(f -> collectFamily(f, out));
            }
            case RangeSelectItemModel.Unbaked range -> {
                range.entries().forEach(e -> collectFamily(e.model(), out));
                range.fallback().ifPresent(f -> collectFamily(f, out));
            }
            case ConditionalItemModel.Unbaked conditional -> {
                collectFamily(conditional.onTrue(), out);
                collectFamily(conditional.onFalse(), out);
            }
            case CompositeModel.Unbaked composite -> composite.models().forEach(m -> collectFamily(m, out));
            case SpecialModelWrapper.Unbaked special -> out.add(special.base());
            default -> { /* anything else - not editable */ }
        }
    }

    private static Identifier findProvider(Map<Identifier, UnbakedModel> models, Identifier start, ItemDisplayContext ctx) {
        Set<Identifier> seen = new HashSet<>();
        Identifier cur = start;
        while (cur != null && seen.add(cur)) {
            UnbakedModel unbaked = models.get(cur);
            if (unbaked == null) {
                return null;
            }
            ItemTransforms transforms = unbaked.transforms();
            if (transforms != null && transforms.getTransform(ctx) != ItemTransform.NO_TRANSFORM) {
                return cur;
            }
            cur = unbaked.parent();
        }
        return null;
    }

    public static ItemTransform resolveOriginal(Identifier modelId, ItemDisplayContext ctx) {
        if (modelId == null) {
            return ItemTransform.NO_TRANSFORM;
        }
        Map<Identifier, UnbakedModel> models = RAW_MODELS;
        Set<Identifier> seen = new HashSet<>();
        Identifier cur = modelId;
        while (cur != null && seen.add(cur)) {
            UnbakedModel unbaked = models.get(cur);
            if (unbaked == null) {
                return ItemTransform.NO_TRANSFORM;
            }
            ItemTransforms transforms = unbaked.transforms();
            if (transforms != null) {
                ItemTransform t = transforms.getTransform(ctx);
                if (t != ItemTransform.NO_TRANSFORM) {
                    return t;
                }
            }
            cur = unbaked.parent();
        }
        return ItemTransform.NO_TRANSFORM;
    }

    public static ItemTransform resolveOverride(Identifier modelId, ItemDisplayContext ctx) {
        if (modelId == null) {
            return null;
        }
        Map<ItemDisplayContext, ItemTransform> own = OVERRIDES.get(modelId);
        if (own != null) {
            ItemTransform t = own.get(ctx);
            if (t != null) {
                return t;
            }
        }
        Map<ItemDisplayContext, Identifier> providers = TRANSFORM_PROVIDER.get(modelId);
        if (providers != null) {
            Identifier provider = providers.get(ctx);
            if (provider != null && !provider.equals(modelId)) {
                Map<ItemDisplayContext, ItemTransform> providerOverrides = OVERRIDES.get(provider);
                if (providerOverrides != null) {
                    return providerOverrides.get(ctx);
                }
            }
        }
        return null;
    }

    public static void setOverride(Identifier modelId, ItemDisplayContext ctx, ItemTransform transform) {
        OVERRIDES.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>()).put(ctx, copy(transform));
        OverrideStorage.markDirty();
    }

    public static void clearOverride(Identifier modelId, ItemDisplayContext ctx) {
        Map<ItemDisplayContext, ItemTransform> own = OVERRIDES.get(modelId);
        if (own != null) {
            own.remove(ctx);
            if (own.isEmpty()) {
                OVERRIDES.remove(modelId);
            }
            OverrideStorage.markDirty();
        }
    }

    public static void clearAll() {
        if (!OVERRIDES.isEmpty()) {
            OVERRIDES.clear();
            OverrideStorage.markDirty();
        }
    }

    public static void removeOverrides(Collection<Identifier> modelIds) {
        boolean removed = false;
        for (Identifier id : modelIds) {
            removed |= OVERRIDES.remove(id) != null;
        }
        if (removed) {
            OverrideStorage.markDirty();
        }
    }

    public static Map<Identifier, Map<ItemDisplayContext, ItemTransform>> overrides() {
        return OVERRIDES;
    }

    public static Identifier providerOf(Identifier modelId, ItemDisplayContext ctx) {
        if (modelId == null) {
            return null;
        }
        Map<ItemDisplayContext, Identifier> providers = TRANSFORM_PROVIDER.get(modelId);
        return providers == null ? null : providers.get(ctx);
    }

    public static List<Identifier> familyOf(Identifier itemId) {
        return VARIANT_FAMILY.getOrDefault(itemId, List.of());
    }

    public static Identifier itemModelId(ItemStack stack) {
        return stack.get(DataComponents.ITEM_MODEL);
    }

    public static void registerWrapper(CuboidItemModelWrapper wrapper, Identifier modelId) {
        WRAPPER_IDS.put(wrapper, modelId);
    }

    public static Identifier wrapperId(CuboidItemModelWrapper wrapper) {
        return WRAPPER_IDS.get(wrapper);
    }

    public static void registerSpecialWrapper(SpecialModelWrapper<?> wrapper, Identifier baseModelId) {
        SPECIAL_WRAPPER_IDS.put(wrapper, baseModelId);
    }

    public static Identifier specialWrapperId(SpecialModelWrapper<?> wrapper) {
        return SPECIAL_WRAPPER_IDS.get(wrapper);
    }

    public static ItemTransform copy(ItemTransform t) {
        return new ItemTransform(
            new Vector3f(t.rotation()),
            new Vector3f(t.translation()),
            new Vector3f(t.scale()));
    }
}
