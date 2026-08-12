package com.hbm.client.weapon.render;

import com.hbm.HbmNuclearTech;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ObjBakedGeoModelLoaderTest {
    private static final String TRIANGLE = """
            o Gun
            v 0 0 0
            v 1 0 0
            v 0 1 0
            vt 0 0
            vt 1 0
            vt 0 1
            vn 0 0 1
            f 1/1/1 2/2/1 3/3/1
            o Slide
            f 1/1/1 2/2/1 3/3/1
            """;

    @Test
    void targetModelNormalizationDoesNotRotatePlayerHands() throws Exception {
        ResourceLocation target = ResourceLocation.fromNamespaceAndPath(
                HbmNuclearTech.MOD_ID, "models/weapons/star_f.obj");
        BakedGeoModel model = ObjBakedGeoModelLoader.parse(target, new StringReader(TRIANGLE));

        GeoBone camera = model.getBone("camera").orElseThrow();
        GeoBone root = model.getBone("root").orElseThrow();
        GeoBone controller = model.getBone("Gun").orElseThrow();
        GeoBone modelSpace = model.getBone("model_space").orElseThrow();
        GeoBone mesh = model.getBone("Gun_mesh").orElseThrow();
        GeoBone rightHand = model.getBone("Righthand").orElseThrow();
        GeoBone leftHand = model.getBone("Lefthand").orElseThrow();
        GeoBone flare = model.getBone("flare").orElseThrow();

        assertSame(camera, root.getParent());
        assertSame(root, controller.getParent());
        assertSame(controller, modelSpace.getParent());
        assertEquals(-0.40F, modelSpace.getPosX(), 1.0E-5F);
        assertEquals(-0.70F, modelSpace.getPosY(), 1.0E-5F);
        assertEquals(-2.20F, modelSpace.getPosZ(), 1.0E-5F);
        assertEquals(Math.toRadians(-1.0D), modelSpace.getRotX(), 1.0E-5D);
        assertEquals(Math.PI, modelSpace.getRotY(), 1.0E-5D);
        assertEquals(1.01F, modelSpace.getScaleX(), 1.0E-5F);
        assertEquals(1.01F, modelSpace.getScaleY(), 1.0E-5F);
        assertEquals(1.01F, modelSpace.getScaleZ(), 1.0E-5F);
        assertSame(modelSpace, mesh.getParent());
        assertFalse(mesh.getCubes().isEmpty());
        assertSame(controller, rightHand.getParent());
        assertSame(controller, leftHand.getParent());
        assertSame(mesh, flare.getParent());
    }
}
