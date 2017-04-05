package oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
	private static DBManager instance;
	
	private String driver="oracle.jdbc.driver.OracleDriver";
	private String url="jdbc:oracle:thin:@localhost:1521:XE";
	private String user="batman";
	private String password="1234";
	
	// 접속 후, 그 정보를 담는 객체
	// 윈도우를 열때 접속, 닫을 떄 종료
	Connection con;	
	
	/*
	 * DB 연결
	 * 1. 드라이버 로드
	 * 2. 접속
	 * 3. 쿼리문 실행
	 * 4. 접속 종료
	 * */
	// new를 막기 위함(다시 생성되는 것을 방지)
	private DBManager(){
		try {
			Class.forName(driver);								// 드라이버 로드
			con=DriverManager.getConnection(url, user, password);	// 접속
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	static public DBManager getInstance(){
		// 오직 한 번만 생성가능
		if(instance==null){
			instance=new DBManager();
		}
		return instance;
	}
	
	// 접속 객체 반환
	public Connection getConnection(){
		return con;
	}
	
	// 접속 해제
	public void disConnect(Connection con){
		if(con!=null){
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
