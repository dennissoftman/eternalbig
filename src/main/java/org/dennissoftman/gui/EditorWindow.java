package org.dennissoftman.gui;

import org.dennissoftman.format.bigf.BigfFileInfo;
import org.dennissoftman.format.bigf.BigfReader;
import org.dennissoftman.format.dds.DDSImageReaderSpi;
import org.dennissoftman.format.tga.TGAImageReaderSpi;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import java.util.prefs.Preferences;

public class EditorWindow extends JFrame implements ActionListener {
    public static final String LAST_USED_FOLDER = "last_used_dir_path";
    private JMenuBar menuBar;
    private JPanel mainPanel;
    // menuBarItems
    private JMenuItem openFileMenuItem, exitFileMenuItem;
    private JMenuItem extractAllItem, extractSelectedItem;
    // frame items
    private JList<BigfFileInfo> fileViewList;
    private JScrollPane fileScrollPane;
    private JTextPane fileEditArea; // for text files
    private String targetFilePath;
    public EditorWindow()
    {
        super("BigEditor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // init tga loader
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new TGAImageReaderSpi());
        // init dds loader
        registry.registerServiceProvider(new DDSImageReaderSpi());
        //

        constructWindow();
        pack();
    }

    private void addMenuBar()
    {
        menuBar = new JMenuBar();

        addFileMenu(menuBar);
        addExtractMenu(menuBar);

        setJMenuBar(menuBar);
    }
    private void addFileMenu(JMenuBar bar)
    {
        JMenu fileMenu = new JMenu("File");

        openFileMenuItem = new JMenuItem("Open");
        openFileMenuItem.addActionListener(this);
        fileMenu.add(openFileMenuItem);

        exitFileMenuItem = new JMenuItem("Exit");
        exitFileMenuItem.addActionListener(this);
        fileMenu.add(exitFileMenuItem);

        bar.add(fileMenu);
    }

    private void addExtractMenu(JMenuBar bar)
    {
        JMenu extractMenu = new JMenu("Extract");

        extractAllItem = new JMenuItem("Extract all");
        extractAllItem.addActionListener(this);
        extractMenu.add(extractAllItem);

        extractSelectedItem = new JMenuItem("Extract selected");
        extractSelectedItem.addActionListener(this);
        extractMenu.add(extractSelectedItem);

        bar.add(extractMenu);
    }

    public static boolean checkExts(String fileName, String[] extensions)
    {
        String fnm = fileName.toLowerCase();
        for(String ext : extensions)
        {
            if (fnm.endsWith(ext))
                return true;
        }
        return false;
    }

    public static final String[] SUPPORTED_TEXT_TYPES = new String[] {
        ".txt",
        ".ini",
        ".wnd" // gui description format
    };
    public static boolean isTextFile(String fileName)
    {
        return checkExts(fileName, SUPPORTED_TEXT_TYPES);
    }

    public static final String[] SUPPORTED_IMAGE_TYPES = new String[] {
        ".png",
        ".jpg", ".jpeg",
        ".tga",
        ".dds"
    };
    public static boolean isImageFile(String fileName)
    {
        return checkExts(fileName, SUPPORTED_IMAGE_TYPES);
    }

    private static byte[] readFileFromArchive(RandomAccessFile archiveFile, BigfFileInfo info) throws IOException
    {
        archiveFile.seek(info.getFileOffset());
        ByteBuffer buf = ByteBuffer.allocate(info.getFileSize());
        archiveFile.read(buf.array());
        return buf.array();
    }

    private void previewTextFile(String fileName, byte[] data)
    {
        fileEditArea.setText(new String(data));
        fileEditArea.setCaretPosition(0);
    }

    private void previewImageFile(String fileName, byte[] data)
    {
        try (
                InputStream imageStream = new ByteArrayInputStream(data);
            )
        {
            Icon icon = new ImageIcon(ImageIO.read(imageStream));
            fileEditArea.setText(String.format("Image size: %dx%d\n\n",
                    icon.getIconWidth(), icon.getIconHeight()));
            fileEditArea.insertIcon(icon);
        } catch (IOException e)
        {
            showErrorMessage(e.getMessage());
        }
    }
    private void openViewFile(BigfFileInfo info)
    {
        if(targetFilePath == null)
            return;
        try (
                RandomAccessFile archFile = new RandomAccessFile(targetFilePath, "r");
            )
        {
            fileEditArea.setText(null);
            String fileName = info.getFileName();
            if(isTextFile(fileName))
            {
                previewTextFile(fileName, readFileFromArchive(archFile, info));
            }
            else if(isImageFile(fileName))
            {
                previewImageFile(fileName, readFileFromArchive(archFile, info));
            }
        }
        catch (IOException e)
        {
            showErrorMessage(e.getMessage());
        }
    }

    private void setupFileViewList()
    {
        fileViewList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileViewList.addListSelectionListener(e -> {
            if(fileViewList.getSelectedIndex() == -1 || e.getValueIsAdjusting())
                return;

            openViewFile(fileViewList.getSelectedValue());
        });
    }

