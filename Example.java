// Example.java
// Example and test program for XFileDialog.

import net.tomahawk.XFileDialog;

import java.io.File;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.*;

class Example extends JFrame
{

  String[] parents = { "Dialog", "Frame", "null" };

  private JSpinner iTrace = new JSpinner();
  private JCheckBox cNative = new JCheckBox();
  private JComboBox<String> dParent = new JComboBox<>(parents);
  private JButton bOpenOne = new JButton("Open File");
  private JButton bOpenMulti = new JButton("Open Files");
  private JButton bSave = new JButton("Save File");
  private JTextField tTitle = new JTextField("Pick Something");
  private JTextField tDir = new JTextField("");
  private JTextField tFile = new JTextField("example.dat");
  private JCheckBox cPNG = new JCheckBox();
  private JCheckBox cJPG = new JCheckBox();
  private JCheckBox cTXT = new JCheckBox();
  private JCheckBox cANY = new JCheckBox();
  private JTextArea tResults = new JTextArea(20, 50);

  void addSpace(JPanel main, int w, int h) {
    JLabel lSpace = new JLabel("");
    lSpace.setPreferredSize(new Dimension(w, h));
    main.add(lSpace);
  }

  public Example()
  {
    try{ UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );	}
    catch(Exception e) { e.printStackTrace(); }

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLocation(200,160);
    setPreferredSize(new Dimension(600, 650));
    setTitle("XFileDialog Example");

    JPanel main = new JPanel(new BorderLayout());
    main.setLayout(new FlowLayout());

    JLabel lTrace = new JLabel("Tracing level:");
    // lTrace.setPreferredSize(new Dimension(575, 30));
    iTrace.setModel(new SpinnerNumberModel(0, 0, 5, 1));
    iTrace.setValue((Integer)5);
    iTrace.setPreferredSize(new Dimension(35, 35));

    JLabel lNative = new JLabel("Try Native Dialog:");
    cNative.setSelected(true);

    JLabel lParent = new JLabel("Parent:");
    dParent.setSelectedIndex(1);

    JLabel lTitle = new JLabel("Dialog Title:");
    lTitle.setPreferredSize(new Dimension(575, 30));
    tTitle.setPreferredSize(new Dimension(575, 30));

    JLabel lPNG = new JLabel("PNG");
    JLabel lJPG = new JLabel("JPG");
    JLabel lTXT = new JLabel("TXT");
    JLabel lANY = new JLabel("*.*");

    JLabel lDir = new JLabel("Initial Directory:");
    lDir.setPreferredSize(new Dimension(575, 30));
    tDir.setPreferredSize(new Dimension(575, 30));

    JLabel lFile = new JLabel("Initial File:");
    lFile.setPreferredSize(new Dimension(575, 30));
    tFile.setPreferredSize(new Dimension(575, 30));

    bOpenOne.setPreferredSize(new Dimension(175, 36));
    bOpenMulti.setPreferredSize(new Dimension(175, 36));
    bSave.setPreferredSize(new Dimension(175, 36));

    // tResults.setPreferredSize(new Dimension(575, 50));

    main.add(lTrace);
    main.add(iTrace);
    addSpace(main, 10, 10);
    main.add(lNative);
    main.add(cNative);
    main.add(lParent);
    main.add(dParent);
    main.add(lTitle);
    main.add(tTitle);
    main.add(lPNG);
    main.add(cPNG);
    addSpace(main, 40, 10);
    main.add(lJPG);
    main.add(cJPG);
    addSpace(main, 40, 10);
    main.add(lTXT);
    main.add(cTXT);
    addSpace(main, 40, 10);
    main.add(lANY);
    main.add(cANY);
    main.add(lDir);
    main.add(tDir);
    main.add(lFile);
    main.add(tFile);
    main.add(bOpenOne);
    main.add(bOpenMulti);
    main.add(bSave);
    main.add(tResults);

    getContentPane().add(main);

    iTrace.addChangeListener(e -> setTraceLevel());
    bOpenOne.addActionListener(e -> doDialog(XFileDialog.LOAD, false));
    bOpenMulti.addActionListener(e -> doDialog(XFileDialog.LOAD, true));
    bSave.addActionListener(e ->  doDialog(XFileDialog.SAVE, false));

    pack();

    setTraceLevel();
  }

  private void setTraceLevel() {
    int i = (Integer)iTrace.getValue();
    XFileDialog.setTraceLevel(i);
  }

  private void doDialog(int mode, boolean multi) {
    if (dParent.getSelectedItem().equals("Frame")) {
      doDialog(mode, multi, this, null);
    } else if (dParent.getSelectedItem().equals("null")) {
      doDialog(mode, multi, null, null);
    } else if (dParent.getSelectedItem().equals("Dialog")) {
      Dialog parent = new Dialog(this, "Example Dialog", true);
      parent.setLayout(new FlowLayout());
      Button b = new Button("Click Me");
      b.addActionListener(e -> {
        doDialog(mode, multi, null, parent);
        parent.setVisible(false);
        parent.dispose();
      });
      parent.add(b);
      parent.setSize(300, 200);
      parent.setVisible(true);
    }
  }

  private void doDialog(int mode, boolean multi, Frame parentFrame, Dialog parentDialog) {

    tResults.setText("");

    boolean useNative = cNative.isSelected();
    String title = tTitle.getText();
    String initialDir = tDir.getText();
    String initialFile = tFile.getText();
    if (title.equalsIgnoreCase("null"))
      title = null;
    if (initialDir.equalsIgnoreCase("null"))
      initialDir = null;
    if (initialFile.equalsIgnoreCase("null"))
      initialFile = null;

    XFileDialog dlg;
    if (parentFrame != null)
      dlg = new XFileDialog(parentFrame, title);
    else if (parentDialog != null)
      dlg = new XFileDialog(parentDialog, title);
    else
      dlg = new XFileDialog((Frame)null, title); // dlg = new XFileDialog(title);
    dlg.attemptNativeWindows(useNative);
    dlg.setDirectory(initialDir);
    dlg.setFile(initialFile);
    dlg.setMode(mode);
    dlg.setMultipleMode(multi);

    if (cPNG.isSelected())
      dlg.addFilenameFilter(new XFileDialog.Filter("PNG Images", "png"));
    if (cJPG.isSelected())
      dlg.addFilenameFilter(new XFileDialog.Filter("JPG Images", "jpg", "jpeg"));
    if (cTXT.isSelected())
      dlg.addFilenameFilter(new XFileDialog.Filter("Plain Text", "txt"));
    if (cANY.isSelected())
      dlg.addFilenameFilter(new XFileDialog.Filter("All Files", "*"));

    dlg.setVisible(true);

    String dir = dlg.getDirectory();
    String file = dlg.getFile();
    File[] files = dlg.getFiles();

    String ret =
        "Dir: " + (dir == null ? "(null)" : dir) + "\n" +
        "File: " + (file == null ? "(null)" : file) + "\n";
    if (files == null) {
      ret += "Files: (null)\n";
    } else if (files.length == 0) {
      ret += "Files: empty array\n";
    } else {
      for (int i = 0; i < files.length; i++)
        ret += "Files["+i+"]: " + files[i] + "\n";
    }

    tResults.setText(ret);
  }

  public static void main(String[] args)
  {
    new Example().setVisible(true);
  }

}




