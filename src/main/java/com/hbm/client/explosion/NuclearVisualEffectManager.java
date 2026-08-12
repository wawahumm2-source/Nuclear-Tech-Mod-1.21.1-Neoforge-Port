package com.hbm.client.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.client.render.HbmNuclearRenderTypes;
import com.hbm.config.HbmClientConfig;
import com.hbm.network.ClientEffectPayload;
import com.hbm.network.NuclearProgressPayload;
import com.hbm.world.explosion.HbmExplosionService;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Routes the NTM Extended nuclear cloud and the separate modern Fallout Rain event. */
public final class NuclearVisualEffectManager {
    private static final ResourceLocation LITTLE_BOY_EFFECT = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "little_boy");
    private static final ResourceLocation FALLOUT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "textures/entity/fallout.png");
    private static final List<ExtendedTorexVisual> ACTIVE = new ArrayList<>();
    private static final Map<BlockPos, FalloutRainVisual> ACTIVE_FALLOUT = new LinkedHashMap<>();

    public static void accept(ClientEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        VisualProfile profile = VisualProfile.from(payload.effect(), payload.variant());
        if (profile == null) {
            return;
        }
        Vec3 origin = new Vec3(payload.x(), payload.y(), payload.z());
        NuclearPresentationOverlay.beginFlash(minecraft.level, origin, profile.flashRange());
        ACTIVE.add(new ExtendedTorexVisual(minecraft.level, origin, profile.sourceRadius()));
    }

    public static void acceptProgress(NuclearProgressPayload payload) {
        NuclearPresentationOverlay.acceptProgress(payload);
        if (payload.stage() != NuclearProgressPayload.Stage.FALLOUT || payload.radius() <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlockPos origin = BlockPos.of(payload.origin());
        ACTIVE_FALLOUT.computeIfAbsent(origin,
                key -> new FalloutRainVisual(minecraft.level, Vec3.atCenterOf(key), payload.radius()))
                .refresh(payload.percent());
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            ACTIVE_FALLOUT.clear();
            NuclearPresentationOverlay.clear();
            return;
        }
        NuclearPresentationOverlay.tick();
        for (Iterator<ExtendedTorexVisual> iterator = ACTIVE.iterator(); iterator.hasNext();) {
            ExtendedTorexVisual visual = iterator.next();
            if (visual.level != minecraft.level || !visual.tick(minecraft)) {
                iterator.remove();
            }
        }
        for (Iterator<Map.Entry<BlockPos, FalloutRainVisual>> iterator = ACTIVE_FALLOUT.entrySet().iterator();
                iterator.hasNext();) {
            FalloutRainVisual visual = iterator.next().getValue();
            if (visual.level != minecraft.level || !visual.tick(minecraft)) {
                iterator.remove();
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER
                || ACTIVE.isEmpty() && ACTIVE_FALLOUT.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        for (ExtendedTorexVisual visual : ACTIVE) {
            if (visual.level == minecraft.level) {
                visual.render(buffer, event.getCamera(), partialTick);
            }
        }
        for (FalloutRainVisual visual : ACTIVE_FALLOUT.values()) {
            if (visual.level == minecraft.level) {
                visual.render(buffer, event.getCamera(), partialTick);
            }
        }
        buffer.endBatch();
    }

    /** Modern presentation of the invisible source Fallout Rain job. */
    private static final class FalloutRainVisual {
        private final ClientLevel level;
        private final Vec3 origin;
        private final int radius;
        private final RandomSource random;
        private int updateGraceTicks;
        private int soundCooldown;

        private FalloutRainVisual(ClientLevel level, Vec3 origin, int radius) {
            this.level = level;
            this.origin = origin;
            this.radius = radius;
            this.random = RandomSource.create(BlockPos.containing(origin).asLong() ^ 0x46A11A7EL);
        }

        private void refresh(int percent) {
            this.updateGraceTicks = percent >= 100 ? 60 : 40;
        }

        private boolean tick(Minecraft minecraft) {
            if (this.updateGraceTicks-- <= 0) {
                return false;
            }
            Player player = minecraft.player;
            if (player == null || player.position().distanceToSqr(this.origin)
                    > (this.radius + 24D) * (this.radius + 24D)) {
                return true;
            }
            if (this.soundCooldown > 0) {
                this.soundCooldown--;
            }
            float volume = HbmClientConfig.CLIENT.nuclearFalloutRainSoundVolume.get().floatValue();
            if (volume > 0F && this.soundCooldown <= 0
                    && player.position().distanceToSqr(this.origin) <= (double) this.radius * this.radius) {
                this.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WEATHER_RAIN, SoundSource.WEATHER,
                        volume, 0.82F + this.random.nextFloat() * 0.18F, false);
                this.soundCooldown = 35 + this.random.nextInt(16);
            }
            return true;
        }

        private void render(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.position().distanceToSqr(this.origin) > (double) this.radius * this.radius
                    || !HbmClientConfig.CLIENT.enableNuclearFalloutRain.get()) {
                return;
            }
            int range = switch (HbmClientConfig.CLIENT.nuclearVisualQuality.get()) {
                case FULL -> 10;
                case REDUCED -> 6;
                case MINIMAL -> 0;
            };
            float density = HbmClientConfig.CLIENT.nuclearFalloutRainDensity.get().floatValue();
            if (range <= 0 || density <= 0F) {
                return;
            }
            VertexConsumer consumer = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_CLOUD.apply(FALLOUT_TEXTURE));
            Matrix4f matrix = new PoseStack().last().pose();
            Vec3 cameraPosition = camera.getPosition();
            int cameraX = Mth.floor(cameraPosition.x);
            int cameraY = Mth.floor(cameraPosition.y);
            int cameraZ = Mth.floor(cameraPosition.z);
            float scroll = ((player.tickCount & 511) + partialTick) / 512F;
            int stride = density >= 0.75F ? 1 : density >= 0.35F ? 2 : 3;
            for (int z = cameraZ - range; z <= cameraZ + range; z += stride) {
                for (int x = cameraX - range; x <= cameraX + range; x += stride) {
                    renderRainColumn(consumer, matrix, cameraPosition, cameraY, x, z, range, density, scroll);
                }
            }
        }

        private void renderRainColumn(VertexConsumer consumer, Matrix4f matrix, Vec3 cameraPosition,
                int cameraY, int x, int z, int range, float density, float scroll) {
            double worldX = x + 0.5D;
            double worldZ = z + 0.5D;
            double originX = worldX - this.origin.x;
            double originZ = worldZ - this.origin.z;
            if (originX * originX + originZ * originZ > (double) this.radius * this.radius) {
                return;
            }
            double cameraX = worldX - cameraPosition.x;
            double cameraZ = worldZ - cameraPosition.z;
            double horizontalDistance = Math.max(0.001D, Math.hypot(cameraX, cameraZ));
            float sideX = (float) (-cameraZ / horizontalDistance * 0.5D);
            float sideZ = (float) (cameraX / horizontalDistance * 0.5D);
            int surface = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int minY = Math.max(surface, cameraY - range);
            int maxY = Math.max(minY, cameraY + range);
            if (minY == maxY) {
                return;
            }
            long seed = x * (long) x * 3121L + x * 45238971L ^ z * (long) z * 418711L + z * 13761L;
            this.random.setSeed(seed);
            float variation = 0.4F + this.random.nextFloat() * 0.2F;
            float sway = this.random.nextFloat();
            float distanceFraction = (float) Math.min(1D, horizontalDistance / range);
            float alpha = ((1F - distanceFraction * distanceFraction) * 0.3F + 0.5F)
                    * Math.min(1F, density);
            float minV = minY / 4F + scroll + sway;
            float maxV = maxY / 4F + scroll + sway;
            float localX = (float) cameraX;
            float localMinY = (float) (minY - cameraPosition.y);
            float localMaxY = (float) (maxY - cameraPosition.y);
            float localZ = (float) cameraZ;
            int color = packColor(1F, 1F, 1F, alpha);
            consumer.addVertex(matrix, localX - sideX, localMinY, localZ - sideZ)
                    .setColor(color).setUv(variation, minV).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setNormal(0F, 1F, 0F).setLight(240);
            consumer.addVertex(matrix, localX + sideX, localMinY, localZ + sideZ)
                    .setColor(color).setUv(1F + variation, minV).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setNormal(0F, 1F, 0F).setLight(240);
            consumer.addVertex(matrix, localX + sideX, localMaxY, localZ + sideZ)
                    .setColor(color).setUv(1F + variation, maxV).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setNormal(0F, 1F, 0F).setLight(240);
            consumer.addVertex(matrix, localX - sideX, localMaxY, localZ - sideZ)
                    .setColor(color).setUv(variation, maxV).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setNormal(0F, 1F, 0F).setLight(240);
        }
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = Math.round(Mth.clamp(red, 0F, 1F) * 255F);
        int g = Math.round(Mth.clamp(green, 0F, 1F) * 255F);
        int b = Math.round(Mth.clamp(blue, 0F, 1F) * 255F);
        int a = Math.round(Mth.clamp(alpha, 0F, 1F) * 255F);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private record VisualProfile(float sourceRadius, float flashRange) {
        private static VisualProfile from(ResourceLocation effect, int variant) {
            if (effect.equals(LITTLE_BOY_EFFECT)) {
                return new VisualProfile(120F, 300F);
            }
            if (effect.equals(HbmExplosionService.PROTOTYPE_NUKE_EFFECT)) {
                float sourceRadius = Math.max(1, variant);
                float scale = ExtendedTorexTimeline.scale(sourceRadius);
                return new VisualProfile(sourceRadius, Math.max(64F, scale * 128F));
            }
            return null;
        }
    }

    private NuclearVisualEffectManager() {
    }
}
