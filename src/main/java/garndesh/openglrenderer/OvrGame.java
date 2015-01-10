package garndesh.openglrenderer;

import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_Chromatic;
import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_TimeWarp;
import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_Vignette;
import static com.oculusvr.capi.OvrLibrary.ovrHmdType.ovrHmd_DK1;
import static com.oculusvr.capi.OvrLibrary.ovrRenderAPIType.ovrRenderAPI_OpenGL;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Orientation;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Position;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Stack;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import com.oculusvr.capi.EyeRenderDesc;
import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.RenderAPIConfig;
import com.oculusvr.capi.Texture;
import com.oculusvr.capi.TextureHeader;
import com.oculusvr.capi.OvrLibrary.ovrHmdCaps;
import com.sun.jna.Pointer;

public class OvrGame {

	private Hmd hmd;
	private EyeRenderDesc eyeRenderDescs[] = null;

	private final Matrix4f[] projections = new Matrix4f[2];
	private final FrameBuffer[] frameBuffers = new FrameBuffer[2];

	private final Texture eyeTextures[] = (Texture[]) new Texture().toArray(2);
	protected final Posef[] poses = (Posef[]) new Posef().toArray(2);
	private final OvrVector3f eyeOffsets[] = (OvrVector3f[]) new OvrVector3f()
			.toArray(2);
	private final FovPort fovPorts[] = (FovPort[]) new FovPort().toArray(2);

	private int frameCount = -1;
	private Stack<Matrix4f> projection = new Stack<Matrix4f>();
	private Stack<Matrix4f> modelview = new Stack<Matrix4f>();
	private ShaderProgram shader;
	private CubeRenderer cubeRenderer;
	private OvrGame instance;
	private Transform transform;
	private Matrix4f view;
	private FloatBuffer viewBuffer;
	private Quaternion orientation;

	public OvrGame() {
		instance = this;

		try {
			// Create the default PixelFormat
			PixelFormat pfmt = new PixelFormat();

			// We need a core context with atleast OpenGL 3.2
			ContextAttribs cattr = new ContextAttribs(3, 2)
					.withForwardCompatible(true).withProfileCore(true);

			// Create the Display
			Display.create(pfmt, cattr);
		} catch (LWJGLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Hmd.initialize();

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
			fovPorts[eye] = hmd.DefaultEyeFov[eye];
			projections[eye] = MatrixUtil.toMatrix4f(Hmd
					.getPerspectiveProjection(fovPorts[eye], 0.1f, 1000000f,
							true));
			// eyeTextures[eye] = new Texture();
			Texture texture = eyeTextures[eye];
			TextureHeader header = texture.Header;
			header.API = ovrRenderAPI_OpenGL;
			header.TextureSize = hmd
					.getFovTextureSize(eye, fovPorts[eye], 1.0f);
			header.RenderViewport.Size = header.TextureSize;
			header.RenderViewport.Pos = new OvrVector2i(0, 0);
		}

		gameLoop();
	}

	public void init() {
		for (int eye = 0; eye < 2; ++eye) {
			TextureHeader eth = eyeTextures[eye].Header;
			frameBuffers[eye] = new FrameBuffer(eth.TextureSize.w,
					eth.TextureSize.h);
			eyeTextures[eye].TextureId = frameBuffers[eye].getTexture().id;
		}

		RenderAPIConfig rc = new RenderAPIConfig();
		rc.Header.RTSize = hmd.Resolution;
		rc.Header.Multisample = 1;

		int distortionCaps = ovrDistortionCap_Chromatic
				| ovrDistortionCap_TimeWarp | ovrDistortionCap_Vignette;

		for (int i = 0; i < rc.PlatformData.length; ++i) {
			rc.PlatformData[i] = Pointer.createConstant(0);
		}

		eyeRenderDescs = hmd.configureRendering(rc, distortionCaps, fovPorts);

		for (int eye = 0; eye < 2; ++eye) {
			this.eyeOffsets[eye].x = eyeRenderDescs[eye].HmdToEyeViewOffset.x;
			this.eyeOffsets[eye].y = eyeRenderDescs[eye].HmdToEyeViewOffset.y;
			this.eyeOffsets[eye].z = eyeRenderDescs[eye].HmdToEyeViewOffset.z;
		}

		view = new Matrix4f();
		// Create the projection and view matrix buffers
		viewBuffer = BufferUtils.createFloatBuffer(16);

		transform = new Transform();

		shader = new ShaderProgram();
		shader.attachVertexShader("garndesh/openglrenderer/vertex01.vert");
		shader.attachFragmentShader("garndesh/openglrenderer/fragment01.frag");
		shader.link();

		// Set the texture sampler
		shader.setUniform("tex", new float[] { 2 });

		cubeRenderer = new CubeRenderer();

		glBindVertexArray(0);

		glEnable(GL_DEPTH_TEST);
		// Native window support currently only available on windows
		/*
		 * if (LWJGLUtil.PLATFORM_WINDOWS == LWJGLUtil.getPlatform()) { long
		 * nativeWindow = getNativeWindow(); if (0 == (hmd.getEnabledCaps() &
		 * ovrHmdCaps.ovrHmdCap_ExtendDesktop)) {
		 * OvrLibrary.INSTANCE.ovrHmd_AttachToWindow(hmd,
		 * Pointer.createConstant(nativeWindow), null, null); } }
		 */
	}

