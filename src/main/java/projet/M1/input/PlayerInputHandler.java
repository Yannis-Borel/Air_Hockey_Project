package projet.M1.input;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Table;

public class PlayerInputHandler {

    private static final float SPEED = 10f;

    private final Paddle paddle;
    private final ActionListener keyListener;
    private final String[] mappingNames;

    private boolean up, down, left, right;

    private final float minX, maxX, minZ, maxZ;

    public PlayerInputHandler(InputManager inputManager, Paddle paddle,
                              String prefix,
                              int keyUp, int keyDown, int keyLeft, int keyRight) {
        this.paddle = paddle;

        String mUp = prefix + "up";
        String mDown = prefix + "down";
        String mLeft = prefix + "left";
        String mRight = prefix + "right";
        mappingNames = new String[]{ mUp, mDown, mLeft, mRight };

        float r = Paddle.RADIUS;
        minX = -Table.HALF_W + r;
        maxX = Table.HALF_W - r;

        if (paddle.getPosition().z < 0) {
            // P1, ne peut pas dépasser la limite de la zone neutre
            minZ = -Table.HALF_L + r;
            maxZ = -Table.NEUTRAL_Z - r;
        } else {
            // P2, ne peut pas dépasser la limite de la zone neutre
            minZ = Table.NEUTRAL_Z + r;
            maxZ = Table.HALF_L - r;
        }

        keyListener = (name, isPressed, tpf) -> {
            if (name.equals(mUp)) up = isPressed;
            else if (name.equals(mDown)) down = isPressed;
            else if (name.equals(mLeft)) left = isPressed;
            else if (name.equals(mRight)) right = isPressed;
        };

        inputManager.addMapping(mUp, new KeyTrigger(keyUp));
        inputManager.addMapping(mDown, new KeyTrigger(keyDown));
        inputManager.addMapping(mLeft, new KeyTrigger(keyLeft));
        inputManager.addMapping(mRight, new KeyTrigger(keyRight));
        inputManager.addListener(keyListener, mappingNames);
    }

    public void update(float tpf) {
        Vector3f pos = paddle.getPosition();
        float x = pos.x;
        float z = pos.z;

        if (up) z -= SPEED * tpf;
        if (down) z += SPEED * tpf;
        if (left) x -= SPEED * tpf;
        if (right) x += SPEED * tpf;

        x = Math.max(minX, Math.min(maxX, x));
        z = Math.max(minZ, Math.min(maxZ, z));

        paddle.moveTo(x, z, tpf);
    }

    public void cleanup(InputManager inputManager) {
        inputManager.removeListener(keyListener);
        for (String m : mappingNames) {
            if (inputManager.hasMapping(m)) inputManager.deleteMapping(m);
        }
    }
}