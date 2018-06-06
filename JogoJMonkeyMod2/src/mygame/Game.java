package mygame;

import com.jme3.app.SettingsDialog;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture.WrapMode;

public class Game extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private VehicleControl player1, player2;
//    private VehicleWheel fr, fl, br, bl;
//    private Node node_fr, node_fl, node_br, node_bl;
    private float wheelRadius1, wheelRadius2;
    private float steeringValue1 = 0, steeringValue2 = 0;
    private float accelerationValue1 = 0, accelerationValue2 = 0;
    private Node carNode1, carNode2;

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
//        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
//        cam.setFrustumFar(150f);
//        flyCam.setMoveSpeed(10);
        flyCam.setEnabled(false);

        cam.setLocation(new Vector3f(0, 200f, 0));
        cam.setRotation(new Quaternion(-1, 0, 0, -90f));

        setupKeys();
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
//        setupFloor();
        buildPlayer1();
        buildPlayer2();

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0.5f, -0.1f, 0.3f).normalizeLocal());
        //   rootNode.addLight(dl);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

//    public void setupFloor() {
//        Material mat = assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m");
//        mat.getTextureParam("DiffuseMap").getTextureValue().setWrap(WrapMode.Repeat);
////        mat.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
////        mat.getTextureParam("ParallaxMap").getTextureValue().setWrap(WrapMode.Repeat);
//
//        Box floor = new Box(Vector3f.ZERO, 140, 1f, 140);
//        floor.scaleTextureCoordinates(new Vector2f(112.0f, 112.0f));
//        Geometry floorGeom = new Geometry("Floor", floor);
//        floorGeom.setShadowMode(ShadowMode.Receive);
//        floorGeom.setMaterial(mat);
//
//        PhysicsNode tb = new PhysicsNode(floorGeom, new MeshCollisionShape(floorGeom.getMesh()), 0);
//        tb.setLocalTranslation(new Vector3f(0f, -6, 0f));
////        tb.attachDebugShape(assetManager);
//        rootNode.attachChild(tb);
//        getPhysicsSpace().add(tb);
//    }
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

    private void buildPlayer1() {
        float stiffness = 120.0f;//200=f1 car
        float compValue = 0.2f; //(lower than damp!)
        float dampValue = 0.3f;
        final float mass = 400;

        //Load model and get chassis Geometry
        carNode1 = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode1.setShadowMode(ShadowMode.Cast);
        Geometry chasis = findGeom(carNode1, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();

        //Create a hull collision shape for the chassis
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);

        //Create a vehicle control
        player1 = new VehicleControl(carHull, mass);
        carNode1.addControl(player1);

        //Setting default values for wheels
        player1.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        player1.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        player1.setSuspensionStiffness(stiffness);
        player1.setMaxSuspensionForce(10000);

        //Create four wheels and add them at their locations
        //note that our fancy car actually goes backwards..
        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        Geometry wheel_fr = findGeom(carNode1, "WheelFrontRight");
        wheel_fr.center();
        box = (BoundingBox) wheel_fr.getModelBound();
        wheelRadius1 = box.getYExtent();
        float back_wheel_h = (wheelRadius1 * 1.7f) - 1f;
        float front_wheel_h = (wheelRadius1 * 1.9f) - 1f;
        player1.addWheel(wheel_fr.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius1, true);

        Geometry wheel_fl = findGeom(carNode1, "WheelFrontLeft");
        wheel_fl.center();
        box = (BoundingBox) wheel_fl.getModelBound();
        player1.addWheel(wheel_fl.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius1, true);

        Geometry wheel_br = findGeom(carNode1, "WheelBackRight");
        wheel_br.center();
        box = (BoundingBox) wheel_br.getModelBound();
        player1.addWheel(wheel_br.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius1, false);

        Geometry wheel_bl = findGeom(carNode1, "WheelBackLeft");
        wheel_bl.center();
        box = (BoundingBox) wheel_bl.getModelBound();
        player1.addWheel(wheel_bl.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius1, false);

        player1.getWheel(2).setFrictionSlip(4);
        player1.getWheel(3).setFrictionSlip(4);

        rootNode.attachChild(carNode1);
        getPhysicsSpace().add(player1);
    }

    private void buildPlayer2() {
        float stiffness = 120.0f;//200=f1 car
        float compValue = 0.2f; //(lower than damp!)
        float dampValue = 0.3f;
        final float mass = 400;

        //Load model and get chassis Geometry
        carNode2 = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode2.setShadowMode(ShadowMode.Cast);
        Geometry chasis = findGeom(carNode2, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();

        //Create a hull collision shape for the chassis
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);

        //Create a vehicle control
        player2 = new VehicleControl(carHull, mass);
        carNode2.addControl(player2);

        //Setting default values for wheels
        player2.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        player2.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        player2.setSuspensionStiffness(stiffness);
        player2.setMaxSuspensionForce(10000);

        //Create four wheels and add them at their locations
        //note that our fancy car actually goes backwards..
        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        Geometry wheel_fr = findGeom(carNode2, "WheelFrontRight");
        wheel_fr.center();
        box = (BoundingBox) wheel_fr.getModelBound();
        wheelRadius2 = box.getYExtent();
        float back_wheel_h = (wheelRadius2 * 1.7f) - 1f;
        float front_wheel_h = (wheelRadius2 * 1.9f) - 1f;
        player2.addWheel(wheel_fr.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius2, true);

        Geometry wheel_fl = findGeom(carNode2, "WheelFrontLeft");
        wheel_fl.center();
        box = (BoundingBox) wheel_fl.getModelBound();
        player2.addWheel(wheel_fl.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius2, true);

        Geometry wheel_br = findGeom(carNode2, "WheelBackRight");
        wheel_br.center();
        box = (BoundingBox) wheel_br.getModelBound();
        player2.addWheel(wheel_br.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius2, false);

        Geometry wheel_bl = findGeom(carNode2, "WheelBackLeft");
        wheel_bl.center();
        box = (BoundingBox) wheel_bl.getModelBound();
        player2.addWheel(wheel_bl.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius2, false);

        player2.getWheel(2).setFrictionSlip(4);
        player2.getWheel(3).setFrictionSlip(4);

        rootNode.attachChild(carNode2);
        getPhysicsSpace().add(player2);
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
        } //note that our fancy car actually goes backwards..
        else if (binding.equals("Ups1")) {
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
        } else if (binding.equals("Reset")) {
            if (value) {
                System.out.println("Reset");
                player1.setPhysicsLocation(Vector3f.ZERO);
                player1.setPhysicsRotation(new Matrix3f());
                player1.setLinearVelocity(Vector3f.ZERO);
                player1.setAngularVelocity(Vector3f.ZERO);
                player1.resetSuspension();
            } else {
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
        } //note that our fancy car actually goes backwards..
        else if (binding.equals("Ups2")) {
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
                System.out.println("Reset");
                player2.setPhysicsLocation(Vector3f.ZERO);
                player2.setPhysicsRotation(new Matrix3f());
                player2.setLinearVelocity(Vector3f.ZERO);
                player2.setAngularVelocity(Vector3f.ZERO);
                player2.resetSuspension();
            } else {
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(carNode1.getWorldTranslation(), Vector3f.UNIT_Y);
    }
}
