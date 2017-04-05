/*
 * SQLLRD 기능 구현하기
 * 
 * 1. csv파일의 경로를 얻어오기
 * 2. csv파일을 ','로 구분하여 insert문으로 쿼리 실행
 * 3. excel파일 얻어오기
 * 4. excel파일의 데이터를 불러와 insert문으로 쿼리 실행
 * 5. JTable 구현
 * 6. JTable 내 column 수정
 * -> JTable 내 수정은 사용되고 있는 TableModel에 의해 제어됨(관련 메소드 override), TableModelListener 사용
 * 주의)
 * insert문을 while문으로 돌리면 while문 속도가 insert(DB oracle 원격) 속도보다 빠르기 때문에 부분 에러
 * -> Sub Thread를 이용하여 sleep을 걸어 싱크를 맞추자!
 * */
package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

public class LoadMain extends JFrame implements ActionListener, TableModelListener{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load, bt_excel, bt_del;
	JTable table;
	JScrollPane scroll;
	
	JFileChooser chooser;
	// 파일, 문자 대상 기반 입력 스트림
	FileReader reader;
	
	// 한 줄 씩 읽어와야 하므로 BufferReader 사용
	BufferedReader buffr;
	
	// 윈도우 창이 열리면 접속 확보, DBManager로부터 접속 정보를 받아옴 
	Connection con;
	
	// DBManager 초기화
	DBManager manager=DBManager.getInstance();
	
	Vector<Vector> list;
	Vector columnName;
	
