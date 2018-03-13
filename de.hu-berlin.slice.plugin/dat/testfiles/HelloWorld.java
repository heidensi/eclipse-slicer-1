public class HelloWorld {

	public static void main(String[] args) {
		int x = 1;
		int y = 2;
		int z = x+y;
		
		
		int o = demo1(2) + 1 + demo() + 1;
		int t = 10 + z;
		int n = 5;
		
		int u = t + n + o;
		
		System.out.println(t + u);
		
	}
	
	public static int demo() {
		int j = 2;
		int i = 1 + j;
		int t = 10;
		return i;
	}

	public static int demo1(int i ) {
		int x = 5 + i;
		return x;
	}
}
	