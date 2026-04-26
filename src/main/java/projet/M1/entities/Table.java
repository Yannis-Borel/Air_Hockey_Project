package projet.M1.entities;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.math.FastMath;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

/**
 * Table de jeu Air Hockey.
 *
 * La largeur du but est dynamique : setGoalWidth() reconstruit les murs de fond
 * et met à jour la valeur lue par PhysicsEngine.
 */
public class Table {

    // Dimensions fixes
    public static final float WIDTH = 10f;
    public static final float LENGTH = 20f;
    public static final float WALL_HEIGHT = 0.5f;
    public static final float WALL_THICK = 0.25f;
    public static final float GOAL_WIDTH = 3.5f;     // valeur par défaut

    public static final float HALF_W = WIDTH / 2f;
    public static final float HALF_L = LENGTH / 2f;
    public static final float NEUTRAL_Z = HALF_L / 4f;
    public static final float CORNER_R = 1.2f;          // rayon des coins arrondis

    private final Node tableNode;
    private final Node endWallsNode = new Node("endWalls");
    private final AssetManager assetManager;

    private float currentGoalWidth = GOAL_WIDTH;

    /**
     * Construit la table complète : surface, zone neutre, bandes,
     * murs de fond, coins arrondis et marquages au sol.
     */
    public Table(AssetManager assetManager) {
        this.assetManager = assetManager;
        tableNode = new Node("table");
        tableNode.attachChild(endWallsNode);
        build();
    }

    /**
     * Orchestre la construction de tous les éléments visuels de la table.
     */
    private void build() {
        buildSurface();
        buildNeutralZone();
        buildSideWalls();
        buildEndWalls();
        buildCorners();
        buildZoneMarkers();
    }

    /**
     * Ajoute des cylindres aux quatre coins de la table pour arrondir les angles
     * et éviter que la rondelle ne se coince dans les coins droits.
     */
    private void buildCorners() {
        Material mat = wallMaterial();
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                Cylinder cyl = new Cylinder(2, 24, CORNER_R, WALL_HEIGHT, true);
                Geometry geo = new Geometry("corner_" + sx + "_" + sz, cyl);
                geo.rotate(-FastMath.HALF_PI, 0, 0);
                geo.setLocalTranslation(sx * HALF_W, WALL_HEIGHT / 2f, sz * HALF_L);
                geo.setMaterial(mat.clone());
                tableNode.attachChild(geo);
            }
        }
    }

    // Surface principale (Lighting pour look feutrine)

    /**
     * Construit la surface de jeu principale avec un matériau Lighting
     * imitant l'aspect feutrine verte d'une vraie table d'air hockey.
     */
    private void buildSurface() {
        Box shape = new Box(HALF_W, 0.05f, HALF_L);
        Geometry geo = new Geometry("tableSurface", shape);
        geo.setLocalTranslation(0f, -0.05f, 0f);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", new ColorRGBA(0.02f, 0.12f, 0.03f, 1f));
        mat.setColor("Diffuse", new ColorRGBA(0.05f, 0.28f, 0.07f, 1f));
        mat.setColor("Specular", ColorRGBA.Black);
        mat.setFloat("Shininess", 1f);
        geo.setMaterial(mat);
        tableNode.attachChild(geo);
    }

    // Zone neutre semi-transparente

    /**
     * Construit la zone neutre centrale en jaune semi-transparent.
     * Les raquettes ne peuvent pas entrer dans cette zone.
     */
    private void buildNeutralZone() {
        Box shape = new Box(HALF_W, 0.04f, NEUTRAL_Z);
        Geometry geo = new Geometry("neutralZone", shape);
        geo.setLocalTranslation(0f, 0.01f, 0f);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1f, 0.85f, 0f, 0.28f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        tableNode.attachChild(geo);
    }

    // Bandes latérales

    /**
     * Construit les deux bandes latérales (gauche et droite)
     * contre lesquelles la rondelle rebondit.
     */
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

    // Murs de fond avec ouverture de but

    /**
     * Construit les murs de fond des deux camps avec l'ouverture de but au centre.
     * Chaque extrémité est composée de deux segments symétriques laissant un espace
     * de largeur currentGoalWidth au milieu. Ces murs sont reconstruits à chaque
     * appel à setGoalWidth() pour refléter la nouvelle largeur de but.
     */
    private void buildEndWalls() {
        float gw = currentGoalWidth;
        float sidelen = (WIDTH - gw) / 2f;
        float xOffset = gw / 2f + sidelen / 2f;

        Material mat = wallMaterial();
        Box seg = new Box(sidelen / 2f, WALL_HEIGHT / 2f, WALL_THICK / 2f);

        for (int side : new int[]{-1, 1}) {
            float zPos = side * (HALF_L + WALL_THICK / 2f);

            Geometry leftSeg = new Geometry("endWall_L_" + side, seg);
            leftSeg.setLocalTranslation(-xOffset, WALL_HEIGHT / 2f, zPos);
            leftSeg.setMaterial(mat.clone());
            endWallsNode.attachChild(leftSeg);

            Geometry rightSeg = new Geometry("endWall_R_" + side, seg.deepClone());
            rightSeg.setLocalTranslation(xOffset, WALL_HEIGHT / 2f, zPos);
            rightSeg.setMaterial(mat.clone());
            endWallsNode.attachChild(rightSeg);
        }
    }

    // Marquages

    /**
     * Ajoute les lignes de marquage au sol :
     * ligne médiane blanche, limites de zone neutre blanches et lignes de but jaunes.
     */
    private void buildZoneMarkers() {
        addHLine("midLine", new ColorRGBA(1f, 1f, 1f, 1f), 0f);
        addHLine("neutralLineP1", new ColorRGBA(1f, 1f, 1f, 1f), -NEUTRAL_Z);
        addHLine("neutralLineP2", new ColorRGBA(1f, 1f, 1f, 1f), NEUTRAL_Z);
        addHLine("goalLineP1", new ColorRGBA(1f, 0.8f, 0f, 1f), -HALF_L);
        addHLine("goalLineP2", new ColorRGBA(1f, 0.8f, 0f, 1f),  HALF_L);
    }

    /**
     * Ajoute une ligne horizontale colorée sur la surface de la table à la position Z donnée.
     */
    private void addHLine(String name, ColorRGBA color, float zPos) {
        Box shape = new Box(HALF_W, 0.01f, 0.04f);
        Geometry geo = new Geometry(name, shape);
        geo.setLocalTranslation(0f, 0.02f, zPos);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        tableNode.attachChild(geo);
    }

    /**
     * Crée un matériau Lighting gris métallique pour les bandes et murs de la table.
     */
    private Material wallMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", new ColorRGBA(0.3f, 0.3f, 0.35f, 1f));
        mat.setColor("Diffuse", new ColorRGBA(0.7f, 0.7f, 0.75f, 1f));
        mat.setColor("Specular", new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
        mat.setFloat("Shininess", 25f);
        return mat;
    }

    /** Modifie la largeur du but et reconstruit les murs de fond visuellement. */
    public void setGoalWidth(float w) {
        currentGoalWidth = w;
        endWallsNode.detachAllChildren();
        buildEndWalls();
    }

    /** Retourne la largeur de but actuellement en vigueur (lue par PhysicsEngine). */
    public float getCurrentGoalWidth() { return currentGoalWidth; }

    /** Retourne le nœud racine de la table à attacher à la scène. */
    public Node  getNode() { return tableNode; }
}