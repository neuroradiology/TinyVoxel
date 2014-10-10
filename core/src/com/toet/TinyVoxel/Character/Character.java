package com.toet.TinyVoxel.Character;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.GameControllers.CharacterController;
import com.toet.TinyVoxel.Renderer.Manager;
import com.toet.TinyVoxel.Time;

/**
 * Created by Kajos on 25-1-14.
 */
public class Character {
    public Vector3 velocity = new Vector3(0f, 0f, 0f);

    protected PerspectiveCamera camera;

    protected CharacterController controller;

    protected boolean touchingGround = false;
    protected boolean touchingCeiling = false;
    protected boolean touchingWalls = false;

    public Character(CharacterController controller, PerspectiveCamera camera) {
        this.controller = controller;
        this.camera = camera;
    }

    public void start()
    {
        camera.position.set(33,2,-33);
        camera.up.set(0, 1, 0);
        camera.lookAt(0, 0, 0);
        camera.up.set(0, 1, 0);
        camera.update();
    }

    protected Vector3 left = new Vector3();
    protected Vector3 tmp = new Vector3();
    protected Vector3 tmp2 = new Vector3();

    protected float size = 1.1f;
    protected float height = 1.1f;

    Vector3 prev = new Vector3();

    float deltaSend = 0f;
    public void update(Manager manager) {
        if (controller == null)
            return;

        tmp2.set(camera.position);
        tmp2.add(0f, -height, 0f);
        while(!Config.NO_CLIPPING && (manager.collidesSphereAllWith(camera.position, size) || manager.collidesSphereAllWith(tmp2, size))) {
            camera.position.y += .5f;
            tmp2.y += .5f;
        }

        // Reset
        touchingGround = false;
        touchingCeiling = false;
        touchingWalls = false;

        // Ground
        tmp.set(camera.position);
        tmp.add(0f, -.1f - height, 0f);
        if (manager.collidesSphereAllWith(tmp, size)) {
            touchingGround = true;
        }

        // Ceiling
        tmp.set(camera.position);
        tmp.add(0f, .1f, 0f);
        if (manager.collidesSphereAllWith(tmp, size)) {
            if (velocity.y > 0f)
                velocity.y = 0f;
            touchingCeiling = true;
        }

        deltaSend += Time.getDelta();
        if (deltaSend > 1f) {
            deltaSend = 0f;
        }

        prev.set(camera.position);

        camera.direction.rotate(camera.up, controller.getFloat(CharacterController.ACTION.X) * Config.get().DRAG_SPEED * (Config.INVERSE_MOUSE ? -1f : 1f));
        tmp.set(camera.direction).crs(camera.up).nor();

        float degrees = controller.getFloat(CharacterController.ACTION.Y) * Config.get().DRAG_SPEED * (Config.INVERSE_MOUSE ? -1f : 1f);
        float angle = camera.direction.dot(Vector3.Y) * 90f;
        if (angle + degrees < 90f && angle + degrees > -90f)
            camera.direction.rotate(tmp, degrees);


        // Forward / backward
        tmp.set(camera.direction);
        if (controller.getBoolean(CharacterController.ACTION.Shift))
            tmp.scl(2.5f);

        tmp.scl(controller.getFloat(CharacterController.ACTION.Forward) * Time.getDelta() * 40f);
        tmp.y = 0f;
        if (!touchingGround)
            tmp.scl(.1f);
        velocity.add(tmp);

        // Strafe
        tmp.set(camera.direction);
        if (controller.getBoolean(CharacterController.ACTION.Shift))
            tmp.scl(2.5f);

        tmp.scl(controller.getFloat(CharacterController.ACTION.Left) * Time.getDelta() * 40f);
        tmp.crs(camera.up);
        tmp.y = 0f;
        velocity.add(tmp);

        tmp.set(velocity);
        tmp.scl(Time.getDelta());
        camera.position.add(tmp);

        tmp2.set(camera.position);
        tmp2.add(0f, -height, 0f);
        if (Config.NO_CLIPPING || manager.collidesSphereAllWith(tmp2, size) || manager.collidesSphereAllWith(camera.position, size)) {
            camera.position.set(prev);

            // Jump
            if (controller.getBoolean(CharacterController.ACTION.Jump)) {
                velocity.add(0f, 50f, 0f);
            }
            
            // Friction
            velocity.scl((float) Math.pow(.01f, Time.getDelta()));

            // Flat ground movement
            tmp.y = 0f; // velocity without y
            camera.position.add(tmp);
            tmp2.set(camera.position);
            tmp2.add(0f, -height, 0f);
            if (!Config.NO_CLIPPING && (manager.collidesSphereAllWith(tmp2, size) || manager.collidesSphereAllWith(camera.position, size))) {
                camera.position.set(prev);

                touchingWalls = true;

                // Climbing
                if (velocity.y < 20f)
                    velocity.add(0f, Time.getDelta() * 200f, 0f);

                tmp.set(velocity);
                tmp.scl(Time.getDelta());

                tmp.x = 0f; // velocity without x
                tmp.z = 0f; // velocity without x
                camera.position.add(tmp);
                tmp2.set(camera.position);
                tmp2.add(0f, -height, 0f);
                if (!Config.NO_CLIPPING && (manager.collidesSphereAllWith(tmp2, size) || manager.collidesSphereAllWith(camera.position, size))) {
                    camera.position.set(prev);
                }
            }
        } else {
            // Gravity
            velocity.add(0f, -Time.getDelta() * 80f, 0f);

            // Friction
            velocity.scl((float) Math.pow(.9f, Time.getDelta()));
        }

        camera.update(true);
    }


    public void dispose() {
    }
}
