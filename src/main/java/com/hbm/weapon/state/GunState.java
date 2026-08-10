package com.hbm.weapon.state;

import com.hbm.HbmNuclearTech;
import com.hbm.weapon.data.FireMode;
import com.hbm.weapon.data.GunDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Persistent, stack-owned weapon state. Transient input and reload timers deliberately live in a
 * player attachment so copying, dropping, or saving a gun cannot duplicate an in-flight action.
 */
public record GunState(
        int version,
        UUID stackIdentity,
        ResourceLocation loadedAmmoId,
        int ammoCount,
        boolean chambered,
        FireMode fireMode,
        ResourceLocation selectedAmmoId,
        float heat,
        int durability
) {
    public static final int CURRENT_VERSION = 1;
    public static final ResourceLocation EMPTY_AMMO = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "empty");

    private static final Codec<FireMode> FIRE_MODE_CODEC = Codec.STRING.xmap(FireMode::parse, value -> value.name().toLowerCase());

    public static final Codec<GunState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("version", CURRENT_VERSION).forGetter(GunState::version),
            UUIDUtil.CODEC.fieldOf("stack_identity").forGetter(GunState::stackIdentity),
            ResourceLocation.CODEC.optionalFieldOf("loaded_ammo", EMPTY_AMMO).forGetter(GunState::loadedAmmoId),
            Codec.INT.optionalFieldOf("ammo_count", 0).forGetter(GunState::ammoCount),
            Codec.BOOL.optionalFieldOf("chambered", false).forGetter(GunState::chambered),
            FIRE_MODE_CODEC.optionalFieldOf("fire_mode", FireMode.SEMI).forGetter(GunState::fireMode),
            ResourceLocation.CODEC.optionalFieldOf("selected_ammo", EMPTY_AMMO).forGetter(GunState::selectedAmmoId),
            Codec.FLOAT.optionalFieldOf("heat", 0.0F).forGetter(GunState::heat),
            Codec.INT.optionalFieldOf("durability", 0).forGetter(GunState::durability)
    ).apply(instance, GunState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunState> STREAM_CODEC = StreamCodec.of(
            GunState::encode,
            GunState::decode
    );

    public GunState {
        if (version < 0 || ammoCount < 0 || durability < 0) {
            throw new IllegalArgumentException("Gun state counters cannot be negative");
        }
        if (!Float.isFinite(heat) || heat < 0.0F) {
            throw new IllegalArgumentException("Gun heat must be finite and non-negative");
        }
    }

    public static GunState create(GunDefinition definition) {
        ResourceLocation firstAmmo = definition.getSupportedAmmo().getFirst();
        return new GunState(
                CURRENT_VERSION,
                UUID.randomUUID(),
                EMPTY_AMMO,
                0,
                false,
                definition.getDefaultFireMode(),
                firstAmmo,
                0.0F,
                0
        );
    }

    public GunState migrated(GunDefinition definition) {
        ResourceLocation selected = definition.getSupportedAmmo().contains(selectedAmmoId)
                ? selectedAmmoId
                : definition.getSupportedAmmo().getFirst();
        FireMode mode = definition.getFireModes().contains(fireMode) ? fireMode : definition.getDefaultFireMode();
        boolean loadedAmmoValid = definition.getSupportedAmmo().contains(loadedAmmoId);
        int boundedCount = loadedAmmoValid ? Math.min(ammoCount, definition.getMagazine().getCapacity()) : 0;
        ResourceLocation loaded = boundedCount == 0 ? EMPTY_AMMO : loadedAmmoId;
        return new GunState(CURRENT_VERSION, stackIdentity, loaded, boundedCount,
                definition.getMagazine().getUsesChamber() && boundedCount > 0 && chambered,
                mode, selected, Math.max(0.0F, heat), Math.max(0, durability));
    }

    public GunState withStackIdentity(UUID identity) {
        return new GunState(CURRENT_VERSION, identity, loadedAmmoId, ammoCount, chambered,
                fireMode, selectedAmmoId, heat, durability);
    }

    public GunState withMagazine(ResourceLocation ammoId, int count, boolean isChambered) {
        ResourceLocation normalizedId = count == 0 ? EMPTY_AMMO : ammoId;
        return new GunState(CURRENT_VERSION, stackIdentity, normalizedId, count, isChambered,
                fireMode, selectedAmmoId, heat, durability);
    }

    public GunState withFireMode(FireMode mode) {
        return new GunState(CURRENT_VERSION, stackIdentity, loadedAmmoId, ammoCount, chambered,
                mode, selectedAmmoId, heat, durability);
    }

    public GunState withSelectedAmmo(ResourceLocation ammoId) {
        return new GunState(CURRENT_VERSION, stackIdentity, loadedAmmoId, ammoCount, chambered,
                fireMode, ammoId, heat, durability);
    }

    public GunState withHeatAndDurability(float newHeat, int newDurability) {
        return new GunState(CURRENT_VERSION, stackIdentity, loadedAmmoId, ammoCount, chambered,
                fireMode, selectedAmmoId, newHeat, newDurability);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, GunState state) {
        buffer.writeVarInt(state.version);
        buffer.writeUUID(state.stackIdentity);
        buffer.writeResourceLocation(state.loadedAmmoId);
        buffer.writeVarInt(state.ammoCount);
        buffer.writeBoolean(state.chambered);
        buffer.writeEnum(state.fireMode);
        buffer.writeResourceLocation(state.selectedAmmoId);
        buffer.writeFloat(state.heat);
        buffer.writeVarInt(state.durability);
    }

    private static GunState decode(RegistryFriendlyByteBuf buffer) {
        return new GunState(
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readEnum(FireMode.class),
                buffer.readResourceLocation(),
                buffer.readFloat(),
                buffer.readVarInt()
        );
    }
}
