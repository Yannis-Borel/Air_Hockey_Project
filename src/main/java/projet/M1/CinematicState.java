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
 * dessus (caméra 1) vers la vue latérale (caméra 2).
 *
 * Durée : 2 secondes, interpolation smooth-step.
 * À la fin, attache CountdownState et se retire.
 */
public class CinematicState extends AbstractAppState {

    private static final float DURATION = 2.0f;

    private static final Vector3f CAM1_POS = new Vector3f(0f, 30f, 0f);
    private static final Vector3f CAM2_POS = new Vector3f(0f,  7f, 19f);

    private final Main       mainApp;
    private Camera           cam;
    private AppStateManager  sm;

    private Quaternion startRot = new Quaternion();
    private Quaternion endRot   = new Quaternion();
    private float      timer    = 0f;

    public CinematicState(Main mainApp) {
        this.mainApp = mainApp;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.sm = stateManager;
        cam = ((SimpleApplication) app).getCamera();

        // Capturer la rotation de départ (vue dessus)
        cam.setLocation(CAM1_POS);
        cam.lookAt(Vector3f.ZERO, new Vector3f(0f, 0f, -1f));
        startRot.set(cam.getRotation());

        // Capturer la rotation de fin (vue latérale)
        cam.setLocation(CAM2_POS);
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        endRot.set(cam.getRotation());

        // Remettre au départ pour que la transition parte de cam1
        cam.setLocation(CAM1_POS);
        cam.setRotation(startRot);
    }

    @Override
    public void update(float tpf) {
        timer += tpf;
        float t = Math.min(timer / DURATION, 1f);

        // Smooth-step : accélération douce au départ, décélération douce à l'arrivée
        float s = t * t * (3f - 2f * t);

        // Translation de position
        Vector3f pos = new Vector3f();
        pos.interpolateLocal(CAM1_POS, CAM2_POS, s);
        cam.setLocation(pos);

        // Rotation interpolée (slerp)
        Quaternion rot = new Quaternion();
        rot.slerp(startRot, endRot, s);
        cam.setRotation(rot);

        if (timer >= DURATION) {
            // Forcer la position exacte finale
            cam.setLocation(CAM2_POS);
            cam.setRotation(endRot);

            sm.attach(new CountdownState(mainApp));
            sm.detach(this);
        }
    }
}
