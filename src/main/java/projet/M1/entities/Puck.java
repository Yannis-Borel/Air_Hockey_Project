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
 * Axe de déplacement : X et Z (la table est dans le plan XZ, Y = hauteur).
 * La vélocité est stockée ici ; c'est PhysicsEngine qui la mettra à jour.
 */
public class Puck {

    public static final float RADIUS = 0.4f;
    public static final float HEIGHT = 0.15f;

    private final Node node;

    // Vitesse en unités/seconde sur X et Z
    private final Vector3f velocity = new Vector3f(0, 0, 0);

    public Puck(AssetManager assetManager) {
        node = new Node("puck");

        // Cylindre JME orienté sur Z par défaut → on le fait pivoter sur Y
        Cylinder shape = new Cylinder(2, 32, RADIUS, HEIGHT, true);
        Geometry geo = new Geometry("puckGeo", shape);
        geo.rotate(-FastMath.HALF_PI, 0, 0);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.08f, 1f)); // noir quasi-mat
        geo.setMaterial(mat);

        // Disque de couleur sur le dessus pour le rendre visible
        Cylinder topShape = new Cylinder(2, 32, RADIUS * 0.7f, HEIGHT + 0.01f, true);
        Geometry topGeo = new Geometry("puckTop", topShape);
        topGeo.rotate(-FastMath.HALF_PI, 0, 0);
        Material topMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        topMat.setColor("Color", new ColorRGBA(0.9f, 0.9f, 0.9f, 1f)); // cercle blanc
        topGeo.setMaterial(topMat);

        node.attachChild(geo);
        node.attachChild(topGeo);

        // Sur la surface de la table (Y = 0)
        node.setLocalTranslation(0f, HEIGHT / 2f, 0f);
    }

    /** Appelé à chaque frame par la boucle principale (avant la détection de collision). */
    public void update(float tpf) {
        Vector3f pos = node.getLocalTranslation();
        node.setLocalTranslation(
            pos.x + velocity.x * tpf,
            pos.y,
            pos.z + velocity.z * tpf
        );
    }

    public void setPosition(float x, float z) {
        node.setLocalTranslation(x, HEIGHT / 2f, z);
    }

    public void setVelocity(float vx, float vz) {
        velocity.set(vx, 0, vz);
    }

    public Vector3f getPosition() { return node.getLocalTranslation(); }
    public Vector3f getVelocity() { return velocity; }
    public Node     getNode()     { return node; }
}
