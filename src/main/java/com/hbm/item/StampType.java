package com.hbm.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

public enum StampType {
    FLAT("flat"),
    PLATE("plate"),
    WIRE("wire"),
    CIRCUIT("circuit"),
    C357("c357"),
    C44("c44"),
    C50("c50"),
    C9("c9"),
    PRINTING1("printing1"),
    PRINTING2("printing2"),
    PRINTING3("printing3"),
    PRINTING4("printing4"),
    PRINTING5("printing5"),
    PRINTING6("printing6"),
    PRINTING7("printing7"),
    PRINTING8("printing8");

    public static final Codec<StampType> CODEC = Codec.STRING.xmap(StampType::byName, StampType::getSerializedName);
    public static final StreamCodec<RegistryFriendlyByteBuf, StampType> STREAM_CODEC = StreamCodec.of(
            (buffer, stampType) -> buffer.writeUtf(stampType.getSerializedName()),
            buffer -> StampType.byName(buffer.readUtf())
    );

    private final String serializedName;

    StampType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static StampType byName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (StampType type : values()) {
            if (type.serializedName.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown HBM press stamp type: " + name);
    }
}
