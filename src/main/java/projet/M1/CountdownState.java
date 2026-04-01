package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

/**
 * Affiche un décompte 3 → 2 → 1 → GO! au centre de l'écran.
 * Se retire automatiquement une fois terminé.
 */
public class CountdownState extends AbstractAppState {

    private SimpleApplication app;
    private Node guiNode;
    private BitmapText countText;

    private float timer = 0f;
    private int   count = 3;   // commence à 3

    private int screenW, screenH;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app    = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        countText = new BitmapText(font);
        countText.setSize(font.getCharSet().getRenderedSize() * 6f);
        updateDisplay();
        center();
        guiNode.attachChild(countText);
    }

    @Override
    public void update(float tpf) {
        timer += tpf;
        if (timer < 1f) return;

        timer -= 1f;
        count--;

        if (count < 0) {
            // Décompte terminé : on se retire, le jeu reprend
            app.getStateManager().detach(this);
            return;
        }

        updateDisplay();
        center();
    }

    private void updateDisplay() {
        if (count > 0) {
            countText.setText(String.valueOf(count));
            countText.setColor(new ColorRGBA(1f, 0.75f, 0.1f, 1f)); // orange
        } else {
            countText.setText("GO!");
            countText.setColor(new ColorRGBA(0.2f, 1f, 0.3f, 1f));  // vert
        }
    }

    // Recentre le texte à chaque changement (la largeur varie entre "3" et "GO!")
    private void center() {
        countText.setLocalTranslation(
            screenW / 2f - countText.getLineWidth() / 2f,
            screenH / 2f + countText.getHeight() / 2f,
            2f
        );
    }

    @Override
    public void cleanup() {
        super.cleanup();
        guiNode.detachChild(countText);
        // Re-cacher le curseur quand on retourne en jeu
        app.getInputManager().setCursorVisible(false);
    }
}
