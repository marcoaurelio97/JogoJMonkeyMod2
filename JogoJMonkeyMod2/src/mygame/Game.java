package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Random;

public class Game extends SimpleApplication implements ActionListener, PhysicsCollisionListener {

    private BulletAppState bulletAppState;
    private VehicleControl player1, player2;
    private float wheelRadius1, wheelRadius2;
    private float steeringValue1 = 0, steeringValue2 = 0;
    private float accelerationValue1 = 0, accelerationValue2 = 0;
    private Node carNode1, carNode2, auxCam;
    private Random r = new Random();
    private long totalTime, currentTime;
    private int points1 = 0, points2 = 0;
    private int wins1 = 0, wins2 = 0;

    public static void main(String[] args) {
        Game app = new Game();
        app.showSettings = false;
        app.start();
    }

    private void setupKeys() {
        inputManager.addMapping("Lefts1", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Rights1", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Ups1", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Downs1", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, "Lefts1");
        inputManager.addListener(this, "Rights1");
        inputManager.addListener(this, "Ups1");
        inputManager.addListener(this, "Downs1");

        inputManager.addMapping("Lefts2", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rights2", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Ups2", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Downs2", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(this, "Lefts2");
        inputManager.addListener(this, "Rights2");
        inputManager.addListener(this, "Ups2");
        inputManager.addListener(this, "Downs2");

        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        flyCam.setEnabled(false);
        setDisplayFps(false);
        setDisplayStatView(false);

        auxCam = new Node();
        rootNode.attachChild(auxCam);

        totalTime = System.currentTimeMillis();

        cam.setLocation(new Vector3f(-80, 50, 0));
        cam.setRotation(new Quaternion(-1, 0, 0, -90f));

        setupKeys();
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
//        buildPlayer1();
//        buildPlayer2();
        player1 = buildPlayer(carNode1, wheelRadius1, 20, "Car1");
        player2 = buildPlayer(carNode2, wheelRadius2, -20, "Car2");

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0.5f, -0.1f, 0.3f).normalizeLocal());

        bulletAppState.getPhysicsSpace().addCollisionListener(this);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    private Geometry findGeom(Spatial spatial, String name) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (int i = 0; i < node.getQuantity(); i++) {
                Spatial child = node.getChild(i);
                Geometry result = findGeom(child, name);
                if (result != null) {
                    return result;
                }
            }
        } else if (spatial instanceof Geometry) {
            if (spatial.getName().startsWith(name)) {
                return (Geometry) spatial;
            }
        }
        return null;
    }

    private VehicleControl buildPlayer(Node carNode, float wheelRadius, int XYZ, String name) {
        float stiffness = 120.0f;
        float compValue = 0.2f;
        float dampValue = 0.3f;
        final float mass = 400;

        carNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode.setShadowMode(ShadowMode.Cast);
        Geometry chasis = findGeom(carNode, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();

        carNode.setLocalTranslation(XYZ, 0, XYZ);

        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);

        VehicleControl player = new VehicleControl(carHull, mass);
        carNode.addControl(player);

        player.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        player.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        player.setSuspensionStiffness(stiffness);
        player.setMaxSuspensionForce(10000);

        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        Geometry wheel_fr = findGeom(carNode, "WheelFrontRight");
        wheel_fr.center();
        box = (BoundingBox) wheel_fr.getModelBound();
        wheelRadius = box.getYExtent();
        float back_wheel_h = (wheelRadius * 1.7f) - 1f;
        float front_wheel_h = (wheelRadius * 1.9f) - 1f;
        player.addWheel(wheel_fr.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        Geometry wheel_fl = findGeom(carNode, "WheelFrontLeft");
        wheel_fl.center();
        box = (BoundingBox) wheel_fl.getModelBound();
        player.addWheel(wheel_fl.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        Geometry wheel_br = findGeom(carNode, "WheelBackRight");
        wheel_br.center();
        box = (BoundingBox) wheel_br.getModelBound();
        player.addWheel(wheel_br.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        Geometry wheel_bl = findGeom(carNode, "WheelBackLeft");
        wheel_bl.center();
        box = (BoundingBox) wheel_bl.getModelBound();
        player.addWheel(wheel_bl.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        player.getWheel(2).setFrictionSlip(4);
        player.getWheel(3).setFrictionSlip(4);

        carNode.setName(name);

        rootNode.attachChild(carNode);
        getPhysicsSpace().add(player);

        if ("Car1".equals(name)) {
            carNode1 = carNode;
        } else {
            carNode2 = carNode;
        }

        return player;
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts1")) {
            if (value) {
                steeringValue1 += .5f;
            } else {
                steeringValue1 += -.5f;
            }
            player1.steer(steeringValue1);
        } else if (binding.equals("Rights1")) {
            if (value) {
                steeringValue1 += -.5f;
            } else {
                steeringValue1 += .5f;
            }
            player1.steer(steeringValue1);
        } else if (binding.equals("Ups1")) {
            if (value) {
                accelerationValue1 -= 800;
            } else {
                accelerationValue1 += 800;
            }
            player1.accelerate(accelerationValue1);
            player1.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(findGeom(carNode1, "Car")));
        } else if (binding.equals("Downs1")) {
            if (value) {
                player1.brake(40f);
            } else {
                player1.brake(0f);
            }
        }

        if (binding.equals("Lefts2")) {
            if (value) {
                steeringValue2 += .5f;
            } else {
                steeringValue2 += -.5f;
            }
            player2.steer(steeringValue2);
        } else if (binding.equals("Rights2")) {
            if (value) {
                steeringValue2 += -.5f;
            } else {
                steeringValue2 += .5f;
            }
            player2.steer(steeringValue2);
        } else if (binding.equals("Ups2")) {
            if (value) {
                accelerationValue2 -= 800;
            } else {
                accelerationValue2 += 800;
            }
            player2.accelerate(accelerationValue2);
            player2.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(findGeom(carNode2, "Car")));
        } else if (binding.equals("Downs2")) {
            if (value) {
                player2.brake(40f);
            } else {
                player2.brake(0f);
            }
        } else if (binding.equals("Reset")) {
            if (value) {
                resetGame();
            } else {
            }
        }
    }

    public void resetGame() {
        player1.setPhysicsLocation(new Vector3f(20, 0, 20));
        player1.setPhysicsRotation(new Matrix3f());
        player1.setLinearVelocity(Vector3f.ZERO);
        player1.setAngularVelocity(Vector3f.ZERO);
        player1.resetSuspension();

        player2.setPhysicsLocation(new Vector3f(-20, 0, -20));
        player2.setPhysicsRotation(new Matrix3f());
        player2.setLinearVelocity(Vector3f.ZERO);
        player2.setAngularVelocity(Vector3f.ZERO);
        player2.resetSuspension();

        for (Spatial spatial : rootNode.getChildren()) {
            if ("Item".equals(spatial.getName())) {
                rootNode.detachChild(spatial);
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(auxCam.getWorldTranslation(), Vector3f.UNIT_Y);

        if (rootNode.getChild("Item") != null) {
            for (Spatial spatial : rootNode.getChildren()) {
                if ("Item".equals(spatial.getName())) {
                    spatial.rotate(0, tpf, 0);
                }
            }
        }

        currentTime = System.currentTimeMillis();

        if (currentTime - totalTime >= 5000) {
            int x = r.nextInt(40);
            int y = r.nextInt(40);
            int z = r.nextInt(40);
            createItem(x - 20, -5, z - 20);
            totalTime = currentTime;
        }

        atualizaPontos();
        verificaPlayerFora();
    }

    private void atualizaPontos() {
        rootNode.detachChildNamed("TextPoints");
        rootNode.detachChildNamed("TextWins");

        BitmapFont labelFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText label = new BitmapText(labelFont);
        label.setSize(2);
        label.setText("Player 1: " + points1 + "   Wins: " + wins1 + "\nPlayer 2: " + points2 + "   Wins: " + wins2);
        label.setLocalTranslation(-50, 43, -7.7f);
        label.rotate(0, 30, -0.05f);
        label.setQueueBucket(RenderQueue.Bucket.Transparent);
        label.setName("TextPoints");
        rootNode.attachChild(label);
    }

    private void verificaPlayerFora() {
        float y1 = rootNode.getChild("Car1").getLocalTranslation().y;
        float y2 = rootNode.getChild("Car2").getLocalTranslation().y;

        if (y1 <= -20) {
            wins2++;
            resetGame();
        }

        if (y2 <= -20) {
            wins1++;
            resetGame();
        }
    }

    private void createItem(float x, float y, float z) {
        Node duck = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        duck.setLocalTranslation(x, y, z);
        duck.setLocalScale(5);
        duck.rotate(0.0f, 0.1f, 0.0f);
        duck.setName("Item");
        rootNode.attachChild(duck);

        RigidBodyControl boxPhysicsNode = new RigidBodyControl(0);
        duck.addControl(boxPhysicsNode);
        bulletAppState.getPhysicsSpace().add(boxPhysicsNode);
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        if (event.getNodeA().getName().equals("Car1")
                || event.getNodeB().getName().equals("Car1")) {

            if (event.getNodeA().getName().equals("Item")) {
                Spatial s = event.getNodeA();
                rootNode.detachChild(s);
                bulletAppState.getPhysicsSpace().removeAll(s);
                points1++;
            } else if (event.getNodeB().getName().equals("Item")) {
                Spatial s = event.getNodeB();
                rootNode.detachChild(s);
                bulletAppState.getPhysicsSpace().removeAll(s);
                points1++;
            }
        }

        if (event.getNodeA().getName().equals("Car2")
                || event.getNodeB().getName().equals("Car2")) {

            if (event.getNodeA().getName().equals("Item")) {
                Spatial s = event.getNodeA();
                rootNode.detachChild(s);
                bulletAppState.getPhysicsSpace().removeAll(s);
                points2++;
            } else if (event.getNodeB().getName().equals("Item")) {
                Spatial s = event.getNodeB();
                rootNode.detachChild(s);
                bulletAppState.getPhysicsSpace().removeAll(s);
                points2++;
            }
        }
    }
}
