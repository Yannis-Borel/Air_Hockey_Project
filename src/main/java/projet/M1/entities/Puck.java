package projet.M1.entities;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;

public class Puck {

    public static final float RADIUS = 0.4f;
    public static final float HEIGHT = 0.15f;

    // Taux de dissipation du spin
    private static final float SPIN_DECAY = 2.5f;

    private final Node node;

    // Vitesse en unités/seconde sur X et Z
    private final Vector3f velocity = new Vector3f(0, 0, 0);

    private float spin = 0f;

    public Puck(AssetManager assetManager) {
        node = new Node("puck");

        Cylinder shape = new Cylinder(2, 32, RADIUS, HEIGHT, true);
        Geometry geo = new Geometry("puckGeo", shape);
        geo.rotate(-FastMath.HALF_PI, 0, 0);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.08f, 1f));
        geo.setMaterial(mat);

        Cylinder topShape = new Cylinder(2, 32, RADIUS * 0.7f, HEIGHT + 0.01f, true);
        Geometry topGeo = new Geometry("puckTop", topShape);
        topGeo.rotate(-FastMath.HALF_PI, 0, 0);
        Material topMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        topMat.setColor("Color", new ColorRGBA(0.9f, 0.9f, 0.9f, 1f));
        topGeo.setMaterial(topMat);

        node.attachChild(geo);
        node.attachChild(topGeo);

        node.setLocalTranslation(0f, HEIGHT / 2f, 0f);
    }


    // Dissipation du spin
    public void applySpinDecay(float tpf) {
        if (spin == 0f) return;
        float decay = SPIN_DECAY * tpf;
        if (Math.abs(spin) <= decay) {
            spin = 0f;
        } else {
            spin -= Math.signum(spin) * decay;
        }
    }

    public void setPosition(float x, float z) {
        node.setLocalTranslation(x, HEIGHT / 2f, z);
    }

    public void setVelocity(float vx, float vz) {
        velocity.set(vx, 0, vz);
    }

    public void setSpin(float spin) { this.spin = spin; }
    public float getSpin() { return spin; }

    public Vector3f getPosition() { return node.getLocalTranslation(); }
    public Vector3f getVelocity() { return velocity; }
    public Node getNode() { return node; }
}