	public static long getCurrentTime() {
		return Sys.getTime() * 1000 / Sys.getTimerResolution();
	}

	private void gameLoop() {

		long lastFrame = getCurrentTime();
		long thisFrame = getCurrentTime();

		init();
		setupDisplay(1920 / 4, 1080 / 4, 1920 / 2, 1080 / 2);

		while (!Display.isCloseRequested()) {

			thisFrame = getCurrentTime();
			update(thisFrame - lastFrame);
			drawFrame();
			Display.update();
			Log.d("fps", ""+(1000/(thisFrame - lastFrame))+"fps");
			lastFrame = thisFrame;
		}
	}

	public final void drawFrame() {
		++frameCount;
		hmd.beginFrame(frameCount);
		Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
		for (int i = 0; i < 2; ++i) {
			int eye = hmd.EyeRenderOrder[i];
			Posef pose = eyePoses[eye];
			// projection.
			// .set(projections[eye]);
			// This doesn't work as it breaks the contiguous nature of the array
			// FIXME there has to be a better way to do this
			//poses[eye].Orientation = pose.Orientation;
			//poses[eye].Position = pose.Position;

			// modelview.push();
			{
				// modelview.preTranslate(MatrixUtil.toVector3f(
				// poses[eye].Position).mult(-1));
				// modelview.preRotate(MatrixUtil.toQuaternion(poses[eye].Orientation).inverse());
				QuaternionUtil.toRotationMatrix(QuaternionUtil.createFromOvrQ(
						pose.Orientation, orientation), view);
				Matrix4f.translate(MatrixUtil.toVector3f(pose.Position), view,
						view);

				// Store the view matrix in the buffer
				view.store(viewBuffer);
				viewBuffer.rewind();

				shader.bind();
				shader.setUniform("m_view", viewBuffer);
				// shader.setUniform("m_proj", null);

				frameBuffers[eye].activate();
				transform.translate(5, 0, 0);

				cubeRenderer.RenderCube(transform, shader);
				// renderScene();
				frameBuffers[eye].deactivate();
			}
			// modelview.pop();
		}
		hmd.endFrame(poses, eyeTextures);
	}

	public void update(long elapsedTime) {
		while (Keyboard.next()) {
			onKeyboardEvent();
		}

		while (Mouse.next()) {
			onMouseEvent();
		}
	}

	private void onMouseEvent() {
		// TODO Auto-generated method stub

	}

	private void onKeyboardEvent() {
		if (0 != hmd.getHSWDisplayState().Displayed) {
			Log.d("tmp", "Dismissing message");
			hmd.dismissHSWDisplay();
			return;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
			this.end();
	}

	private static Hmd openFirstHmd() {
		Hmd hmd = Hmd.create(0);
		if (null == hmd) {
			hmd = Hmd.createDebug(ovrHmd_DK1);
		}
		return hmd;
	}

	protected void setupDisplay(int left, int top, int width, int height) {
		try {
			Display.setDisplayMode(new DisplayMode(width, height));
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		Display.setLocation(left, top);
		Display.setVSyncEnabled(true);
		resized();
	}

	/**
	 * Handle Display resizing
	 */
	public void resized() {
		glViewport(0, 0, Display.getWidth(), Display.getHeight());
	}

	protected void end() {
		shader.dispose();
		cubeRenderer.dispose();

		hmd.destroy();
		Hmd.shutdown();

		Display.destroy();
		System.exit(0);
	}
}
