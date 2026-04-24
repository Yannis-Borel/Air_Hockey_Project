package projet.M1.hud;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import projet.M1.game.GameRules;
import projet.M1.game.PowerUpManager;
import projet.M1.game.PowerUpManager.Type;

/**
 * Affiche :
 *   — Scores 3D posés sur la table (CrispLabel dans rootNode)
 *   — Effets power-up actifs avec timers (CrispLabel dans guiNode)
 *   — Label de round en tournoi (CrispLabel dans guiNode)
 */
public class HUDManager {

    // Hauteur des labels 3D en world-units
    private static final float SCORE_H  = 2.0f;
    // Hauteur des labels 2D en pixels
    private static final float EFFECT_H = 22f;
    private static final float TOURN_H  = 24f;

    private static final float BASE_ALPHA  = 0.82f;
    private static final float FLASH_ALPHA = 1f;
    private static final float TEXT_Y      = 0.08f;
    private static final float Z_P1        = -7f;
    private static final float Z_P2        =  7f;

    private final GameRules    gameRules;
    private final Node         rootNode;
    private final Node         guiNode;
    private final AssetManager assetManager;
    private final int          screenW, screenH;

    // Scores 3D
    private final CrispLabel scoreP1Label;
    private final CrispLabel scoreP2Label;
    private final Quaternion flatRot;
    private int lastS1 = -1;
    private int lastS2 = -1;

    // Effets power-up 2D
    private final CrispLabel[] effectLines = new CrispLabel[4];
    private final Node         effectsNode;

    // Label tournoi 2D
    private CrispLabel tournLabel = null;

    private PowerUpManager powerUpManager = null;

