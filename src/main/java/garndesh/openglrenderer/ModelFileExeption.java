package garndesh.openglrenderer;

public class ModelFileExeption extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int INPUT_TO_LONG = 1;
	public static final int INPUT_TO_SHORT  = 2;
	public static final int INPUT_INCORRECT_LENGTH = 4;
	public static final int INPUT_CANNOT_PARSE = 8;
	
	public int error;
	
	public ModelFileExeption(int error){
		this.error = error;
	}

}
