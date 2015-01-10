package garndesh.openglrenderer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import com.oculusvr.capi.OvrQuaternionf;

public class QuaternionUtil {
	
	public static Quaternion createFromOvrQ(OvrQuaternionf q, Quaternion dest){
		if (dest == null)
			dest = new Quaternion();
		dest.x = q.x;
		dest.y = q.y;
		dest.z = q.z;
		dest.w = q.w;
		return dest;
	}
	
	public static Quaternion createFromAxisAngle(Vector3f axis, float angle,
			Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();

		// Normalize the axis
		axis.normalise();

		// Calculate the halfAngle
		float halfAngle = (float) Math.toRadians(angle / 2);

		// Create the quaternion from axis-angle
		dest.x = axis.x * (float) Math.sin(halfAngle);
		dest.y = axis.y * (float) Math.sin(halfAngle);
		dest.z = axis.z * (float) Math.sin(halfAngle);

		dest.w = (float) Math.cos(halfAngle);

		// Normalize the quaternion
		dest.normalise();

		return dest;
	}

	public static Quaternion conjugate(Quaternion src, Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();

		// Only negate the axis
		dest.x = -src.x;
		dest.y = -src.y;
		dest.z = -src.z;
		dest.w = +src.w;

		return dest;
	}

	public static Vector3f rotate(Vector3f v, Quaternion q, Vector3f dest) {
		if (dest == null)
			dest = new Vector3f();

		// Calculate the conjugate of quaternion
		Quaternion q1 = conjugate(q, null);
		Quaternion qv = new Quaternion(v.x, v.y, v.z, 1);

		// Rotate the quaternion
		Quaternion.mul(q, qv, qv);
		Quaternion.mul(qv, q1, qv);

		// Extract vector from rotated quaternion
		dest.x = qv.x;
		dest.y = qv.y;
		dest.z = qv.z;

		return dest;
	}
	
	public static Matrix4f toRotationMatrix(Quaternion q, Matrix4f dest){
	    if (dest == null)
	        dest = new Matrix4f();
	    else
	        dest.setIdentity();

	    // Normalize the quaternion
	    q.normalise();

	    // The length of the quaternion
	    float s = 2f / q.length();

	    // Convert the quaternion to matrix
	    dest.m00 = 1 - s * (q.y * q.y + q.z * q.z);
	    dest.m10 = s * (q.x * q.y + q.w * q.z);
	    dest.m20 = s * (q.x * q.z - q.w * q.y);

	    dest.m01 = s * (q.x * q.y - q.w * q.z);
	    dest.m11 = 1 - s * (q.x * q.x + q.z * q.z);
	    dest.m21 = s * (q.y * q.z + q.w * q.x);

	    dest.m02 = s * (q.x * q.z + q.w * q.y);
	    dest.m12 = s * (q.y * q.z - q.w * q.x);
	    dest.m22 = 1 - s * (q.x * q.x + q.y * q.y);

	    return dest;
	}
}
