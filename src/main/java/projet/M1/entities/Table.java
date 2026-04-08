package projet.M1.entities;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Représente la table de jeu Air Hockey.
 *
 * Dimensions (axe X = largeur, axe Z = longueur) :
 *   - Table : 10 x 20 unités, centrée en (0, 0, 0)
 *   - Joueur 1 : côté Z négatif (Z < -NEUTRAL_Z)
 *   - Joueur 2 : côté Z positif (Z >  NEUTRAL_Z)
 *   - Zone neutre : Z in [-NEUTRAL_Z, +NEUTRAL_Z] = 1/3 de chaque moitié
 *
 * Coordonnées X :
 *   - Bande gauche  : X = -5
 *   - Bande droite  : X = +5
 *
 * Les buts sont centrés sur X=0, ouverture de GOAL_WIDTH unités.
 */
public class Table {

    // --- Dimensions fixes ---
    public static final float WIDTH         = 10f;
    public static final float LENGTH        = 20f;
    public static final float WALL_HEIGHT   = 0.5f;
    public static final float WALL_THICK    = 0.25f;

    // Taille de l'ouverture du but (centrée sur X=0)
    public static final float GOAL_WIDTH    = 3.5f;

    // Limites utiles pour la physique
    public static final float HALF_W        = WIDTH  / 2f;   // 5f
    public static final float HALF_L        = LENGTH / 2f;   // 10f

    // Zone neutre : 1/3 de chaque moitié → Z in [-NEUTRAL_Z, +NEUTRAL_Z]
    public static final float NEUTRAL_Z     = HALF_L / 4f;   // ≈ 3.33

    private final Node tableNode;
    private final AssetManager assetManager;

    public Table(AssetManager assetManager) {
        this.assetManager = assetManager;
        tableNode = new Node("table");
        build();
    }

    private void build() {
        buildSurface();
        buildNeutralZone();
        buildSideWalls();
        buildEndWalls();
        buildZoneMarkers();
    }

    // Surface de jeu principale (fine boîte verte)
    private void buildSurface() {
        Box shape = new Box(HALF_W, 0.05f, HALF_L);
        Geometry geo = new Geometry("tableSurface", shape);
        geo.setLocalTranslation(0f, -0.05f, 0f);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.04f, 0.25f, 0.06f, 1f)); // vert sombre
        geo.setMaterial(mat);
        tableNode.attachChild(geo);
    }

    // Zone neutre jaune semi-transparente entre -NEUTRAL_Z et +NEUTRAL_Z
    private void buildNeutralZone() {
        Box shape = new Box(HALF_W, 0.04f, NEUTRAL_Z);
        Geometry geo = new Geometry("neutralZone", shape);
        geo.setLocalTranslation(0f, 0.01f, 0f); // juste au-dessus de la surface

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1f, 0.85f, 0f, 0.35f)); // jaune semi-transparent
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        tableNode.attachChild(geo);
    }

    // Bandes latérales gauche et droite (rebonds)
    private void buildSideWalls() {
        Material mat = wallMaterial();

        Box shape = new Box(WALL_THICK / 2f, WALL_HEIGHT / 2f, HALF_L + WALL_THICK);

        Geometry leftWall = new Geometry("leftWall", shape);
        leftWall.setLocalTranslation(-HALF_W - WALL_THICK / 2f, WALL_HEIGHT / 2f, 0f);
        leftWall.setMaterial(mat);
        tableNode.attachChild(leftWall);

        Geometry rightWall = new Geometry("rightWall", shape.deepClone());
        rightWall.setLocalTranslation(HALF_W + WALL_THICK / 2f, WALL_HEIGHT / 2f, 0f);
        rightWall.setMaterial(mat.clone());
        tableNode.attachChild(rightWall);
    }

    // Murs de fond avec ouverture de but (deux segments par extrémité)
    private void buildEndWalls() {
        float sidelen = (WIDTH - GOAL_WIDTH) / 2f;
        float xOffset = GOAL_WIDTH / 2f + sidelen / 2f;

        Material mat = wallMaterial();

        for (int side : new int[]{-1, 1}) {
            float zPos = side * (HALF_L + WALL_THICK / 2f);

            Box seg = new Box(sidelen / 2f, WALL_HEIGHT / 2f, WALL_THICK / 2f);

            Geometry leftSeg = new Geometry("endWall_L_" + side, seg);
            leftSeg.setLocalTranslation(-xOffset, WALL_HEIGHT / 2f, zPos);
            leftSeg.setMaterial(mat.clone());
            tableNode.attachChild(leftSeg);

            Geometry rightSeg = new Geometry("endWall_R_" + side, seg.deepClone());
            rightSeg.setLocalTranslation(xOffset, WALL_HEIGHT / 2f, zPos);
            rightSeg.setMaterial(mat.clone());
            tableNode.attachChild(rightSeg);
        }
    }

    // Marquages : ligne médiane, lignes de but jaunes, traits blancs de zone neutre
    private void buildZoneMarkers() {
        // Ligne médiane blanche
        addHLine("midLine",       new ColorRGBA(1f, 1f, 1f, 1f),          0f);
        // Limites de la zone neutre — traits blancs
        addHLine("neutralLineP1", new ColorRGBA(1f, 1f, 1f, 1f), -NEUTRAL_Z);
        addHLine("neutralLineP2", new ColorRGBA(1f, 1f, 1f, 1f),  NEUTRAL_Z);
        // Lignes de but jaunes
        addHLine("goalLineP1",    new ColorRGBA(1f, 0.8f, 0f, 1f), -HALF_L);
        addHLine("goalLineP2",    new ColorRGBA(1f, 0.8f, 0f, 1f),  HALF_L);
    }

    private void addHLine(String name, ColorRGBA color, float zPos) {
        Box shape = new Box(HALF_W, 0.01f, 0.04f);
        Geometry geo = new Geometry(name, shape);
        geo.setLocalTranslation(0f, 0.02f, zPos);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        tableNode.attachChild(geo);
    }

    private Material wallMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.75f, 0.75f, 0.8f, 1f));
        return mat;
    }

    public Node getNode() {
        return tableNode;
    }
}