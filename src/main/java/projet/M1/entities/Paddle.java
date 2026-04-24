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
 * Raquette (paddle) — cylindre plat.
 *
 * Rayon dynamique : setNominalRadius() pour les changements durables (tournoi),
 * setRadius() pour les effets temporaires (power-ups).
 * Quand un power-up expire, restorer avec setRadius(getNominalRadius()).
 */
public class Paddle {

    public static final float RADIUS = 0.7f;
    public static final float HEIGHT = 0.2f;

    private final Node     node;
    private final float    initX, initZ;
    private final Vector3f velocity     = new Vector3f(0, 0, 0);
    private final Vector3f prevPosition = new Vector3f();

    private float nominalRadius = RADIUS;   // rayon de base (tournoi)
    private float currentRadius = RADIUS;   // rayon effectif (inclut power-ups)

    public Paddle(AssetManager assetManager, ColorRGBA color, float startX, float startZ) {
        this.initX = startX;
        this.initZ = startZ;

        node = new Node("paddle");

        Cylinder body = new Cylinder(2, 32, RADIUS, HEIGHT, true);
        Geometry bodyGeo = new Geometry("paddleBody", body);
        bodyGeo.rotate(-FastMath.HALF_PI, 0, 0);

        Material bodyMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        bodyMat.setBoolean("UseMaterialColors", true);
        bodyMat.setColor("Ambient",   color.mult(0.25f));
        bodyMat.setColor("Diffuse",   color);
        bodyMat.setColor("Specular",  new ColorRGBA(1f, 1f, 1f, 1f));
        bodyMat.setFloat("Shininess", 55f);
        bodyGeo.setMaterial(bodyMat);

        // Bouton central blanc
        Cylinder btn = new Cylinder(2, 32, RADIUS * 0.35f, HEIGHT * 0.5f, true);
        Geometry btnGeo = new Geometry("paddleBtn", btn);
        btnGeo.rotate(-FastMath.HALF_PI, 0, 0);
        btnGeo.setLocalTranslation(0f, HEIGHT / 2f + 0.01f, 0f);

        Material btnMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        btnMat.setBoolean("UseMaterialColors", true);
        btnMat.setColor("Ambient",   new ColorRGBA(0.4f, 0.4f, 0.4f, 1f));
        btnMat.setColor("Diffuse",   ColorRGBA.White);
        btnMat.setColor("Specular",  new ColorRGBA(1f, 1f, 1f, 1f));
        btnMat.setFloat("Shininess", 90f);
        btnGeo.setMaterial(btnMat);

        node.attachChild(bodyGeo);
        node.attachChild(btnGeo);
        node.setLocalTranslation(startX, HEIGHT / 2f, startZ);
        prevPosition.set(node.getLocalTranslation());
    }

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

    public void resetPosition() {
        node.setLocalTranslation(initX, HEIGHT / 2f, initZ);
        velocity.set(0, 0, 0);
        prevPosition.set(node.getLocalTranslation());
    }

    public void setPosition(float x, float z) {
        node.setLocalTranslation(x, HEIGHT / 2f, z);
    }

    /** Rayon de base durable (tournoi). Met aussi à jour le rayon courant. */
    public void setNominalRadius(float r) {
        nominalRadius = r;
        setRadius(r);
    }

    /** Rayon effectif — met à jour l'échelle visuelle (XZ seulement). */
    public void setRadius(float r) {
        currentRadius = r;
        float s = r / RADIUS;
        node.setLocalScale(s, 1f, s);
    }

    public float    getNominalRadius() { return nominalRadius; }
    public float    getRadius()        { return currentRadius; }
    public Vector3f getPosition()      { return node.getLocalTranslation(); }
    public Vector3f getVelocity()      { return velocity; }
    public Node     getNode()          { return node; }
}
