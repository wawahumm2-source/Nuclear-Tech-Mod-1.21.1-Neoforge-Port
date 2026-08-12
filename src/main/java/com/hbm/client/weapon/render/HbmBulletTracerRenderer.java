package com.hbm.client.weapon.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** Two-frame, world-space bullet streaks; gameplay trajectories remain server-owned. */
public final class HbmBulletTracerRenderer {
    static final double STREAK_LENGTH = 0.90D;
    static final double MUZZLE_HIDE_DISTANCE = 1.0D;
    private static final int LIFETIME_FRAMES = 3;
    private static final int MAX_ACTIVE = 256;
    private static final Deque<ActiveTracer> ACTIVE = new ArrayDeque<>();

    public static void enqueue(Level level, Vec3 start, Vec3 end, Vec3 shooterEye,
                               int color, boolean localShooter) {
        Segment segment = clipSegment(start, end, shooterEye);
        if (level == null || segment == null) {
            return;
        }
        while (ACTIVE.size() >= MAX_ACTIVE) {
            ACTIVE.removeFirst();
        }
        ACTIVE.addLast(new ActiveTracer(level, segment.start(), segment.end(), color,
                localShooter ? 0.72F : 1.0F, LIFETIME_FRAMES));
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer ribbons = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        for (Iterator<ActiveTracer> iterator = ACTIVE.iterator(); iterator.hasNext();) {
            ActiveTracer tracer = iterator.next();
            if (tracer.level != minecraft.level) {
                iterator.remove();
                continue;
            }
            float fade = tracer.framesRemaining / (float) LIFETIME_FRAMES;
            float brightness = tracer.brightness * (0.45F + 0.55F * fade);
            int red = Math.round(((tracer.color >> 16) & 0xFF) * brightness);
            int green = Math.round(((tracer.color >> 8) & 0xFF) * brightness);
            int blue = Math.round((tracer.color & 0xFF) * brightness);
            int alpha = Math.round(175.0F * fade);
            Vec3 side = ribbonSide(tracer.start, tracer.end, camera,
                    tracer.brightness < 1.0F ? 0.026D : 0.034D);
            Vec3 start = tracer.start.subtract(camera);
            Vec3 end = tracer.end.subtract(camera);
            addVertex(ribbons, pose, start.subtract(side), red, green, blue, alpha);
            addVertex(ribbons, pose, start.add(side), red, green, blue, alpha);
            addVertex(ribbons, pose, end.add(side), red, green, blue, alpha);
            addVertex(ribbons, pose, end.subtract(side), red, green, blue, alpha);

            tracer.framesRemaining--;
            if (tracer.framesRemaining <= 0) {
                iterator.remove();
            }
        }
        buffers.endBatch(RenderType.lightning());
    }

    static Segment clipSegment(Vec3 start, Vec3 end, Vec3 shooterEye) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return null;
        }
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 visibleStart = start;
        if (shooterEye != null) {
            double startDistance = start.distanceTo(shooterEye);
            double trim = Math.max(0.0D, MUZZLE_HIDE_DISTANCE - startDistance);
            if (trim >= length) {
                return null;
            }
            visibleStart = start.add(direction.scale(trim));
        }

        double visibleLength = visibleStart.distanceTo(end);
        if (visibleLength < 1.0E-4D) {
            return null;
        }
        double streakLength = Math.min(STREAK_LENGTH, visibleLength);
        return new Segment(end.add(direction.scale(-streakLength)), end);
    }

    static Vec3 ribbonSide(Vec3 start, Vec3 end, Vec3 camera, double halfWidth) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = camera.subtract(midpoint);
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return side.normalize().scale(halfWidth);
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
                                  int red, int green, int blue, int alpha) {
        consumer.addVertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha);
    }

    record Segment(Vec3 start, Vec3 end) {
    }

    private static final class ActiveTracer {
        private final Level level;
        private final Vec3 start;
        private final Vec3 end;
        private final int color;
        private final float brightness;
        private int framesRemaining;

        private ActiveTracer(Level level, Vec3 start, Vec3 end, int color,
                             float brightness, int framesRemaining) {
            this.level = level;
            this.start = start;
            this.end = end;
            this.color = color;
            this.brightness = brightness;
            this.framesRemaining = framesRemaining;
        }
    }

    private HbmBulletTracerRenderer() {
    }
}
