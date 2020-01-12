import java.io.*;

public class Heatmap {

    private double heatmap[][];

    public Heatmap(String filename) {
        heatmap = new double[60][181];
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
                System.out.print(v + " ");
                heatmap[i][j] = Double.valueOf(v);
                j++;
            }
            System.out.println();

            i++;
        }
    }

    public double[][] getHeatmap() {
        return heatmap;
    }
}
