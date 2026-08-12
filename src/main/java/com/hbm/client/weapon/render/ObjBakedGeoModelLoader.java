package com.hbm.client.weapon.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.HbmNuclearTech;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.loading.json.raw.ModelProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Faithful OBJ-to-Gecko bridge. It preserves the original triangles and UVs while exposing each
 * OBJ object as a Gecko bone, allowing modern GeckoLib animation without replacing HBM artwork.
 */
public final class ObjBakedGeoModelLoader {
    private static final Map<ResourceLocation, BakedGeoModel> CACHE = new LinkedHashMap<>();

    public static BakedGeoModel load(ResourceLocation modelResource) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(modelResource, ObjBakedGeoModelLoader::parseResource);
        }
    }

    public static void clearCache(ResourceManager ignored) {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static BakedGeoModel parseResource(ResourceLocation location) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Resource resource = manager.getResource(location)
                .orElseThrow(() -> new IllegalStateException("Missing HBM weapon OBJ " + location));
        try (Reader reader = resource.openAsReader()) {
            return parse(location, reader, loadLegacyRig(manager, location));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to convert HBM weapon OBJ " + location, exception);
        }
    }

    static BakedGeoModel parse(ResourceLocation location, Reader source) throws IOException {
        return parse(location, source, LegacyRig.EMPTY);
    }

    private static BakedGeoModel parse(ResourceLocation location, Reader source, LegacyRig rig) throws IOException {
        List<Vec3> positions = new ArrayList<>();
        List<float[]> textureCoordinates = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        Map<String, ObjGroup> groups = new LinkedHashMap<>();
        ObjGroup current = null;

        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\s+");
                try {
                    switch (fields[0]) {
                        case "o", "g" -> {
                            String name = line.substring(2).trim();
                            if (!name.isEmpty()) {
                                current = groups.computeIfAbsent(name, ObjGroup::new);
                            }
                        }
                        case "v" -> positions.add(new Vec3(
                                Double.parseDouble(fields[1]),
                                Double.parseDouble(fields[2]),
                                Double.parseDouble(fields[3])));
                        case "vt" -> textureCoordinates.add(new float[]{
                                Float.parseFloat(fields[1]),
                                Float.parseFloat(fields[2])});
                        case "vn" -> normals.add(new Vec3(
                                Double.parseDouble(fields[1]),
                                Double.parseDouble(fields[2]),
                                Double.parseDouble(fields[3])));
                        case "f" -> {
                            if (fields.length < 4) {
                                throw new IllegalArgumentException("face has fewer than three vertices");
                            }
                            if (current == null) {
                                current = groups.computeIfAbsent("Gun", ObjGroup::new);
                            }
                            List<ObjIndex> face = new ArrayList<>(fields.length - 1);
                            for (int i = 1; i < fields.length; i++) {
                                face.add(parseIndex(fields[i], positions.size(), textureCoordinates.size(), normals.size()));
                            }
                            for (int i = 1; i < face.size() - 1; i++) {
                                addTriangle(current, face.getFirst(), face.get(i), face.get(i + 1),
                                        positions, textureCoordinates, normals);
                            }
                        }
                        default -> {
                            // Material, smoothing, and metadata statements do not alter geometry.
                        }
                    }
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException(location + " line " + lineNumber + ": " + exception.getMessage(), exception);
                }
            }
        }

        if (groups.values().stream().noneMatch(group -> !group.quads.isEmpty())) {
            throw new IllegalArgumentException(location + " contains no renderable faces");
        }
        return bake(location, groups, rig);
    }

    /**
     * HBM's bus-animation file is also the authoritative rig description. OBJ vertices are stored
     * in model space, while this companion file supplies the authored rotation origins and bone
     * hierarchy. Falling back to a bounds centre is safe for static assets, but it is visibly wrong
     * for charging handles, pumps, and nested ammunition once those groups animate.
     */
    private static LegacyRig loadLegacyRig(ResourceManager manager, ResourceLocation modelLocation) {
        String modelPath = modelLocation.getPath();
        int slash = modelPath.lastIndexOf('/');
        int extension = modelPath.lastIndexOf('.');
        if (slash < 0 || extension <= slash) {
            return LegacyRig.EMPTY;
        }

        String baseName = modelPath.substring(slash + 1, extension);
        List<String> candidates = new ArrayList<>();
        candidates.add("models/weapons/animations/" + baseName + ".json");
        String compactName = baseName.replaceAll("[^A-Za-z0-9_]", "");
        if (!compactName.equals(baseName)) {
            candidates.add("models/weapons/animations/" + compactName + ".json");
        }

        for (String candidate : candidates) {
            ResourceLocation rigLocation = ResourceLocation.fromNamespaceAndPath(
                    modelLocation.getNamespace(), candidate);
            Resource resource = manager.getResource(rigLocation).orElse(null);
            if (resource == null) {
                continue;
            }
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Vec3> pivots = new LinkedHashMap<>();
                JsonObject offsets = root.has("offset") && root.get("offset").isJsonObject()
                        ? root.getAsJsonObject("offset") : null;
                if (offsets != null) {
                    for (Map.Entry<String, JsonElement> entry : offsets.entrySet()) {
                        JsonArray values = entry.getValue().getAsJsonArray();
                        if (values.size() != 3) {
                            throw new IllegalArgumentException("offset for " + entry.getKey() + " must have 3 values");
                        }
                        pivots.put(entry.getKey(), new Vec3(
                                values.get(0).getAsDouble(),
                                values.get(1).getAsDouble(),
                                values.get(2).getAsDouble()));
                    }
                }

                Map<String, String> hierarchy = new LinkedHashMap<>();
                JsonObject hierarchyJson = root.has("hierarchy") && root.get("hierarchy").isJsonObject()
                        ? root.getAsJsonObject("hierarchy") : null;
                if (hierarchyJson != null) {
                    for (Map.Entry<String, JsonElement> entry : hierarchyJson.entrySet()) {
                        hierarchy.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                HbmNuclearTech.LOGGER.debug("Loaded authored weapon rig {} for {}", rigLocation, modelLocation);
                return new LegacyRig(Map.copyOf(pivots), Map.copyOf(hierarchy));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Unable to read HBM weapon rig " + rigLocation, exception);
            }
        }

        HbmNuclearTech.LOGGER.debug("No legacy rig metadata found for {}; using static bounds pivots", modelLocation);
        return LegacyRig.EMPTY;
    }

    private static void addTriangle(ObjGroup group, ObjIndex first, ObjIndex second, ObjIndex third,
                                    List<Vec3> positions, List<float[]> textureCoordinates, List<Vec3> normals) {
        ObjIndex[] indices = {first, second, third, third};
        GeoVertex[] vertices = new GeoVertex[4];
        Vector3f averagedNormal = new Vector3f();
        boolean hasNormals = true;
        for (int i = 0; i < indices.length; i++) {
            ObjIndex index = indices[i];
            Vec3 sourcePosition = positions.get(index.position);
            group.bounds.include(sourcePosition);
            Vector3f bakedPosition = new Vector3f(
                    (float) (-sourcePosition.x / 16.0D),
                    (float) (sourcePosition.y / 16.0D),
                    (float) (sourcePosition.z / 16.0D));
            float[] uv = index.textureCoordinate >= 0
                    ? textureCoordinates.get(index.textureCoordinate)
                    : new float[]{0.0F, 0.0F};
            vertices[i] = new GeoVertex(bakedPosition, uv[0], 1.0F - uv[1]);
            if (i < 3 && index.normal >= 0) {
                Vec3 normal = normals.get(index.normal);
                averagedNormal.add((float) -normal.x, (float) normal.y, (float) normal.z);
            } else if (i < 3) {
                hasNormals = false;
            }
        }

        Vector3f normal;
        if (hasNormals && averagedNormal.lengthSquared() > 1.0E-8F) {
            normal = averagedNormal.normalize();
        } else {
            Vector3f edgeOne = new Vector3f(vertices[1].position()).sub(vertices[0].position());
            Vector3f edgeTwo = new Vector3f(vertices[2].position()).sub(vertices[0].position());
            normal = edgeOne.cross(edgeTwo).normalize();
        }
        Direction direction = Direction.getNearest(normal.x, normal.y, normal.z);
        group.quads.add(new GeoQuad(vertices, normal, direction));
    }

    private static ObjIndex parseIndex(String token, int positionCount, int textureCount, int normalCount) {
        String[] fields = token.split("/", -1);
        int position = resolveIndex(fields[0], positionCount, "position");
        int texture = fields.length > 1 && !fields[1].isEmpty()
                ? resolveIndex(fields[1], textureCount, "texture coordinate")
                : -1;
        int normal = fields.length > 2 && !fields[2].isEmpty()
                ? resolveIndex(fields[2], normalCount, "normal")
                : -1;
        return new ObjIndex(position, texture, normal);
    }

    private static int resolveIndex(String raw, int size, String kind) {
        int parsed = Integer.parseInt(raw);
        int resolved = parsed > 0 ? parsed - 1 : size + parsed;
        if (resolved < 0 || resolved >= size) {
            throw new IllegalArgumentException(kind + " index " + parsed + " is outside 1.." + size);
        }
        return resolved;
    }

    private static BakedGeoModel bake(ResourceLocation location, Map<String, ObjGroup> groups, LegacyRig rig) {
        String rootName = groups.containsKey("Gun")
                ? "Gun"
                : groups.containsKey("MainBody") ? "MainBody" : groups.keySet().iterator().next();
        Map<String, GeoBone> bones = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();
        // Superb Warfare's modern models separate camera/root presentation motion from the actual
        // weapon mechanism. Synthesize that hierarchy around HBM's untouched OBJ groups so its
        // animations can target "root" without baking presentation transforms into the artwork.
        GeoBone camera = new GeoBone(null, "camera", Boolean.FALSE, 0.0D,
                Boolean.FALSE, Boolean.FALSE);
        GeoBone presentationRoot = new GeoBone(camera, "root", Boolean.FALSE, 0.0D,
                Boolean.FALSE, Boolean.FALSE);
        camera.getChildBones().add(presentationRoot);
        SuperbGunRig frameworkRig = SuperbGunRig.find(location).orElse(null);
        boolean normalizedModelSpace = frameworkRig != null
                && !frameworkRig.modelPose().isIdentity();

        // The Star-F OBJ uses HBM's forward-positive-Z convention while Superb Warfare's
        // first-person hands use forward-negative-Z. Keep those transforms on separate bones:
        // animations move the shared Gun controller, model_space fixes only OBJ geometry, and
        // player hands remain in the unmirrored viewmodel coordinate system.
        GeoBone controller = null;
        GeoBone geometryParent = presentationRoot;
        String bakedRootName = rootName;
        if (normalizedModelSpace) {
            controller = new GeoBone(presentationRoot, rootName, Boolean.FALSE, 0.0D,
                    Boolean.FALSE, Boolean.FALSE);
            presentationRoot.getChildBones().add(controller);
            GeoBone modelSpace = new GeoBone(controller, "model_space", Boolean.FALSE, 0.0D,
                    Boolean.FALSE, Boolean.FALSE);
            SuperbGunRig.ModelPose modelPose = frameworkRig.modelPose();
            // Gecko negates animation-space X during rendering. Store author-facing X as the
            // intuitive visual direction so increasing it moves the mesh right on screen.
            modelSpace.setPosX((float) -modelPose.translation().x);
            modelSpace.setPosY((float) modelPose.translation().y);
            modelSpace.setPosZ((float) modelPose.translation().z);
            modelSpace.setRotX((float) Math.toRadians(modelPose.rotationDegrees().x));
            modelSpace.setRotY((float) Math.toRadians(modelPose.rotationDegrees().y));
            modelSpace.setRotZ((float) Math.toRadians(modelPose.rotationDegrees().z));
            modelSpace.setScaleX(modelPose.scale());
            modelSpace.setScaleY(modelPose.scale());
            modelSpace.setScaleZ(modelPose.scale());
            controller.getChildBones().add(modelSpace);
            geometryParent = modelSpace;
            bakedRootName = rootName + "_mesh";
        }

        GeoBone root = bakeBoneTree(rootName, rootName, groups, rig, bones, visiting,
                geometryParent, bakedRootName);
        for (String groupName : groups.keySet()) {
            bakeBoneTree(groupName, rootName, groups, rig, bones, visiting,
                    geometryParent, bakedRootName);
        }
        if (frameworkRig != null) {
            GeoBone handParent = normalizedModelSpace ? controller : root;
            frameworkRig.virtualBones().forEach(anchor -> {
                GeoBone anchorParent = anchor.role() == SuperbGunRig.BoneRole.MUZZLE_FLASH
                        ? root : handParent;
                GeoBone hand = new GeoBone(anchorParent, anchor.name(), Boolean.FALSE, 0.0D,
                        Boolean.FALSE, Boolean.FALSE);
                hand.setPivotX((float) anchor.pivot().x);
                hand.setPivotY((float) anchor.pivot().y);
                hand.setPivotZ((float) anchor.pivot().z);
                hand.setRotX((float) Math.toRadians(anchor.rotationDegrees().x));
                hand.setRotY((float) Math.toRadians(anchor.rotationDegrees().y));
                hand.setRotZ((float) Math.toRadians(anchor.rotationDegrees().z));
                anchorParent.getChildBones().add(hand);
            });
        }
        saveSnapshots(camera);

        ModelProperties properties = new ModelProperties(
                null, null, null, null, null, null, null, null, null, null,
                "geometry.hbm.obj." + location.getPath().replace('/', '.'),
                Boolean.TRUE,
                256.0D,
                256.0D,
                null,
                new double[]{0.0D, 0.0D, 0.0D},
                null
        );
        HbmNuclearTech.LOGGER.debug("Converted {} OBJ groups from {} into GeckoLib bones.", groups.size(), location);
        return new BakedGeoModel(Collections.singletonList(camera), properties);
    }

    private static GeoBone bakeBoneTree(String name, String rootName, Map<String, ObjGroup> groups,
                                        LegacyRig rig, Map<String, GeoBone> bones, Set<String> visiting,
                                        GeoBone presentationRoot, String bakedRootName) {
        GeoBone existing = bones.get(name);
        if (existing != null) {
            return existing;
        }
        ObjGroup group = groups.get(name);
        if (group == null) {
            return bones.get(rootName);
        }
        if (!visiting.add(name)) {
            throw new IllegalArgumentException("Cyclic HBM weapon hierarchy at bone " + name);
        }

        boolean root = name.equals(rootName);
        GeoBone parent = presentationRoot;
        if (!root) {
            String requestedParent = rig.hierarchy().getOrDefault(name, rootName);
            if (requestedParent.equals(name) || !groups.containsKey(requestedParent)) {
                requestedParent = rootName;
            }
            parent = bakeBoneTree(requestedParent, rootName, groups, rig, bones, visiting,
                    presentationRoot, bakedRootName);
        }
        GeoBone bone = bakeBone(parent, group, root, rig.pivots().get(name),
                root ? bakedRootName : group.name);
        bones.put(name, bone);
        parent.getChildBones().add(bone);
        visiting.remove(name);
        return bone;
    }

    private static GeoBone bakeBone(GeoBone parent, ObjGroup group, boolean root, Vec3 authoredPivot,
                                    String bakedName) {
        GeoBone bone = new GeoBone(parent, bakedName, Boolean.FALSE, 0.0D, Boolean.FALSE, Boolean.FALSE);
        if (!root && group.bounds.initialized) {
            Vec3 pivot = authoredPivot != null
                    ? authoredPivot
                    : new Vec3(group.bounds.centerX(), group.bounds.centerY(), group.bounds.centerZ());
            bone.setPivotX((float) -pivot.x);
            bone.setPivotY((float) pivot.y);
            bone.setPivotZ((float) pivot.z);
        }
        if (!group.quads.isEmpty()) {
            Vec3 size = group.bounds.initialized
                    ? new Vec3(group.bounds.sizeX(), group.bounds.sizeY(), group.bounds.sizeZ())
                    : Vec3.ZERO;
            bone.getCubes().add(new GeoCube(
                    group.quads.toArray(GeoQuad[]::new),
                    Vec3.ZERO,
                    Vec3.ZERO,
                    size,
                    0.0D,
                    false
            ));
        }
        return bone;
    }

    private static void saveSnapshots(GeoBone bone) {
        bone.saveInitialSnapshot();
        bone.getChildBones().forEach(ObjBakedGeoModelLoader::saveSnapshots);
    }

    private record ObjIndex(int position, int textureCoordinate, int normal) {
    }

    private record LegacyRig(Map<String, Vec3> pivots, Map<String, String> hierarchy) {
        private static final LegacyRig EMPTY = new LegacyRig(Map.of(), Map.of());
    }

    private static final class ObjGroup {
        private final String name;
        private final List<GeoQuad> quads = new ArrayList<>();
        private final Bounds bounds = new Bounds();

        private ObjGroup(String name) {
            this.name = name;
        }
    }

    private static final class Bounds {
        private boolean initialized;
        private double minX;
        private double minY;
        private double minZ;
        private double maxX;
        private double maxY;
        private double maxZ;

        private void include(Vec3 point) {
            if (!initialized) {
                minX = maxX = point.x;
                minY = maxY = point.y;
                minZ = maxZ = point.z;
                initialized = true;
                return;
            }
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        private double centerX() { return (minX + maxX) * 0.5D; }
        private double centerY() { return (minY + maxY) * 0.5D; }
        private double centerZ() { return (minZ + maxZ) * 0.5D; }
        private double sizeX() { return maxX - minX; }
        private double sizeY() { return maxY - minY; }
        private double sizeZ() { return maxZ - minZ; }
    }

    private ObjBakedGeoModelLoader() {
    }
}
