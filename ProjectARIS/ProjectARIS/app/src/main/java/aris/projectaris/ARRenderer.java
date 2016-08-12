package aris.projectaris;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.vuforia.CameraCalibration;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vec2F;
import com.vuforia.Vuforia;

import com.threed.jpct.*;
import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.*;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;
import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.*;
import java.util.*;

import android.content.res.AssetManager;
import android.view.View;

//The renderer class for the AR activity
public class ARRenderer implements GLSurfaceView.Renderer {
    private static final String LOGTAG = "ARRenderer";
    private VuforiaSession vuforiaAppSession;
    private ARCameraActivity mActivity;

    private Renderer mRenderer;
    boolean mIsActive = false;

    private World world;
    private Light sun;
    private Object3D monster;
    private Camera cam;
    private FrameBuffer fb;
    private float[] modelViewMat;
    private float fov;
    private float fovy;

    private int monsterScale = 1;

    private float ind = 0;
    private boolean doLoop = true;
    private int fps = 0;
    private long time = System.currentTimeMillis();

    private int r = 0;
    private int g = 0;
    private int b = 0;

    //region { Animator - miscellaneous }
    //private Ticker ticker = new Ticker(15);

    /*public ImageTargetRenderer(ImageTargets activity, SampleApplicationSession session) {
        mActivity = activity;
        vuforiaAppSession = session;

        world = new World();
        world.setAmbientLight(20, 20, 20);
        //set the following value according to your need, so the object won't be disappeared.
        world.setClippingPlanes(2.0f, 3000.0f);

        sun = new Light(world);
        sun.setIntensity(250, 250, 250);

        //Create a texture out of the icon...:-)
        if (!TextureManager.getInstance().containsTexture("texture")) {
            Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(
                mActivity.getResources().getDrawable(R.drawable.icon)), 64, 64));
            TextureManager.getInstance().addTexture("texture", texture);
        }

        cylinder = Primitives.getCylinder(80, 80);
        cylinder.calcTextureWrapSpherical();
        cylinder.setTexture("texture");
        cylinder.strip();
        cylinder.build();

        //Transform (scale, rotate, translate) the object: Depends on your need.
        //cylinder.scale(scale);
        cylinder.rotateX(180.0f);
        //cylinder.rotateY(w); cylinder.rotateZ(w);
        //cylinder.translate(x, y, z);

        world.addObject(cylinder);
        cam = world.getCamera();

        SimpleVector sv = new SimpleVector();
        sv.set(cylinder.getTransformedCenter());
        sv.y -= 100;
        sv.z -= 100;
        sun.setPosition(sv);

        //for older Android versions, which had massive problems with garbage collection
        MemoryHelper.compact();
    }*/
    //endregion

    public ARRenderer (ARCameraActivity activity, VuforiaSession session) {
        mActivity = activity;
        vuforiaAppSession = session;

        world = new World();
        world.setAmbientLight(20, 20, 20);
        //set the following value according to your need, so the object won't be disappeared.
        world.setClippingPlanes(2.0f, 3000.0f);

        sun = new Light(world);
        sun.setIntensity(25500, 25500, 25500);

        cam = world.getCamera();
        cam.setPosition(0,0,-20);

        //Create a texture out of the icon...:-)
        /*if ( !TextureManager.getInstance().containsTexture("texture") ) {
            Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(
                    mActivity.getResources().getDrawable(R.drawable.icon)), 64, 64));
            TextureManager.getInstance().addTexture("texture", texture);
        }*/
        /*for (int i = 0; i < textures.length; ++i) {
            TextureManager.getInstance().addTexture(textures[i] + ".jpg", new Texture("res/" + textures[i] + ".jpg"));
        }*/
        //FileSystem fs = FileSystem.getDefault();
        //String slash = fs.getSeparator();
        try {
            //FileInputStream fis = new FileInputStream("/src/main/assets/models/monster.jpg");
            AssetManager as = activity.getApplicationContext().getAssets();
            //TextureManager.getInstance().addTexture("monster.jpg", new Texture(fis));
            if (!TextureManager.getInstance().containsTexture("monster.jpg"))
                TextureManager.getInstance().addTexture("monster.jpg", new Texture(as.open("models/monster.jpg")));
        } catch (FileNotFoundException fnfe) {
            Log.e("file not found: " + "monster", "file not found: " + "monster");
        } catch (IOException ioe) {
            Log.e("IOException", "monster");
            Log.e("IOException", ioe.getMessage());
        }
        monster = loadModel("models/monster.3DS", monsterScale, activity);
        //monster = loadModel("models/monster.md2", monsterScale, activity);
        monster.build();
        world.addObject(monster);

        cam.lookAt(monster.getTransformedCenter());

        SimpleVector sv = new SimpleVector();
        sv.set(monster.getTransformedCenter());
        sv.y += 100;
        sv.z += 100;
        sun.setPosition(sv);

        //sun.setPosition(cam.getPosition()); //Sets sun's pos to the cam's


        //for older Android versions, which had massive problems with garbage collection
        MemoryHelper.compact();
    }

