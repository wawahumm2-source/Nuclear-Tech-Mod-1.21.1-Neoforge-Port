package com.hbm.client.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.client.render.HbmNuclearRenderTypes;
import com.hbm.config.HbmClientConfig;
import com.hbm.network.ClientEffectPayload;
import com.hbm.network.NuclearProgressPayload;
import com.hbm.registry.HbmSounds;
import com.hbm.world.explosion.HbmExplosionService;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
import org.joml.Vector3f;

/** Client-side Reloaded 1.12.2 mushroom, modern pressure front, and Fallout Rain presentation. */
public final class NuclearVisualEffectManager {
    private static final ResourceLocation LITTLE_BOY_EFFECT =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "little_boy");
    private static final ResourceLocation CLOUDLET_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/particle/particle_base.png");
    private static final ResourceLocation RELOADED_FIREBALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/particle/explosion_1_12.png");
    private static final ResourceLocation FALLOUT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/entity/fallout.png");
    private static final ResourceLocation FLARE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/particle/flare.png");
    private static final Map<VisualKey, NuclearVisual> ACTIVE = new LinkedHashMap<>();
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
        VisualKey key = new VisualKey(payload.effect(), BlockPos.containing(origin));
        if (ACTIVE.containsKey(key)) {
            return;
        }
        NuclearPresentationOverlay.beginFlash(minecraft.level, origin, profile.flashRange());
        ACTIVE.put(key, new NuclearVisual(minecraft.level, origin, profile));
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
        for (Iterator<Map.Entry<VisualKey, NuclearVisual>> iterator = ACTIVE.entrySet().iterator(); iterator.hasNext();) {
            NuclearVisual visual = iterator.next().getValue();
            if (visual.level != minecraft.level || !visual.tick(minecraft)) {
                iterator.remove();
            }
        }
        for (Iterator<Map.Entry<BlockPos, FalloutRainVisual>> iterator = ACTIVE_FALLOUT.entrySet().iterator(); iterator.hasNext();) {
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
        for (NuclearVisual visual : ACTIVE.values()) {
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

    private static void renderBillboard(Matrix4f matrix, VertexConsumer consumer, Vector3f cameraLeft,
            Vector3f cameraUp, double positionX, double positionY, double positionZ,
            float scale, float red, float green, float blue, float alpha) {
        renderBillboard(matrix, consumer, cameraLeft, cameraUp, positionX, positionY, positionZ,
                scale, red, green, blue, alpha, 0F, 0F, 1F, 1F);
    }

    private static void renderBillboard(Matrix4f matrix, VertexConsumer consumer, Vector3f cameraLeft,
            Vector3f cameraUp, double positionX, double positionY, double positionZ,
            float scale, float red, float green, float blue, float alpha,
            float minU, float minV, float maxU, float maxV) {
        float leftX = cameraLeft.x * scale;
        float leftY = cameraLeft.y * scale;
        float leftZ = cameraLeft.z * scale;
        float upX = cameraUp.x * scale;
        float upY = cameraUp.y * scale;
        float upZ = cameraUp.z * scale;
        int color = packColor(red, green, blue, alpha);

        consumer.addVertex(matrix,
                        (float) positionX - leftX - upX,
                        (float) positionY - leftY - upY,
                        (float) positionZ - leftZ - upZ)
                .setColor(color).setUv(maxU, maxV).setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(0F, 1F, 0F).setLight(240);
        consumer.addVertex(matrix,
                        (float) positionX - leftX + upX,
                        (float) positionY - leftY + upY,
                        (float) positionZ - leftZ + upZ)
                .setColor(color).setUv(maxU, minV).setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(0F, 1F, 0F).setLight(240);
        consumer.addVertex(matrix,
                        (float) positionX + leftX + upX,
                        (float) positionY + leftY + upY,
                        (float) positionZ + leftZ + upZ)
                .setColor(color).setUv(minU, minV).setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(0F, 1F, 0F).setLight(240);
        consumer.addVertex(matrix,
                        (float) positionX + leftX - upX,
                        (float) positionY + leftY - upY,
                        (float) positionZ + leftZ - upZ)
                .setColor(color).setUv(minU, maxV).setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(0F, 1F, 0F).setLight(240);
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = Math.round(Mth.clamp(red, 0F, 1F) * 255F);
        int g = Math.round(Mth.clamp(green, 0F, 1F) * 255F);
        int b = Math.round(Mth.clamp(blue, 0F, 1F) * 255F);
        int a = Math.round(Mth.clamp(alpha, 0F, 1F) * 255F);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static final class NuclearVisual {
        private final ClientLevel level;
        private final Vec3 origin;
        private final VisualProfile profile;
        private final RandomSource random;
        private final List<TorexCloudlet> cloudlets = new ArrayList<>();
        private final List<TorexCloudlet> renderCloudlets = new ArrayList<>();
        private final Deque<ReloadedGroundCloudlet> groundCloudlets = new ArrayDeque<>();
        private final Deque<ShockCloudlet> shockCloudlets = new ArrayDeque<>();
        private final double aftermathWindX;
        private final double aftermathWindZ;
        private double lastSpawnY = Double.NaN;
        private double lastSortCameraX = Double.NaN;
        private double lastSortCameraY = Double.NaN;
        private double lastSortCameraZ = Double.NaN;
        private double coreHeight;
        private double convectionHeight;
        private double torusWidth;
        private double rollerSize;
        private int age;
        private int renderedFramesSinceSort;
        private boolean playedShockSound;
        private boolean cloudOrderDirty = true;

        private NuclearVisual(ClientLevel level, Vec3 origin, VisualProfile profile) {
            this.level = level;
            this.origin = origin;
            this.profile = profile;
            long seed = Double.doubleToLongBits(origin.x) ^ Double.doubleToLongBits(origin.z);
            this.random = RandomSource.create(seed);
            RandomSource windRandom = RandomSource.create(seed ^ 0x6A09E667F3BCC909L);
            double windAngle = windRandom.nextDouble() * Math.PI * 2D;
            this.aftermathWindX = Math.cos(windAngle);
            this.aftermathWindZ = Math.sin(windAngle);
            this.coreHeight = 3D / 1.5D * profile.scale();
            this.torusWidth = 3D / 1.5D * profile.scale();
            this.rollerSize = 1D / 1.5D * profile.scale();
            this.convectionHeight = this.coreHeight + this.rollerSize;
        }

        private boolean tick(Minecraft minecraft) {
            HbmClientConfig.NuclearVisualQuality quality = HbmClientConfig.CLIENT.nuclearVisualQuality.get();
            spawnReloadedGroundCloudlets(quality);
            this.groundCloudlets.removeIf(ReloadedGroundCloudlet::tick);
            updateShockFront(quality, minecraft);
            this.age++;
            return this.age <= this.profile.maxAge();
        }

        private void spawnReloadedGroundCloudlets(HbmClientConfig.NuclearVisualQuality quality) {
            if (this.age >= 200) {
                return;
            }
            int sourceCount = this.age * 3;
            int spawnCount = switch (quality) {
                case FULL -> Math.min(sourceCount, 64);
                case REDUCED -> Math.min(sourceCount, 28);
                case MINIMAL -> this.age % 2 == 0 ? Math.min(sourceCount, 8) : 0;
            };
            int limit = switch (quality) {
                case FULL -> 2400;
                case REDUCED -> 1000;
                case MINIMAL -> 240;
            };
            double radius = this.age * 2D;
            for (int index = 0; index < spawnCount; index++) {
                double angle = this.random.nextDouble() * Math.PI * 2D;
                double worldX = this.origin.x + Math.cos(angle) * radius;
                double worldZ = this.origin.z + Math.sin(angle) * radius;
                double worldY = this.level.getHeight(Heightmap.Types.WORLD_SURFACE,
                        Mth.floor(worldX), Mth.floor(worldZ)) + 2D;
                while (this.groundCloudlets.size() >= limit) {
                    this.groundCloudlets.removeFirst();
                }
                this.groundCloudlets.addLast(new ReloadedGroundCloudlet(
                        new Vec3(worldX, worldY, worldZ),
                        0.25F + this.random.nextFloat() * 0.25F));
            }
        }

        /** Matches EntityNukeTorex's surface-following lower cloud emitter. */
        private void updateCloudState() {
            if (Double.isNaN(this.lastSpawnY)) {
                this.lastSpawnY = this.origin.y - 3D;
            }
            int surface = this.level.getHeight(Heightmap.Types.WORLD_SURFACE,
                    Mth.floor(this.origin.x), Mth.floor(this.origin.z));
            double target = Math.max(this.level.getMinBuildHeight() + 1D, surface - 3D);
            double delta = target - this.lastSpawnY;
            this.lastSpawnY = Math.abs(delta) <= 0.5D ? target : this.lastSpawnY + Math.copySign(0.5D, delta);
        }

        private void advanceCloudState() {
            this.coreHeight += 0.15D / this.profile.scale();
            this.torusWidth += 0.05D / this.profile.scale();
            this.rollerSize = this.torusWidth * 0.35D;
            this.convectionHeight = this.coreHeight + this.rollerSize;
        }

        private void spawnCloudlets(HbmClientConfig.NuclearVisualQuality quality) {
            double speed = simulationSpeed();
            int sourceCount = (int) Math.ceil(10D * speed * speed);
            int count = switch (quality) {
                case FULL -> sourceCount;
                case REDUCED -> Math.max(1, (int) Math.ceil(sourceCount * 0.45D));
                case MINIMAL -> this.age % 5 == 0 && sourceCount > 0 ? 1 : 0;
            };
            int cloudletLimit = switch (quality) {
                case FULL -> 2400;
                case REDUCED -> 1200;
                case MINIMAL -> 300;
            };
            int standardLimit = cloudletLimit * 3 / 4;
            int ringLimit = cloudletLimit / 8;
            int condensationLimit = cloudletLimit - standardLimit - ringLimit;
            int standardPopulation = countCloudlets(CloudletType.STANDARD);
            int ringPopulation = countCloudlets(CloudletType.RING);
            int condensationPopulation = countCloudlets(CloudletType.CONDENSATION);
            double range = Math.max(0.5D, (this.torusWidth - this.rollerSize) * 0.25D);
            int lifetime = Math.max(20, Math.min(this.age * this.age + 200,
                    this.profile.formationAge() - this.age + 200));
            for (int index = 0; index < count; index++) {
                if (standardPopulation >= standardLimit) {
                    if (!removeLeastUsefulStandardCloudlet()) {
                        break;
                    }
                    standardPopulation--;
                }
                if (!addCloudlet(new TorexCloudlet(
                        this.origin.x + this.random.nextGaussian() * range,
                        this.lastSpawnY,
                        this.origin.z + this.random.nextGaussian() * range,
                        this.random.nextDouble() * Math.PI * 2D,
                        lifetime,
                        CloudletType.STANDARD,
                        1F + this.age * 0.005F * 1.5F,
                        5F * 1.5F
                ), cloudletLimit)) {
                    break;
                }
                standardPopulation++;
            }

            if (this.age < 130F * 1.5F) {
                int ringCount = switch (quality) {
                    case FULL -> 2;
                    case REDUCED -> 1;
                    case MINIMAL -> this.age % 10 == 0 ? 1 : 0;
                };
                for (int index = 0; index < ringCount; index++) {
                    if (ringPopulation >= ringLimit) {
                        if (!removeOldestCloudlet(CloudletType.RING)) {
                            break;
                        }
                        ringPopulation--;
                    }
                    if (!addCloudlet(new TorexCloudlet(
                            this.origin.x,
                            this.origin.y + this.coreHeight,
                            this.origin.z,
                            this.random.nextDouble() * Math.PI * 2D,
                            Math.max(20, (int) (lifetime * 1.5D)),
                            CloudletType.RING,
                            1F + this.age * 0.0025F * 2.25F,
                            3F * 2.25F
                    ), cloudletLimit)) {
                        break;
                    }
                    ringPopulation++;
                }
            }

            if (quality != HbmClientConfig.NuclearVisualQuality.MINIMAL && this.age % 2 == 0) {
                int condensationCount = quality == HbmClientConfig.NuclearVisualQuality.FULL ? 12 : 4;
                if (this.age > 130F * 1.5F && this.age < 600F * 1.5F) {
                    for (int index = 0; index < condensationCount
                            && condensationPopulation < condensationLimit; index++) {
                        if (spawnCondensationCloudlet(false, cloudletLimit)) {
                            condensationPopulation++;
                        }
                    }
                }
                if (this.age > 200F * 1.5F && this.age < 600F * 1.5F) {
                    for (int index = 0; index < condensationCount
                            && condensationPopulation < condensationLimit; index++) {
                        if (spawnCondensationCloudlet(true, cloudletLimit)) {
                            condensationPopulation++;
                        }
                    }
                }
            }
        }

        private int countCloudlets(CloudletType type) {
            int count = 0;
            for (TorexCloudlet cloudlet : this.cloudlets) {
                if (cloudlet.type == type) {
                    count++;
                }
            }
            return count;
        }

        private boolean removeOldestCloudlet(CloudletType type) {
            for (Iterator<TorexCloudlet> iterator = this.cloudlets.iterator(); iterator.hasNext();) {
                if (iterator.next().type == type) {
                    iterator.remove();
                    return true;
                }
            }
            return false;
        }

        private boolean removeLeastUsefulStandardCloudlet() {
            int candidateIndex = -1;
            double candidateScore = Double.POSITIVE_INFINITY;
            for (int index = 0; index < this.cloudlets.size(); index++) {
                TorexCloudlet cloudlet = this.cloudlets.get(index);
                if (cloudlet.type != CloudletType.STANDARD) {
                    continue;
                }
                double score = cloudlet.retentionScore();
                if (score < candidateScore) {
                    candidateScore = score;
                    candidateIndex = index;
                }
            }
            if (candidateIndex < 0) {
                return false;
            }
            this.cloudlets.remove(candidateIndex);
            return true;
        }

        private boolean addCloudlet(TorexCloudlet cloudlet, int cloudletLimit) {
            if (this.cloudlets.size() >= cloudletLimit
                    && !(cloudlet.type == CloudletType.STANDARD
                    ? removeLeastUsefulStandardCloudlet()
                    : removeOldestCloudlet(cloudlet.type))) {
                return false;
            }
            this.cloudlets.add(cloudlet);
            return true;
        }

        private boolean spawnCondensationCloudlet(boolean upper, int cloudletLimit) {
            if (this.cloudlets.size() >= cloudletLimit) {
                return false;
            }
            double angle = this.random.nextDouble() * Math.PI * 2D;
            double radius = this.torusWidth + this.rollerSize
                    * (upper ? 3D + this.random.nextDouble() * 0.5D : 5D + this.random.nextDouble());
            double height = this.origin.y + this.coreHeight + (upper ? 25D : -5D)
                    + this.random.nextGaussian() * 2D;
            this.cloudlets.add(new TorexCloudlet(
                    this.origin.x + Math.cos(angle) * radius,
                    height,
                    this.origin.z + Math.sin(angle) * radius,
                    angle,
                    Math.max(20, (int) ((20D + this.age / 10D) * (1D + this.random.nextDouble() * 0.1D))),
                    CloudletType.CONDENSATION,
                    0.125F * 1.5F,
                    3F * 1.5F
            ));
            return true;
        }

        private void updateCloudlets() {
            this.cloudlets.removeIf(TorexCloudlet::tick);
        }

        private void updateShockFront(HbmClientConfig.NuclearVisualQuality quality, Minecraft minecraft) {
            this.shockCloudlets.removeIf(ShockCloudlet::tick);
            double radius = (this.age * 1.5D + 0.5D) * 1.5D;
            if (this.age < 150) {
                int sourceSpawnCount = this.age * 5;
                int spawnCount = switch (quality) {
                    case FULL -> Math.min(sourceSpawnCount, 96);
                    case REDUCED -> Math.min(sourceSpawnCount, 40);
                    case MINIMAL -> this.age % 2 == 0 ? Math.min(sourceSpawnCount, 12) : 0;
                };
                int cloudletLimit = switch (quality) {
                    case FULL -> 1800;
                    case REDUCED -> 800;
                    case MINIMAL -> 240;
                };
                while (this.shockCloudlets.size() + spawnCount > cloudletLimit) {
                    this.shockCloudlets.removeFirst();
                }
                int shockLifetime = NuclearCloudTiming.shockLifetimeTicks(this.age);
                double motionMultiplier = NuclearCloudTiming.shockMotionMultiplier(this.age);
                for (int index = 0; index < spawnCount; index++) {
                    double angle = this.random.nextDouble() * Math.PI * 2D;
                    double cloudRadius = NuclearCloudTiming.shockRadius(this.age, this.random.nextDouble());
                    double x = this.origin.x + Math.cos(angle) * cloudRadius;
                    double z = this.origin.z + Math.sin(angle) * cloudRadius;
                    double y = this.level.getHeight(Heightmap.Types.WORLD_SURFACE,
                            Mth.floor(x) + 1, Mth.floor(z));
                    this.shockCloudlets.addLast(new ShockCloudlet(
                            new Vec3(x, y, z),
                            angle,
                            shockLifetime,
                            motionMultiplier,
                            0.8F + this.random.nextFloat() * 0.2F
                    ));
                }
            }
            Player player = minecraft.player;
            if (!this.playedShockSound && player != null && player.position().distanceTo(this.origin) <= radius) {
                this.level.playLocalSound(this.origin.x, this.origin.y, this.origin.z,
                        HbmSounds.NUCLEAR_EXPLOSION.get(), SoundSource.BLOCKS, 10F, 1F, false);
                NuclearPresentationOverlay.beginShock(this.level, this.origin, this.profile.shockRange());
                this.playedShockSound = true;
            }
        }

        private void render(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick) {
            Matrix4f matrix = new PoseStack().last().pose();
            Vec3 cameraPosition = camera.getPosition();
            Vector3f cameraLeft = camera.getLeftVector();
            Vector3f cameraUp = camera.getUpVector();
            renderSourceFlare(buffer, matrix, cameraPosition, cameraLeft, cameraUp, partialTick);
            ReloadedMushroomRenderer.render(buffer, cameraPosition, this.origin,
                    this.age, partialTick, this.profile.sourceRadius(),
                    this.profile.maxAge(), this.profile.fadeStartAge());

            VertexConsumer consumer = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_CLOUD.apply(CLOUDLET_TEXTURE));
            for (ReloadedGroundCloudlet cloudlet : this.groundCloudlets) {
                renderBillboard(
                        matrix,
                        consumer,
                        cameraLeft,
                        cameraUp,
                        cloudlet.position.x - cameraPosition.x,
                        cloudlet.position.y - cameraPosition.y,
                        cloudlet.position.z - cameraPosition.z,
                        cloudlet.scale(),
                        cloudlet.brightness,
                        cloudlet.brightness,
                        cloudlet.brightness,
                        cloudlet.alpha()
                );
            }

            for (ShockCloudlet shockPoint : this.shockCloudlets) {
                renderBillboard(
                        matrix,
                        consumer,
                        cameraLeft,
                        cameraUp,
                        shockPoint.position.x - cameraPosition.x,
                        shockPoint.position.y - cameraPosition.y,
                        shockPoint.position.z - cameraPosition.z,
                        shockPoint.scale(),
                        0.48F * shockPoint.colorModifier,
                        0.50F * shockPoint.colorModifier,
                        0.52F * shockPoint.colorModifier,
                        shockPoint.alpha()
                );
            }
        }

        private static final class ReloadedGroundCloudlet {
            private static final int LIFETIME = 50;
            private final Vec3 position;
            private final float brightness;
            private int cloudletAge;

            private ReloadedGroundCloudlet(Vec3 position, float brightness) {
                this.position = position;
                this.brightness = brightness;
            }

            private boolean tick() {
                return ++this.cloudletAge > LIFETIME;
            }

            private float scale() {
                float remaining = 1F - (float) this.cloudletAge / LIFETIME;
                return 2.5F * remaining + 2.5F;
            }

            private float alpha() {
                if (this.cloudletAge < 3) {
                    return Mth.clamp(this.cloudletAge * 0.333F, 0F, 1F);
                }
                return Mth.clamp(1F - (float) this.cloudletAge / LIFETIME, 0F, 1F);
            }
        }

        private void rebuildRenderOrder(Vec3 cameraPosition, float partialTick) {
            boolean cameraMoved = Double.isNaN(this.lastSortCameraX)
                    || cameraPosition.distanceToSqr(this.lastSortCameraX, this.lastSortCameraY, this.lastSortCameraZ) > 4D;
            if (!this.cloudOrderDirty && !cameraMoved && this.renderedFramesSinceSort < 3) {
                this.renderedFramesSinceSort++;
                return;
            }

            this.renderCloudlets.clear();
            this.renderCloudlets.addAll(this.cloudlets);
            double cameraX = cameraPosition.x;
            double cameraY = cameraPosition.y;
            double cameraZ = cameraPosition.z;
            this.renderCloudlets.sort((left, right) -> Double.compare(
                    right.renderDistanceSquared(partialTick, cameraX, cameraY, cameraZ),
                    left.renderDistanceSquared(partialTick, cameraX, cameraY, cameraZ)
            ));
            this.lastSortCameraX = cameraX;
            this.lastSortCameraY = cameraY;
            this.lastSortCameraZ = cameraZ;
            this.renderedFramesSinceSort = 0;
            this.cloudOrderDirty = false;
        }

        private void renderSourceFlare(MultiBufferSource.BufferSource buffer, Matrix4f matrix,
                Vec3 cameraPosition, Vector3f cameraLeft, Vector3f cameraUp, float partialTick) {
            float visualAge = this.age + partialTick;
            if (visualAge >= 100F) {
                return;
            }
            float alpha = Mth.clamp((100F - visualAge) / 100F, 0F, 1F);
            VertexConsumer flare = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_FLARE.apply(FLARE_TEXTURE));
            RandomSource flareRandom = RandomSource.create(BlockPos.containing(this.origin).asLong() ^ 0x4F11A5E5L);
            float scale = (float) (25D * this.rollerSize);
            for (int index = 0; index < 3; index++) {
                double x = this.origin.x + flareRandom.nextGaussian() * 0.5D * this.rollerSize;
                double y = this.origin.y + this.coreHeight + flareRandom.nextGaussian() * 0.5D * this.rollerSize;
                double z = this.origin.z + flareRandom.nextGaussian() * 0.5D * this.rollerSize;
                renderBillboard(matrix, flare, cameraLeft, cameraUp,
                        x - cameraPosition.x, y - cameraPosition.y, z - cameraPosition.z,
                        scale, 1F, 1F, 1F, alpha);
            }
        }

        /**
         * Reloaded's early blast used a five-cloudlet cluster with a 5x5 animated explosion sheet.
         * This short hot core sits beneath the long-lived Torex cloud and does not change its geometry.
         */
        private void renderReloadedFireball(MultiBufferSource.BufferSource buffer, Matrix4f matrix,
                Vec3 cameraPosition, Vector3f cameraLeft, Vector3f cameraUp, float partialTick) {
            float visualAge = this.age + partialTick;
            float lifetime = 150F;
            if (visualAge < 0F || visualAge >= lifetime) {
                return;
            }

            float progress = Mth.clamp(visualAge / lifetime, 0F, 1F);
            float expansion = 1F - (float) Math.pow(1F - progress, 3D);
            float fadeIn = Mth.clamp(visualAge / 8F, 0F, 1F);
            float fadeOut = 1F - Mth.clamp((progress - 0.62F) / 0.38F, 0F, 1F);
            int frame = Mth.clamp((int) (progress * 25F), 0, 24);
            float frameSize = 0.2F;
            float minU = frame % 5 * frameSize;
            float minV = frame / 5 * frameSize;
            float maxU = minU + frameSize;
            float maxV = minV + frameSize;
            float baseScale = this.profile.scale() * (5F + 18F * expansion);
            double rise = this.profile.scale() * (4D + 15D * expansion);
            float alpha = fadeIn * fadeOut * 0.95F;
            RandomSource fireballRandom = RandomSource.create(
                    BlockPos.containing(this.origin).asLong() ^ 0x12B10B0FL
            );
            VertexConsumer fireball = buffer.getBuffer(
                    HbmNuclearRenderTypes.NUCLEAR_CLOUD.apply(RELOADED_FIREBALL_TEXTURE)
            );

            for (int index = 0; index < 5; index++) {
                double spread = this.profile.scale() * (1D + expansion * 3D);
                double x = this.origin.x + fireballRandom.nextGaussian() * spread;
                double y = this.origin.y + rise + fireballRandom.nextGaussian() * spread * 0.45D;
                double z = this.origin.z + fireballRandom.nextGaussian() * spread;
                float scale = baseScale * (0.72F + fireballRandom.nextFloat() * 0.38F);
                renderBillboard(
                        matrix,
                        fireball,
                        cameraLeft,
                        cameraUp,
                        x - cameraPosition.x,
                        y - cameraPosition.y,
                        z - cameraPosition.z,
                        scale,
                        1F,
                        1F,
                        1F,
                        alpha,
                        minU,
                        minV,
                        maxU,
                        maxV
                );
            }
        }

        private float aftermathProgress() {
            return NuclearCloudTiming.aftermathProgress(
                    this.age,
                    this.profile.formationAge(),
                    this.profile.maxAge()
            );
        }

        private double aftermathExpansion() {
            return 1D + aftermathProgress() * 0.08D;
        }

        private double aftermathTravel() {
            return aftermathProgress() * this.profile.scale() * 6D;
        }

        private double aftermathRise() {
            return aftermathProgress() * this.profile.scale() * 3D;
        }

        private final class TorexCloudlet {
            private double x;
            private double y;
            private double z;
            private double previousX;
            private double previousY;
            private double previousZ;
            private final double angle;
            private final int lifetime;
            private final CloudletType type;
            private final float rangeModifier;
            private final float colorModifier;
            private final float startingScale;
            private final float growingScale;
            private final double noisePhase;
            private final float scaleModifier;
            private Vec3 color = new Vec3(1D, 0.75D, 0.25D);
            private Vec3 previousColor = this.color;
            private int cloudletAge;

            private TorexCloudlet(double x, double y, double z, double angle, int lifetime, CloudletType type,
                    float startingScale, float growingScale) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.previousX = x;
                this.previousY = y;
                this.previousZ = z;
                this.angle = angle;
                this.lifetime = Math.max(1, lifetime);
                this.type = type;
                this.startingScale = startingScale;
                this.growingScale = growingScale;
                this.rangeModifier = 0.3F + NuclearVisual.this.random.nextFloat() * 0.7F;
                this.colorModifier = 0.8F + NuclearVisual.this.random.nextFloat() * 0.2F;
                this.noisePhase = NuclearVisual.this.random.nextDouble() * Math.PI * 2D;
                this.scaleModifier = 0.85F + NuclearVisual.this.random.nextFloat() * 0.30F;
                updateColor();
                this.previousColor = this.color;
            }

            private boolean tick() {
                if (NuclearVisual.this.age <= NuclearVisual.this.profile.formationAge()
                        && ++this.cloudletAge > this.lifetime) {
                    return true;
                }
                this.previousX = this.x;
                this.previousY = this.y;
                this.previousZ = this.z;
                Vec3 motion = switch (this.type) {
                    case STANDARD -> standardMotion();
                    case RING -> ringMotion();
                    case CONDENSATION -> condensationMotion();
                };
                double speed = NuclearVisual.this.simulationSpeed();
                this.x += motion.x * speed;
                this.y += motion.y * speed;
                this.z += motion.z * speed;
                updateColor();
                return false;
            }

            private Vec3 standardMotion() {
                double radial = Math.hypot(this.x - NuclearVisual.this.origin.x,
                        this.z - NuclearVisual.this.origin.z);
                double factor = Mth.clamp((this.y - NuclearVisual.this.origin.y)
                        / Math.max(0.001D, NuclearVisual.this.coreHeight), 0D, 1D);
                NuclearCloudMotion.Motion sourceMotion = NuclearCloudMotion.convection(
                        radial,
                        this.y - NuclearVisual.this.origin.y,
                        this.angle,
                        NuclearVisual.this.torusWidth,
                        NuclearVisual.this.coreHeight,
                        NuclearVisual.this.rollerSize,
                        this.rangeModifier
                );
                Vec3 convection = new Vec3(sourceMotion.x(), sourceMotion.y(), sourceMotion.z());
                Vec3 lift = liftMotion();
                Vec3 source = convection.scale(factor).add(lift.scale(1D - factor));
                NuclearCloudStructure.Motion turbulence = NuclearCloudStructure.turbulence(
                        this.angle,
                        this.noisePhase,
                        this.cloudletAge,
                        NuclearVisual.this.age,
                        factor
                );
                Vec3 shaped = source.add(turbulence.x(), turbulence.y(), turbulence.z());
                double length = shaped.length();
                return length > 1.08D ? shaped.scale(1.08D / length) : shaped;
            }

            private Vec3 ringMotion() {
                double radial = Math.hypot(this.x - NuclearVisual.this.origin.x,
                        this.z - NuclearVisual.this.origin.z);
                NuclearCloudMotion.Motion sourceMotion = NuclearCloudMotion.ring(
                        radial,
                        this.y - NuclearVisual.this.origin.y,
                        this.angle,
                        NuclearVisual.this.torusWidth,
                        NuclearVisual.this.coreHeight,
                        NuclearVisual.this.rollerSize,
                        this.rangeModifier
                );
                return new Vec3(sourceMotion.x(), sourceMotion.y(), sourceMotion.z());
            }

            private Vec3 liftMotion() {
                double radial = Math.hypot(this.x - NuclearVisual.this.origin.x,
                        this.z - NuclearVisual.this.origin.z);
                double strength = Mth.clamp(1D - (radial - NuclearVisual.this.torusWidth), 0D, 1D);
                Vec3 lift = new Vec3(
                        NuclearVisual.this.origin.x - this.x,
                        NuclearVisual.this.origin.y + NuclearVisual.this.convectionHeight - this.y,
                        NuclearVisual.this.origin.z - this.z
                );
                return lift.lengthSqr() <= 0.000001D ? Vec3.ZERO : lift.normalize().scale(strength);
            }

            private Vec3 condensationMotion() {
                Vec3 outward = new Vec3(this.x - NuclearVisual.this.origin.x, 0D,
                        this.z - NuclearVisual.this.origin.z);
                return outward.scale(0.00002D * NuclearVisual.this.age);
            }

            private void updateColor() {
                this.previousColor = this.color;
                if (this.type == CloudletType.CONDENSATION) {
                    this.color = new Vec3(1D, 1D, 1D);
                    return;
                }
                double dx = NuclearVisual.this.origin.x - this.x;
                double dy = NuclearVisual.this.origin.y + NuclearVisual.this.coreHeight - this.y;
                double dz = NuclearVisual.this.origin.z - this.z;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                NuclearCloudPalette.Rgb palette = NuclearCloudPalette.colorAt(
                        NuclearVisual.this.age,
                        NuclearVisual.this.profile.formationAge(),
                        distance,
                        this.type == CloudletType.RING
                );
                this.color = new Vec3(palette.red(), palette.green(), palette.blue());
            }

            private double renderX(float partialTick) {
                double interpolatedX = Mth.lerp(partialTick, this.previousX, this.x);
                double scale = NuclearVisual.this.profile.scale() * NuclearVisual.this.aftermathExpansion();
                return NuclearVisual.this.origin.x + (interpolatedX - NuclearVisual.this.origin.x) * scale
                        + NuclearVisual.this.aftermathWindX * NuclearVisual.this.aftermathTravel();
            }

            private double renderY(float partialTick) {
                double interpolatedY = Mth.lerp(partialTick, this.previousY, this.y);
                double scale = NuclearVisual.this.profile.scale() * NuclearVisual.this.aftermathExpansion();
                return NuclearVisual.this.origin.y + (interpolatedY - NuclearVisual.this.origin.y) * scale
                        + NuclearVisual.this.aftermathRise();
            }

            private double renderZ(float partialTick) {
                double interpolatedZ = Mth.lerp(partialTick, this.previousZ, this.z);
                double scale = NuclearVisual.this.profile.scale() * NuclearVisual.this.aftermathExpansion();
                return NuclearVisual.this.origin.z + (interpolatedZ - NuclearVisual.this.origin.z) * scale
                        + NuclearVisual.this.aftermathWindZ * NuclearVisual.this.aftermathTravel();
            }

            private double renderDistanceSquared(float partialTick, double cameraX, double cameraY, double cameraZ) {
                double dx = renderX(partialTick) - cameraX;
                double dy = renderY(partialTick) - cameraY;
                double dz = renderZ(partialTick) - cameraZ;
                return dx * dx + dy * dy + dz * dz;
            }

            private double retentionScore() {
                double radial = Math.hypot(this.x - NuclearVisual.this.origin.x,
                        this.z - NuclearVisual.this.origin.z);
                return NuclearCloudStructure.retentionScore(
                        radial,
                        this.y - NuclearVisual.this.origin.y,
                        NuclearVisual.this.coreHeight,
                        NuclearVisual.this.torusWidth,
                        NuclearVisual.this.rollerSize,
                        alpha()
                );
            }

            private float renderRed(float partialTick) {
                return (float) Mth.lerp(partialTick, this.previousColor.x, this.color.x);
            }

            private float renderGreen(float partialTick) {
                return (float) Mth.lerp(partialTick, this.previousColor.y, this.color.y);
            }

            private float renderBlue(float partialTick) {
                return (float) Mth.lerp(partialTick, this.previousColor.z, this.color.z);
            }

            private float renderScale() {
                float progress = (float) this.cloudletAge / this.lifetime;
                return Math.min(24F, (this.startingScale + progress * this.growingScale) * this.scaleModifier
                        * NuclearVisual.this.profile.scale()
                        * (1F + NuclearVisual.this.aftermathProgress() * 0.10F));
            }

            private float alpha() {
                float parentAlpha = 1F;
                int fadeStart = NuclearVisual.this.profile.fadeStartAge();
                if (NuclearVisual.this.age > fadeStart) {
                    parentAlpha = 1F - (float) (NuclearVisual.this.age - fadeStart)
                            / Math.max(1, NuclearVisual.this.profile.maxAge() - fadeStart);
                }
                float alpha = (1F - (float) this.cloudletAge / this.lifetime) * parentAlpha;
                if (this.type == CloudletType.CONDENSATION) {
                    alpha *= 0.25F;
                }
                return Mth.clamp(alpha, 0.0001F, 0.9F);
            }
        }

        private enum CloudletType {
            STANDARD,
            RING,
            CONDENSATION
        }

        private final class ShockCloudlet {
            private Vec3 position;
            private final double angle;
            private final int lifetime;
            private final double motionMultiplier;
            private final float colorModifier;
            private int cloudletAge;

            private ShockCloudlet(Vec3 position, double angle, int lifetime,
                    double motionMultiplier, float colorModifier) {
                this.position = position;
                this.angle = angle;
                this.lifetime = Math.max(1, lifetime);
                this.motionMultiplier = motionMultiplier;
                this.colorModifier = colorModifier;
            }

            private boolean tick() {
                if (++this.cloudletAge > this.lifetime) {
                    return true;
                }
                double heightFactor = Mth.clamp((this.position.y - NuclearVisual.this.origin.y)
                        / Math.max(0.001D, NuclearVisual.this.coreHeight), 0D, 1D);
                double speed = this.motionMultiplier * NuclearVisual.this.simulationSpeed() * heightFactor;
                this.position = this.position.add(
                        Math.cos(this.angle) * speed,
                        0D,
                        Math.sin(this.angle) * speed
                );
                return false;
            }

            private float scale() {
                return 7F + 2F * this.cloudletAge / this.lifetime;
            }

            private float alpha() {
                return Mth.clamp((1F - (float) this.cloudletAge / this.lifetime) * 0.48F, 0F, 0.48F);
            }
        }

        private double simulationSpeed() {
            return NuclearCloudTiming.simulationSpeed(
                    this.age,
                    this.profile.formationAge(),
                    this.profile.maxAge()
            );
        }
    }

    /** Modern presentation of the invisible source Fallout Rain job, limited to the player-visible slice of its radius. */
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
            if (player == null || player.position().distanceToSqr(this.origin) > (this.radius + 24D) * (this.radius + 24D)) {
                return true;
            }
            if (this.soundCooldown > 0) {
                this.soundCooldown--;
            }
            float soundVolume = HbmClientConfig.CLIENT.nuclearFalloutRainSoundVolume.get().floatValue();
            if (soundVolume > 0F && this.soundCooldown <= 0
                    && player.position().distanceToSqr(this.origin) <= (double) this.radius * this.radius) {
                this.level.playLocalSound(
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.WEATHER_RAIN,
                        SoundSource.WEATHER,
                        soundVolume,
                        0.82F + this.random.nextFloat() * 0.18F,
                        false
                );
                this.soundCooldown = 35 + this.random.nextInt(16);
            }
            return true;
        }

        private void render(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.position().distanceToSqr(this.origin) > (double) this.radius * this.radius) {
                return;
            }
            if (!HbmClientConfig.CLIENT.enableNuclearFalloutRain.get()) {
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
                    double worldX = x + 0.5D;
                    double worldZ = z + 0.5D;
                    double originDx = worldX - this.origin.x;
                    double originDz = worldZ - this.origin.z;
                    if (originDx * originDx + originDz * originDz > (double) this.radius * this.radius) {
                        continue;
                    }

                    double cameraDx = worldX - cameraPosition.x;
                    double cameraDz = worldZ - cameraPosition.z;
                    double horizontalDistance = Math.max(0.001D, Math.hypot(cameraDx, cameraDz));
                    float sideX = (float) (-cameraDz / horizontalDistance * 0.5D);
                    float sideZ = (float) (cameraDx / horizontalDistance * 0.5D);
                    int surface = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int minY = Math.max(surface, cameraY - range);
                    int maxY = Math.max(minY, cameraY + range);
                    if (minY == maxY) {
                        continue;
                    }

                    long seed = x * (long) x * 3121L + x * 45238971L
                            ^ z * (long) z * 418711L + z * 13761L;
                    this.random.setSeed(seed);
                    float variation = 0.4F + this.random.nextFloat() * 0.2F;
                    float sway = this.random.nextFloat();
                    float distanceFraction = (float) Math.min(1D, horizontalDistance / range);
                    float alpha = ((1F - distanceFraction * distanceFraction) * 0.3F + 0.5F)
                            * Math.min(1F, density);
                    float minV = minY / 4F + scroll + sway;
                    float maxV = maxY / 4F + scroll + sway;
                    float localX = (float) (worldX - cameraPosition.x);
                    float localMinY = (float) (minY - cameraPosition.y);
                    float localMaxY = (float) (maxY - cameraPosition.y);
                    float localZ = (float) (worldZ - cameraPosition.z);
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
        }

    }

    private record VisualProfile(float scale, int formationAge, int maxAge, int fadeStartAge, float sourceRadius,
            float flashRange, float shockRange) {
        private static VisualProfile from(ResourceLocation effect, int variant) {
            if (effect.equals(LITTLE_BOY_EFFECT)) {
                return create(1.5F, 45 * 20 * 3 / 2, 120F, 300F, 337.5F);
            }
            if (effect.equals(HbmExplosionService.PROTOTYPE_NUKE_EFFECT)) {
                float sourceRadius = Math.max(1, variant);
                float scale = Mth.clamp((float) Math.sqrt(sourceRadius * 0.01D) * 1.5F, 0.5F, 5F);
                return create(scale, Math.max(200, Math.round(45F * 20F * scale)),
                        sourceRadius, Math.max(64F, scale * 128F), Math.max(64F, scale * 150F));
            }
            return null;
        }

        private static VisualProfile create(float scale, int formationAge, float sourceRadius,
                float flashRange, float shockRange) {
            int maxAge = NuclearCloudTiming.totalLifetimeTicks(
                    formationAge,
                    sourceRadius
            );
            int fadeStartAge = NuclearCloudTiming.fadeStartTick(maxAge);
            return new VisualProfile(scale, formationAge, maxAge, fadeStartAge,
                    Math.max(0F, sourceRadius), flashRange, shockRange);
        }
    }

    private record VisualKey(ResourceLocation effect, BlockPos origin) {
    }

    private NuclearVisualEffectManager() {
    }
}
