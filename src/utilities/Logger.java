package utilities;

public class Logger {
	public enum LEVELS {DEBUG, INFO};
	public static LEVELS level = LEVELS.DEBUG;
	public static void println(LEVELS l, Object s) {
		print (l, s + "\n");
	}
	
	public static void println(LEVELS l) {
		print(l, "\n");
	}
	
	public static void println() {
		print(LEVELS.INFO, "\n");
	}
	
	public static void println(String s) {
		println(LEVELS.INFO);
	}
	
	public static void print(String s) {
		print(LEVELS.INFO, s);
	}
	
	public static void print(LEVELS l, String s) {
		if (l == LEVELS.DEBUG) {
			System.out.print(s.toString());
		} else if (level == LEVELS.INFO){
			System.out.print(s.toString());
		}
	}
}
