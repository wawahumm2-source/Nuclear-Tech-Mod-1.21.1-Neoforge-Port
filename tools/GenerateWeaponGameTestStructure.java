import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.nio.file.Files;
import java.nio.file.Path;

/** Reproducibly generates the otherwise-binary empty platform used by weapon GameTests. */
public final class GenerateWeaponGameTestStructure {
    private static ListTag integers(int... values) {
        ListTag result = new ListTag();
        for (int value : values) {
            result.add(IntTag.valueOf(value));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output .nbt path");
        }

        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3955); // Minecraft 1.21.1
        root.put("size", integers(16, 8, 16));
        root.put("entities", new ListTag());

        CompoundTag smoothStone = new CompoundTag();
        smoothStone.putString("Name", "minecraft:smooth_stone");
        ListTag palette = new ListTag();
        palette.add(smoothStone);
        root.put("palette", palette);

        ListTag blocks = new ListTag();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                CompoundTag block = new CompoundTag();
                block.put("pos", integers(x, 0, z));
                block.putInt("state", 0);
                blocks.add(block);
            }
        }
        root.put("blocks", blocks);

        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        NbtIo.writeCompressed(root, output);
        System.out.println("Wrote weapon GameTest platform: " + output);
    }

    private GenerateWeaponGameTestStructure() {
    }
}
