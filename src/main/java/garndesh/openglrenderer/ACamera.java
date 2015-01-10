package garndesh.openglrenderer;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

public abstract class ACamera {

	protected byte eyes;

	protected Vector3f   position;
	protected Quaternion orientation;
	
	// The projection and view matrices
	protected Matrix4f[] projection;
	protected Matrix4f view;

	// Projection and View matrix buffers
	protected FloatBuffer projBuffer;
	protected FloatBuffer viewBuffer;
	
	protected static final Vector3f AXIS_X = new Vector3f(0, 0, -1);
	protected static final Vector3f AXIS_Y = new Vector3f(0, 1, 0);
	protected static final Vector3f AXIS_Z = new Vector3f(1, 0, 0);
	
	// Local axes (relative to the Camera)
	protected Vector3f up; 
	protected Vector3f forward; 
	protected Vector3f right; 
	
	public enum Direction {
		FORWARD,
		BACKWARD,
		LEFT,
		RIGHT,
		UP,
		DOWN
	}
	
	public ACamera(){

	    // Default position is the origin
	    position    = new Vector3f();
	    orientation = new Quaternion();
	    
		view       = new Matrix4f();
	    // Create the projection and view matrix buffers
	    viewBuffer = BufferUtils.createFloatBuffer(16);
	    // Create the default local axes
	    up      = new Vector3f(AXIS_Y);
	    forward = new Vector3f(AXIS_X);
	    right   = new Vector3f(AXIS_Z);
	}
	
	public void rotateY(float angle){
	    Quaternion yRot = QuaternionUtil.createFromAxisAngle(AXIS_Y, angle, null);
	    Quaternion.mul(yRot, orientation, orientation);
	    //orientation.setY(orientation.getY()+yRot.y);

	    QuaternionUtil.rotate(right, yRot, right);
	    QuaternionUtil.rotate(forward, yRot, forward);

	    right.normalise();
	    forward.normalise();
	}
	
	public void rotateZ(float angle){
	    Quaternion zRot = QuaternionUtil.createFromAxisAngle(right, angle, null);
	    Quaternion.mul(zRot, orientation, orientation);
	    //orientation.setZ(orientation.getZ()+zRot.z);

	    QuaternionUtil.rotate(up, zRot, up);
	    QuaternionUtil.rotate(forward, zRot, forward);

	    up.normalise();
	    forward.normalise();
	}
	
	public void rotateX(float angle){
	    
	}
	
	public abstract void move(Vector3f dir, float amount);
	
	public abstract void move(Direction dir, float amount);
	
	public abstract void update(int eye);

	public void setPosition(Vector3f position) {
		// TODO Auto-generated method stub
		this.position = position;
	}

	public FloatBuffer getViewBuffer() {
		return this.viewBuffer;
	}

	public FloatBuffer getProjectionBuffer() {
		// TODO Auto-generated method stub
		return this.projBuffer;
	}
	
	public Vector3f getPosition(){
		return this.position;
	}
	
	public byte getEyes(){
		return eyes;
	}

	public abstract void preUpdate();

	public abstract void postUpdate();
}
