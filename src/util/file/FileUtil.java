/*
 * ���ϰ� ���õ� �۾��� �����ִ� ���뼺�� Ŭ���� ����
 * */
package util.file;

public class FileUtil {
	/* �Ѱܹ��� ��ο��� Ȯ���� ���ϱ� */
	public static String getExt(String path){	// FileUtil.getExt()�� ��� ����
		//c://aa/ddd/test....aa.jpg
		int last=path.lastIndexOf(".");
		
		// ������ .�������� path�� ���̱��� ���ϸ� Ȯ���ڸ� ���� �� ����
		return path.substring(last+1, path.length());
	}

}
