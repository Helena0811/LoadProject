/*
 * JTable�� ���÷� ������ ���� Controller
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector columnName;		// �÷��� ������ ���� ����
	Vector<Vector> list;			// ���ڵ带 ���� 2���� ����
	
	// rs�κ��� vector �� ������
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

	// row, col�� ��ġ�� cell�� ���� ���θ� �Ǵ��ϴ� �޼ҵ�
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	// �� cell�� ���� ���������ϵ��� - row�� col�� ��ġ�ϴ� value�� ����
	public void setValueAt(Object value, int row, int col) {
		// 2���� vector�� ���� �����ؾ� ��
		
		// row ����
		Vector vec=list.get(row);	// 1���� vector ��ȯ
		vec.set(col, value);		// col��°�� value�� ����
		
		// �� ���� �� ����Ǿ��ٴ� �޼ҵ带 �ݵ�� ȣ���� �־�� ��!
		this.fireTableCellUpdated(row, col);
	}
	
	public Object getValueAt(int row, int col) {
		// 2���� vector�� �ʿ�
		Vector vec=list.get(row);	// 2���� �迭�� row��° ��(�� ���ڵ�)
		return vec.elementAt(col);	// �� ���ڵ��� col��° column��(�� ĭ�� ��)
	}

}
