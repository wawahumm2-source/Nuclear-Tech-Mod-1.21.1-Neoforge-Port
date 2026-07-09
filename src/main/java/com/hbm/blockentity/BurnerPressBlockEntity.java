package com.hbm.blockentity;

import com.hbm.blockentity.machine.MachineOutputHelper;
import com.hbm.blockentity.machine.SidedMachineItemHandler;
import com.hbm.item.StampItem;
import com.hbm.menu.BurnerPressMenu;
import com.hbm.recipe.BurnerPressRecipe;
import com.hbm.recipe.BurnerPressRecipeInput;
import com.hbm.registry.HbmBlockEntities;
import com.hbm.registry.HbmRecipes;
import com.hbm.registry.HbmSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class BurnerPressBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_STAMP = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_TEMPLATE_START = 4;
    public static final int SLOT_COUNT = 13;

    private static final int CLIENT_SYNC_INTERVAL = 4;
    private static final int BURN_COST = 200;
    private static final int DEFAULT_MAX_PRESS = 200;
    private static final int MAX_SPEED = 400;
    private static final int PROGRESS_AT_MAX_SPEED = 25;
    private static final int PRESS_DELAY_TICKS = 5;

    private int press;
    private int maxPress = DEFAULT_MAX_PRESS;
    private int speed;
    private int delay;
    private int burnTime;
    private boolean processing;
    private boolean retracting;
    private float previousAnimationProgress;
    private float animationProgress;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_FUEL -> AbstractFurnaceBlockEntity.isFuel(stack);
                case SLOT_STAMP -> StampItem.isStamp(stack);
                case SLOT_INPUT -> !stack.isEmpty();
                case SLOT_OUTPUT -> false;
                default -> slot >= SLOT_TEMPLATE_START && slot < SLOT_COUNT;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final IItemHandler inputAutomationItems = new SidedMachineItemHandler(this.items, this::canAutomationInsert, slot -> false);
    private final IItemHandler outputAutomationItems = new SidedMachineItemHandler(this.items, (slot, stack) -> false, slot -> slot == SLOT_OUTPUT);
    private final IItemHandler sidedAutomationItems = new SidedMachineItemHandler(this.items, this::canAutomationInsert, slot -> slot == SLOT_OUTPUT);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> press;
                case 1 -> maxPress;
                case 2 -> burnTime;
                case 3 -> BURN_COST;
                case 4 -> speed;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> press = value;
                case 1 -> maxPress = value;
                case 2 -> burnTime = value;
                case 4 -> speed = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public BurnerPressBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BURNER_PRESS.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BurnerPressBlockEntity blockEntity) {
        boolean changed = false;
        ItemStack input = blockEntity.items.getStackInSlot(SLOT_INPUT);
        ItemStack stamp = blockEntity.items.getStackInSlot(SLOT_STAMP);
        BurnerPressRecipe recipe = blockEntity.getRecipe(level, input, stamp);
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.assemble(new BurnerPressRecipeInput(input, stamp), level.registryAccess());
        boolean canProcess = recipe != null && blockEntity.burnTime >= BURN_COST && blockEntity.canOutput(result);

        int oldSpeed = blockEntity.speed;
        if ((canProcess || blockEntity.retracting) && blockEntity.burnTime >= BURN_COST) {
            blockEntity.speed = Math.min(MAX_SPEED, blockEntity.speed + blockEntity.getSpeedIncrease(level, pos));
        } else {
            blockEntity.speed = Math.max(0, blockEntity.speed - 1);
        }
        changed |= oldSpeed != blockEntity.speed;

        if (blockEntity.delay > 0) {
            blockEntity.delay--;
            changed = true;
        } else {
            int stampSpeed = blockEntity.getStampSpeed();
            if (blockEntity.retracting) {
                blockEntity.press -= stampSpeed;
                if (blockEntity.press <= 0) {
                    blockEntity.press = 0;
                    blockEntity.retracting = false;
                    blockEntity.delay = PRESS_DELAY_TICKS;
                }
                blockEntity.processing = false;
                changed |= stampSpeed > 0;
            } else if (canProcess) {
                blockEntity.maxPress = Math.max(1, recipe.getProcessingTicks());
                blockEntity.processing = true;
                blockEntity.press += stampSpeed;
                changed |= stampSpeed > 0;

                if (blockEntity.press >= blockEntity.maxPress) {
                    blockEntity.completeRecipe(level, pos, input, stamp, result);
                    changed = true;
                }
            } else if (blockEntity.press > 0) {
                blockEntity.retracting = true;
                blockEntity.processing = false;
                changed = true;
            } else if (blockEntity.processing) {
                blockEntity.processing = false;
                changed = true;
            }
        }

        changed |= blockEntity.consumeFuelIfNeeded();

        if (changed) {
            blockEntity.setChanged();
            if (blockEntity.press == 0 || blockEntity.press >= blockEntity.maxPress || blockEntity.press % CLIENT_SYNC_INTERVAL == 0) {
                blockEntity.sync(level, state);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BurnerPressBlockEntity blockEntity) {
        blockEntity.previousAnimationProgress = blockEntity.animationProgress;
        if (blockEntity.processing && !blockEntity.retracting && blockEntity.press < blockEntity.maxPress) {
            blockEntity.press += blockEntity.getStampSpeed();
        } else if (blockEntity.retracting && blockEntity.press > 0) {
            blockEntity.press = Math.max(0, blockEntity.press - blockEntity.getStampSpeed());
        }
        blockEntity.animationProgress = blockEntity.maxPress <= 0 ? 0.0F : Math.min(1.0F, (float) blockEntity.press / (float) blockEntity.maxPress);
    }

    public ItemStackHandler getItems() {
        return this.items;
    }

    public IItemHandler getAutomationItems(@Nullable Direction side) {
        if (side == null) {
            return this.sidedAutomationItems;
        }
        return side == Direction.DOWN ? this.outputAutomationItems : this.inputAutomationItems;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public float getPreviousAnimationProgress() {
        return this.previousAnimationProgress;
    }

    public float getAnimationProgress() {
        return this.animationProgress;
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(this.items.getSlots());
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            container.setItem(slot, this.items.getStackInSlot(slot));
        }
        Containers.dropContents(level, pos, container);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm.burner_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BurnerPressMenu(containerId, playerInventory, this.items, this.dataAccess, ContainerLevelAccess.create(player.level(), this.worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.items.serializeNBT(registries));
        saveClientData(tag);
        tag.putInt("BurnTime", this.burnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.items.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        this.press = tag.getInt("Press");
        this.maxPress = tag.contains("MaxPress") ? tag.getInt("MaxPress") : DEFAULT_MAX_PRESS;
        this.speed = tag.getInt("Speed");
        this.delay = tag.getInt("Delay");
        this.burnTime = tag.getInt("BurnTime");
        this.processing = tag.getBoolean("Processing");
        this.retracting = tag.getBoolean("Retracting");
        this.animationProgress = this.maxPress <= 0 ? 0.0F : Math.min(1.0F, (float) this.press / (float) this.maxPress);
        this.previousAnimationProgress = this.animationProgress;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveClientData(tag);
        return tag;
    }

    @Nullable
    private BurnerPressRecipe getRecipe(Level level, ItemStack input, ItemStack stamp) {
        if (input.isEmpty() || stamp.isEmpty()) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(HbmRecipes.BURNER_PRESS_TYPE.get(), new BurnerPressRecipeInput(input, stamp), level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    private boolean consumeFuelIfNeeded() {
        if (this.burnTime >= BURN_COST) {
            return false;
        }
        ItemStack fuel = this.items.getStackInSlot(SLOT_FUEL);
        int fuelValue = fuel.getBurnTime(null);
        if (fuelValue <= 0) {
            return false;
        }

        this.burnTime += fuelValue;
        if (fuel.hasCraftingRemainingItem()) {
            this.items.setStackInSlot(SLOT_FUEL, fuel.getCraftingRemainingItem());
        } else {
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                this.items.setStackInSlot(SLOT_FUEL, ItemStack.EMPTY);
            }
        }
        return true;
    }

    private void completeRecipe(Level level, BlockPos pos, ItemStack input, ItemStack stamp, ItemStack result) {
        input.shrink(1);
        if (level instanceof ServerLevel serverLevel) {
            stamp.hurtAndBreak(1, serverLevel, null, item -> {
            });
        }
        output(result);
        this.burnTime = Math.max(0, this.burnTime - BURN_COST);
        this.press = this.maxPress;
        this.delay = PRESS_DELAY_TICKS;
        this.processing = false;
        this.retracting = true;
        level.playSound(null, pos, HbmSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 0.65F, 1.0F);
    }

    private boolean canOutput(ItemStack result) {
        return MachineOutputHelper.canMerge(this.items.getStackInSlot(SLOT_OUTPUT), result);
    }

    private void output(ItemStack result) {
        MachineOutputHelper.mergeIntoSlot(this.items, SLOT_OUTPUT, result);
    }

    private void saveClientData(CompoundTag tag) {
        tag.putInt("Press", this.press);
        tag.putInt("MaxPress", this.maxPress);
        tag.putInt("Speed", this.speed);
        tag.putInt("Delay", this.delay);
        tag.putInt("BurnTime", this.burnTime);
        tag.putBoolean("Processing", this.processing);
        tag.putBoolean("Retracting", this.retracting);
    }

    private void sync(Level level, BlockState state) {
        if (!level.isClientSide) {
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private boolean canAutomationInsert(int slot, ItemStack stack) {
        return (slot == SLOT_FUEL || slot == SLOT_STAMP || slot == SLOT_INPUT) && this.items.isItemValid(slot, stack);
    }

    private int getStampSpeed() {
        return this.speed * PROGRESS_AT_MAX_SPEED / MAX_SPEED;
    }

    private int getSpeedIncrease(Level level, BlockPos pos) {
        return 1;
    }
}
