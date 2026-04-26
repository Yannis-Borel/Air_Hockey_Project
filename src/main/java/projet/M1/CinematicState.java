package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Cinématique de lancement : la caméra glisse en translation douce de la vue
 * dessus (caméra 1) vers la vue finale selon le mode de jeu :
 *   - MULTIPLAYER -> vue du dessus (reste sur CAM1_POS)
 *   - SOLO_AI / TOURNAMENT -> vue latérale (CAM2_POS)
 *
 * Durée : 2 secondes, interpolation smooth-step.
 * À la fin, attache CountdownState et se retire.
 */
public class CinematicState extends AbstractAppState {

    private static final float DURATION = 2.0f;

    private static final Vector3f CAM1_POS = new Vector3f(0f, 30f, 0f);
    private static final Vector3f CAM2_POS = new Vector3f(0f,  7f, 19f);

    private final Main mainApp;
    private final Main.GameMode mode;
    private Camera cam;
    private AppStateManager sm;

    private Quaternion startRot = new Quaternion();
    private Quaternion endRot = new Quaternion();
    private float timer = 0f;

    /**
     * Crée la cinématique pour le mode de jeu donné.
     * Le mode détermine la position finale de la caméra.
     */
    public CinematicState(Main mainApp, Main.GameMode mode) {
        this.mainApp = mainApp;
        this.mode = mode;
    }

    /**
     * Calcule les rotations de départ et de fin selon le mode,
     * puis place la caméra à la position initiale (vue dessus).
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.sm = stateManager;
        cam = ((SimpleApplication) app).getCamera();

        // Position finale selon le mode
        Vector3f endPos = (mode == Main.GameMode.MULTIPLAYER) ? CAM1_POS : CAM2_POS;
        Vector3f endUp = (mode == Main.GameMode.MULTIPLAYER)
                ? new Vector3f(0f, 0f, -1f) : Vector3f.UNIT_Y;

        // Capturer la rotation de départ (vue dessus)
        cam.setLocation(CAM1_POS);
        cam.lookAt(Vector3f.ZERO, new Vector3f(0f, 0f, -1f));
        startRot.set(cam.getRotation());

        // Capturer la rotation de fin
        cam.setLocation(endPos);
        cam.lookAt(Vector3f.ZERO, endUp);
        endRot.set(cam.getRotation());

        // Remettre au départ pour que la transition parte de cam1
        cam.setLocation(CAM1_POS);
        cam.setRotation(startRot);
    }

    /**
     * Interpole la position et la rotation de la caméra avec smooth-step.
     * Quand la durée est écoulée, force la position finale,
     * attache CountdownState et se retire.
     */
    @Override
    public void update(float tpf) {
        timer += tpf;
        float t = Math.min(timer / DURATION, 1f);

        // Smooth-step : accélération douce au départ, décélération douce à l'arrivée
        float s = t * t * (3f - 2f * t);

        Vector3f endPos = (mode == Main.GameMode.MULTIPLAYER) ? CAM1_POS : CAM2_POS;

        // Translation de position
        Vector3f pos = new Vector3f();
        pos.interpolateLocal(CAM1_POS, endPos, s);
        cam.setLocation(pos);

        // Rotation interpolée (slerp)
        Quaternion rot = new Quaternion();
        rot.slerp(startRot, endRot, s);
        cam.setRotation(rot);

        if (timer >= DURATION) {
            // Forcer la position exacte finale
            cam.setLocation(endPos);
            cam.setRotation(endRot);

            sm.attach(new CountdownState(mainApp));
            sm.detach(this);
        }
    }
}