package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.weapon.state.WeaponSession;
import com.hbm.world.radiation.RadiationPlayerState;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class HbmAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HbmNuclearTech.MOD_ID);

    public static final Supplier<AttachmentType<RadiationPlayerState>> PLAYER_RADIATION = ATTACHMENTS.register(
            "player_radiation",
            () -> AttachmentType.serializable(RadiationPlayerState::new).copyOnDeath().build()
    );

    public static final Supplier<AttachmentType<RadiationPlayerState>> MOB_RADIATION = ATTACHMENTS.register(
            "mob_radiation",
            () -> AttachmentType.serializable(RadiationPlayerState::new).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WeaponSession>> WEAPON_SESSION =
            ATTACHMENTS.register("weapon_session", () -> AttachmentType.builder(WeaponSession::new).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }

    private HbmAttachments() {
    }
}
