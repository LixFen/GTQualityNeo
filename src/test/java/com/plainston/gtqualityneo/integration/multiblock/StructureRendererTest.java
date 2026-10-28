package com.plainston.gtqualityneo.integration.multiblock;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class StructureRendererTest {
    private static final float SCALE = 10;
    private static final Vector3f CENTER = new Vector3f(3.5f, 65.5f, -2.5f);

    private static PoseStack camera(Quaternionf rotation) {
        var pose = new PoseStack();
        // The native PiP renderer supplies this transform before calling renderToTexture.
        pose.translate(100, 61, 0);
        pose.scale(SCALE, SCALE, -SCALE);
        StructureRenderer.applyCamera(pose, rotation, CENTER);
        return pose;
    }

    @Test void worldAxesRemainUpright() {
        var matrix = camera(new Quaternionf()).last().pose();
        var center = matrix.transformPosition(new Vector3f(CENTER));
        var east = matrix.transformPosition(new Vector3f(CENTER).add(1, 0, 0));
        var up = matrix.transformPosition(new Vector3f(CENTER).add(0, 1, 0));
        assertEquals(100, center.x, .001f);
        assertEquals(61, center.y, .001f);
        assertTrue(east.x > center.x, "East projects right before yaw rotation");
        assertTrue(up.y < center.y, "Up must project above the scene center");
    }

    @Test void renderedPositionsAgreeWithTheInversePickingRayAtEveryViewAngle() {
        for (float yaw : new float[] {0, 45, 135, 225, 315}) for (float pitch : new float[] {-65, 0, 25, 65}) {
            var rotation = new Quaternionf().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
            var point = new Vector3f(CENTER).add(1.25f, -.75f, 2);
            var projected = camera(rotation).last().pose().transformPosition(new Vector3f(point));
            // Same +Y-up conversion and near-to-far direction used by Scene.hit().
            float vx = (projected.x - 100) / SCALE;
            float vy = (61 - projected.y) / SCALE;
            var inverse = new Quaternionf(rotation).conjugate();
            var near = inverse.transform(new Vector3f(vx, vy, 1000)).add(CENTER);
            var far = inverse.transform(new Vector3f(vx, vy, -1000)).add(CENTER);
            var direction = far.sub(near, new Vector3f()).normalize();
            var difference = point.sub(near, new Vector3f());
            var onRay = new Vector3f(near).fma(difference.dot(direction), direction);
            assertEquals(0, onRay.distance(point), .002f, "yaw=" + yaw + ", pitch=" + pitch);
            for (boolean zeroToOne : new boolean[] {false, true}) {
                var projection = new Matrix4f().setOrtho(0, 200, 122, 0, -1000, 1000, zeroToOne);
                var viewProjection = projection.mul(camera(rotation).last().pose());
                var nearDepth = viewProjection.transformPosition(new Vector3f(near));
                var farDepth = viewProjection.transformPosition(new Vector3f(far));
                assertTrue(nearDepth.z < farDepth.z, "The picking ray must start at the smallest depth-buffer value");
            }
        }
    }

    @Test void frontShellOccludesTheCavityAndRearWallInTheNativeProjection() {
        for (boolean zeroToOne : new boolean[] {false, true}) {
            var projection = new Matrix4f().setOrtho(0, 200, 122, 0, -1000, 1000, zeroToOne);
            var matrix = projection.mul(camera(new Quaternionf()).last().pose());
            var front = matrix.transformPosition(new Vector3f(CENTER).add(0, 0, 1));
            var cavity = matrix.transformPosition(new Vector3f(CENTER));
            var rear = matrix.transformPosition(new Vector3f(CENTER).add(0, 0, -1));
            assertTrue(front.z < cavity.z && cavity.z < rear.z, "Opaque front walls must hide the interior");
            // Outward +Z face, counterclockwise in world XY: it must still be front-facing in clip XY.
            var a = matrix.transformPosition(new Vector3f(CENTER).add(-1, 1, 1));
            var b = matrix.transformPosition(new Vector3f(CENTER).add(-1, -1, 1));
            var c = matrix.transformPosition(new Vector3f(CENTER).add(1, -1, 1));
            assertTrue((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) > 0);
        }
    }

    @Test void defaultViewAnglesTheVisibleControllerFaceFortyFiveDegreesRight() {
        var fronts = new Vector3f[] {new Vector3f(0, 0, -1), new Vector3f(0, 0, 1),
            new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0)};
        for (byte side = 2; side <= 5; side++) {
            var rotation = new Quaternionf().rotateX((float) Math.toRadians(25))
                .rotateY((float) Math.toRadians(StructureRenderer.frontYaw(side)));
            var front = rotation.transform(new Vector3f(fronts[side - 2]));
            assertEquals((float) Math.sqrt(.5), front.x, .0001f, "Controller front must angle 45 degrees right");
            assertEquals((float) (Math.sqrt(.5) * Math.cos(Math.toRadians(25))), front.z, .0001f);
            assertTrue(front.z > 0, "Controller front must point towards the viewer");
        }
    }
}
