package garndesh.openglrenderer;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;


/**
 * @author christiaan
 *
 */
public class Camera extends ACamera{	
	
	public Camera(float fov, float aspect, float zNear, float zFar)
	{
		super();
		eyes = 1;
	    
	 // Create projection and view matrices
	    projection = new Matrix4f[1];

	    projBuffer = BufferUtils.createFloatBuffer(16);


	    // Create projection matrice
	    projection[0] = MatrixUtil.createPerspective(fov, aspect, zNear, zFar);

	    // Store the projection matrix in buffer
	    projection[0].store(projBuffer);
	    projBuffer.rewind();
	}
	
	public void move(Vector3f dir, float amount){
	    // Create a copy of direction
	    Vector3f deltaMove = new Vector3f(dir);

	    // Normalise the direction and scale it by amount
	    deltaMove.normalise();
	    deltaMove.scale(amount);

	    // Add the delta to the camera's position
	    Vector3f.add(position, deltaMove, position);
	}
	
	// raw move function will change given vector
	private void moveRaw(Vector3f dir, float amount){
		dir.normalise();
		dir.scale(amount);
		Vector3f.add(position, dir, position);
	}
	
	public void move(Direction dir, float amount)
	{
	    switch (dir)
	    {
	        case FORWARD:  moveRaw(new Vector3f(forward.x, 0, forward.z), +amount); break;
	        case BACKWARD: moveRaw(new Vector3f(forward.x, 0, forward.z), -amount); break;
	        case LEFT:     moveRaw(new Vector3f(right.x, 0, right.z),   -amount); break;
	        case RIGHT:    moveRaw(new Vector3f(right.x, 0, right.z),   +amount); break;
	        case UP:       move(AXIS_Y,      +amount); break;
	        case DOWN:     move(AXIS_Y,      -amount); break;
	    }
	}
	
	public void update(int eye){
	    // Rotate the scene and translate the world back
	    QuaternionUtil.toRotationMatrix(orientation, view);
	    Matrix4f.translate(position.negate(null), view, view);

	    // Store the view matrix in the buffer
	    view.store(viewBuffer);
	    viewBuffer.rewind();
	}

	@Override
	public void preUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postUpdate() {
		// TODO Auto-generated method stub
		
	}
}
