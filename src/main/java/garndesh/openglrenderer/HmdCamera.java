package garndesh.openglrenderer;

import static com.oculusvr.capi.OvrLibrary.ovrHmdType.ovrHmd_DK1;
import static com.oculusvr.capi.OvrLibrary.ovrRenderAPIType.ovrRenderAPI_OpenGL;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Orientation;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Position;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.Texture;
import com.oculusvr.capi.TextureHeader;

public class HmdCamera extends ACamera {

	private static final String TAG = "HMD_Camera";
	private Hmd hmd;
	// private float hFov;
	// private float aspect;
	private final FovPort fovPorts[] = new FovPort[2];
	// private final Texture eyeTextures[] = new Texture[2];
	private final Texture eyeTextures[] = (Texture[]) new Texture().toArray(2);
	// private final Matrix4f projections[] = new Matrix4f[2];
	private Posef poses[] = new Posef[2];
	private int frameCount = -1;
	private OvrVector3f eyeOffsets[] = new OvrVector3f[2];
	private FloatBuffer[] projBuffers;
	public final FrameBuffer frameBuffers[] = new FrameBuffer[2];
	private int[] framebuffer = new int[2];

	public HmdCamera(float zNear, float zFar) {
		super();
		Hmd.initialize();

		eyes = 2;
		projection = new Matrix4f[2];

		// FrameBuffer f = new FrameBuffer();
		projBuffers = new FloatBuffer[2];

		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		hmd = openFirstHmd();
		if (null == hmd) {
			throw new IllegalStateException("Unable to initialize HMD");
		}

		if (0 == hmd.configureTracking(ovrTrackingCap_Orientation
				| ovrTrackingCap_Position, 0)) {
			throw new IllegalStateException("Unable to start the sensor");
		}
		for (int eye = 0; eye < 2; ++eye) {
			projBuffers[eye] = BufferUtils.createFloatBuffer(16);
			Log.d(TAG, "setting up eye " + eye);
			fovPorts[eye] = hmd.DefaultEyeFov[eye];
			Log.d(TAG, "fovPort size: " + fovPorts[eye].size());
			projection[eye] = MatrixUtil
					.toMatrix4f(Hmd.getPerspectiveProjection(fovPorts[eye],
							zNear, zFar, true));

			// Store the projection matrix in buffer
			projection[0].store(projBuffers[eye]);
			projBuffers[eye].rewind();

			Texture texture = eyeTextures[eye];
			TextureHeader header = texture.Header;
			header.API = ovrRenderAPI_OpenGL;
			header.TextureSize = hmd
					.getFovTextureSize(eye, fovPorts[eye], 1.0f);
			header.RenderViewport.Size = header.TextureSize;
			header.RenderViewport.Pos = new OvrVector2i(0, 0);
			frameBuffers[eye] = new FrameBuffer(header.TextureSize.w,
					header.TextureSize.h);
			eyeTextures[eye].TextureId = frameBuffers[eye].getTexture().id;


			framebuffer[eye] = GL30.glGenFramebuffers();
		}
	}

	@Override
	public void preUpdate() {
		++frameCount;
		poses = hmd.getEyePoses(frameCount, eyeOffsets);
		hmd.beginFrame(frameCount);
	}

	@Override
	public void postUpdate() {
		hmd.endFrame(poses, eyeTextures);
	}

	@Override
	public void update(int eye) {
		
		projBuffer = projBuffers[eye];
		orientation.set(poses[eye].Orientation.x, poses[eye].Orientation.y,
				poses[eye].Orientation.z, poses[eye].Orientation.w);

		Matrix4f.translate(position.negate(null), view, view);

		// Store the view matrix in the buffer
		view.store(viewBuffer);
		viewBuffer.rewind();

		GL11.glViewport(0, 0, frameBuffers[eye].width, frameBuffers[eye].height);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer[eye]);
	}

	private static Hmd openFirstHmd() {
		Hmd hmd = Hmd.create(0);
		if (null == hmd) {
			hmd = Hmd.createDebug(ovrHmd_DK1);
		}
		return hmd;
	}

	// should not be in de camera class, move to the renderer
	/*
	 * public void render(RenderScene scene){
	 * 
	 * ++frameCount; hmd.beginFrame(frameCount); Posef eyePoses[] =
	 * hmd.getEyePoses(frameCount, eyeOffsets); for (int i = 0; i < 2; ++i) {
	 * int eye = hmd.EyeRenderOrder[i]; Posef pose = eyePoses[eye];
	 * //MatrixStack.PROJECTION.set(projections[eye]); projection =
	 * projections[eye]; // This doesn't work as it breaks the contiguous nature
	 * of the array // FIXME there has to be a better way to do this
	 * poses[eye].Orientation = pose.Orientation; poses[eye].Position =
	 * pose.Position;
	 * 
	 * 
	 * } hmd.endFrame(poses, eyeTextures); }
	 */

	public void rotateY(float angle) {
	}

	public void rotateZ(float angle) {
	}

	public void rotateX(float angle) {
	}

	@Override
	public void move(Vector3f dir, float amount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(Direction dir, float amount) {
		// TODO Auto-generated method stub

	}

}
