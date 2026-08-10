package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Source-asset particle used for the modern Torex flash, shock, ring, and cloudlets. */
public final class NuclearCloudParticle extends TextureSheetParticle {
    private final Style style;
    private final float baseSize;

    private NuclearCloudParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY,
            double velocityZ, SpriteSet sprites, Style style) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.style = style;
        this.hasPhysics = false;
        this.friction = 0.98F;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.lifetime = style.lifetime + this.random.nextInt(style.lifetimeJitter + 1);
        this.baseSize = style == Style.FLASH
                ? Math.max(0.5F, (float) velocityX * 25F)
                : style.baseSize * (0.8F + this.random.nextFloat() * 0.4F);
        this.quadSize = this.baseSize;
        this.setColor(style.red, style.green, style.blue);
        this.setAlpha(style == Style.FLASH ? 0.95F : 0.75F);
        this.pickSprite(sprites);

        if (style == Style.FLASH) {
            this.xd = 0D;
            this.yd = 0D;
            this.zd = 0D;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        float progress = (float) this.age / this.lifetime;
        switch (this.style) {
            case CLOUD -> {
                this.yd = Math.min(0.22D, this.yd + 0.002D);
                this.xd *= 0.994D;
                this.zd *= 0.994D;
                this.setAlpha(0.58F * (1F - progress * progress));
            }
            case SHOCK -> {
                this.xd *= 0.99D;
                this.zd *= 0.99D;
                this.setAlpha(0.45F * (1F - progress));
            }
            case RING -> {
                this.yd += 0.0015D;
                this.setAlpha(0.52F * (1F - progress));
            }
            case CAP -> {
                this.yd = Math.min(0.05D, this.yd + 0.0005D);
                this.xd *= 0.996D;
                this.zd *= 0.996D;
                this.setAlpha(0.60F * (1F - progress * progress));
            }
            case FALLOUT -> {
                this.yd = Math.max(-0.18D, this.yd - 0.006D);
                this.xd *= 0.97D;
                this.zd *= 0.97D;
                this.setAlpha(0.70F * (1F - progress));
            }
            case FLASH -> this.setAlpha(0.95F * (1F - progress));
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Math.min(1F, (this.age + partialTick) / this.lifetime);
        return this.baseSize * (1F + progress * this.style.growth);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public enum Style {
        CLOUD(380, 220, 1.25F, 3.8F, 0.57F, 0.50F, 0.38F),
        SHOCK(90, 70, 6.5F, 1.2F, 0.67F, 0.65F, 0.58F),
        RING(280, 160, 2.1F, 2.7F, 0.56F, 0.53F, 0.47F),
        CAP(360, 180, 2.25F, 3.2F, 0.57F, 0.52F, 0.44F),
        FALLOUT(52, 20, 0.32F, 0.25F, 0.68F, 0.66F, 0.54F),
        FLASH(100, 0, 1F, 0F, 1F, 1F, 1F);

        private final int lifetime;
        private final int lifetimeJitter;
        private final float baseSize;
        private final float growth;
        private final float red;
        private final float green;
        private final float blue;

        Style(int lifetime, int lifetimeJitter, float baseSize, float growth, float red, float green, float blue) {
            this.lifetime = lifetime;
            this.lifetimeJitter = lifetimeJitter;
            this.baseSize = baseSize;
            this.growth = growth;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Style style;

        public Provider(SpriteSet sprites, Style style) {
            this.sprites = sprites;
            this.style = style;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new NuclearCloudParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites, this.style);
        }
    }
}
