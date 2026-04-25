package projet.M1.entities;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;

/**
 * La raquette (paddle) — cylindre plat, plus grand que la rondelle.
 *
 * Chaque joueur a sa propre instance avec sa couleur.
 * La raquette est contrainte à sa paddle zone par PhysicsEngine / PlayerInputHandler.
 */
public class Paddle {

    public static final float RADIUS = 0.7f;
    public static final float HEIGHT = 0.2f;

    private final Node node;

    // Rayon courant (peut être modifié par un bonus)
    private float currentRadius;

    // Position initiale (pour le reset après un but)
    private final float initX;
    private final float initZ;

    // Vitesse de déplacement du paddle (utilisée pour calculer l'effet smash)
    private final Vector3f velocity = new Vector3f(0, 0, 0);
    private final Vector3f prevPosition = new Vector3f();

    public Paddle(AssetManager assetManager, ColorRGBA color, float startX, float startZ) {
        this.initX = startX;
        this.initZ = startZ;
        this.currentRadius = RADIUS;

        node = new Node("paddle");

        // Corps principal du paddle
        Cylinder body = new Cylinder(2, 32, RADIUS, HEIGHT, true);
        Geometry bodyGeo = new Geometry("paddleBody", body);
        bodyGeo.rotate(-FastMath.HALF_PI, 0, 0);
        Material bodyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bodyMat.setColor("Color", color);
        bodyGeo.setMaterial(bodyMat);

        // Bouton central blanc — décalé vers le haut pour éviter le z-fighting
        Cylinder btn = new Cylinder(2, 32, RADIUS * 0.35f, HEIGHT * 0.5f, true);
        Geometry btnGeo = new Geometry("paddleBtn", btn);
        btnGeo.rotate(-FastMath.HALF_PI, 0, 0);
        btnGeo.setLocalTranslation(0f, HEIGHT / 2f + 0.01f, 0f);
        Material btnMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        btnMat.setColor("Color", ColorRGBA.White);
        btnGeo.setMaterial(btnMat);

        node.attachChild(bodyGeo);
        node.attachChild(btnGeo);

        node.setLocalTranslation(startX, HEIGHT / 2f, startZ);
        prevPosition.set(node.getLocalTranslation());
    }

    /**
     * Déplace la raquette et met à jour la vélocité (utile pour le smash).
     * Appelé par PlayerInputHandler ou AIController.
     */
    public void moveTo(float x, float z, float tpf) {
        prevPosition.set(node.getLocalTranslation());
        node.setLocalTranslation(x, HEIGHT / 2f, z);

        if (tpf > 0) {
            velocity.set(
                    (x - prevPosition.x) / tpf,
                    0,
                    (z - prevPosition.z) / tpf
            );
        }
    }

    /**
     * Replace la raquette à sa position de départ et annule sa vitesse.
     * Remet aussi le scale à 1 pour annuler tout effet Paddle+/Paddle-.
     * Appelé par GameRules après un but.
     */
    public void resetPosition() {
        node.setLocalTranslation(initX, HEIGHT / 2f, initZ);
        node.setLocalScale(1f, 1f, 1f); // annule tout effet de bonus visuel
        currentRadius = RADIUS;         // remet le rayon physique à la valeur initiale
        velocity.set(0, 0, 0);
        prevPosition.set(node.getLocalTranslation());
    }

    /**
     * Applique un facteur multiplicatif au rayon de la raquette.
     * Modifie visuellement le nœud via setLocalScale.
     * Utilisé par BonusManager pour les effets Paddle+ et Paddle-.
     */
    public void scaleRadius(float factor) {
        currentRadius *= factor;
        float scale = currentRadius / RADIUS;
        node.setLocalScale(scale, 1f, scale); // on ne touche pas à Y (hauteur fixe)
    }

    public void setPosition(float x, float z) {
        node.setLocalTranslation(x, HEIGHT / 2f, z);
    }

    public Vector3f getPosition()      { return node.getLocalTranslation(); }
    public Vector3f getVelocity()      { return velocity; }
    public float    getCurrentRadius() { return currentRadius; }
    public Node     getNode()          { return node; }
}