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
import projet.M1.game.OpponentProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Fenêtre affichée entre chaque round du tournoi.
 *
 * Cas victoire (round < 5) : "Adversaire vaincu !" + "Prochain adversaire" + "Menu principal"
 * Cas victoire finale (round == 5) : "Vous avez gagné le tournoi !" + "Menu principal"
 * Cas défaite : "Défaite" + "Menu principal"
 */
public class TournamentResultState extends AbstractAppState {

    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;

    private final boolean won;      // true = victoire, false = défaite
    private final int     round;    // round qui vient de se terminer (1 à 5)

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

    public TournamentResultState(boolean won, int round) {
        this.won   = won;
        this.round = round;
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
        guiRoot = new Node("tournamentResultRoot");

        // Fond semi-transparent
        addQuad(0, 0, screenW, screenH, new ColorRGBA(0f, 0f, 0f, 0.78f), 0f);

        float btnW = 300f, btnH = 55f;
        float cx   = screenW / 2f - btnW / 2f;
        float gap  = 75f;

        if (!won) {
            // --- Défaite ---
            BitmapText title = makeText("DÉFAITE", font.getCharSet().getRenderedSize() * 3f,
                    new ColorRGBA(0.9f, 0.15f, 0.15f, 1f));
            title.setLocalTranslation(screenW / 2f - title.getLineWidth() / 2f,
                    screenH / 2f + 200f, 1f);
            guiRoot.attachChild(title);

            String oppName = OpponentProfile.forRound(round).name;
            BitmapText msg = makeText("Éliminé par " + oppName,
                    font.getCharSet().getRenderedSize() * 1.5f, ColorRGBA.White);
            msg.setLocalTranslation(screenW / 2f - msg.getLineWidth() / 2f,
                    screenH / 2f + 120f, 1f);
            guiRoot.attachChild(msg);

            addButton("Menu principal", cx, screenH / 2f + 20f, btnW, btnH,
                    new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),
                    () -> { app.getStateManager().detach(this); ((Main) app).returnToMainMenu(); });

        } else if (round == OpponentProfile.totalRounds()) {
            // --- Victoire finale du tournoi ---
            BitmapText title = makeText("TOURNOI REMPORTÉ !", font.getCharSet().getRenderedSize() * 2.5f,
                    new ColorRGBA(1f, 0.85f, 0f, 1f));
            title.setLocalTranslation(screenW / 2f - title.getLineWidth() / 2f,
                    screenH / 2f + 200f, 1f);
            guiRoot.attachChild(title);

            BitmapText msg = makeText("Vous avez gagné le tournoi !",
                    font.getCharSet().getRenderedSize() * 1.5f, ColorRGBA.White);
            msg.setLocalTranslation(screenW / 2f - msg.getLineWidth() / 2f,
                    screenH / 2f + 120f, 1f);
            guiRoot.attachChild(msg);

            addButton("Menu principal", cx, screenH / 2f + 20f, btnW, btnH,
                    new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),
                    () -> { app.getStateManager().detach(this); ((Main) app).returnToMainMenu(); });

        } else {
            // --- Victoire intermédiaire ---
            String oppName = OpponentProfile.forRound(round).name;
            BitmapText title = makeText("Adversaire vaincu !", font.getCharSet().getRenderedSize() * 2.5f,
                    new ColorRGBA(0.1f, 0.9f, 0.2f, 1f));
            title.setLocalTranslation(screenW / 2f - title.getLineWidth() / 2f,
                    screenH / 2f + 200f, 1f);
            guiRoot.attachChild(title);

            BitmapText msg = makeText(oppName + " est éliminé !",
                    font.getCharSet().getRenderedSize() * 1.5f, ColorRGBA.White);
            msg.setLocalTranslation(screenW / 2f - msg.getLineWidth() / 2f,
                    screenH / 2f + 120f, 1f);
            guiRoot.attachChild(msg);

            // Prochain adversaire
            String nextName = OpponentProfile.forRound(round + 1).name;
            BitmapText next = makeText("Prochain : " + nextName,
                    font.getCharSet().getRenderedSize() * 1.2f,
                    new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
            next.setLocalTranslation(screenW / 2f - next.getLineWidth() / 2f,
                    screenH / 2f + 60f, 1f);
            guiRoot.attachChild(next);

            addButton("Prochain adversaire", cx, screenH / 2f + 20f - gap * 0.5f, btnW, btnH,
                    new ColorRGBA(0.1f, 0.5f, 0.1f, 0.95f),
                    () -> {
                        app.getStateManager().detach(this);
                        ((Main) app).startTournamentRound(round + 1);
                    });

            addButton("Menu principal", cx, screenH / 2f + 20f - gap * 1.5f, btnW, btnH,
                    new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),
                    () -> { app.getStateManager().detach(this); ((Main) app).returnToMainMenu(); });
        }

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
        app.getInputManager().addMapping("tournamentResultClick",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "tournamentResultClick");
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
        if (app.getInputManager().hasMapping("tournamentResultClick")) {
            app.getInputManager().deleteMapping("tournamentResultClick");
        }
        buttons.clear();
    }
}