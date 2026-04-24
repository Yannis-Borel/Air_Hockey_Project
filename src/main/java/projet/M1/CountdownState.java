package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import projet.M1.hud.CrispLabel;

public class CountdownState extends AbstractAppState {

    private SimpleApplication app;
    private final Main mainApp;
    private Node guiNode;
    private CrispLabel countLabel;

    private float timer = 0f;
    private int   count = 3;

    private int screenW, screenH;

    public CountdownState(Main mainApp) {
        this.mainApp = mainApp;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app    = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();

        countLabel = new CrispLabel(app.getAssetManager(), 120f, ColorRGBA.White);
        updateDisplay();
        guiNode.attachChild(countLabel);
    }

    @Override
    public void update(float tpf) {
        timer += tpf;
        if (timer < 1f) return;

        timer -= 1f;
        count--;

        if (count < 0) {
            app.getStateManager().detach(this);
            return;
        }

        updateDisplay();
    }

    private void updateDisplay() {
        ColorRGBA color;
        String    text;
        if (count > 0) {
            color = new ColorRGBA(1f, 0.75f, 0.1f, 1f);
            text  = String.valueOf(count);
        } else {
            color = new ColorRGBA(0.2f, 1f, 0.3f, 1f);
            text  = "GO!";
        }
        countLabel.setColorAndText(color, text);
        countLabel.setLocalTranslation(
                screenW / 2f - countLabel.getWidth()  / 2f,
                screenH / 2f + countLabel.getHeight() / 2f,
                2f);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        guiNode.detachChild(countLabel);
        mainApp.restorePuckVelocity();
        app.getInputManager().setCursorVisible(false);
    }
}
