/*
 * 파일과 관련된 작업을 도와주는 재사용성의 클래스 정의
 * */
package util.file;

public class FileUtil {
	/* 넘겨받은 경로에서 확장자 구하기 */
	public static String getExt(String path){	// FileUtil.getExt()로 사용 가능
		//c://aa/ddd/test....aa.jpg
		int last=path.lastIndexOf(".");
		
		// 마지막 .다음부터 path의 길이까지 구하면 확장자를 구할 수 있음
		return path.substring(last+1, path.length());
	}

}
