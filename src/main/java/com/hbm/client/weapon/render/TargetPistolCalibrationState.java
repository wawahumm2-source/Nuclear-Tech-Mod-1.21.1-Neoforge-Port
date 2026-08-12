package com.hbm.client.weapon.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.HbmNuclearTech;
import com.hbm.item.HbmGunItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.cache.object.GeoBone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Temporary live calibration controls for the approved HBM Star-F presentation. Hip-fire mesh
 * and hand values remain independently adjustable while ADS uses its own baked mesh/hand target;
 * all components share one render-frame blend so neither endpoint overwrites the other.
 */
@EventBusSubscriber(modid = HbmNuclearTech.MOD_ID, value = Dist.CLIENT)
public final class TargetPistolCalibrationState {
    private static final ResourceLocation TARGET_MODEL = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "models/weapons/star_f.obj");
    private static final String CATEGORY = "key.categories.hbm.weapons";
    private static final KeyMapping TOGGLE = key("key.hbm.target_calibration",
            GLFW.GLFW_KEY_F8);
    private static final KeyMapping NEXT_FIELD = key("key.hbm.target_calibration_field",
            GLFW.GLFW_KEY_F9);
    private static final KeyMapping NEXT_GROUP = key("key.hbm.target_calibration_group",
            GLFW.GLFW_KEY_F10);
    private static final KeyMapping RESET = key("key.hbm.target_calibration_reset",
            GLFW.GLFW_KEY_HOME);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hbm-target-pistol-calibration.json";
    private static final SuperbGunRig.ModelPose TARGET_NON_FIRST_PERSON_POSE =
            new SuperbGunRig.ModelPose(Vec3.ZERO, new Vec3(0.0D, 180.0D, 0.0D), 1.0F);

    private static Values values;
    private static Field selected = Field.X;
    private static boolean hudVisible = true;
    private static int saveCountdown;
    private static String saveStatus = "not saved yet";

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
        event.register(NEXT_FIELD);
        event.register(NEXT_GROUP);
        event.register(RESET);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        SuperbGunRig rig = targetRig();
        ensureLoaded(minecraft, rig);

        boolean holdingTarget = isHoldingTarget(minecraft);
        while (TOGGLE.consumeClick()) {
            if (holdingTarget) {
                hudVisible = !hudVisible;
                if (!hudVisible) {
                    saveNow(minecraft);
                }
            }
        }
        while (NEXT_FIELD.consumeClick()) {
            if (holdingTarget && hudVisible) {
                selected = selected.nextInGroup();
            }
        }
        while (NEXT_GROUP.consumeClick()) {
            if (holdingTarget && hudVisible) {
                selected = selected.firstOfNextGroup();
            }
        }
        while (RESET.consumeClick()) {
            if (holdingTarget && hudVisible) {
                values = Values.from(rig);
                queueSave();
            }
        }

        if (holdingTarget && hudVisible && minecraft.screen == null) {
            long window = minecraft.getWindow().getWindow();
            int direction = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT) ? 1 : 0)
                    - (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT) ? 1 : 0);
            if (direction != 0) {
                Step step = Screen.hasControlDown() ? Step.FINE
                        : Screen.hasShiftDown() ? Step.COARSE : Step.NORMAL;
                values = values.adjust(selected, direction, step);
                queueSave();
            }
        }

        if (saveCountdown > 0 && --saveCountdown == 0) {
            saveNow(minecraft);
        }
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hudVisible || minecraft.options.hideGui || !isHoldingTarget(minecraft)) {
            return;
        }
        ensureLoaded(minecraft, targetRig());
        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = 8;
        int line = 10;
        draw(graphics, minecraft, "TARGET PISTOL LIVE CALIBRATION", x, y, 0xFFFFD65A);
        draw(graphics, minecraft, "Group: " + selected.group.label
                + "   Selected: " + selected.label, x, y += line, 0xFFFFFFFF);
        draw(graphics, minecraft, String.format(Locale.ROOT,
                "X %.3f   Y %.3f   Z %.3f", values.x, values.y, values.z),
                x, y += line, selected.isPosition() ? 0xFF8FE8FF : 0xFFB8B8B8);
        draw(graphics, minecraft, String.format(Locale.ROOT,
                "RX %.1f   RY %.1f   RZ %.1f", values.rotationX, values.rotationY,
                values.rotationZ), x, y += line,
                selected.isRotation() ? 0xFFFFA8E8 : 0xFFB8B8B8);
        draw(graphics, minecraft, String.format(Locale.ROOT, "Scale %.3f", values.scale),
                x, y += line, selected == Field.SCALE ? 0xFFA8FFA8 : 0xFFB8B8B8);
        draw(graphics, minecraft, String.format(Locale.ROOT,
                "Right P %.2f %.2f %.2f   R %.1f %.1f %.1f",
                values.rightArm.pivotX, values.rightArm.pivotY, values.rightArm.pivotZ,
                values.rightArm.rotationX, values.rightArm.rotationY,
                values.rightArm.rotationZ), x, y += line,
                selected.group == Group.RIGHT_ARM ? 0xFFFFD36A : 0xFF9E9E9E);
        draw(graphics, minecraft, String.format(Locale.ROOT,
                "Left  P %.2f %.2f %.2f   R %.1f %.1f %.1f",
                values.leftArm.pivotX, values.leftArm.pivotY, values.leftArm.pivotZ,
                values.leftArm.rotationX, values.leftArm.rotationY,
                values.leftArm.rotationZ), x, y += line,
                selected.group == Group.LEFT_ARM ? 0xFF6FE8FF : 0xFF9E9E9E);
        draw(graphics, minecraft, String.format(Locale.ROOT, "Arm scale %.3f", values.armScale),
                x, y += line, selected == Field.ARM_SCALE ? 0xFFA8FFA8 : 0xFF9E9E9E);
        draw(graphics, minecraft, String.format(Locale.ROOT,
                "ADS P %.3f %.3f %.3f   R %.1f %.1f %.1f   S %.3f",
                values.adsPose.x, values.adsPose.y, values.adsPose.z,
                values.adsPose.rotationX, values.adsPose.rotationY,
                values.adsPose.rotationZ, values.adsPose.scale), x, y += line,
                selected.group == Group.ADS_POSE ? 0xFFA8FFA8 : 0xFF9E9E9E);
        draw(graphics, minecraft,
                "F10 group | F9 field | hold Left/Right adjust",
                x, y += line + 2, 0xFFD7D7D7);
        draw(graphics, minecraft, "Shift coarse | Ctrl fine | Home reset | F8 hide | " + saveStatus,
                x, y += line, 0xFFD7D7D7);
        draw(graphics, minecraft,
                "Yellow/Cyan fixed hands | Red/Blue HBM grip | Green muzzle",
                x, y += line, 0xFFB8B8B8);
        draw(graphics, minecraft, "Saved in config/" + FILE_NAME,
                x, y + line, 0xFF8C8C8C);
    }

    static void applyToModelSpace(GeoBone bone, SuperbGunRig rig,
                                  ItemDisplayContext displayContext) {
        if (!TARGET_MODEL.equals(rig.model())) {
            applyModel(bone, rig.modelPose());
            return;
        }
        if (!displayContext.firstPerson()) {
            // Inventory and third person own their transform through the item model JSON. Only
            // preserve the OBJ's axis normalization here; first-person calibration offsets must
            // never leak into those display contexts.
            applyModel(bone, TARGET_NON_FIRST_PERSON_POSE);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ensureLoaded(minecraft, rig);
        applyModel(bone, modelPoseForContext(rig, values, displayContext,
                SuperbGunPresentationState.adsPresentationBlend()));
    }

    static SuperbGunRig.ModelPose modelPoseForContext(SuperbGunRig rig, Values current,
                                                       ItemDisplayContext displayContext,
                                                       float adsBlend) {
        if (TARGET_MODEL.equals(rig.model()) && !displayContext.firstPerson()) {
            return TARGET_NON_FIRST_PERSON_POSE;
        }
        return TARGET_MODEL.equals(rig.model())
                ? current.modelPoseAt(rig, adsBlend)
                : rig.modelPose();
    }

    static void applyToHandBone(GeoBone bone, SuperbGunRig rig,
                                SuperbGunRig.BoneRole role) {
        if (!TARGET_MODEL.equals(rig.model())) {
            return;
        }
        ensureLoaded(Minecraft.getInstance(), rig);
        ArmPose hipArm = role == SuperbGunRig.BoneRole.LEFT_HAND
                ? values.leftArm : values.rightArm;
        ArmPose adsArm = ArmPose.from(hand(rig, role, true));
        ArmPose arm = hipArm.lerp(adsArm,
                SuperbGunPresentationState.adsPresentationBlend());
        bone.setPivotX((float) arm.pivotX);
        bone.setPivotY((float) arm.pivotY);
        bone.setPivotZ((float) arm.pivotZ);
        // This must be absolute. Feeding the prior rendered rotation back into the next frame
        // accumulates GeckoLib's ADS delta and eventually makes the right arm spin.
        bone.setRotX((float) Math.toRadians(arm.rotationX));
        bone.setRotY((float) Math.toRadians(arm.rotationY));
        bone.setRotZ((float) Math.toRadians(arm.rotationZ));
    }

    static float armScale(SuperbGunRig rig) {
        if (!TARGET_MODEL.equals(rig.model())) {
            return rig.armScale();
        }
        ensureLoaded(Minecraft.getInstance(), rig);
        return Mth.lerp(SuperbGunPresentationState.adsPresentationBlend(),
                values.armScale, rig.adsArmScale());
    }

    static SuperbGunRig.FirstPersonPose adsPose(SuperbGunRig rig) {
        if (!TARGET_MODEL.equals(rig.model())) {
            return rig.ads();
        }
        ensureLoaded(Minecraft.getInstance(), rig);
        return values.adsPose.toRigPose();
    }

    static boolean markersVisible() {
        return hudVisible;
    }

    static Values currentValues() {
        return values == null ? Values.from(targetRig()) : values;
    }

    private static void applyModel(GeoBone bone, SuperbGunRig.ModelPose current) {
        // Gecko's animation translation negates X. Keep the HUD direction intuitive: increasing
        // MODEL X moves the pistol to the right, not the left.
        bone.setPosX((float) -current.translation().x);
        bone.setPosY((float) current.translation().y);
        bone.setPosZ((float) current.translation().z);
        bone.setRotX((float) Math.toRadians(current.rotationDegrees().x));
        bone.setRotY((float) Math.toRadians(current.rotationDegrees().y));
        bone.setRotZ((float) Math.toRadians(current.rotationDegrees().z));
        bone.setScaleX(current.scale());
        bone.setScaleY(current.scale());
        bone.setScaleZ(current.scale());
    }

    private static void ensureLoaded(Minecraft minecraft, SuperbGunRig rig) {
        if (values != null) {
            return;
        }
        Values fallback = Values.from(rig);
        Path file = calibrationFile(minecraft);
        if (!Files.isRegularFile(file)) {
            values = fallback;
            saveStatus = "using defaults";
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            int schema = (int) number(json, "schema", 0.0D);
            if (schema < 4) {
                // Schema 3 stored one shared mesh/hand pose. During ADS calibration that pose
                // could replace hip fire, which is precisely the regression schema 4 removes.
                values = fallback;
                saveStatus = "legacy shared pose ignored; using baked endpoints";
                return;
            }
            values = new Values(
                    number(json, "x", fallback.x),
                    number(json, "y", fallback.y),
                    number(json, "z", fallback.z),
                    number(json, "rotation_x", fallback.rotationX),
                    number(json, "rotation_y", fallback.rotationY),
                    number(json, "rotation_z", fallback.rotationZ),
                    (float) number(json, "scale", fallback.scale),
                    arm(json, "right_arm", fallback.rightArm),
                    arm(json, "left_arm", fallback.leftArm),
                    (float) number(json, "arm_scale", fallback.armScale),
                    viewPose(json, "ads_pose", fallback.adsPose)
            ).sanitized();
            saveStatus = "loaded";
        } catch (RuntimeException | IOException exception) {
            values = fallback;
            saveStatus = "invalid file; using defaults";
            HbmNuclearTech.LOGGER.warn("Could not load Target Pistol calibration from {}",
                    file, exception);
        }
    }

    private static double number(JsonObject json, String name, double fallback) {
        return json.has(name) && json.get(name).isJsonPrimitive()
                ? json.get(name).getAsDouble() : fallback;
    }

    private static ArmPose arm(JsonObject json, String name, ArmPose fallback) {
        if (!json.has(name) || !json.get(name).isJsonObject()) {
            return fallback;
        }
        JsonObject arm = json.getAsJsonObject(name);
        return new ArmPose(
                number(arm, "pivot_x", fallback.pivotX),
                number(arm, "pivot_y", fallback.pivotY),
                number(arm, "pivot_z", fallback.pivotZ),
                number(arm, "rotation_x", fallback.rotationX),
                number(arm, "rotation_y", fallback.rotationY),
                number(arm, "rotation_z", fallback.rotationZ));
    }

    private static ViewPose viewPose(JsonObject json, String name, ViewPose fallback) {
        if (!json.has(name) || !json.get(name).isJsonObject()) {
            return fallback;
        }
        JsonObject pose = json.getAsJsonObject(name);
        return new ViewPose(
                number(pose, "x", fallback.x),
                number(pose, "y", fallback.y),
                number(pose, "z", fallback.z),
                number(pose, "rotation_x", fallback.rotationX),
                number(pose, "rotation_y", fallback.rotationY),
                number(pose, "rotation_z", fallback.rotationZ),
                (float) number(pose, "scale", fallback.scale));
    }

    private static void queueSave() {
        saveCountdown = 5;
        saveStatus = "unsaved changes";
    }

    private static void saveNow(Minecraft minecraft) {
        if (values == null) {
            return;
        }
        Path file = calibrationFile(minecraft);
        JsonObject json = new JsonObject();
        json.addProperty("schema", 4);
        json.addProperty("x", values.x);
        json.addProperty("y", values.y);
        json.addProperty("z", values.z);
        json.addProperty("rotation_x", values.rotationX);
        json.addProperty("rotation_y", values.rotationY);
        json.addProperty("rotation_z", values.rotationZ);
        json.addProperty("scale", values.scale);
        json.add("right_arm", armJson(values.rightArm));
        json.add("left_arm", armJson(values.leftArm));
        json.addProperty("arm_scale", values.armScale);
        json.add("ads_pose", viewPoseJson(values.adsPose));
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            saveStatus = "saved";
            saveCountdown = 0;
        } catch (IOException exception) {
            saveStatus = "SAVE FAILED (see log)";
            HbmNuclearTech.LOGGER.error("Could not save Target Pistol calibration to {}",
                    file, exception);
        }
    }

    private static JsonObject armJson(ArmPose arm) {
        JsonObject json = new JsonObject();
        json.addProperty("pivot_x", arm.pivotX);
        json.addProperty("pivot_y", arm.pivotY);
        json.addProperty("pivot_z", arm.pivotZ);
        json.addProperty("rotation_x", arm.rotationX);
        json.addProperty("rotation_y", arm.rotationY);
        json.addProperty("rotation_z", arm.rotationZ);
        return json;
    }

    private static JsonObject viewPoseJson(ViewPose pose) {
        JsonObject json = new JsonObject();
        json.addProperty("x", pose.x);
        json.addProperty("y", pose.y);
        json.addProperty("z", pose.z);
        json.addProperty("rotation_x", pose.rotationX);
        json.addProperty("rotation_y", pose.rotationY);
        json.addProperty("rotation_z", pose.rotationZ);
        json.addProperty("scale", pose.scale);
        return json;
    }

    private static Path calibrationFile(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    private static boolean isHoldingTarget(Minecraft minecraft) {
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        return stack.getItem() instanceof HbmGunItem gun && TARGET_MODEL.equals(gun.modelResource());
    }

    private static SuperbGunRig targetRig() {
        return SuperbGunRig.find(TARGET_MODEL).orElseThrow();
    }

    private static SuperbGunRig.VirtualBone hand(SuperbGunRig rig,
                                                  SuperbGunRig.BoneRole role) {
        return hand(rig, role, false);
    }

    private static SuperbGunRig.VirtualBone hand(SuperbGunRig rig,
                                                  SuperbGunRig.BoneRole role,
                                                  boolean ads) {
        return (ads ? rig.adsVirtualBones() : rig.virtualBones()).stream()
                .filter(bone -> bone.role() == role)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Target Pistol rig is missing " + (ads ? "ADS " : "") + role));
    }

    private static KeyMapping key(String translation, int defaultKey) {
        return new KeyMapping(translation, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, String text,
                             int x, int y, int color) {
        graphics.drawString(minecraft.font, text, x, y, color, true);
    }

    public record Values(double x, double y, double z, double rotationX, double rotationY,
                         double rotationZ, float scale, ArmPose rightArm, ArmPose leftArm,
                         float armScale, ViewPose adsPose) {
        static Values from(SuperbGunRig rig) {
            SuperbGunRig.ModelPose pose = rig.modelPose();
            SuperbGunRig.VirtualBone right = hand(rig, SuperbGunRig.BoneRole.RIGHT_HAND);
            SuperbGunRig.VirtualBone left = hand(rig, SuperbGunRig.BoneRole.LEFT_HAND);
            return new Values(pose.translation().x, pose.translation().y, pose.translation().z,
                    pose.rotationDegrees().x, pose.rotationDegrees().y,
                    pose.rotationDegrees().z, pose.scale(), ArmPose.from(right),
                    ArmPose.from(left), rig.armScale(), ViewPose.from(rig.ads()));
        }

        SuperbGunRig.ModelPose modelPoseAt(SuperbGunRig rig, float adsBlend) {
            SuperbGunRig.ModelPose hip = new SuperbGunRig.ModelPose(
                    new Vec3(x, y, z), new Vec3(rotationX, rotationY, rotationZ), scale);
            return hip.lerp(rig.adsModelPose(), adsBlend);
        }

        ArmPose armPoseAt(SuperbGunRig rig, SuperbGunRig.BoneRole role, float adsBlend) {
            ArmPose hip = role == SuperbGunRig.BoneRole.LEFT_HAND ? leftArm : rightArm;
            return hip.lerp(ArmPose.from(hand(rig, role, true)), adsBlend);
        }

        float armScaleAt(SuperbGunRig rig, float adsBlend) {
            return Mth.lerp(Mth.clamp(adsBlend, 0.0F, 1.0F), armScale, rig.adsArmScale());
        }

        public Values adjust(Field field, int direction, Step step) {
            double positionStep = switch (step) {
                case FINE -> 0.01D;
                case NORMAL -> 0.05D;
                case COARSE -> 0.25D;
            };
            double rotationStep = switch (step) {
                case FINE -> 0.1D;
                case NORMAL -> 1.0D;
                case COARSE -> 5.0D;
            };
            float scaleStep = switch (step) {
                case FINE -> 0.002F;
                case NORMAL -> 0.01F;
                case COARSE -> 0.05F;
            };
            return switch (field) {
                case X -> new Values(x + positionStep * direction, y, z,
                        rotationX, rotationY, rotationZ, scale, rightArm, leftArm,
                        armScale, adsPose).sanitized();
                case Y -> new Values(x, y + positionStep * direction, z,
                        rotationX, rotationY, rotationZ, scale, rightArm, leftArm,
                        armScale, adsPose).sanitized();
                case Z -> new Values(x, y, z + positionStep * direction,
                        rotationX, rotationY, rotationZ, scale, rightArm, leftArm,
                        armScale, adsPose).sanitized();
                case ROT_X -> new Values(x, y, z, rotationX + rotationStep * direction,
                        rotationY, rotationZ, scale, rightArm, leftArm,
                        armScale, adsPose).sanitized();
                case ROT_Y -> new Values(x, y, z, rotationX,
                        rotationY + rotationStep * direction, rotationZ, scale,
                        rightArm, leftArm, armScale, adsPose).sanitized();
                case ROT_Z -> new Values(x, y, z, rotationX, rotationY,
                        rotationZ + rotationStep * direction, scale,
                        rightArm, leftArm, armScale, adsPose).sanitized();
                case SCALE -> new Values(x, y, z, rotationX, rotationY, rotationZ,
                        scale + scaleStep * direction, rightArm, leftArm,
                        armScale, adsPose).sanitized();
                case ADS_X, ADS_Y, ADS_Z, ADS_ROT_X, ADS_ROT_Y, ADS_ROT_Z,
                        ADS_SCALE -> withAds(adsPose.adjust(
                        field, direction, positionStep, rotationStep, scaleStep));
                case RIGHT_PIVOT_X, RIGHT_PIVOT_Y, RIGHT_PIVOT_Z,
                        RIGHT_ROT_X, RIGHT_ROT_Y, RIGHT_ROT_Z ->
                        withRight(rightArm.adjust(field, direction, positionStep, rotationStep));
                case LEFT_PIVOT_X, LEFT_PIVOT_Y, LEFT_PIVOT_Z,
                        LEFT_ROT_X, LEFT_ROT_Y, LEFT_ROT_Z ->
                        withLeft(leftArm.adjust(field, direction, positionStep, rotationStep));
                case ARM_SCALE -> new Values(x, y, z, rotationX, rotationY, rotationZ,
                        scale, rightArm, leftArm, armScale + scaleStep * direction,
                        adsPose).sanitized();
            };
        }

        private Values withRight(ArmPose changed) {
            return new Values(x, y, z, rotationX, rotationY, rotationZ,
                    scale, changed, leftArm, armScale, adsPose).sanitized();
        }

        private Values withLeft(ArmPose changed) {
            return new Values(x, y, z, rotationX, rotationY, rotationZ,
                    scale, rightArm, changed, armScale, adsPose).sanitized();
        }

        private Values withAds(ViewPose changed) {
            return new Values(x, y, z, rotationX, rotationY, rotationZ,
                    scale, rightArm, leftArm, armScale, changed).sanitized();
        }

        Values sanitized() {
            return new Values(
                    Mth.clamp(x, -32.0D, 32.0D),
                    Mth.clamp(y, -32.0D, 32.0D),
                    Mth.clamp(z, -32.0D, 32.0D),
                    Mth.clamp(rotationX, -720.0D, 720.0D),
                    Mth.clamp(rotationY, -720.0D, 720.0D),
                    Mth.clamp(rotationZ, -720.0D, 720.0D),
                    Mth.clamp(scale, 0.10F, 3.0F),
                    rightArm.sanitized(), leftArm.sanitized(),
                    Mth.clamp(armScale, 0.10F, 3.0F), adsPose.sanitized());
        }
    }

    public record ArmPose(double pivotX, double pivotY, double pivotZ,
                          double rotationX, double rotationY, double rotationZ) {
        static ArmPose from(SuperbGunRig.VirtualBone bone) {
            return new ArmPose(bone.pivot().x, bone.pivot().y, bone.pivot().z,
                    bone.rotationDegrees().x, bone.rotationDegrees().y,
                    bone.rotationDegrees().z);
        }

        ArmPose lerp(ArmPose target, float amount) {
            double blend = Mth.clamp(amount, 0.0F, 1.0F);
            if (blend <= 0.0D) {
                return this;
            }
            if (blend >= 1.0D) {
                return target;
            }
            return new ArmPose(
                    Mth.lerp(blend, pivotX, target.pivotX),
                    Mth.lerp(blend, pivotY, target.pivotY),
                    Mth.lerp(blend, pivotZ, target.pivotZ),
                    Mth.lerp(blend, rotationX, target.rotationX),
                    Mth.lerp(blend, rotationY, target.rotationY),
                    Mth.lerp(blend, rotationZ, target.rotationZ));
        }

        ArmPose adjust(Field field, int direction, double positionStep, double rotationStep) {
            return switch (field) {
                case RIGHT_PIVOT_X, LEFT_PIVOT_X -> new ArmPose(
                        pivotX + positionStep * direction, pivotY, pivotZ,
                        rotationX, rotationY, rotationZ).sanitized();
                case RIGHT_PIVOT_Y, LEFT_PIVOT_Y -> new ArmPose(
                        pivotX, pivotY + positionStep * direction, pivotZ,
                        rotationX, rotationY, rotationZ).sanitized();
                case RIGHT_PIVOT_Z, LEFT_PIVOT_Z -> new ArmPose(
                        pivotX, pivotY, pivotZ + positionStep * direction,
                        rotationX, rotationY, rotationZ).sanitized();
                case RIGHT_ROT_X, LEFT_ROT_X -> new ArmPose(
                        pivotX, pivotY, pivotZ, rotationX + rotationStep * direction,
                        rotationY, rotationZ).sanitized();
                case RIGHT_ROT_Y, LEFT_ROT_Y -> new ArmPose(
                        pivotX, pivotY, pivotZ, rotationX,
                        rotationY + rotationStep * direction, rotationZ).sanitized();
                case RIGHT_ROT_Z, LEFT_ROT_Z -> new ArmPose(
                        pivotX, pivotY, pivotZ, rotationX, rotationY,
                        rotationZ + rotationStep * direction).sanitized();
                default -> this;
            };
        }

        ArmPose sanitized() {
            return new ArmPose(
                    Mth.clamp(pivotX, -32.0D, 32.0D),
                    Mth.clamp(pivotY, -32.0D, 32.0D),
                    Mth.clamp(pivotZ, -32.0D, 32.0D),
                    Mth.clamp(rotationX, -720.0D, 720.0D),
                    Mth.clamp(rotationY, -720.0D, 720.0D),
                    Mth.clamp(rotationZ, -720.0D, 720.0D));
        }
    }

    public record ViewPose(double x, double y, double z,
                           double rotationX, double rotationY, double rotationZ,
                           float scale) {
        static ViewPose from(SuperbGunRig.FirstPersonPose pose) {
            return new ViewPose(pose.x(), pose.y(), pose.z(), pose.xRotation(),
                    pose.yRotation(), pose.zRotation(), pose.scale());
        }

        ViewPose adjust(Field field, int direction, double positionStep,
                        double rotationStep, float scaleStep) {
            return switch (field) {
                case ADS_X -> new ViewPose(x + positionStep * direction, y, z,
                        rotationX, rotationY, rotationZ, scale).sanitized();
                case ADS_Y -> new ViewPose(x, y + positionStep * direction, z,
                        rotationX, rotationY, rotationZ, scale).sanitized();
                case ADS_Z -> new ViewPose(x, y, z + positionStep * direction,
                        rotationX, rotationY, rotationZ, scale).sanitized();
                case ADS_ROT_X -> new ViewPose(x, y, z,
                        rotationX + rotationStep * direction, rotationY, rotationZ,
                        scale).sanitized();
                case ADS_ROT_Y -> new ViewPose(x, y, z, rotationX,
                        rotationY + rotationStep * direction, rotationZ,
                        scale).sanitized();
                case ADS_ROT_Z -> new ViewPose(x, y, z, rotationX, rotationY,
                        rotationZ + rotationStep * direction, scale).sanitized();
                case ADS_SCALE -> new ViewPose(x, y, z, rotationX, rotationY,
                        rotationZ, scale + scaleStep * direction).sanitized();
                default -> this;
            };
        }

        ViewPose sanitized() {
            return new ViewPose(
                    Mth.clamp(x, -4.0D, 4.0D),
                    Mth.clamp(y, -4.0D, 4.0D),
                    Mth.clamp(z, -4.0D, 4.0D),
                    Mth.clamp(rotationX, -720.0D, 720.0D),
                    Mth.clamp(rotationY, -720.0D, 720.0D),
                    Mth.clamp(rotationZ, -720.0D, 720.0D),
                    Mth.clamp(scale, 0.10F, 3.0F));
        }

        SuperbGunRig.FirstPersonPose toRigPose() {
            return new SuperbGunRig.FirstPersonPose(x, y, z,
                    (float) rotationX, (float) rotationY, (float) rotationZ, scale);
        }
    }

    public enum Field {
        X(Group.MODEL, "MODEL X"), Y(Group.MODEL, "MODEL Y"),
        Z(Group.MODEL, "MODEL Z"), ROT_X(Group.MODEL, "ROTATION X"),
        ROT_Y(Group.MODEL, "ROTATION Y"), ROT_Z(Group.MODEL, "ROTATION Z"),
        SCALE(Group.MODEL, "MODEL SCALE"),
        ADS_X(Group.ADS_POSE, "ADS X"), ADS_Y(Group.ADS_POSE, "ADS Y"),
        ADS_Z(Group.ADS_POSE, "ADS Z"),
        ADS_ROT_X(Group.ADS_POSE, "ADS ROTATION X"),
        ADS_ROT_Y(Group.ADS_POSE, "ADS ROTATION Y"),
        ADS_ROT_Z(Group.ADS_POSE, "ADS ROTATION Z"),
        ADS_SCALE(Group.ADS_POSE, "ADS SCALE"),
        RIGHT_PIVOT_X(Group.RIGHT_ARM, "RIGHT PIVOT X"),
        RIGHT_PIVOT_Y(Group.RIGHT_ARM, "RIGHT PIVOT Y"),
        RIGHT_PIVOT_Z(Group.RIGHT_ARM, "RIGHT PIVOT Z"),
        RIGHT_ROT_X(Group.RIGHT_ARM, "RIGHT ROTATION X"),
        RIGHT_ROT_Y(Group.RIGHT_ARM, "RIGHT ROTATION Y"),
        RIGHT_ROT_Z(Group.RIGHT_ARM, "RIGHT ROTATION Z"),
        ARM_SCALE(Group.RIGHT_ARM, "BOTH ARM SCALE"),
        LEFT_PIVOT_X(Group.LEFT_ARM, "LEFT PIVOT X"),
        LEFT_PIVOT_Y(Group.LEFT_ARM, "LEFT PIVOT Y"),
        LEFT_PIVOT_Z(Group.LEFT_ARM, "LEFT PIVOT Z"),
        LEFT_ROT_X(Group.LEFT_ARM, "LEFT ROTATION X"),
        LEFT_ROT_Y(Group.LEFT_ARM, "LEFT ROTATION Y"),
        LEFT_ROT_Z(Group.LEFT_ARM, "LEFT ROTATION Z");

        private final Group group;
        private final String label;

        Field(Group group, String label) {
            this.group = group;
            this.label = label;
        }

        Field nextInGroup() {
            Field[] fields = values();
            int index = ordinal();
            do {
                index = (index + 1) % fields.length;
            } while (fields[index].group != group);
            return fields[index];
        }

        Field firstOfNextGroup() {
            Group next = group.next();
            for (Field field : values()) {
                if (field.group == next) {
                    return field;
                }
            }
            return this;
        }

        boolean isPosition() {
            return this == X || this == Y || this == Z;
        }

        boolean isRotation() {
            return this == ROT_X || this == ROT_Y || this == ROT_Z;
        }
    }

    public enum Group {
        MODEL("MODEL"),
        ADS_POSE("ADS POSE"),
        RIGHT_ARM("RIGHT ARM"),
        LEFT_ARM("LEFT ARM");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        Group next() {
            Group[] groups = values();
            return groups[(ordinal() + 1) % groups.length];
        }
    }

    public enum Step {
        FINE,
        NORMAL,
        COARSE
    }

    private TargetPistolCalibrationState() {
    }
}
