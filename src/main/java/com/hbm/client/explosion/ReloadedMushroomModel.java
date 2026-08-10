package com.hbm.client.explosion;

import com.hbm.HbmNuclearTech;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Lightweight renderer for the original Reloaded mushroom OBJ, preserving its named Stem and Ball parts. */
final class ReloadedMushroomModel {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "models/effect/mush.obj");
    private static volatile Mesh cachedMesh;
    private static volatile boolean loadFailed;

    static void renderPart(String partName, PoseStack.Pose pose, VertexConsumer consumer, int color, float vScroll) {
        Mesh mesh = getMesh();
        if (mesh == null) {
            return;
        }
        mesh.render(partName, pose, consumer, color, vScroll);
    }

    private static Mesh getMesh() {
        Mesh mesh = cachedMesh;
        if (mesh != null || loadFailed) {
            return mesh;
        }
        synchronized (ReloadedMushroomModel.class) {
            if (cachedMesh != null || loadFailed) {
                return cachedMesh;
            }
            try (BufferedReader reader = Minecraft.getInstance().getResourceManager()
                    .getResource(MODEL).orElseThrow().openAsReader()) {
                cachedMesh = Mesh.parse(reader);
            } catch (IOException | RuntimeException exception) {
                loadFailed = true;
                HbmNuclearTech.LOGGER.error("Failed to load Reloaded nuclear mushroom model {}", MODEL, exception);
            }
            return cachedMesh;
        }
    }

    private record Position(float x, float y, float z) {
    }

    private record TextureCoordinate(float u, float v) {
    }

    private record Corner(Position position, TextureCoordinate texture, Position normal) {
    }

    private record Triangle(Corner first, Corner second, Corner third) {
    }

    private record FaceIndex(int position, int texture, int normal) {
        private static FaceIndex parse(String token) {
            String[] indices = token.split("/", -1);
            if (indices.length != 3) {
                throw new IllegalArgumentException("Unsupported OBJ face token: " + token);
            }
            return new FaceIndex(Integer.parseInt(indices[0]) - 1,
                    Integer.parseInt(indices[1]) - 1,
                    Integer.parseInt(indices[2]) - 1);
        }
    }

    private static final class Mesh {
        private final Map<String, List<Triangle>> parts;

        private Mesh(Map<String, List<Triangle>> parts) {
            this.parts = parts;
        }

        private static Mesh parse(BufferedReader reader) throws IOException {
            List<Position> positions = new ArrayList<>();
            List<TextureCoordinate> textureCoordinates = new ArrayList<>();
            List<Position> normals = new ArrayList<>();
            Map<String, List<Triangle>> parts = new HashMap<>();
            String currentPart = "";
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] tokens = trimmed.split("\\s+");
                switch (tokens[0]) {
                    case "v" -> positions.add(new Position(
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])));
                    case "vt" -> textureCoordinates.add(new TextureCoordinate(
                            Float.parseFloat(tokens[1]),
                            1F - Float.parseFloat(tokens[2])));
                    case "vn" -> normals.add(new Position(
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])));
                    case "o", "g" -> currentPart = tokens[1].toLowerCase(Locale.ROOT);
                    case "f" -> {
                        if (tokens.length != 4 || currentPart.isEmpty()) {
                            throw new IllegalArgumentException("Reloaded mushroom OBJ requires named triangle faces");
                        }
                        FaceIndex first = FaceIndex.parse(tokens[1]);
                        FaceIndex second = FaceIndex.parse(tokens[2]);
                        FaceIndex third = FaceIndex.parse(tokens[3]);
                        parts.computeIfAbsent(currentPart, ignored -> new ArrayList<>()).add(new Triangle(
                                corner(first, positions, textureCoordinates, normals),
                                corner(second, positions, textureCoordinates, normals),
                                corner(third, positions, textureCoordinates, normals)));
                    }
                    default -> {
                    }
                }
            }
            if (!parts.containsKey("stem") || !parts.containsKey("ball")) {
                throw new IllegalArgumentException("Reloaded mushroom OBJ is missing Stem or Ball geometry");
            }
            return new Mesh(Map.copyOf(parts));
        }

        private static Corner corner(FaceIndex index, List<Position> positions,
                List<TextureCoordinate> textureCoordinates, List<Position> normals) {
            return new Corner(positions.get(index.position), textureCoordinates.get(index.texture),
                    normals.get(index.normal));
        }

        private void render(String partName, PoseStack.Pose pose, VertexConsumer consumer, int color, float vScroll) {
            List<Triangle> triangles = this.parts.get(partName.toLowerCase(Locale.ROOT));
            if (triangles == null) {
                return;
            }
            for (Triangle triangle : triangles) {
                emit(pose, consumer, triangle.first, color, vScroll);
                emit(pose, consumer, triangle.second, color, vScroll);
                emit(pose, consumer, triangle.third, color, vScroll);
                // Entity render types use quads; the repeated corner closes this source triangle as a degenerate quad.
                emit(pose, consumer, triangle.third, color, vScroll);
            }
        }

        private static void emit(PoseStack.Pose pose, VertexConsumer consumer, Corner corner, int color, float vScroll) {
            consumer.addVertex(pose.pose(), corner.position.x, corner.position.y, corner.position.z)
                    .setColor(color)
                    .setUv(corner.texture.u, corner.texture.v - vScroll)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setNormal(corner.normal.x, corner.normal.y, corner.normal.z)
                    .setLight(LightTexture.FULL_BRIGHT);
        }
    }

    private ReloadedMushroomModel() {
    }
}
