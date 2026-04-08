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
import projet.M1.game.GameRules;

/**
 * Affiche les scores directement sur le terrain en 3D.
 *
 * Les chiffres sont posés à plat sur la table, blancs semi-transparents,
 * et pulsent en jaune quand un but vient d'être marqué.
 */
public class HUDManager {

    private static final float TEXT_SIZE   = 2f;
    private static final float BASE_ALPHA  = 0.82f;  // blanc bien visible
    private static final float FLASH_ALPHA = 1f;     // opaque lors d'un but
    private static final float TEXT_Y      = 0.08f;  // juste au-dessus de la surface

    // Le texte s'étend en -Z world (rotation -HALF_PI autour X).
    // Pour P1 (Z négatif) l'ancre se retrouve côté ligne centrale → bien.
    // Pour P2 (Z positif) l'ancre se retrouve côté ligne de but → on compense
    // en rapprochant Z_P2 pour que les deux baselines soient symétriques / ~3.5 u de la ligne.
    private static final float Z_P1 = -8f;
    private static final float Z_P2 =  3f;

    private final BitmapText scoreP1Text;
    private final BitmapText scoreP2Text;
    private final GameRules  gameRules;
    private final Node       rootNode;

    private int lastS1 = -1;
    private int lastS2 = -1;

    public HUDManager(AssetManager assetManager, Node rootNode, GameRules gameRules) {
        this.rootNode  = rootNode;
        this.gameRules = gameRules;

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // Rotation : couche le texte à plat sur la table (face vers le haut)
        // fromAngleAxis(-90° autour X) : axe Z local du texte → axe Y monde (vers la caméra)
        Quaternion flatOnTable = new Quaternion();
        flatOnTable.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

        scoreP1Text = buildText(font, flatOnTable);
        scoreP2Text = buildText(font, flatOnTable);

        rootNode.attachChild(scoreP1Text);
        rootNode.attachChild(scoreP2Text);

        forceRefresh();
    }

    private BitmapText buildText(BitmapFont font, Quaternion rotation) {
        BitmapText t = new BitmapText(font);
        t.setSize(TEXT_SIZE);
        t.setColor(new ColorRGBA(1f, 1f, 1f, BASE_ALPHA));
        t.setLocalRotation(rotation);
        t.setQueueBucket(RenderQueue.Bucket.Transparent);
        return t;
    }

    // Recentre le texte autour de X=0 et de worldZ
    // Avec la rotation -HALF_PI autour X : local Y → world -Z, donc on décale de +lineHeight/2
    // pour que le centre visuel du chiffre tombe exactement sur worldZ.
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

    /** Appelé à chaque frame depuis simpleUpdate(). */
    public void update() {
        // Mise à jour du contenu seulement si le score a changé
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
            // Pulsation sinusoïdale en blanc pendant la pause
            float t     = gameRules.getPauseTimer() / gameRules.getPauseDuration();
            float alpha = BASE_ALPHA + (FLASH_ALPHA - BASE_ALPHA) * FastMath.sin(t * FastMath.PI);

            int scorer = gameRules.getLastScorer();
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
    }
}
