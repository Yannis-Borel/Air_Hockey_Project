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
 * La rondelle (puck) — cylindre plat posé sur la surface de la table.
 *
 * Le radius peut être modifié dynamiquement par les power-ups.
 */
public class Puck {

    public static final float RADIUS = 0.4f;
    public static final float HEIGHT = 0.15f;

    private final Node     node;
    private final Vector3f velocity = new Vector3f(0, 0, 0);
    private float currentRadius = RADIUS;

    public Puck(AssetManager assetManager) {
        node = new Node("puck");

        Cylinder shape = new Cylinder(2, 32, RADIUS, HEIGHT, true);
        Geometry geo   = new Geometry("puckGeo", shape);
        geo.rotate(-FastMath.HALF_PI, 0, 0);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient",   new ColorRGBA(0.04f, 0.04f, 0.04f, 1f));
        mat.setColor("Diffuse",   new ColorRGBA(0.10f, 0.10f, 0.12f, 1f));
        mat.setColor("Specular",  new ColorRGBA(0.90f, 0.90f, 0.90f, 1f));
        mat.setFloat("Shininess", 110f);
        geo.setMaterial(mat);

        // Anneau blanc sur le dessus — décalé légèrement en Y
        Cylinder ring = new Cylinder(2, 32, RADIUS * 0.68f, HEIGHT + 0.01f, true);
        Geometry ringGeo = new Geometry("puckRing", ring);
        ringGeo.rotate(-FastMath.HALF_PI, 0, 0);
        ringGeo.setLocalTranslation(0f, 0.01f, 0f);
        Material ringMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        ringMat.setBoolean("UseMaterialColors", true);
        ringMat.setColor("Ambient",   new ColorRGBA(0.4f, 0.4f, 0.4f, 1f));
        ringMat.setColor("Diffuse",   new ColorRGBA(0.9f, 0.9f, 0.9f, 1f));
        ringMat.setColor("Specular",  new ColorRGBA(1f,   1f,   1f,   1f));
        ringMat.setFloat("Shininess", 80f);
        ringGeo.setMaterial(ringMat);

        node.attachChild(geo);
        node.attachChild(ringGeo);
        node.setLocalTranslation(0f, HEIGHT / 2f, 0f);
    }

    public void setPosition(float x, float z) {
        node.setLocalTranslation(x, HEIGHT / 2f, z);
    }

    public void setVelocity(float vx, float vz) {
        velocity.set(vx, 0, vz);
    }

    /** Modifie le rayon courant (power-up). Met à jour l'échelle visuelle (XZ). */
    public void setRadius(float r) {
        currentRadius = r;
        float s = r / RADIUS;
        node.setLocalScale(s, 1f, s);
    }

    public float    getRadius()   { return currentRadius; }
    public Vector3f getPosition() { return node.getLocalTranslation(); }
    public Vector3f getVelocity() { return velocity; }
    public Node     getNode()     { return node; }
}
