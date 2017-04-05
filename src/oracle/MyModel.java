/*
 * JTable이 수시로 정보를 얻어가는 Controller
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector columnName;		// 컬럼의 제목을 담을 벡터
	Vector<Vector> list;			// 레코드를 담을 2차원 벡터
	
	// rs로부터 vector 값 얻어오기
	public MyModel(Vector columnName, Vector list) {
		this.columnName=columnName;
		this.list=list;
	}
	
	public int getRowCount() {
		return list.size();
	}

	public int getColumnCount() {
		return columnName.size();
	}
	
	public String getColumnName(int col) {
		return (String)columnName.elementAt(col);
	}

	// row, col에 위치한 cell을 편집 여부를 판단하는 메소드
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	// 각 cell의 값을 수정가능하도록 - row와 col에 위치하는 value를 수정
	public void setValueAt(Object value, int row, int col) {
		// 2차원 vector의 값을 변경해야 함
		
		// row 변경
		Vector vec=list.get(row);	// 1차원 vector 반환
		vec.set(col, value);		// col번째에 value값 저장
		
		// 값 변경 후 변경되었다는 메소드를 반드시 호출해 주어야 함!
		this.fireTableCellUpdated(row, col);
	}
	
	public Object getValueAt(int row, int col) {
		// 2차원 vector가 필요
		Vector vec=list.get(row);	// 2차원 배열의 row번째 값(한 레코드)
		return vec.elementAt(col);	// 한 레코드의 col번째 column값(각 칸의 값)
	}

}
