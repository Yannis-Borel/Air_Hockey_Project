package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

public class CountdownState extends AbstractAppState {

    private SimpleApplication app;
    private final Main mainApp;
    private Node guiNode;
    private BitmapText countText;

    private float timer = 0f;
    private int count = 3;

    private int screenW, screenH;

    public CountdownState(Main mainApp) {
        this.mainApp = mainApp;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
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
            // Décompte fini, le jeu reprend
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

    // Recentre le texte à chaque changement
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

        // Restaurer la vitesse du palet sauvegardée avant la pause
        mainApp.restorePuckVelocity();
        app.getInputManager().setCursorVisible(false);
    }
}