    private void constructWindow()
    {
        addMenuBar();
        setContentPane(mainPanel);
        setupFileViewList();
    }

    private void showDialog(int diagType, String title, String msg)
    {
        JOptionPane optionPane = new JOptionPane(msg, diagType);
        JDialog dialog = optionPane.createDialog(title);
        dialog.setAlwaysOnTop(true); // to show top of all other application
        dialog.setVisible(true); // to visible the dialog
    }

    private void showErrorMessage(String msg)
    {
        showDialog(JOptionPane.ERROR_MESSAGE, "Error occurred!", msg);
    }

    private void showInfoMessage(String msg)
    {
        showDialog(JOptionPane.INFORMATION_MESSAGE, "Info", msg);
    }

    private void openFilePerformed()
    {
        Preferences prefs = Preferences.userRoot().node(getClass().getName());

        JFileChooser fc = new JFileChooser(prefs.get(LAST_USED_FOLDER,
                new File(".").getAbsolutePath()));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "BIG archive file", "big"
        );
        fc.setFileFilter(filter);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archFile = fc.getSelectedFile();
        prefs.put(LAST_USED_FOLDER, archFile.getParent());
        targetFilePath = archFile.getAbsolutePath();
        try
                (
                    InputStream fstream = new FileInputStream(targetFilePath);
                )
        {
            BigfReader archReader = new BigfReader();
            List<BigfFileInfo> fileInfoList = archReader.getFileInfos(fstream);

            DefaultListModel<BigfFileInfo> newModel = new DefaultListModel<>();
            for(BigfFileInfo info : fileInfoList)
            {
                newModel.addElement(info);
            }

            fileViewList.setModel(newModel);
            fileViewList.ensureIndexIsVisible(0);
        }
        catch (IOException e)
        {
            showErrorMessage(e.getMessage());
        }
    }

    private String getTargetDirectory()
    {
        Preferences prefs = Preferences.userRoot().node(getClass().getName());

        JFileChooser fc = new JFileChooser(prefs.get(LAST_USED_FOLDER,
                new File(".").getAbsolutePath()));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return fc.getSelectedFile().getAbsolutePath();
    }

    private void extractAllPerformed()
    {
        if(targetFilePath == null) {
            showErrorMessage("No active archive opened!");
            return;
        }

        if(fileViewList.getModel().getSize() <= 0) {
            showErrorMessage("Archive is empty!");
            return;
        }

        String outDir = getTargetDirectory();
        if(outDir == null) // cancelled operation
            return;

        Vector<BigfFileInfo> allFiles = new Vector<>(fileViewList.getModel().getSize());
        for(int i=0; i < fileViewList.getModel().getSize(); i++)
            allFiles.add(fileViewList.getModel().getElementAt(i));

        extractFilesFromArchive(targetFilePath, outDir, allFiles);
        showInfoMessage(String.format("%d files extracted", allFiles.size()));
    }

    public void extractSelectedPerformed()
    {
        if(targetFilePath == null)
        {
            showErrorMessage("No active archive opened!");
            return;
        }

        if(fileViewList.getModel().getSize() <= 0)
        {
            showErrorMessage("Archive is empty!");
            return;
        }

        List<BigfFileInfo> fileList = fileViewList.getSelectedValuesList();
        if(fileList.size() == 0)
        {
            showErrorMessage("No files selected");
            return;
        }

        String outDir = getTargetDirectory();
        if(outDir == null) // cancelled operation
            return;

        extractFilesFromArchive(targetFilePath, outDir, fileList);
        showInfoMessage(String.format("%d files extracted", fileList.size()));
    }

    private void extractFilesFromArchive(String archFilePath, String outDir, List<BigfFileInfo> filesInArchive)
    {
        try (
                RandomAccessFile fileReader = new RandomAccessFile(targetFilePath, "r");
        ) {
            for (BigfFileInfo info : filesInArchive) {
                byte[] data = readFileFromArchive(fileReader, info);

                File targetFile = new File(info.getFileName());
                if(targetFile.getParent() != null) {
                    File targetDir = new File(Paths.get(outDir, targetFile.getParent()).toString());
                    if(!targetDir.exists()) {
                        if(!targetDir.mkdirs()) {
                            showErrorMessage("Failed to create folder: " + targetDir.getPath());
                            continue;
                        }
                    }
                }

                try (
                        OutputStream writer = new FileOutputStream(Paths.get(outDir, targetFile.getPath()).toFile());
                ) {
                    writer.write(data);
                } catch (IOException e)
                {
                    showErrorMessage("Failed to extract file: "+e.getMessage());
                }
            }
        }
        catch (IOException e)
        {
            showErrorMessage("Failed to extract archive" + e.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();

        if(obj == openFileMenuItem)
        {
            openFilePerformed();
        }
        else if(obj == exitFileMenuItem)
        {
            setVisible(false);
            dispose();
            System.exit(0);
        }
        else if(obj == extractAllItem)
        {
            extractAllPerformed();
        }
        else if(obj == extractSelectedItem)
        {
            extractSelectedPerformed();
        }
    }
}
