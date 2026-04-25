package projet.M1.menu;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import projet.M1.Main;
import projet.M1.Main.GameMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran de fin de partie affiché quand un joueur atteint WIN_SCORE points.
 * Affiche le gagnant et propose "Nouvelle partie" ou "Menu principal".
 */
public class GameOverState extends AbstractAppState {

    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;

    private final int     winner;   // 1 ou 2
    private final GameMode mode;

    private int screenW, screenH;

    private final List<ButtonInfo> buttons = new ArrayList<>();

    private static class ButtonInfo {
        float x, y, w, h;
        Runnable action;
        Geometry bg;
        ColorRGBA baseColor;
        ColorRGBA hoverColor;

        ButtonInfo(float x, float y, float w, float h,
                   Runnable action, Geometry bg, ColorRGBA base) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.action = action; this.bg = bg;
            this.baseColor  = base.clone();
            this.hoverColor = new ColorRGBA(
                    Math.min(base.r * 1.4f, 1f),
                    Math.min(base.g * 1.4f, 1f),
                    Math.min(base.b * 1.4f, 1f),
                    base.a
            );
        }

        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public GameOverState(int winner, GameMode mode) {
        this.winner = winner;
        this.mode   = mode;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();
        font = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        buildScreen();
        registerInput();

        this.app.getInputManager().setCursorVisible(true);
    }

    private void buildScreen() {
        guiRoot = new Node("gameOverRoot");

        // Fond semi-transparent noir
        addQuad(0, 0, screenW, screenH, new ColorRGBA(0f, 0f, 0f, 0.78f), 0f);

        // Couleur du gagnant : rouge pour P1, bleu pour P2
        ColorRGBA winColor = (winner == 1)
                ? new ColorRGBA(0.9f, 0.15f, 0.15f, 1f)
                : new ColorRGBA(0.1f, 0.3f, 0.9f, 1f);

        // Titre "VICTOIRE !"
        BitmapText title = makeText("VICTOIRE !", font.getCharSet().getRenderedSize() * 3f, winColor);
        title.setLocalTranslation(
                screenW / 2f - title.getLineWidth() / 2f,
                screenH / 2f + 200f, 1f
        );
        guiRoot.attachChild(title);

        // Message gagnant
        String winnerName = (winner == 1) ? "Joueur Rouge" : "Joueur Bleu";
        BitmapText msg = makeText(winnerName + " a gagné !", font.getCharSet().getRenderedSize() * 1.8f, ColorRGBA.White);
        msg.setLocalTranslation(
                screenW / 2f - msg.getLineWidth() / 2f,
                screenH / 2f + 120f, 1f
        );
        guiRoot.attachChild(msg);

        float btnW = 280f, btnH = 55f;
        float cx   = screenW / 2f - btnW / 2f;
        float topY = screenH / 2f + 20f;
        float gap  = 75f;

        // Bouton Nouvelle partie
        addButton("Nouvelle partie", cx, topY, btnW, btnH,
                new ColorRGBA(0.1f, 0.5f, 0.1f, 0.95f),
                () -> {
                    app.getStateManager().detach(this);
                    ((Main) app).startGame(mode);
                });

        // Bouton Menu principal
        addButton("Menu principal", cx, topY - gap, btnW, btnH,
                new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),
                () -> {
                    app.getStateManager().detach(this);
                    ((Main) app).returnToMainMenu();
                });

        app.getGuiNode().attachChild(guiRoot);
    }

    private void addButton(String label, float x, float y, float w, float h,
                           ColorRGBA color, Runnable action) {
        Geometry bg = addQuad(x, y, w, h, color, 0.5f);
        BitmapText text = makeText(label, font.getCharSet().getRenderedSize() * 1.3f, ColorRGBA.White);
        text.setLocalTranslation(
                x + w / 2f - text.getLineWidth() / 2f,
                y + h / 2f + text.getHeight() / 3f,
                1f
        );
        guiRoot.attachChild(text);
        buttons.add(new ButtonInfo(x, y, w, h, action, bg, color));
    }

    @Override
    public void update(float tpf) {
        Vector2f m = app.getInputManager().getCursorPosition();
        for (ButtonInfo b : buttons) {
            ColorRGBA c = b.contains(m.x, m.y) ? b.hoverColor : b.baseColor;
            b.bg.getMaterial().setColor("Color", c);
        }
    }

    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        Vector2f m = app.getInputManager().getCursorPosition();
        for (ButtonInfo b : buttons) {
            if (b.contains(m.x, m.y)) { b.action.run(); return; }
        }
    };

    private void registerInput() {
        app.getInputManager().addMapping("gameOverClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "gameOverClick");
    }

    private Geometry addQuad(float x, float y, float w, float h, ColorRGBA color, float z) {
        Geometry geo = new Geometry("quad_" + System.nanoTime(), new Quad(w, h));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setLocalTranslation(x, y, z);
        guiRoot.attachChild(geo);
        return geo;
    }

    private BitmapText makeText(String content, float size, ColorRGBA color) {
        BitmapText t = new BitmapText(font);
        t.setSize(size);
        t.setText(content);
        t.setColor(color);
        return t;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        app.getGuiNode().detachChild(guiRoot);
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("gameOverClick")) {
            app.getInputManager().deleteMapping("gameOverClick");
        }
        buttons.clear();
    }
}