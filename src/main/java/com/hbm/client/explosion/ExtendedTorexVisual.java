package com.hbm.client.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.client.render.HbmNuclearRenderTypes;
import com.hbm.config.HbmClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** NeoForge-safe port of NTM Extended 3.0.3's EntityNukeTorex and RenderTorex. */
final class ExtendedTorexVisual {
    private static final ResourceLocation CLOUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "textures/particle/particle_base.png");
    private static final ResourceLocation FLARE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "textures/particle/flare.png");

    private static final double HOT_RED = 2.5D;
    private static final double HOT_GREEN = 1.3D;
    private static final double HOT_BLUE = 0.4D;
    private static final double COLD_RED = 0.1D;
    private static final double COLD_GREEN = 0.075D;
    private static final double COLD_BLUE = 0.05D;

    final ClientLevel level;
    private final Vec3 origin;
    private final float scale;
    private final int maxAge;
    private final RandomSource random;
    private final List<Cloudlet> cloudlets = new ArrayList<>();
    private final List<Cloudlet> renderCloudlets = new ArrayList<>();
    private double coreHeight;
    private double convectionHeight;
    private double torusWidth;
    private double rollerSize;
    private double heat = 1D;
    private double lastSpawnY = -1D;
    private float humidity = -1F;
    private int age;
    private boolean shockReachedPlayer;

    ExtendedTorexVisual(ClientLevel level, Vec3 origin, float sourceRadius) {
        this.level = level;
        this.origin = origin;
        this.scale = ExtendedTorexTimeline.scale(sourceRadius);
        this.maxAge = ExtendedTorexTimeline.maxAge(this.scale);
        this.random = RandomSource.create(BlockPos.containing(origin).asLong() ^ 0x455854454E444544L);
        this.coreHeight = 3D * this.scale;
        this.convectionHeight = 3D * this.scale;
        this.torusWidth = 3D * this.scale;
        this.rollerSize = this.scale;
        this.age = 1;
    }

    boolean tick(Minecraft minecraft) {
        if (this.humidity < 0F) {
            this.humidity = this.level.getBiome(BlockPos.containing(this.origin)).value()
                    .getModifiedClimateSettings().downfall();
        }
        updateLowerEmitter();
        spawnCloudlets(HbmClientConfig.CLIENT.nuclearVisualQuality.get());
        updateCloudlets();
        updateShockArrival(minecraft.player);

        this.coreHeight += 0.15D;
        this.torusWidth += 0.05D;
        this.rollerSize = this.torusWidth * 0.35D;
        this.convectionHeight = this.coreHeight + this.rollerSize;
        this.heat = ExtendedTorexTimeline.heat(this.age, this.maxAge, this.scale);
        this.age++;
        return this.age <= this.maxAge;
    }

    void render(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        Matrix4f matrix = new PoseStack().last().pose();
        Vector3f cameraLeft = camera.getLeftVector();
        Vector3f cameraUp = camera.getUpVector();

        renderCloudlets(buffer, matrix, cameraPosition, cameraLeft, cameraUp, partialTick);
        renderFlare(buffer, matrix, cameraPosition, cameraLeft, cameraUp, partialTick);
        if (HbmClientConfig.CLIENT.enableNuclearFlash.get()) {
            renderFlash(buffer, matrix, cameraPosition, partialTick);
        }
    }

    private void updateLowerEmitter() {
        if (this.lastSpawnY == -1D) {
            this.lastSpawnY = this.origin.y - 3D;
        }
        int target = Math.max(this.level.getHeight(Heightmap.Types.WORLD_SURFACE,
                Mth.floor(this.origin.x), Mth.floor(this.origin.z)) - 3, 1);
        double difference = target - this.lastSpawnY;
        this.lastSpawnY = Math.abs(difference) < 0.5D
                ? target : this.lastSpawnY + 0.5D * Math.signum(difference);
    }

    private void spawnCloudlets(HbmClientConfig.NuclearVisualQuality quality) {
        int limit = cloudletLimit(quality);
        double simulationSpeed = simulationSpeed();
        int lifetime = ExtendedTorexTimeline.cloudletLifetime(this.age, this.maxAge);
        int standardCount = ExtendedTorexTimeline.standardSpawnCount(
                this.cloudlets.size(), lifetime, simulationSpeed, limit);
        double cloudRange = (this.torusWidth - this.rollerSize) * 0.5D;
        double cloudScale = Math.sqrt(this.scale) * 3D + this.age * 0.0025D * this.scale;
        double cloudGrowth = Math.sqrt(this.scale) * 3D
                + this.age * 0.0025D * 9D * this.scale;
        for (int index = 0; index < standardCount; index++) {
            addCloudlet(new Cloudlet(
                    this.origin.x + this.random.nextGaussian() * cloudRange,
                    this.lastSpawnY,
                    this.origin.z + this.random.nextGaussian() * cloudRange,
                    this.random.nextDouble() * Math.PI * 2D,
                    lifetime,
                    CloudletType.STANDARD,
                    (float) cloudScale,
                    (float) cloudGrowth,
                    1D
            ), quality, limit);
        }

        if (this.age < 120F * this.scale) {
            this.level.setSkyFlashTime(2);
        }

        if (this.age * ExtendedTorexTimeline.SHOCK_SPEED < 160D) {
            int cloudCount = (int) Math.min(this.age * ExtendedTorexTimeline.SHOCK_SPEED, 100D);
            int shockLife = (int) Math.max(this.scale * 300D
                    - this.age * ExtendedTorexTimeline.SHOCK_SPEED * 10D, 60D);
            double motion = Mth.clamp(0.25D * this.age - 5D, 0D, 1D);
            for (int index = 0; index < cloudCount; index++) {
                double distance = (this.age + this.random.nextDouble() * 2D - 2D)
                        * ExtendedTorexTimeline.SHOCK_SPEED;
                double angle = this.random.nextDouble() * Math.PI * 2D;
                double x = this.origin.x + Math.cos(angle) * distance;
                double z = this.origin.z + Math.sin(angle) * distance;
                double y = this.level.getHeight(Heightmap.Types.WORLD_SURFACE,
                        Mth.floor(x) + 1, Mth.floor(z));
                addCloudlet(new Cloudlet(x, y, z, angle, shockLife, CloudletType.SHOCK,
                        this.scale * 5F, this.scale * 2F, motion), quality, limit);
            }
        }

        if (this.age < 200) {
            int ringLifetime = lifetime * (int) this.scale;
            for (int index = 0; index < 2; index++) {
                double ringScale = Math.sqrt(this.scale) * 1.5D + this.age * 0.0015D * this.scale;
                double ringGrowth = Math.sqrt(this.scale) * 1.5D
                        + this.age * 0.0015D * 9D * this.scale;
                addCloudlet(new Cloudlet(this.origin.x, this.origin.y + this.coreHeight, this.origin.z,
                        this.random.nextDouble() * Math.PI * 2D,
                        ringLifetime, CloudletType.RING,
                        (float) ringScale, (float) ringGrowth, 1D), quality, limit);
            }
        }

        double condensationRange = this.age * ExtendedTorexTimeline.SHOCK_SPEED - 8D;
        if (this.humidity > 0F && this.age * ExtendedTorexTimeline.SHOCK_SPEED < 180D) {
            spawnCondensationClouds(condensationRange, 130, 80, 4, quality, limit);
            spawnCondensationClouds(condensationRange, 170, 80, 2, quality, limit);
        }
    }

    private void spawnCondensationClouds(double range, int height, int count, int spreadAngle,
            HbmClientConfig.NuclearVisualQuality quality, int limit) {
        if (range <= 0D || this.origin.y + range <= height) {
            return;
        }
        int outerCount = (int) (5D * this.humidity * count / spreadAngle);
        for (int index = 0; index < outerCount; index++) {
            for (int ring = 1; ring < spreadAngle; ring++) {
                double angle = Math.PI * 2D * this.random.nextDouble();
                double tilt = Math.acos((height - this.origin.y) / range)
                        + Math.toRadians(this.humidity * this.humidity * 90D * ring
                        * (0.1D * this.random.nextDouble() - 0.05D));
                double horizontal = -range * Math.sin(tilt);
                double x = this.origin.x + horizontal * Math.cos(angle);
                double y = this.origin.y + range * Math.cos(tilt);
                double z = this.origin.z - horizontal * Math.sin(angle);
                int lifetime = (int) ((20D + range / 10D) * (1D + this.random.nextDouble() * 0.1D));
                addCloudlet(new Cloudlet(x, y, z, angle, lifetime, CloudletType.CONDENSATION,
                        4.5F * this.scale, 6F * this.scale, 1D), quality, limit);
            }
        }
    }

    private void addCloudlet(Cloudlet cloudlet, HbmClientConfig.NuclearVisualQuality quality, int limit) {
        if (quality != HbmClientConfig.NuclearVisualQuality.FULL && this.cloudlets.size() >= limit) {
            return;
        }
        this.cloudlets.add(cloudlet);
    }

    private void updateCloudlets() {
        this.cloudlets.removeIf(cloudlet -> cloudlet.dead);
        for (Cloudlet cloudlet : this.cloudlets) {
            cloudlet.tick();
        }
    }

    private void updateShockArrival(Player player) {
        if (this.shockReachedPlayer || player == null || this.age > 300
                || !HbmClientConfig.CLIENT.enableNuclearScreenShake.get()) {
            return;
        }
        double distance = player.distanceToSqr(this.origin.x, this.origin.y, this.origin.z);
        double directDistance = Math.sqrt(distance);
        double frontDistance = directDistance - this.age * ExtendedTorexTimeline.SHOCK_SPEED;
        if (frontDistance > ExtendedTorexTimeline.SHOCK_SPEED * 2D || frontDistance < 0D) {
            return;
        }

        float amplitude = this.scale * 100F;
        this.level.playLocalSound(this.origin.x, this.origin.y, this.origin.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT,
                amplitude, 0.8F + this.random.nextFloat() * 0.2F, false);
        int duration = (int) (40D * Math.min(1.5D,
                amplitude * amplitude / Math.max(0.0001D, distance)));
        if (duration >= 15) {
            player.animateHurt(0F);
            player.hurtTime = duration << 1;
            player.hurtDuration = duration;
        }
        this.shockReachedPlayer = true;
    }

    private void renderCloudlets(MultiBufferSource.BufferSource buffer, Matrix4f matrix,
            Vec3 cameraPosition, Vector3f cameraLeft, Vector3f cameraUp, float partialTick) {
        this.renderCloudlets.clear();
        this.renderCloudlets.addAll(this.cloudlets);
        this.renderCloudlets.sort(Comparator.comparingDouble(
                cloudlet -> -cloudlet.distanceSquared(cameraPosition, partialTick)));
        VertexConsumer consumer = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_CLOUD.apply(CLOUD_TEXTURE));
        for (Cloudlet cloudlet : this.renderCloudlets) {
            Vec3 position = cloudlet.interpolatedPosition(partialTick);
            Vec3 color = cloudlet.interpolatedColor(partialTick);
            float brightness = cloudlet.type == CloudletType.CONDENSATION
                    ? 0.9F : 0.75F * cloudlet.colorModifier;
            float red = Math.min(1F, Math.max(0.15F, (float) color.x * brightness));
            float green = Math.min(1F, Math.max(0.15F, (float) color.y * brightness));
            float blue = Math.min(1F, Math.max(0.15F, (float) color.z * brightness));
            int light = (int) Math.max(48D, Math.min((red + green + blue) / 3D, 1D) * 240D);
            renderBillboard(matrix, consumer, cameraLeft, cameraUp,
                    position.x - cameraPosition.x,
                    position.y - cameraPosition.y,
                    position.z - cameraPosition.z,
                    cloudlet.renderScale(), red, green, blue, cloudlet.alpha(),
                    LightTexture.pack(light, light));
        }
    }

    private void renderFlare(MultiBufferSource.BufferSource buffer, Matrix4f matrix,
            Vec3 cameraPosition, Vector3f cameraLeft, Vector3f cameraUp, float partialTick) {
        float duration = ExtendedTorexTimeline.flareDuration(this.scale);
        float visualAge = Math.min(this.age + partialTick, duration);
        if (visualAge >= duration) {
            return;
        }
        float alpha = Math.min(1F, (duration - visualAge) / duration);
        RandomSource flareRandom = RandomSource.create(BlockPos.containing(this.origin).asLong());
        VertexConsumer consumer = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_FLARE.apply(FLARE_TEXTURE));
        for (int index = 0; index < 3; index++) {
            double x = this.origin.x + flareRandom.nextGaussian() * 0.5D * this.rollerSize;
            double y = this.origin.y + this.coreHeight + flareRandom.nextGaussian() * 0.5D * this.rollerSize;
            double z = this.origin.z + flareRandom.nextGaussian() * 0.5D * this.rollerSize;
            renderBillboard(matrix, consumer, cameraLeft, cameraUp,
                    x - cameraPosition.x, y - cameraPosition.y, z - cameraPosition.z,
                    (float) (10D * this.rollerSize), 1F, 1F, 1F, alpha,
                    LightTexture.pack((int) (alpha * 240F), (int) (alpha * 240F)));
        }
    }

    private void renderFlash(MultiBufferSource.BufferSource buffer, Matrix4f matrix,
            Vec3 cameraPosition, float partialTick) {
        float duration = ExtendedTorexTimeline.flashDuration(this.scale);
        float visualAge = this.age + partialTick;
        if (visualAge >= duration) {
            return;
        }
        double intensity = visualAge / duration;
        intensity = intensity * Math.exp(-intensity) * 2.717391304D;
        float inverse = (float) (1D - intensity);
        float flashScale = 50F * duration / ExtendedTorexTimeline.FLASH_BASE_DURATION;
        RandomSource flashRandom = RandomSource.create(432L);
        Quaternionf rotation = new Quaternionf();
        VertexConsumer consumer = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_FLASH);
        float centerX = (float) (this.origin.x - cameraPosition.x);
        float centerY = (float) (this.origin.y + this.coreHeight * 0.8D - cameraPosition.y);
        float centerZ = (float) (this.origin.z - cameraPosition.z);
        int centerColor = packColor(1F, 1F, 1F, inverse);
        int edgeColor = packColor(1F, 1F, 1F, 0F);

        for (int index = 0; index < 300; index++) {
            rotation.rotateX((float) Math.toRadians(flashRandom.nextFloat() * 360F));
            rotation.rotateY((float) Math.toRadians(flashRandom.nextFloat() * 360F));
            rotation.rotateZ((float) Math.toRadians(flashRandom.nextFloat() * 360F));
            rotation.rotateX((float) Math.toRadians(flashRandom.nextFloat() * 360F));
            rotation.rotateY((float) Math.toRadians(flashRandom.nextFloat() * 360F));
            float length = (flashRandom.nextFloat() * 20F + 15F) * (float) intensity * flashScale * 0.2F;
            float width = (flashRandom.nextFloat() * 2F + 3F) * (float) intensity * flashScale * 0.2F;
            Vector3f first = new Vector3f(-0.866F * width, length, -0.5F * width).rotate(rotation);
            Vector3f second = new Vector3f(0.866F * width, length, -0.5F * width).rotate(rotation);
            Vector3f third = new Vector3f(0F, length, width).rotate(rotation);
            flashTriangle(matrix, consumer, centerX, centerY, centerZ, first, second, centerColor, edgeColor);
            flashTriangle(matrix, consumer, centerX, centerY, centerZ, second, third, centerColor, edgeColor);
            flashTriangle(matrix, consumer, centerX, centerY, centerZ, third, first, centerColor, edgeColor);
        }
    }

    private static void flashTriangle(Matrix4f matrix, VertexConsumer consumer,
            float centerX, float centerY, float centerZ, Vector3f first, Vector3f second,
            int centerColor, int edgeColor) {
        consumer.addVertex(matrix, centerX, centerY, centerZ).setColor(centerColor);
        consumer.addVertex(matrix, centerX + first.x, centerY + first.y, centerZ + first.z).setColor(edgeColor);
        consumer.addVertex(matrix, centerX + second.x, centerY + second.y, centerZ + second.z).setColor(edgeColor);
    }

    private static void renderBillboard(Matrix4f matrix, VertexConsumer consumer,
            Vector3f cameraLeft, Vector3f cameraUp,
            double x, double y, double z, float scale,
            float red, float green, float blue, float alpha, int light) {
        float leftX = cameraLeft.x * scale;
        float leftY = cameraLeft.y * scale;
        float leftZ = cameraLeft.z * scale;
        float upX = cameraUp.x * scale;
        float upY = cameraUp.y * scale;
        float upZ = cameraUp.z * scale;
        int color = packColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) x - leftX - upX, (float) y - leftY - upY, (float) z - leftZ - upZ)
                .setColor(color).setUv(1F, 1F).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0F, 1F, 0F).setLight(light);
        consumer.addVertex(matrix, (float) x - leftX + upX, (float) y - leftY + upY, (float) z - leftZ + upZ)
                .setColor(color).setUv(1F, 0F).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0F, 1F, 0F).setLight(light);
        consumer.addVertex(matrix, (float) x + leftX + upX, (float) y + leftY + upY, (float) z + leftZ + upZ)
                .setColor(color).setUv(0F, 0F).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0F, 1F, 0F).setLight(light);
        consumer.addVertex(matrix, (float) x + leftX - upX, (float) y + leftY - upY, (float) z + leftZ - upZ)
                .setColor(color).setUv(0F, 1F).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0F, 1F, 0F).setLight(light);
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = Math.round(Mth.clamp(red, 0F, 1F) * 255F);
        int g = Math.round(Mth.clamp(green, 0F, 1F) * 255F);
        int b = Math.round(Mth.clamp(blue, 0F, 1F) * 255F);
        int a = Math.round(Mth.clamp(alpha, 0F, 1F) * 255F);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int cloudletLimit(HbmClientConfig.NuclearVisualQuality quality) {
        return switch (quality) {
            case FULL -> ExtendedTorexTimeline.MAX_CLOUDLETS;
            case REDUCED -> 6_000;
            case MINIMAL -> 2_000;
        };
    }

    private double simulationSpeed() {
        return ExtendedTorexTimeline.simulationSpeed(this.age, this.maxAge);
    }

    private final class Cloudlet {
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
        private final double motionMultiplier;
        private Vec3 color;
        private Vec3 previousColor;
        private int cloudletAge;
        private boolean dead;

        private Cloudlet(double x, double y, double z, double angle, int lifetime,
                CloudletType type, float startingScale, float growingScale, double motionMultiplier) {
            this.x = this.previousX = x;
            this.y = this.previousY = y;
            this.z = this.previousZ = z;
            this.angle = angle;
            this.lifetime = Math.max(1, lifetime);
            this.type = type;
            this.startingScale = startingScale;
            this.growingScale = growingScale;
            this.motionMultiplier = motionMultiplier;
            this.rangeModifier = 0.3F + ExtendedTorexVisual.this.random.nextFloat() * 0.7F;
            this.colorModifier = 0.8F + ExtendedTorexVisual.this.random.nextFloat() * 0.2F;
            updateColor();
            this.previousColor = this.color;
        }

        private void tick() {
            this.cloudletAge++;
            if (this.cloudletAge > this.lifetime) {
                this.dead = true;
            }
            this.previousX = this.x;
            this.previousY = this.y;
            this.previousZ = this.z;
            Vec3 motion = switch (this.type) {
                case STANDARD -> standardMotion();
                case RING -> ringMotion();
                case CONDENSATION -> radialMotion(ExtendedTorexVisual.this.scale * 0.125D);
                case SHOCK -> radialMotion(ExtendedTorexVisual.this.scale * 0.25D);
            };
            double multiplier = this.motionMultiplier * ExtendedTorexVisual.this.simulationSpeed();
            this.x += motion.x * multiplier;
            this.y += motion.y * multiplier;
            this.z += motion.z * multiplier;
            updateColor();
        }

        private Vec3 standardMotion() {
            double radial = Math.hypot(this.x - origin.x, this.z - origin.z);
            NuclearCloudMotion.Motion convection = NuclearCloudMotion.convection(
                    radial, this.y - origin.y, this.angle,
                    torusWidth, coreHeight, rollerSize, this.rangeModifier);
            Vec3 lift = liftMotion(radial);
            double factor = Mth.clamp((this.y - origin.y) / coreHeight, 0D, 1D);
            return new Vec3(convection.x() * 0.5D, convection.y() * 0.5D, convection.z() * 0.5D)
                    .scale(factor).add(lift.scale(1D - factor));
        }

        private Vec3 ringMotion() {
            double radial = Math.hypot(this.x - origin.x, this.z - origin.z);
            NuclearCloudMotion.Motion ring = NuclearCloudMotion.ring(
                    radial, this.y - origin.y, this.angle,
                    torusWidth, coreHeight, rollerSize, this.rangeModifier);
            return new Vec3(ring.x(), ring.y(), ring.z()).scale(0.25D);
        }

        private Vec3 liftMotion(double radial) {
            double strength = Mth.clamp(1D - (radial - torusWidth), 0D, 1D) * 0.625D;
            Vec3 lift = new Vec3(origin.x - this.x, origin.y + convectionHeight - this.y, origin.z - this.z);
            return lift.lengthSqr() == 0D ? Vec3.ZERO : lift.normalize().scale(strength);
        }

        private Vec3 radialMotion(double speed) {
            Vec3 direction = new Vec3(this.x - origin.x, 0D, this.z - origin.z);
            return direction.lengthSqr() == 0D ? Vec3.ZERO : direction.normalize().scale(speed);
        }

        private void updateColor() {
            this.previousColor = this.color;
            double dx = origin.x - this.x;
            double dy = origin.y + coreHeight - this.y;
            double dz = origin.z - this.z;
            double distanceSquared = (dx * dx + dy * dy + dz * dz)
                    / (this.type == CloudletType.SHOCK ? heat * 3D : heat);
            double interpolation = 2D / Math.max(distanceSquared, 1D);
            this.color = new Vec3(
                    COLD_RED + (HOT_RED - COLD_RED) * interpolation,
                    COLD_GREEN + (HOT_GREEN - COLD_GREEN) * interpolation,
                    COLD_BLUE + (HOT_BLUE - COLD_BLUE) * interpolation
            );
        }

        private Vec3 interpolatedPosition(float partialTick) {
            return new Vec3(
                    Mth.lerp(partialTick, this.previousX, this.x),
                    Mth.lerp(partialTick, this.previousY, this.y),
                    Mth.lerp(partialTick, this.previousZ, this.z)
            );
        }

        private Vec3 interpolatedColor(float partialTick) {
            if (this.type == CloudletType.CONDENSATION) {
                return new Vec3(1D, 1D, 1D);
            }
            double gray = this.type == CloudletType.RING ? 0.05D : 0D;
            return new Vec3(
                    Mth.lerp(partialTick, this.previousColor.x, this.color.x) + gray,
                    Mth.lerp(partialTick, this.previousColor.y, this.color.y) + gray,
                    Mth.lerp(partialTick, this.previousColor.z, this.color.z) + gray
            );
        }

        private double distanceSquared(Vec3 cameraPosition, float partialTick) {
            return interpolatedPosition(partialTick).distanceToSqr(cameraPosition);
        }

        private float alpha() {
            float alpha = (1F - (float) this.cloudletAge / this.lifetime)
                    * ExtendedTorexTimeline.parentAlpha(age, maxAge);
            if (this.type == CloudletType.CONDENSATION) {
                alpha *= 0.25F;
            }
            return Mth.clamp(alpha, 0.0001F, 1F);
        }

        private float renderScale() {
            return this.startingScale + (float) this.cloudletAge / this.lifetime * this.growingScale;
        }
    }

    private enum CloudletType {
        STANDARD,
        RING,
        CONDENSATION,
        SHOCK
    }
}