	public LoadMain() {
		p_north=new JPanel();
		t_path=new JTextField(25);
		bt_open=new JButton("파일 열기");
		bt_load=new JButton("로드 하기");
		bt_excel=new JButton("엑셀 로드");
		bt_del=new JButton("삭제 하기");
		
		// 아무 모델을 적용하지 않는 JTable은 편집 가능
		// TableModel을 이용하면 JTable의 편집 여부도 TableModel이 관여
		table=new JTable();
		scroll=new JScrollPane(table);
		
		// 버튼을 누를때가 아닌, 생성될 때 만들어져야 함!
		chooser=new JFileChooser("C:/animal");
		
		// 버튼과 ActionListener 연결
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_excel.addActionListener(this);
		bt_del.addActionListener(this);
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		add(p_north,BorderLayout.NORTH);
		add(scroll);
		
		// 윈도우와 WindowListener 연결(내부 익명 클래스, Override 할 메소드가 많으므로 Adapter 사용)
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// 데이터베이스 자원 해제
				manager.disConnect(con);
				
				// 프로세스 종료
				System.exit(0);
			}
		});
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
	}
	
	// 윈도우 창이 열리면 접속 확보, DBManager로부터 접속 정보를 받아옴
	public void init(){
		// Connection 얻기
		con=manager.getConnection();
	}
	
	// 파일 탐색기 띄우기
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		// 열기를 누르면 목적 파일에 스트림 생성
		if(result==JFileChooser.APPROVE_OPTION){
			// 사용자가 선택한 파일
			File file=chooser.getSelectedFile();
			
			// 경로 출력
			t_path.setText(file.getAbsolutePath());
			
			try {
				reader=new FileReader(file);
				buffr=new BufferedReader(reader);
				
				String data=null;
				
				// 로드 버튼을 누를 때 DB 연동이 되어야 하므로 stream만 생성해놓자!
				/*
				while(true){
					// 한 줄씩 읽기
					data=buffr.readLine();
					
					// 데이터가 null이면
					if(data==null)
						break;
					
					System.out.println(data);
					
				}
				*/
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	// csv에서 oracle로 데이터 이전하기(migration)
	public void load(){
		// System.out.println(buffr);
		// BufferStream을 이용하여 읽어들일 레코드가 더 이상 없을때까지 csv의 데이터를 한 줄씩 읽어 insert 쿼리 실행
		// while문으로 돌리면 너무 빠르므로 네트워크가 감당할 수 없기 때문에 지연시켜 실행
		String data;
		StringBuffer sb=new StringBuffer();
		
		// sql문 하나당 객체 하나씩
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data=buffr.readLine();
				// System.out.println(data);
				/*
				 * 첫번째 줄은 컬럼명이므로 첫번째 줄은 제외하고 insert 실행
				 */
				// ,는 프로그래밍 언어에서 특수한 기능 역할을 하지 않으므로 escape 필요X
				// .는 프로그래밍 언어에서 객체의 소유권을 뜻하므로 escape 필요
				
				// 레코드가 존재하지 않으면
				if(data==null)
					break;
				
				// 레코드가 존재하면
				String[] value=data.split(",");
					
				// 첫번째 줄을 제외하고 insert
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					// 현재 StringBuffer는 while문 밖에 선언되어 있으므로 계속 누적됨
					// 기존에 누적된 StringBuffer의 데이터를 모두 지워야 함!
					//sb.delete(0, sb.length());
					
					System.out.println(sb.toString());
					
					pstmt=con.prepareStatement(sb.toString());
					
					// insert문이므로 반환형X, ResultSet 사용X
					int result=pstmt.executeUpdate();
					
					sb.delete(0, sb.length());
				}
				else{
					System.out.println("첫번째 줄은 제외");
				}
			}
			JOptionPane.showMessageDialog(this, "Migration 완료!");
			
			// JTable 출력 -> Model 적용
			getList();
			
			// JTable에 모델 적용
			table.setModel(new MyModel(columnName, list));
			
			// 이때, JTable에 TableModel을 적용하고 적용한 TableModel에 Listener를 추가해야 시점이 맞음!
			// TableModel과 Listener 연결
			// JTable은 현재 자신이 사용하고 있는 모델을 반환해줌!(굳이 변수로 빼지 않아도 됨!)
			table.getModel().addTableModelListener(this);
			
			table.updateUI();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			// 모든 작업이 끝나면 닫기
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Excel파일을 읽어서 DB에 migrate
	// javaSE에는 Excel제어 라이브러리가 존재하지 않음
	// Open Source 공개 소프트웨어
	// Copyright	<->		CopyLeft(Apache 단체)
	// POI 라이브러리	http://apache.org
	// -> lib에 추가하기!
	/*
	 * Excel에서 상위 -> 하위로 내려가는 계층구조
	 * HSSFWorkbook	엑셀파일
	 * HSSFSheet	sheet
	 * HSSFRow		row
	 * HSSFCell		cell
	*/
	/*
	 * Excel
	*/
	public void loadExcel(){
		StringBuffer sb=new StringBuffer();
		PreparedStatement pstmt=null;
		
		ArrayList<String> valueArr=new ArrayList<>();
		
		// HSSFWorkbook(java.io.InputStream s)
		// 위의 load()는 BufferReader를 사용하기 때문에 맞지 않아 FileInputStream을 따로 사용
		int result=chooser.showOpenDialog(this);
		
		if(result==JFileChooser.APPROVE_OPTION){
			File file=chooser.getSelectedFile();
			FileInputStream fis=null;
			
			try {
				fis=new FileInputStream(file);
				
				// Excel파일을 이해(해석)하기 위한 라이브러리 사용
				HSSFWorkbook book=null;		// try-catch문이므로 밖에 선언
				book=new HSSFWorkbook(fis);
				
				// getSheet(java.lang.String name)
				HSSFSheet sheet=null;
				sheet=book.getSheet("동물병원");
				// System.out.println(sheet);
				
				/*
				HSSFRow row=sheet.getRow(0);					// 0번째 row 가져오기
				HSSFCell cell=row.getCell(0);					// 0번째 cell 가져오기
				System.out.println(cell.getStringCellValue());	// 0번째 cell의 String값
				*/
				
				// 가져온 sheet에서 데이터 가져오기
				// 첫번째 row는 컬럼명이므로 제외
				int totRow=sheet.getLastRowNum();
				
				// 형식을 변경할 수 있는 클래스(Cell의 데이터 형식이 numeric, String 섞여있음)
				DataFormatter df=new DataFormatter();
				
				for(int i=1; i<=totRow; i++){		// Row
					HSSFRow row=sheet.getRow(i);
					
					int totCol=row.getLastCellNum(); 
					
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					//sb.append(" values(");
					
					for(int j=0; j<totCol; j++){	// Column
						HSSFCell cell=row.getCell(j);
						/*
						// numeric과 String 구분하기
						if(cell.getCellType()==HSSFCell.CELL_TYPE_NUMERIC){
							System.out.println(cell.getNumericCellValue());
						}
						else if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							System.out.println(cell.getStringCellValue());
						}
						*/
						// 자료형에 국한되지 않고 모두 String 처리 가능
						String value=df.formatCellValue(cell);
						// System.out.print(value+" ");
						
						// oracle에 저장
						valueArr.add(value);
						System.out.println(valueArr.get(j));
						
					}
					sb.append(" values("+valueArr.get(0)+",'"+valueArr.get(1)+"','"+valueArr.get(2)+"','"+valueArr.get(3)+"','"+valueArr.get(4)+"',"+valueArr.get(5)+",'"+valueArr.get(6)+"')");
					// 반복문으로 구현해보기
					
					pstmt=con.prepareStatement(sb.toString());
					// insert문이므로 반환형X, ResultSet 사용X
					pstmt.executeUpdate();
					// System.out.println();
					System.out.println(sb.toString());
					sb.delete(0, sb.length());
					valueArr.removeAll(valueArr);
				}	
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				// 모든 작업이 끝나면 닫기
				if(pstmt!=null){
					try {
						pstmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	// 선택한 레코드 삭제
	public void delete(){
		
	}
	
	// 모든 레코드 가져오기
	public void getList(){
		String sql="select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();	// JTable에서 TableModel의 getValue는 2차원 Vector 지원
			
			// 컬럼명도 추출 -> JTable의 MyModel의 생성자 인수(Vector columnName, Vector list)에서 필요함!
			ResultSetMetaData meta=rs.getMetaData();
			int colCnt=meta.getColumnCount();
			columnName=new Vector();
			
			for(int i=0; i<colCnt; i++){
				// MetaData는 1번째부터 시작
				columnName.add(meta.getColumnName(i+1));
			}
			
			// ResultSet을 2차원 vector로 가공하기
			list=new Vector<Vector>();		// 2차원 vector(멤버변수로 선언해서 필요할때마다 접근 가능하도록 구현)
	
			// 커서 한 칸 전진할때마다
			while(rs.next()){
				// 레코드 1건 정보를 담을 vector
				Vector vec=new Vector();	// 1차원 vector
				
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));			
				vec.add(rs.getString("addr"));			
				vec.add(rs.getString("regdate"));			
				vec.add(rs.getString("status"));			
				vec.add(rs.getString("dimension"));			
				vec.add(rs.getString("type"));
				
				list.add(vec);					// 2차원 vector로 담기!
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// 버튼 클릭 구현
	public void actionPerformed(ActionEvent e) {
		Object obj=e.getSource();
		
		if(obj==bt_open){
			open();
		}
		else if(obj==bt_load){
			load();
		}
		else if(obj==bt_excel){
			loadExcel();
		}
		else if(obj==bt_del){
			delete();
		}
	}
	
	// TableModel의 데이터값이 변경되면, 그 찰나를 감지하는 리스너
	public void tableChanged(TableModelEvent e) {
		Object obj=e.getSource();
		MyModel model=(MyModel)obj;
		
		//System.out.println(e.getColumn());
		//System.out.println(table.getSelectedRow()+","+ e.getColumn());
		
		// System.out.println("바꿨댜");
		
		/*
		 * cell을 편집하면 row, col
			당신이 편집한 cell은 row, col번째 cell입니다.
			+
			sql문 출력만 해보기
			update hospital set 컬럼명=값 where (seq을 이용해 구분)
		 * */
		System.out.println("지금 "+table.getSelectedRow()+","+e.getColumn()+" 번째 cell을 변경했습니다.");
		
		StringBuffer sb=null;
		sb.append("update hospital set ");
		sb.append(model.getValueAt(table.getSelectedRow(), e.getColumn())+" where seq="+table.getValueAt(table.getSelectedRow(), 0));
		System.out.println(sb.toString());
		
	}
	
	public static void main(String[] args) {
		new LoadMain();
	}
}
