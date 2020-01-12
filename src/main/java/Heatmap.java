import java.io.*;

public class Heatmap {

    public final static int HEATMAP_ROW = 60;

    public final static int HEATMAP_COL = 181;

    private double heatmap[][];

    public Heatmap(String filename) {
        heatmap = new double[HEATMAP_ROW][HEATMAP_COL];
        this.loadFromFile(filename);
    }

    private void loadFromFile(String filename) {
        File file = new File(filename);

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String st = null;
        try {
            st = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.buildHeatmapFromLine(st);
    }

    private void buildHeatmapFromLine(String line) {
        String lines[] = line.split("\",\"");

        int i = 0;
        for (String l : lines) {
            l = l.replace("\"", "");
            l = l.replace("[","");
            l = l.replace("]","");
            l = l.replace(" ","");

            String values[] = l.split(",");

            int j = 0;
            for (String v : values) {
                heatmap[i][j] = Double.valueOf(v);
                j++;
            }
            i++;
        }
    }

    public static void saveHeatmap(String filename, double heatmap[][]) {
        String heatmapLine = buildHeatmapLine(heatmap);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            writer.write(heatmapLine);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String buildHeatmapLine(double heatmap[][]) {
        String line = "";

        for (int i = 0; i < HEATMAP_ROW; i++) {
            line += "\"[";
            for (int j = 0; j < HEATMAP_COL; j++) {
                line += String.valueOf(heatmap[i][j]);
                if (j + 1 < HEATMAP_COL)
                    line += ", ";
            }
            line += "]\"";
            if (i + 1 < HEATMAP_ROW)
                line += ",";
        }

        return line;
    }

    public double[][] getHeatmap() {
        return heatmap;
    }
}
