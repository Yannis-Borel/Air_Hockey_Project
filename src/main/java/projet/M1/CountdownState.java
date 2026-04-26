package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import projet.M1.hud.CrispLabel;

/**
 * Affiche le décompte 3 -> 2 -> 1 -> GO! au centre de l'écran avant le début de la partie.
 * Joue un bip sonore à chaque chiffre et un son GO! à la fin.
 * Restaure la vitesse du palet sauvegardée et se retire automatiquement.
 */
public class CountdownState extends AbstractAppState {

    private SimpleApplication app;
    private final Main mainApp;
    private Node guiNode;
    private CrispLabel countLabel;

    private float timer = 0f;
    private int count = 3;

    private int screenW, screenH;

    /**
     * Crée le décompte lié à l'instance Main pour accéder au SoundManager
     * et restaurer la vitesse du palet à la fin.
     */
    public CountdownState(Main mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Crée le label de décompte centré à l'écran et l'attache au guiNode.
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();

        countLabel = new CrispLabel(app.getAssetManager(), 120f, ColorRGBA.White);
        updateDisplay();
        guiNode.attachChild(countLabel);
    }

    /**
     * Décrémente le compteur chaque seconde.
     * Quand count < 0, détache cet état pour lancer la partie.
     */
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

    /**
     * Met à jour le label avec le chiffre courant ou "GO!", sa couleur
     * et joue le son correspondant (bip pour les chiffres, go pour la fin).
     */
    private void updateDisplay() {
        ColorRGBA color;
        String text;
        if (count > 0) {
            color = new ColorRGBA(1f, 0.75f, 0.1f, 1f);
            text  = String.valueOf(count);
            if (mainApp.getSoundManager() != null) mainApp.getSoundManager().playCountBeep();
        } else {
            color = new ColorRGBA(0.2f, 1f, 0.3f, 1f);
            text  = "GO!";
            if (mainApp.getSoundManager() != null) mainApp.getSoundManager().playGo();
        }
        countLabel.setColorAndText(color, text);
        countLabel.setLocalTranslation(
                screenW / 2f - countLabel.getWidth()  / 2f,
                screenH / 2f + countLabel.getHeight() / 2f,
                2f);
    }

    /**
     * Détache le label du guiNode, restaure la vitesse du palet
     * et réactive le curseur.
     */
    @Override
    public void cleanup() {
        super.cleanup();
        guiNode.detachChild(countLabel);
        mainApp.restorePuckVelocity();
        app.getInputManager().setCursorVisible(false);
    }
}