package projet.M1.bonus;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Représente un bonus posé sur le terrain.
 * Visuellement : un petit rectangle coloré posé à plat sur la table.
 */
public class Bonus {

    public static final float SIZE   = 0.6f;  // demi-côté du carré
    public static final float HEIGHT = 0.05f; // hauteur au-dessus de la surface

    private final Node      node;
    private final BonusType type;

    public Bonus(AssetManager assetManager, BonusType type, float x, float z) {
        this.type = type;

        node = new Node("bonus_" + type.name());

        Box shape = new Box(SIZE, HEIGHT, SIZE);
        Geometry geo = new Geometry("bonusGeo", shape);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", type.color);
        geo.setMaterial(mat);

        node.attachChild(geo);
        node.setLocalTranslation(x, HEIGHT, z);
    }

    public BonusType getType()   { return type; }
    public Node      getNode()   { return node; }
    public Vector3f  getPosition() { return node.getLocalTranslation(); }
}