package projet.M1.input;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Table;

/**
 * Gère les entrées clavier d'un joueur.
 *
 * Instancier avec un préfixe unique ("p1_", "p2_") pour éviter
 * les conflits de mappings JME quand les deux joueurs sont actifs.
 *
 * La zone autorisée est détectée automatiquement selon le côté du paddle.
 * Les raquettes ne peuvent pas entrer dans la zone neutre (+-NEUTRAL_Z).
 */
public class PlayerInputHandler {

    private static final float SPEED = 10f;

    private final Paddle paddle;
    private final ActionListener keyListener;
    private final String[] mappingNames;

    private boolean up, down, left, right;

    private final float minX, maxX, minZ, maxZ;

    /**
     * Enregistre les mappings clavier pour les quatre directions du joueur.
     * Calcule automatiquement les limites de déplacement selon le côté de la raquette :
     * Z négatif pour P1, Z positif pour P2, avec la zone neutre comme frontière.
     */
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
            // P1 : côté Z négatif, ne peut pas dépasser la limite de la zone neutre
            minZ = -Table.HALF_L + r;
            maxZ = -Table.NEUTRAL_Z - r;
        } else {
            // P2 : côté Z positif, ne peut pas dépasser la limite de la zone neutre
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

    /**
     * Calcule le déplacement de la raquette selon les touches enfoncées,
     * applique les limites de zone et déplace la raquette via moveTo().
     */
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

    /**
     * Supprime les mappings clavier enregistrés pour ce joueur.
     * À appeler lors du retour au menu ou de la fin de partie.
     */
    public void cleanup(InputManager inputManager) {
        inputManager.removeListener(keyListener);
        for (String m : mappingNames) {
            if (inputManager.hasMapping(m)) inputManager.deleteMapping(m);
        }
    }
}