    private Object3D loadModel(String filename, float scale, ARCameraActivity activity) {
        try {
            AssetManager as = activity.getApplicationContext().getAssets();
            //FileInputStream fis = new FileInputStream(filename);
            //Object3D[] model = Loader.load3DS(fis, scale);
            Object3D[] model = Loader.load3DS(as.open(filename), scale  * 0.05f);
            //Object3D model = Loader.loadMD2(as.open(filename), scale * 0.05f);
            Object3D o3d = new Object3D(0);
            Object3D temp = null;
            //for (int i = 0; i < model.length; i++) {
            temp = model[0];//i];
            temp.setCenter(SimpleVector.ORIGIN);
            //temp.rotateX((float)( -.5*Math.PI));
            //temp.rotateMesh();
            //temp.scale(0.05f);
            temp.setRotationMatrix(new Matrix());
            o3d = Object3D.mergeObjects(o3d, temp);
            try {
                o3d.setTexture("monster.jpg");
                //o3d.translate(0,0,0);
                o3d.build();
            } catch (NullPointerException e) {
                if ( !TextureManager.getInstance().containsTexture("texture") ) {
                    Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(
                            mActivity.getResources().getDrawable(R.drawable.icon)), 64, 64));
                    TextureManager.getInstance().addTexture("texture", texture);
                }
                o3d.build();
                //o3d.setTexture("texture");
            }

