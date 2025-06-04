import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;

class Graph {
    int maxNeighbors;
    int[] adjncy;
    int[] xadj;
    int nvtxs;
    int[] components;
    int[] componentPtr;
    int numComponents;

    public Graph(int maxNeighbors, int[] adjncy, int[] xadj, int nvtxs,
                 int[] components, int[] componentPtr, int numComponents) {
        this.maxNeighbors = maxNeighbors;
        this.adjncy = adjncy;
        this.xadj = xadj;
        this.nvtxs = nvtxs;
        this.components = components;
        this.componentPtr = componentPtr;
        this.numComponents = numComponents;
    }
}

public class GraphVisualizer extends JFrame {
    private Graph currentGraph;
    private JPanel graphPanel;
    private JLabel infoLabel;

    // Zmienne do przechowywania konfiguracji podziału
    private int numParts = 2;
    private int partitionMargin = 10;
    private String outputFormat = "text";
    private boolean isPartitioned = false;
    private java.util.List<Graph> partitionedGraphs;

    public GraphVisualizer() {
        setTitle("Graph Partition Visualizer");
        setSize(800, 600); // Przywrócono oryginalny rozmiar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createMenu();
        createMainPanel();
        createConfigPanel();
    }

    private void createConfigPanel() {
        JPanel configPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        configPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Wybór formatu wyjściowego
        JLabel formatLabel = new JLabel("Wybierz format wyjściowy:");
        String[] formats = {"tekstowy", "binarny"};
        JComboBox<String> formatCombo = new JComboBox<>(formats);
        formatCombo.setSelectedItem("tekstowy");
        formatCombo.addActionListener(e -> outputFormat = (String) formatCombo.getSelectedItem());

        // Liczba części
        JLabel partsLabel = new JLabel("Liczba części:");
        JSpinner partsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        partsSpinner.addChangeListener(e -> numParts = (Integer) partsSpinner.getValue());

        // Margines podziału
        JLabel marginLabel = new JLabel("Margines procentowy:");
        JSpinner marginSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 50, 1));
        marginSpinner.addChangeListener(e -> partitionMargin = (Integer) marginSpinner.getValue());

        // Przycisk Start
        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> partitionGraph());

        // Dodanie elementów do panelu
        configPanel.add(formatLabel);
        configPanel.add(formatCombo);
        configPanel.add(partsLabel);
        configPanel.add(partsSpinner);
        configPanel.add(marginLabel);
        configPanel.add(marginSpinner);
        configPanel.add(new JLabel());
        configPanel.add(startButton);

        add(configPanel, BorderLayout.NORTH);
    }

    private void partitionGraph() {
        if (currentGraph == null) {
            JOptionPane.showMessageDialog(this,
                    "Proszę najpierw wczytać graf",
                    "Brak grafu",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Symulacja podziału grafu
        partitionedGraphs = new ArrayList<>();
        for (int i = 0; i < numParts; i++) {
            partitionedGraphs.add(currentGraph);
        }

        isPartitioned = true;
        infoLabel.setText("Graf podzielony na " + numParts + " części | Format: " + outputFormat);
        JOptionPane.showMessageDialog(this,
                "Graf został podzielony na " + numParts + " części.\n" +
                        "Możesz teraz eksportować części osobno.",
                "Podział zakończony",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Zmieniamy "File" na "Open"
        JMenu openMenu = new JMenu("Open");

        JMenuItem openTextItem = new JMenuItem("Open Text (.csrrg)");
        openTextItem.addActionListener(e -> openFile("text"));

        JMenuItem openBinaryItem = new JMenuItem("Open Binary (.bin)");
        openBinaryItem.addActionListener(e -> openFile("binary"));

        openMenu.add(openTextItem);
        openMenu.add(openBinaryItem);
        menuBar.add(openMenu);

        // Dodajemy przycisk Eksportuj w prawym górnym rogu
        JButton exportButton = new JButton("Eksportuj");
        exportButton.addActionListener(e -> exportGraph());
        menuBar.add(Box.createHorizontalGlue()); // Wypycha przycisk na prawo
        menuBar.add(exportButton);

        setJMenuBar(menuBar);
    }

    private void exportGraph() {
        if (!isPartitioned || partitionedGraphs == null) {
            // Eksportuj pojedynczy graf
            if (currentGraph == null) {
                JOptionPane.showMessageDialog(this,
                        "No graph to export",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Graph");

            // Tworzymy filtr dla formatu wybranego w konfiguracji
            String actualFormat = outputFormat.equals("tekstowy") ? "text" : "binary";
            if (actualFormat.equals("text")) {
                fileChooser.setFileFilter(new FileNameExtensionFilter("Text Graph Files (.csrrg)", "csrrg"));
            } else {
                fileChooser.setFileFilter(new FileNameExtensionFilter("Binary Graph Files (.bin)", "bin"));
            }

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String filePath = file.getAbsolutePath();

                // Upewnij się o rozszerzeniu pliku
                if (actualFormat.equals("text") && !filePath.endsWith(".csrrg")) {
                    file = new File(filePath + ".csrrg");
                } else if (actualFormat.equals("binary") && !filePath.endsWith(".bin")) {
                    file = new File(filePath + ".bin");
                }

                try {
                    if (actualFormat.equals("text")) {
                        writeGraphText(file, currentGraph);
                    } else {
                        writeGraphBinary(file, currentGraph);
                    }
                    JOptionPane.showMessageDialog(this,
                            "Graph exported successfully",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error exporting file: " + ex.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            // Eksportuj wiele plików dla podzielonego grafu
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Partitioned Graphs");
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File baseFile = fileChooser.getSelectedFile();
                String baseName = baseFile.getName();
                if (baseName.contains(".")) {
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                }
                File parentDir = baseFile.getParentFile();

                String actualFormat = outputFormat.equals("tekstowy") ? "text" : "binary";
                String extension = actualFormat.equals("text") ? ".csrrg" : ".bin";

                int savedCount = 0;
                for (int i = 0; i < partitionedGraphs.size(); i++) {
                    File partFile = new File(parentDir, baseName + "_part" + i + extension);
                    try {
                        if (actualFormat.equals("text")) {
                            writeGraphText(partFile, partitionedGraphs.get(i));
                        } else {
                            writeGraphBinary(partFile, partitionedGraphs.get(i));
                        }
                        savedCount++;
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Error exporting part " + i + ": " + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }

                JOptionPane.showMessageDialog(this,
                        "Exported " + savedCount + " of " + partitionedGraphs.size() + " parts",
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Graph display panel
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentGraph != null) {
                    drawGraph(g);
                }
            }
        };
        graphPanel.setBackground(Color.WHITE);

        // Info panel
        JPanel infoPanel = new JPanel();
        infoLabel = new JLabel("No graph loaded");
        infoPanel.add(infoLabel);

        mainPanel.add(graphPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void openFile(String fileType) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileType.equals("text")) {
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text Graph Files (.csrrg)", "csrrg"));
        } else {
            fileChooser.setFileFilter(new FileNameExtensionFilter("Binary Graph Files (.bin)", "bin"));
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                if (fileType.equals("text")) {
                    currentGraph = readGraphText(file);
                } else {
                    currentGraph = readGraphBinary(file);
                }
                infoLabel.setText("Loaded graph: " + file.getName() +
                        " | Vertices: " + currentGraph.nvtxs);
                graphPanel.repaint();

                isPartitioned = false;
                partitionedGraphs = null;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error loading file: " + ex.getMessage(),
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void drawGraph(Graphics g) {
        if (currentGraph == null) return;

        int n = currentGraph.nvtxs;
        int maxNeighbors = currentGraph.maxNeighbors;
        int[] xadj = currentGraph.xadj;
        int[] adjncy = currentGraph.adjncy;
        int[] components = currentGraph.components;
        int[] componentPtr = currentGraph.componentPtr;

        int rows = n;
        int cols = maxNeighbors;

        int width = graphPanel.getWidth();
        int height = graphPanel.getHeight();
        int cellWidth = Math.max(1, width / cols);
        int cellHeight = Math.max(1, height / rows);

        Map<Integer, Point> circlePositions = new HashMap<>();
        int circleCounter = 0;

        for (int i = 0; i < rows; i++) {
            int start = xadj[i];
            int end = xadj[i+1];

            for (int k = start; k < end; k++) {
                int col = adjncy[k];
                if (col < cols) {
                    int x = col * cellWidth + cellWidth / 2;
                    int y = i * cellHeight + cellHeight / 2;

                    circlePositions.put(circleCounter, new Point(x, y));
                    circleCounter++;
                }
            }
        }

        g.setColor(Color.BLACK);
        for (int vertex = 0; vertex < componentPtr.length - 1; vertex++) {
            int startIdx = componentPtr[vertex];
            int endIdx = componentPtr[vertex + 1];

            if (endIdx - startIdx < 2) continue;

            int centerCircle = components[startIdx];
            Point centerPoint = circlePositions.get(centerCircle);
            if (centerPoint == null) continue;

            for (int j = startIdx + 1; j < endIdx; j++) {
                int connectedCircle = components[j];
                Point connectedPoint = circlePositions.get(connectedCircle);
                if (connectedPoint != null) {
                    g.drawLine(centerPoint.x, centerPoint.y, connectedPoint.x, connectedPoint.y);
                }
            }
        }

        circleCounter = 0;
        for (int i = 0; i < rows; i++) {
            int start = xadj[i];
            int end = xadj[i+1];

            for (int k = start; k < end; k++) {
                int col = adjncy[k];
                if (col < cols) {
                    int x = col * cellWidth + cellWidth / 2;
                    int y = i * cellHeight + cellHeight / 2;

                    g.setColor(Color.BLUE);
                    g.fillOval(x - 10, y - 10, 20, 20);

                    g.setColor(Color.WHITE);
                    String num = String.valueOf(circleCounter);
                    FontMetrics fm = g.getFontMetrics();
                    int textWidth = fm.stringWidth(num);
                    int textHeight = fm.getAscent();
                    g.drawString(num, x - textWidth/2, y + textHeight/4);

                    circleCounter++;
                }
            }
        }
    }

    private Graph readGraphText(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int maxNeighbors = Integer.parseInt(reader.readLine().trim());

            String[] adjncyTokens = reader.readLine().split(";");
            int[] adjncy = new int[adjncyTokens.length];
            for (int i = 0; i < adjncyTokens.length; i++) {
                adjncy[i] = Integer.parseInt(adjncyTokens[i].trim());
            }

            String[] xadjTokens = reader.readLine().split(";");
            int[] xadj = new int[xadjTokens.length];
            for (int i = 0; i < xadjTokens.length; i++) {
                xadj[i] = Integer.parseInt(xadjTokens[i].trim());
            }
            int nvtxs = xadj.length - 1;

            String[] compTokens = reader.readLine().split(";");
            int[] components = new int[compTokens.length];
            for (int i = 0; i < compTokens.length; i++) {
                components[i] = Integer.parseInt(compTokens[i].trim());
            }

            String[] compPtrTokens = reader.readLine().split(";");
            int[] componentPtr = new int[compPtrTokens.length];
            for (int i = 0; i < compPtrTokens.length; i++) {
                componentPtr[i] = Integer.parseInt(compPtrTokens[i].trim());
            }
            int numComponents = componentPtr.length - 1;

            return new Graph(maxNeighbors, adjncy, xadj, nvtxs,
                    components, componentPtr, numComponents);
        }
    }

    private Graph readGraphBinary(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            int maxNeighbors = dis.readInt();

            int xadjSize = dis.readInt();
            int nvtxs = xadjSize - 1;

            int adjncySize = dis.readInt();
            int numComponents = dis.readInt();
            int componentsSize = dis.readInt();

            int[] xadj = new int[xadjSize];
            for (int i = 0; i < xadjSize; i++) {
                xadj[i] = dis.readInt();
            }

            int[] adjncy = new int[adjncySize];
            for (int i = 0; i < adjncySize; i++) {
                adjncy[i] = dis.readInt();
            }

            int[] componentPtr = new int[numComponents + 1];
            for (int i = 0; i <= numComponents; i++) {
                componentPtr[i] = dis.readInt();
            }

            int[] components = new int[componentsSize];
            for (int i = 0; i < componentsSize; i++) {
                components[i] = dis.readInt();
            }

            return new Graph(maxNeighbors, adjncy, xadj, nvtxs,
                    components, componentPtr, numComponents);
        }
    }

    private void writeGraphText(File file, Graph graph) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println(graph.maxNeighbors);

            for (int i = 0; i < graph.adjncy.length; i++) {
                writer.print(graph.adjncy[i]);
                if (i < graph.adjncy.length - 1) writer.print(";");
            }
            writer.println();

            for (int i = 0; i < graph.xadj.length; i++) {
                writer.print(graph.xadj[i]);
                if (i < graph.xadj.length - 1) writer.print(";");
            }
            writer.println();

            for (int i = 0; i < graph.components.length; i++) {
                writer.print(graph.components[i]);
                if (i < graph.components.length - 1) writer.print(";");
            }
            writer.println();

            for (int i = 0; i < graph.componentPtr.length; i++) {
                writer.print(graph.componentPtr[i]);
                if (i < graph.componentPtr.length - 1) writer.print(";");
            }
        }
    }

    private void writeGraphBinary(File file, Graph graph) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(graph.maxNeighbors);
            dos.writeInt(graph.xadj.length);
            dos.writeInt(graph.adjncy.length);
            dos.writeInt(graph.numComponents);
            dos.writeInt(graph.components.length);

            for (int value : graph.xadj) {
                dos.writeInt(value);
            }

            for (int value : graph.adjncy) {
                dos.writeInt(value);
            }

            for (int value : graph.componentPtr) {
                dos.writeInt(value);
            }

            for (int value : graph.components) {
                dos.writeInt(value);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GraphVisualizer visualizer = new GraphVisualizer();
            visualizer.setVisible(true);
        });
    }
}