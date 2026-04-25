package projet.M1.hud;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import projet.M1.bonus.BonusType;
import projet.M1.game.GameRules;
import projet.M1.Main;

public class HUDManager {

    private static final float TEXT_SIZE = 2f;
    private static final float BASE_ALPHA = 0.82f;
    private static final float FLASH_ALPHA = 1f;
    private static final float TEXT_Y = 0.08f;
    private static final float Z_P1 = -8f;
    private static final float Z_P2 = 3f;

    private final BitmapText scoreP1Text;
    private final BitmapText scoreP2Text;
    private final GameRules gameRules;
    private final Node rootNode;

    // Légende des bonus (GUI 2D) — uniquement en mode VS
    private Node legendNode;
    private final Node guiNode;
    private final boolean showLegend;
    private final int screenW;
    private final int screenH;

    private int lastS1 = -1;
    private int lastS2 = -1;

    public HUDManager(AssetManager assetManager, Node rootNode, Node guiNode,
                      GameRules gameRules, Main.GameMode mode,
                      int screenW, int screenH) {
        this.rootNode = rootNode;
        this.guiNode = guiNode;
        this.gameRules = gameRules;
        this.showLegend = (mode == Main.GameMode.VS);
        this.screenW = screenW;
        this.screenH = screenH;

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        Quaternion flatOnTable = new Quaternion();
        flatOnTable.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

        scoreP1Text = buildText(font, flatOnTable);
        scoreP2Text = buildText(font, flatOnTable);

        rootNode.attachChild(scoreP1Text);
        rootNode.attachChild(scoreP2Text);

        if (showLegend) buildLegend(assetManager, font);

        forceRefresh();
    }


    private void buildLegend(AssetManager assetManager, BitmapFont font) {
        legendNode = new Node("bonusLegend");

        float x = screenW - 230f;
        float topY = screenH / 2f + 100f;
        float gap = 38f;
        float size = font.getCharSet().getRenderedSize();

        // Titre
        BitmapText title = makeGuiText(font, "BONUS", size * 1.1f, ColorRGBA.White);
        title.setLocalTranslation(x, topY + 30f, 1f);
        legendNode.attachChild(title);

        // Une ligne par type de bonus
        BonusType[] types = BonusType.values();
        for (int i = 0; i < types.length; i++) {
            BonusType t = types[i];
            float y = topY - i * gap;

            // Carré coloré
            com.jme3.scene.Geometry square = makeColorSquare(assetManager, t.color, 14f);
            square.setLocalTranslation(x, y - 4f, 0.5f);
            legendNode.attachChild(square);

            // Nom court
            BitmapText label = makeGuiText(font, t.shortName, size * 0.95f, ColorRGBA.White);
            label.setLocalTranslation(x + 20f, y + 10f, 1f);
            legendNode.attachChild(label);

            // Description
            BitmapText desc = makeGuiText(font, t.description, size * 0.75f,
                    new ColorRGBA(0.75f, 0.75f, 0.75f, 1f));
            desc.setLocalTranslation(x + 20f, y - 6f, 1f);
            legendNode.attachChild(desc);
        }

        guiNode.attachChild(legendNode);
    }

    private com.jme3.scene.Geometry makeColorSquare(AssetManager assetManager,
                                                    ColorRGBA color, float size) {
        com.jme3.scene.shape.Quad q = new com.jme3.scene.shape.Quad(size, size);
        com.jme3.scene.Geometry geo = new com.jme3.scene.Geometry("legendSquare", q);
        com.jme3.material.Material mat = new com.jme3.material.Material(
                assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }

    private BitmapText makeGuiText(BitmapFont font, String text, float size, ColorRGBA color) {
        BitmapText t = new BitmapText(font);
        t.setSize(size);
        t.setText(text);
        t.setColor(color);
        return t;
    }

    // --- Scores 3D ---

    private BitmapText buildText(BitmapFont font, Quaternion rotation) {
        BitmapText t = new BitmapText(font);
        t.setSize(TEXT_SIZE);
        t.setColor(new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
        t.setLocalRotation(rotation);
        t.setQueueBucket(RenderQueue.Bucket.Transparent);
        return t;
    }

    private void place(BitmapText t, float worldZ) {
        t.setLocalTranslation(-t.getLineWidth() / 2f, TEXT_Y, worldZ + t.getLineHeight() / 2f);
    }

    private void forceRefresh() {
        scoreP1Text.setText(String.valueOf(gameRules.getScoreP1()));
        scoreP2Text.setText(String.valueOf(gameRules.getScoreP2()));
        place(scoreP1Text, Z_P1);
        place(scoreP2Text, Z_P2);
        lastS1 = gameRules.getScoreP1();
        lastS2 = gameRules.getScoreP2();
    }

    public void update() {
        int s1 = gameRules.getScoreP1();
        int s2 = gameRules.getScoreP2();

        if (s1 != lastS1) {
            scoreP1Text.setText(String.valueOf(s1));
            place(scoreP1Text, Z_P1);
            lastS1 = s1;
        }
        if (s2 != lastS2) {
            scoreP2Text.setText(String.valueOf(s2));
            place(scoreP2Text, Z_P2);
            lastS2 = s2;
        }

        updateColors();
    }

    private void updateColors() {
        if (gameRules.getState() == GameRules.State.GOAL_PAUSE) {
            float t     = gameRules.getPauseTimer() / gameRules.getPauseDuration();
            float alpha = BASE_ALPHA + (FLASH_ALPHA - BASE_ALPHA) * FastMath.sin(t * FastMath.PI);
            int scorer  = gameRules.getLastScorer();
            if (scorer == 1) {
                scoreP1Text.setColor(new ColorRGBA(1f, 1f, 1f, alpha));
                scoreP2Text.setColor(new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
            } else {
                scoreP1Text.setColor(new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
                scoreP2Text.setColor(new ColorRGBA(1f, 1f, 1f, alpha));
            }
        } else {
            scoreP1Text.setColor(new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
            scoreP2Text.setColor(new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
        }
    }

    public void cleanup() {
        rootNode.detachChild(scoreP1Text);
        rootNode.detachChild(scoreP2Text);
        if (legendNode != null) guiNode.detachChild(legendNode);
    }
}