            //}
            //o3d.setTexture("monster.jpg");
            //o3d.align(cam);
            //temp.setTexture("monster.jpg");
            //temp.align(cam);
            //model = temp;
            //o3d.build();
            return o3d;
            //return model;
        } catch (FileNotFoundException fnfe) {
            Log.e("file not found: " + filename, "file not found: " + filename);
        } catch (IOException ioe) {
            Log.e("IOException", filename);
            Log.e("IOException", ioe.getMessage());
        }
        Object3D cylinder = Primitives.getCylinder(80, 80);
        cylinder.calcTextureWrapSpherical();
        //cylinder.setTexture("texture");
        cylinder.strip();
        cylinder.build();
        return cylinder;
    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                Log.e("File:", fileEntry.getName());
            }
        }
    }

    //Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;
        //region Animator - Miscellaneous
        //Animation - start
        //animate(ticker.getTicks(), world.getObject(0));
        //Animation - end
        //endregion
        //Call our function to render content
        renderFrame();

        updateCamera();
        world.renderScene(fb);
        world.draw(fb);
        fb.display();
    }

    //Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        initRendering(); // NOTE: Cocokin sama cpp - DONE

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
    }

    //Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        if (fb != null) {
            fb.dispose();
        }
        fb = new FrameBuffer(width, height);
        Config.viewportOffsetAffectsRenderTarget = true;

        updateRendering(width, height);

        //Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }

    // Function for initializing the renderer.
    private void initRendering() {
        mRenderer = Renderer.getInstance();

        //Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        //Hide the Loading Dialog
        //CBL: mActivity.loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
    }

    /*
    <ProgressBar
            style="@android:style/Widget.ProgressBar"
            android:id="@+id/loading_indicator"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerHorizontal="true"
            android:layout_centerVertical="true" />
     */
    //jpct-ae
    //NOTE: cocokin sama cpp - DONE
    private void updateRendering(int width, int height) {

        //Update screen dimensions
        vuforiaAppSession.setmScreenWidth(width);
        vuforiaAppSession.setmScreenHeight(height);

        //Reconfigure the video background
        vuforiaAppSession.configureVideoBackground();

        CameraCalibration camCalibration = com.vuforia.CameraDevice.getInstance().getCameraCalibration();
        Vec2F size = camCalibration.getSize();
        Vec2F focalLength = camCalibration.getFocalLength();
        float fovyRadians = (float) (2 * Math.atan(0.5f * size.getData()[1] / focalLength.getData()[1]));
        float fovRadians = (float) (2 * Math.atan(0.5f * size.getData()[0] / focalLength.getData()[0]));

        if (vuforiaAppSession.mIsPortrait) {
            setFovy(fovRadians);
            setFov(fovyRadians);
        } else {
            setFov(fovRadians);
            setFovy(fovyRadians);
        }

    }

    //The render function.
    private void renderFrame() {
        // clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // get the state, and mark the beginning of a rendering section
        State state = mRenderer.begin();
        // explicitly render the video background
        mRenderer.drawVideoBackground();

        float[] modelviewArray = new float[16];
        // did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            // get the trackable
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            printUserData(trackable);

            Matrix44F modelViewMatrix = Tool.convertPose2GLMatrix(result.getPose());
            Matrix44F inverseMV = VuforiaMath.Matrix44FInverse(modelViewMatrix);
            Matrix44F invTranspMV = VuforiaMath.Matrix44FTranspose(inverseMV);

            modelviewArray = invTranspMV.getData();
            updateModelviewMatrix(modelviewArray);

        }
        //hide the objects when the targets are not detected
        if (state.getNumTrackableResults() == 0) {
            float m [] = {
                    1,0,0,0,
                    0,1,0,0,
                    0,0,1,0,
                    0,0,-10000,1
            };
            modelviewArray = m;
            updateModelviewMatrix(modelviewArray);
        }

        mRenderer.end();
    }

    private void printUserData(Trackable trackable) {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }

    private void updateModelviewMatrix(float mat[]) {
        modelViewMat = mat;
    }

    private void updateCamera() {
        if (modelViewMat != null) {
            float[] m = modelViewMat;

            final SimpleVector camUp;
            if (vuforiaAppSession.mIsPortrait) {
                camUp = new SimpleVector(-m[0], -m[1], -m[2]);
            } else {
                camUp = new SimpleVector(-m[4], -m[5], -m[6]);
            }

            final SimpleVector camDirection = new SimpleVector(m[8], m[9], m[10]);
            final SimpleVector camPosition = new SimpleVector(m[12], m[13], m[14]);

            cam.setOrientation(camDirection, camUp);
            cam.setPosition(camPosition);

            cam.setFOV(fov);
            cam.setYFOV(fovy);

            //Update SUN - start
            sun.setPosition(cam.getPosition());
            sun.rotate(cam.getDirection(), new SimpleVector(0,0,0));
            //updateRGB();
            //sun.setIntensity(r, g, b);
            //Update SUN - end

        }
    }

    private void animate(long ticks, Object3D obj) {
        if (ticks > 0) {
            float ft = (float) ticks;
            ind += 0.02f * ft;
            if (ind > 1) {
                ind -= 1;
            }
            try {
                obj.animate(ind, 2);
            } catch(RuntimeException rte) {
                Log.e("RuntimeException", rte.getMessage());
            }
            //obj.rotateY(-0.02f * ft);
            //obj.translate(0, -50, 0);
            //SimpleVector dir = obj.getXAxis();
            //dir.scalarMul(ft);
            //dir = obj.checkForCollisionEllipsoid(dir, new SimpleVector(5, 20,	5), 5);
            //obj.translate(dir);
            //dir = obj.checkForCollisionEllipsoid(new SimpleVector(0, 100, 0),	new SimpleVector(5, 20, 5), 1);
            //obj.translate(dir);
        }
    }

    private void setFov(float fov) {
        this.fov = fov;
    }

    private void setFovy(float fovy) {
        this.fovy = fovy;
    }

    private void updateRGB() {
        b += 2;
        if (b == 256) {
            b = 0;
            g += 2;
        }
        if (g == 256) {
            g = 0;
            r += 2;
        }
        if (r == 256) r = 0;
        Log.d("RGB: ", r + " " + g + " " + b);
    }
}