    public HUDManager(AssetManager assetManager, Node rootNode, Node guiNode,
                      int screenW, int screenH, GameRules gameRules) {
        this.assetManager = assetManager;
        this.rootNode     = rootNode;
        this.guiNode      = guiNode;
        this.gameRules    = gameRules;
        this.screenW      = screenW;
        this.screenH      = screenH;

        flatRot = new Quaternion();
        flatRot.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

        // Scores 3D
        scoreP1Label = new CrispLabel(assetManager, SCORE_H, new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
        scoreP2Label = new CrispLabel(assetManager, SCORE_H, new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
        scoreP1Label.depthTest = true;
        scoreP2Label.depthTest = true;
        scoreP1Label.setLocalRotation(flatRot);
        scoreP2Label.setLocalRotation(flatRot);
        scoreP1Label.setQueueBucket(RenderQueue.Bucket.Transparent);
        scoreP2Label.setQueueBucket(RenderQueue.Bucket.Transparent);
        rootNode.attachChild(scoreP1Label);
        rootNode.attachChild(scoreP2Label);
        forceRefresh();

        // Effets 2D
        effectsNode = new Node("effectsHUD");
        float lineH = EFFECT_H * 1.25f;
        for (int i = 0; i < effectLines.length; i++) {
            effectLines[i] = new CrispLabel(assetManager, EFFECT_H, ColorRGBA.White);
            effectLines[i].setLocalTranslation(10f,
                    10f + (effectLines.length - 1 - i) * lineH, 1f);
            effectsNode.attachChild(effectLines[i]);
        }
        guiNode.attachChild(effectsNode);
    }

    public void setPowerUpManager(PowerUpManager pm) { this.powerUpManager = pm; }

    public void forceRefreshScores() {
        lastS1 = -1;
        lastS2 = -1;
    }

    public void setTournamentLabel(String text) {
        if (text == null) {
            if (tournLabel != null) { guiNode.detachChild(tournLabel); tournLabel = null; }
            return;
        }
        if (tournLabel == null) {
            tournLabel = new CrispLabel(assetManager, TOURN_H, new ColorRGBA(1f, 0.8f, 0.2f, 1f));
            guiNode.attachChild(tournLabel);
        }
        tournLabel.setText(text);
        tournLabel.setLocalTranslation(
                screenW / 2f - tournLabel.getWidth() / 2f,
                screenH - TOURN_H - 8f, 1f);
    }

    // ------------------------------------------------------------------

    public void update() {
        updateScores();
        updateFlash();
        updateEffectsOverlay();
    }

    private void updateScores() {
        int s1 = gameRules.getScoreP1();
        int s2 = gameRules.getScoreP2();
        if (s1 != lastS1) { scoreP1Label.setText(String.valueOf(s1)); placeScore(scoreP1Label, Z_P1); lastS1 = s1; }
        if (s2 != lastS2) { scoreP2Label.setText(String.valueOf(s2)); placeScore(scoreP2Label, Z_P2); lastS2 = s2; }
    }

    private void updateFlash() {
        ColorRGBA c1, c2;
        if (gameRules.getState() == GameRules.State.GOAL_PAUSE) {
            float t   = gameRules.getPauseTimer() / gameRules.getPauseDuration();
            float a   = BASE_ALPHA + (FLASH_ALPHA - BASE_ALPHA) * FastMath.sin(t * FastMath.PI);
            int scorer = gameRules.getLastScorer();
            c1 = new ColorRGBA(1f, 1f, 1f, scorer == 1 ? a        : BASE_ALPHA);
            c2 = new ColorRGBA(1f, 1f, 1f, scorer == 2 ? a        : BASE_ALPHA);
        } else {
            c1 = c2 = new ColorRGBA(1f, 1f, 1f, BASE_ALPHA);
        }
        // Mise à jour couleur en rebakant uniquement si changée (optimisation simple)
        scoreP1Label.color = c1;
        scoreP2Label.color = c2;
    }

    private void updateEffectsOverlay() {
        if (powerUpManager == null) {
            for (CrispLabel l : effectLines) l.setText("");
            return;
        }
        int line = 0;

        if (powerUpManager.isSpeedOn() && line < effectLines.length)
            setEffect(line++, Type.SPEED_PLUS, powerUpManager.getSpeedTimer(), "");
        if (powerUpManager.isPuckSizeOn() && line < effectLines.length) {
            Type t = powerUpManager.getPuckSizeFactor() > 1f ? Type.PUCK_BIGGER : Type.PUCK_SMALLER;
            setEffect(line++, t, powerUpManager.getPuckSizeTimer(), "");
        }
        if (powerUpManager.isP1PadOn() && line < effectLines.length) {
            Type t = powerUpManager.getP1PadFactor() > 1f ? Type.PADDLE_PLUS : Type.PADDLE_MINUS;
            setEffect(line++, t, powerUpManager.getP1PadTimer(), "P1 ");
        }
        if (powerUpManager.isP2PadOn() && line < effectLines.length) {
            Type t = powerUpManager.getP2PadFactor() > 1f ? Type.PADDLE_PLUS : Type.PADDLE_MINUS;
            setEffect(line++, t, powerUpManager.getP2PadTimer(), "P2 ");
        }
        for (int i = line; i < effectLines.length; i++) effectLines[i].setText("");
    }

    private void setEffect(int idx, Type t, float timer, String prefix) {
        effectLines[idx].color = t.color;
        effectLines[idx].setText(prefix + t.label + " : " + String.format("%.1f", timer) + "s");
    }

    // ------------------------------------------------------------------

    private void placeScore(CrispLabel label, float worldZ) {
        // Le quad a son coin bas-gauche à l'origine locale → on centre
        label.setLocalTranslation(
                -label.getWidth()  / 2f,
                TEXT_Y,
                worldZ + label.getHeight() / 2f);
    }

    private void forceRefresh() {
        scoreP1Label.setText(String.valueOf(gameRules.getScoreP1()));
        scoreP2Label.setText(String.valueOf(gameRules.getScoreP2()));
        placeScore(scoreP1Label, Z_P1);
        placeScore(scoreP2Label, Z_P2);
        lastS1 = gameRules.getScoreP1();
        lastS2 = gameRules.getScoreP2();
    }

    public void cleanup() {
        rootNode.detachChild(scoreP1Label);
        rootNode.detachChild(scoreP2Label);
        guiNode.detachChild(effectsNode);
        if (tournLabel != null) guiNode.detachChild(tournLabel);
    }
}
