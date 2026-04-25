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

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal affiché au lancement du jeu.
 * Permet de choisir entre le mode 1v1, le mode Solo, ou quitter.
 */
public class MainMenuState extends AbstractAppState {

    private SimpleApplication app;
    private Node menuRoot;
    private BitmapFont font;

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

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();
        font = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        buildMenu();
        registerInput();

        this.app.getInputManager().setCursorVisible(true);
    }

    private void buildMenu() {
        menuRoot = new Node("mainMenuRoot");

        // Fond noir opaque
        addQuad(0, 0, screenW, screenH, new ColorRGBA(0.03f, 0.03f, 0.08f, 1f), 0f);

        // Titre
        BitmapText title = makeText("AIR HOCKEY", font.getCharSet().getRenderedSize() * 3f, ColorRGBA.White);
        title.setLocalTranslation(
                screenW / 2f - title.getLineWidth() / 2f,
                screenH / 2f + 220f, 1f
        );
        menuRoot.attachChild(title);

        float btnW = 300f, btnH = 60f;
        float cx = screenW / 2f - btnW / 2f;
        float topY = screenH / 2f + 60f;
        float gap  = 80f;

        addButton("1v1 - Deux joueurs", cx, topY, btnW, btnH,
                new ColorRGBA(0.1f, 0.5f, 0.15f, 0.95f),
                () -> startGame(Main.GameMode.VS));

        addButton("Solo - Contre l'ordi", cx, topY - gap, btnW, btnH,
                new ColorRGBA(0.15f, 0.2f, 0.6f, 0.95f),
                () -> startGame(Main.GameMode.SOLO));

        addButton("Tournoi - 5 niveaux", cx, topY - gap * 2, btnW, btnH,
                new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),
                () -> startGame(Main.GameMode.TOURNAMENT));

        addButton("Quitter", cx, topY - gap * 3, btnW, btnH,
                new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),
                () -> app.stop());

        app.getGuiNode().attachChild(menuRoot);
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
        menuRoot.attachChild(text);
        buttons.add(new ButtonInfo(x, y, w, h, action, bg, color));
    }

    private void startGame(Main.GameMode mode) {
        app.getStateManager().detach(this);
        ((Main) app).startGame(mode);
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
        app.getInputManager().addMapping("mainMenuClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "mainMenuClick");
    }

    private Geometry addQuad(float x, float y, float w, float h, ColorRGBA color, float z) {
        Geometry geo = new Geometry("quad_" + System.nanoTime(), new Quad(w, h));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setLocalTranslation(x, y, z);
        menuRoot.attachChild(geo);
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
        app.getGuiNode().detachChild(menuRoot);
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("mainMenuClick")) {
            app.getInputManager().deleteMapping("mainMenuClick");
        }
        buttons.clear();
    }
}