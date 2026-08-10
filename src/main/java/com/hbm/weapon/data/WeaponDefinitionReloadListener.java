package com.hbm.weapon.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.HbmNuclearTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strict resource loader. Unlike SimpleJsonResourceReloadListener, malformed files are retained as
 * explicit errors and therefore prevent the entire candidate generation from replacing live data.
 */
public final class WeaponDefinitionReloadListener extends SimplePreparableReloadListener<WeaponDefinitionReloadListener.LoadResult> {
    public static final WeaponDefinitionReloadListener INSTANCE = new WeaponDefinitionReloadListener();

    @Override
    protected LoadResult prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, GunDefinition> guns = new LinkedHashMap<>();
        Map<ResourceLocation, AmmoDefinition> ammo = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        loadGuns(resourceManager, guns, errors);
        loadAmmo(resourceManager, ammo, errors);
        return new LoadResult(guns, ammo, errors);
    }

    @Override
    protected void apply(LoadResult result, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (!result.errors().isEmpty()) {
            HbmNuclearTech.LOGGER.error("Rejected weapon definition reload with {} error(s); generation {} remains active.",
                    result.errors().size(), GunDefinitionRegistry.snapshot().getGeneration());
            result.errors().forEach(error -> HbmNuclearTech.LOGGER.error("Weapon definition: {}", error));
            return;
        }

        try {
            GunDefinitionRegistry.Snapshot snapshot = GunDefinitionRegistry.install(result.guns(), result.ammo());
            HbmNuclearTech.LOGGER.info("Installed weapon definition generation {} ({} guns, {} ammo profiles).",
                    snapshot.getGeneration(), snapshot.getGuns().size(), snapshot.getAmmo().size());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            HbmNuclearTech.LOGGER.error("Rejected weapon definition reload; previous generation remains active: {}",
                    exception.getMessage());
        }
    }

    private static void loadGuns(ResourceManager resourceManager, Map<ResourceLocation, GunDefinition> output, List<String> errors) {
        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager
                .listResources("guns", id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            ResourceLocation id = definitionId(file, "guns/");
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                GunDefinition previous = output.put(id, WeaponDefinitionParser.parseGun(id, json));
                if (previous != null) {
                    errors.add("duplicate gun id " + id + " from " + file);
                }
            } catch (Exception exception) {
                errors.add(file + ": " + rootMessage(exception));
            }
        }
    }

    private static void loadAmmo(ResourceManager resourceManager, Map<ResourceLocation, AmmoDefinition> output, List<String> errors) {
        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager
                .listResources("ammo", id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            ResourceLocation id = definitionId(file, "ammo/");
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                AmmoDefinition previous = output.put(id, WeaponDefinitionParser.parseAmmo(id, json));
                if (previous != null) {
                    errors.add("duplicate ammo id " + id + " from " + file);
                }
            } catch (Exception exception) {
                errors.add(file + ": " + rootMessage(exception));
            }
        }
    }

    private static ResourceLocation definitionId(ResourceLocation file, String prefix) {
        String path = file.getPath();
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                path.substring(prefix.length(), path.length() - ".json".length()));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    public record LoadResult(
            Map<ResourceLocation, GunDefinition> guns,
            Map<ResourceLocation, AmmoDefinition> ammo,
            List<String> errors
    ) {
    }

    private WeaponDefinitionReloadListener() {
    }
}
