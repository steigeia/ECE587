package automenta.vivisect.swing.property.swing.table;import java.awt.Component;import java.awt.Dimension;import java.awt.Point;import java.beans.PropertyChangeEvent;import java.beans.PropertyChangeListener;import javax.swing.JScrollPane;import javax.swing.JTable;import javax.swing.SwingUtilities;import javax.swing.event.TableColumnModelListener;import javax.swing.event.TableModelListener;import javax.swing.table.JTableHeader;import javax.swing.table.TableColumn;import javax.swing.table.TableColumnModel;import javax.swing.table.TableModel;public class TableHelper {  public static PropertyChangeListener addModelTracker(JTable p_Table,      final TableModelListener p_Listener) {    PropertyChangeListener propListener = new PropertyChangeListener() {      public void propertyChange(PropertyChangeEvent event) {        TableModel oldModel = (TableModel) event.getOldValue();        TableModel newModel = (TableModel) event.getNewValue();        if (oldModel != null)          oldModel.removeTableModelListener(p_Listener);        if (newModel != null)          newModel.addTableModelListener(p_Listener);      }    };    p_Table.addPropertyChangeListener("model", propListener);    p_Table.getModel().addTableModelListener(p_Listener);    return propListener;  }  public static PropertyChangeListener addColumnModelTracker(JTable p_Table,      final TableColumnModelListener p_Listener) {    PropertyChangeListener propListener = new PropertyChangeListener() {      public void propertyChange(PropertyChangeEvent event) {        TableColumnModel oldModel = (TableColumnModel) event.getOldValue();        TableColumnModel newModel = (TableColumnModel) event.getNewValue();        if (oldModel != null)          oldModel.removeColumnModelListener(p_Listener);        if (newModel != null)          newModel.addColumnModelListener(p_Listener);      }    };    p_Table.addPropertyChangeListener("columnModel", propListener);    p_Table.getColumnModel().addColumnModelListener(p_Listener);    return propListener;  }  public static void layoutHeaders(JTable p_Table) {    int column = 0;    for (java.util.Enumeration columns = p_Table.getTableHeader()        .getColumnModel().getColumns(); columns.hasMoreElements(); column++) {      TableColumn c = (TableColumn) columns.nextElement();      Component component = c.getHeaderRenderer()          .getTableCellRendererComponent(p_Table, c.getHeaderValue(), false,              false, -1, column);      c.setPreferredWidth(Math.max(c.getPreferredWidth(), component          .getPreferredSize().width));    }  }  public static void layoutColumns(JTable p_Table, boolean p_OnlyVisibleRows) {    int column = 0;    TableColumn c = null;    int firstRow = p_OnlyVisibleRows ? getFirstVisibleRow(p_Table) : 0;    int lastRow = p_OnlyVisibleRows ? getLastVisibleRow(p_Table) : (p_Table        .getModel().getRowCount() - 1);    Dimension intercellSpacing = p_Table.getIntercellSpacing();    JTableHeader tableHeader = p_Table.getTableHeader();    for (java.util.Enumeration columns = tableHeader.getColumnModel()        .getColumns(); columns.hasMoreElements(); column++) {      c = (TableColumn) columns.nextElement();      Component component = (c.getHeaderRenderer() != null) ? c          .getHeaderRenderer().getTableCellRendererComponent(p_Table,              c.getHeaderValue(), false, false, -1, column) : tableHeader          .getDefaultRenderer().getTableCellRendererComponent(p_Table,              c.getHeaderValue(), false, false, -1, column);      int width = Math.max(c.getWidth(), component.getPreferredSize().width);      if (firstRow >= 0) {        for (int i = firstRow, d = lastRow; i <= d; i++) {          width = Math.max(width, p_Table.getCellRenderer(i, column)              .getTableCellRendererComponent(p_Table,                  p_Table.getModel().getValueAt(i, column), false, false, i,                  column).getPreferredSize().width              + intercellSpacing.width);        }      }      c.setPreferredWidth(width);      c.setWidth(width);    }  }  public static JScrollPane findScrollPane(JTable p_Table) {    return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class,        p_Table);  }  public static int getFirstVisibleRow(JTable p_Table) {    Point p = p_Table.getVisibleRect().getLocation();    return p_Table.rowAtPoint(p);  }  public static int getLastVisibleRow(JTable p_Table) {    Point p = p_Table.getVisibleRect().getLocation();    p.y = p.y + p_Table.getVisibleRect().height - 1;    int result = p_Table.rowAtPoint(p);    if (result > 0)      return result;    // if there is no rows at this point,rowatpoint() return -1,    // It means that there is not enough rows to fill the rectangle where    // the table is displayed.    // if this case we return getRowCount()-1 because    // we are sure that the last row is visible    if (p_Table.getVisibleRect().height > 0)      return p_Table.getRowCount() - 1;    else      return -1;  }  public static void setColumnWidths(JTable p_Table, int[] p_ColumnWidths) {    TableColumnModel columns = p_Table.getTableHeader().getColumnModel();    // when restoring from the prefs with a new version of the product,    // then it is possible that: p_ColumnWidths.length !=    // columns.getColumnCount()    if (p_ColumnWidths == null        || p_ColumnWidths.length != columns.getColumnCount()) {      return;    }    for (int i = 0, c = columns.getColumnCount(); i < c; i++) {      columns.getColumn(i).setPreferredWidth(p_ColumnWidths[i]);    }    p_Table.getTableHeader().resizeAndRepaint();    JScrollPane scrollpane = findScrollPane(p_Table);    if (scrollpane != null) {      scrollpane.invalidate();    }    p_Table.invalidate();  }  public static int[] getColumnWidths(JTable p_Table) {    TableColumnModel model = p_Table.getTableHeader().getColumnModel();    int[] columnWidths = new int[model.getColumnCount()];    for (int i = 0, c = columnWidths.length; i < c; i++) {      columnWidths[i] = model.getColumn(i).getWidth();    }    return columnWidths;  }}