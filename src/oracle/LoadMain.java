/*
 * SQLLRD ��� �����ϱ�
 * 
 * 1. csv������ ��θ� ������
 * 2. csv������ ','�� �����Ͽ� insert������ ���� ����
 * 
 * ����)
 * insert���� while������ ������ while�� �ӵ��� insert(DB oracle ����) �ӵ����� ������ ������ �κ� ����
 * -> Sub Thread�� �̿��Ͽ� sleep�� �ɾ� ��ũ�� ������!
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
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

public class LoadMain extends JFrame implements ActionListener{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load, bt_excel, bt_del;
	JTable table;
	JScrollPane scroll;
	
	JFileChooser chooser;
	// ����, ���� ��� ��� �Է� ��Ʈ��
	FileReader reader;
	
	// �� �� �� �о�;� �ϹǷ� BufferReader ���
	BufferedReader buffr;
	
	// ������ â�� ������ ���� Ȯ��, DBManager�κ��� ���� ������ �޾ƿ� 
	Connection con;
	
	// DBManager �ʱ�ȭ
	DBManager manager=DBManager.getInstance();
	
	public LoadMain() {
		p_north=new JPanel();
		t_path=new JTextField(25);
		bt_open=new JButton("���� ����");
		bt_load=new JButton("�ε� �ϱ�");
		bt_excel=new JButton("���� �ε�");
		bt_del=new JButton("���� �ϱ�");
		
		table=new JTable();
		scroll=new JScrollPane(table);
		
		// ��ư�� �������� �ƴ�, ������ �� ��������� ��!
		chooser=new JFileChooser("C:/animal");
		
		// ��ư�� ActionListener ����
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
		
		// ������� WindowListener ����(���� �͸� Ŭ����, Override �� �޼ҵ尡 �����Ƿ� Adapter ���)
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// �����ͺ��̽� �ڿ� ����
				manager.disConnect(con);
				
				// ���μ��� ����
				System.exit(0);
			}
		});
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
	}
	
	// ������ â�� ������ ���� Ȯ��, DBManager�κ��� ���� ������ �޾ƿ�
	public void init(){
		// Connection ���
		con=manager.getConnection();
	}
	
	// ���� Ž���� ����
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		// ���⸦ ������ ���� ���Ͽ� ��Ʈ�� ����
		if(result==JFileChooser.APPROVE_OPTION){
			// ����ڰ� ������ ����
			File file=chooser.getSelectedFile();
			
			// ��� ���
			t_path.setText(file.getAbsolutePath());
			
			try {
				reader=new FileReader(file);
				buffr=new BufferedReader(reader);
				
				String data=null;
				
				// �ε� ��ư�� ���� �� DB ������ �Ǿ�� �ϹǷ� stream�� �����س���!
				/*
				while(true){
					// �� �پ� �б�
					data=buffr.readLine();
					
					// �����Ͱ� null�̸�
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
	
	// csv���� oracle�� ������ �����ϱ�(migration)
	public void load(){
		// System.out.println(buffr);
		// BufferStream�� �̿��Ͽ� �о���� ���ڵ尡 �� �̻� ���������� csv�� �����͸� �� �پ� �о� insert ���� ����
		// while������ ������ �ʹ� �����Ƿ� ��Ʈ��ũ�� ������ �� ���� ������ �������� ����
		String data;
		StringBuffer sb=new StringBuffer();
		
		// sql�� �ϳ��� ��ü �ϳ���
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data=buffr.readLine();
				// System.out.println(data);
				/*
				 * ù��° ���� �÷����̹Ƿ� ù��° ���� �����ϰ� insert ����
				 */
				// ,�� ���α׷��� ���� Ư���� ��� ������ ���� �����Ƿ� escape �ʿ�X
				// .�� ���α׷��� ���� ��ü�� �������� ���ϹǷ� escape �ʿ�
				
				// ���ڵ尡 �������� ������
				if(data==null)
					break;
				
				// ���ڵ尡 �����ϸ�
				String[] value=data.split(",");
					
				// ù��° ���� �����ϰ� insert
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					// ���� StringBuffer�� while�� �ۿ� ����Ǿ� �����Ƿ� ��� ������
					// ������ ������ StringBuffer�� �����͸� ��� ������ ��!
					//sb.delete(0, sb.length());
					
					System.out.println(sb.toString());
					
					pstmt=con.prepareStatement(sb.toString());
					
					// insert���̹Ƿ� ��ȯ��X, ResultSet ���X
					int result=pstmt.executeUpdate();
					
					sb.delete(0, sb.length());
				}
				else{
					System.out.println("ù��° ���� ����");
				}
					
			}
			JOptionPane.showMessageDialog(this, "Migration �Ϸ�!");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			// ��� �۾��� ������ �ݱ�
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Excel������ �о DB�� migrate
	// javaSE���� Excel���� ���̺귯���� �������� ����
	// Open Source ���� ����Ʈ����
	// Copyright	<->		CopyLeft(Apache ��ü)
	// POI ���̺귯��	http://apache.org
	// -> lib�� �߰��ϱ�!
	/*
	 * Excel���� ���� -> ������ �������� ��������
	 * HSSFWorkbook	��������
	 * HSSFSheet	sheet
	 * HSSFRow		row
	 * HSSFCell		cell
	*/
	/*
	 * Excel
	*/
	public void loadExcel(){
		// HSSFWorkbook(java.io.InputStream s)
		// ���� load()�� BufferReader�� ����ϱ� ������ ���� �ʾ� FileInputStream�� ���� ���
		int result=chooser.showOpenDialog(this);
		
		if(result==JFileChooser.APPROVE_OPTION){
			File file=chooser.getSelectedFile();
			FileInputStream fis=null;
			
			try {
				fis=new FileInputStream(file);
				
				// Excel������ ����(�ؼ�)�ϱ� ���� ���̺귯�� ���
				HSSFWorkbook book=null;		// try-catch���̹Ƿ� �ۿ� ����
				book=new HSSFWorkbook(fis);
				
				// getSheet(java.lang.String name)
				HSSFSheet sheet=null;
				sheet=book.getSheet("��������");
				// System.out.println(sheet);
				
				/*
				HSSFRow row=sheet.getRow(0);					// 0��° row ��������
				HSSFCell cell=row.getCell(0);					// 0��° cell ��������
				System.out.println(cell.getStringCellValue());	// 0��° cell�� String��
				*/
				
				// ������ sheet���� ������ ��������
				// ù��° row�� �÷����̹Ƿ� ����
				int totRow=sheet.getLastRowNum();
				
				// ������ ������ �� �ִ� Ŭ����(Cell�� ������ ������ numeric, String ��������)
				DataFormatter df=new DataFormatter();
				
				for(int i=1; i<=totRow; i++){		// Row
					HSSFRow row=sheet.getRow(i);
					
					int totCol=row.getLastCellNum(); 
					
					for(int j=0; j<totCol; j++){	// Column
						HSSFCell cell=row.getCell(j);
						/*
						// numeric�� String �����ϱ�
						if(cell.getCellType()==HSSFCell.CELL_TYPE_NUMERIC){
							System.out.println(cell.getNumericCellValue());
						}
						else if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							System.out.println(cell.getStringCellValue());
						}
						*/
						// �ڷ����� ���ѵ��� �ʰ� ��� String ó�� ����
						String value=df.formatCellValue(cell);
						System.out.print(value+" ");
					}
					System.out.println();
				}
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	// ������ ���ڵ� ����
	public void delete(){
		
	}
	
	// ��ư Ŭ�� ����
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
	
	public static void main(String[] args) {
		new LoadMain();
	}

}
